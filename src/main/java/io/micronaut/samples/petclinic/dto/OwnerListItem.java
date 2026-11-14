package io.micronaut.samples.petclinic.dto;

import io.micronaut.serde.annotation.Serdeable;
import org.jspecify.annotations.Nullable;

import java.util.List;

/**
 * Owner card data for asynchronous list updates, without nested owner/pet associations.
 *
 * @param id the owner identifier
 * @param firstName the owner's first name
 * @param lastName the owner's last name
 * @param city the owner's city
 * @param address the owner's address
 * @param telephone the telephone number, or {@code null} when masked by the database
 * @param pets the owner's pet names
 */
@Serdeable
public record OwnerListItem(Integer id, String firstName, String lastName, String city,
                            String address, @Nullable String telephone, List<String> pets) {
}
