package org.license.licenseexpr;

import java.time.Clock;
import java.time.LocalDate;
import java.util.List;
import java.util.Set;

/**
 * One run of the job (PLAN §6): evaluates every license for the as-of date and sends the
 * notifications that are due, without repeating ones already sent.
 */
public class LicenseExpirationJob {

    private final LicenseSource licenseSource;
    private final SentNotificationStore sentStore;
    private final NotificationPublisher publisher;
    private final WindowStatusCalculator calculator;
    private final Clock clock;

    public LicenseExpirationJob(LicenseSource licenseSource, SentNotificationStore sentStore,
            NotificationPublisher publisher, WindowStatusCalculator calculator, Clock clock) {
        this.licenseSource = licenseSource;
        this.sentStore = sentStore;
        this.publisher = publisher;
        this.calculator = calculator;
        this.clock = clock;
    }

    public void run(LocalDate asOf) {
        // SPEC §4: any as-of date other than today is a forecast run.
        boolean forecast = !asOf.equals(LocalDate.now(clock));

        // Loads and validates everything first, so invalid data fails before anything is sent (SPEC §2).
        List<Clinician> clinicians = licenseSource.loadClinicians();

        // Forecasts ignore what was sent and remember nothing (SPEC §6.3).
        Set<NotificationKey> sent = forecast ? Set.of() : sentStore.loadSent();

        for (Clinician clinician : clinicians) {
            for (License license : clinician.licenses()) {
                WindowStatus status = calculator.statusOf(license.expirationDate(), asOf);
                if (status == WindowStatus.OK) {
                    continue; // SPEC §6.1
                }
                Notification notification =
                        new Notification(clinician.id(), license.id(), status, license.expirationDate());
                if (!forecast && sent.contains(notification.key())) {
                    continue; // SPEC §6.2: already sent
                }
                publisher.publish(notification); // throws on failure, stopping the run before recording
                if (!forecast) {
                    sentStore.recordSent(notification.key());
                }
            }
        }
    }
}
