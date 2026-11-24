package org.license.licenseexpr;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.io.ByteArrayOutputStream;
import java.io.OutputStream;
import java.io.PrintStream;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;

import org.junit.jupiter.api.Test;

class ConsoleNotificationPublisherTest {

    @Test
    void writesOneKeyValueLinePerNotification() {
        ByteArrayOutputStream bytes = new ByteArrayOutputStream();
        ConsoleNotificationPublisher publisher =
                new ConsoleNotificationPublisher(new PrintStream(bytes, true, StandardCharsets.UTF_8));

        publisher.publish(new Notification("C-1", "L-7", WindowStatus.EXPIRING_30, LocalDate.of(2026, 1, 31)));

        assertThat(bytes.toString(StandardCharsets.UTF_8))
                .isEqualTo("clinician_id=C-1 license_id=L-7 status=expiring_30 expiration_date=2026-01-31\n");
    }

    @Test
    void throwsWhenTheOutputCannotBeWritten() {
        OutputStream broken = new OutputStream() {
            @Override
            public void write(int b) throws java.io.IOException {
                throw new java.io.IOException("closed");
            }
        };
        ConsoleNotificationPublisher publisher = new ConsoleNotificationPublisher(new PrintStream(broken));

        assertThatThrownBy(() -> publisher.publish(
                new Notification("C-1", "L-7", WindowStatus.EXPIRED, LocalDate.of(2026, 1, 31))))
                .isInstanceOf(IllegalStateException.class);
    }
}
