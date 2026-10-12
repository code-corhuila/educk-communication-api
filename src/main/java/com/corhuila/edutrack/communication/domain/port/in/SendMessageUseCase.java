package com.corhuila.edutrack.communication.domain.port.in;

import java.util.UUID;

public interface SendMessageUseCase {
    /**
     * Sends a message. A repeated idempotency key from the same sender returns the
     * originally stored message with {@code isNew = false} instead of creating a duplicate.
     */
    SendMessageResult sendMessage(UUID senderId, UUID receiverId, UUID subjectId, String content, String idempotencyKey);
}
