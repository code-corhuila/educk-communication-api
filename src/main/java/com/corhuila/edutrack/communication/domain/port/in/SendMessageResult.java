package com.corhuila.edutrack.communication.domain.port.in;

import com.corhuila.edutrack.communication.domain.model.Message;

public record SendMessageResult(Message message, boolean isNew) {
}
