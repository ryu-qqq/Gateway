package com.ryuqq.connectly.domain.sample.vo;

import com.ryuqq.connectly.domain.common.model.ValueObject;
import java.math.BigDecimal;
import java.math.RoundingMode;

/**
 * Sample Money Value Object (예시)
 *
 * <p>금액을 표현하는 Value Object 예시입니다.
 *
 * <p><strong>TODO: 실제 프로젝트에 맞게 수정</strong>
 *
 * <pre>
 * 1. 패키지명 변경: com.ryuqq.domain.sample → com.ryuqq.domain.common.vo (여러 Context 공유)
 * 2. 클래스명 변경: SampleMoney → Money
 * 3. 통화(Currency) 추가 고려
 * 4. 비즈니스 규칙 추가 (할인, 세금 등)
 * </pre>
 *
 * <p><strong>Value Object 패턴:</strong>
 *
 * <ul>
 *   <li>✅ Record 타입 (불변)
 *   <li>✅ ValueObject 인터페이스 구현
 *   <li>✅ of() Factory Method
 *   <li>✅ 생성자 검증
 *   <li>✅ 비즈니스 로직 포함 (add, subtract 등)
 * </ul>
 *
 * @author ryu-qqq
 * @since 2025-11-21
 * @see ValueObject
 */
public record SampleMoney(BigDecimal amount) implements ValueObject {

    /**
     * Compact Constructor - 유효성 검증
     *
     * <p>금액은 null이거나 음수일 수 없습니다.
     *
     * @throws IllegalArgumentException amount가 null이거나 음수인 경우
     */
    public SampleMoney {
        if (amount == null) {
            throw new IllegalArgumentException("Amount cannot be null");
        }
        if (amount.compareTo(BigDecimal.ZERO) < 0) {
            throw new IllegalArgumentException("Amount cannot be negative");
        }
        // 소수점 2자리로 고정
        amount = amount.setScale(2, RoundingMode.HALF_UP);
    }

    /**
     * Factory Method - long 값으로 생성
     *
     * @param amount 금액 (원 단위)
     * @return SampleMoney 인스턴스
     */
    public static SampleMoney of(long amount) {
        return new SampleMoney(BigDecimal.valueOf(amount));
    }

    /**
     * Factory Method - BigDecimal 값으로 생성
     *
     * @param amount 금액
     * @return SampleMoney 인스턴스
     */
    public static SampleMoney of(BigDecimal amount) {
        return new SampleMoney(amount);
    }

    /** Zero Money */
    public static SampleMoney zero() {
        return new SampleMoney(BigDecimal.ZERO);
    }

    // ========================================
    // 비즈니스 로직
    // ========================================

    /**
     * 금액 더하기
     *
     * @param other 더할 금액
     * @return 합계 금액
     */
    public SampleMoney add(SampleMoney other) {
        return new SampleMoney(this.amount.add(other.amount));
    }

    /**
     * 금액 빼기
     *
     * @param other 뺄 금액
     * @return 차액 금액
     * @throws IllegalArgumentException 결과가 음수인 경우
     */
    public SampleMoney subtract(SampleMoney other) {
        BigDecimal result = this.amount.subtract(other.amount);
        if (result.compareTo(BigDecimal.ZERO) < 0) {
            throw new IllegalArgumentException("Result cannot be negative");
        }
        return new SampleMoney(result);
    }

    /**
     * 금액 곱하기
     *
     * @param multiplier 배수
     * @return 곱한 금액
     */
    public SampleMoney multiply(int multiplier) {
        return new SampleMoney(this.amount.multiply(BigDecimal.valueOf(multiplier)));
    }

    /**
     * 금액 비교
     *
     * @param other 비교할 금액
     * @return this가 other보다 크면 true
     */
    public boolean isGreaterThan(SampleMoney other) {
        return this.amount.compareTo(other.amount) > 0;
    }

    /**
     * 금액 비교
     *
     * @param other 비교할 금액
     * @return this가 other보다 크거나 같으면 true
     */
    public boolean isGreaterThanOrEqual(SampleMoney other) {
        return this.amount.compareTo(other.amount) >= 0;
    }

    // TODO: 추가 비즈니스 로직
    // 예: 할인 적용, 세금 계산 등
}
