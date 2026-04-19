package com.arcanaerp.platform.communicationevents.internal;

import com.arcanaerp.platform.communicationevents.CommunicationEventStatusTypeDirectory;
import com.arcanaerp.platform.communicationevents.CommunicationEventStatusTypeView;
import com.arcanaerp.platform.communicationevents.RegisterCommunicationEventStatusTypeCommand;
import com.arcanaerp.platform.core.api.ConflictException;
import com.arcanaerp.platform.core.pagination.PageQuery;
import com.arcanaerp.platform.core.pagination.PageResult;
import java.time.Clock;
import java.time.Instant;
import java.util.NoSuchElementException;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional
@RequiredArgsConstructor
class CommunicationEventStatusTypeDirectoryService implements CommunicationEventStatusTypeDirectory {

    private final CommunicationEventStatusTypeRepository statusTypeRepository;
    private final Clock clock;

    @Override
    public CommunicationEventStatusTypeView registerStatusType(RegisterCommunicationEventStatusTypeCommand command) {
        String tenantCode = normalizeRequired(command.tenantCode(), "tenantCode").toUpperCase();
        String code = normalizeRequired(command.code(), "code").toUpperCase();
        String name = normalizeRequired(command.name(), "name");
        Instant now = Instant.now(clock);

        if (statusTypeRepository.findByTenantCodeAndCode(tenantCode, code).isPresent()) {
            throw new ConflictException("Communication event status type already exists for tenant/code: " + tenantCode + "/" + code);
        }

        return toView(statusTypeRepository.save(CommunicationEventStatusType.create(tenantCode, code, name, now)));
    }

    @Override
    @Transactional(readOnly = true)
    public CommunicationEventStatusTypeView statusTypeByCode(String tenantCode, String code) {
        String normalizedTenantCode = normalizeRequired(tenantCode, "tenantCode").toUpperCase();
        String normalizedCode = normalizeRequired(code, "code").toUpperCase();
        return toView(statusTypeRepository.findByTenantCodeAndCode(normalizedTenantCode, normalizedCode)
            .orElseThrow(() -> new NoSuchElementException(
                "Communication event status type not found for tenant/code: " + normalizedTenantCode + "/" + normalizedCode
            )));
    }

    @Override
    @Transactional(readOnly = true)
    public PageResult<CommunicationEventStatusTypeView> listStatusTypes(String tenantCode, PageQuery pageQuery) {
        String normalizedTenantCode = normalizeRequired(tenantCode, "tenantCode").toUpperCase();
        return PageResult.from(statusTypeRepository.findByTenantCode(
            normalizedTenantCode,
            pageQuery.toPageable(Sort.by(Sort.Direction.ASC, "createdAt"))
        )).map(this::toView);
    }

    private CommunicationEventStatusTypeView toView(CommunicationEventStatusType statusType) {
        return new CommunicationEventStatusTypeView(
            statusType.getId(),
            statusType.getTenantCode(),
            statusType.getCode(),
            statusType.getName(),
            statusType.getCreatedAt()
        );
    }

    private static String normalizeRequired(String value, String fieldName) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException(fieldName + " is required");
        }
        return value.trim();
    }
}
