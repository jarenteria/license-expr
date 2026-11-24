package org.license.licenseexpr;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDate;
import java.util.List;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class CsvSentNotificationStoreTest {

    private static final NotificationKey L7_30 =
            new NotificationKey("L-7", LocalDate.of(2026, 1, 31), WindowStatus.EXPIRING_30);
    private static final NotificationKey L8_90 =
            new NotificationKey("L-8", LocalDate.of(2026, 4, 1), WindowStatus.EXPIRING_90);

    @TempDir
    Path dir;

    @Test
    void missingFileMeansNothingWasSent() {
        Path file = dir.resolve("sent.csv");

        assertThat(new CsvSentNotificationStore(file).loadSent()).isEmpty();
        assertThat(file).doesNotExist();
    }

    @Test
    void recordedKeysAreLoadedBackByANewStore() {
        Path file = dir.resolve("sent.csv");
        CsvSentNotificationStore store = new CsvSentNotificationStore(file);

        store.recordSent(L7_30);
        store.recordSent(L8_90);

        assertThat(new CsvSentNotificationStore(file).loadSent()).containsExactlyInAnyOrder(L7_30, L8_90);
    }

    @Test
    void writesHeaderOnceAndAppendsWithoutRewriting() throws IOException {
        Path file = dir.resolve("sent.csv");

        new CsvSentNotificationStore(file).recordSent(L7_30);
        new CsvSentNotificationStore(file).recordSent(L8_90);

        assertThat(Files.readAllLines(file, StandardCharsets.UTF_8)).containsExactly(
                "license_id,expiration_date,status",
                "L-7,2026-01-31,expiring_30",
                "L-8,2026-04-01,expiring_90");
    }

    @Test
    void malformedLineFailsWithItsLineNumber() throws IOException {
        Path file = dir.resolve("sent.csv");
        Files.write(file, List.of(
                "license_id,expiration_date,status",
                "L-7,2026-01-31,expiring_30",
                "L-8,not-a-date,expiring_90"));

        assertThatThrownBy(() -> new CsvSentNotificationStore(file).loadSent())
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("line 3");
    }

    @Test
    void unknownStatusOrWrongFieldCountFails() throws IOException {
        Path badStatus = dir.resolve("bad-status.csv");
        Files.write(badStatus, List.of("license_id,expiration_date,status", "L-7,2026-01-31,soon"));
        Path badFields = dir.resolve("bad-fields.csv");
        Files.write(badFields, List.of("license_id,expiration_date,status", "L-7,2026-01-31"));

        assertThatThrownBy(() -> new CsvSentNotificationStore(badStatus).loadSent())
                .hasMessageContaining("line 2");
        assertThatThrownBy(() -> new CsvSentNotificationStore(badFields).loadSent())
                .hasMessageContaining("line 2");
    }

    @Test
    void wrongHeaderFails() throws IOException {
        Path file = dir.resolve("sent.csv");
        Files.write(file, List.of("something,else", "L-7,2026-01-31,expiring_30"));

        assertThatThrownBy(() -> new CsvSentNotificationStore(file).loadSent())
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("line 1");
    }
}
