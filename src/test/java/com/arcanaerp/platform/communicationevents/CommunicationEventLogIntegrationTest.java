package com.arcanaerp.platform.communicationevents;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.arcanaerp.platform.core.pagination.PageQuery;
import com.arcanaerp.platform.identity.RegisterUserCommand;
import com.arcanaerp.platform.identity.UserDirectory;
import java.time.Instant;
import java.util.NoSuchElementException;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

@SpringBootTest
class CommunicationEventLogIntegrationTest {

    @Autowired
    private CommunicationEventLog communicationEventLog;

    @Autowired
    private UserDirectory userDirectory;

    @Autowired
    private CommunicationEventStatusTypeDirectory statusTypeDirectory;

    @Autowired
    private CommunicationEventPurposeTypeDirectory purposeTypeDirectory;

    @Test
    void createsReadsAndListsCommunicationEvents() {
        statusTypeDirectory.registerStatusType(
            new RegisterCommunicationEventStatusTypeCommand("commtenant01", "open", "Open")
        );
        purposeTypeDirectory.registerPurposeType(
            new RegisterCommunicationEventPurposeTypeCommand("commtenant01", "support", "Support")
        );
        userDirectory.registerUser(
            new RegisterUserCommand(
                "commtenant01",
                "Comm Tenant 01",
                "ops",
                "Operations",
                "agent01@commtenant.com",
                "Agent 01"
            )
        );

        CommunicationEventView created = communicationEventLog.createEvent(
            new CreateCommunicationEventCommand(
                "commtenant01",
                "open",
                "support",
                "email",
                "inbound",
                "Support Request",
                "Customer asked for help",
                Instant.parse("2026-04-18T10:15:30Z"),
                "AGENT01@COMMTENANT.COM",
                "CRM-100"
            )
        );

        CommunicationEventView loaded = communicationEventLog.getEvent("commtenant01", created.eventNumber());
        var listed = communicationEventLog.listEvents(
            "commtenant01",
            new PageQuery(0, 10),
            "OPEN",
            "SUPPORT",
            "EMAIL",
            "INBOUND",
            "agent01@commtenant.com"
        );

        assertThat(loaded.eventNumber()).isEqualTo(created.eventNumber());
        assertThat(loaded.tenantCode()).isEqualTo("COMMTENANT01");
        assertThat(loaded.statusCode()).isEqualTo("OPEN");
        assertThat(loaded.purposeCode()).isEqualTo("SUPPORT");
        assertThat(listed.totalItems()).isEqualTo(1);
        assertThat(listed.items()).extracting(CommunicationEventView::eventNumber).containsExactly(created.eventNumber());
    }

    @Test
    void rejectsUnknownRecordedByActor() {
        statusTypeDirectory.registerStatusType(
            new RegisterCommunicationEventStatusTypeCommand("commtenant02", "open", "Open")
        );
        purposeTypeDirectory.registerPurposeType(
            new RegisterCommunicationEventPurposeTypeCommand("commtenant02", "support", "Support")
        );
        assertThatThrownBy(() -> communicationEventLog.createEvent(
            new CreateCommunicationEventCommand(
                "commtenant02",
                "open",
                "support",
                "phone",
                "outbound",
                "Follow-up Call",
                "Attempted outreach",
                Instant.parse("2026-04-18T10:15:30Z"),
                "missing@commtenant.com",
                null
            )
        ))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessage("recordedBy actor not found in tenant: COMMTENANT02/missing@commtenant.com");
    }

    @Test
    void rejectsUnknownStatusType() {
        purposeTypeDirectory.registerPurposeType(
            new RegisterCommunicationEventPurposeTypeCommand("commtenant04", "support", "Support")
        );
        userDirectory.registerUser(
            new RegisterUserCommand("commtenant04", "Comm Tenant 04", "ops", "Operations", "agent04@commtenant.com", "Agent 04")
        );

        assertThatThrownBy(() -> communicationEventLog.createEvent(
            new CreateCommunicationEventCommand(
                "commtenant04",
                "missing",
                "support",
                "email",
                "inbound",
                "Support Request",
                "Customer asked for help",
                Instant.parse("2026-04-18T10:15:30Z"),
                "agent04@commtenant.com",
                null
            )
        ))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessage("communication event status type not found for tenant/code: COMMTENANT04/MISSING");
    }

