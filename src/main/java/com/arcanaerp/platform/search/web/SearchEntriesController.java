package com.arcanaerp.platform.search.web;

import com.arcanaerp.platform.core.pagination.PageQuery;
import com.arcanaerp.platform.core.pagination.PageResult;
import com.arcanaerp.platform.search.RegisterSearchEntryCommand;
import com.arcanaerp.platform.search.SearchCatalog;
import com.arcanaerp.platform.search.SearchEntryView;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/search/entries")
@RequiredArgsConstructor
public class SearchEntriesController {

    private final SearchCatalog searchCatalog;

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public SearchEntryResponse registerSearchEntry(@Valid @RequestBody CreateSearchEntryRequest request) {
        SearchEntryView created = searchCatalog.registerSearchEntry(
            new RegisterSearchEntryCommand(
                request.tenantCode(),
                request.entryNumber(),
                request.title(),
                request.snippet(),
                request.category(),
                request.targetType(),
                request.targetIdentifier(),
                request.targetUri()
            )
        );
        return toResponse(created);
    }

    @GetMapping("/{entryNumber}")
    public SearchEntryResponse getSearchEntry(
        @PathVariable String entryNumber,
        @RequestParam String tenantCode
    ) {
        return toResponse(searchCatalog.getSearchEntry(tenantCode, entryNumber));
    }

    @GetMapping
    public PageResult<SearchEntryResponse> listSearchEntries(
        @RequestParam String tenantCode,
        @RequestParam(required = false) String query,
        @RequestParam(required = false) String category,
        @RequestParam(required = false) Integer page,
        @RequestParam(required = false) Integer size
    ) {
        return searchCatalog.listSearchEntries(
            tenantCode,
            PageQuery.of(page, size),
            query,
            normalizeOptionalCategory(category)
        ).map(this::toResponse);
    }

    private SearchEntryResponse toResponse(SearchEntryView view) {
        return new SearchEntryResponse(
            view.id(),
            view.tenantCode(),
            view.entryNumber(),
            view.title(),
            view.snippet(),
            view.category(),
            view.targetType(),
            view.targetIdentifier(),
            view.targetUri(),
            view.createdAt()
        );
    }

    private static String normalizeOptionalCategory(String category) {
        if (category == null) {
            return null;
        }
        if (category.isBlank()) {
            throw new IllegalArgumentException("category query parameter must not be blank");
        }
        return category.trim().toUpperCase();
    }
}
