package no.nav.syfo.config;

import org.springframework.cache.CacheManager;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.cache.concurrent.ConcurrentMapCache;
import org.springframework.cache.support.SimpleCacheManager;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import static java.util.Arrays.asList;

@Configuration
@EnableCaching
public class CacheConfig {

    public static final String CACHENAME_AKTOR_ID = "aktoerid";
    public static final String CACHENAME_AKTOR_FNR = "aktoerfnr";
    public static final String CACHENAME_ORGN_KONTORGEOGRAFISK = "orgnkontorgeografisk";
    public static final String CACHENAME_PERSON_DISKRESJONSKODE = "persondiskresjonskode";
    public static final String CACHENAME_PERSON_GEOGRAFISK = "persongeografisk";
    public static final String CACHENAME_PERSON_NAVN = "personnavn";

    @Bean
    public CacheManager cacheManager() {
        SimpleCacheManager cacheManager = new SimpleCacheManager();
        cacheManager.setCaches(asList(
                new ConcurrentMapCache(CACHENAME_AKTOR_ID),
                new ConcurrentMapCache(CACHENAME_AKTOR_FNR),
                new ConcurrentMapCache(CACHENAME_ORGN_KONTORGEOGRAFISK),
                new ConcurrentMapCache(CACHENAME_PERSON_DISKRESJONSKODE),
                new ConcurrentMapCache(CACHENAME_PERSON_GEOGRAFISK),
                new ConcurrentMapCache(CACHENAME_PERSON_NAVN)
        ));
        return cacheManager;
    }
}
