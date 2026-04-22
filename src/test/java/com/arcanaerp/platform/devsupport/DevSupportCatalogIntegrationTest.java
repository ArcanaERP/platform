package com.arcanaerp.platform.devsupport;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.arcanaerp.platform.core.api.ConflictException;
import com.arcanaerp.platform.core.pagination.PageQuery;
import java.util.NoSuchElementException;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

@SpringBootTest
class DevSupportCatalogIntegrationTest {

    @Autowired
    private DevSupportCatalog devSupportCatalog;

    @Test
    void registersReadsAndListsSystemNotices() {
        SystemNoticeView created = devSupportCatalog.registerSystemNotice(
            new RegisterSystemNoticeCommand(
                "devsupport01",
                "notice-001",
                "Planned Maintenance",
                "System will be unavailable overnight.",
                NoticeSeverity.WARNING,
                true
            )
        );
        devSupportCatalog.registerSystemNotice(
            new RegisterSystemNoticeCommand(
                "devsupport01",
                "notice-002",
                "Incident Resolved",
                "Prior outage has been resolved.",
                NoticeSeverity.INFO,
                false
            )
        );

        SystemNoticeView loaded = devSupportCatalog.getSystemNotice("devsupport01", "notice-001");
        var listed = devSupportCatalog.listSystemNotices(
            "devsupport01",
            new PageQuery(0, 10),
            NoticeSeverity.WARNING,
            true
        );

        assertThat(loaded.noticeCode()).isEqualTo(created.noticeCode());
        assertThat(loaded.tenantCode()).isEqualTo("DEVSUPPORT01");
        assertThat(loaded.severity()).isEqualTo(NoticeSeverity.WARNING);
        assertThat(listed.totalItems()).isEqualTo(1);
        assertThat(listed.items()).extracting(SystemNoticeView::noticeCode).containsExactly("NOTICE-001");
    }

    @Test
    void rejectsDuplicateTenantLocalNoticeCodes() {
        devSupportCatalog.registerSystemNotice(
            new RegisterSystemNoticeCommand(
                "devsupport02",
                "notice-001",
                "Planned Maintenance",
                "System will be unavailable overnight.",
                NoticeSeverity.WARNING,
                true
            )
        );

        assertThatThrownBy(() -> devSupportCatalog.registerSystemNotice(
            new RegisterSystemNoticeCommand(
                "devsupport02",
                "NOTICE-001",
                "Duplicate Notice",
                "Duplicate message.",
                NoticeSeverity.ERROR,
                true
            )
        ))
            .isInstanceOf(ConflictException.class)
            .hasMessage("System notice already exists for tenant/code: DEVSUPPORT02/NOTICE-001");
    }

    @Test
    void rejectsMissingNoticeLookup() {
        assertThatThrownBy(() -> devSupportCatalog.getSystemNotice("devsupport03", "missing"))
            .isInstanceOf(NoSuchElementException.class)
            .hasMessage("System notice not found for tenant/code: DEVSUPPORT03/MISSING");
    }
}
