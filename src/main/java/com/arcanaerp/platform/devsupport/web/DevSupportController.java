package com.arcanaerp.platform.devsupport.web;

import com.arcanaerp.platform.core.pagination.PageQuery;
import com.arcanaerp.platform.core.pagination.PageResult;
import com.arcanaerp.platform.devsupport.DevSupportCatalog;
import com.arcanaerp.platform.devsupport.NoticeSeverity;
import com.arcanaerp.platform.devsupport.RegisterSystemNoticeCommand;
import com.arcanaerp.platform.devsupport.SystemNoticeView;
import jakarta.validation.Valid;
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
@RequestMapping("/api/dev-support/system-notices")
@RequiredArgsConstructor
public class DevSupportController {

    private final DevSupportCatalog devSupportCatalog;

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public SystemNoticeResponse registerSystemNotice(@Valid @RequestBody CreateSystemNoticeRequest request) {
        SystemNoticeView created = devSupportCatalog.registerSystemNotice(
            new RegisterSystemNoticeCommand(
                request.tenantCode(),
                request.noticeCode(),
                request.title(),
                request.message(),
                request.severity(),
                request.active()
            )
        );
        return toResponse(created);
    }

    @GetMapping("/{noticeCode}")
    public SystemNoticeResponse getSystemNotice(
        @PathVariable String noticeCode,
        @RequestParam String tenantCode
    ) {
        return toResponse(devSupportCatalog.getSystemNotice(tenantCode, noticeCode));
    }

    @GetMapping
    public PageResult<SystemNoticeResponse> listSystemNotices(
        @RequestParam String tenantCode,
        @RequestParam(required = false) NoticeSeverity severity,
        @RequestParam(required = false) Boolean active,
        @RequestParam(required = false) Integer page,
        @RequestParam(required = false) Integer size
    ) {
        return devSupportCatalog.listSystemNotices(
            tenantCode,
            PageQuery.of(page, size),
            severity,
            active
        ).map(this::toResponse);
    }

    private SystemNoticeResponse toResponse(SystemNoticeView view) {
        return new SystemNoticeResponse(
            view.id(),
            view.tenantCode(),
            view.noticeCode(),
            view.title(),
            view.message(),
            view.severity(),
            view.active(),
            view.createdAt()
        );
    }
}
