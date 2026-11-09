package io.micronaut.samples.petclinic.system;

import com.fasterxml.jackson.annotation.JsonInclude;
import io.micronaut.samples.petclinic.generated.OwnerCareSummary;
import io.micronaut.serde.annotation.SerdeImport;

/**
 * Includes the empty {@code pets} and {@code visits} arrays required by the export schema.
 * The mixin applies this policy to generated records while omitting null properties.
 */
@SerdeImport(value = OwnerCareSummary.class, mixin = OwnerCareSummarySerialization.IncludeCollections.class)
@SerdeImport(value = OwnerCareSummary.Pets.class, mixin = OwnerCareSummarySerialization.IncludeCollections.class)
final class OwnerCareSummarySerialization {
    /** Includes non-null properties, including empty collections. */
    @JsonInclude(JsonInclude.Include.NON_NULL)
    interface IncludeCollections {
    }
}
