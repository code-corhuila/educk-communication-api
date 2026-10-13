package com.corhuila.edutrack.communication.infrastructure.persistence;

import java.util.List;
import java.util.Optional;
import org.springframework.data.domain.Sort;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.data.mongodb.repository.Query;

public interface SpringDataMongoMessageRepository extends MongoRepository<MessageDocument, String> {
    Optional<MessageDocument> findBySenderIdAndIdempotencyKey(String senderId, String idempotencyKey);

    @Query("{$or: [{'senderId': ?0, 'receiverId': ?1}, {'senderId': ?1, 'receiverId': ?0}]}")
    List<MessageDocument> findConversation(String user1, String user2, Sort sort);
}
