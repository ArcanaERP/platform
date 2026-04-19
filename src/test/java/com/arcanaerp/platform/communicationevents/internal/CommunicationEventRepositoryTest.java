package com.arcanaerp.platform.communicationevents.internal;

import static org.assertj.core.api.Assertions.assertThat;

import com.arcanaerp.platform.communicationevents.CommunicationChannel;
import com.arcanaerp.platform.communicationevents.CommunicationDirection;
import java.time.Instant;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;

@DataJpaTest
class CommunicationEventRepositoryTest {

    @Autowired
    private CommunicationEventRepository communicationEventRepository;

    @Test
    void findsEventByTenantAndEventNumber() {
        communicationEventRepository.save(CommunicationEvent.create(
            "COMM-1001",
            "TENANT-A",
            "OPEN",
            "Open",
            "SUPPORT",
            "Support",
            CommunicationChannel.EMAIL,
            CommunicationDirection.INBOUND,
            "Support Request",
            "Customer requested support",
            Instant.parse("2026-04-18T10:15:30Z"),
            "agent@tenant-a.com",
            null,
            Instant.parse("2026-04-18T10:16:30Z")
        ));

        CommunicationEvent event = communicationEventRepository.findByTenantCodeAndEventNumber("TENANT-A", "COMM-1001")
            .orElseThrow();

        assertThat(event.getSubject()).isEqualTo("Support Request");
        assertThat(event.getDirection()).isEqualTo(CommunicationDirection.INBOUND);
    }
}
