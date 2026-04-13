package com.arcanaerp.platform.identity.web;

import com.arcanaerp.platform.core.pagination.PageQuery;
import com.arcanaerp.platform.core.pagination.PageResult;
import com.arcanaerp.platform.identity.RegisterRoleCommand;
import com.arcanaerp.platform.identity.RoleDirectory;
import com.arcanaerp.platform.identity.RoleView;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/identity/roles")
@RequiredArgsConstructor
public class RolesController {

    private final RoleDirectory roleDirectory;

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public RoleResponse registerRole(@Valid @RequestBody CreateRoleRequest request) {
        RoleView created = roleDirectory.registerRole(
            new RegisterRoleCommand(
                request.tenantCode(),
                request.tenantName(),
                request.code(),
                request.name()
            )
        );
        return toResponse(created);
    }

    @GetMapping
    public PageResult<RoleResponse> listRoles(
        @RequestParam String tenantCode,
        @RequestParam(required = false) Integer page,
        @RequestParam(required = false) Integer size
    ) {
        return roleDirectory.listRoles(tenantCode, PageQuery.of(page, size)).map(this::toResponse);
    }

    private RoleResponse toResponse(RoleView role) {
        return new RoleResponse(
            role.id(),
            role.tenantId(),
            role.tenantCode(),
            role.tenantName(),
            role.code(),
            role.name(),
            role.createdAt()
        );
    }
}
