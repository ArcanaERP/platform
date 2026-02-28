package com.arcanaerp.platform.identity.internal;

import com.arcanaerp.platform.core.pagination.PageQuery;
import com.arcanaerp.platform.core.pagination.PageResult;
import com.arcanaerp.platform.identity.OrgUnitDirectory;
import com.arcanaerp.platform.identity.OrgUnitView;
import com.arcanaerp.platform.identity.RegisterOrgUnitCommand;
import java.time.Clock;
import java.time.Instant;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional
@RequiredArgsConstructor
class OrgUnitDirectoryService implements OrgUnitDirectory {

    private final TenantRepository tenantRepository;
    private final OrgUnitRepository orgUnitRepository;
    private final Clock clock;

    @Override
    public OrgUnitView registerOrgUnit(RegisterOrgUnitCommand command) {
        String tenantCode = normalizeRequired(command.tenantCode(), "tenantCode").toUpperCase();
        String tenantName = normalizeRequired(command.tenantName(), "tenantName");
        String code = normalizeRequired(command.code(), "code").toUpperCase();
        String name = normalizeRequired(command.name(), "name");
        Instant now = Instant.now(clock);

        Tenant tenant = tenantRepository.findByCode(tenantCode)
            .orElseGet(() -> tenantRepository.save(Tenant.create(tenantCode, tenantName, now)));

        if (orgUnitRepository.findByTenantIdAndCode(tenant.getId(), code).isPresent()) {
            throw new IllegalArgumentException("Org unit code already exists in tenant: " + code);
        }

        OrgUnit created = orgUnitRepository.save(OrgUnit.create(tenant.getId(), code, name, now));
        return toView(created, tenant);
    }

    @Override
    @Transactional(readOnly = true)
    public PageResult<OrgUnitView> listOrgUnits(String tenantCode, PageQuery pageQuery) {
        String normalizedTenantCode = normalizeRequired(tenantCode, "tenantCode").toUpperCase();
        Tenant tenant = tenantRepository.findByCode(normalizedTenantCode)
            .orElseThrow(() -> new java.util.NoSuchElementException("Tenant not found: " + normalizedTenantCode));

        Page<OrgUnit> orgUnits = orgUnitRepository.findByTenantId(
            tenant.getId(),
            pageQuery.toPageable(Sort.by(Sort.Direction.DESC, "createdAt"))
        );
        return PageResult.from(orgUnits).map(orgUnit -> toView(orgUnit, tenant));
    }

    private OrgUnitView toView(OrgUnit orgUnit, Tenant tenant) {
        return new OrgUnitView(
            orgUnit.getId(),
            orgUnit.getTenantId(),
            tenant.getCode(),
            tenant.getName(),
            orgUnit.getCode(),
            orgUnit.getName(),
            orgUnit.isActive(),
            orgUnit.getCreatedAt()
        );
    }

    private static String normalizeRequired(String value, String fieldName) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException(fieldName + " is required");
        }
        return value.trim();
    }
}
