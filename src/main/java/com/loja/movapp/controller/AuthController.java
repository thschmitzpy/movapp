package com.loja.movapp.controller;

import com.loja.movapp.dto.LoginRequestDTO;
import com.loja.movapp.dto.LoginResponseDTO;
import com.loja.movapp.exception.ErroResponse;
import com.loja.movapp.security.JwtUtil;
import com.loja.movapp.security.TokenBlacklist;
import io.micrometer.core.instrument.MeterRegistry;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import io.jsonwebtoken.ExpiredJwtException;
import io.jsonwebtoken.JwtException;
import java.util.Map;
import org.springframework.security.authentication.AccountExpiredException;
import org.springframework.security.authentication.CredentialsExpiredException;
import org.springframework.security.authentication.DisabledException;
import org.springframework.security.authentication.LockedException;
import org.springframework.security.core.AuthenticationException;

@RestController
@RequestMapping("/auth")
@Tag(name = "Autenticação", description = "Endpoint de login para obtenção do token JWT")
public class AuthController {

    private static final Logger log = LoggerFactory.getLogger(AuthController.class);

    @Autowired
    private AuthenticationManager authenticationManager;

    @Autowired
    private JwtUtil jwtUtil;

    @Autowired
    private TokenBlacklist tokenBlacklist;

    @Autowired
    private MeterRegistry meterRegistry;

    @PostMapping("/login")
    @Operation(summary = "Login", description = "Retorna um token JWT válido por 24h")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Login realizado, token JWT retornado"),
            @ApiResponse(responseCode = "400", description = "Dados inválidos (usuário ou senha em branco)"),
            @ApiResponse(responseCode = "401", description = "Credenciais incorretas")
    })
    public ResponseEntity<?> login(@Valid @RequestBody LoginRequestDTO dto) {
        try {
            Authentication auth = authenticationManager.authenticate(
                    new UsernamePasswordAuthenticationToken(dto.getUsername(), dto.getPassword())
            );
            UserDetails userDetails = (UserDetails) auth.getPrincipal();
            String role = userDetails.getAuthorities().stream()
                    .map(GrantedAuthority::getAuthority)
                    .findFirst()
                    .orElseThrow(() -> new BadCredentialsException("Usuário sem permissões atribuídas"));
            String token = jwtUtil.generateToken(userDetails);

            meterRegistry.counter("login.sucesso.total", "role", role).increment();
            log.info("Login realizado: username={}, role={}", userDetails.getUsername(), role);
            return ResponseEntity.ok(new LoginResponseDTO(token, userDetails.getUsername(), role));
        } catch (AuthenticationException e) {
            String motivo;
            if      (e instanceof BadCredentialsException)       motivo = "credenciais_invalidas";
            else if (e instanceof DisabledException)             motivo = "conta_desabilitada";
            else if (e instanceof LockedException)               motivo = "conta_bloqueada";
            else if (e instanceof AccountExpiredException)       motivo = "conta_expirada";
            else if (e instanceof CredentialsExpiredException)   motivo = "senha_expirada";
            else                                                 motivo = "outro";

            meterRegistry.counter("login.falhas.total", "motivo", motivo).increment();
            log.warn("Tentativa de login falhou: username={}, motivo={}", dto.getUsername(), motivo);
            return ResponseEntity.status(401)
                    .body(new ErroResponse(401, "Credenciais inválidas", "/auth/login"));
        }
    }

    @PostMapping("/logout")
    @Operation(summary = "Logout", description = "Invalida o token JWT atual adicionando-o à blacklist")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Logout realizado com sucesso"),
            @ApiResponse(responseCode = "401", description = "Token ausente ou inválido")
    })
    public ResponseEntity<?> logout(HttpServletRequest request) {
        String authHeader = request.getHeader("Authorization");
        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            return ResponseEntity.status(401)
                    .body(new ErroResponse(401, "Token ausente ou inválido", "/auth/logout"));
        }

        String token = authHeader.substring(7).trim();
        if (token.isEmpty()) {
            return ResponseEntity.status(401)
                    .body(new ErroResponse(401, "Token ausente ou inválido", "/auth/logout"));
        }

        try {
            tokenBlacklist.add(token, jwtUtil.extractExpiration(token));
            log.info("Logout realizado com sucesso");
        } catch (ExpiredJwtException e) {

            log.info("Logout com token já expirado — nada a blacklistar");
        } catch (JwtException | IllegalArgumentException e) {
            log.warn("Logout com token inválido: {}", e.getClass().getSimpleName());
            return ResponseEntity.status(401)
                    .body(new ErroResponse(401, "Token inválido", "/auth/logout"));
        }
        return ResponseEntity.ok(Map.of("mensagem", "Logout realizado com sucesso"));
    }
}
