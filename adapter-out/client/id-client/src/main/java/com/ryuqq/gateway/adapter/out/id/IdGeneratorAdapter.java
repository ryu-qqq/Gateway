package com.ryuqq.gateway.adapter.out.id;

import com.ryuqq.gateway.application.trace.port.out.client.IdGeneratorPort;
import java.util.UUID;
import org.springframework.stereotype.Component;

/**
 * IdGeneratorAdapter - UUID 생성 어댑터
 *
 * @author development-team
 * @since 1.0.0
 */
@Component
public class IdGeneratorAdapter implements IdGeneratorPort {

    @Override
    public String generateUuid() {
        return UUID.randomUUID().toString().toLowerCase();
    }
}
