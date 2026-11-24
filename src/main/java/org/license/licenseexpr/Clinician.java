package org.license.licenseexpr;

import java.util.List;

/**
 * A clinician and the licenses they hold (SPEC §2).
 */
public record Clinician(String id, List<License> licenses) {

    public Clinician {
        if (id == null || id.isBlank()) {
            throw new IllegalArgumentException("Clinician id is required");
        }
        licenses = List.copyOf(licenses);
    }
}
