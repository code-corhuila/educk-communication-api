package com.corhuila.edutrack.communication.infrastructure.web;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import com.corhuila.edutrack.communication.domain.port.in.GetConversationUseCase;
import com.corhuila.edutrack.communication.domain.port.in.SendMessageResult;
import com.corhuila.edutrack.communication.domain.port.in.SendMessageUseCase;
import com.corhuila.edutrack.communication.infrastructure.security.AuthFilter;
import com.corhuila.edutrack.communication.infrastructure.security.Rs256Verifier;
import com.corhuila.edutrack.communication.infrastructure.web.dto.MessageResponse;
import com.corhuila.edutrack.communication.infrastructure.web.dto.SendMessageRequest;
import jakarta.validation.Valid;

@CrossOrigin(origins = "*")
@RestController
@RequestMapping("/api/v1/messages")
public class MessageController {

    private static final Set<String> ADMIN_ROLES = Set.of("ADMIN", "SUPER_ADMIN");

    private final SendMessageUseCase sendMessageUseCase;
    private final GetConversationUseCase getConversationUseCase;

    public MessageController(SendMessageUseCase sendMessageUseCase, GetConversationUseCase getConversationUseCase) {
        this.sendMessageUseCase = sendMessageUseCase;
        this.getConversationUseCase = getConversationUseCase;
    }

    @PostMapping
    public ResponseEntity<MessageResponse> sendMessage(
            @Valid @RequestBody SendMessageRequest request,
            @RequestHeader(value = "Idempotency-Key", required = false) String idempotencyKey,
            @RequestAttribute(name = AuthFilter.PRINCIPAL_ATTRIBUTE, required = false) Rs256Verifier.VerifiedPrincipal principal) {
        if (principal == null || principal.userId() == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }
        SendMessageResult result = sendMessageUseCase.sendMessage(
                principal.userId(),
                request.receiverId(),
                request.subjectId(),
                request.content(),
                idempotencyKey
        );
        HttpStatus status = result.isNew() ? HttpStatus.CREATED : HttpStatus.OK;
        return ResponseEntity.status(status).body(MessageResponse.fromDomain(result.message()));
    }

    @GetMapping("/conversation")
    public ResponseEntity<List<MessageResponse>> getConversation(
            @RequestParam UUID user1,
            @RequestParam UUID user2,
            @RequestAttribute(name = AuthFilter.PRINCIPAL_ATTRIBUTE, required = false) Rs256Verifier.VerifiedPrincipal principal) {
        if (principal == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }
        if (!ADMIN_ROLES.contains(principal.role())
                && !principal.userId().equals(user1) && !principal.userId().equals(user2)) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
        }
        List<MessageResponse> list = getConversationUseCase.getConversation(user1, user2)
                .stream()
                .map(MessageResponse::fromDomain)
                .toList();
        return ResponseEntity.ok(list);
    }

    @GetMapping("/health")
    public ResponseEntity<Map<String, String>> health() {
        return ResponseEntity.ok(Map.of(
                "status", "UP",
                "service", "communication-service",
                "timestamp", Instant.now().toString()
        ));
    }
}
