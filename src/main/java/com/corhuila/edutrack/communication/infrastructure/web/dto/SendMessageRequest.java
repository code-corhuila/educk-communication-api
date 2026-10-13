package com.corhuila.edutrack.communication.infrastructure.web.dto;

import java.util.UUID;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record SendMessageRequest(
    @NotNull UUID senderId,
    @NotNull UUID receiverId,
    UUID subjectId,
    @NotBlank @Size(max = 2000) String content
) {}
