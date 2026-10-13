package com.corhuila.edutrack.communication.infrastructure.persistence;

import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Repository;

import com.corhuila.edutrack.communication.domain.model.Message;
import com.corhuila.edutrack.communication.domain.port.out.MessageRepository;

@Repository
public class MongoMessageRepositoryAdapter implements MessageRepository {

    private final SpringDataMongoMessageRepository repository;

    public MongoMessageRepositoryAdapter(SpringDataMongoMessageRepository repository) {
        this.repository = repository;
    }

    @Override
    public Optional<Message> findBySenderAndIdempotencyKey(UUID senderId, String idempotencyKey) {
        if (senderId == null || idempotencyKey == null) return Optional.empty();
        return repository.findBySenderIdAndIdempotencyKey(senderId.toString(), idempotencyKey)
                .map(this::toDomain);
    }

    @Override
    public Message save(Message message, String idempotencyKey) {
        MessageDocument doc = new MessageDocument(
                message.getId().toString(),
                message.getSenderId().toString(),
                message.getReceiverId().toString(),
                message.getSubjectId() != null ? message.getSubjectId().toString() : null,
                message.getContent(),
                idempotencyKey,
                message.getCreatedAt()
        );
        try {
            MessageDocument saved = repository.save(doc);
            return toDomain(saved);
        } catch (DuplicateKeyException ex) {
            return findBySenderAndIdempotencyKey(message.getSenderId(), idempotencyKey)
                    .orElseThrow(() -> ex);
        }
    }

    @Override
    public List<Message> findConversation(UUID user1, UUID user2) {
        Sort sort = Sort.by(Sort.Direction.ASC, "createdAt");
        return repository.findConversation(user1.toString(), user2.toString(), sort)
                .stream()
                .map(this::toDomain)
                .toList();
    }

    private Message toDomain(MessageDocument doc) {
        return new Message(
                UUID.fromString(doc.getId()),
                UUID.fromString(doc.getSenderId()),
                UUID.fromString(doc.getReceiverId()),
                doc.getSubjectId() != null ? UUID.fromString(doc.getSubjectId()) : null,
                doc.getContent(),
                doc.getCreatedAt()
        );
    }
}
