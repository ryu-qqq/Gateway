package com.ryuqq.gateway.application.architecture;

/**
 * Application Layer ArchUnit 테스트 공통 패키지 상수
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
    public static final String BASE_PACKAGE = "com.ryuqq.fileflow";

    // ========================================================================
    // 레이어별 패키지 (자동 파생 - 수정 불필요)
    // ========================================================================

    /** Application Layer 패키지 */
    public static final String APPLICATION = BASE_PACKAGE + ".application";

    /** Domain Layer 패키지 */
    public static final String DOMAIN = BASE_PACKAGE + ".domain";

    /** Adapter Layer 패키지 */
    public static final String ADAPTER = BASE_PACKAGE + ".adapter";

    // ========================================================================
    // 패키지 패턴 (ArchUnit 규칙에서 사용)
    // ========================================================================

    /** Application 전체 패키지 패턴 */
    public static final String APPLICATION_ALL = APPLICATION + "..";

    /** Domain 전체 패키지 패턴 */
    public static final String DOMAIN_ALL = DOMAIN + "..";

    /** Adapter 전체 패키지 패턴 */
    public static final String ADAPTER_ALL = ADAPTER + "..";

    // ========================================================================
    // Application 서브 패키지 패턴
    // ========================================================================

    /** Service 패키지 패턴 */
    public static final String SERVICE_PATTERN = "..service..";

    /** Port 패키지 패턴 */
    public static final String PORT_PATTERN = "..port..";

    /** Port In 패키지 패턴 */
    public static final String PORT_IN_PATTERN = "..port.in..";

    /** Port Out 패키지 패턴 */
    public static final String PORT_OUT_PATTERN = "..port.out..";

    /** DTO 패키지 패턴 */
    public static final String DTO_PATTERN = "..dto..";

    /** Command DTO 패키지 패턴 */
    public static final String DTO_COMMAND_PATTERN = "..dto.command..";

    /** Query DTO 패키지 패턴 */
    public static final String DTO_QUERY_PATTERN = "..dto.query..";

    /** Response DTO 패키지 패턴 */
    public static final String DTO_RESPONSE_PATTERN = "..dto.response..";

    /** Factory 패키지 패턴 */
    public static final String FACTORY_PATTERN = "..factory..";

    /** Assembler 패키지 패턴 */
    public static final String ASSEMBLER_PATTERN = "..assembler..";

    /** Manager 패키지 패턴 */
    public static final String MANAGER_PATTERN = "..manager..";

    /** Facade 패키지 패턴 */
    public static final String FACADE_PATTERN = "..facade..";

    /** Listener 패키지 패턴 */
    public static final String LISTENER_PATTERN = "..listener..";

    /** Event 패키지 패턴 */
    public static final String EVENT_PATTERN = "..event..";

    /** Common 패키지 패턴 */
    public static final String COMMON_PATTERN = "..common..";

    /** Architecture 패키지 패턴 (테스트 제외용) */
    public static final String ARCHITECTURE_PATTERN = "..architecture..";

    private ArchUnitPackageConstants() {
        // 인스턴스화 방지
    }
}
