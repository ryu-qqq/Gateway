package com.company.connectly.fixture.domain;

import com.company.connectly.domain.sample.vo.SampleMoney;

import java.math.BigDecimal;

/**
 * SampleMoney Domain 객체 Test Fixture (domain testFixtures)
 *
 * <p><strong>Fixture 패턴:</strong></p>
 * <ul>
 *   <li>✅ forNew(): 기본 테스트 값 생성</li>
 *   <li>✅ of(): 특정 값으로 테스트 데이터 생성</li>
 *   <li>✅ reconstitute(): DB에서 조회한 것처럼 테스트 데이터 생성</li>
 *   <li>❌ create*() 메서드 금지</li>
 * </ul>
 *
 * @author development-team
 * @since 1.0.0
 */
public class SampleMoneyFixture {

    private SampleMoneyFixture() {
        throw new AssertionError("Utility class");
    }

    /**
     * Factory Method - 기본 테스트 Money 생성
     *
     * <p>테스트 시 기본 금액(1,000원)을 사용합니다.</p>
     *
     * @return SampleMoney 인스턴스 (1,000원)
     */
    public static SampleMoney forNew() {
        return SampleMoney.of(1000L);
    }

    /**
     * Factory Method - 특정 값으로 Money 생성
     *
     * @param amount 금액 (long)
     * @return SampleMoney 인스턴스
     */
    public static SampleMoney of(long amount) {
        return SampleMoney.of(amount);
    }

    /**
     * Factory Method - 특정 값으로 Money 생성
     *
     * @param amount 금액 (BigDecimal)
     * @return SampleMoney 인스턴스
     */
    public static SampleMoney of(BigDecimal amount) {
        return SampleMoney.of(amount);
    }

    /**
     * Factory Method - DB 재구성 시뮬레이션
     *
     * <p>DB에서 조회한 Money를 재구성하는 경우를 시뮬레이션합니다.</p>
     * <p>Value Object이므로 of()와 동일합니다.</p>
     *
     * @param amount 금액
     * @return SampleMoney 인스턴스
     */
    public static SampleMoney reconstitute(long amount) {
        return SampleMoney.of(amount);
    }

    /**
     * Factory Method - DB 재구성 시뮬레이션
     *
     * @param amount 금액 (BigDecimal)
     * @return SampleMoney 인스턴스
     */
    public static SampleMoney reconstitute(BigDecimal amount) {
        return SampleMoney.of(amount);
    }

    /**
     * Zero Money Fixture (0원)
     *
     * @return SampleMoney 인스턴스 (0원)
     */
    public static SampleMoney zero() {
        return SampleMoney.zero();
    }
}
