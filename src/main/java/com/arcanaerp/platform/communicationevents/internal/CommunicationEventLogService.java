package com.arcanaerp.platform.communicationevents.internal;

import com.arcanaerp.platform.communicationevents.CommunicationChannel;
import com.arcanaerp.platform.communicationevents.CommunicationDirection;
import com.arcanaerp.platform.communicationevents.CommunicationEventLog;
import com.arcanaerp.platform.communicationevents.CommunicationEventView;
import com.arcanaerp.platform.communicationevents.CreateCommunicationEventCommand;
import com.arcanaerp.platform.core.pagination.PageQuery;
import com.arcanaerp.platform.core.pagination.PageResult;
import com.arcanaerp.platform.identity.IdentityActorLookup;
import jakarta.persistence.criteria.Predicate;
import java.time.Clock;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Locale;
import java.util.NoSuchElementException;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional
@RequiredArgsConstructor
class CommunicationEventLogService implements CommunicationEventLog {

    private final CommunicationEventRepository communicationEventRepository;
    private final IdentityActorLookup identityActorLookup;
    private final Clock clock;

    @Override
    public CommunicationEventView createEvent(CreateCommunicationEventCommand command) {
        String tenantCode = normalizeRequired(command.tenantCode(), "tenantCode").toUpperCase(Locale.ROOT);
        CommunicationChannel channel = parseChannel(command.channel());
        CommunicationDirection direction = parseDirection(command.direction());
        String subject = normalizeRequired(command.subject(), "subject");
        String summary = normalizeRequired(command.summary(), "summary");
        Instant occurredAt = requireOccurredAt(command.occurredAt());
        String recordedBy = normalizeActor(command.recordedBy());
        String externalReference = normalizeOptional(command.externalReference());
        Instant now = Instant.now(clock);

        if (!identityActorLookup.actorExists(tenantCode, recordedBy)) {
            throw new IllegalArgumentException("recordedBy actor not found in tenant: " + tenantCode + "/" + recordedBy);
        }

        CommunicationEvent saved = communicationEventRepository.save(
            CommunicationEvent.create(
                generateEventNumber(),
                tenantCode,
                channel,
                direction,
                subject,
                summary,
                occurredAt,
                recordedBy,
                externalReference,
                now
            )
        );
        return toView(saved);
    }

    @Override
    @Transactional(readOnly = true)
    public CommunicationEventView getEvent(String tenantCode, String eventNumber) {
        String normalizedTenantCode = normalizeRequired(tenantCode, "tenantCode").toUpperCase(Locale.ROOT);
        String normalizedEventNumber = normalizeRequired(eventNumber, "eventNumber").toUpperCase(Locale.ROOT);

        CommunicationEvent event = communicationEventRepository.findByTenantCodeAndEventNumber(
            normalizedTenantCode,
            normalizedEventNumber
        ).orElseThrow(() -> new NoSuchElementException(
            "Communication event not found for tenant/eventNumber: " + normalizedTenantCode + "/" + normalizedEventNumber
        ));
        return toView(event);
    }

    @Override
    @Transactional(readOnly = true)
    public PageResult<CommunicationEventView> listEvents(
        String tenantCode,
        PageQuery pageQuery,
        String channel,
        String direction,
        String recordedBy
    ) {
        String normalizedTenantCode = normalizeRequired(tenantCode, "tenantCode").toUpperCase(Locale.ROOT);
        CommunicationChannel normalizedChannel = parseOptionalChannel(channel);
        CommunicationDirection normalizedDirection = parseOptionalDirection(direction);
        String normalizedRecordedBy = normalizeOptionalActor(recordedBy);

        Specification<CommunicationEvent> specification = (root, query, criteriaBuilder) -> {
            var predicates = new ArrayList<Predicate>();
            predicates.add(criteriaBuilder.equal(root.get("tenantCode"), normalizedTenantCode));
            if (normalizedChannel != null) {
                predicates.add(criteriaBuilder.equal(root.get("channel"), normalizedChannel));
            }
            if (normalizedDirection != null) {
                predicates.add(criteriaBuilder.equal(root.get("direction"), normalizedDirection));
            }
            if (normalizedRecordedBy != null) {
                predicates.add(criteriaBuilder.equal(root.get("recordedBy"), normalizedRecordedBy));
            }
            return criteriaBuilder.and(predicates.toArray(Predicate[]::new));
        };

        Page<CommunicationEvent> page = communicationEventRepository.findAll(
            specification,
            pageQuery.toPageable(Sort.by(Sort.Direction.DESC, "occurredAt").and(Sort.by(Sort.Direction.DESC, "createdAt")))
        );
        return PageResult.from(page).map(this::toView);
    }

