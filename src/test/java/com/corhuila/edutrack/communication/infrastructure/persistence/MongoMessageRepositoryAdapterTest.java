package com.corhuila.edutrack.communication.infrastructure.persistence;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.dao.DuplicateKeyException;

import com.corhuila.edutrack.communication.domain.model.Message;

class MongoMessageRepositoryAdapterTest {
    private SpringDataMongoMessageRepository mongoRepo;
    private MongoMessageRepositoryAdapter adapter;
    private final UUID s = UUID.randomUUID(), r = UUID.randomUUID();

    @BeforeEach
    void setUp() {
        mongoRepo = mock(SpringDataMongoMessageRepository.class);
        adapter = new MongoMessageRepositoryAdapter(mongoRepo);
    }

    @Test
    void savesAndMapsMessage() {
        Message msg = new Message(UUID.randomUUID(), s, r, null, "Hello Mongo", Instant.now());
        MessageDocument doc = new MessageDocument(msg.getId().toString(), s.toString(), r.toString(), null, "Hello Mongo", "k1", msg.getCreatedAt());
        when(mongoRepo.save(any())).thenReturn(doc);
        assertEquals(msg.getId(), adapter.save(msg, "k1").getId());
    }

    @Test
    void handlesDuplicateKeyGracefully() {
        Message msg = new Message(UUID.randomUUID(), s, r, null, "Hello", Instant.now());
        MessageDocument existing = new MessageDocument(UUID.randomUUID().toString(), s.toString(), r.toString(), null, "Hello", "k1", Instant.now());
        when(mongoRepo.save(any())).thenThrow(new DuplicateKeyException("E11000"));
        when(mongoRepo.findBySenderIdAndIdempotencyKey(s.toString(), "k1")).thenReturn(Optional.of(existing));
        assertEquals(UUID.fromString(existing.getId()), adapter.save(msg, "k1").getId());
    }

    @Test
    void findsConversation() {
        MessageDocument d = new MessageDocument(UUID.randomUUID().toString(), s.toString(), r.toString(), null, "Hi", null, Instant.now());
        when(mongoRepo.findConversation(eq(s.toString()), eq(r.toString()), any())).thenReturn(List.of(d));
        assertEquals(1, adapter.findConversation(s, r).size());
    }
}
