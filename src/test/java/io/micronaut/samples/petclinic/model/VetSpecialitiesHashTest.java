package io.micronaut.samples.petclinic.model;

import org.junit.jupiter.api.Test;

import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;

class VetSpecialitiesHashTest {
    @Test
    void hashesSortedSpecialityIdsAndIgnoresNamesAndInputOrder() {
        Speciality firstId = new Speciality(4, "radiology");
        Speciality secondId = new Speciality(12, "surgery");
        Set<Speciality> ascending = new LinkedHashSet<>(List.of(firstId, secondId));
        Set<Speciality> descending = new LinkedHashSet<>(List.of(secondId, firstId));

        String expectedHash = new Vet(1, "Sam", "Lee", ascending, null).specialitiesHash();

        assertThat(new Vet(1, "Sam", "Lee", descending, null).specialitiesHash()).isEqualTo(expectedHash);
        assertThat(new Vet(1, "Sam", "Lee", Set.of(
                new Speciality(4, "renamed"), secondId), null).specialitiesHash()).isEqualTo(expectedHash);
        assertThat(new Vet(1, "Sam", "Lee", Set.of(firstId), null).specialitiesHash()).isNotEqualTo(expectedHash);
        assertThat(new Vet(1, "Sam", "Lee", ascending, null).withSpecialities(Set.of(firstId)).specialitiesHash())
                .isNotEqualTo(expectedHash);
        assertThat(new Vet("Sam", "Lee").specialitiesHash())
                .isEqualTo("E3B0C44298FC1C149AFBF4C8996FB92427AE41E4649B934CA495991B7852B855");
    }
}
