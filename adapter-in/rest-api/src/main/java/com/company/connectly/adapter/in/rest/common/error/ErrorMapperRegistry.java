package com.company.connectly.adapter.in.rest.common.error;

import com.company.connectly.adapter.in.rest.common.mapper.ErrorMapper;
import com.company.connectly.domain.common.exception.DomainException;

import java.net.URI;
import java.util.List;
import java.util.Locale;
import java.util.Optional;

import org.springframework.http.HttpStatus;

public class ErrorMapperRegistry {
    private final List<ErrorMapper> mappers;

    public ErrorMapperRegistry(List<ErrorMapper> mappers) {
        this.mappers = mappers;
    }

    public Optional<ErrorMapper.MappedError> map(DomainException ex, Locale locale) {
        return mappers.stream()
            .filter(m -> m.supports(ex.code()))
            .findFirst()
            .map(m -> m.map(ex, locale));
    }

    public ErrorMapper.MappedError defaultMapping(DomainException ex) {
        return new ErrorMapper.MappedError(
            HttpStatus.BAD_REQUEST,
            "Bad Request",
            ex.getMessage() != null ? ex.getMessage() : "Invalid request",
            URI.create("about:blank")
        );
    }

}
