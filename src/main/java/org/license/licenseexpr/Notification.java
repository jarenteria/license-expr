package org.license.licenseexpr;

import java.time.LocalDate;

/**
 * One notification about a license's window status, with exactly the fields of SPEC §6.1.
 */
public record Notification(String clinicianId, String licenseId, WindowStatus status, LocalDate expirationDate) {

    public NotificationKey key() {
        return new NotificationKey(licenseId, expirationDate, status);
    }
}
