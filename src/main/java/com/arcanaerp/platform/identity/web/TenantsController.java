package com.arcanaerp.platform.identity.web;

import com.arcanaerp.platform.core.pagination.PageQuery;
import com.arcanaerp.platform.core.pagination.PageResult;
import com.arcanaerp.platform.identity.TenantDirectory;
import com.arcanaerp.platform.identity.TenantView;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/identity/tenants")
@RequiredArgsConstructor
public class TenantsController {

    private final TenantDirectory tenantDirectory;

    @GetMapping
    public PageResult<TenantResponse> listTenants(
        @RequestParam(required = false) Integer page,
        @RequestParam(required = false) Integer size
    ) {
        return tenantDirectory.listTenants(PageQuery.of(page, size)).map(this::toResponse);
    }

    private TenantResponse toResponse(TenantView tenant) {
        return new TenantResponse(
            tenant.id(),
            tenant.code(),
            tenant.name(),
            tenant.createdAt()
        );
    }
}
