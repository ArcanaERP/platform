package com.arcanaerp.platform.core.pagination;

import java.util.List;
import java.util.function.Function;
import org.springframework.data.domain.Page;

public record PageResult<T>(
    List<T> items,
    int page,
    int size,
    long totalItems,
    int totalPages,
    boolean hasNext,
    boolean hasPrevious
) {

    public PageResult {
        items = List.copyOf(items);
    }

    public static <T> PageResult<T> from(Page<T> page) {
        return new PageResult<>(
            page.getContent(),
            page.getNumber(),
            page.getSize(),
            page.getTotalElements(),
            page.getTotalPages(),
            page.hasNext(),
            page.hasPrevious()
        );
    }

    public <R> PageResult<R> map(Function<? super T, R> mapper) {
        return new PageResult<>(
            items.stream().map(mapper).toList(),
            page,
            size,
            totalItems,
            totalPages,
            hasNext,
            hasPrevious
        );
    }
}
