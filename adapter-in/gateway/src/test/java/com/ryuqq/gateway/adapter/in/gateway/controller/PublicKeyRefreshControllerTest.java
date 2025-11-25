package com.ryuqq.gateway.adapter.in.gateway.controller;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

import com.ryuqq.gateway.application.authentication.port.in.command.RefreshPublicKeysUseCase;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

/**
 * PublicKeyRefreshController 테스트
 *
 * @author development-team
 * @since 1.0.0
 */
@ExtendWith(MockitoExtension.class)
class PublicKeyRefreshControllerTest {

    @Mock private RefreshPublicKeysUseCase refreshPublicKeysUseCase;

    private PublicKeyRefreshController controller;

    @BeforeEach
    void setUp() {
        controller = new PublicKeyRefreshController(refreshPublicKeysUseCase);
    }

    @Test
    @DisplayName("refreshPublicKeys 호출 시 성공하면 200 OK를 반환해야 한다")
    void shouldRefreshPublicKeysSuccessfully() {
        // given
        when(refreshPublicKeysUseCase.execute()).thenReturn(Mono.empty());

        // when & then
        StepVerifier.create(controller.refreshPublicKeys())
                .assertNext(
                        response -> assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK))
                .verifyComplete();
    }

    @Test
    @DisplayName("Refresh 실패 시 에러가 전파되어야 한다")
    void shouldPropagateErrorWhenRefreshFails() {
        // given
        when(refreshPublicKeysUseCase.execute())
                .thenReturn(Mono.error(new RuntimeException("Failed to refresh")));

        // when & then
        StepVerifier.create(controller.refreshPublicKeys())
                .expectError(RuntimeException.class)
                .verify();
    }
}