    private CommunicationEventView toView(CommunicationEvent event) {
        return new CommunicationEventView(
            event.getId(),
            event.getEventNumber(),
            event.getTenantCode(),
            event.getChannel(),
            event.getDirection(),
            event.getSubject(),
            event.getSummary(),
            event.getOccurredAt(),
            event.getRecordedBy(),
            event.getExternalReference(),
            event.getCreatedAt()
        );
    }

    private String generateEventNumber() {
        return "COMM-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase(Locale.ROOT);
    }

    private static Instant requireOccurredAt(Instant occurredAt) {
        if (occurredAt == null) {
            throw new IllegalArgumentException("occurredAt is required");
        }
        return occurredAt;
    }

    private static CommunicationChannel parseChannel(String channel) {
        String normalized = normalizeRequired(channel, "channel").toUpperCase(Locale.ROOT);
        try {
            return CommunicationChannel.valueOf(normalized);
        } catch (IllegalArgumentException exception) {
            throw new IllegalArgumentException("channel is invalid");
        }
    }

    private static CommunicationDirection parseDirection(String direction) {
        String normalized = normalizeRequired(direction, "direction").toUpperCase(Locale.ROOT);
        try {
            return CommunicationDirection.valueOf(normalized);
        } catch (IllegalArgumentException exception) {
            throw new IllegalArgumentException("direction is invalid");
        }
    }

    private static CommunicationChannel parseOptionalChannel(String channel) {
        if (channel == null) {
            return null;
        }
        if (channel.isBlank()) {
            throw new IllegalArgumentException("channel query parameter must not be blank");
        }
        try {
            return CommunicationChannel.valueOf(channel.trim().toUpperCase(Locale.ROOT));
        } catch (IllegalArgumentException exception) {
            throw new IllegalArgumentException("channel query parameter is invalid");
        }
    }

    private static CommunicationDirection parseOptionalDirection(String direction) {
        if (direction == null) {
            return null;
        }
        if (direction.isBlank()) {
            throw new IllegalArgumentException("direction query parameter must not be blank");
        }
        try {
            return CommunicationDirection.valueOf(direction.trim().toUpperCase(Locale.ROOT));
        } catch (IllegalArgumentException exception) {
            throw new IllegalArgumentException("direction query parameter is invalid");
        }
    }

    private static String normalizeActor(String recordedBy) {
        String normalized = normalizeRequired(recordedBy, "recordedBy").toLowerCase(Locale.ROOT);
        if (!normalized.contains("@")) {
            throw new IllegalArgumentException("recordedBy is invalid");
        }
        return normalized;
    }

    private static String normalizeOptionalActor(String recordedBy) {
        if (recordedBy == null) {
            return null;
        }
        if (recordedBy.isBlank()) {
            throw new IllegalArgumentException("recordedBy query parameter must not be blank");
        }
        return normalizeActor(recordedBy);
    }

    private static String normalizeRequired(String value, String fieldName) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException(fieldName + " is required");
        }
        return value.trim();
    }

    private static String normalizeOptional(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        return value.trim();
    }
}
