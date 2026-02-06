package com.ryuqq.gateway.application.authorization.dto.command;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

@DisplayName("SyncPermissionSpecCommand 테스트")
class SyncPermissionSpecCommandTest {

    @Nested
    @DisplayName("생성자 테스트")
    class ConstructorTest {

        @Test
        @DisplayName("정상적인 SyncPermissionSpecCommand 생성")
        void shouldCreateSyncPermissionSpecCommand() {
            // given
            Long version = 123L;
            List<String> changedServices = List.of("user-service", "order-service");

            // when
            SyncPermissionSpecCommand command =
                    new SyncPermissionSpecCommand(version, changedServices);

            // then
            assertThat(command.version()).isEqualTo(version);
            assertThat(command.changedServices()).isEqualTo(changedServices);
        }

        @Test
        @DisplayName("null changedServices는 빈 List로 변환됨")
        void shouldConvertNullChangedServicesToEmptyList() {
            // given
            Long version = 456L;
            List<String> changedServices = null;

            // when
            SyncPermissionSpecCommand command =
                    new SyncPermissionSpecCommand(version, changedServices);

            // then
            assertThat(command.version()).isEqualTo(version);
            assertThat(command.changedServices()).isEmpty();
        }

        @Test
        @DisplayName("빈 changedServices List로 생성")
        void shouldCreateWithEmptyChangedServices() {
            // given
            Long version = 789L;
            List<String> changedServices = List.of();

            // when
            SyncPermissionSpecCommand command =
                    new SyncPermissionSpecCommand(version, changedServices);

            // then
            assertThat(command.version()).isEqualTo(version);
            assertThat(command.changedServices()).isEmpty();
        }

        @Test
        @DisplayName("단일 서비스로 생성")
        void shouldCreateWithSingleService() {
            // given
            Long version = 100L;
            List<String> changedServices = List.of("payment-service");

            // when
            SyncPermissionSpecCommand command =
                    new SyncPermissionSpecCommand(version, changedServices);

            // then
            assertThat(command.version()).isEqualTo(version);
            assertThat(command.changedServices()).hasSize(1);
            assertThat(command.changedServices()).containsExactly("payment-service");
        }

        @Test
        @DisplayName("복수 서비스로 생성")
        void shouldCreateWithMultipleServices() {
            // given
            Long version = 200L;
            List<String> changedServices =
                    List.of(
                            "user-service",
                            "order-service",
                            "payment-service",
                            "notification-service");

            // when
            SyncPermissionSpecCommand command =
                    new SyncPermissionSpecCommand(version, changedServices);

            // then
            assertThat(command.version()).isEqualTo(version);
            assertThat(command.changedServices()).hasSize(4);
            assertThat(command.changedServices())
                    .containsExactly(
                            "user-service",
                            "order-service",
                            "payment-service",
                            "notification-service");
        }
    }

    @Nested
    @DisplayName("of() 정적 팩토리 메서드 테스트")
    class OfMethodTest {

        @Test
        @DisplayName("version과 changedServices로 생성")
        void shouldCreateUsingStaticFactoryMethodWithServices() {
            // given
            Long version = 300L;
            List<String> changedServices = List.of("inventory-service", "analytics-service");

            // when
            SyncPermissionSpecCommand command =
                    SyncPermissionSpecCommand.of(version, changedServices);

            // then
            assertThat(command.version()).isEqualTo(version);
            assertThat(command.changedServices()).isEqualTo(changedServices);
        }

        @Test
        @DisplayName("version만으로 생성")
        void shouldCreateUsingStaticFactoryMethodWithVersionOnly() {
            // given
            Long version = 400L;

            // when
            SyncPermissionSpecCommand command = SyncPermissionSpecCommand.of(version);

            // then
            assertThat(command.version()).isEqualTo(version);
            assertThat(command.changedServices()).isEmpty();
        }

        @Test
        @DisplayName("정적 팩토리 메서드도 null changedServices를 빈 List로 변환")
        void shouldConvertNullChangedServicesInStaticFactoryMethod() {
            // when
            SyncPermissionSpecCommand command = SyncPermissionSpecCommand.of(500L, null);

            // then
            assertThat(command.version()).isEqualTo(500L);
            assertThat(command.changedServices()).isEmpty();
        }
    }

    @Nested
    @DisplayName("불변성 테스트")
    class ImmutabilityTest {

