package org.license.licenseexpr;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.time.LocalDate;
import java.util.List;

import org.junit.jupiter.api.Test;

class LicenseTest {

    private static final LocalDate DATE = LocalDate.of(2026, 1, 31);

    @Test
    void keepsIdAndExpirationDate() {
        License license = new License("L-7", DATE);

        assertThat(license.id()).isEqualTo("L-7");
        assertThat(license.expirationDate()).isEqualTo(DATE);
    }

    @Test
    void rejectsNullExpirationDate() {
        assertThatThrownBy(() -> new License("L-7", null))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("expiration date");
    }

    @Test
    void rejectsBlankOrNullId() {
        assertThatThrownBy(() -> new License(" ", DATE)).isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> new License(null, DATE)).isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void clinicianRejectsBlankIdAndKeepsItsLicenses() {
        License license = new License("L-7", DATE);

        assertThat(new Clinician("C-1", List.of(license)).licenses()).containsExactly(license);
        assertThatThrownBy(() -> new Clinician("", List.of(license))).isInstanceOf(IllegalArgumentException.class);
    }
}
