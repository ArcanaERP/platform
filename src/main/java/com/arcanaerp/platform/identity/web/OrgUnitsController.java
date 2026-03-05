package com.arcanaerp.platform.identity.web;

import com.arcanaerp.platform.core.pagination.PageQuery;
import com.arcanaerp.platform.core.pagination.PageResult;
import com.arcanaerp.platform.identity.OrgUnitDirectory;
import com.arcanaerp.platform.identity.OrgUnitView;
import com.arcanaerp.platform.identity.RegisterOrgUnitCommand;
import com.arcanaerp.platform.identity.UpdateOrgUnitCommand;
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
@RequestMapping("/api/identity/org-units")
@RequiredArgsConstructor
public class OrgUnitsController {

    private final OrgUnitDirectory orgUnitDirectory;

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public OrgUnitResponse registerOrgUnit(@Valid @RequestBody CreateOrgUnitRequest request) {
        OrgUnitView created = orgUnitDirectory.registerOrgUnit(
            new RegisterOrgUnitCommand(
                request.tenantCode(),
                request.tenantName(),
                request.code(),
                request.name()
            )
        );
        return toResponse(created);
    }

    @GetMapping
    public PageResult<OrgUnitResponse> listOrgUnits(
        @RequestParam String tenantCode,
        @RequestParam(required = false) Integer page,
        @RequestParam(required = false) Integer size
    ) {
        return orgUnitDirectory.listOrgUnits(tenantCode, PageQuery.of(page, size)).map(this::toResponse);
    }

    @GetMapping("/{code}")
    public OrgUnitResponse orgUnitByCode(
        @PathVariable String code,
        @RequestParam String tenantCode
    ) {
        return toResponse(orgUnitDirectory.orgUnitByCode(tenantCode, code));
    }

    @PatchMapping("/{code}")
    public OrgUnitResponse updateOrgUnit(
        @PathVariable String code,
        @RequestParam String tenantCode,
        @Valid @RequestBody UpdateOrgUnitRequest request
    ) {
        OrgUnitView updated = orgUnitDirectory.updateOrgUnit(
            new UpdateOrgUnitCommand(
                tenantCode,
                code,
                request.name(),
                request.active()
            )
        );
        return toResponse(updated);
    }

    private OrgUnitResponse toResponse(OrgUnitView orgUnit) {
        return new OrgUnitResponse(
            orgUnit.id(),
            orgUnit.tenantId(),
            orgUnit.tenantCode(),
            orgUnit.tenantName(),
            orgUnit.code(),
            orgUnit.name(),
            orgUnit.active(),
            orgUnit.createdAt()
        );
    }
}
