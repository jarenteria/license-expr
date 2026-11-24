package org.license.licenseexpr;

import static org.assertj.core.api.Assertions.assertThat;

import java.nio.file.Path;
import java.time.Clock;
import java.time.ZoneId;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;

/** Checks the Spring wiring without running the job (ApplicationContextRunner doesn't call runners). */
class LicenseJobWiringTest {

    private final ApplicationContextRunner context =
            new ApplicationContextRunner().withUserConfiguration(LicenseExprApplication.class);

    @Test
    void sentFileDefaultsToSentNotificationsCsvInTheWorkingDirectory() {
        context.run(ctx -> assertThat(ctx.getBean(CsvSentNotificationStore.class).file())
                .isEqualTo(Path.of("sent-notifications.csv")));
    }

    @Test
    void fileArgumentsAreWiredIntoTheSourceAndStore() {
        context.withPropertyValues("licenses-file=in/licenses.csv", "sent-file=out/sent.csv")
                .run(ctx -> {
                    assertThat(ctx.getBean(CsvLicenseSource.class).file()).isEqualTo(Path.of("in/licenses.csv"));
                    assertThat(ctx.getBean(CsvSentNotificationStore.class).file()).isEqualTo(Path.of("out/sent.csv"));
                });
    }

    @Test
    void clockUsesTheLocalTimeZoneOfTheMachine() {
        context.run(ctx -> assertThat(ctx.getBean(Clock.class).getZone()).isEqualTo(ZoneId.systemDefault()));
    }

    @Test
    void wiresTheJobAndRunner() {
        context.run(ctx -> {
            assertThat(ctx).hasSingleBean(LicenseExpirationJob.class);
            assertThat(ctx).hasSingleBean(LicenseJobRunner.class);
            assertThat(ctx.getBean(NotificationPublisher.class)).isInstanceOf(ConsoleNotificationPublisher.class);
        });
    }
}
