package com.ryuqq.gateway.adapter.out.redis.entity;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * Public Key Entity (Plain Java, Lombok 금지)
 *
 * <p>Redis에 저장되는 Public Key Entity
 *
 * <p>JSON 직렬화/역직렬화 지원
 *
 * <p><strong>Redis Key</strong>: {@code authhub:jwt:publickey:{kid}}
 *
 * <p><strong>TTL</strong>: 1시간
 *
 * @author development-team
 * @since 1.0.0
 */
public final class PublicKeyEntity {

    private final String kid;
    private final String modulus;
    private final String exponent;
    private final String kty;
    private final String use;
    private final String alg;

    /** Constructor (Jackson 역직렬화용) */
    @JsonCreator
    public PublicKeyEntity(
            @JsonProperty("kid") String kid,
            @JsonProperty("modulus") String modulus,
            @JsonProperty("exponent") String exponent,
            @JsonProperty("kty") String kty,
            @JsonProperty("use") String use,
            @JsonProperty("alg") String alg) {
        this.kid = kid;
        this.modulus = modulus;
        this.exponent = exponent;
        this.kty = kty;
        this.use = use;
        this.alg = alg;
    }

    // Getters (Jackson 직렬화용)
    public String getKid() {
        return kid;
    }

    public String getModulus() {
        return modulus;
    }

    public String getExponent() {
        return exponent;
    }

    public String getKty() {
        return kty;
    }

    public String getUse() {
        return use;
    }

    public String getAlg() {
        return alg;
    }

    @Override
    public String toString() {
        return "PublicKeyEntity{"
                + "kid='"
                + kid
                + '\''
                + ", modulus='"
                + modulus
                + '\''
                + ", exponent='"
                + exponent
                + '\''
                + ", kty='"
                + kty
                + '\''
                + ", use='"
                + use
                + '\''
                + ", alg='"
                + alg
                + '\''
                + '}';
    }
}
