package com.ryuqq.connectly.adapter.out.persistence.config;

import org.springframework.boot.autoconfigure.flyway.FlywayMigrationStrategy;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;

/**
 * FlywayConfig - Flyway 마이그레이션 커스텀 설정
 *
 * <p>Flyway 데이터베이스 마이그레이션을 위한 추가 설정을 제공합니다.
 *
 * <p><strong>Flyway란?</strong>
 *
 * <ul>
 *   <li>데이터베이스 버전 관리 도구
 *   <li>SQL 스크립트 기반 마이그레이션
 *   <li>자동 버전 추적 (flyway_schema_history 테이블)
 *   <li>롤백 불가 (Forward-only migration)
 * </ul>
 *
 * <p><strong>마이그레이션 파일 네이밍 규칙:</strong>
 *
 * <ul>
 *   <li><strong>버전 마이그레이션:</strong> V{version}__{description}.sql
 *   <li>예: V1__Create_example_table.sql
 *   <li>예: V2__Add_user_table.sql
 *   <li>예: V3__Alter_example_add_column.sql
 *   <li><strong>반복 마이그레이션:</strong> R__{description}.sql (체크섬 변경 시 재실행)
 *   <li>예: R__Create_views.sql
 * </ul>
 *
 * <p><strong>마이그레이션 위치:</strong>
 *
 * <ul>
 *   <li>기본: classpath:db/migration
 *   <li>실제 경로: src/main/resources/db/migration
 *   <li>환경별 분리 가능: db/migration/{env}
 * </ul>
 *
 * <p><strong>마이그레이션 전략:</strong>
 *
 * <ul>
 *   <li><strong>개발:</strong> baseline-on-migrate=true, clean 허용
 *   <li><strong>스테이징:</strong> validate 후 migrate
 *   <li><strong>프로덕션:</strong> clean 금지, 수동 검증 후 migrate
 * </ul>
 *
 * <p><strong>주의사항:</strong>
 *
 * <ul>
 *   <li>한 번 적용된 마이그레이션은 수정 불가
 *   <li>새로운 변경사항은 새 버전으로 추가
 *   <li>프로덕션에서는 clean 절대 사용 금지
 *   <li>롤백이 필요한 경우 새로운 마이그레이션으로 처리
 * </ul>
 *
 * @author windsurf
 * @since 1.0.0
 * @see org.flywaydb.core.Flyway
 * @see org.springframework.boot.autoconfigure.flyway.FlywayProperties
 */
@Configuration
public class FlywayConfig {

    /**
     * 개발 환경 전용 Flyway 마이그레이션 전략
     *
     * <p>개발 환경에서는 스키마를 초기화하고 재생성할 수 있도록 설정합니다.
     *
     * <p><strong>동작 방식:</strong>
     *
     * <ol>
     *   <li>기존 스키마 정리 (clean)
     *   <li>마이그레이션 실행 (migrate)
     *   <li>마이그레이션 검증 (validate)
     * </ol>
     *
     * <p><strong>주의:</strong>
     *
     * <ul>
     *   <li>모든 데이터가 삭제됩니다
     *   <li>로컬 개발 환경에서만 사용
     *   <li>프로덕션에서는 절대 사용 금지
     * </ul>
     *
     * @return FlywayMigrationStrategy
     */
    @Bean
    @Profile("local")
    public FlywayMigrationStrategy cleanMigrateStrategy() {
        return flyway -> {
            // 1. 기존 스키마 정리 (주의: 모든 데이터 삭제)
            flyway.clean();

            // 2. 마이그레이션 실행
            flyway.migrate();

            // 3. 마이그레이션 검증
            flyway.validate();
        };
    }

    /**
     * 스테이징/프로덕션 환경 전용 Flyway 마이그레이션 전략
     *
     * <p>운영 환경에서는 안전한 마이그레이션을 위해 검증 후 실행합니다.
     *
     * <p><strong>동작 방식:</strong>
     *
     * <ol>
     *   <li>마이그레이션 검증 (validate)
     *   <li>마이그레이션 실행 (migrate)
     * </ol>
     *
     * <p><strong>검증 실패 시:</strong>
     *
     * <ul>
     *   <li>마이그레이션이 중단됩니다
     *   <li>로그를 확인하여 문제 해결 필요
     *   <li>체크섬 불일치, 누락된 마이그레이션 등
     * </ul>
     *
     * @return FlywayMigrationStrategy
     */
    @Bean
    @Profile({"dev", "stage", "prod"})
    public FlywayMigrationStrategy validateMigrateStrategy() {
        return flyway -> {
            // 1. 마이그레이션 검증
            flyway.validate();

            // 2. 마이그레이션 실행
            flyway.migrate();
        };
    }

    /**
     * Flyway 콜백 설정 (선택 사항)
     *
     * <p>마이그레이션 전후에 커스텀 로직을 실행할 수 있습니다.
     *
     * <p><strong>사용 예시:</strong>
     *
     * <pre>{@code
     * @Bean
     * public Callback flywayCallback() {
     *     return new Callback() {
     *         @Override
     *         public boolean supports(Event event, Context context) {
     *             return event == Event.AFTER_MIGRATE;
     *         }
     *
     *         @Override
     *         public boolean canHandleInTransaction(Event event, Context context) {
     *             return true;
     *         }
     *
     *         @Override
     *         public void handle(Event event, Context context) {
     *             // 마이그레이션 완료 후 실행할 로직
     *             System.out.println("Migration completed successfully!");
     *         }
     *     };
     * }
     * }</pre>
     *
     * <p><strong>이벤트 종류:</strong>
     *
     * <ul>
     *   <li>BEFORE_MIGRATE: 마이그레이션 시작 전
     *   <li>AFTER_MIGRATE: 마이그레이션 완료 후
     *   <li>BEFORE_EACH_MIGRATE: 각 마이그레이션 실행 전
     *   <li>AFTER_EACH_MIGRATE: 각 마이그레이션 실행 후
     *   <li>BEFORE_VALIDATE: 검증 시작 전
     *   <li>AFTER_VALIDATE: 검증 완료 후
     * </ul>
     */
    // 필요 시 Callback 구현
    // @Bean
    // public Callback flywayCallback() {
    //     return new Callback() { ... };
    // }

    /**
     * Flyway 정보 조회 (선택 사항)
     *
     * <p>Flyway 마이그레이션 상태를 조회하는 유틸리티 메서드입니다.
     *
     * <p><strong>사용 예시:</strong>
     *
     * <pre>{@code
     * @Autowired
     * private Flyway flyway;
     *
     * public void printMigrationInfo() {
     *     MigrationInfoService info = flyway.info();
     *
     *     System.out.println("Current version: " + info.current().getVersion());
     *     System.out.println("Pending migrations: " + info.pending().length);
     *
     *     for (MigrationInfo migration : info.all()) {
     *         System.out.println(
     *             migration.getVersion() + " - " +
     *             migration.getDescription() + " - " +
     *             migration.getState()
     *         );
     *     }
     * }
     * }</pre>
     *
     * <p><strong>마이그레이션 상태:</strong>
     *
     * <ul>
     *   <li>SUCCESS: 성공적으로 적용됨
     *   <li>PENDING: 적용 대기 중
     *   <li>MISSING_SUCCESS: 적용되었으나 파일 누락
     *   <li>FAILED: 적용 실패
     *   <li>IGNORED: 무시됨
     * </ul>
     */
}
