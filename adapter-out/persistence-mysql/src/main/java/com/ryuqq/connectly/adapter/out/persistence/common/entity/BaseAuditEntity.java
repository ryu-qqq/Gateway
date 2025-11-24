package com.ryuqq.connectly.adapter.out.persistence.common.entity;

import jakarta.persistence.Column;
import jakarta.persistence.MappedSuperclass;
import java.time.LocalDateTime;

/**
 * BaseAuditEntity - 감사 정보 공통 추상 클래스
 *
 * <p>모든 JPA 엔티티의 공통 감사 필드(생성일시, 수정일시)를 제공합니다.
 *
 * <p><strong>설계 원칙:</strong>
 *
 * <ul>
 *   <li>@MappedSuperclass: 엔티티가 아닌 매핑 정보만 제공
 *   <li>추상 클래스로 직접 인스턴스화 불가
 *   <li>protected 생성자로 상속 클래스만 접근 가능
 * </ul>
 *
 * <p><strong>필드 불변성 전략:</strong>
 *
 * <ul>
 *   <li>JPA 프록시 생성을 위해 final 사용 안 함
 *   <li>불변성은 비즈니스 로직에서 보장 (setter 미제공)
 *   <li>수정 일시는 markAsUpdated() 메서드로만 변경
 * </ul>
 *
 * <p><strong>사용 예시:</strong>
 *
 * <pre>{@code
 * @Entity
 * @Table(name = "example")
 * public class ExampleJpaEntity extends BaseAuditEntity {
 *     // 엔티티별 고유 필드
 * }
 * }</pre>
 *
 * @author windsurf
 * @since 1.0.0
 */
@MappedSuperclass
public abstract class BaseAuditEntity {

    /**
     * 생성 일시
     *
     * <p>엔티티 최초 생성 시각을 기록합니다.
     *
     * <p>updatable = false로 수정 불가능하게 설정합니다.
     */
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    /**
     * 수정 일시
     *
     * <p>엔티티 최종 수정 시각을 기록합니다.
     *
     * <p>Mapper에서 of() 메서드로 새 Entity 생성 시 updatedAt을 직접 설정합니다.
     */
    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    /**
     * 기본 생성자 (protected)
     *
     * <p>JPA 스펙 요구사항 및 상속 클래스 전용 생성자입니다.
     */
    protected BaseAuditEntity() {}

    /**
     * 감사 정보 생성자
     *
     * <p>상속 클래스에서 감사 필드를 초기화할 때 사용합니다.
     *
     * @param createdAt 생성 일시
     * @param updatedAt 수정 일시
     */
    protected BaseAuditEntity(LocalDateTime createdAt, LocalDateTime updatedAt) {
        this.createdAt = createdAt;
        this.updatedAt = updatedAt;
    }

    /**
     * 생성 일시 조회
     *
     * @return 생성 일시
     */
    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    /**
     * 수정 일시 조회
     *
     * @return 수정 일시
     */
    public LocalDateTime getUpdatedAt() {
        return updatedAt;
    }
}