        @Test
        @DisplayName("changedServices List가 불변임")
        void shouldHaveImmutableChangedServices() {
            // given
            List<String> originalServices = List.of("service1", "service2");
            SyncPermissionSpecCommand command =
                    SyncPermissionSpecCommand.of(600L, originalServices);

            // when
            List<String> returnedServices = command.changedServices();

            // then
            assertThat(returnedServices).isEqualTo(originalServices);
            assertThatThrownBy(() -> returnedServices.add("new-service"))
                    .isInstanceOf(UnsupportedOperationException.class);
        }

        @Test
        @DisplayName("원본 changedServices List 변경이 command에 영향 없음")
        void shouldNotBeAffectedByOriginalServicesModification() {
            // given
            List<String> mutableServices = List.of("service1");
            SyncPermissionSpecCommand command = SyncPermissionSpecCommand.of(700L, mutableServices);

            // when & then
            assertThat(command.changedServices()).containsExactly("service1");
        }
    }

    @Nested
    @DisplayName("Record 동작 테스트")
    class RecordBehaviorTest {

        @Test
        @DisplayName("equals()가 올바르게 동작")
        void shouldHaveCorrectEquals() {
            // given
            SyncPermissionSpecCommand command1 =
                    SyncPermissionSpecCommand.of(800L, List.of("service1"));
            SyncPermissionSpecCommand command2 =
                    SyncPermissionSpecCommand.of(800L, List.of("service1"));
            SyncPermissionSpecCommand command3 =
                    SyncPermissionSpecCommand.of(900L, List.of("service1"));

            // when & then
            assertThat(command1).isEqualTo(command2);
            assertThat(command1).isNotEqualTo(command3);
        }

        @Test
        @DisplayName("hashCode()가 올바르게 동작")
        void shouldHaveCorrectHashCode() {
            // given
            SyncPermissionSpecCommand command1 =
                    SyncPermissionSpecCommand.of(1000L, List.of("service1"));
            SyncPermissionSpecCommand command2 =
                    SyncPermissionSpecCommand.of(1000L, List.of("service1"));

            // when & then
            assertThat(command1.hashCode()).isEqualTo(command2.hashCode());
        }

        @Test
        @DisplayName("toString()이 모든 필드를 포함")
        void shouldIncludeAllFieldsInToString() {
            // given
            SyncPermissionSpecCommand command =
                    SyncPermissionSpecCommand.of(1100L, List.of("test-service"));

            // when
            String toString = command.toString();

            // then
            assertThat(toString).contains("1100");
            assertThat(toString).contains("test-service");
        }

        @Test
        @DisplayName("빈 changedServices의 toString()")
        void shouldHaveCorrectToStringForEmptyServices() {
            // given
            SyncPermissionSpecCommand command = SyncPermissionSpecCommand.of(1200L);

            // when
            String toString = command.toString();

            // then
            assertThat(toString).contains("1200");
            assertThat(toString).contains("[]");
        }
    }

    @Nested
    @DisplayName("다양한 입력값 테스트")
    class VariousInputTest {

        @Test
        @DisplayName("0 버전으로 생성")
        void shouldCreateWithZeroVersion() {
            // when
            SyncPermissionSpecCommand command =
                    SyncPermissionSpecCommand.of(0L, List.of("service1"));

            // then
            assertThat(command.version()).isEqualTo(0L);
            assertThat(command.changedServices()).containsExactly("service1");
        }

        @Test
        @DisplayName("음수 버전으로 생성")
        void shouldCreateWithNegativeVersion() {
            // when
            SyncPermissionSpecCommand command =
                    SyncPermissionSpecCommand.of(-1L, List.of("service1"));

            // then
            assertThat(command.version()).isEqualTo(-1L);
            assertThat(command.changedServices()).containsExactly("service1");
        }

        @Test
        @DisplayName("매우 큰 버전 번호로 생성")
        void shouldCreateWithLargeVersion() {
            // given
            Long largeVersion = Long.MAX_VALUE;

            // when
            SyncPermissionSpecCommand command =
                    SyncPermissionSpecCommand.of(largeVersion, List.of("service1"));

            // then
            assertThat(command.version()).isEqualTo(largeVersion);
            assertThat(command.changedServices()).containsExactly("service1");
        }

        @Test
        @DisplayName("특수 문자가 포함된 서비스명으로 생성")
        void shouldCreateWithSpecialCharactersInServiceNames() {
            // given
            List<String> servicesWithSpecialChars =
                    List.of(
                            "user-service",
                            "order_service",
                            "payment.service",
                            "notification@service");

            // when
            SyncPermissionSpecCommand command =
                    SyncPermissionSpecCommand.of(1300L, servicesWithSpecialChars);

            // then
            assertThat(command.version()).isEqualTo(1300L);
            assertThat(command.changedServices()).isEqualTo(servicesWithSpecialChars);
        }
    }
}
