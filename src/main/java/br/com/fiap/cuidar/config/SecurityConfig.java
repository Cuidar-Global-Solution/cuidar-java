package br.com.fiap.cuidar.config;

import br.com.fiap.cuidar.dto.ErroResponse;
import br.com.fiap.cuidar.exception.PersistenciaException;
import br.com.fiap.cuidar.service.AutenticacaoService;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.AnonymousAuthenticationFilter;
import org.springframework.web.filter.OncePerRequestFilter;
import org.springframework.web.servlet.HandlerExceptionResolver;
import tools.jackson.databind.ObjectMapper;

import java.io.IOException;
import java.time.Instant;
import java.util.List;

@Configuration
public class SecurityConfig {
    @Bean
    PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder(12);
    }

    @Bean
    SecurityFilterChain securityFilterChain(HttpSecurity http, AutenticacaoService autenticacao, CorsConfig cors,
            ObjectMapper json, @Qualifier("handlerExceptionResolver") HandlerExceptionResolver resolver) throws Exception {
        return http
                .cors(config -> config.configurationSource(cors.configurationSource()))
                .csrf(AbstractHttpConfigurer::disable)
                .logout(AbstractHttpConfigurer::disable)
                .requestCache(AbstractHttpConfigurer::disable)
                .sessionManagement(config -> config.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .exceptionHandling(config -> config.authenticationEntryPoint((request, response, cause) -> {
                    response.setStatus(HttpStatus.UNAUTHORIZED.value());
                    response.setHeader(HttpHeaders.WWW_AUTHENTICATE, "Bearer");
                    response.setContentType("application/json");
                    json.writeValue(response.getOutputStream(), new ErroResponse(Instant.now(), 401,
                            "Unauthorized", "Sessão ausente, inválida ou expirada.", request.getRequestURI(), List.of()));
                }))
                .authorizeHttpRequests(requests -> requests
                        .requestMatchers(HttpMethod.POST, "/auth/cadastro", "/auth/login").permitAll()
                        .anyRequest().authenticated())
                .addFilterBefore(new OncePerRequestFilter() {
                    @Override
                    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response,
                            FilterChain chain) throws ServletException, IOException {
                        String header = request.getHeader(HttpHeaders.AUTHORIZATION);
                        if (header != null && header.regionMatches(true, 0, "Bearer ", 0, 7)) {
                            try {
                                autenticacao.consultar(header.substring(7)).ifPresent(conta -> {
                                    var autoridade = new SimpleGrantedAuthority("ROLE_" + conta.perfil().name());
                                    var contexto = SecurityContextHolder.createEmptyContext();
                                    contexto.setAuthentication(UsernamePasswordAuthenticationToken.authenticated(
                                            conta, null, List.of(autoridade)));
                                    SecurityContextHolder.setContext(contexto);
                                });
                            } catch (PersistenciaException e) {
                                resolver.resolveException(request, response, null, e);
                                return;
                            }
                        }
                        chain.doFilter(request, response);
                    }
                }, AnonymousAuthenticationFilter.class)
                .build();
    }
}
