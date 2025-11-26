package com.ryuqq.gateway.domain.authorization.vo;

import java.util.Objects;
import java.util.regex.Pattern;

/**
 * Permission - 권한 Value Object
 *
 * <p>권한 문자열을 나타내는 불변 객체입니다.
 *
 * <p><strong>권한 형식:</strong>
 *
 * <ul>
 *   <li>형식: {resource}:{action}
 *   <li>예시: order:read, order:create, order:delete
 *   <li>와일드카드: order:* (모든 order 액션 포함)
 * </ul>
 *
 * @author development-team
 * @since 1.0.0
 */
public record Permission(String value) {

    private static final Pattern PERMISSION_PATTERN =
            Pattern.compile("^[a-z][a-z0-9-]*:[a-z*][a-z0-9-*]*$");
    private static final String WILDCARD = "*";

    public Permission {
        Objects.requireNonNull(value, "Permission value cannot be null");
        if (value.isBlank()) {
            throw new IllegalArgumentException("Permission value cannot be blank");
        }
        if (!PERMISSION_PATTERN.matcher(value).matches()) {
            throw new IllegalArgumentException(
                    "Invalid permission format: "
                            + value
                            + ". Expected format: {resource}:{action}");
        }
    }

    /**
     * 정적 팩토리 메서드
     *
     * @param value 권한 문자열
     * @return Permission 객체
     */
    public static Permission of(String value) {
        return new Permission(value);
    }

    /**
     * 리소스 부분 추출
     *
     * @return 리소스 문자열 (예: "order")
     */
    public String resource() {
        return value.split(":")[0];
    }

    /**
     * 액션 부분 추출
     *
     * @return 액션 문자열 (예: "read", "*")
     */
    public String action() {
        return value.split(":")[1];
    }

    /**
     * 와일드카드 권한 여부 확인
     *
     * @return 와일드카드(*) 액션을 가진 경우 true
     */
    public boolean isWildcard() {
        return WILDCARD.equals(action());
    }

    /**
     * 다른 권한을 포함하는지 확인 (와일드카드 매칭)
     *
     * <p>예시:
     *
     * <ul>
     *   <li>order:* includes order:read → true
     *   <li>order:read includes order:read → true
     *   <li>order:read includes order:create → false
     * </ul>
     *
     * @param other 비교할 권한
     * @return 포함 여부
     */
    public boolean includes(Permission other) {
        if (other == null) {
            return false;
        }
        if (!this.resource().equals(other.resource())) {
            return false;
        }
        if (this.isWildcard()) {
            return true;
        }
        return this.action().equals(other.action());
    }
}
