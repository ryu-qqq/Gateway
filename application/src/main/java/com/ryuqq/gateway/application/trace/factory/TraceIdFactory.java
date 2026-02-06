package com.ryuqq.gateway.application.trace.factory;

import com.ryuqq.gateway.application.common.time.TimeProvider;
import com.ryuqq.gateway.application.trace.port.out.client.IdGeneratorPort;
import com.ryuqq.gateway.domain.trace.id.TraceId;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import org.springframework.stereotype.Component;

/**
 * TraceIdFactory - Trace-ID 생성 팩토리
 *
 * @author development-team
 * @since 1.0.0
 */
@Component
public class TraceIdFactory {

    private static final String TIMESTAMP_FORMAT = "yyyyMMddHHmmssSSS";
    private static final DateTimeFormatter TIMESTAMP_FORMATTER =
            DateTimeFormatter.ofPattern(TIMESTAMP_FORMAT);

    private final TimeProvider timeProvider;
    private final IdGeneratorPort idGeneratorPort;

    public TraceIdFactory(TimeProvider timeProvider, IdGeneratorPort idGeneratorPort) {
        this.timeProvider = timeProvider;
        this.idGeneratorPort = idGeneratorPort;
    }

    public TraceId create() {
        ZonedDateTime now = timeProvider.now().atZone(java.time.ZoneId.systemDefault());
        String timestamp = now.format(TIMESTAMP_FORMATTER);
        String uuid = idGeneratorPort.generateUuid();
        return TraceId.of(timestamp, uuid);
    }
}
