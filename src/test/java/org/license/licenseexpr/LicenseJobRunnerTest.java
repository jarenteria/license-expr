package org.license.licenseexpr;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;

import java.time.Clock;
import java.time.LocalDate;
import java.time.ZoneId;

import org.junit.jupiter.api.Test;
import org.springframework.boot.DefaultApplicationArguments;

class LicenseJobRunnerTest {

    private static final ZoneId ZONE = ZoneId.of("America/Phoenix");
    private static final LocalDate TODAY = LocalDate.of(2026, 11, 24);

    private final LicenseExpirationJob job = mock(LicenseExpirationJob.class);
    private final Clock clock = Clock.fixed(TODAY.atStartOfDay(ZONE).toInstant(), ZONE);
    private final LicenseJobRunner runner = new LicenseJobRunner(job, clock);

    @Test
    void usesAsOfWhenGiven() throws Exception {
        runner.run(args("--licenses-file=licenses.csv", "--as-of=2026-01-01"));

        verify(job).run(LocalDate.of(2026, 1, 1));
    }

    @Test
    void defaultsToTodayFromTheClock() throws Exception {
        runner.run(args("--licenses-file=licenses.csv"));

        verify(job).run(TODAY);
    }

    @Test
    void missingLicensesFileFailsBeforeTheJobRuns() {
        assertThatThrownBy(() -> runner.run(args("--as-of=2026-01-01")))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("--licenses-file");
        assertThatThrownBy(() -> runner.run(args("--licenses-file=")))
                .hasMessageContaining("--licenses-file");
        verifyNoInteractions(job);
    }

    @Test
    void invalidAsOfFailsBeforeTheJobRuns() {
        assertThatThrownBy(() -> runner.run(args("--licenses-file=licenses.csv", "--as-of=01/01/2026")))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("--as-of")
                .hasMessageContaining("YYYY-MM-DD");
        verifyNoInteractions(job);
    }

    private static DefaultApplicationArguments args(String... args) {
        return new DefaultApplicationArguments(args);
    }
}
