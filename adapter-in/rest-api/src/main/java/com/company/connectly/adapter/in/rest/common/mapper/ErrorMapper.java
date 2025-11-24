package com.company.connectly.adapter.in.rest.common.mapper;

import com.company.connectly.domain.common.exception.DomainException;

import java.net.URI;
import java.util.Locale;

import org.springframework.http.HttpStatus;

public interface ErrorMapper {

    boolean supports(String code);

    /** DomainException을 HTTP 응답용으로 매핑 */
    MappedError map(DomainException ex, Locale locale);

    /** 매핑 결과 DTO */
    record MappedError(HttpStatus status, String title, String detail, URI type) {}

}
