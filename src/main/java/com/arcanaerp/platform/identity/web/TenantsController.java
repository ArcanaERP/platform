package com.arcanaerp.platform.identity.web;

import com.arcanaerp.platform.core.pagination.PageQuery;
import com.arcanaerp.platform.core.pagination.PageResult;
import com.arcanaerp.platform.identity.RegisterTenantCommand;
import com.arcanaerp.platform.identity.TenantDirectory;
import com.arcanaerp.platform.identity.TenantView;
import com.arcanaerp.platform.identity.UpdateTenantCommand;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/identity/tenants")
@RequiredArgsConstructor
public class TenantsController {

    private final TenantDirectory tenantDirectory;

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public TenantResponse registerTenant(@Valid @RequestBody CreateTenantRequest request) {
        return toResponse(tenantDirectory.registerTenant(new RegisterTenantCommand(
            request.code(),
            request.name()
        )));
    }

    @GetMapping
    public PageResult<TenantResponse> listTenants(
        @RequestParam(required = false) Integer page,
        @RequestParam(required = false) Integer size
    ) {
        return tenantDirectory.listTenants(PageQuery.of(page, size)).map(this::toResponse);
    }

    @GetMapping("/{code}")
    public TenantResponse tenantByCode(@PathVariable String code) {
        return toResponse(tenantDirectory.tenantByCode(code));
    }

    @PatchMapping("/{code}")
    public TenantResponse updateTenant(
        @PathVariable String code,
        @Valid @RequestBody UpdateTenantRequest request
    ) {
        return toResponse(tenantDirectory.updateTenant(new UpdateTenantCommand(code, request.name())));
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
