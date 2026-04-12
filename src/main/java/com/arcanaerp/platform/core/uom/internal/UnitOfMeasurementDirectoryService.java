package com.arcanaerp.platform.core.uom.internal;

import com.arcanaerp.platform.core.api.ConflictException;
import com.arcanaerp.platform.core.pagination.PageQuery;
import com.arcanaerp.platform.core.pagination.PageResult;
import com.arcanaerp.platform.core.uom.RegisterUnitOfMeasurementCommand;
import com.arcanaerp.platform.core.uom.UnitOfMeasurementDirectory;
import com.arcanaerp.platform.core.uom.UnitOfMeasurementView;
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
class UnitOfMeasurementDirectoryService implements UnitOfMeasurementDirectory {

    private final UnitOfMeasurementRepository unitOfMeasurementRepository;
    private final Clock clock;

    @Override
    public UnitOfMeasurementView registerUnitOfMeasurement(RegisterUnitOfMeasurementCommand command) {
        String normalizedCode = normalizeRequired(command.code(), "code").toUpperCase();
        String normalizedDescription = normalizeRequired(command.description(), "description");
        String normalizedDomain = normalizeOptional(command.domain(), true);
        String normalizedComments = normalizeOptional(command.comments(), false);
        Instant now = Instant.now(clock);

        if (unitOfMeasurementRepository.findByCode(normalizedCode).isPresent()) {
            throw new ConflictException("Unit of measurement code already exists: " + normalizedCode);
        }

        UnitOfMeasurement created = unitOfMeasurementRepository.save(
            UnitOfMeasurement.create(normalizedCode, normalizedDescription, normalizedDomain, normalizedComments, now)
        );
        return toView(created);
    }

    @Override
    @Transactional(readOnly = true)
    public PageResult<UnitOfMeasurementView> listUnitsOfMeasurement(PageQuery pageQuery, String queryFilter, String domain) {
        String normalizedQueryFilter = normalizeOptional(queryFilter, false);
        String normalizedDomain = normalizeOptional(domain, true);
        Page<UnitOfMeasurement> units = findUnits(normalizedQueryFilter, normalizedDomain, pageQuery);
        return PageResult.from(units).map(this::toView);
    }

    private Page<UnitOfMeasurement> findUnits(String queryFilter, String domain, PageQuery pageQuery) {
        var pageable = pageQuery.toPageable(Sort.by(Sort.Direction.ASC, "createdAt"));
        if (queryFilter != null && domain != null) {
            return unitOfMeasurementRepository.findByDescriptionStartingWithIgnoreCaseAndDomainIgnoreCase(
                queryFilter,
                domain,
                pageable
            );
        }
        if (queryFilter != null) {
            return unitOfMeasurementRepository.findByDescriptionStartingWithIgnoreCase(queryFilter, pageable);
        }
        if (domain != null) {
            return unitOfMeasurementRepository.findByDomainIgnoreCase(domain, pageable);
        }
        return unitOfMeasurementRepository.findAll(pageable);
    }

    private UnitOfMeasurementView toView(UnitOfMeasurement unit) {
        return new UnitOfMeasurementView(
            unit.getId(),
            unit.getCode(),
            unit.getDescription(),
            unit.getDomain(),
            unit.getComments(),
            unit.getCreatedAt()
        );
    }

    private static String normalizeRequired(String value, String fieldName) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException(fieldName + " is required");
        }
        return value.trim();
    }

    private static String normalizeOptional(String value, boolean upperCase) {
        if (value == null) {
            return null;
        }
        if (value.isBlank()) {
            throw new IllegalArgumentException((upperCase ? "domain" : "queryFilter") + " must not be blank");
        }
        String normalized = value.trim();
        return upperCase ? normalized.toUpperCase() : normalized;
    }
}
