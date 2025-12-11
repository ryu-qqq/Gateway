package com.ryuqq.gateway.domain.tenant.exception;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

@DisplayName("Tenant Exception 테스트")
class TenantExceptionTest {

    @Nested
    @DisplayName("MfaRequiredException 테스트")
    class MfaRequiredExceptionTest {

        @Test
        @DisplayName("tenantId로 예외 생성")
        void shouldCreateWithTenantId() {
            // given
            String tenantId = "tenant-1";

            // when
            MfaRequiredException exception = new MfaRequiredException(tenantId);

            // then
            assertThat(exception).isNotNull();
            assertThat(exception.tenantId()).isEqualTo(tenantId);
            assertThat(exception.getMessage()).contains("tenantId=" + tenantId);
            assertThat(exception.getErrorCode()).isEqualTo(TenantErrorCode.MFA_REQUIRED);
        }

        @Test
        @DisplayName("기본 생성자로 예외 생성")
        void shouldCreateWithDefaultConstructor() {
            // when
            MfaRequiredException exception = new MfaRequiredException();

            // then
            assertThat(exception).isNotNull();
            assertThat(exception.tenantId()).isNull();
            assertThat(exception.getErrorCode()).isEqualTo(TenantErrorCode.MFA_REQUIRED);
        }
    }

    @Nested
    @DisplayName("SocialLoginNotAllowedException 테스트")
    class SocialLoginNotAllowedExceptionTest {

        @Test
        @DisplayName("tenantId와 provider로 예외 생성")
        void shouldCreateWithTenantIdAndProvider() {
            // given
            String tenantId = "tenant-1";
            String provider = "naver";

            // when
            SocialLoginNotAllowedException exception = new SocialLoginNotAllowedException(tenantId, provider);

            // then
            assertThat(exception).isNotNull();
            assertThat(exception.tenantId()).isEqualTo(tenantId);
            assertThat(exception.provider()).isEqualTo(provider);
            assertThat(exception.getMessage()).contains("tenantId=" + tenantId);
            assertThat(exception.getMessage()).contains("provider=" + provider);
            assertThat(exception.getErrorCode()).isEqualTo(TenantErrorCode.SOCIAL_LOGIN_NOT_ALLOWED);
        }

        @Test
        @DisplayName("provider만으로 예외 생성")
        void shouldCreateWithProviderOnly() {
            // given
            String provider = "facebook";

            // when
            SocialLoginNotAllowedException exception = new SocialLoginNotAllowedException(provider);

            // then
            assertThat(exception).isNotNull();
            assertThat(exception.tenantId()).isNull();
            assertThat(exception.provider()).isEqualTo(provider);
            assertThat(exception.getMessage()).contains("provider=" + provider);
            assertThat(exception.getErrorCode()).isEqualTo(TenantErrorCode.SOCIAL_LOGIN_NOT_ALLOWED);
        }
    }

    @Nested
    @DisplayName("TenantMismatchException 테스트")
    class TenantMismatchExceptionTest {

        @Test
        @DisplayName("expected와 actual tenantId로 예외 생성")
        void shouldCreateWithExpectedAndActual() {
            // given
            String expected = "tenant-1";
            String actual = "tenant-2";

            // when
            TenantMismatchException exception = new TenantMismatchException(expected, actual);

            // then
            assertThat(exception).isNotNull();
            assertThat(exception.expectedTenantId()).isEqualTo(expected);
            assertThat(exception.actualTenantId()).isEqualTo(actual);
            assertThat(exception.getMessage()).contains("expected=" + expected);
            assertThat(exception.getMessage()).contains("actual=" + actual);
            assertThat(exception.getErrorCode()).isEqualTo(TenantErrorCode.TENANT_MISMATCH);
        }

        @Test
        @DisplayName("단일 tenantId로 예외 생성")
        void shouldCreateWithSingleTenantId() {
            // given
            String tenantId = "tenant-1";

            // when
            TenantMismatchException exception = new TenantMismatchException(tenantId);

            // then
            assertThat(exception).isNotNull();
            assertThat(exception.expectedTenantId()).isEqualTo(tenantId);
            assertThat(exception.actualTenantId()).isNull();
            assertThat(exception.getMessage()).contains("tenantId=" + tenantId);
            assertThat(exception.getErrorCode()).isEqualTo(TenantErrorCode.TENANT_MISMATCH);
        }

