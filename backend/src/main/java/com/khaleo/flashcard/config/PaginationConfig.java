package com.khaleo.flashcard.config;

import com.khaleo.flashcard.service.persistence.PersistenceValidationExceptionMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class PaginationConfig {

    public static final int DEFAULT_PAGE = 0;
    public static final int DEFAULT_SIZE = 20;
    public static final int MAX_SIZE = 100;

    private final PersistenceValidationExceptionMapper exceptionMapper;

    public Pageable resolvePageable(Integer page, Integer size) {
        int resolvedPage = page == null ? DEFAULT_PAGE : page;
        int resolvedSize = size == null ? DEFAULT_SIZE : size;

        if (resolvedPage < 0 || resolvedSize < 1 || resolvedSize > MAX_SIZE) {
            throw exceptionMapper.invalidPagination(page, size);
        }

        return PageRequest.of(resolvedPage, resolvedSize);
    }
}
