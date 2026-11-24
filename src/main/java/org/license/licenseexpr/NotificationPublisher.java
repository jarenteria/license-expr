package org.license.licenseexpr;

/**
 * Where notifications go (SPEC §6.4). Replace the implementation to change the destination,
 * for example to publish events instead of writing output.
 */
public interface NotificationPublisher {

    /**
     * Delivers one notification. Must throw if delivery fails, so the job doesn't record it as sent.
     */
    void publish(Notification notification);
}
