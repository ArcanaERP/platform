package com.arcanaerp.platform.identity.internal;

import com.arcanaerp.platform.core.pagination.PageQuery;
import com.arcanaerp.platform.core.pagination.PageResult;
import com.arcanaerp.platform.identity.RegisterRoleCommand;
import com.arcanaerp.platform.identity.RoleDirectory;
import com.arcanaerp.platform.identity.RoleView;
import com.arcanaerp.platform.identity.UpdateRoleCommand;
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
class RoleDirectoryService implements RoleDirectory {

    private final TenantRepository tenantRepository;
    private final RoleRepository roleRepository;
    private final Clock clock;

    @Override
    public RoleView registerRole(RegisterRoleCommand command) {
        String normalizedTenantCode = normalizeRequired(command.tenantCode(), "tenantCode").toUpperCase();
        String normalizedTenantName = normalizeRequired(command.tenantName(), "tenantName");
        String normalizedCode = normalizeRequired(command.code(), "code").toUpperCase();
        String normalizedName = normalizeRequired(command.name(), "name");
        Instant now = Instant.now(clock);

        Tenant tenant = tenantRepository.findByCode(normalizedTenantCode)
            .orElseGet(() -> tenantRepository.save(Tenant.create(normalizedTenantCode, normalizedTenantName, now)));

        if (roleRepository.findByTenantIdAndCode(tenant.getId(), normalizedCode).isPresent()) {
            throw new IllegalArgumentException("Role code already exists in tenant: " + normalizedCode);
        }

        Role role = roleRepository.save(Role.create(tenant.getId(), normalizedCode, normalizedName, now));
        return toView(role, tenant);
    }

    @Override
    @Transactional
    public RoleView updateRole(UpdateRoleCommand command) {
        String normalizedTenantCode = normalizeRequired(command.tenantCode(), "tenantCode").toUpperCase();
        String normalizedCode = normalizeRequired(command.code(), "code").toUpperCase();
        String normalizedName = normalizeRequired(command.name(), "name");

        Tenant tenant = tenantRepository.findByCode(normalizedTenantCode)
            .orElseThrow(() -> new NoSuchElementException("Tenant not found: " + normalizedTenantCode));

        Role role = roleRepository.findByTenantIdAndCode(tenant.getId(), normalizedCode)
            .orElseThrow(() -> new NoSuchElementException(
                "Role not found for tenant/code: " + normalizedTenantCode + "/" + normalizedCode
            ));
        role.update(normalizedName);
        Role saved = roleRepository.save(role);
        return toView(saved, tenant);
    }

    @Override
    @Transactional(readOnly = true)
    public RoleView roleByCode(String tenantCode, String code) {
        String normalizedTenantCode = normalizeRequired(tenantCode, "tenantCode").toUpperCase();
        String normalizedCode = normalizeRequired(code, "code").toUpperCase();

        Tenant tenant = tenantRepository.findByCode(normalizedTenantCode)
            .orElseThrow(() -> new NoSuchElementException("Tenant not found: " + normalizedTenantCode));

        Role role = roleRepository.findByTenantIdAndCode(tenant.getId(), normalizedCode)
            .orElseThrow(() -> new NoSuchElementException(
                "Role not found for tenant/code: " + normalizedTenantCode + "/" + normalizedCode
            ));
        return toView(role, tenant);
    }

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
