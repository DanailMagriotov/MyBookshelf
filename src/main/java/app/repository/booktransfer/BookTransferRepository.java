package app.repository.booktransfer;

import app.model.entity.booktransfer.BookTransfer;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.UUID;

@Repository
public interface BookTransferRepository extends JpaRepository<BookTransfer, UUID> {

    void deleteBySender_Id(UUID senderId);

    void deleteByReceiver_Id(UUID receiverId);

    void deleteByBook_Owner_Id(UUID ownerId);
}
