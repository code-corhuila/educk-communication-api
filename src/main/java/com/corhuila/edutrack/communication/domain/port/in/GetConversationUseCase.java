package com.corhuila.edutrack.communication.domain.port.in;

import java.util.List;
import java.util.UUID;

import com.corhuila.edutrack.communication.domain.model.Message;

public interface GetConversationUseCase {
    List<Message> getConversation(UUID user1, UUID user2);
}
