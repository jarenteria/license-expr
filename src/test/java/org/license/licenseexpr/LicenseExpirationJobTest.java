package org.license.licenseexpr;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.time.Clock;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class LicenseExpirationJobTest {

    private static final ZoneId ZONE = ZoneId.of("America/Phoenix");

    private final InMemoryLicenseSource source = new InMemoryLicenseSource();
    private final InMemoryStore store = new InMemoryStore();
    private final RecordingPublisher publisher = new RecordingPublisher();

    @BeforeEach
    void specLicenses() {
        // SPEC §7.2
        source.clinicians = List.of(
                new Clinician("C-1", List.of(
                        new License("L-7", LocalDate.of(2026, 1, 31)),
                        new License("L-8", LocalDate.of(2026, 4, 1)))),
                new Clinician("C-2", List.of(
                        new License("L-9", LocalDate.of(2027, 6, 30)))));
    }

    @Test
    void specRunSequenceSendsEachWindowOnceIncludingAfterRenewal() {
        assertThat(regularRunOn("2026-01-01")).containsExactlyInAnyOrder(
                n("C-1", "L-7", WindowStatus.EXPIRING_30, "2026-01-31"),
                n("C-1", "L-8", WindowStatus.EXPIRING_90, "2026-04-01"));
        assertThat(regularRunOn("2026-01-02")).isEmpty();
        assertThat(regularRunOn("2026-01-31")).containsExactlyInAnyOrder(
                n("C-1", "L-7", WindowStatus.EXPIRED, "2026-01-31"),
                n("C-1", "L-8", WindowStatus.EXPIRING_60, "2026-04-01"));
        assertThat(regularRunOn("2026-02-01")).isEmpty();

        renewL7To("2027-01-31");

        assertThat(regularRunOn("2026-02-02")).isEmpty();
        assertThat(regularRunOn("2026-04-01")).containsExactly(
                n("C-1", "L-8", WindowStatus.EXPIRED, "2026-04-01"));
        assertThat(regularRunOn("2026-11-02")).containsExactly(
                n("C-1", "L-7", WindowStatus.EXPIRING_90, "2027-01-31"));
    }

    @Test
    void forecastReportsEverythingDueAndRemembersNothing() {
        regularRunOn("2026-01-01");
        regularRunOn("2026-01-31");
        Set<NotificationKey> sentBefore = Set.copyOf(store.sent);

        // SPEC §7.3: run on 2026-02-01 with as-of date 2026-04-01
        assertThat(runOn("2026-02-01", "2026-04-01")).containsExactlyInAnyOrder(
                n("C-1", "L-7", WindowStatus.EXPIRED, "2026-01-31"),
                n("C-1", "L-8", WindowStatus.EXPIRED, "2026-04-01"));
        assertThat(store.sent).isEqualTo(sentBefore);
        assertThat(store.loadCalls).isEqualTo(2); // only the two regular runs read the store

        assertThat(regularRunOn("2026-04-01")).containsExactly(
                n("C-1", "L-8", WindowStatus.EXPIRED, "2026-04-01"));
    }

    @Test
    void okLicensesAreNeverPublished() {
        source.clinicians = List.of(new Clinician("C-2", List.of(new License("L-9", LocalDate.of(2027, 6, 30)))));

        assertThat(regularRunOn("2026-01-01")).isEmpty();
        assertThat(runOn("2026-01-01", "2026-06-01")).isEmpty();
        assertThat(store.sent).isEmpty();
    }

    @Test
    void licenseThatSkippedWindowsIsNotifiedOnlyForItsCurrentStatus() {
        source.clinicians = List.of(new Clinician("C-1", List.of(new License("L-1", LocalDate.of(2026, 3, 1)))));

        assertThat(regularRunOn("2026-02-15")).containsExactly(
                n("C-1", "L-1", WindowStatus.EXPIRING_30, "2026-03-01"));
        assertThat(store.sent).containsExactly(
                new NotificationKey("L-1", LocalDate.of(2026, 3, 1), WindowStatus.EXPIRING_30));
    }

    @Test
    void failedDeliveryStopsTheRunAndOnlyDeliveredNotificationsAreRecorded() {
        publisher.failOnCall = 2;

        assertThatThrownBy(() -> regularRunOn("2026-01-01")).isInstanceOf(IllegalStateException.class);
        assertThat(publisher.published).hasSize(1);
        assertThat(store.sent).containsExactly(publisher.published.get(0).key());

        publisher.failOnCall = 0;
        assertThat(regularRunOn("2026-01-02")).hasSize(1).doesNotContain(publisher.published.get(0));
    }

    @Test
    void invalidLicenseDataFailsBeforeAnythingIsPublished() {
        source.failure = new IllegalStateException("line 3: invalid expiration_date");

        assertThatThrownBy(() -> regularRunOn("2026-01-01")).hasMessageContaining("line 3");
        assertThat(publisher.published).isEmpty();
        assertThat(store.loadCalls).isZero();
    }

    // --- helpers ---

    private List<Notification> regularRunOn(String today) {
        return runOn(today, today);
    }

    private List<Notification> runOn(String today, String asOf) {
        Clock clock = Clock.fixed(LocalDate.parse(today).atStartOfDay(ZONE).toInstant(), ZONE);
        LicenseExpirationJob job =
                new LicenseExpirationJob(source, store, publisher, new WindowStatusCalculator(), clock);
        int before = publisher.published.size();
        try {
            job.run(LocalDate.parse(asOf));
        } finally {
            publisher.lastRun = List.copyOf(publisher.published.subList(before, publisher.published.size()));
        }
        return publisher.lastRun;
    }

    private void renewL7To(String newDate) {
        source.clinicians = List.of(
                new Clinician("C-1", List.of(
                        new License("L-7", LocalDate.parse(newDate)),
                        new License("L-8", LocalDate.of(2026, 4, 1)))),
                new Clinician("C-2", List.of(
                        new License("L-9", LocalDate.of(2027, 6, 30)))));
    }

    private static Notification n(String clinician, String license, WindowStatus status, String expires) {
        return new Notification(clinician, license, status, LocalDate.parse(expires));
    }

    static class InMemoryLicenseSource implements LicenseSource {
        List<Clinician> clinicians = List.of();
        RuntimeException failure;

        @Override
        public List<Clinician> loadClinicians() {
            if (failure != null) {
                throw failure;
            }
            return clinicians;
        }
    }

    static class InMemoryStore implements SentNotificationStore {
        final Set<NotificationKey> sent = new LinkedHashSet<>();
        int loadCalls;

        @Override
        public Set<NotificationKey> loadSent() {
            loadCalls++;
            return Set.copyOf(sent);
        }

        @Override
        public void recordSent(NotificationKey key) {
            sent.add(key);
        }
    }

    static class RecordingPublisher implements NotificationPublisher {
        final List<Notification> published = new ArrayList<>();
        List<Notification> lastRun = List.of();
        int failOnCall;
        int calls;

        @Override
        public void publish(Notification notification) {
            calls++;
            if (calls == failOnCall) {
                throw new IllegalStateException("delivery failed");
            }
            published.add(notification);
        }
    }
}
