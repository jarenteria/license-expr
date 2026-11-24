package org.license.licenseexpr;

import java.util.List;

/**
 * Where the set of licenses comes from. Implementations must load and validate everything before
 * returning, so invalid data fails the run before anything is sent (SPEC §2).
 */
public interface LicenseSource {

    List<Clinician> loadClinicians();
}
