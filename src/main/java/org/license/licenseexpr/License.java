package org.license.licenseexpr;

import java.time.LocalDate;

/**
 * A clinician's license (SPEC §2). The expiration date is required.
 */
public record License(String id, LocalDate expirationDate) {

    public License {
        if (id == null || id.isBlank()) {
            throw new IllegalArgumentException("License id is required");
        }
        if (expirationDate == null) {
            throw new IllegalArgumentException("License " + id + " has no expiration date; it is required");
        }
    }
}
