package app.messageservice.scheduler;

import app.messageservice.repository.MessageRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.cache.Cache;
import org.springframework.cache.CacheManager;
import org.springframework.cache.concurrent.ConcurrentMapCache;

import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class SchedulerTest {

    @Mock
    private MessageRepository messageRepository;

    @Mock
    private CacheManager cacheManager;

    @InjectMocks
    private MessageStatsScheduler messageStatsScheduler;

    @Test
    void logDailyMessageStats_delegatesToRepository() {
        when(messageRepository.count()).thenReturn(5L);

        messageStatsScheduler.logDailyMessageStats();

        verify(messageRepository).count();
    }

    @Test
    void evictUnreadCountCache_clearsCache() {
        Cache cache = new ConcurrentMapCache("unreadCounts");
        var scheduler = new MessageCacheEvictionScheduler(cacheManager);

        when(cacheManager.getCache("unreadCounts")).thenReturn(cache);
        cache.put("user", 3L);

        scheduler.evictUnreadCountCache();

        verify(cacheManager).getCache("unreadCounts");
    }
}
