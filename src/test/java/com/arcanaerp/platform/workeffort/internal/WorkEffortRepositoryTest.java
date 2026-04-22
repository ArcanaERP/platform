package com.arcanaerp.platform.workeffort.internal;

import static org.assertj.core.api.Assertions.assertThat;

import com.arcanaerp.platform.workeffort.WorkEffortStatus;
import java.time.Instant;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;

@DataJpaTest
class WorkEffortRepositoryTest {

    @Autowired
    private WorkEffortRepository workEffortRepository;

    @Test
    void filtersWorkEffortsByTenantStatusAndAssignedTo() {
        workEffortRepository.save(
            WorkEffort.create(
                "tenant01",
                "we-001",
                "Prepare shipment",
                "Prepare shipment for dispatch",
                WorkEffortStatus.PLANNED,
                "agent01@tenant.com",
                null,
                Instant.parse("2026-04-21T10:00:00Z")
            )
        );
        workEffortRepository.save(
            WorkEffort.create(
                "tenant01",
                "we-002",
                "Confirm receipt",
                "Confirm inbound receipt",
                WorkEffortStatus.IN_PROGRESS,
                "agent02@tenant.com",
                null,
                Instant.parse("2026-04-21T11:00:00Z")
            )
        );
        workEffortRepository.save(
            WorkEffort.create(
                "tenant02",
                "we-003",
                "Other tenant task",
                "Ignore for filter",
                WorkEffortStatus.PLANNED,
                "agent01@tenant.com",
                null,
                Instant.parse("2026-04-21T12:00:00Z")
            )
        );

        var page = workEffortRepository.findByTenantCodeAndStatusAndAssignedTo(
            "TENANT01",
            WorkEffortStatus.PLANNED,
            "agent01@tenant.com",
            PageRequest.of(0, 10, Sort.by(Sort.Direction.DESC, "createdAt"))
        );

        assertThat(page.getTotalElements()).isEqualTo(1);
        assertThat(page.getContent().get(0).getEffortNumber()).isEqualTo("WE-001");
    }
}
