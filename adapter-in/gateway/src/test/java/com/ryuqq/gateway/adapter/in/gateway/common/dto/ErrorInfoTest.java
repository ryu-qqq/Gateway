package com.ryuqq.gateway.adapter.in.gateway.common.dto;

import static org.assertj.core.api.Assertions.*;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.junit.jupiter.params.provider.ValueSource;

/**
 * ErrorInfo 단위 테스트
 *
 * @author development-team
 * @since 1.0.0
 */
@DisplayName("ErrorInfo 단위 테스트")
class ErrorInfoTest {

    @Nested
    @DisplayName("생성 테스트")
    class CreationTest {

        @Test
        @DisplayName("유효한 값으로 ErrorInfo를 생성해야 한다")
        void shouldCreateErrorInfoWithValidValues() {
            // given
            String errorCode = "JWT_EXPIRED";
            String message = "토큰이 만료되었습니다";

            // when
            ErrorInfo errorInfo = new ErrorInfo(errorCode, message);

            // then
            assertThat(errorInfo.errorCode()).isEqualTo("JWT_EXPIRED");
            assertThat(errorInfo.message()).isEqualTo("토큰이 만료되었습니다");
        }
    }

    @Nested
    @DisplayName("유효성 검증 테스트")
    class ValidationTest {

        @ParameterizedTest
        @NullAndEmptySource
        @ValueSource(strings = {"  ", "\t", "\n"})
        @DisplayName("errorCode가 null이거나 빈 문자열이면 예외가 발생해야 한다")
        void shouldThrowExceptionWhenErrorCodeIsNullOrBlank(String errorCode) {
            // when & then
            assertThatThrownBy(() -> new ErrorInfo(errorCode, "test message"))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessage("errorCode는 필수입니다");
        }

        @ParameterizedTest
        @NullAndEmptySource
        @ValueSource(strings = {"  ", "\t", "\n"})
        @DisplayName("message가 null이거나 빈 문자열이면 예외가 발생해야 한다")
        void shouldThrowExceptionWhenMessageIsNullOrBlank(String message) {
            // when & then
            assertThatThrownBy(() -> new ErrorInfo("ERROR_CODE", message))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessage("message는 필수입니다");
        }
    }
}
