package app.scheduler;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.cache.CacheManager;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.util.Objects;

@Component
public class CacheEvictionScheduler {

    private static final Logger log = LoggerFactory.getLogger(CacheEvictionScheduler.class);

    private final CacheManager cacheManager;

    public CacheEvictionScheduler(CacheManager cacheManager) {
        this.cacheManager = cacheManager;
    }

    @Scheduled(fixedRateString = "${app.scheduler.cache-eviction-rate-ms:300000}")
    public void evictApplicationCaches() {
        cacheManager.getCacheNames().forEach(cacheName ->
                Objects.requireNonNull(cacheManager.getCache(cacheName)).clear());
        log.info("Scheduled cache eviction cleared all application caches");
    }
}
