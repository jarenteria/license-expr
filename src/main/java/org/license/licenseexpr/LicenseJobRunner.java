package org.license.licenseexpr;

import java.time.Clock;
import java.time.LocalDate;
import java.time.format.DateTimeParseException;
import java.util.List;

import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.stereotype.Component;

/**
 * Entry point of a run: checks the arguments, works out the as-of date and runs the job (PLAN §4).
 * Throwing here makes Spring Boot exit with a non-zero code.
 */
@Component
public class LicenseJobRunner implements ApplicationRunner {

    private final LicenseExpirationJob job;
    private final Clock clock;

    public LicenseJobRunner(LicenseExpirationJob job, Clock clock) {
        this.job = job;
        this.clock = clock;
    }

    @Override
    public void run(ApplicationArguments args) {
        String licensesFile = singleValue(args, "licenses-file");
        if (licensesFile == null || licensesFile.isBlank()) {
            throw new IllegalArgumentException("Missing required argument --licenses-file=<path to licenses CSV>");
        }
        job.run(asOf(args));
    }

    private LocalDate asOf(ApplicationArguments args) {
        String value = singleValue(args, "as-of");
        if (value == null) {
            return LocalDate.now(clock);
        }
        try {
            return LocalDate.parse(value);
        } catch (DateTimeParseException e) {
            throw new IllegalArgumentException("Invalid --as-of '" + value + "': expected a YYYY-MM-DD date");
        }
    }

    private static String singleValue(ApplicationArguments args, String name) {
        List<String> values = args.getOptionValues(name);
        return values == null || values.isEmpty() ? null : values.get(values.size() - 1);
    }
}
