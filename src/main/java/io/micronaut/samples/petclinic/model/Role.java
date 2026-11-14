package io.micronaut.samples.petclinic.model;

import io.micronaut.core.annotation.Nullable;
import io.micronaut.data.annotation.GeneratedValue;
import io.micronaut.data.annotation.Id;
import io.micronaut.data.annotation.MappedEntity;
import jakarta.validation.constraints.NotNull;

/**
 * Security role granted to users for authorization checks.
 *
 * @param id the generated role id
 * @param authority the role authority stored in the database and exposed to Micronaut Security
 */
@MappedEntity("ROLE")
public record Role(@Nullable
                   @Id
                   @GeneratedValue
                   Integer id,
                   @NotNull
                   Authority authority) implements BaseEntity {

    /**
     * Supported application authorities.
     */
    public enum Authority {
        /**
         * Standard staff authority.
         */
        ROLE_STAFF,

        /**
         * Administrator authority.
         */
        ROLE_ADMIN;

        public static final String ROLE_STAFF_ = "ROLE_STAFF";
        public static final String ROLE_ADMIN_ = "ROLE_ADMIN";


    }
}
