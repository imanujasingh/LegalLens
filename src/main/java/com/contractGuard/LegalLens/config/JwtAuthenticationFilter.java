package com.contractGuard.LegalLens.config;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.filter.OncePerRequestFilter;

import javax.crypto.SecretKey;
import java.io.IOException;
import java.util.List;

/**
 * JWT Authentication Filter.
 *
 * Reads the Authorization: Bearer <token> header on every request.
 * Validates the token signature and expiry using HMAC-SHA256.
 * Sets the authenticated principal in SecurityContext if valid.
 * Passes through silently if no token is present (public route handling
 * is delegated to SecurityConfig).
 *
 * Token claims expected:
 *  - sub  : user email / identifier
 *  - roles: comma-separated role list (e.g. "ROLE_USER,ROLE_ADMIN")
 */
@Component
@Slf4j
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    private final SecretKey signingKey;

    public JwtAuthenticationFilter(@Value("${legal-lens.jwt.secret:default-dev-secret-change-in-production}") String secret) {
        // In production: inject a proper 256-bit key from Vault / Secret Manager
        this.signingKey = io.jsonwebtoken.security.Keys.hmacShaKeyFor(
                secret.getBytes(java.nio.charset.StandardCharsets.UTF_8)
        );
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain filterChain) throws ServletException, IOException {

        String token = extractToken(request);

        if (token != null) {
            try {
                Claims claims = Jwts.parser()
                        .verifyWith(signingKey)
                        .build()
                        .parseSignedClaims(token)
                        .getPayload();

                String subject = claims.getSubject();
                String rolesRaw = claims.get("roles", String.class);
                List<SimpleGrantedAuthority> authorities = parseRoles(rolesRaw);

                UsernamePasswordAuthenticationToken authentication =
                        new UsernamePasswordAuthenticationToken(subject, null, authorities);

                SecurityContextHolder.getContext().setAuthentication(authentication);
                log.debug("Authenticated user '{}' via JWT", subject);

            } catch (JwtException ex) {
                log.warn("Invalid JWT token: {}", ex.getMessage());
                // Do not set authentication — SecurityConfig decides if route is public
            }
        }

        filterChain.doFilter(request, response);
    }

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        // Skip JWT parsing for multipart uploads — reading headers on a multipart
        // request can consume the input stream before Spring's multipart resolver runs.
        String contentType = request.getContentType();
        return contentType != null && contentType.toLowerCase().startsWith("multipart/");
    }

    private String extractToken(HttpServletRequest request) {
        String header = request.getHeader("Authorization");
        if (StringUtils.hasText(header) && header.startsWith("Bearer ")) {
            return header.substring(7);
        }
        return null;
    }

    private List<SimpleGrantedAuthority> parseRoles(String rolesRaw) {
        if (!StringUtils.hasText(rolesRaw)) {
            return List.of(new SimpleGrantedAuthority("ROLE_USER"));
        }
        return java.util.Arrays.stream(rolesRaw.split(","))
                .map(String::trim)
                .filter(StringUtils::hasText)
                .map(SimpleGrantedAuthority::new)
                .toList();
    }
}
