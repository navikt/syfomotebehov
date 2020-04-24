package no.nav.syfo.cache

import org.springframework.cache.CacheManager
import org.springframework.cache.annotation.EnableCaching
import org.springframework.cache.concurrent.ConcurrentMapCache
import org.springframework.cache.support.SimpleCacheManager
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration

@Configuration
@EnableCaching
class CacheConfig {
    @Bean
    fun cacheManager(): CacheManager {
        val cacheManager = SimpleCacheManager()
        cacheManager.setCaches(listOf(
                ConcurrentMapCache(CACHENAME_AKTOR_ID),
                ConcurrentMapCache(CACHENAME_AKTOR_FNR),
                ConcurrentMapCache(CACHENAME_BEHANDLENDEENHET_FNR),
                ConcurrentMapCache(CACHENAME_TILGANG_IDENT)
        ))
        return cacheManager
    }

    companion object {
        const val CACHENAME_AKTOR_ID = "aktoerid"
        const val CACHENAME_AKTOR_FNR = "aktoerfnr"
        const val CACHENAME_BEHANDLENDEENHET_FNR = "behandlendeenhetfnr"
        const val CACHENAME_TILGANG_IDENT = "tilgangtilident"
    }
}