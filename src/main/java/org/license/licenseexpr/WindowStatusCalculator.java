package org.license.licenseexpr;

import java.time.LocalDate;
import java.time.temporal.ChronoUnit;

import org.springframework.stereotype.Component;

/**
 * The window status rule (SPEC §3). A pure function of the expiration date and the as-of date.
 */
@Component
public class WindowStatusCalculator {

    public WindowStatus statusOf(LocalDate expirationDate, LocalDate asOf) {
        long daysLeft = ChronoUnit.DAYS.between(asOf, expirationDate);

        if (daysLeft <= 0) {
            return WindowStatus.EXPIRED;
        }
        if (daysLeft <= 30) {
            return WindowStatus.EXPIRING_30;
        }
        if (daysLeft <= 60) {
            return WindowStatus.EXPIRING_60;
        }
        if (daysLeft <= 90) {
            return WindowStatus.EXPIRING_90;
        }
        return WindowStatus.OK;
    }
}
