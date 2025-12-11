package com.ryuqq.gateway.domain.tenant.vo;

import java.util.Objects;
import java.util.regex.Pattern;

/**
 * TenantId - 테넌트 식별자 Value Object
 *
 * <p>멀티테넌트 시스템에서 테넌트를 고유하게 식별하는 불변 객체입니다.
 *
 * <p><strong>형식 규칙:</strong>
 *
 * <ul>
 *   <li>"tenant-{숫자}" 형식 (예: tenant-1, tenant-123)
 *   <li>UUID 형식 (예: 550e8400-e29b-41d4-a716-446655440000)
 *   <li>null 또는 빈 문자열 불허
 * </ul>
 *
 * <p><strong>사용 예시:</strong>
 *
 * <pre>{@code
 * TenantId tenantId = TenantId.of("tenant-1");
 * String value = tenantId.value(); // "tenant-1"
 * }</pre>
 *
 * @param value 테넌트 ID 문자열
 * @author development-team
 * @since 1.0.0
 */
public record TenantId(String value) {

    /** "tenant-{숫자}" 형식 검증 정규식 */
    private static final Pattern TENANT_PATTERN = Pattern.compile("^tenant-\\d+$");

    /** UUID 형식 검증 정규식 */
    private static final Pattern UUID_PATTERN =
            Pattern.compile(
                    "^[a-f0-9]{8}-[a-f0-9]{4}-[a-f0-9]{4}-[a-f0-9]{4}-[a-f0-9]{12}$",
                    Pattern.CASE_INSENSITIVE);

    /** Compact Constructor (검증 로직) */
    public TenantId {
        Objects.requireNonNull(value, "TenantId cannot be null");
        if (value.isBlank()) {
            throw new IllegalArgumentException("TenantId cannot be blank");
        }
    }

    /**
     * TenantId 생성
     *
     * <p>형식 검증 후 TenantId 인스턴스를 생성합니다.
     *
     * @param value 테넌트 ID 문자열
     * @return TenantId 인스턴스
     * @throws IllegalArgumentException 형식이 올바르지 않은 경우
     * @author development-team
     * @since 1.0.0
     */
    public static TenantId of(String value) {
        validate(value);
        return new TenantId(value);
    }

    /**
     * 형식 검증 없이 TenantId 생성 (내부용)
     *
     * <p>이미 검증된 값에 대해서만 사용합니다.
     *
     * @param value 테넌트 ID 문자열
     * @return TenantId 인스턴스
     * @author development-team
     * @since 1.0.0
     */
    public static TenantId from(String value) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException("TenantId cannot be null or blank");
        }
        return new TenantId(value);
    }

    /**
     * TenantId 형식 검증
     *
     * @param value 검증할 테넌트 ID 문자열
     * @throws IllegalArgumentException 유효하지 않은 경우
     */
    private static void validate(String value) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException("TenantId cannot be null or blank");
        }

        if (!isValidFormat(value)) {
            throw new IllegalArgumentException(
                    String.format(
                            "Invalid TenantId format: %s. Expected 'tenant-{number}' or UUID"
                                    + " format",
                            value));
        }
    }

    /**
     * 형식 유효성 검사
     *
     * @param value 검증할 값
     * @return 유효하면 true
     */
    private static boolean isValidFormat(String value) {
        return TENANT_PATTERN.matcher(value).matches() || UUID_PATTERN.matcher(value).matches();
    }

    /**
     * UUID 형식인지 확인
     *
     * @return UUID 형식이면 true
     * @author development-team
     * @since 1.0.0
     */
    public boolean isUuidFormat() {
        return UUID_PATTERN.matcher(value).matches();
    }

    /**
     * "tenant-{숫자}" 형식인지 확인
     *
     * @return "tenant-{숫자}" 형식이면 true
     * @author development-team
     * @since 1.0.0
     */
    public boolean isTenantNumberFormat() {
        return TENANT_PATTERN.matcher(value).matches();
    }

    @Override
    public String toString() {
        return "TenantId{" + "value='" + value + '\'' + '}';
    }
}
