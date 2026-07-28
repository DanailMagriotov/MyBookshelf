package app.scheduler;

import app.service.booktransfer.BookTransferService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.cache.Cache;
import org.springframework.cache.CacheManager;
import org.springframework.cache.concurrent.ConcurrentMapCache;

import java.util.List;

import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class SchedulerTest {

    @Mock
    private BookTransferService bookTransferService;

    @Mock
    private CacheManager cacheManager;

    @InjectMocks
    private BookTransferScheduler bookTransferScheduler;

    @Test
    void sendOverdueTransferReminders_delegatesToService() {
        when(bookTransferService.sendOverdueTransferReminders()).thenReturn(2);

        bookTransferScheduler.sendOverdueTransferReminders();

        verify(bookTransferService).sendOverdueTransferReminders();
    }

    @Test
    void sendOverdueTransferReminders_whenNoneDue_stillCallsService() {
        when(bookTransferService.sendOverdueTransferReminders()).thenReturn(0);

        bookTransferScheduler.sendOverdueTransferReminders();

        verify(bookTransferService).sendOverdueTransferReminders();
        verifyNoMoreInteractions(bookTransferService);
    }

    @Test
    void evictApplicationCaches_clearsAllCaches() {
        Cache bookshelfCache = new ConcurrentMapCache("bookshelfCounts");
        Cache sendableCache = new ConcurrentMapCache("sendableBooks");
        var scheduler = new CacheEvictionScheduler(cacheManager);

        when(cacheManager.getCacheNames()).thenReturn(List.of("bookshelfCounts", "sendableBooks"));
        when(cacheManager.getCache("bookshelfCounts")).thenReturn(bookshelfCache);
        when(cacheManager.getCache("sendableBooks")).thenReturn(sendableCache);

        bookshelfCache.put("key", "value");
        sendableCache.put("key", "value");

        scheduler.evictApplicationCaches();

        verify(cacheManager).getCache("bookshelfCounts");
        verify(cacheManager).getCache("sendableBooks");
    }
}
