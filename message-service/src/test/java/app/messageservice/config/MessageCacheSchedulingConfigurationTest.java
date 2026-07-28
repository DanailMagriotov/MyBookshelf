package app.messageservice.config;

import org.junit.jupiter.api.Test;
import org.springframework.cache.CacheManager;

import static org.assertj.core.api.Assertions.assertThat;

class MessageCacheSchedulingConfigurationTest {

    @Test
    void cacheManager_exposesUnreadCountsCache() {
        MessageCacheSchedulingConfiguration configuration = new MessageCacheSchedulingConfiguration();

        CacheManager cacheManager = configuration.cacheManager();

        assertThat(cacheManager.getCacheNames()).containsExactly("unreadCounts");
        assertThat(cacheManager.getCache("unreadCounts")).isNotNull();
    }
}
