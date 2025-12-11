package com.ryuqq.gateway.domain.architecture;

/**
 * Domain Layer ArchUnit 테스트 공통 패키지 상수
 *
 * <p>다른 프로젝트에 템플릿을 적용할 때 {@link #BASE_PACKAGE} 값만 변경하면 됩니다.
 *
 * @author development-team
 * @since 1.0.0
 */
public final class ArchUnitPackageConstants {

    // ========================================================================
    // 프로젝트 적용 시 이 값만 변경하세요
    // ========================================================================

    /** 프로젝트 기본 패키지 (루트 패키지) */
    public static final String BASE_PACKAGE = "com.ryuqq.gateway";

    // ========================================================================
    // 레이어별 패키지 (자동 파생 - 수정 불필요)
    // ========================================================================

    /** Domain Layer 패키지 */
    public static final String DOMAIN = BASE_PACKAGE + ".domain";

    /** Domain Common 패키지 */
    public static final String DOMAIN_COMMON = DOMAIN + ".common";

    /** Application Layer 패키지 */
    public static final String APPLICATION = BASE_PACKAGE + ".application";

    /** Adapter Layer 패키지 */
    public static final String ADAPTER = BASE_PACKAGE + ".adapter";

    /** Bootstrap Layer 패키지 */
    public static final String BOOTSTRAP = BASE_PACKAGE + ".bootstrap";

    /** Persistence Layer 패키지 */
    public static final String PERSISTENCE = BASE_PACKAGE + ".persistence";

    // ========================================================================
    // 패키지 패턴 (ArchUnit 규칙에서 사용)
    // ========================================================================

    /** Domain 전체 패키지 패턴 */
    public static final String DOMAIN_ALL = DOMAIN + "..";

    /** Application 전체 패키지 패턴 */
    public static final String APPLICATION_ALL = APPLICATION + "..";

    /** Adapter 전체 패키지 패턴 */
    public static final String ADAPTER_ALL = ADAPTER + "..";

    /** Bootstrap 전체 패키지 패턴 */
    public static final String BOOTSTRAP_ALL = BOOTSTRAP + "..";

    /** Persistence 전체 패키지 패턴 */
    public static final String PERSISTENCE_ALL = PERSISTENCE + "..";

    // ========================================================================
    // Domain 서브 패키지 패턴
    // ========================================================================

    /** Aggregate 패키지 패턴 */
    public static final String AGGREGATE_PATTERN = "..aggregate..";

    /** VO 패키지 패턴 */
    public static final String VO_PATTERN = "..vo..";

    /** Event 패키지 패턴 */
    public static final String EVENT_PATTERN = "..event..";

    /** Exception 패키지 패턴 */
    public static final String EXCEPTION_PATTERN = "..exception..";

    /** Criteria 패키지 패턴 */
    public static final String CRITERIA_PATTERN = "..criteria..";

    /** Common 패키지 패턴 */
    public static final String COMMON_PATTERN = "..common..";

    /** Architecture 패키지 패턴 (테스트 제외용) */
    public static final String ARCHITECTURE_PATTERN = "..architecture..";

    private ArchUnitPackageConstants() {
        // 인스턴스화 방지
    }
}
