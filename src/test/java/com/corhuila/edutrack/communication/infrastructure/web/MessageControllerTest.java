package com.corhuila.edutrack.communication.infrastructure.web;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

import java.time.Instant;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import com.corhuila.edutrack.communication.domain.model.Message;
import com.corhuila.edutrack.communication.domain.port.in.GetConversationUseCase;
import com.corhuila.edutrack.communication.domain.port.in.SendMessageResult;
import com.corhuila.edutrack.communication.domain.port.in.SendMessageUseCase;
import com.corhuila.edutrack.communication.infrastructure.web.dto.MessageResponse;
import com.corhuila.edutrack.communication.infrastructure.web.dto.SendMessageRequest;

class MessageControllerTest {
    private SendMessageUseCase sendUseCase;
    private GetConversationUseCase getUseCase;
    private MessageController controller;
    private final UUID s = UUID.randomUUID(), r = UUID.randomUUID();

    @BeforeEach
    void setUp() {
        sendUseCase = mock(SendMessageUseCase.class);
        getUseCase = mock(GetConversationUseCase.class);
        controller = new MessageController(sendUseCase, getUseCase);
    }

    @Test
    void sendsNewMessageReturnsCreated() {
        Message msg = new Message(UUID.randomUUID(), s, r, null, "Hello", Instant.now());
        when(sendUseCase.sendMessage(any(), any(), any(), any(), any())).thenReturn(new SendMessageResult(msg, true));
        ResponseEntity<MessageResponse> resp = controller.sendMessage(new SendMessageRequest(s, r, null, "Hello"), "k1");
        assertEquals(HttpStatus.CREATED, resp.getStatusCode());
    }

    @Test
    void replayReturnsOk() {
        Message msg = new Message(UUID.randomUUID(), s, r, null, "Hello", Instant.now());
        when(sendUseCase.sendMessage(any(), any(), any(), any(), any())).thenReturn(new SendMessageResult(msg, false));
        assertEquals(HttpStatus.OK, controller.sendMessage(new SendMessageRequest(s, r, null, "Hello"), "k1").getStatusCode());
    }

    @Test
    void retrievesConversationAndHealth() {
        Message msg = new Message(UUID.randomUUID(), s, r, null, "Hello", Instant.now());
        when(getUseCase.getConversation(s, r)).thenReturn(List.of(msg));
        assertEquals(1, controller.getConversation(s, r).getBody().size());
        assertEquals("UP", controller.health().getBody().get("status"));
    }
}
