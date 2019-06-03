package no.nav.syfo.scheduler;

import org.springframework.cache.CacheManager;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import javax.inject.Inject;

@Component
public class CacheEvictionScheduler {

    private final int MINUTE_MS = 60 * 1000;
    private final int HOUR_MS = 60 * MINUTE_MS;

    private CacheManager cachemanager;

    @Inject
    public CacheEvictionScheduler(CacheManager cachemanager) {
        this.cachemanager = cachemanager;
    }

    @Scheduled(fixedRate = HOUR_MS)
    public void evictAllCachesAtInteval() {
        evictAllCaches();
    }

    private void evictAllCaches() {
        cachemanager.getCacheNames()
                .forEach(cacheName -> cachemanager.getCache(cacheName).clear());
    }
}
