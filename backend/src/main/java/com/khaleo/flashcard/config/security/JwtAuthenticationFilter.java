package com.khaleo.flashcard.config.security;

import com.khaleo.flashcard.service.auth.JwtTokenService;
import com.khaleo.flashcard.repository.UserRepository;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jws;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.lang.NonNull;
import org.springframework.web.filter.OncePerRequestFilter;

@Slf4j
@Component
@RequiredArgsConstructor
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    private final JwtTokenService jwtTokenService;
    private final UserRepository userRepository;

    @Override
    protected void doFilterInternal(
            @NonNull HttpServletRequest request,
            @NonNull HttpServletResponse response,
            @NonNull FilterChain filterChain) throws ServletException, IOException {

        String authHeader = request.getHeader("Authorization");
        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            filterChain.doFilter(request, response);
            return;
        }

        String token = authHeader.substring("Bearer ".length());
        try {
            Jws<Claims> claims = jwtTokenService.parseAndValidate(token);
            String subject = claims.getPayload().getSubject();

            if (subject != null && SecurityContextHolder.getContext().getAuthentication() == null) {
                UUID userId = UUID.fromString(subject);
                var user = userRepository.findById(userId).orElse(null);
                if (user == null) {
                    filterChain.doFilter(request, response);
                    return;
                }

                if (user.getBannedAt() != null && user.getBannedAt().isBefore(Instant.now().plusSeconds(1))) {
                    log.info("event=auth_banned_request_denied userId={} path={}", subject, request.getRequestURI());
                    response.setStatus(HttpServletResponse.SC_FORBIDDEN);
                    response.setContentType("application/json");
                    response.getWriter().write(
                            "{\"timestamp\":\"" + Instant.now() +
                                    "\",\"status\":403,\"error\":\"BANNED_USER_REQUEST_DENIED\",\"message\":\"Banned account access denied.\",\"path\":\""
                                    + request.getRequestURI() + "\"}");
                    return;
                }

                UsernamePasswordAuthenticationToken authentication =
                        new UsernamePasswordAuthenticationToken(
                                subject,
                                null,
                                List.of(new SimpleGrantedAuthority(user.getRole().name())));
                authentication.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));
                SecurityContextHolder.getContext().setAuthentication(authentication);
            }
        } catch (RuntimeException ex) {
            log.debug("event=jwt_parse_failed reason={}", ex.getMessage());
        }

        filterChain.doFilter(request, response);
    }
}
