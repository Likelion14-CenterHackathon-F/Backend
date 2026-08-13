package com.centerton.centerton.domain.aichat.entity;

import com.centerton.centerton.domain.aichat.entity.enums.ChatMessageRole;
import com.centerton.centerton.domain.patient.entity.Patient;
import com.centerton.centerton.global.entity.BaseEntity;
import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;
import jakarta.persistence.OrderBy;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Getter
@Entity
@Table(
        name = "ai_chat_rooms",
        indexes = {
                @Index(
                        name = "idx_ai_chat_rooms_patient_last_message",
                        columnList = "patient_id, last_message_at"
                )
        }
)
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class AiChatRoom extends BaseEntity {

    private static final int TITLE_MAX_LENGTH = 40;
    private static final String DEFAULT_TITLE = "새로운 증상 문의";

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "chat_room_id")
    private Long chatRoomId;

    @Column(name = "title", nullable = false, length = 80)
    private String title;

    @Column(name = "last_message_at")
    private LocalDateTime lastMessageAt;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "patient_id", nullable = false)
    private Patient patient;

    @OrderBy("sentAt ASC, chatMessageId ASC")
    @OneToMany(
            mappedBy = "chatRoom",
            cascade = CascadeType.ALL,
            orphanRemoval = true
    )
    private List<AiChatMessage> messages = new ArrayList<>();

    private AiChatRoom(Patient patient, String firstQuestion, LocalDateTime now) {
        this.patient = patient;
        this.title = createTitle(firstQuestion);
        this.lastMessageAt = now;
    }

    public static AiChatRoom create(Patient patient, String firstQuestion, LocalDateTime now) {
        return new AiChatRoom(patient, firstQuestion, now);
    }

    public AiChatMessage addUserMessage(String content, LocalDateTime sentAt) {
        return addMessage(ChatMessageRole.USER, content, sentAt);
    }

    public AiChatMessage addAssistantMessage(String content, LocalDateTime sentAt) {
        return addMessage(ChatMessageRole.ASSISTANT, content, sentAt);
    }

    public void rename(String title) {
        this.title = createTitle(title);
    }

    private AiChatMessage addMessage(ChatMessageRole role, String content, LocalDateTime sentAt) {
        AiChatMessage message = AiChatMessage.create(this, role, content, sentAt);
        messages.add(message);
        updateLastMessageAt(sentAt);
        return message;
    }

    private void updateLastMessageAt(LocalDateTime sentAt) {
        lastMessageAt = sentAt;
    }

    private static String createTitle(String content) {
        String normalized = normalize(content);
        if (normalized.isBlank()) {
            return DEFAULT_TITLE;
        }
        if (normalized.length() <= TITLE_MAX_LENGTH) {
            return normalized;
        }
        return normalized.substring(0, TITLE_MAX_LENGTH);
    }

    private static String normalize(String content) {
        if (content == null) {
            return "";
        }
        return content.trim().replaceAll("\\s+", " ");
    }
}
