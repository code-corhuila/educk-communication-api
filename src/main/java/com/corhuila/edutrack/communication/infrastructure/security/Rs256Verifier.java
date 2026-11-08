package com.corhuila.edutrack.communication.infrastructure.security;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.nio.charset.StandardCharsets;
import java.security.KeyFactory;
import java.security.Signature;
import java.security.interfaces.RSAPublicKey;
import java.security.spec.X509EncodedKeySpec;
import java.time.Instant;
import java.util.Base64;
import java.util.UUID;

/**
 * Verifies RS256 JWTs independently using the Identity public key.
 */
@Component
public class Rs256Verifier {

    public static final String EXPECTED_ISSUER = "educk-identity-api";
    public static final String EXPECTED_AUDIENCE = "edutrack";
    private static final long LEEWAY_SECONDS = 30L;
    private static final int MIN_RSA_BITS = 2048;

    public record VerifiedPrincipal(UUID userId, String role) {}

    public static class InvalidTokenException extends Exception {
        public InvalidTokenException(String message) {
            super(message);
        }
    }

    private final RSAPublicKey publicKey;
    private final ObjectMapper json = new ObjectMapper();

    public Rs256Verifier(@Value("${jwt.public-key:}") String publicKeyPem) {
        this.publicKey = parsePublicKey(publicKeyPem);
    }

    public VerifiedPrincipal verify(String token, Instant now) throws InvalidTokenException {
        if (token == null || token.isBlank()) {
            throw new InvalidTokenException("missing token");
        }
        String[] parts = token.split("\\.", -1);
        if (parts.length != 3 || parts[0].isEmpty() || parts[1].isEmpty() || parts[2].isEmpty()) {
            throw new InvalidTokenException("malformed token");
        }
        try {
            JsonNode header = json.readTree(Base64.getUrlDecoder().decode(parts[0]));
            if (!"RS256".equals(header.path("alg").asText())) {
                throw new InvalidTokenException("algorithm not allowed");
            }
            Signature rsa = Signature.getInstance("SHA256withRSA");
            rsa.initVerify(publicKey);
            rsa.update((parts[0] + "." + parts[1]).getBytes(StandardCharsets.US_ASCII));
            if (!rsa.verify(Base64.getUrlDecoder().decode(parts[2]))) {
                throw new InvalidTokenException("bad signature");
            }

            JsonNode claims = json.readTree(Base64.getUrlDecoder().decode(parts[1]));
            if (!claims.path("exp").isNumber() || !claims.path("sub").isTextual()) {
                throw new InvalidTokenException("exp and sub are required");
            }
            if (now.getEpochSecond() > claims.get("exp").asLong() + LEEWAY_SECONDS) {
                throw new InvalidTokenException("expired");
            }
            if (claims.path("nbf").isNumber() && now.getEpochSecond() + LEEWAY_SECONDS < claims.get("nbf").asLong()) {
                throw new InvalidTokenException("not yet valid");
            }
            if (!EXPECTED_ISSUER.equals(claims.path("iss").asText())) {
                throw new InvalidTokenException("invalid issuer");
            }
            if (!hasAudience(claims.path("aud"))) {
                throw new InvalidTokenException("invalid audience");
            }
            String role = claims.path("role").asText("");
            if (role.isBlank()) {
                throw new InvalidTokenException("role is required");
            }
            UUID userId = UUID.fromString(claims.get("sub").asText());
            return new VerifiedPrincipal(userId, role);
        } catch (InvalidTokenException e) {
            throw e;
        } catch (Exception e) {
            throw new InvalidTokenException("unreadable token");
        }
    }

    private static boolean hasAudience(JsonNode audNode) {
        if (audNode.isTextual()) {
            return EXPECTED_AUDIENCE.equals(audNode.asText());
        }
        if (audNode.isArray()) {
            for (JsonNode item : audNode) {
                if (EXPECTED_AUDIENCE.equals(item.asText())) {
                    return true;
                }
            }
        }
        return false;
    }

    private static RSAPublicKey parsePublicKey(String pem) {
        if (pem == null || pem.isBlank()) {
            throw new IllegalArgumentException("JWT_PUBLIC_KEY is required");
        }
        String normalized = pem.replace("\\r", "").replace("\\n", "\n").trim();
        if (!normalized.contains("BEGIN PUBLIC KEY")) {
            throw new IllegalArgumentException("JWT_PUBLIC_KEY must be an X.509 PEM public key");
        }
        String base64 = normalized.replaceAll("-----[A-Z0-9 ]+-----", "").replaceAll("\\s+", "");
        try {
            byte[] der = Base64.getDecoder().decode(base64);
            RSAPublicKey rsaKey = (RSAPublicKey) KeyFactory.getInstance("RSA")
                    .generatePublic(new X509EncodedKeySpec(der));
            if (rsaKey.getModulus().bitLength() < MIN_RSA_BITS) {
                throw new IllegalArgumentException("RSA public key must be at least 2048 bits");
            }
            return rsaKey;
        } catch (IllegalArgumentException e) {
            throw e;
        } catch (Exception e) {
            throw new IllegalArgumentException("JWT_PUBLIC_KEY cannot be parsed", e);
        }
    }
}
