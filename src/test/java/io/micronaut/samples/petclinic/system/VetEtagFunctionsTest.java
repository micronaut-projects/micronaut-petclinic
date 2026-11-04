package io.micronaut.samples.petclinic.system;

import org.junit.jupiter.api.Test;

import static io.micronaut.samples.petclinic.system.VetEtagFunctions.sysRowEtag;
import static org.assertj.core.api.Assertions.assertThat;

class VetEtagFunctionsTest {
    private static final String NO_SPECIALITIES_HASH = "E3B0C44298FC1C149AFBF4C8996FB92427AE41E4649B934CA495991B7852B855";

    @Test
    void tagIsStableAndIncludesEachRowValue() {
        String tag = sysRowEtag(1, "Sam", "Lee", NO_SPECIALITIES_HASH);
        assertThat(tag).matches("[0-9A-F]{32}").isEqualTo(sysRowEtag(1, "Sam", "Lee", NO_SPECIALITIES_HASH));
        assertThat(tag).isNotEqualTo(sysRowEtag(2, "Sam", "Lee", NO_SPECIALITIES_HASH));
        assertThat(tag).isNotEqualTo(sysRowEtag(1, "Pam", "Lee", NO_SPECIALITIES_HASH));
        assertThat(tag).isNotEqualTo(sysRowEtag(1, "Sam", "Li", NO_SPECIALITIES_HASH));
        assertThat(tag).isNotEqualTo(sysRowEtag(1, "Sam", "Lee", "another-speciality-hash"));
    }

    @Test
    void distinguishesNullEmptyAndAmbiguousFieldBoundaries() {
        assertThat(sysRowEtag(1, null, "Name", NO_SPECIALITIES_HASH))
                .isNotEqualTo(sysRowEtag(1, "", "Name", NO_SPECIALITIES_HASH));
        assertThat(sysRowEtag(null, "First", "Last", NO_SPECIALITIES_HASH))
                .isNotEqualTo(sysRowEtag(0, "First", "Last", NO_SPECIALITIES_HASH));
        assertThat(sysRowEtag(1, "ab", "c", NO_SPECIALITIES_HASH))
                .isNotEqualTo(sysRowEtag(1, "a", "bc", NO_SPECIALITIES_HASH));
        assertThat(sysRowEtag(1, "a:b", "c", NO_SPECIALITIES_HASH))
                .isNotEqualTo(sysRowEtag(1, "a", "b:c", NO_SPECIALITIES_HASH));
    }
}
