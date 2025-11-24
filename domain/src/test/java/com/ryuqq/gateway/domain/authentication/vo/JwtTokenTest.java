package com.ryuqq.gateway.domain.authentication.vo;

import com.ryuqq.gateway.fixture.domain.JwtTokenFixture;
import com.tngtech.archunit.core.domain.JavaClasses;
import com.tngtech.archunit.core.importer.ClassFileImporter;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.time.Instant;
import java.time.temporal.ChronoUnit;

import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.classes;
import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("JwtToken Aggregate 테스트")
class JwtTokenTest {

    @Nested
    @DisplayName("동작 검증 테스트")
    class BehaviorTests {

        @Test
        @DisplayName("유효한 데이터로 JwtToken 생성")
        void shouldCreateJwtTokenWithValidData() {
            // Given & When
            JwtToken jwtToken = JwtTokenFixture.aValidJwtToken();

            // Then
            assertThat(jwtToken.accessToken()).isNotNull();
            assertThat(jwtToken.expiresAt()).isNotNull();
            assertThat(jwtToken.createdAt()).isNotNull();
            assertThat(jwtToken.expiresAt()).isAfter(jwtToken.createdAt());
        }

        @Test
        @DisplayName("토큰이 만료되지 않았음을 검증")
        void shouldValidateTokenNotExpired() {
            // Given
            JwtToken jwtToken = JwtTokenFixture.aValidJwtToken();

            // When
            boolean expired = jwtToken.isExpired();

            // Then
            assertThat(expired).isFalse();
        }

        @Test
        @DisplayName("토큰이 만료되었음을 검증")
        void shouldValidateTokenExpired() {
            // Given
            JwtToken jwtToken = JwtTokenFixture.anExpiredJwtToken();

            // When
            boolean expired = jwtToken.isExpired();

            // Then
            assertThat(expired).isTrue();
        }
    }

    @Nested
    @DisplayName("ArchUnit 검증 테스트")
    class ArchUnitTests {

        private final JavaClasses jwtClasses = new ClassFileImporter()
                .importPackages("com.ryuqq.gateway.domain.authentication.vo");

        @Test
        @DisplayName("JwtToken은 final 클래스여야 함 (불변성)")
        void jwtTokenShouldBeFinal() {
            classes()
                    .that().haveSimpleNameEndingWith("JwtToken")
                    .should().haveModifier(com.tngtech.archunit.core.domain.JavaModifier.FINAL)
                    .check(jwtClasses);
        }

        @Test
        @DisplayName("JwtToken의 모든 필드는 final이어야 함 (불변성)")
        void jwtTokenFieldsShouldBeFinal() throws Exception {
            // Given
            Class<?> jwtTokenClass = JwtToken.class;
            Field[] fields = jwtTokenClass.getDeclaredFields();

            // Then
            for (Field field : fields) {
                assertThat(Modifier.isFinal(field.getModifiers()))
                        .as("Field '%s' should be final", field.getName())
                        .isTrue();
            }
        }

        @Test
        @DisplayName("Domain Layer는 외부 의존성이 없어야 함 (production 코드만)")
        void domainLayerShouldNotDependOnExternalLibraries() {
            classes()
                    .that().resideInAPackage("..domain.authentication.vo..")
                    .and().haveSimpleNameNotEndingWith("Test")
                    .and().haveSimpleNameNotEndingWith("Tests")
                    .should().onlyDependOnClassesThat().resideInAnyPackage(
                            "java..",
                            "..domain.."
                    )
                    .check(jwtClasses);
        }

        @Test
        @DisplayName("JwtToken은 Lombok을 사용하지 않아야 함 (Zero-Tolerance)")
        void jwtTokenShouldNotUseLombok() {
            classes()
                    .that().haveSimpleNameEndingWith("JwtToken")
                    .should().notBeAnnotatedWith("lombok..")
                    .check(jwtClasses);
        }
    }
}
