package org.license.licenseexpr;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDate;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Reads licenses from a CSV file (PLAN §4) and groups them by clinician. Every row is validated
 * before anything is returned: one invalid row fails the whole load, naming its line (SPEC §2).
 */
public class CsvLicenseSource implements LicenseSource {

    static final String HEADER = "clinician_id,license_id,expiration_date";

    private final Path file;

    public CsvLicenseSource(Path file) {
        this.file = file;
    }

    public Path file() {
        return file;
    }

    @Override
    public List<Clinician> loadClinicians() {
        List<String> lines = readLines();

        Map<String, List<License>> licensesByClinician = new LinkedHashMap<>();
        for (int i = 0; i < lines.size(); i++) {
            int lineNumber = i + 1;
            String line = lines.get(i);
            if (lineNumber == 1) {
                if (!line.strip().equals(HEADER)) {
                    throw invalid(lineNumber, "expected header '" + HEADER + "'");
                }
                continue;
            }
            if (line.isBlank()) {
                continue;
            }
            String[] fields = line.split(",", -1);
            if (fields.length != 3) {
                throw invalid(lineNumber, "expected 3 fields but found " + fields.length);
            }
            String clinicianId = fields[0].strip();
            String licenseId = fields[1].strip();
            String date = fields[2].strip();
            if (clinicianId.isEmpty()) {
                throw invalid(lineNumber, "clinician_id is missing");
            }
            if (licenseId.isEmpty()) {
                throw invalid(lineNumber, "license_id is missing");
            }
            if (date.isEmpty()) {
                throw invalid(lineNumber, "expiration_date is missing; it is required");
            }
            LocalDate expirationDate;
            try {
                expirationDate = LocalDate.parse(date);
            } catch (DateTimeParseException e) {
                throw invalid(lineNumber, "expiration_date '" + date + "' is not a valid YYYY-MM-DD date");
            }
            licensesByClinician.computeIfAbsent(clinicianId, id -> new ArrayList<>())
                    .add(new License(licenseId, expirationDate));
        }

        List<Clinician> clinicians = new ArrayList<>();
        licensesByClinician.forEach((id, licenses) -> clinicians.add(new Clinician(id, licenses)));
        return clinicians;
    }

    private List<String> readLines() {
        if (!Files.isReadable(file)) {
            throw new IllegalStateException("Licenses file " + file + " does not exist or can't be read");
        }
        try {
            return Files.readAllLines(file, StandardCharsets.UTF_8);
        } catch (IOException e) {
            throw new IllegalStateException("Could not read licenses file " + file + ": " + e.getMessage(), e);
        }
    }

    private IllegalStateException invalid(int lineNumber, String reason) {
        return new IllegalStateException("Invalid licenses file " + file + " at line " + lineNumber + ": " + reason);
    }
}
