package io.micronaut.samples.petclinic.system;

import org.junit.jupiter.api.Test;

import static io.micronaut.samples.petclinic.system.VetEtagFunctions.sysRowEtag;
import static org.assertj.core.api.Assertions.assertThat;

class VetEtagFunctionsTest {
    @Test
    void tagIsStableAndIncludesEachScalarValue() {
        String tag = sysRowEtag(1, "Zoë", "李");
        assertThat(tag).matches("[0-9A-F]{32}").isEqualTo(sysRowEtag(1, "Zoë", "李"));
        assertThat(tag).isNotEqualTo(sysRowEtag(2, "Zoë", "李"));
        assertThat(tag).isNotEqualTo(sysRowEtag(1, "Zoe", "李"));
        assertThat(tag).isNotEqualTo(sysRowEtag(1, "Zoë", "Li"));
    }

    @Test
    void distinguishesNullEmptyAndAmbiguousFieldBoundaries() {
        assertThat(sysRowEtag(1, null, "Name")).isNotEqualTo(sysRowEtag(1, "", "Name"));
        assertThat(sysRowEtag(null, "First", "Last")).isNotEqualTo(sysRowEtag(0, "First", "Last"));
        assertThat(sysRowEtag(1, "ab", "c")).isNotEqualTo(sysRowEtag(1, "a", "bc"));
        assertThat(sysRowEtag(1, "a:b", "c")).isNotEqualTo(sysRowEtag(1, "a", "b:c"));
    }
}
