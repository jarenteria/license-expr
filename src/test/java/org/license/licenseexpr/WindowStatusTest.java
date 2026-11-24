package org.license.licenseexpr;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.junit.jupiter.api.Test;

class WindowStatusTest {

    @Test
    void hasExactlyTheFiveSpecStatusesInOrderOfUrgency() {
        assertThat(WindowStatus.values()).containsExactly(
                WindowStatus.OK,
                WindowStatus.EXPIRING_90,
                WindowStatus.EXPIRING_60,
                WindowStatus.EXPIRING_30,
                WindowStatus.EXPIRED);
    }

    @Test
    void labelsMatchSpecNamesExactly() {
        assertThat(WindowStatus.OK.label()).isEqualTo("OK");
        assertThat(WindowStatus.EXPIRING_90.label()).isEqualTo("expiring_90");
        assertThat(WindowStatus.EXPIRING_60.label()).isEqualTo("expiring_60");
        assertThat(WindowStatus.EXPIRING_30.label()).isEqualTo("expiring_30");
        assertThat(WindowStatus.EXPIRED.label()).isEqualTo("expired");
    }

    @Test
    void fromLabelIsTheInverseOfLabel() {
        for (WindowStatus status : WindowStatus.values()) {
            assertThat(WindowStatus.fromLabel(status.label())).isEqualTo(status);
        }
        assertThatThrownBy(() -> WindowStatus.fromLabel("EXPIRED"))
                .isInstanceOf(IllegalArgumentException.class);
    }
}
