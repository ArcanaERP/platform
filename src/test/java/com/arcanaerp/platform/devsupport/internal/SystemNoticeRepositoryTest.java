package com.arcanaerp.platform.devsupport.internal;

import static org.assertj.core.api.Assertions.assertThat;

import com.arcanaerp.platform.devsupport.NoticeSeverity;
import java.time.Instant;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;

@DataJpaTest
class SystemNoticeRepositoryTest {

    @Autowired
    private SystemNoticeRepository systemNoticeRepository;

    @Test
    void filtersNoticesByTenantSeverityAndActive() {
        systemNoticeRepository.save(
            SystemNotice.create(
                "tenant01",
                "notice-001",
                "Planned Maintenance",
                "System will be unavailable overnight.",
                NoticeSeverity.WARNING,
                true,
                Instant.parse("2026-04-22T01:00:00Z")
            )
        );
        systemNoticeRepository.save(
            SystemNotice.create(
                "tenant01",
                "notice-002",
                "Incident Resolved",
                "Prior outage has been resolved.",
                NoticeSeverity.INFO,
                false,
                Instant.parse("2026-04-22T02:00:00Z")
            )
        );
        systemNoticeRepository.save(
            SystemNotice.create(
                "tenant02",
                "notice-003",
                "Other Tenant Notice",
                "Different tenant message.",
                NoticeSeverity.WARNING,
                true,
                Instant.parse("2026-04-22T03:00:00Z")
            )
        );

        var page = systemNoticeRepository.findByTenantCodeAndSeverityAndActive(
            "TENANT01",
            NoticeSeverity.WARNING,
            true,
            PageRequest.of(0, 10, Sort.by(Sort.Direction.DESC, "createdAt"))
        );

        assertThat(page.getTotalElements()).isEqualTo(1);
        assertThat(page.getContent().get(0).getNoticeCode()).isEqualTo("NOTICE-001");
    }
}
