package app.messageservice.scheduler;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.cache.CacheManager;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.util.Objects;

@Component
public class MessageCacheEvictionScheduler {

    private static final Logger log = LoggerFactory.getLogger(MessageCacheEvictionScheduler.class);

    private final CacheManager cacheManager;

    public MessageCacheEvictionScheduler(CacheManager cacheManager) {
        this.cacheManager = cacheManager;
    }

    @Scheduled(fixedDelayString = "${app.scheduler.cache-eviction-delay-ms:180000}")
    public void evictUnreadCountCache() {
        Objects.requireNonNull(cacheManager.getCache("unreadCounts")).clear();
        log.info("Scheduled cache eviction cleared unread message count cache");
    }
}
