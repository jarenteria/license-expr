package org.license.licenseexpr;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.LocalDate;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

class WindowStatusCalculatorTest {

    private static final LocalDate AS_OF = LocalDate.of(2026, 1, 1);

    private final WindowStatusCalculator calculator = new WindowStatusCalculator();

    /** Every row of SPEC §7.1. */
    @ParameterizedTest(name = "expires {0} ({1} days left) -> {2}")
    @CsvSource({
            "2026-04-02,  91, OK",
            "2026-04-01,  90, EXPIRING_90",
            "2026-03-03,  61, EXPIRING_90",
            "2026-03-02,  60, EXPIRING_60",
            "2026-02-01,  31, EXPIRING_60",
            "2026-01-31,  30, EXPIRING_30",
            "2026-01-02,   1, EXPIRING_30",
            "2026-01-01,   0, EXPIRED",
            "2025-12-31,  -1, EXPIRED",
    })
    void matchesSpecTable(LocalDate expirationDate, int daysLeft, WindowStatus expected) {
        assertThat(expirationDate.toEpochDay() - AS_OF.toEpochDay()).isEqualTo(daysLeft);

        assertThat(calculator.statusOf(expirationDate, AS_OF)).isEqualTo(expected);
    }

    @ParameterizedTest
    @CsvSource({"2027-06-30, OK", "2020-01-01, EXPIRED"})
    void farFromTheWindowsStillWorks(LocalDate expirationDate, WindowStatus expected) {
        assertThat(calculator.statusOf(expirationDate, AS_OF)).isEqualTo(expected);
    }
}
