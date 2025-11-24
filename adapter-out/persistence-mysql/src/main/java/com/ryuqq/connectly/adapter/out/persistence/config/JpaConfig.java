package com.ryuqq.connectly.adapter.out.persistence.config;

import com.querydsl.jpa.impl.JPAQueryFactory;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.jpa.repository.config.EnableJpaAuditing;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;
import org.springframework.transaction.annotation.EnableTransactionManagement;

/**
 * JpaConfig - JPA 및 QueryDSL 설정
 *
 * <p>Spring Data JPA와 QueryDSL을 위한 설정을 제공합니다.
 *
 * <p><strong>주요 기능:</strong>
 *
 * <ul>
 *   <li>JPAQueryFactory 빈 등록 (QueryDSL 사용)
 *   <li>JPA Repository 스캔 경로 설정
 *   <li>JPA Auditing 활성화 (생성/수정 일시 자동 관리)
 *   <li>트랜잭션 관리 활성화
 * </ul>
 *
 * <p><strong>QueryDSL 사용 이유:</strong>
 *
 * <ul>
 *   <li>타입 안전한 쿼리 작성
 *   <li>컴파일 타임 오류 검증
 *   <li>복잡한 동적 쿼리 작성 용이
 *   <li>IDE 자동완성 지원
 *   <li>리팩토링 안전성
 * </ul>
 *
 * <p><strong>JPA Auditing:</strong>
 *
 * <ul>
 *   <li>@CreatedDate: 엔티티 생성 일시 자동 설정
 *   <li>@LastModifiedDate: 엔티티 수정 일시 자동 설정
 *   <li>@CreatedBy: 생성자 자동 설정 (AuditorAware 필요)
 *   <li>@LastModifiedBy: 수정자 자동 설정 (AuditorAware 필요)
 * </ul>
 *
 * <p><strong>트랜잭션 관리:</strong>
 *
 * <ul>
 *   <li>Application Layer에서 @Transactional 사용
 *   <li>Persistence Layer는 트랜잭션 경계 없음
 *   <li>읽기 전용 트랜잭션 최적화 (@Transactional(readOnly = true))
 * </ul>
 *
 * @author windsurf
 * @since 1.0.0
 * @see com.querydsl.jpa.impl.JPAQueryFactory
 * @see org.springframework.data.jpa.repository.config.EnableJpaRepositories
 * @see org.springframework.data.jpa.repository.config.EnableJpaAuditing
 */
@Configuration
@EnableJpaRepositories(
        basePackages = "com.ryuqq.adapter.out.persistence" // 전체 persistence 패키지
        )
@EnableJpaAuditing
@EnableTransactionManagement
public class JpaConfig {

    @PersistenceContext private EntityManager entityManager;

