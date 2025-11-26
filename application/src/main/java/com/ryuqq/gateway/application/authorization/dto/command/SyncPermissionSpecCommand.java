package com.ryuqq.gateway.application.authorization.dto.command;

import java.util.List;
import java.util.Objects;

/**
 * SyncPermissionSpecCommand - Permission Spec 동기화 요청 Command
 *
 * @param version 새 Spec 버전
 * @param changedServices 변경된 서비스 목록 (선택)
 * @author development-team
 * @since 1.0.0
 */
public record SyncPermissionSpecCommand(Long version, List<String> changedServices) {

    public SyncPermissionSpecCommand {
        Objects.requireNonNull(version, "version cannot be null");
        changedServices = changedServices == null ? List.of() : List.copyOf(changedServices);
    }

    public static SyncPermissionSpecCommand of(Long version, List<String> changedServices) {
        return new SyncPermissionSpecCommand(version, changedServices);
    }

    public static SyncPermissionSpecCommand of(Long version) {
        return new SyncPermissionSpecCommand(version, List.of());
    }
}
