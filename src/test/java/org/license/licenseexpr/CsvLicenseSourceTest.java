package org.license.licenseexpr;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDate;
import java.util.List;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

class CsvLicenseSourceTest {

    private static final String HEADER = "clinician_id,license_id,expiration_date";

    @TempDir
    Path dir;

    @Test
    void skipsHeaderParsesDatesAndGroupsRowsByClinician() throws IOException {
        Path file = write(HEADER,
                "C-1,L-7,2026-01-31",
                "C-2,L-9,2027-06-30",
                "C-1,L-8,2026-04-01");

        List<Clinician> clinicians = new CsvLicenseSource(file).loadClinicians();

        assertThat(clinicians).containsExactlyInAnyOrder(
                new Clinician("C-1", List.of(
                        new License("L-7", LocalDate.of(2026, 1, 31)),
                        new License("L-8", LocalDate.of(2026, 4, 1)))),
                new Clinician("C-2", List.of(
                        new License("L-9", LocalDate.of(2027, 6, 30)))));
    }

    @Test
    void ignoresBlankLinesAndSurroundingSpaces() throws IOException {
        Path file = write(HEADER, " C-1 , L-7 , 2026-01-31 ", "");

        assertThat(new CsvLicenseSource(file).loadClinicians()).containsExactly(
                new Clinician("C-1", List.of(new License("L-7", LocalDate.of(2026, 1, 31)))));
    }

    @Test
    void headerOnlyFileHasNoClinicians() throws IOException {
        assertThat(new CsvLicenseSource(write(HEADER)).loadClinicians()).isEmpty();
    }

    @Test
    void missingFileGivesClearError() {
        Path missing = dir.resolve("nope.csv");

        assertThatThrownBy(() -> new CsvLicenseSource(missing).loadClinicians())
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("nope.csv");
    }

    @ParameterizedTest
    @ValueSource(strings = {
            "C-2,L-9,",            // empty date
            "C-2,L-9,2026-02-30",  // invalid date
            "C-2,L-9,31/01/2026",  // wrong date format
            "C-2,,2026-01-31",     // blank license id
            ",L-9,2026-01-31",     // blank clinician id
            "C-2,L-9",             // missing field
    })
    void invalidRowAnywhereFailsTheWholeLoadWithItsLineNumber(String badRow) throws IOException {
        Path file = write(HEADER, "C-1,L-7,2026-01-31", badRow, "C-3,L-10,2026-05-01");

        assertThatThrownBy(() -> new CsvLicenseSource(file).loadClinicians())
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("line 3");
    }

    @Test
    void wrongHeaderFails() throws IOException {
        Path file = write("license_id,clinician_id,expiration_date", "L-7,C-1,2026-01-31");

        assertThatThrownBy(() -> new CsvLicenseSource(file).loadClinicians())
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("line 1");
    }

    private Path write(String... lines) throws IOException {
        Path file = dir.resolve("licenses.csv");
        Files.write(file, List.of(lines));
        return file;
    }
}
