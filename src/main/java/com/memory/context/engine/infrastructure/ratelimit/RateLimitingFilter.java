package com.memory.context.engine.infrastructure.ratelimit;

import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.time.Duration;
import java.time.Instant;
import java.util.Map;
import java.util.concurrent.TimeUnit;

/**
 * Rate limiting filter using Redis-based token bucket algorithm.
 * Limits requests per user based on X-User-Id header.
 */
@Slf4j
@Component
public class RateLimitingFilter extends OncePerRequestFilter {

    private static final String RATE_LIMIT_KEY_PREFIX = "rate-limit:";
    private static final int MAX_REQUESTS_PER_MINUTE = 60;
    private static final Duration WINDOW_DURATION = Duration.ofMinutes(1);

    private final RedisTemplate<String, Object> redisTemplate;
    private final ObjectMapper objectMapper;

    public RateLimitingFilter(RedisTemplate<String, Object> redisTemplate, ObjectMapper objectMapper) {
        this.redisTemplate = redisTemplate;
        this.objectMapper = objectMapper;
    }

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        String path = request.getRequestURI();
        // Don't rate limit health checks and static endpoints
        return path.equals("/")
                || path.startsWith("/actuator")
                || path.equals("/api/test");
    }

    @Override
    protected void doFilterInternal(
            HttpServletRequest request,
            HttpServletResponse response,
            FilterChain filterChain) throws ServletException, IOException {

        String userId = request.getHeader("X-User-Id");
        if (userId == null || userId.isBlank()) {
            // Let the security filter handle missing user ID
            filterChain.doFilter(request, response);
            return;
        }

        String key = RATE_LIMIT_KEY_PREFIX + userId;

        try {
            Long currentCount = redisTemplate.opsForValue().increment(key);

            if (currentCount == null) {
                currentCount = 1L;
            }

            // Set expiry on first request
            if (currentCount == 1) {
                redisTemplate.expire(key, WINDOW_DURATION.toSeconds(), TimeUnit.SECONDS);
            }

            // Add rate limit headers
            Long ttl = redisTemplate.getExpire(key, TimeUnit.SECONDS);
            response.setHeader("X-RateLimit-Limit", String.valueOf(MAX_REQUESTS_PER_MINUTE));
            response.setHeader("X-RateLimit-Remaining",
                    String.valueOf(Math.max(0, MAX_REQUESTS_PER_MINUTE - currentCount)));
            response.setHeader("X-RateLimit-Reset",
                    String.valueOf(Instant.now().plusSeconds(ttl != null ? ttl : 60).getEpochSecond()));

            if (currentCount > MAX_REQUESTS_PER_MINUTE) {
                log.warn("Rate limit exceeded for user: {}, count: {}", userId, currentCount);
                sendRateLimitResponse(response, ttl);
                return;
            }

            filterChain.doFilter(request, response);

        } catch (Exception e) {
            log.error("Rate limiting error for user: {}", userId, e);
            // On Redis failure, allow the request (fail open)
            filterChain.doFilter(request, response);
        }
    }

    private void sendRateLimitResponse(HttpServletResponse response, Long ttlSeconds) throws IOException {
        response.setStatus(HttpStatus.TOO_MANY_REQUESTS.value());
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);

        Map<String, Object> errorBody = Map.of(
                "code", "RATE_LIMIT_EXCEEDED",
                "message", "Too many requests. Please try again later.",
                "retryAfterSeconds", ttlSeconds != null ? ttlSeconds : 60);

        response.getWriter().write(objectMapper.writeValueAsString(errorBody));
    }
}
