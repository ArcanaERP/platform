package com.arcanaerp.platform.communicationevents.internal;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.arcanaerp.platform.communicationevents.CommunicationChannel;
import com.arcanaerp.platform.communicationevents.CommunicationDirection;
import java.time.Instant;
import org.junit.jupiter.api.Test;

class CommunicationEventDomainTest {

    @Test
    void createNormalizesTenantActorAndEventNumber() {
        CommunicationEvent event = CommunicationEvent.create(
            " comm-0001 ",
            " tenant-a ",
            " open ",
            " Open ",
            " support ",
            " Support ",
            CommunicationChannel.EMAIL,
            CommunicationDirection.INBOUND,
            "  Support Request  ",
            "  Customer asked for help  ",
            Instant.parse("2026-04-18T10:15:30Z"),
            " AGENT@ACME.COM ",
            " ext-123 ",
            Instant.parse("2026-04-18T10:16:30Z")
        );

        assertThat(event.getEventNumber()).isEqualTo("COMM-0001");
        assertThat(event.getTenantCode()).isEqualTo("TENANT-A");
        assertThat(event.getStatusCode()).isEqualTo("OPEN");
        assertThat(event.getPurposeCode()).isEqualTo("SUPPORT");
        assertThat(event.getRecordedBy()).isEqualTo("agent@acme.com");
        assertThat(event.getExternalReference()).isEqualTo("ext-123");
    }

    @Test
    void createRequiresOccurredAt() {
        assertThatThrownBy(() -> CommunicationEvent.create(
            "COMM-0002",
            "TENANT-A",
            "OPEN",
            "Open",
            "SUPPORT",
            "Support",
            CommunicationChannel.EMAIL,
            CommunicationDirection.OUTBOUND,
            "Support Reply",
            "Reply sent",
            null,
            "agent@acme.com",
            null,
            Instant.parse("2026-04-18T10:16:30Z")
        ))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessage("occurredAt is required");
    }

    @Test
    void createRejectsInvalidActorEmail() {
        assertThatThrownBy(() -> CommunicationEvent.create(
            "COMM-0003",
            "TENANT-A",
            "OPEN",
            "Open",
            "SUPPORT",
            "Support",
            CommunicationChannel.PHONE,
            CommunicationDirection.INTERNAL,
            "Call Logged",
            "Internal escalation",
            Instant.parse("2026-04-18T10:15:30Z"),
            "not-an-email",
            null,
            Instant.parse("2026-04-18T10:16:30Z")
        ))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessage("recordedBy is invalid");
    }
}
