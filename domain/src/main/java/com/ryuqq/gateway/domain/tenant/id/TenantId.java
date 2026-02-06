package com.ryuqq.gateway.domain.tenant.id;

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
 * </ul>
 *
 * @param value 테넌트 ID 문자열
 * @author development-team
 * @since 1.0.0
 */
public record TenantId(String value) {

    private static final Pattern TENANT_PATTERN = Pattern.compile("^tenant-\\d+$");

    private static final Pattern UUID_PATTERN =
            Pattern.compile(
                    "^[a-f0-9]{8}-[a-f0-9]{4}-[a-f0-9]{4}-[a-f0-9]{4}-[a-f0-9]{12}$",
                    Pattern.CASE_INSENSITIVE);

    /**
     * TenantId 생성 (형식 검증 포함)
     *
     * @param value 테넌트 ID 문자열
     * @return TenantId 인스턴스
     * @throws IllegalArgumentException 형식이 올바르지 않은 경우
     */
    public static TenantId of(String value) {
        validate(value);
        return new TenantId(value);
    }

    /**
     * 형식 검증 없이 TenantId 생성 (신뢰할 수 있는 소스용)
     *
     * @param value 테넌트 ID 문자열
     * @return TenantId 인스턴스
     */
    public static TenantId from(String value) {
        return new TenantId(value);
    }

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

    private static boolean isValidFormat(String value) {
        return TENANT_PATTERN.matcher(value).matches() || UUID_PATTERN.matcher(value).matches();
    }

    public boolean isUuidFormat() {
        return UUID_PATTERN.matcher(value).matches();
    }

    public boolean isTenantNumberFormat() {
        return TENANT_PATTERN.matcher(value).matches();
    }

    @Override
    public String toString() {
        return "TenantId{value='" + value + "'}";
    }
}
