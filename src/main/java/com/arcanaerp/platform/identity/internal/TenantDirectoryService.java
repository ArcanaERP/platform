package com.arcanaerp.platform.identity.internal;

import com.arcanaerp.platform.core.pagination.PageQuery;
import com.arcanaerp.platform.core.pagination.PageResult;
import com.arcanaerp.platform.identity.TenantDirectory;
import com.arcanaerp.platform.identity.TenantView;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional(readOnly = true)
@RequiredArgsConstructor
class TenantDirectoryService implements TenantDirectory {

    private final TenantRepository tenantRepository;

    @Override
    public PageResult<TenantView> listTenants(PageQuery pageQuery) {
        return PageResult.from(
            tenantRepository.findAll(pageQuery.toPageable(Sort.by(Sort.Direction.ASC, "createdAt")))
        ).map(this::toView);
    }

    private TenantView toView(Tenant tenant) {
        return new TenantView(
            tenant.getId(),
            tenant.getCode(),
            tenant.getName(),
            tenant.getCreatedAt()
        );
    }
}
