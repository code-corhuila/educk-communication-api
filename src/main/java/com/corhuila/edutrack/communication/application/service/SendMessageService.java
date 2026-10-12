package com.corhuila.edutrack.communication.application.service;

import java.time.Clock;
import java.util.Optional;
import java.util.UUID;

import com.corhuila.edutrack.communication.domain.exception.InvalidMessageException;
import com.corhuila.edutrack.communication.domain.model.Message;
import com.corhuila.edutrack.communication.domain.port.in.SendMessageResult;
import com.corhuila.edutrack.communication.domain.port.in.SendMessageUseCase;
import com.corhuila.edutrack.communication.domain.port.out.MessageRepository;

public class SendMessageService implements SendMessageUseCase {

    static final int MAX_IDEMPOTENCY_KEY_LENGTH = 64;

    private final MessageRepository repository;
    private final Clock clock;

    public SendMessageService(MessageRepository repository, Clock clock) {
        this.repository = repository;
        this.clock = clock;
    }

    @Override
    public SendMessageResult sendMessage(UUID senderId, UUID receiverId, UUID subjectId, String content, String idempotencyKey) {
        String key = normalizeKey(idempotencyKey);
        if (key != null && senderId != null) {
            Optional<Message> existing = repository.findBySenderAndIdempotencyKey(senderId, key);
            if (existing.isPresent()) {
                return new SendMessageResult(existing.get(), false);
            }
        }
        Message candidate = new Message(UUID.randomUUID(), senderId, receiverId, subjectId, content, clock.instant());
        Message stored = repository.save(candidate, key);
        return new SendMessageResult(stored, stored.getId().equals(candidate.getId()));
    }

    private static String normalizeKey(String idempotencyKey) {
        if (idempotencyKey == null || idempotencyKey.isBlank()) {
            return null;
        }
        String key = idempotencyKey.trim();
        if (key.length() > MAX_IDEMPOTENCY_KEY_LENGTH) {
            throw new InvalidMessageException("Idempotency-Key exceeds " + MAX_IDEMPOTENCY_KEY_LENGTH + " characters.");
        }
        return key;
    }
}
