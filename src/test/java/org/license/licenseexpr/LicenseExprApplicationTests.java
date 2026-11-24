package org.license.licenseexpr;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.PrintStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDate;
import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.boot.SpringApplication;

/**
 * End to end: starts the real app the way a person would, with command-line arguments, and checks stdout.
 * Regular runs use the real clock, so the licenses file is written relative to today.
 */
class LicenseExprApplicationTests {

    @TempDir
    Path dir;

    private final LocalDate today = LocalDate.now();
    private Path licenses;
    private Path sent;

    @BeforeEach
    void writeLicenses() throws IOException {
        licenses = dir.resolve("licenses.csv");
        sent = dir.resolve("sent-notifications.csv");
        Files.write(licenses, List.of(
                "clinician_id,license_id,expiration_date",
                "C-1,L-7," + today.plusDays(30),
                "C-1,L-8," + today.plusDays(90),
                "C-2,L-9," + today.plusDays(200),
                "C-3,L-10," + today.minusDays(1)));
    }

    @Test
    void firstRunNotifiesAndSecondRunOnTheSameDayPrintsNothing() throws IOException {
        List<String> first = run("--licenses-file=" + licenses, "--sent-file=" + sent);

        assertThat(first).containsExactlyInAnyOrder(
                "clinician_id=C-1 license_id=L-7 status=expiring_30 expiration_date=" + today.plusDays(30),
                "clinician_id=C-1 license_id=L-8 status=expiring_90 expiration_date=" + today.plusDays(90),
                "clinician_id=C-3 license_id=L-10 status=expired expiration_date=" + today.minusDays(1));
        assertThat(Files.readAllLines(sent)).hasSize(4); // header + 3

        assertThat(run("--licenses-file=" + licenses, "--sent-file=" + sent)).isEmpty();
    }

    @Test
    void forecastRunReportsEverythingDueAndRecordsNothing() throws IOException {
        run("--licenses-file=" + licenses, "--sent-file=" + sent);
        List<String> sentBefore = Files.readAllLines(sent);

        List<String> forecast = run("--licenses-file=" + licenses, "--sent-file=" + sent,
                "--as-of=" + today.plusDays(31));

        assertThat(forecast).containsExactlyInAnyOrder(
                "clinician_id=C-1 license_id=L-7 status=expired expiration_date=" + today.plusDays(30),
                "clinician_id=C-1 license_id=L-8 status=expiring_60 expiration_date=" + today.plusDays(90),
                "clinician_id=C-3 license_id=L-10 status=expired expiration_date=" + today.minusDays(1));
        assertThat(Files.readAllLines(sent)).isEqualTo(sentBefore);
    }

    @Test
    void invalidRowFailsTheRunAndPrintsNothing() throws IOException {
        Files.write(licenses, List.of(
                "clinician_id,license_id,expiration_date",
                "C-1,L-7," + today.plusDays(30),
                "C-2,L-9,not-a-date"));
        ByteArrayOutputStream stdout = new ByteArrayOutputStream();

        assertThatThrownBy(() -> runCapturing(stdout, "--licenses-file=" + licenses, "--sent-file=" + sent))
                .hasStackTraceContaining("line 3");
        assertThat(stdout.toString(StandardCharsets.UTF_8)).isEmpty();
        assertThat(sent).doesNotExist();
    }

    private List<String> run(String... args) {
        ByteArrayOutputStream stdout = new ByteArrayOutputStream();
        runCapturing(stdout, args);
        return stdout.toString(StandardCharsets.UTF_8).lines().toList();
    }

    private static void runCapturing(ByteArrayOutputStream stdout, String... args) {
        PrintStream original = System.out;
        System.setOut(new PrintStream(stdout, true, StandardCharsets.UTF_8));
        try {
            SpringApplication.run(LicenseExprApplication.class, args).close();
        } finally {
            System.setOut(original);
        }
    }
}
