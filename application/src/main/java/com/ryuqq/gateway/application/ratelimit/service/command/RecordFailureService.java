package com.ryuqq.gateway.application.ratelimit.service.command;

import com.ryuqq.gateway.application.ratelimit.dto.command.RecordFailureCommand;
import com.ryuqq.gateway.application.ratelimit.internal.FailureRecordCoordinator;
import com.ryuqq.gateway.application.ratelimit.port.in.command.RecordFailureUseCase;
import com.ryuqq.observability.logging.annotation.Loggable;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;

/**
 * 실패 기록 Service
 *
 * <p>FailureRecordCoordinator를 통해 실패 기록을 수행하는 서비스
 *
 * <p><strong>의존성 방향</strong>:
 *
 * <pre>
 * RecordFailureService (Application Service - UseCase 구현)
 *   ↓ (calls)
 * FailureRecordCoordinator (Application Coordinator - internal)
 *   ↓ (calls)
 * Manager → Port
 * </pre>
 *
 * @author development-team
 * @since 1.0.0
 */
@Service
public class RecordFailureService implements RecordFailureUseCase {

    private final FailureRecordCoordinator failureRecordCoordinator;

    public RecordFailureService(FailureRecordCoordinator failureRecordCoordinator) {
        this.failureRecordCoordinator = failureRecordCoordinator;
    }

    /**
     * 실패 기록 실행
     *
     * @param command RecordFailureCommand
     * @return Mono&lt;Void&gt;
     */
    @Loggable(value = "실패 기록", includeArgs = false, slowThreshold = 100)
    @Override
    public Mono<Void> execute(RecordFailureCommand command) {
        return failureRecordCoordinator.record(command);
    }
}
