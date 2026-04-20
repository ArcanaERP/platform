package com.arcanaerp.platform.search.internal;

import com.arcanaerp.platform.core.api.ConflictException;
import com.arcanaerp.platform.core.pagination.PageQuery;
import com.arcanaerp.platform.core.pagination.PageResult;
import com.arcanaerp.platform.search.RegisterSearchEntryCommand;
import com.arcanaerp.platform.search.SearchCatalog;
import com.arcanaerp.platform.search.SearchEntryView;
import java.time.Clock;
import java.time.Instant;
import java.util.NoSuchElementException;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional
@RequiredArgsConstructor
class SearchCatalogService implements SearchCatalog {

    private final SearchEntryRepository searchEntryRepository;
    private final Clock clock;

    @Override
    public SearchEntryView registerSearchEntry(RegisterSearchEntryCommand command) {
        String tenantCode = normalizeRequired(command.tenantCode(), "tenantCode").toUpperCase();
        String entryNumber = normalizeRequired(command.entryNumber(), "entryNumber").toUpperCase();
        Instant now = Instant.now(clock);

        if (searchEntryRepository.findByTenantCodeAndEntryNumber(tenantCode, entryNumber).isPresent()) {
            throw new ConflictException("Search entry already exists for tenant/entryNumber: " + tenantCode + "/" + entryNumber);
        }

        SearchEntry created = searchEntryRepository.save(
            SearchEntry.create(
                tenantCode,
                entryNumber,
                command.title(),
                command.snippet(),
                command.category(),
                command.targetType(),
                command.targetIdentifier(),
                command.targetUri(),
                now
            )
        );
        return toView(created);
    }

    @Override
    @Transactional(readOnly = true)
    public SearchEntryView getSearchEntry(String tenantCode, String entryNumber) {
        String normalizedTenantCode = normalizeRequired(tenantCode, "tenantCode").toUpperCase();
        String normalizedEntryNumber = normalizeRequired(entryNumber, "entryNumber").toUpperCase();
        SearchEntry entry = searchEntryRepository.findByTenantCodeAndEntryNumber(normalizedTenantCode, normalizedEntryNumber)
            .orElseThrow(() -> new NoSuchElementException(
                "Search entry not found for tenant/entryNumber: " + normalizedTenantCode + "/" + normalizedEntryNumber
            ));
        return toView(entry);
    }

    @Override
    @Transactional(readOnly = true)
    public PageResult<SearchEntryView> listSearchEntries(
        String tenantCode,
        PageQuery pageQuery,
        String query,
        String category
    ) {
        String normalizedTenantCode = normalizeRequired(tenantCode, "tenantCode").toUpperCase();
        String normalizedQuery = normalizeOptionalFilter(query, "query");
        String normalizedCategory = category == null ? null : normalizeOptionalFilter(category, "category").toUpperCase();

        Page<SearchEntry> page = searchEntryRepository.findFiltered(
            normalizedTenantCode,
            normalizedQuery,
            normalizedCategory,
            pageQuery.toPageable(Sort.by(Sort.Direction.DESC, "createdAt"))
        );
        return PageResult.from(page).map(this::toView);
    }

    private SearchEntryView toView(SearchEntry entry) {
        return new SearchEntryView(
            entry.getId(),
            entry.getTenantCode(),
            entry.getEntryNumber(),
            entry.getTitle(),
            entry.getSnippet(),
            entry.getCategory(),
            entry.getTargetType(),
            entry.getTargetIdentifier(),
            entry.getTargetUri(),
            entry.getCreatedAt()
        );
    }

    private static String normalizeRequired(String value, String fieldName) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException(fieldName + " is required");
        }
        return value.trim();
    }

    private static String normalizeOptionalFilter(String value, String fieldName) {
        if (value == null) {
            return null;
        }
        if (value.isBlank()) {
            throw new IllegalArgumentException(fieldName + " query parameter must not be blank");
        }
        return value.trim();
    }
}
