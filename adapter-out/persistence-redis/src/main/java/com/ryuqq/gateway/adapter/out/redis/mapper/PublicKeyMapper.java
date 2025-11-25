package com.ryuqq.gateway.adapter.out.redis.mapper;

import com.ryuqq.gateway.adapter.out.redis.entity.PublicKeyEntity;
import com.ryuqq.gateway.domain.authentication.vo.PublicKey;
import org.springframework.stereotype.Component;

/**
 * Public Key Mapper
 *
 * <p>PublicKey (Domain VO) ↔ PublicKeyEntity (Redis Entity) 양방향 매핑
 *
 * <p><strong>책임</strong>:
 *
 * <ul>
 *   <li>Domain VO → Redis Entity 변환
 *   <li>Redis Entity → Domain VO 변환
 * </ul>
 *
 * @author development-team
 * @since 1.0.0
 */
@Component
public class PublicKeyMapper {

    /**
     * Redis Entity → Domain VO
     *
     * @param entity PublicKeyEntity
     * @return PublicKey
     */
    public PublicKey toPublicKey(PublicKeyEntity entity) {
        if (entity == null) {
            return null;
        }

        return new PublicKey(
                entity.getKid(),
                entity.getModulus(),
                entity.getExponent(),
                entity.getKty(),
                entity.getUse(),
                entity.getAlg());
    }

    /**
     * Domain VO → Redis Entity
     *
     * @param publicKey PublicKey
     * @return PublicKeyEntity
     */
    public PublicKeyEntity toPublicKeyEntity(PublicKey publicKey) {
        if (publicKey == null) {
            return null;
        }

        return new PublicKeyEntity(
                publicKey.kid(),
                publicKey.modulus(),
                publicKey.exponent(),
                publicKey.kty(),
                publicKey.use(),
                publicKey.alg());
    }
}
