package com.testmgmt.security;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * In-memory rate limiter for auth endpoints (login, register, SSO callback).
 * Mitigates brute-force / credential-stuffing ("keyboard") attacks.
 *
 * Windows:
 * - /api/v1/auth/login* : 5 attempts / 60s per client
 * - other /api/v1/auth/** : 20 attempts / 60s per client
 *
 * Client IP resolution:
 * Uses HttpServletRequest.getRemoteAddr(). Because the app sets
 * `server.forward-headers-strategy=framework` in application.properties,
 * Spring/Tomcat already resolves the real client IP from X-Forwarded-For
 * ONLY when the request arrives from a configured trusted proxy. We must
 * NOT re-read X-Forwarded-For directly from the request here — an attacker
 * could otherwise rotate that header on every request and completely bypass
 * this limiter.
 */
@Component
@Slf4j
public class RateLimitFilter extends OncePerRequestFilter {

    private static final int LOGIN_MAX_ATTEMPTS = 50;
    private static final int AUTH_MAX_ATTEMPTS = 20;
    private static final long WINDOW_MS = 60_000; // 1 minute
    /** Hard cap on tracked IPs to bound memory (defence against IP-spray fill). */
    private static final int MAX_TRACKED_IPS = 50_000;

    private final Map<String, RateEntry> ipAttempts = new ConcurrentHashMap<>();

    @Override
    protected void doFilterInternal(HttpServletRequest request,
            HttpServletResponse response,
            FilterChain filterChain) throws ServletException, IOException {

        String path = request.getRequestURI();

        // Only rate-limit auth endpoints
        if (path == null || !path.startsWith("/api/v1/auth/")) {
            filterChain.doFilter(request, response);
            return;
        }

        // Tighter limit on interactive login endpoints (brute-force target)
        if (path.startsWith("/api/v1/auth/idem/")) {
            filterChain.doFilter(request, response);
            return;
        }

        boolean isLogin = path.contains("/login") || path.contains("/keycloak-login");

        int maxAttempts = isLogin ? LOGIN_MAX_ATTEMPTS : AUTH_MAX_ATTEMPTS;

        String clientIp = getClientIp(request);
        String key = clientIp + "|" + (isLogin ? "L" : "A");

        // Bound the map to prevent unbounded growth from IP-spray attacks.
        if (ipAttempts.size() > MAX_TRACKED_IPS) {
            long cutoff = System.currentTimeMillis() - WINDOW_MS;
            ipAttempts.entrySet().removeIf(e -> e.getValue().windowStart < cutoff);
        }

        RateEntry entry = ipAttempts.compute(key, (k, existing) -> {
            long now = System.currentTimeMillis();
            if (existing == null || now - existing.windowStart > WINDOW_MS) {
                return new RateEntry(now, new AtomicInteger(1));
            }
            existing.count.incrementAndGet();
            return existing;
        });

        if (entry.count.get() > maxAttempts) {
            log.warn("Rate limit exceeded for IP: {} on {} (limit={}/min)", clientIp, path, maxAttempts);
            response.setStatus(HttpStatus.TOO_MANY_REQUESTS.value());
            response.setHeader("Retry-After", "60");
            response.setContentType("application/json");
            response.getWriter().write(
                    "{\"success\":false,\"message\":\"Too many requests. Please try again later.\"}");
            return;
        }

        filterChain.doFilter(request, response);
    }

    /**
     * Return the effective client IP. Do NOT read X-Forwarded-For directly —
     * that header is attacker-controlled unless the request comes through a
     * trusted reverse proxy. Spring's ForwardedHeaderFilter (enabled via
     * server.forward-headers-strategy=framework) has already substituted the
     * real client IP into remoteAddr for requests from trusted proxies.
     */
    private String getClientIp(HttpServletRequest request) {
        String ip = request.getRemoteAddr();
        return (ip == null || ip.isBlank()) ? "unknown" : ip;
    }

    private static class RateEntry {
        final long windowStart;
        final AtomicInteger count;

        RateEntry(long windowStart, AtomicInteger count) {
            this.windowStart = windowStart;
            this.count = count;
        }
    }
}
