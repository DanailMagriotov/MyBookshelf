package app.messageservice.model.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;
import java.util.UUID;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@Builder
@Entity
@Table(name = "message")
public class Message {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @NotNull
    @Column(name = "sender_id", nullable = false)
    private UUID senderId;

    @NotNull
    @Column(name = "receiver_id", nullable = false)
    private UUID receiverId;

    @NotBlank
    @Size(max = 30)
    @Column(nullable = false, length = 30)
    private String about;

    @NotBlank
    @Size(max = 1000)
    @Column(nullable = false, length = 1000)
    private String content;

    @NotNull
    @Column(name = "sent_at", nullable = false)
    private LocalDateTime sentAt;

    @Column(name = "read_at")
    private LocalDateTime readAt;

    @Column(name = "is_read", nullable = false)
    private boolean read;

    @Column(name = "hidden_from_sender", nullable = false)
    @Builder.Default
    private boolean hiddenFromSender = false;

    @Column(name = "hidden_from_receiver", nullable = false)
    @Builder.Default
    private boolean hiddenFromReceiver = false;
}
