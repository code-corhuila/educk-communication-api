package com.corhuila.edutrack.communication.domain.port.out;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import com.corhuila.edutrack.communication.domain.model.Message;

public interface MessageRepository {

    Optional<Message> findBySenderAndIdempotencyKey(UUID senderId, String idempotencyKey);

    /**
     * Atomically stores the message together with its MessageSent outbox event.
     * If another request already stored a message for the same sender and idempotency
     * key, the adapter returns that stored message instead of the given one.
     */
    Message save(Message message, String idempotencyKey);

    /** Messages exchanged between both users in either direction, oldest first. */
    List<Message> findConversation(UUID user1, UUID user2);
}
