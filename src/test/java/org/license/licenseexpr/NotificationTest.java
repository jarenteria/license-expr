package org.license.licenseexpr;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.LocalDate;

import org.junit.jupiter.api.Test;

class NotificationTest {

    private static final LocalDate DATE = LocalDate.of(2026, 1, 31);

    @Test
    void keyIsLicenseIdExpirationDateAndStatus() {
        Notification notification = new Notification("C-1", "L-7", WindowStatus.EXPIRING_30, DATE);

        assertThat(notification.key()).isEqualTo(new NotificationKey("L-7", DATE, WindowStatus.EXPIRING_30));
    }

    @Test
    void keyDoesNotDependOnClinician() {
        assertThat(new Notification("C-1", "L-7", WindowStatus.EXPIRING_30, DATE).key())
                .isEqualTo(new Notification("C-2", "L-7", WindowStatus.EXPIRING_30, DATE).key());
    }

    @Test
    void differentExpirationDatesGiveDifferentKeys() {
        Notification original = new Notification("C-1", "L-7", WindowStatus.EXPIRING_90, DATE);
        Notification renewed = new Notification("C-1", "L-7", WindowStatus.EXPIRING_90, DATE.plusYears(1));

        assertThat(original.key()).isNotEqualTo(renewed.key());
    }

    @Test
    void differentStatusesGiveDifferentKeys() {
        assertThat(new Notification("C-1", "L-7", WindowStatus.EXPIRING_90, DATE).key())
                .isNotEqualTo(new Notification("C-1", "L-7", WindowStatus.EXPIRING_60, DATE).key());
    }
}
