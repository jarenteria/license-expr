package org.license.licenseexpr;

/**
 * A license's expiration window (SPEC §3), from least to most urgent.
 */
public enum WindowStatus {

    OK("OK"),
    EXPIRING_90("expiring_90"),
    EXPIRING_60("expiring_60"),
    EXPIRING_30("expiring_30"),
    EXPIRED("expired");

    private final String label;

    WindowStatus(String label) {
        this.label = label;
    }

    /** The status name exactly as the spec writes it, used in output and stored records. */
    public String label() {
        return label;
    }

    /** The status whose {@link #label()} is exactly {@code label}. */
    public static WindowStatus fromLabel(String label) {
        for (WindowStatus status : values()) {
            if (status.label.equals(label)) {
                return status;
            }
        }
        throw new IllegalArgumentException("Unknown window status '" + label + "'");
    }
}
