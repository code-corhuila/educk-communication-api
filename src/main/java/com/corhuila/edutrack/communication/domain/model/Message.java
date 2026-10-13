package com.corhuila.edutrack.communication.domain.model;

import java.time.Instant;
import java.util.Objects;
import java.util.UUID;

import com.corhuila.edutrack.communication.domain.exception.InvalidMessageException;

/** Direct message between two users. Pure domain: no framework types. */
public final class Message {

    public static final int MAX_CONTENT_LENGTH = 2000;

    private final UUID id;
    private final UUID senderId;
    private final UUID receiverId;
    private final UUID subjectId;
    private final String content;
    private final Instant createdAt;

    public Message(UUID id, UUID senderId, UUID receiverId, UUID subjectId, String content, Instant createdAt) {
        if (id == null) {
            throw new InvalidMessageException("Message id is required.");
        }
        if (senderId == null || receiverId == null) {
            throw new InvalidMessageException("Sender and receiver are required.");
        }
        if (senderId.equals(receiverId)) {
            throw new InvalidMessageException("A user cannot send a message to themselves.");
        }
        if (content == null || content.isBlank()) {
            throw new InvalidMessageException("Message content cannot be empty.");
        }
        String trimmed = content.trim();
        if (trimmed.length() > MAX_CONTENT_LENGTH) {
            throw new InvalidMessageException("Message content exceeds " + MAX_CONTENT_LENGTH + " characters.");
        }
        this.id = id;
        this.senderId = senderId;
        this.receiverId = receiverId;
        this.subjectId = subjectId;
        this.content = trimmed;
        this.createdAt = Objects.requireNonNull(createdAt, "createdAt");
    }

    public UUID getId() { return id; }
    public UUID getSenderId() { return senderId; }
    public UUID getReceiverId() { return receiverId; }
    public UUID getSubjectId() { return subjectId; }
    public String getContent() { return content; }
    public Instant getCreatedAt() { return createdAt; }
}