    @Test
    void rejectsMissingCommunicationEventLookup() {
        assertThatThrownBy(() -> communicationEventLog.getEvent("commtenant03", "COMM-MISSING"))
            .isInstanceOf(NoSuchElementException.class)
            .hasMessage("Communication event not found for tenant/eventNumber: COMMTENANT03/COMM-MISSING");
    }

    @Test
    void changesStatusAndListsStatusHistory() {
        statusTypeDirectory.registerStatusType(
            new RegisterCommunicationEventStatusTypeCommand("commtenant05", "open", "Open")
        );
        statusTypeDirectory.registerStatusType(
            new RegisterCommunicationEventStatusTypeCommand("commtenant05", "closed", "Closed")
        );
        purposeTypeDirectory.registerPurposeType(
            new RegisterCommunicationEventPurposeTypeCommand("commtenant05", "support", "Support")
        );
        userDirectory.registerUser(
            new RegisterUserCommand("commtenant05", "Comm Tenant 05", "ops", "Operations", "agent05@commtenant.com", "Agent 05")
        );

        CommunicationEventView created = communicationEventLog.createEvent(
            new CreateCommunicationEventCommand(
                "commtenant05",
                "open",
                "support",
                "email",
                "inbound",
                "Ticket",
                "Customer needs help",
                Instant.parse("2026-04-18T10:15:30Z"),
                "agent05@commtenant.com",
                null
            )
        );

        CommunicationEventView changed = communicationEventLog.changeStatus(
            new ChangeCommunicationEventStatusCommand(
                "commtenant05",
                created.eventNumber(),
                "closed",
                "Resolved by support",
                "agent05@commtenant.com"
            )
        );
        var history = communicationEventLog.listStatusHistory(
            "commtenant05",
            created.eventNumber(),
            "agent05@commtenant.com",
            null,
            null,
            new PageQuery(0, 10)
        );

        assertThat(changed.statusCode()).isEqualTo("CLOSED");
        assertThat(changed.statusName()).isEqualTo("Closed");
        assertThat(history.totalItems()).isEqualTo(1);
        assertThat(history.items().get(0).eventNumber()).isEqualTo(created.eventNumber());
        assertThat(history.items().get(0).previousStatusCode()).isEqualTo("OPEN");
        assertThat(history.items().get(0).currentStatusCode()).isEqualTo("CLOSED");
        assertThat(history.items().get(0).reason()).isEqualTo("Resolved by support");
        assertThat(history.items().get(0).changedBy()).isEqualTo("agent05@commtenant.com");
    }

    @Test
    void ignoresNoOpStatusChangesInHistory() {
        statusTypeDirectory.registerStatusType(
            new RegisterCommunicationEventStatusTypeCommand("commtenant06", "open", "Open")
        );
        purposeTypeDirectory.registerPurposeType(
            new RegisterCommunicationEventPurposeTypeCommand("commtenant06", "support", "Support")
        );
        userDirectory.registerUser(
            new RegisterUserCommand("commtenant06", "Comm Tenant 06", "ops", "Operations", "agent06@commtenant.com", "Agent 06")
        );

        CommunicationEventView created = communicationEventLog.createEvent(
            new CreateCommunicationEventCommand(
                "commtenant06",
                "open",
                "support",
                "email",
                "inbound",
                "Ticket",
                "No-op change test",
                Instant.parse("2026-04-18T10:15:30Z"),
                "agent06@commtenant.com",
                null
            )
        );

        CommunicationEventView changed = communicationEventLog.changeStatus(
            new ChangeCommunicationEventStatusCommand(
                "commtenant06",
                created.eventNumber(),
                "open",
                "Reaffirm current state",
                "agent06@commtenant.com"
            )
        );

        var history = communicationEventLog.listStatusHistory(
            "commtenant06",
            created.eventNumber(),
            null,
            null,
            null,
            new PageQuery(0, 10)
        );

        assertThat(changed.statusCode()).isEqualTo("OPEN");
        assertThat(history.totalItems()).isZero();
    }
}
