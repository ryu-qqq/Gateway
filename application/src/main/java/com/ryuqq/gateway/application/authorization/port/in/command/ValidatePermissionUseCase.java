package com.ryuqq.gateway.application.authorization.port.in.command;

import com.ryuqq.gateway.application.authorization.dto.command.ValidatePermissionCommand;
import com.ryuqq.gateway.application.authorization.dto.response.ValidatePermissionResponse;
import reactor.core.publisher.Mono;

/**
 * ValidatePermissionUseCase - 권한 검증 UseCase
 *
 * <p>사용자의 요청에 대한 권한을 검증합니다.
 *
 * <p><strong>검증 로직:</strong>
 *
 * <ol>
 *   <li>Permission Spec에서 엔드포인트 권한 조회
 *   <li>Public 엔드포인트인 경우 바로 통과
 *   <li>Permission Hash 비교 (JWT vs Cache)
 *   <li>Required Permissions 검증
 * </ol>
 *
 * @author development-team
 * @since 1.0.0
 */
public interface ValidatePermissionUseCase {

    /**
     * 권한 검증 실행
     *
     * @param command 권한 검증 요청
     * @return 권한 검증 결과
     */
    Mono<ValidatePermissionResponse> execute(ValidatePermissionCommand command);
}
