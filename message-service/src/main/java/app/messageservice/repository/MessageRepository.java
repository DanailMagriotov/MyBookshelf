package app.messageservice.repository;

import app.messageservice.model.entity.Message;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface MessageRepository extends JpaRepository<Message, UUID> {

    List<Message> findByReceiverIdAndHiddenFromReceiverFalseOrderBySentAtDesc(UUID receiverId);

    List<Message> findBySenderIdAndHiddenFromSenderFalseOrderBySentAtDesc(UUID senderId);

    long countByReceiverIdAndHiddenFromReceiverFalseAndReadFalse(UUID receiverId);
}
