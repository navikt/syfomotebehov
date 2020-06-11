package no.nav.syfo.cache

import org.springframework.cache.CacheManager
import org.springframework.scheduling.annotation.Scheduled
import org.springframework.stereotype.Component
import java.util.function.Consumer
import javax.inject.Inject

@Component
class CacheEvictionScheduler @Inject constructor(
    private val cachemanager: CacheManager
) {
    @Scheduled(fixedRate = HOUR_MS.toLong())
    fun evictAllCachesAtInteval() {
        evictAllCaches()
    }

    private fun evictAllCaches() {
        cachemanager.cacheNames
            .forEach(Consumer { cacheName: String ->
                val cache = cachemanager.getCache(cacheName)
                cache?.clear()
            })
    }

    companion object {
        private const val MINUTE_MS = 60 * 1000
        private const val HOUR_MS = 60 * MINUTE_MS
    }
}
