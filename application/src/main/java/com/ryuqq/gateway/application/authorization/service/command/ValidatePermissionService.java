package com.ryuqq.gateway.application.authorization.service.command;

import com.ryuqq.gateway.application.authorization.dto.command.ValidatePermissionCommand;
import com.ryuqq.gateway.application.authorization.dto.response.ValidatePermissionResponse;
import com.ryuqq.gateway.application.authorization.internal.PermissionValidationCoordinator;
import com.ryuqq.gateway.application.authorization.port.in.command.ValidatePermissionUseCase;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;

/**
 * 권한 검증 Service
 *
 * <p>PermissionValidationCoordinator를 통해 권한 검증을 수행하는 서비스
 *
 * <p><strong>의존성 방향</strong>:
 *
 * <pre>
 * ValidatePermissionService (Application Service - UseCase 구현)
 *   ↓ (calls)
 * PermissionValidationCoordinator (Application Coordinator - internal)
 *   ↓ (calls)
 * PermissionSpecCoordinator + PermissionHashCoordinator
 * </pre>
 *
 * @author development-team
 * @since 1.0.0
 */
@Service
public class ValidatePermissionService implements ValidatePermissionUseCase {

    private final PermissionValidationCoordinator permissionValidationCoordinator;

    public ValidatePermissionService(
            PermissionValidationCoordinator permissionValidationCoordinator) {
        this.permissionValidationCoordinator = permissionValidationCoordinator;
    }

    /**
     * 권한 검증 실행
     *
     * @param command 검증 요청 (경로, 메서드, 테넌트, 사용자, permissionHash)
     * @return 검증 결과
     */
    @Override
    public Mono<ValidatePermissionResponse> execute(ValidatePermissionCommand command) {
        return permissionValidationCoordinator.validate(command);
    }
}
