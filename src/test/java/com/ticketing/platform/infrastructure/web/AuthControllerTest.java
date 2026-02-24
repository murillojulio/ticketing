package com.ticketing.platform.infrastructure.web;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

import com.ticketing.platform.application.model.AuthResult;
import com.ticketing.platform.application.port.in.AuthUseCase;
import com.ticketing.platform.domain.exception.DomainException;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.autoconfigure.web.reactive.AutoConfigureWebTestClient;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.reactive.WebFluxTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.web.reactive.server.WebTestClient;
import reactor.core.publisher.Mono;

@WebFluxTest(controllers = AuthController.class)
@AutoConfigureWebTestClient(addFilters = false)
@Import(GlobalExceptionHandler.class)
class AuthControllerTest {

    @Autowired
    private WebTestClient webTestClient;

    @MockBean
    private AuthUseCase authUseCase;

    @Test
    void shouldRegisterUser() {
        when(authUseCase.register(any())).thenReturn(Mono.just(
            new AuthResult(
                UUID.randomUUID(),
                "user@example.com",
                "USER",
                "jwt-token",
                "Bearer"
            )
        ));

        webTestClient.post()
            .uri("/api/auth/register")
            .contentType(MediaType.APPLICATION_JSON)
            .bodyValue("""
                {
                  "email": "user@example.com",
                  "password": "password123"
                }
                """)
            .exchange()
            .expectStatus().isCreated()
            .expectBody()
            .jsonPath("$.email").isEqualTo("user@example.com")
            .jsonPath("$.token").isEqualTo("jwt-token");
    }

    @Test
    void shouldLoginUser() {
        when(authUseCase.login(any())).thenReturn(Mono.just(
            new AuthResult(
                UUID.randomUUID(),
                "user@example.com",
                "USER",
                "jwt-token",
                "Bearer"
            )
        ));

        webTestClient.post()
            .uri("/api/auth/login")
            .contentType(MediaType.APPLICATION_JSON)
            .bodyValue("""
                {
                  "email": "user@example.com",
                  "password": "password123"
                }
                """)
            .exchange()
            .expectStatus().isOk()
            .expectBody()
            .jsonPath("$.tokenType").isEqualTo("Bearer");
    }

    @Test
    void shouldValidateAuthRequest() {
        webTestClient.post()
            .uri("/api/auth/register")
            .contentType(MediaType.APPLICATION_JSON)
            .bodyValue("""
                {
                  "email": "invalid-email",
                  "password": "123"
                }
                """)
            .exchange()
            .expectStatus().isBadRequest()
            .expectBody()
            .jsonPath("$.status").isEqualTo(400);
    }

    @Test
    void shouldMapDomainExceptionOnLogin() {
        when(authUseCase.login(any()))
            .thenReturn(Mono.error(new DomainException("Invalid credentials")));

        webTestClient.post()
            .uri("/api/auth/login")
            .contentType(MediaType.APPLICATION_JSON)
            .bodyValue("""
                {
                  "email": "user@example.com",
                  "password": "password123"
                }
                """)
            .exchange()
            .expectStatus().isBadRequest()
            .expectBody()
            .jsonPath("$.message").isEqualTo("Invalid credentials");
    }
}
