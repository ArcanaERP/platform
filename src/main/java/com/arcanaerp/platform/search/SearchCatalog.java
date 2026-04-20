package com.arcanaerp.platform.search;

import com.arcanaerp.platform.core.pagination.PageQuery;
import com.arcanaerp.platform.core.pagination.PageResult;

public interface SearchCatalog {

    SearchEntryView registerSearchEntry(RegisterSearchEntryCommand command);

    SearchEntryView getSearchEntry(String tenantCode, String entryNumber);

    PageResult<SearchEntryView> listSearchEntries(
        String tenantCode,
        PageQuery pageQuery,
        String query,
        String category
    );
}
