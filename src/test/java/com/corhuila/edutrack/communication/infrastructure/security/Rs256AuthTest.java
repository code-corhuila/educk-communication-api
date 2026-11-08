package com.corhuila.edutrack.communication.infrastructure.security;

import com.corhuila.edutrack.communication.domain.model.Message;
import com.corhuila.edutrack.communication.domain.port.in.GetConversationUseCase;
import com.corhuila.edutrack.communication.domain.port.in.SendMessageUseCase;
import com.corhuila.edutrack.communication.infrastructure.web.MessageController;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.PrivateKey;
import java.security.Signature;
import java.time.Instant;
import java.util.Base64;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class Rs256AuthTest {

    private static KeyPair kp;
    private static Rs256Verifier verifier;
    private static MockMvc mockMvc;
    private static final UUID USER_ID = UUID.randomUUID();
    private static final UUID OTHER_ID = UUID.randomUUID();

    @BeforeAll
    static void setUp() throws Exception {
        KeyPairGenerator kpg = KeyPairGenerator.getInstance("RSA");
        kpg.initialize(2048);
        kp = kpg.generateKeyPair();
        String pem = "-----BEGIN PUBLIC KEY-----\n"
                + Base64.getMimeEncoder(64, "\n".getBytes(StandardCharsets.US_ASCII)).encodeToString(kp.getPublic().getEncoded())
                + "\n-----END PUBLIC KEY-----";
        verifier = new Rs256Verifier(pem);

        SendMessageUseCase sendUseCase = mock(SendMessageUseCase.class);
        GetConversationUseCase getUseCase = mock(GetConversationUseCase.class);
        when(getUseCase.getConversation(USER_ID, OTHER_ID)).thenReturn(
                List.of(new Message(UUID.randomUUID(), USER_ID, OTHER_ID, null, "ok", Instant.now())));
        mockMvc = MockMvcBuilders.standaloneSetup(new MessageController(sendUseCase, getUseCase))
                .addFilters(new AuthFilter(verifier, new ObjectMapper()))
                .build();
    }

    private static String signRs256(String headerJson, String payloadJson, PrivateKey key) throws Exception {
        var enc = Base64.getUrlEncoder().withoutPadding();
        String signingInput = enc.encodeToString(headerJson.getBytes(StandardCharsets.UTF_8)) + "."
                + enc.encodeToString(payloadJson.getBytes(StandardCharsets.UTF_8));
        Signature rsa = Signature.getInstance("SHA256withRSA");
        rsa.initSign(key);
        rsa.update(signingInput.getBytes(StandardCharsets.US_ASCII));
        return signingInput + "." + enc.encodeToString(rsa.sign());
    }

    @Test
    void enforcesRs256AuthAndRejectsMissingOrTamperedTokens() throws Exception {
        long future = Instant.now().plusSeconds(300).getEpochSecond();
        String valid = signRs256("{\"alg\":\"RS256\",\"typ\":\"JWT\"}",
                "{\"iss\":\"educk-identity-api\",\"aud\":\"edutrack\",\"sub\":\"" + USER_ID + "\",\"role\":\"PARENT\",\"exp\":" + future + "}",
                kp.getPrivate());

        mockMvc.perform(get("/api/v1/messages/conversation")
                        .param("user1", USER_ID.toString()).param("user2", OTHER_ID.toString())
                        .header("Authorization", "Bearer " + valid))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].content").value("ok"));

        mockMvc.perform(get("/api/v1/messages/conversation")
                        .param("user1", USER_ID.toString()).param("user2", OTHER_ID.toString()))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.error").value("UNAUTHORIZED"));

        // HS256 rejected
        var enc = Base64.getUrlEncoder().withoutPadding();
        String hsInput = enc.encodeToString("{\"alg\":\"HS256\"}".getBytes(StandardCharsets.UTF_8)) + "."
                + enc.encodeToString(("{\"iss\":\"educk-identity-api\",\"aud\":\"edutrack\",\"sub\":\"" + USER_ID + "\",\"role\":\"PARENT\",\"exp\":" + future + "}").getBytes(StandardCharsets.UTF_8));
        Mac mac = Mac.getInstance("HmacSHA256");
        mac.init(new SecretKeySpec("01234567890123456789012345678901".getBytes(StandardCharsets.UTF_8), "HmacSHA256"));
        String hs = hsInput + "." + enc.encodeToString(mac.doFinal(hsInput.getBytes(StandardCharsets.US_ASCII)));
        assertThrows(Rs256Verifier.InvalidTokenException.class, () -> verifier.verify(hs, Instant.now()));
        mockMvc.perform(get("/api/v1/messages/conversation")
                        .param("user1", USER_ID.toString()).param("user2", OTHER_ID.toString())
                        .header("Authorization", "Bearer " + hs))
                .andExpect(status().isUnauthorized());

        // alg=none rejected
        String none = enc.encodeToString("{\"alg\":\"none\"}".getBytes(StandardCharsets.UTF_8)) + "."
                + enc.encodeToString(("{\"iss\":\"educk-identity-api\",\"aud\":\"edutrack\",\"sub\":\"" + USER_ID + "\",\"role\":\"PARENT\",\"exp\":" + future + "}").getBytes(StandardCharsets.UTF_8)) + ".sig";
        assertThrows(Rs256Verifier.InvalidTokenException.class, () -> verifier.verify(none, Instant.now()));

        // expired rejected
        long past = Instant.now().minusSeconds(120).getEpochSecond();
        String expired = signRs256("{\"alg\":\"RS256\"}",
                "{\"iss\":\"educk-identity-api\",\"aud\":\"edutrack\",\"sub\":\"" + USER_ID + "\",\"role\":\"PARENT\",\"exp\":" + past + "}",
                kp.getPrivate());
        assertThrows(Rs256Verifier.InvalidTokenException.class, () -> verifier.verify(expired, Instant.now()));

        // wrong audience rejected
        String wrongAud = signRs256("{\"alg\":\"RS256\"}",
                "{\"iss\":\"educk-identity-api\",\"aud\":\"other\",\"sub\":\"" + USER_ID + "\",\"role\":\"PARENT\",\"exp\":" + future + "}",
                kp.getPrivate());
        assertThrows(Rs256Verifier.InvalidTokenException.class, () -> verifier.verify(wrongAud, Instant.now()));

        // health stays public
        mockMvc.perform(get("/api/v1/messages/health")).andExpect(status().isOk());
    }
}
