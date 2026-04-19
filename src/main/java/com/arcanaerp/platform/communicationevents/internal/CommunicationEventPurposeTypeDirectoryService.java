package com.arcanaerp.platform.communicationevents.internal;

import com.arcanaerp.platform.communicationevents.CommunicationEventPurposeTypeDirectory;
import com.arcanaerp.platform.communicationevents.CommunicationEventPurposeTypeView;
import com.arcanaerp.platform.communicationevents.RegisterCommunicationEventPurposeTypeCommand;
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
class CommunicationEventPurposeTypeDirectoryService implements CommunicationEventPurposeTypeDirectory {

    private final CommunicationEventPurposeTypeRepository purposeTypeRepository;
    private final Clock clock;

    @Override
    public CommunicationEventPurposeTypeView registerPurposeType(RegisterCommunicationEventPurposeTypeCommand command) {
        String tenantCode = normalizeRequired(command.tenantCode(), "tenantCode").toUpperCase();
        String code = normalizeRequired(command.code(), "code").toUpperCase();
        String name = normalizeRequired(command.name(), "name");
        Instant now = Instant.now(clock);

        if (purposeTypeRepository.findByTenantCodeAndCode(tenantCode, code).isPresent()) {
            throw new ConflictException("Communication event purpose type already exists for tenant/code: " + tenantCode + "/" + code);
        }

        return toView(purposeTypeRepository.save(CommunicationEventPurposeType.create(tenantCode, code, name, now)));
    }

    @Override
    @Transactional(readOnly = true)
    public CommunicationEventPurposeTypeView purposeTypeByCode(String tenantCode, String code) {
        String normalizedTenantCode = normalizeRequired(tenantCode, "tenantCode").toUpperCase();
        String normalizedCode = normalizeRequired(code, "code").toUpperCase();
        return toView(purposeTypeRepository.findByTenantCodeAndCode(normalizedTenantCode, normalizedCode)
            .orElseThrow(() -> new NoSuchElementException(
                "Communication event purpose type not found for tenant/code: " + normalizedTenantCode + "/" + normalizedCode
            )));
    }

    @Override
    @Transactional(readOnly = true)
    public PageResult<CommunicationEventPurposeTypeView> listPurposeTypes(String tenantCode, PageQuery pageQuery) {
        String normalizedTenantCode = normalizeRequired(tenantCode, "tenantCode").toUpperCase();
        return PageResult.from(purposeTypeRepository.findByTenantCode(
            normalizedTenantCode,
            pageQuery.toPageable(Sort.by(Sort.Direction.ASC, "createdAt"))
        )).map(this::toView);
    }

    private CommunicationEventPurposeTypeView toView(CommunicationEventPurposeType purposeType) {
        return new CommunicationEventPurposeTypeView(
            purposeType.getId(),
            purposeType.getTenantCode(),
            purposeType.getCode(),
            purposeType.getName(),
            purposeType.getCreatedAt()
        );
    }

    private static String normalizeRequired(String value, String fieldName) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException(fieldName + " is required");
        }
        return value.trim();
    }
}
