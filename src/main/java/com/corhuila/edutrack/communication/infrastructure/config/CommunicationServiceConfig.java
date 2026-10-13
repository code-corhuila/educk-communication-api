package com.corhuila.edutrack.communication.infrastructure.config;

import java.time.Clock;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import com.corhuila.edutrack.communication.application.service.*;
import com.corhuila.edutrack.communication.domain.port.in.*;
import com.corhuila.edutrack.communication.domain.port.out.MessageRepository;

@Configuration
public class CommunicationServiceConfig {
    @Bean
    public Clock clock() { return Clock.systemUTC(); }

    @Bean
    public SendMessageUseCase sendMessageUseCase(MessageRepository r, Clock c) { return new SendMessageService(r, c); }

    @Bean
    public GetConversationUseCase getConversationUseCase(MessageRepository r) { return new GetConversationService(r); }
}