        @Test
        @DisplayName("기본 생성자로 예외 생성")
        void shouldCreateWithDefaultConstructor() {
            // when
            TenantMismatchException exception = new TenantMismatchException();

            // then
            assertThat(exception).isNotNull();
            assertThat(exception.expectedTenantId()).isNull();
            assertThat(exception.actualTenantId()).isNull();
            assertThat(exception.getErrorCode()).isEqualTo(TenantErrorCode.TENANT_MISMATCH);
        }
    }

    @Nested
    @DisplayName("TenantConfigPersistenceException 테스트")
    class TenantConfigPersistenceExceptionTest {

        @Test
        @DisplayName("tenantId와 operation으로 예외 생성")
        void shouldCreateWithTenantIdAndOperation() {
            // given
            String tenantId = "tenant-1";
            String operation = "save";

            // when
            TenantConfigPersistenceException exception =
                    new TenantConfigPersistenceException(tenantId, operation);

            // then
            assertThat(exception).isNotNull();
            assertThat(exception.tenantId()).isEqualTo(tenantId);
            assertThat(exception.operation()).isEqualTo(operation);
            assertThat(exception.getMessage()).contains("tenantId=" + tenantId);
            assertThat(exception.getMessage()).contains("operation=" + operation);
            assertThat(exception.getErrorCode()).isEqualTo(TenantErrorCode.TENANT_CONFIG_PERSISTENCE_ERROR);
        }

        @Test
        @DisplayName("tenantId, operation, cause로 예외 생성")
        void shouldCreateWithCause() {
            // given
            String tenantId = "tenant-1";
            String operation = "fetch";
            Throwable cause = new RuntimeException("Redis connection failed");

            // when
            TenantConfigPersistenceException exception =
                    new TenantConfigPersistenceException(tenantId, operation, cause);

            // then
            assertThat(exception).isNotNull();
            assertThat(exception.tenantId()).isEqualTo(tenantId);
            assertThat(exception.operation()).isEqualTo(operation);
            assertThat(exception.getCause()).isEqualTo(cause);
            assertThat(exception.getErrorCode()).isEqualTo(TenantErrorCode.TENANT_CONFIG_PERSISTENCE_ERROR);
        }

        @Test
        @DisplayName("다양한 operation 값 테스트")
        void shouldSupportVariousOperations() {
            // when
            TenantConfigPersistenceException saveException =
                    new TenantConfigPersistenceException("tenant-1", "save");
            TenantConfigPersistenceException fetchException =
                    new TenantConfigPersistenceException("tenant-1", "fetch");
            TenantConfigPersistenceException deleteException =
                    new TenantConfigPersistenceException("tenant-1", "delete");

            // then
            assertThat(saveException.operation()).isEqualTo("save");
            assertThat(fetchException.operation()).isEqualTo("fetch");
            assertThat(deleteException.operation()).isEqualTo("delete");
        }
    }

    @Nested
    @DisplayName("TenantErrorCode 테스트")
    class TenantErrorCodeTest {

        @Test
        @DisplayName("모든 에러 코드가 정의됨")
        void shouldHaveAllErrorCodes() {
            // when & then
            assertThat(TenantErrorCode.TENANT_MISMATCH).isNotNull();
            assertThat(TenantErrorCode.MFA_REQUIRED).isNotNull();
            assertThat(TenantErrorCode.SOCIAL_LOGIN_NOT_ALLOWED).isNotNull();
            assertThat(TenantErrorCode.TENANT_CONFIG_PERSISTENCE_ERROR).isNotNull();
        }

        @Test
        @DisplayName("에러 코드가 고유함")
        void shouldHaveUniqueErrorCodes() {
            // when
            String code1 = TenantErrorCode.TENANT_MISMATCH.getCode();
            String code2 = TenantErrorCode.MFA_REQUIRED.getCode();
            String code3 = TenantErrorCode.SOCIAL_LOGIN_NOT_ALLOWED.getCode();
            String code4 = TenantErrorCode.TENANT_CONFIG_PERSISTENCE_ERROR.getCode();

            // then
            assertThat(code1).isNotEqualTo(code2);
            assertThat(code1).isNotEqualTo(code3);
            assertThat(code1).isNotEqualTo(code4);
            assertThat(code2).isNotEqualTo(code3);
            assertThat(code2).isNotEqualTo(code4);
            assertThat(code3).isNotEqualTo(code4);
        }
    }
}
