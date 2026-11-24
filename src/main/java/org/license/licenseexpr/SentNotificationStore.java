package org.license.licenseexpr;

import java.util.Set;

/**
 * What the job remembers between runs: the notifications it has already sent (SPEC §6.2).
 */
public interface SentNotificationStore {

    /** Every notification recorded as sent so far. */
    Set<NotificationKey> loadSent();

    /** Records one notification as sent. Called right after it was delivered. */
    void recordSent(NotificationKey key);
}
