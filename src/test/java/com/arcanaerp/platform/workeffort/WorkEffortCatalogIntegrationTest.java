package com.arcanaerp.platform.workeffort;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.arcanaerp.platform.core.api.ConflictException;
import com.arcanaerp.platform.core.pagination.PageQuery;
import com.arcanaerp.platform.identity.RegisterUserCommand;
import com.arcanaerp.platform.identity.UserDirectory;
import java.time.Instant;
import java.time.YearMonth;
import java.util.NoSuchElementException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;

@SpringBootTest
@Import(WorkEffortDeterministicClockTestSupport.Configuration.class)
class WorkEffortCatalogIntegrationTest {

    @Autowired
    private WorkEffortCatalog workEffortCatalog;

    @Autowired
    private UserDirectory userDirectory;

    @Autowired
    private WorkEffortDeterministicClockTestSupport.AdjustableClock testClock;

    @BeforeEach
    void resetClock() {
        testClock.resetToBaseInstant();
    }

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

    @Test
    void changesStatusAndReadsStatusHistory() {
        userDirectory.registerUser(
            new RegisterUserCommand("work05", "Work Tenant", "ops", "Operations", "agent01@work.com", "Agent 01")
        );

        workEffortCatalog.createWorkEffort(
            new CreateWorkEffortCommand(
                "work05",
                "we-001",
                "Prepare shipment",
                "Prepare shipment for dispatch",
                WorkEffortStatus.PLANNED,
                "agent01@work.com",
                null
            )
        );

        WorkEffortView updated = workEffortCatalog.changeWorkEffortStatus(
            new ChangeWorkEffortStatusCommand(
                "work05",
                "we-001",
                WorkEffortStatus.IN_PROGRESS,
                "Started picking",
                "AGENT01@WORK.COM"
            )
        );
        workEffortCatalog.changeWorkEffortStatus(
            new ChangeWorkEffortStatusCommand(
                "work05",
                "we-001",
                WorkEffortStatus.IN_PROGRESS,
                "No-op status change",
                "agent01@work.com"
            )
        );

        var history = workEffortCatalog.listStatusHistory(
            "work05",
            "we-001",
            "agent01@work.com",
            null,
            null,
            new PageQuery(0, 10)
        );

        assertThat(updated.status()).isEqualTo(WorkEffortStatus.IN_PROGRESS);
        assertThat(history.totalItems()).isEqualTo(1);
        assertThat(history.items()).extracting(WorkEffortStatusChangeView::currentStatus)
            .containsExactly(WorkEffortStatus.IN_PROGRESS);
    }

    @Test
    void changesAssignmentAndReadsAssignmentHistory() {
        userDirectory.registerUser(
            new RegisterUserCommand("work07", "Work Tenant", "ops", "Operations", "agent01@work.com", "Agent 01")
        );
        userDirectory.registerUser(
            new RegisterUserCommand("work07", "Work Tenant", "ops", "Operations", "agent02@work.com", "Agent 02")
        );
        userDirectory.registerUser(
            new RegisterUserCommand("work07", "Work Tenant", "ops", "Operations", "manager@work.com", "Manager")
        );
        workEffortCatalog.createWorkEffort(
            new CreateWorkEffortCommand(
                "work07",
                "we-001",
                "Prepare shipment",
                "Prepare shipment for dispatch",
                WorkEffortStatus.PLANNED,
                "agent01@work.com",
                null
            )
        );

        WorkEffortView updated = workEffortCatalog.assignWorkEffort(
            new AssignWorkEffortCommand(
                "work07",
                "we-001",
                "AGENT02@WORK.COM",
                "Coverage handoff",
                "MANAGER@WORK.COM"
            )
        );
        workEffortCatalog.assignWorkEffort(
            new AssignWorkEffortCommand(
                "work07",
                "we-001",
                "agent02@work.com",
                "No-op assignment",
                "manager@work.com"
            )
        );

        var history = workEffortCatalog.listAssignmentHistory(
            "work07",
            "we-001",
            "agent02@work.com",
            "manager@work.com",
            null,
            null,
            new PageQuery(0, 10)
        );

        assertThat(updated.assignedTo()).isEqualTo("agent02@work.com");
        assertThat(workEffortCatalog.getWorkEffortAssignment("work07", "we-001").assignedTo()).isEqualTo("agent02@work.com");
        assertThat(history.totalItems()).isEqualTo(1);
        assertThat(history.items()).extracting(WorkEffortAssignmentChangeView::previousAssignedTo)
            .containsExactly("agent01@work.com");
        assertThat(history.items()).extracting(WorkEffortAssignmentChangeView::currentAssignedTo)
            .containsExactly("agent02@work.com");

        var summaries = workEffortCatalog.listAssignmentActivitySummaries(
            "work07",
            "AGENT02@WORK.COM",
            null,
            null,
            new PageQuery(0, 10)
        );

        assertThat(summaries.totalItems()).isEqualTo(1);
        assertThat(summaries.items().get(0).tenantCode()).isEqualTo("WORK07");
        assertThat(summaries.items().get(0).assignedTo()).isEqualTo("agent02@work.com");
        assertThat(summaries.items().get(0).assignmentCount()).isEqualTo(1);
    }

