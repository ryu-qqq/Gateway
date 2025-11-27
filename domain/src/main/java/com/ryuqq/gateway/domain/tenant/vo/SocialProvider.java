package com.ryuqq.gateway.domain.tenant.vo;

/**
 * SocialProvider - 소셜 로그인 제공자 열거형
 *
 * <p>Tenant별로 허용되는 소셜 로그인 제공자를 정의합니다.
 *
 * <p><strong>지원 제공자:</strong>
 *
 * <ul>
 *   <li>KAKAO: 카카오 로그인
 *   <li>NAVER: 네이버 로그인
 *   <li>GOOGLE: 구글 로그인
 * </ul>
 *
 * <p><strong>사용 예시:</strong>
 *
 * <pre>{@code
 * Set<SocialProvider> allowedProviders = Set.of(SocialProvider.KAKAO, SocialProvider.GOOGLE);
 * if (!allowedProviders.contains(SocialProvider.NAVER)) {
 *     throw new SocialLoginNotAllowedException(tenantId, "naver");
 * }
 * }</pre>
 *
 * @author development-team
 * @since 1.0.0
 */
public enum SocialProvider {

    /** 카카오 로그인 */
    KAKAO("kakao", "Kakao"),

    /** 네이버 로그인 */
    NAVER("naver", "Naver"),

    /** 구글 로그인 */
    GOOGLE("google", "Google");

    private final String code;
    private final String displayName;

    /**
     * Constructor - SocialProvider 생성
     *
     * @param code 소문자 코드 (API에서 사용)
     * @param displayName 표시용 이름
     * @author development-team
     * @since 1.0.0
     */
    SocialProvider(String code, String displayName) {
        this.code = code;
        this.displayName = displayName;
    }

    /**
     * 소문자 코드 반환
     *
     * @return 소문자 코드 (예: "kakao", "naver", "google")
     * @author development-team
     * @since 1.0.0
     */
    public String getCode() {
        return code;
    }

    /**
     * 표시용 이름 반환
     *
     * @return 표시용 이름 (예: "Kakao", "Naver", "Google")
     * @author development-team
     * @since 1.0.0
     */
    public String getDisplayName() {
        return displayName;
    }

    /**
     * 코드로 SocialProvider 조회
     *
     * <p>대소문자 구분 없이 조회합니다.
     *
     * @param code 소셜 제공자 코드
     * @return 해당 SocialProvider
     * @throws IllegalArgumentException 존재하지 않는 코드인 경우
     * @author development-team
     * @since 1.0.0
     */
    public static SocialProvider fromCode(String code) {
        if (code == null || code.isBlank()) {
            throw new IllegalArgumentException("Social provider code cannot be null or blank");
        }

        String normalizedCode = code.toLowerCase();
        for (SocialProvider provider : values()) {
            if (provider.code.equals(normalizedCode)) {
                return provider;
            }
        }

        throw new IllegalArgumentException(String.format("Unknown social provider code: %s", code));
    }

    /**
     * 이름으로 SocialProvider 조회 (Enum.valueOf 대체)
     *
     * <p>대소문자 구분 없이 조회합니다.
     *
     * @param name 소셜 제공자 이름
     * @return 해당 SocialProvider
     * @throws IllegalArgumentException 존재하지 않는 이름인 경우
     * @author development-team
     * @since 1.0.0
     */
    public static SocialProvider fromName(String name) {
        if (name == null || name.isBlank()) {
            throw new IllegalArgumentException("Social provider name cannot be null or blank");
        }

        String normalizedName = name.toUpperCase();
        for (SocialProvider provider : values()) {
            if (provider.name().equals(normalizedName)) {
                return provider;
            }
        }

        throw new IllegalArgumentException(String.format("Unknown social provider name: %s", name));
    }
}
