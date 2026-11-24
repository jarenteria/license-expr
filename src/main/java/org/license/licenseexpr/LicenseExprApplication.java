package org.license.licenseexpr;

import java.nio.file.Path;
import java.time.Clock;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;

@SpringBootApplication
public class LicenseExprApplication {

    public static void main(String[] args) {
        SpringApplication.run(LicenseExprApplication.class, args);
    }

    /** "Today" is the local date of the machine running the job (SPEC §4). */
    @Bean
    Clock clock() {
        return Clock.systemDefaultZone();
    }

    /** {@code --licenses-file}; {@link LicenseJobRunner} rejects the run if it's missing. */
    @Bean
    CsvLicenseSource licenseSource(@Value("${licenses-file:}") String file) {
        return new CsvLicenseSource(Path.of(file));
    }

    @Bean
    CsvSentNotificationStore sentNotificationStore(@Value("${sent-file:sent-notifications.csv}") String file) {
        return new CsvSentNotificationStore(Path.of(file));
    }

    @Bean
    LicenseExpirationJob licenseExpirationJob(LicenseSource licenseSource, SentNotificationStore sentStore,
            NotificationPublisher publisher, WindowStatusCalculator calculator, Clock clock) {
        return new LicenseExpirationJob(licenseSource, sentStore, publisher, calculator, clock);
    }
}
