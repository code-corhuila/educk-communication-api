package com.corhuila.edutrack.communication.infrastructure.persistence;

import java.time.Instant;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.CompoundIndex;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;

@Document(collection = "messages")
@CompoundIndex(name = "idx_sender_idempotency", def = "{'senderId': 1, 'idempotencyKey': 1}", unique = true, sparse = true)
public class MessageDocument {
    @Id
    private String id;
    @Indexed
    private String senderId;
    @Indexed
    private String receiverId;
    private String subjectId;
    private String content;
    private String idempotencyKey;
    private Instant createdAt;

    public MessageDocument() {}

    public MessageDocument(String id, String senderId, String receiverId, String subjectId, String content, String idempotencyKey, Instant createdAt) {
        this.id = id;
        this.senderId = senderId;
        this.receiverId = receiverId;
        this.subjectId = subjectId;
        this.content = content;
        this.idempotencyKey = idempotencyKey;
        this.createdAt = createdAt;
    }

    public String getId() { return id; }
    public String getSenderId() { return senderId; }
    public String getReceiverId() { return receiverId; }
    public String getSubjectId() { return subjectId; }
    public String getContent() { return content; }
    public String getIdempotencyKey() { return idempotencyKey; }
    public Instant getCreatedAt() { return createdAt; }
}
