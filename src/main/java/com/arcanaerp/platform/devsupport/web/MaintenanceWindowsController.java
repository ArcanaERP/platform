package com.arcanaerp.platform.devsupport.web;

import com.arcanaerp.platform.core.pagination.PageQuery;
import com.arcanaerp.platform.core.pagination.PageResult;
import com.arcanaerp.platform.devsupport.DevSupportCatalog;
import com.arcanaerp.platform.devsupport.MaintenanceWindowView;
import com.arcanaerp.platform.devsupport.RegisterMaintenanceWindowCommand;
import jakarta.validation.Valid;
import java.time.Instant;
import java.time.format.DateTimeParseException;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/dev-support/maintenance-windows")
@RequiredArgsConstructor
public class MaintenanceWindowsController {

    private final DevSupportCatalog devSupportCatalog;

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public MaintenanceWindowResponse registerMaintenanceWindow(@Valid @RequestBody CreateMaintenanceWindowRequest request) {
        MaintenanceWindowView created = devSupportCatalog.registerMaintenanceWindow(
            new RegisterMaintenanceWindowCommand(
                request.tenantCode(),
                request.windowCode(),
                request.title(),
                request.description(),
                request.startsAt(),
                request.endsAt(),
                request.active()
            )
        );
        return toResponse(created);
    }

    @GetMapping("/{windowCode}")
    public MaintenanceWindowResponse getMaintenanceWindow(
        @PathVariable String windowCode,
        @RequestParam String tenantCode
    ) {
        return toResponse(devSupportCatalog.getMaintenanceWindow(tenantCode, windowCode));
    }

    @GetMapping
    public PageResult<MaintenanceWindowResponse> listMaintenanceWindows(
        @RequestParam String tenantCode,
        @RequestParam(required = false) Boolean active,
        @RequestParam(required = false) String startsAtFrom,
        @RequestParam(required = false) String startsAtTo,
        @RequestParam(required = false) Integer page,
        @RequestParam(required = false) Integer size
    ) {
        Instant parsedStartsAtFrom = parseOptionalInstant(startsAtFrom, "startsAtFrom");
        Instant parsedStartsAtTo = parseOptionalInstant(startsAtTo, "startsAtTo");
        validateStartsAtRange(parsedStartsAtFrom, parsedStartsAtTo);

        return devSupportCatalog.listMaintenanceWindows(
            tenantCode,
            PageQuery.of(page, size),
            active,
            parsedStartsAtFrom,
            parsedStartsAtTo
        ).map(this::toResponse);
    }

    private MaintenanceWindowResponse toResponse(MaintenanceWindowView view) {
        return new MaintenanceWindowResponse(
            view.id(),
            view.tenantCode(),
            view.windowCode(),
            view.title(),
            view.description(),
            view.startsAt(),
            view.endsAt(),
            view.active(),
            view.createdAt()
        );
    }

    private static Instant parseOptionalInstant(String value, String parameterName) {
        if (value == null) {
            return null;
        }
        if (value.isBlank()) {
            throw new IllegalArgumentException(parameterName + " query parameter must not be blank");
        }
        try {
            return Instant.parse(value.trim());
        } catch (DateTimeParseException exception) {
            throw new IllegalArgumentException(parameterName + " query parameter must be a valid ISO-8601 instant");
        }
    }

    private static void validateStartsAtRange(Instant startsAtFrom, Instant startsAtTo) {
        if (startsAtFrom != null && startsAtTo != null && startsAtFrom.isAfter(startsAtTo)) {
            throw new IllegalArgumentException("startsAtFrom must be before or equal to startsAtTo");
        }
    }
}