    /**
     * JPAQueryFactory 빈 등록
     *
     * <p>QueryDSL을 사용하여 타입 안전한 쿼리를 작성하기 위한 Factory 클래스입니다.
     *
     * <p><strong>사용 방법:</strong>
     *
     * <pre>{@code
     * @Repository
     * public class ExampleQueryRepository {
     *     private final JPAQueryFactory queryFactory;
     *
     *     public ExampleQueryRepository(JPAQueryFactory queryFactory) {
     *         this.queryFactory = queryFactory;
     *     }
     *
     *     public List<ExampleJpaEntity> findByStatus(String status) {
     *         QExampleJpaEntity example = QExampleJpaEntity.exampleJpaEntity;
     *
     *         return queryFactory
     *             .selectFrom(example)
     *             .where(example.status.eq(ExampleStatus.valueOf(status)))
     *             .orderBy(example.createdAt.desc())
     *             .fetch();
     *     }
     * }
     * }</pre>
     *
     * <p><strong>QueryDSL 주요 메서드:</strong>
     *
     * <ul>
     *   <li><strong>select():</strong> 조회 대상 지정
     *   <li><strong>from():</strong> 조회 테이블 지정
     *   <li><strong>where():</strong> 조건절 (동적 쿼리 가능)
     *   <li><strong>orderBy():</strong> 정렬
     *   <li><strong>fetch():</strong> 리스트 조회
     *   <li><strong>fetchOne():</strong> 단건 조회
     *   <li><strong>fetchFirst():</strong> 첫 번째 결과 조회
     *   <li><strong>fetchCount():</strong> 카운트 조회
     * </ul>
     *
     * <p><strong>동적 쿼리 예시:</strong>
     *
     * <pre>{@code
     * BooleanBuilder builder = new BooleanBuilder();
     *
     * if (status != null) {
     *     builder.and(example.status.eq(status));
     * }
     * if (keyword != null) {
     *     builder.and(example.message.contains(keyword));
     * }
     *
     * return queryFactory
     *     .selectFrom(example)
     *     .where(builder)
     *     .fetch();
     * }</pre>
     *
     * <p><strong>조인 예시:</strong>
     *
     * <pre>{@code
     * QExampleJpaEntity example = QExampleJpaEntity.exampleJpaEntity;
     * QRelatedEntity related = QRelatedEntity.relatedEntity;
     *
     * return queryFactory
     *     .selectFrom(example)
     *     .leftJoin(example.related, related).fetchJoin()
     *     .where(example.id.eq(id))
     *     .fetchOne();
     * }</pre>
     *
     * <p><strong>페이징 예시:</strong>
     *
     * <pre>{@code
     * return queryFactory
     *     .selectFrom(example)
     *     .where(example.status.eq(ExampleStatus.ACTIVE))
     *     .offset(pageable.getOffset())
     *     .limit(pageable.getPageSize())
     *     .fetch();
     * }</pre>
     *
     * <p><strong>주의사항:</strong>
     *
     * <ul>
     *   <li>Q클래스는 컴파일 시 자동 생성됨 (build/generated/sources/annotationProcessor)
     *   <li>Entity 변경 시 재컴파일 필요
     *   <li>복잡한 쿼리는 Native Query보다 QueryDSL 권장
     *   <li>N+1 문제 주의 (fetchJoin 활용)
     * </ul>
     *
     * @param entityManager JPA EntityManager
     * @return JPAQueryFactory 인스턴스
     */
    @Bean
    public JPAQueryFactory jpaQueryFactory(EntityManager entityManager) {
        return new JPAQueryFactory(entityManager);
    }

    /**
     * AuditorAware 빈 등록 (선택 사항)
     *
     * <p>생성자/수정자 정보를 자동으로 설정하려면 AuditorAware를 구현해야 합니다.
     *
     * <p><strong>사용 예시:</strong>
     *
     * <pre>{@code
     * @Bean
     * public AuditorAware<String> auditorProvider() {
     *     return () -> {
     *         // Spring Security에서 현재 사용자 정보 가져오기
     *         Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
     *
     *         if (authentication == null || !authentication.isAuthenticated()) {
     *             return Optional.of("SYSTEM");
     *         }
     *
     *         return Optional.of(authentication.getName());
     *     };
     * }
     * }</pre>
     *
     * <p><strong>Entity 적용:</strong>
     *
     * <pre>{@code
     * @Entity
     * @EntityListeners(AuditingEntityListener.class)
     * public class ExampleJpaEntity {
     *     @CreatedBy
     *     private String createdBy;
     *
     *     @LastModifiedBy
     *     private String lastModifiedBy;
     *
     *     @CreatedDate
     *     private LocalDateTime createdAt;
     *
     *     @LastModifiedDate
     *     private LocalDateTime updatedAt;
     * }
     * }</pre>
     */
    // 필요 시 AuditorAware 구현
    // @Bean
    // public AuditorAware<String> auditorProvider() {
    //     return () -> Optional.of("SYSTEM");
    // }
}
