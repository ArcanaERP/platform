package com.arcanaerp.platform.workeffort;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.arcanaerp.platform.core.api.ConflictException;
import com.arcanaerp.platform.core.pagination.PageQuery;
import com.arcanaerp.platform.identity.RegisterUserCommand;
import com.arcanaerp.platform.identity.UserDirectory;
import java.time.Instant;
import java.util.NoSuchElementException;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

@SpringBootTest
class WorkEffortCatalogIntegrationTest {

    @Autowired
    private WorkEffortCatalog workEffortCatalog;

    @Autowired
    private UserDirectory userDirectory;

    @Test
    void createsReadsAndListsWorkEfforts() {
        userDirectory.registerUser(
            new RegisterUserCommand("work01", "Work Tenant", "ops", "Operations", "agent01@work.com", "Agent 01")
        );
        userDirectory.registerUser(
            new RegisterUserCommand("work01", "Work Tenant", "ops", "Operations", "agent02@work.com", "Agent 02")
        );

        WorkEffortView created = workEffortCatalog.createWorkEffort(
            new CreateWorkEffortCommand(
                "work01",
                "we-001",
                "Prepare shipment",
                "Prepare shipment for dispatch",
                WorkEffortStatus.PLANNED,
                "AGENT01@WORK.COM",
                Instant.parse("2026-04-22T10:00:00Z")
            )
        );
        workEffortCatalog.createWorkEffort(
            new CreateWorkEffortCommand(
                "work01",
                "we-002",
                "Confirm receipt",
                "Confirm inbound receipt",
                WorkEffortStatus.IN_PROGRESS,
                "agent02@work.com",
                null
            )
        );

        WorkEffortView loaded = workEffortCatalog.getWorkEffort("work01", "we-001");
        var listed = workEffortCatalog.listWorkEfforts("work01", new PageQuery(0, 10), WorkEffortStatus.PLANNED, "agent01@work.com");

        assertThat(loaded.effortNumber()).isEqualTo(created.effortNumber());
        assertThat(loaded.tenantCode()).isEqualTo("WORK01");
        assertThat(loaded.assignedTo()).isEqualTo("agent01@work.com");
        assertThat(listed.totalItems()).isEqualTo(1);
        assertThat(listed.items()).extracting(WorkEffortView::effortNumber).containsExactly("WE-001");
    }

    @Test
    void rejectsDuplicateTenantLocalEffortNumbers() {
        userDirectory.registerUser(
            new RegisterUserCommand("work02", "Work Tenant", "ops", "Operations", "agent01@work.com", "Agent 01")
        );
        workEffortCatalog.createWorkEffort(
            new CreateWorkEffortCommand(
                "work02",
                "we-001",
                "Prepare shipment",
                "Prepare shipment for dispatch",
                WorkEffortStatus.PLANNED,
                "agent01@work.com",
                null
            )
        );

        assertThatThrownBy(() -> workEffortCatalog.createWorkEffort(
            new CreateWorkEffortCommand(
                "work02",
                "WE-001",
                "Duplicate effort",
                "Duplicate effort description",
                WorkEffortStatus.PLANNED,
                "agent01@work.com",
                null
            )
        ))
            .isInstanceOf(ConflictException.class)
            .hasMessage("Work effort already exists for tenant/effortNumber: WORK02/WE-001");
    }

    @Test
    void rejectsUnknownAssignee() {
        assertThatThrownBy(() -> workEffortCatalog.createWorkEffort(
            new CreateWorkEffortCommand(
                "work03",
                "we-001",
                "Prepare shipment",
                "Prepare shipment for dispatch",
                WorkEffortStatus.PLANNED,
                "missing@work.com",
                null
            )
        ))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessage("work effort assignee not found in tenant: WORK03/missing@work.com");
    }

    @Test
    void rejectsMissingWorkEffortLookup() {
        assertThatThrownBy(() -> workEffortCatalog.getWorkEffort("work04", "missing"))
            .isInstanceOf(NoSuchElementException.class)
            .hasMessage("Work effort not found for tenant/effortNumber: WORK04/MISSING");
    }
}
