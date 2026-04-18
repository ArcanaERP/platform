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

    @Test
    void createsReadsAndListsCommunicationEvents() {
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
        var listed = communicationEventLog.listEvents("commtenant01", new PageQuery(0, 10), "EMAIL", "INBOUND", "agent01@commtenant.com");

        assertThat(loaded.eventNumber()).isEqualTo(created.eventNumber());
        assertThat(loaded.tenantCode()).isEqualTo("COMMTENANT01");
        assertThat(listed.totalItems()).isEqualTo(1);
        assertThat(listed.items()).extracting(CommunicationEventView::eventNumber).containsExactly(created.eventNumber());
    }

    @Test
    void rejectsUnknownRecordedByActor() {
        assertThatThrownBy(() -> communicationEventLog.createEvent(
            new CreateCommunicationEventCommand(
                "commtenant02",
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
    void rejectsMissingCommunicationEventLookup() {
        assertThatThrownBy(() -> communicationEventLog.getEvent("commtenant03", "COMM-MISSING"))
            .isInstanceOf(NoSuchElementException.class)
            .hasMessage("Communication event not found for tenant/eventNumber: COMMTENANT03/COMM-MISSING");
    }
}
