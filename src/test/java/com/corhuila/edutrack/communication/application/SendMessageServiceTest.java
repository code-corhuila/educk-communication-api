package com.corhuila.edutrack.communication.application;

import static org.junit.jupiter.api.Assertions.*;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.*;

import org.junit.jupiter.api.Test;
import com.corhuila.edutrack.communication.application.service.SendMessageService;
import com.corhuila.edutrack.communication.domain.exception.InvalidMessageException;
import com.corhuila.edutrack.communication.domain.model.Message;
import com.corhuila.edutrack.communication.domain.port.in.SendMessageResult;
import com.corhuila.edutrack.communication.domain.port.out.MessageRepository;

class SendMessageServiceTest {

    static class InMemoryRepo implements MessageRepository {
        final List<Message> list = new ArrayList<>();
        final Map<String, Message> map = new HashMap<>();
        boolean racing;

        public Optional<Message> findBySenderAndIdempotencyKey(UUID s, String k) {
            return racing ? Optional.empty() : Optional.ofNullable(map.get(s + ":" + k));
        }

        public Message save(Message m, String k) {
            if (k != null) {
                Message prev = map.putIfAbsent(m.getSenderId() + ":" + k, m);
                if (prev != null) return prev;
            }
            list.add(m);
            return m;
        }

        public List<Message> findConversation(UUID u1, UUID u2) { return list; }
    }

    private final InMemoryRepo repo = new InMemoryRepo();
    private final SendMessageService svc =
            new SendMessageService(repo, Clock.fixed(Instant.parse("2026-10-12T12:00:00Z"), ZoneOffset.UTC));
    private final UUID p = UUID.randomUUID(), t = UUID.randomUUID();

    @Test
    void storesNewMessage() {
        assertTrue(svc.sendMessage(p, t, null, "Hello", "k1").isNew());
        assertEquals(1, repo.list.size());
    }

    @Test
    void replayReturnsOriginalWithoutDuplicate() {
        SendMessageResult r1 = svc.sendMessage(p, t, null, "Hello", "k1");
        SendMessageResult r2 = svc.sendMessage(p, t, null, "Hello", "k1");
        assertFalse(r2.isNew());
        assertEquals(r1.message().getId(), r2.message().getId());
        assertEquals(1, repo.list.size());
    }

    @Test
    void concurrentDuplicateIsNotNew() {
        SendMessageResult r1 = svc.sendMessage(p, t, null, "Hello", "k1");
        repo.racing = true;
        SendMessageResult racing = svc.sendMessage(p, t, null, "Hello", "k1");
        assertFalse(racing.isNew());
        assertEquals(r1.message().getId(), racing.message().getId());
    }

    @Test
    void differentSenderCreatesSeparateMessage() {
        svc.sendMessage(p, t, null, "Hello", "k1");
        assertTrue(svc.sendMessage(t, p, null, "Hi", "k1").isNew());
        assertEquals(2, repo.list.size());
    }

    @Test
    void blankKeyDisablesDeduplication() {
        svc.sendMessage(p, t, null, "Hello", " ");
        svc.sendMessage(p, t, null, "Hello", null);
        assertEquals(2, repo.list.size());
    }

    @Test
    void rejectsOversizedKey() {
        assertThrows(InvalidMessageException.class, () -> svc.sendMessage(p, t, null, "H", "k".repeat(65)));
    }
}
