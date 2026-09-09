package com.loja.movapp.security;

import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import io.github.bucket4j.Bandwidth;
import io.github.bucket4j.Bucket;
import io.github.bucket4j.Refill;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.time.Duration;
import java.util.Set;

@Component
@Order(1)
public class LoginRateLimitFilter extends OncePerRequestFilter {

    private static final Logger log = LoggerFactory.getLogger(LoginRateLimitFilter.class);

    private static final int MAX_TENTATIVAS = 20;
    private static final Duration JANELA = Duration.ofMinutes(1);

    private static final Set<String> URIS_LIMITADAS = Set.of(
            "/auth/login",
            "/auth/forgot-password",
            "/auth/reset-password"
    );


    private final Cache<String, Bucket> buckets = Caffeine.newBuilder()
            .expireAfterAccess(Duration.ofMinutes(10))
            .maximumSize(10_000)
            .build();

    private Bucket criarBucket() {
        return Bucket.builder()
                .addLimit(Bandwidth.classic(MAX_TENTATIVAS, Refill.intervally(MAX_TENTATIVAS, JANELA)))
                .build();
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain chain) throws ServletException, IOException {

        if (!URIS_LIMITADAS.contains(request.getRequestURI()) || !"POST".equals(request.getMethod())) {
            chain.doFilter(request, response);
            return;
        }

        String ip = extrairIpCliente(request);
        Bucket bucket = buckets.get(ip + "|" + request.getRequestURI(), k -> criarBucket());

        if (bucket.tryConsume(1)) {
            chain.doFilter(request, response);
        } else {
            log.warn("Rate limit excedido: ip={}, uri={}", ip, request.getRequestURI());
            response.setStatus(429);
            response.setContentType("application/json;charset=UTF-8");
            response.getWriter().write(
                    "{\"status\":429,\"mensagem\":\"Muitas tentativas. Tente novamente em 1 minuto.\",\"caminho\":\"" + request.getRequestURI() + "\"}"
            );
        }
    }

    private String extrairIpCliente(HttpServletRequest request) {
        String remote = request.getRemoteAddr();
        return (remote != null && !remote.isBlank()) ? remote : "desconhecido";
    }
}
