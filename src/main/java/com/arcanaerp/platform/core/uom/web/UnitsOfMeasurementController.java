package com.arcanaerp.platform.core.uom.web;

import com.arcanaerp.platform.core.pagination.PageQuery;
import com.arcanaerp.platform.core.pagination.PageResult;
import com.arcanaerp.platform.core.uom.RegisterUnitOfMeasurementCommand;
import com.arcanaerp.platform.core.uom.UnitOfMeasurementDirectory;
import com.arcanaerp.platform.core.uom.UnitOfMeasurementView;
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
@RequestMapping("/api/core/units-of-measurement")
@RequiredArgsConstructor
public class UnitsOfMeasurementController {

    private final UnitOfMeasurementDirectory unitOfMeasurementDirectory;

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public UnitOfMeasurementResponse registerUnitOfMeasurement(
        @Valid @RequestBody CreateUnitOfMeasurementRequest request
    ) {
        UnitOfMeasurementView created = unitOfMeasurementDirectory.registerUnitOfMeasurement(
            new RegisterUnitOfMeasurementCommand(
                request.code(),
                request.description(),
                request.domain(),
                request.comments()
            )
        );
        return toResponse(created);
    }

    @GetMapping
    public PageResult<UnitOfMeasurementResponse> listUnitsOfMeasurement(
        @RequestParam(required = false) Integer page,
        @RequestParam(required = false) Integer size,
        @RequestParam(required = false) String queryFilter,
        @RequestParam(required = false) String domain
    ) {
        return unitOfMeasurementDirectory
            .listUnitsOfMeasurement(PageQuery.of(page, size), queryFilter, domain)
            .map(this::toResponse);
    }

    private UnitOfMeasurementResponse toResponse(UnitOfMeasurementView unit) {
        return new UnitOfMeasurementResponse(
            unit.id(),
            unit.code(),
            unit.description(),
            unit.domain(),
            unit.comments(),
            unit.createdAt()
        );
    }
}
