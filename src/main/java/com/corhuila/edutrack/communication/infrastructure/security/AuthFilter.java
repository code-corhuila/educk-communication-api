package com.corhuila.edutrack.communication.infrastructure.security;

import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.http.MediaType;
import org.springframework.lang.NonNull;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.time.Instant;
import java.util.Map;
import java.util.Set;

/**
 * Validates the RS256 bearer token on every /api/ route except health endpoints.
 */
@Component
public class AuthFilter extends OncePerRequestFilter {

    public static final String PRINCIPAL_ATTRIBUTE = "auth.principal";
    public static final String SUBJECT_ATTRIBUTE = "auth.subject";

    private static final Set<String> PUBLIC_PATHS = Set.of(
            "/health",
            "/actuator/health",
            "/api/v1/messages/health"
    );

    private final Rs256Verifier verifier;
    private final ObjectMapper json;

    public AuthFilter(Rs256Verifier verifier, ObjectMapper json) {
        this.verifier = verifier;
        this.json = json;
    }

    @Override
    protected boolean shouldNotFilter(@NonNull HttpServletRequest request) {
        if ("OPTIONS".equalsIgnoreCase(request.getMethod())) {
            return true;
        }
        String path = request.getRequestURI();
        if (PUBLIC_PATHS.contains(path)) {
            return true;
        }
        return !path.startsWith("/api/");
    }

    @Override
    protected void doFilterInternal(@NonNull HttpServletRequest request, @NonNull HttpServletResponse response, @NonNull FilterChain chain)
            throws ServletException, IOException {
        String header = request.getHeader("Authorization");
        if (header == null || !header.startsWith("Bearer ") || header.length() <= 7) {
            reject(response, "a bearer token is required");
            return;
        }
        try {
            Rs256Verifier.VerifiedPrincipal principal = verifier.verify(header.substring(7).trim(), Instant.now());
            request.setAttribute(PRINCIPAL_ATTRIBUTE, principal);
            request.setAttribute(SUBJECT_ATTRIBUTE, principal.userId().toString());
        } catch (Rs256Verifier.InvalidTokenException e) {
            reject(response, "the token is invalid or has expired");
            return;
        }
        chain.doFilter(request, response);
    }

    private void reject(HttpServletResponse response, String message) throws IOException {
        response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        json.writeValue(response.getOutputStream(), Map.of(
                "error", "UNAUTHORIZED",
                "message", message,
                "timestamp", Instant.now().toString()
        ));
    }
}
