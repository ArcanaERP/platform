package com.arcanaerp.platform.devsupport.internal;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.arcanaerp.platform.devsupport.NoticeSeverity;
import java.time.Instant;
import org.junit.jupiter.api.Test;

class SystemNoticeDomainTest {

    @Test
    void normalizesNoticeFields() {
        SystemNotice notice = SystemNotice.create(
            "tenant01",
            "notice-001",
            " Planned Maintenance ",
            " System will be unavailable overnight. ",
            NoticeSeverity.WARNING,
            true,
            Instant.parse("2026-04-22T01:00:00Z")
        );

        assertThat(notice.getTenantCode()).isEqualTo("TENANT01");
        assertThat(notice.getNoticeCode()).isEqualTo("NOTICE-001");
        assertThat(notice.getTitle()).isEqualTo("Planned Maintenance");
        assertThat(notice.getMessage()).isEqualTo("System will be unavailable overnight.");
    }

    @Test
    void rejectsMissingSeverity() {
        assertThatThrownBy(() -> SystemNotice.create(
            "tenant01",
            "notice-001",
            "Planned Maintenance",
            "System will be unavailable overnight.",
            null,
            true,
            Instant.parse("2026-04-22T01:00:00Z")
        ))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessage("severity is required");
    }
}