    @Test
    void rejectsUnknownAssignmentAssignee() {
        userDirectory.registerUser(
            new RegisterUserCommand("work08", "Work Tenant", "ops", "Operations", "agent01@work.com", "Agent 01")
        );
        userDirectory.registerUser(
            new RegisterUserCommand("work08", "Work Tenant", "ops", "Operations", "manager@work.com", "Manager")
        );
        workEffortCatalog.createWorkEffort(
            new CreateWorkEffortCommand(
                "work08",
                "we-001",
                "Prepare shipment",
                "Prepare shipment for dispatch",
                WorkEffortStatus.PLANNED,
                "agent01@work.com",
                null
            )
        );

        assertThatThrownBy(() -> workEffortCatalog.assignWorkEffort(
            new AssignWorkEffortCommand(
                "work08",
                "we-001",
                "missing@work.com",
                "Coverage handoff",
                "manager@work.com"
            )
        ))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessage("work effort assignee not found in tenant: WORK08/missing@work.com");
    }

    @Test
    void rejectsUnknownAssignmentActor() {
        userDirectory.registerUser(
            new RegisterUserCommand("work09", "Work Tenant", "ops", "Operations", "agent01@work.com", "Agent 01")
        );
        userDirectory.registerUser(
            new RegisterUserCommand("work09", "Work Tenant", "ops", "Operations", "agent02@work.com", "Agent 02")
        );
        workEffortCatalog.createWorkEffort(
            new CreateWorkEffortCommand(
                "work09",
                "we-001",
                "Prepare shipment",
                "Prepare shipment for dispatch",
                WorkEffortStatus.PLANNED,
                "agent01@work.com",
                null
            )
        );

        assertThatThrownBy(() -> workEffortCatalog.assignWorkEffort(
            new AssignWorkEffortCommand(
                "work09",
                "we-001",
                "agent02@work.com",
                "Coverage handoff",
                "missing-manager@work.com"
            )
        ))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessage("work effort assignment actor not found in tenant: WORK09/missing-manager@work.com");
    }

