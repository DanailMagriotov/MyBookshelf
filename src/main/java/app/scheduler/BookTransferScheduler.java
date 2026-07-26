package app.scheduler;

import app.service.booktransfer.BookTransferService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
public class BookTransferScheduler {

    private static final Logger log = LoggerFactory.getLogger(BookTransferScheduler.class);

    private final BookTransferService bookTransferService;

    public BookTransferScheduler(BookTransferService bookTransferService) {
        this.bookTransferService = bookTransferService;
    }

    @Scheduled(cron = "${app.scheduler.overdue-transfers-cron:0 0 1 * * *}")
    public void sendOverdueTransferReminders() {
        int notifiedCount = bookTransferService.sendOverdueTransferReminders();
        if (notifiedCount > 0) {
            log.info("Scheduled job sent overdue return reminders for {} book transfer(s)", notifiedCount);
        }
    }
}
