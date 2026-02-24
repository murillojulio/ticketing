package com.ticketing.platform.infrastructure.config;

import com.ticketing.platform.infrastructure.security.JwtPrincipal;
import com.ticketing.platform.infrastructure.security.JwtTokenService;
import java.util.List;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.security.authentication.ReactiveAuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.config.annotation.web.reactive.EnableWebFluxSecurity;
import org.springframework.security.config.web.server.SecurityWebFiltersOrder;
import org.springframework.security.config.web.server.ServerHttpSecurity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.web.server.SecurityWebFilterChain;
import org.springframework.security.web.server.authentication.AuthenticationWebFilter;
import org.springframework.security.web.server.context.NoOpServerSecurityContextRepository;
import reactor.core.publisher.Mono;

@Configuration
@EnableWebFluxSecurity
@ConditionalOnProperty(prefix = "ticketing.security", name = "enabled", havingValue = "true", matchIfMissing = true)
public class SecurityConfiguration {

    @Bean
    public SecurityWebFilterChain securityWebFilterChain(
        ServerHttpSecurity http,
        JwtTokenService jwtTokenService
    ) {
        AuthenticationWebFilter jwtFilter = new AuthenticationWebFilter(reactiveAuthenticationManager(jwtTokenService));
        jwtFilter.setServerAuthenticationConverter(this::extractBearerToken);
        jwtFilter.setSecurityContextRepository(NoOpServerSecurityContextRepository.getInstance());

        return http
            .csrf(ServerHttpSecurity.CsrfSpec::disable)
            .httpBasic(ServerHttpSecurity.HttpBasicSpec::disable)
            .formLogin(ServerHttpSecurity.FormLoginSpec::disable)
            .logout(ServerHttpSecurity.LogoutSpec::disable)
            .securityContextRepository(NoOpServerSecurityContextRepository.getInstance())
            .authorizeExchange(exchange -> exchange
                .pathMatchers("/api/auth/**").permitAll()
                .pathMatchers(HttpMethod.POST, "/api/payments/webhook").permitAll()
                .pathMatchers("/actuator/**").permitAll()
                .anyExchange().authenticated()
            )
            .addFilterAt(jwtFilter, SecurityWebFiltersOrder.AUTHENTICATION)
            .build();
    }

    private ReactiveAuthenticationManager reactiveAuthenticationManager(JwtTokenService jwtTokenService) {
        return authentication -> {
            String token = String.valueOf(authentication.getCredentials());
            return jwtTokenService.parse(token)
                .map(this::toAuthentication);
        };
    }

    private Authentication toAuthentication(JwtPrincipal principal) {
        List<GrantedAuthority> authorities = List.of(
            new SimpleGrantedAuthority("ROLE_" + principal.role())
        );
        return new UsernamePasswordAuthenticationToken(principal, null, authorities);
    }

    private Mono<Authentication> extractBearerToken(org.springframework.web.server.ServerWebExchange exchange) {
        String authorization = exchange.getRequest().getHeaders().getFirst(HttpHeaders.AUTHORIZATION);
        if (authorization == null || !authorization.startsWith("Bearer ")) {
            return Mono.empty();
        }
        String token = authorization.substring(7).trim();
        if (token.isEmpty()) {
            return Mono.empty();
        }
        return Mono.just(new UsernamePasswordAuthenticationToken("jwt", token));
    }
}
