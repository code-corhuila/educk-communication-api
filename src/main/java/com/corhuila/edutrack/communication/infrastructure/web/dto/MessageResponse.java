package com.corhuila.edutrack.communication.infrastructure.web.dto;

import java.time.Instant;
import java.util.UUID;
import com.corhuila.edutrack.communication.domain.model.Message;

public record MessageResponse(
    UUID id,
    UUID senderId,
    UUID receiverId,
    UUID subjectId,
    String content,
    Instant createdAt
) {
    public static MessageResponse fromDomain(Message m) {
        return new MessageResponse(m.getId(), m.getSenderId(), m.getReceiverId(), m.getSubjectId(), m.getContent(), m.getCreatedAt());
    }
}
