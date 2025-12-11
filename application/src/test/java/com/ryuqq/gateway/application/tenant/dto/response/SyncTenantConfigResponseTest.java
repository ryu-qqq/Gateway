package com.ryuqq.gateway.application.tenant.dto.response;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

@DisplayName("SyncTenantConfigResponse 테스트")
class SyncTenantConfigResponseTest {

    @Nested
    @DisplayName("success() 팩토리 메서드 테스트")
    class SuccessFactoryTest {

        @Test
        @DisplayName("success 응답 생성")
        void shouldCreateSuccessResponse() {
            // given
            String tenantId = "tenant-1";

            // when
            SyncTenantConfigResponse response = SyncTenantConfigResponse.success(tenantId);

            // then
            assertThat(response.success()).isTrue();
            assertThat(response.tenantId()).isEqualTo(tenantId);
        }

        @Test
        @DisplayName("다양한 tenantId로 success 응답 생성")
        void shouldCreateSuccessResponseWithVariousTenantIds() {
            // given
            String tenantId = "tenant-12345";

            // when
            SyncTenantConfigResponse response = SyncTenantConfigResponse.success(tenantId);

            // then
            assertThat(response.success()).isTrue();
            assertThat(response.tenantId()).isEqualTo(tenantId);
        }
    }

    @Nested
    @DisplayName("failure() 팩토리 메서드 테스트")
    class FailureFactoryTest {

        @Test
        @DisplayName("failure 응답 생성")
        void shouldCreateFailureResponse() {
            // given
            String tenantId = "tenant-2";

            // when
            SyncTenantConfigResponse response = SyncTenantConfigResponse.failure(tenantId);

            // then
            assertThat(response.success()).isFalse();
            assertThat(response.tenantId()).isEqualTo(tenantId);
        }

        @Test
        @DisplayName("다양한 tenantId로 failure 응답 생성")
        void shouldCreateFailureResponseWithVariousTenantIds() {
            // given
            String tenantId = "tenant-error-99";

            // when
            SyncTenantConfigResponse response = SyncTenantConfigResponse.failure(tenantId);

            // then
            assertThat(response.success()).isFalse();
            assertThat(response.tenantId()).isEqualTo(tenantId);
        }
    }

    @Nested
    @DisplayName("Record 생성자 테스트")
    class ConstructorTest {

        @Test
        @DisplayName("직접 생성자로 생성")
        void shouldCreateWithConstructor() {
            // when
            SyncTenantConfigResponse response = new SyncTenantConfigResponse(true, "tenant-3");

            // then
            assertThat(response.success()).isTrue();
            assertThat(response.tenantId()).isEqualTo("tenant-3");
        }

        @Test
        @DisplayName("success=false로 직접 생성")
        void shouldCreateWithConstructorFalse() {
            // when
            SyncTenantConfigResponse response = new SyncTenantConfigResponse(false, "tenant-4");

            // then
            assertThat(response.success()).isFalse();
            assertThat(response.tenantId()).isEqualTo("tenant-4");
        }
    }

    @Nested
    @DisplayName("Record 동등성 테스트")
    class EqualityTest {

        @Test
        @DisplayName("같은 값을 가진 Response는 동등하다")
        void shouldBeEqualWhenSameValues() {
            // given
            SyncTenantConfigResponse response1 = SyncTenantConfigResponse.success("tenant-1");
            SyncTenantConfigResponse response2 = SyncTenantConfigResponse.success("tenant-1");

            // then
            assertThat(response1).isEqualTo(response2);
            assertThat(response1.hashCode()).isEqualTo(response2.hashCode());
        }

        @Test
        @DisplayName("다른 success 값을 가진 Response는 동등하지 않다")
        void shouldNotBeEqualWhenDifferentSuccess() {
            // given
            SyncTenantConfigResponse response1 = SyncTenantConfigResponse.success("tenant-1");
            SyncTenantConfigResponse response2 = SyncTenantConfigResponse.failure("tenant-1");

            // then
            assertThat(response1).isNotEqualTo(response2);
        }

        @Test
        @DisplayName("다른 tenantId를 가진 Response는 동등하지 않다")
        void shouldNotBeEqualWhenDifferentTenantId() {
            // given
            SyncTenantConfigResponse response1 = SyncTenantConfigResponse.success("tenant-1");
            SyncTenantConfigResponse response2 = SyncTenantConfigResponse.success("tenant-2");

            // then
            assertThat(response1).isNotEqualTo(response2);
        }
    }
}
