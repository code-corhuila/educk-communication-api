package com.corhuila.edutrack.communication.application.service;

import java.util.List;
import java.util.UUID;

import com.corhuila.edutrack.communication.domain.exception.InvalidMessageException;
import com.corhuila.edutrack.communication.domain.model.Message;
import com.corhuila.edutrack.communication.domain.port.in.GetConversationUseCase;
import com.corhuila.edutrack.communication.domain.port.out.MessageRepository;

public class GetConversationService implements GetConversationUseCase {

    private final MessageRepository repository;

    public GetConversationService(MessageRepository repository) {
        this.repository = repository;
    }

    @Override
    public List<Message> getConversation(UUID user1, UUID user2) {
        if (user1 == null || user2 == null) {
            throw new InvalidMessageException("Both conversation participants are required.");
        }
        return repository.findConversation(user1, user2);
    }
}
