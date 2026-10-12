package com.corhuila.edutrack.communication.domain;

import static org.junit.jupiter.api.Assertions.*;

import java.time.Instant;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import com.corhuila.edutrack.communication.domain.exception.InvalidMessageException;
import com.corhuila.edutrack.communication.domain.model.Message;

class MessageTest {
    private final UUID s = UUID.randomUUID(), r = UUID.randomUUID();

    @Test
    void createsTrimmedValidMessage() {
        Message m = new Message(UUID.randomUUID(), s, r, null, "  Question  ", Instant.now());
        assertEquals("Question", m.getContent());
        assertEquals(s, m.getSenderId());
    }

    @Test
    void rejectsBlankContent() {
        assertThrows(InvalidMessageException.class, () -> new Message(UUID.randomUUID(), s, r, null, " ", Instant.now()));
    }

    @Test
    void rejectsSelfAddressedMessage() {
        assertThrows(InvalidMessageException.class, () -> new Message(UUID.randomUUID(), s, s, null, "Hi", Instant.now()));
    }

    @Test
    void rejectsContentOverLimit() {
        assertThrows(InvalidMessageException.class, () -> new Message(UUID.randomUUID(), s, r, null, "x".repeat(2001), Instant.now()));
    }
}
