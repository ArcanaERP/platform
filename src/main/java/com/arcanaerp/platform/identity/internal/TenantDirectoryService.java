package com.arcanaerp.platform.identity.internal;

import com.arcanaerp.platform.core.pagination.PageQuery;
import com.arcanaerp.platform.core.pagination.PageResult;
import com.arcanaerp.platform.identity.TenantDirectory;
import com.arcanaerp.platform.identity.TenantView;
import com.arcanaerp.platform.identity.RegisterTenantCommand;
import java.time.Clock;
import java.time.Instant;
import java.util.NoSuchElementException;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional(readOnly = true)
@RequiredArgsConstructor
class TenantDirectoryService implements TenantDirectory {

    private final TenantRepository tenantRepository;
    private final Clock clock;

    @Override
    @Transactional
    public TenantView registerTenant(RegisterTenantCommand command) {
        String normalizedCode = normalizeRequired(command.code(), "code").toUpperCase();
        String normalizedName = normalizeRequired(command.name(), "name");
        Instant now = Instant.now(clock);

        if (tenantRepository.findByCode(normalizedCode).isPresent()) {
            throw new IllegalArgumentException("Tenant code already exists: " + normalizedCode);
        }

        Tenant tenant = tenantRepository.save(Tenant.create(normalizedCode, normalizedName, now));
        return toView(tenant);
    }

    @Override
    public PageResult<TenantView> listTenants(PageQuery pageQuery) {
        return PageResult.from(
            tenantRepository.findAll(pageQuery.toPageable(Sort.by(Sort.Direction.ASC, "createdAt")))
        ).map(this::toView);
    }

    @Override
    public TenantView tenantByCode(String code) {
        String normalizedCode = normalizeRequired(code, "code").toUpperCase();
        Tenant tenant = tenantRepository.findByCode(normalizedCode)
            .orElseThrow(() -> new NoSuchElementException("Tenant not found: " + normalizedCode));
        return toView(tenant);
    }

    private TenantView toView(Tenant tenant) {
        return new TenantView(
            tenant.getId(),
            tenant.getCode(),
            tenant.getName(),
            tenant.getCreatedAt()
        );
    }

    private static String normalizeRequired(String value, String fieldName) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException(fieldName + " is required");
        }
        return value.trim();
    }
}
