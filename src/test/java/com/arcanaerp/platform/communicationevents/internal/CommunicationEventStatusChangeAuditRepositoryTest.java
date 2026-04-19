package com.arcanaerp.platform.communicationevents.internal;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Instant;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;

@DataJpaTest
class CommunicationEventStatusChangeAuditRepositoryTest {

    @Autowired
    private CommunicationEventStatusChangeAuditRepository communicationEventStatusChangeAuditRepository;

    @Test
    void listsStatusChangesForEventOrderedByChangedAtDesc() {
        UUID eventId = UUID.randomUUID();
        communicationEventStatusChangeAuditRepository.save(
            CommunicationEventStatusChangeAudit.create(
                eventId,
                "OPEN",
                "Open",
                "CLOSED",
                "Closed",
                "TENANT02",
                "Resolved",
                "ops02@arcanaerp.com",
                Instant.parse("2026-04-18T01:00:00Z")
            )
        );
        communicationEventStatusChangeAuditRepository.save(
            CommunicationEventStatusChangeAudit.create(
                eventId,
                "NEW",
                "New",
                "OPEN",
                "Open",
                "TENANT01",
                "Picked up",
                "ops01@arcanaerp.com",
                Instant.parse("2026-04-18T02:00:00Z")
            )
        );

        var page = communicationEventStatusChangeAuditRepository.findHistoryFiltered(
            eventId,
            null,
            null,
            null,
            null,
            PageRequest.of(0, 10, Sort.by(Sort.Direction.DESC, "changedAt"))
        );

        assertThat(page.getTotalElements()).isEqualTo(2);
        assertThat(page.getContent().get(0).getCurrentStatusCode()).isEqualTo("OPEN");
        assertThat(page.getContent().get(0).getTenantCode()).isEqualTo("TENANT01");
        assertThat(page.getContent().get(0).getChangedBy()).isEqualTo("ops01@arcanaerp.com");
        assertThat(page.getContent().get(0).getReason()).isEqualTo("Picked up");
        assertThat(page.getContent().get(1).getCurrentStatusCode()).isEqualTo("CLOSED");
    }

    @Test
    void filtersStatusHistoryByTenantChangedByAndChangedAtRange() {
        UUID eventId = UUID.randomUUID();
        communicationEventStatusChangeAuditRepository.save(
            CommunicationEventStatusChangeAudit.create(
                eventId,
                "NEW",
                "New",
                "OPEN",
                "Open",
                "TENANT01",
                "Accepted",
                "actor.one@arcanaerp.com",
                Instant.parse("2026-04-18T01:00:00Z")
            )
        );
        communicationEventStatusChangeAuditRepository.save(
            CommunicationEventStatusChangeAudit.create(
                eventId,
                "OPEN",
                "Open",
                "CLOSED",
                "Closed",
                "TENANT02",
                "Completed",
                "actor.two@arcanaerp.com",
                Instant.parse("2026-04-18T02:00:00Z")
            )
        );

        var tenantFiltered = communicationEventStatusChangeAuditRepository.findHistoryFiltered(
            eventId,
            "TENANT01",
            null,
            null,
            null,
            PageRequest.of(0, 10)
        );
        var actorFiltered = communicationEventStatusChangeAuditRepository.findHistoryFiltered(
            eventId,
            null,
            "actor.two@arcanaerp.com",
            null,
            null,
            PageRequest.of(0, 10)
        );
        var rangeFiltered = communicationEventStatusChangeAuditRepository.findHistoryFiltered(
            eventId,
            null,
            null,
            Instant.parse("2026-04-18T01:30:00Z"),
            Instant.parse("2026-04-18T02:30:00Z"),
            PageRequest.of(0, 10)
        );

        assertThat(tenantFiltered.getTotalElements()).isEqualTo(1);
        assertThat(tenantFiltered.getContent().get(0).getTenantCode()).isEqualTo("TENANT01");
        assertThat(actorFiltered.getTotalElements()).isEqualTo(1);
        assertThat(actorFiltered.getContent().get(0).getChangedBy()).isEqualTo("actor.two@arcanaerp.com");
        assertThat(rangeFiltered.getTotalElements()).isEqualTo(1);
        assertThat(rangeFiltered.getContent().get(0).getCurrentStatusCode()).isEqualTo("CLOSED");
    }
}
