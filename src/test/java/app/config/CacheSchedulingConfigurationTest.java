package app.config;

import org.junit.jupiter.api.Test;
import org.springframework.cache.CacheManager;

import static org.assertj.core.api.Assertions.assertThat;

class CacheSchedulingConfigurationTest {

    @Test
    void cacheManager_exposesExpectedCaches() {
        CacheSchedulingConfiguration configuration = new CacheSchedulingConfiguration();

        CacheManager cacheManager = configuration.cacheManager();

        assertThat(cacheManager.getCacheNames())
                .containsExactlyInAnyOrder("bookshelfCounts", "sendableBooks");
        assertThat(cacheManager.getCache("bookshelfCounts")).isNotNull();
        assertThat(cacheManager.getCache("sendableBooks")).isNotNull();
    }
}
