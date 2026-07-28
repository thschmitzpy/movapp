package com.loja.movapp.security;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.loja.movapp.exception.ErroResponse;
import io.jsonwebtoken.ExpiredJwtException;
import io.jsonwebtoken.JwtException;
import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

@Component
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    private static final Logger log = LoggerFactory.getLogger(JwtAuthenticationFilter.class);

    private final JwtUtil jwtUtil;
    private final UserDetailsService userDetailsService;
    private final TokenBlacklist tokenBlacklist;
    private final ObjectMapper objectMapper;
    private final Counter authExpired;
    private final Counter authBlacklisted;
    private final Counter authInvalid;

    public JwtAuthenticationFilter(JwtUtil jwtUtil,
                                   UserDetailsService userDetailsService,
                                   TokenBlacklist tokenBlacklist,
                                   ObjectMapper objectMapper,
                                   MeterRegistry meterRegistry) {
        this.jwtUtil = jwtUtil;
        this.userDetailsService = userDetailsService;
        this.tokenBlacklist = tokenBlacklist;
        this.objectMapper = objectMapper;
        this.authExpired     = Counter.builder("jwt.auth.failures").tag("reason", "expired").register(meterRegistry);
        this.authBlacklisted = Counter.builder("jwt.auth.failures").tag("reason", "blacklisted").register(meterRegistry);
        this.authInvalid     = Counter.builder("jwt.auth.failures").tag("reason", "invalid").register(meterRegistry);
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain chain) throws ServletException, IOException {

        String authHeader = request.getHeader("Authorization");

        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            chain.doFilter(request, response);
            return;
        }

        String token = authHeader.substring(7);

        if (tokenBlacklist.isBlacklisted(token)) {
            authBlacklisted.increment();
            log.warn("Token blacklistado apresentado [{}]", request.getRequestURI());
            sendUnauthorized(request, response, "token_blacklisted", "Token revogado. Faça login novamente.");
            return;
        }

        String username;
        try {
            username = jwtUtil.extractUsername(token);
        } catch (ExpiredJwtException e) {
            authExpired.increment();
            log.warn("Token expirado [{}] sub={}", request.getRequestURI(), e.getClaims().getSubject());
            sendUnauthorized(request, response, "token_expired", "Token expirado. Faça login novamente.");
            return;
        } catch (JwtException e) {
            authInvalid.increment();
            log.warn("Token inválido [{}]: {}", request.getRequestURI(), e.getClass().getSimpleName());
            sendUnauthorized(request, response, "token_invalid", "Token inválido.");
            return;
        }

        if (username != null && SecurityContextHolder.getContext().getAuthentication() == null) {
            UserDetails userDetails = userDetailsService.loadUserByUsername(username);
            if (!jwtUtil.isValid(token, userDetails)) {
                authInvalid.increment();
                log.warn("Token com sujeito inconsistente [{}] username={}", request.getRequestURI(), username);
                sendUnauthorized(request, response, "token_invalid", "Token inválido.");
                return;
            }
            UsernamePasswordAuthenticationToken auth =
                    new UsernamePasswordAuthenticationToken(userDetails, null, userDetails.getAuthorities());
            auth.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));
            SecurityContextHolder.getContext().setAuthentication(auth);
            MDC.put(MdcLoggingFilter.MDC_USER_ID, username);
        }

        chain.doFilter(request, response);
    }

    private void sendUnauthorized(HttpServletRequest request,
                                  HttpServletResponse response,
                                  String code,
                                  String message) throws IOException {
        response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        response.setCharacterEncoding("UTF-8");
        response.setHeader("X-Auth-Error", code);           // frontend lê pra decidir ação
        objectMapper.writeValue(response.getWriter(),
                new ErroResponse(401, message, request.getRequestURI()));
    }
}