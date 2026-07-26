package app.messageservice.scheduler;

import app.messageservice.repository.MessageRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
public class MessageStatsScheduler {

    private static final Logger log = LoggerFactory.getLogger(MessageStatsScheduler.class);

    private final MessageRepository messageRepository;

    public MessageStatsScheduler(MessageRepository messageRepository) {
        this.messageRepository = messageRepository;
    }

    @Scheduled(cron = "${app.scheduler.message-stats-cron:0 0 2 * * *}")
    public void logDailyMessageStats() {
        long totalMessages = messageRepository.count();
        log.info("Daily message stats: {} messages stored in message-service database", totalMessages);
    }
}
