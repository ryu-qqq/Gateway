package com.company.connectly.adapter.out.persistence.common.entity;

import jakarta.persistence.Column;
import jakarta.persistence.MappedSuperclass;

import java.time.LocalDateTime;

/**
 * SoftDeletableEntity - 소프트 딜리트 지원 추상 클래스
 *
 * <p>BaseAuditEntity를 상속하여 소프트 딜리트 필드(deletedAt)를 제공합니다.</p>
 *
 * <p><strong>중요: 이 클래스는 데이터 컨테이너일 뿐입니다!</strong></p>
 * <ul>
 *   <li>삭제 여부 판단은 Domain Layer에서 수행</li>
 *   <li>Entity는 deletedAt 필드만 제공</li>
 *   <li>비즈니스 로직은 포함하지 않음</li>
 * </ul>
 *
 * <p><strong>올바른 삭제 흐름:</strong></p>
 * <pre>{@code
 * // 1. Domain Layer: 비즈니스 로직 처리
 * OrderDomain orderDomain = ...;
 * orderDomain.delete();  // 비즈니스 검증 후 deletedAt 설정
 *
 * // 2. Mapper: Domain → Entity 변환
 * OrderJpaEntity entity = mapper.toEntity(orderDomain);
 *
 * // 3. Repository: 저장
 * repository.save(entity);
 * }</pre>
 *
 * <p><strong>사용 여부 결정 기준:</strong></p>
 * <ul>
 *   <li><strong>사용 O:</strong> 삭제 이력 추적 필요, 복구 가능성, GDPR 준수</li>
 *   <li><strong>사용 X:</strong> 임시 데이터, 로그성 데이터, 용량 최적화 필요</li>
 * </ul>
 *
 * <p><strong>Query 작성 시 주의사항:</strong></p>
 * <pre>{@code
 * // QueryDSL: 삭제되지 않은 데이터만 조회
 * return queryFactory
 *     .selectFrom(entity)
 *     .where(entity.deletedAt.isNull())  // 필수 조건
 *     .fetch();
 * }</pre>
 *
 * @author windsurf
 * @since 1.0.0
 */
@MappedSuperclass
public abstract class SoftDeletableEntity extends BaseAuditEntity {

    /**
     * 삭제 일시
     *
     * <p>논리적 삭제 시각을 기록합니다.</p>
     * <ul>
     *   <li>null: 활성 상태 (삭제되지 않음)</li>
     *   <li>not null: 삭제 상태 (삭제된 시각 기록)</li>
     * </ul>
     *
     * <p><strong>인덱스 전략:</strong></p>
     * <ul>
     *   <li>deleted_at IS NULL 조건 자주 사용 시 Partial Index 권장</li>
     *   <li>MySQL 8.0+: CREATE INDEX idx_active ON table (id) WHERE deleted_at IS NULL</li>
     * </ul>
     */
    @Column(name = "deleted_at")
    private LocalDateTime deletedAt;

    /**
     * 기본 생성자 (protected)
     *
     * <p>JPA 스펙 요구사항 및 상속 클래스 전용 생성자입니다.</p>
     */
    protected SoftDeletableEntity() {
        super();
    }

    /**
     * 감사 정보 생성자 (삭제 일시 미포함)
     *
     * <p>상속 클래스에서 감사 필드를 초기화할 때 사용합니다.</p>
     * <p>deletedAt은 null로 초기화됩니다 (활성 상태).</p>
     *
     * @param createdAt 생성 일시
     * @param updatedAt 수정 일시
     */
    protected SoftDeletableEntity(LocalDateTime createdAt, LocalDateTime updatedAt) {
        super(createdAt, updatedAt);
        this.deletedAt = null;  // 기본값: 활성 상태
    }

    /**
     * 전체 필드 생성자 (삭제 일시 포함)
     *
     * <p>Mapper에서 Domain → Entity 변환 시 사용합니다.</p>
     * <p>Domain의 deletedAt 값을 그대로 전달받습니다.</p>
     *
     * @param createdAt 생성 일시
     * @param updatedAt 수정 일시
     * @param deletedAt 삭제 일시 (null이면 활성 상태)
     */
    protected SoftDeletableEntity(
        LocalDateTime createdAt,
        LocalDateTime updatedAt,
        LocalDateTime deletedAt
    ) {
        super(createdAt, updatedAt);
        this.deletedAt = deletedAt;
    }

    /**
     * 삭제 일시 조회
     *
     * @return 삭제 일시 (null이면 활성 상태)
     */
    public LocalDateTime getDeletedAt() {
        return deletedAt;
    }

    /**
     * 삭제 여부 확인
     *
     * <p>deletedAt이 null이 아니면 삭제된 상태로 판단합니다.</p>
     * <p>이 메서드는 단순 상태 조회만 수행하며 비즈니스 로직을 포함하지 않습니다.</p>
     *
     * @return true: 삭제됨, false: 활성 상태
     */
    public boolean isDeleted() {
        return deletedAt != null;
    }

    /**
     * 활성 여부 확인
     *
     * <p>deletedAt이 null이면 활성 상태로 판단합니다.</p>
     * <p>이 메서드는 단순 상태 조회만 수행하며 비즈니스 로직을 포함하지 않습니다.</p>
     *
     * @return true: 활성, false: 삭제됨
     */
    public boolean isActive() {
        return deletedAt == null;
    }
}