    @Test
    void readsDailyWeeklyAndMonthlyAssignmentActivitySummaries() {
        userDirectory.registerUser(
            new RegisterUserCommand("work10", "Work Tenant", "ops", "Operations", "agent01@work.com", "Agent 01")
        );
        userDirectory.registerUser(
            new RegisterUserCommand("work10", "Work Tenant", "ops", "Operations", "agent02@work.com", "Agent 02")
        );
        userDirectory.registerUser(
            new RegisterUserCommand("work10", "Work Tenant", "ops", "Operations", "agent03@work.com", "Agent 03")
        );
        userDirectory.registerUser(
            new RegisterUserCommand("work10", "Work Tenant", "ops", "Operations", "manager@work.com", "Manager")
        );
        workEffortCatalog.createWorkEffort(
            new CreateWorkEffortCommand(
                "work10",
                "we-001",
                "Prepare shipment",
                "Prepare shipment for dispatch",
                WorkEffortStatus.PLANNED,
                "agent01@work.com",
                null
            )
        );
        workEffortCatalog.createWorkEffort(
            new CreateWorkEffortCommand(
                "work10",
                "we-002",
                "Confirm receipt",
                "Confirm inbound receipt",
                WorkEffortStatus.PLANNED,
                "agent01@work.com",
                null
            )
        );

        testClock.setInstant(Instant.parse("2026-04-22T10:00:00Z"));
        workEffortCatalog.assignWorkEffort(
            new AssignWorkEffortCommand("work10", "we-001", "agent02@work.com", "Coverage handoff", "manager@work.com")
        );
        testClock.setInstant(Instant.parse("2026-04-23T11:00:00Z"));
        workEffortCatalog.assignWorkEffort(
            new AssignWorkEffortCommand("work10", "we-001", "agent03@work.com", "Escalation", "manager@work.com")
        );
        testClock.setInstant(Instant.parse("2026-05-04T12:00:00Z"));
        workEffortCatalog.assignWorkEffort(
            new AssignWorkEffortCommand("work10", "we-002", "agent03@work.com", "Month handoff", "manager@work.com")
        );

        var daily = workEffortCatalog.listDailyAssignmentActivitySummaries(
            "work10",
            null,
            null,
            null,
            new PageQuery(0, 10)
        );
        var weekly = workEffortCatalog.listWeeklyAssignmentActivitySummaries(
            "work10",
            null,
            null,
            null,
            new PageQuery(0, 10)
        );
        var monthly = workEffortCatalog.listMonthlyAssignmentActivitySummaries(
            "work10",
            null,
            null,
            null,
            new PageQuery(0, 10)
        );
        var filteredDaily = workEffortCatalog.listDailyAssignmentActivitySummaries(
            "work10",
            "AGENT03@WORK.COM",
            Instant.parse("2026-04-23T00:00:00Z"),
            Instant.parse("2026-05-04T23:59:59Z"),
            new PageQuery(0, 10)
        );

        assertThat(daily.totalItems()).isEqualTo(3);
        assertThat(daily.items()).extracting(DailyWorkEffortAssignmentActivitySummaryView::businessDate)
            .containsExactly(
                java.time.LocalDate.parse("2026-05-04"),
                java.time.LocalDate.parse("2026-04-23"),
                java.time.LocalDate.parse("2026-04-22")
            );
        assertThat(weekly.totalItems()).isEqualTo(2);
        assertThat(weekly.items().get(0).businessWeekStart()).isEqualTo(java.time.LocalDate.parse("2026-05-04"));
        assertThat(weekly.items().get(1).businessWeekStart()).isEqualTo(java.time.LocalDate.parse("2026-04-20"));
        assertThat(weekly.items().get(1).assignmentCount()).isEqualTo(2);
        assertThat(weekly.items().get(1).workEffortCount()).isEqualTo(1);
        assertThat(monthly.items()).extracting(MonthlyWorkEffortAssignmentActivitySummaryView::businessMonth)
            .containsExactly(YearMonth.parse("2026-05"), YearMonth.parse("2026-04"));
        assertThat(monthly.items().get(1).assignmentCount()).isEqualTo(2);
        assertThat(filteredDaily.totalItems()).isEqualTo(2);
        assertThat(filteredDaily.items()).extracting(DailyWorkEffortAssignmentActivitySummaryView::businessDate)
            .containsExactly(java.time.LocalDate.parse("2026-05-04"), java.time.LocalDate.parse("2026-04-23"));
    }

    @Test
    void rejectsUnknownStatusActor() {
        userDirectory.registerUser(
            new RegisterUserCommand("work06", "Work Tenant", "ops", "Operations", "agent01@work.com", "Agent 01")
        );
        workEffortCatalog.createWorkEffort(
            new CreateWorkEffortCommand(
                "work06",
                "we-001",
                "Prepare shipment",
                "Prepare shipment for dispatch",
                WorkEffortStatus.PLANNED,
                "agent01@work.com",
                null
            )
        );

        assertThatThrownBy(() -> workEffortCatalog.changeWorkEffortStatus(
            new ChangeWorkEffortStatusCommand(
                "work06",
                "we-001",
                WorkEffortStatus.IN_PROGRESS,
                "Started picking",
                "missing@work.com"
            )
        ))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessage("work effort status actor not found in tenant: WORK06/missing@work.com");
    }
}
