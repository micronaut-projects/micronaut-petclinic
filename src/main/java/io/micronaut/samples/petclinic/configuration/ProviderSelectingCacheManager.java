package io.micronaut.samples.petclinic.configuration;

import io.micronaut.cache.CacheManager;
import io.micronaut.cache.DefaultCacheManager;
import io.micronaut.cache.SyncCache;
import io.micronaut.cache.caffeine.DefaultSyncCache;
import io.micronaut.cache.oracle.OracleSyncCache;
import io.micronaut.context.annotation.Primary;
import io.micronaut.context.annotation.Replaces;
import io.micronaut.context.env.Environment;
import io.micronaut.context.exceptions.ConfigurationException;
import jakarta.inject.Singleton;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Exposes caches from the provider used by the active database environment.
 *
 * <p>The Oracle and Caffeine providers both create cache beans from
 * {@code micronaut.caches.*}. This manager filters those beans so the default
 * manager does not register duplicate names from both providers.</p>
 */
@Singleton
@Primary
@Replaces(DefaultCacheManager.class)
public final class ProviderSelectingCacheManager implements CacheManager<Object> {

    private final Map<String, SyncCache<Object>> caches = new LinkedHashMap<>();

    /**
     * Creates a manager containing only the active environment's cache provider.
     *
     * @param availableCaches cache beans created by the installed providers
     * @param environment the active Micronaut environment
     * @throws ConfigurationException if the active provider has no caches or has duplicate names
     */
    @SuppressWarnings("unchecked")
    public ProviderSelectingCacheManager(List<SyncCache<?>> availableCaches, Environment environment) {
        boolean oracle = environment.getActiveNames().contains("oracle");
        for (SyncCache<?> cache : availableCaches) {
            boolean selectedProvider = oracle
                    ? cache instanceof OracleSyncCache
                    : cache instanceof DefaultSyncCache;
            if (selectedProvider) {
                SyncCache<Object> typedCache = (SyncCache<Object>) cache;
                SyncCache<Object> previous = caches.putIfAbsent(typedCache.getName(), typedCache);
                if (previous != null) {
                    throw new ConfigurationException("Duplicate cache name: " + typedCache.getName());
                }
            }
        }
        if (caches.isEmpty()) {
            throw new ConfigurationException("No cache configured for the active provider");
        }
    }

    /**
     * Returns the names exposed by the active provider.
     *
     * @return active provider cache names
     */
    @Override
    public Set<String> getCacheNames() {
        return caches.keySet();
    }

    /**
     * Returns a cache registered by the active provider.
     *
     * @param name the cache name
     * @return the matching cache
     * @throws ConfigurationException if the active provider has no cache with that name
     */
    @Override
    public SyncCache<Object> getCache(String name) {
        SyncCache<Object> cache = caches.get(name);
        if (cache == null) {
            throw new ConfigurationException("No cache configured for name: " + name);
        }
        return cache;
    }

}
