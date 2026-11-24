package org.license.licenseexpr;

import java.time.LocalDate;

/**
 * What makes two notifications "the same" for the no-repeat rule (SPEC §6.2).
 */
public record NotificationKey(String licenseId, LocalDate expirationDate, WindowStatus status) {
}
