package org.license.licenseexpr;

import java.io.PrintStream;

import org.springframework.stereotype.Component;

/**
 * Writes each notification as one {@code key=value} line on stdout (PLAN §5).
 */
@Component
public class ConsoleNotificationPublisher implements NotificationPublisher {

    private final PrintStream out;

    public ConsoleNotificationPublisher() {
        this(System.out);
    }

    ConsoleNotificationPublisher(PrintStream out) {
        this.out = out;
    }

    @Override
    public void publish(Notification n) {
        out.print("clinician_id=" + n.clinicianId()
                + " license_id=" + n.licenseId()
                + " status=" + n.status().label()
                + " expiration_date=" + n.expirationDate()
                + "\n");
        out.flush();
        // PrintStream swallows I/O errors; surface them so a failed send isn't recorded as sent.
        if (out.checkError()) {
            throw new IllegalStateException("Could not write notification for license " + n.licenseId());
        }
    }
}
