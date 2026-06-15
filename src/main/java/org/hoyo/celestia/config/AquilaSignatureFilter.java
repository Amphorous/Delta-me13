package org.hoyo.celestia.config;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.Base64;

// Verifies that the Aquila-User-Key header (and the binding checks that rely on it) was set by
// the Aquila gateway and not forged by a client calling this service directly. Requests without
// an Aquila-User-Key header are passed through untouched - that header is only required by
// endpoints that opt into @RequestHeader("Aquila-User-Key").
@Slf4j
@Component
public class AquilaSignatureFilter extends OncePerRequestFilter {

    private static final String USER_KEY_HEADER = "Aquila-User-Key";
    private static final String TIMESTAMP_HEADER = "Aquila-Request-Timestamp";
    private static final String SIGNATURE_HEADER = "Aquila-Signature";
    private static final long MAX_SKEW_MILLIS = 60_000L;

    private final SecretKeySpec secretKeySpec;

    public AquilaSignatureFilter(@Value("${application.security.gateway-signing-secret}") String secret) {
        this.secretKeySpec = new SecretKeySpec(secret.getBytes(StandardCharsets.UTF_8), "HmacSHA256");
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, java.io.IOException {

        String userKey = request.getHeader(USER_KEY_HEADER);
        if (userKey == null) {
            filterChain.doFilter(request, response);
            return;
        }

        String timestamp = request.getHeader(TIMESTAMP_HEADER);
        String signature = request.getHeader(SIGNATURE_HEADER);

        if (timestamp == null || signature == null || !isValid(userKey, timestamp, signature)) {
            log.warn("Rejected request with invalid Aquila signature for path {}", request.getRequestURI());
            response.sendError(HttpServletResponse.SC_FORBIDDEN);
            return;
        }

        filterChain.doFilter(request, response);
    }

    private boolean isValid(String userKey, String timestamp, String signature) {
        long requestTime;
        try {
            requestTime = Long.parseLong(timestamp);
        } catch (NumberFormatException e) {
            return false;
        }

        if (Math.abs(System.currentTimeMillis() - requestTime) > MAX_SKEW_MILLIS) {
            return false;
        }

        String expectedSignature = sign(userKey, timestamp);
        return MessageDigest.isEqual(
                expectedSignature.getBytes(StandardCharsets.UTF_8),
                signature.getBytes(StandardCharsets.UTF_8)
        );
    }

    private String sign(String userKey, String timestamp) {
        try {
            Mac mac = Mac.getInstance("HmacSHA256");
            mac.init(secretKeySpec);
            byte[] result = mac.doFinal((userKey + ":" + timestamp).getBytes(StandardCharsets.UTF_8));
            return Base64.getEncoder().encodeToString(result);
        } catch (Exception e) {
            throw new RuntimeException("Failed verifying gateway signature", e);
        }
    }
}
