package io.micronaut.samples.petclinic.configuration;

import io.micronaut.cache.CacheManager;
import io.micronaut.cache.caffeine.DefaultSyncCache;
import io.micronaut.cache.oracle.OracleSyncCache;
import io.micronaut.cache.oracle.schema.OracleCacheSchemaInitializer;
import io.micronaut.context.ApplicationContext;
import io.micronaut.context.env.Environment;
import io.micronaut.test.extensions.junit5.annotation.MicronautTest;
import jakarta.inject.Inject;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

@MicronautTest
class CacheProviderIsolationTest {

    public static final String OWNER_DETAILS = "owner-details";
    @Inject
    CacheManager<?> cacheManager;

    @Inject
    Environment environment;

    @Test
    void eachEnvironmentUsesTheOwnerDetailsCacheFromItsProvider() {
        assertThat(cacheManager.getCacheNames()).containsExactly(OWNER_DETAILS);
        if (environment.getActiveNames().contains("oracle")) {
            assertThat(cacheManager.getCache(OWNER_DETAILS)).isInstanceOf(OracleSyncCache.class);
        } else {
            assertThat(cacheManager.getCache(OWNER_DETAILS)).isInstanceOf(DefaultSyncCache.class);
        }
    }
}
