package io.micronaut.samples.petclinic.service;

import io.micronaut.cache.CacheManager;
import io.micronaut.context.annotation.Requires;
import io.micronaut.samples.petclinic.exception.NotFoundEntityException;
import io.micronaut.samples.petclinic.model.Owner;
import io.micronaut.samples.petclinic.repository.OwnerRepository;
import io.micronaut.test.extensions.junit5.annotation.MicronautTest;
import jakarta.inject.Inject;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Behavior tests for owner-detail caching in environments that use Caffeine.
 */
@MicronautTest
@Requires(notEnv = "oracle")
class CaffeineOwnerDetailsCacheTest {

    @Inject
    ClinicService clinicService;

    @Inject
    OwnerRepository ownerRepository;

    @Inject
    CacheManager<?> cacheManager;

    @BeforeEach
    @AfterEach
    void clearOwnerDetailsCache() {
        cacheManager.getCache("owner-details").invalidateAll();
    }

    @Test
    void repeatedLookupsReuseCachedDetailsUntilTheCacheIsCleared() throws NotFoundEntityException {
        Owner original = createOwner("First");
        assertThat(clinicService.findOwnerById(original.id())).isEqualTo(original);

        Owner updated = ownerRepository.update(original.withAddress("Updated address"));
        assertThat(ownerRepository.findById(original.id())).contains(updated);
        assertThat(clinicService.findOwnerById(original.id())).isEqualTo(original);

        cacheManager.getCache("owner-details").invalidateAll();
        assertThat(clinicService.findOwnerById(original.id())).isEqualTo(updated);
    }

    @Test
    void differentOwnerIdsHaveIndependentCacheEntries() throws NotFoundEntityException {
        Owner first = createOwner("First");
        Owner second = createOwner("Second");
        assertThat(clinicService.findOwnerById(first.id())).isEqualTo(first);
        assertThat(clinicService.findOwnerById(second.id())).isEqualTo(second);

        ownerRepository.update(first.withAddress("Updated first address"));
        ownerRepository.update(second.withAddress("Updated second address"));

        assertThat(clinicService.findOwnerById(first.id())).isEqualTo(first);
        assertThat(clinicService.findOwnerById(second.id())).isEqualTo(second);
    }

    private Owner createOwner(String firstName) {
        return ownerRepository.save(new Owner(
                firstName, "CacheTest", "Original address", "Madison", "1234567890"));
    }
}
