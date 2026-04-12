package com.arcanaerp.platform.identity.internal;

import com.arcanaerp.platform.core.pagination.PageQuery;
import com.arcanaerp.platform.core.pagination.PageResult;
import com.arcanaerp.platform.identity.RoleDirectory;
import com.arcanaerp.platform.identity.RoleView;
import java.util.NoSuchElementException;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional(readOnly = true)
@RequiredArgsConstructor
class RoleDirectoryService implements RoleDirectory {

    private final TenantRepository tenantRepository;
    private final RoleRepository roleRepository;

    @Override
    public PageResult<RoleView> listRoles(String tenantCode, PageQuery pageQuery) {
        String normalizedTenantCode = normalizeRequired(tenantCode, "tenantCode").toUpperCase();
        Tenant tenant = tenantRepository.findByCode(normalizedTenantCode)
            .orElseThrow(() -> new NoSuchElementException("Tenant not found: " + normalizedTenantCode));

        Page<Role> roles = roleRepository.findByTenantId(
            tenant.getId(),
            pageQuery.toPageable(Sort.by(Sort.Direction.ASC, "createdAt"))
        );
        return PageResult.from(roles).map(role -> toView(role, tenant));
    }

    private RoleView toView(Role role, Tenant tenant) {
        return new RoleView(
            role.getId(),
            role.getTenantId(),
            tenant.getCode(),
            tenant.getName(),
            role.getCode(),
            role.getName(),
            role.getCreatedAt()
        );
    }

    private static String normalizeRequired(String value, String fieldName) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException(fieldName + " is required");
        }
        return value.trim();
    }
}
