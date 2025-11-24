package com.ryuqq.gateway.domain.common.exception;

import java.util.Map;

// domain 모듈
public class DomainException extends RuntimeException {

    private final String code; // ex) TENANT_NOT_FOUND
    private final Map<String, Object> args; // 메시지 템플릿 파라미터 등 (선택)

    public DomainException(String code, String message) {
        super(message);
        this.code = code;
        this.args = Map.of();
    }

    public DomainException(String code, String message, Map<String, Object> args) {
        super(message);
        this.code = code;
        this.args = args == null ? Map.of() : Map.copyOf(args);
    }

    public String code() {
        return code;
    }

    public Map<String, Object> args() {
        return args;
    }
}
