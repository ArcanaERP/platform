package com.arcanaerp.platform.communicationevents.internal;

import com.arcanaerp.platform.communicationevents.ChangeCommunicationEventStatusCommand;
import com.arcanaerp.platform.communicationevents.CommunicationChannel;
import com.arcanaerp.platform.communicationevents.CommunicationDirection;
import com.arcanaerp.platform.communicationevents.CommunicationEventLog;
import com.arcanaerp.platform.communicationevents.CommunicationEventStatusChangeView;
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
    private final CommunicationEventStatusChangeAuditRepository communicationEventStatusChangeAuditRepository;
    private final CommunicationEventStatusTypeRepository statusTypeRepository;
    private final CommunicationEventPurposeTypeRepository purposeTypeRepository;
    private final IdentityActorLookup identityActorLookup;
    private final Clock clock;

    @Override
    public CommunicationEventView createEvent(CreateCommunicationEventCommand command) {
        String tenantCode = normalizeRequired(command.tenantCode(), "tenantCode").toUpperCase(Locale.ROOT);
        CommunicationEventStatusType statusType = requireStatusType(tenantCode, command.statusCode());
        CommunicationEventPurposeType purposeType = requirePurposeType(tenantCode, command.purposeCode());
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
                statusType.getCode(),
                statusType.getName(),
                purposeType.getCode(),
                purposeType.getName(),
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
    public CommunicationEventView changeStatus(ChangeCommunicationEventStatusCommand command) {
        String tenantCode = normalizeRequired(command.tenantCode(), "tenantCode").toUpperCase(Locale.ROOT);
        String eventNumber = normalizeRequired(command.eventNumber(), "eventNumber").toUpperCase(Locale.ROOT);
        String reason = normalizeRequired(command.reason(), "reason");
        String changedBy = normalizeActor(command.changedBy());

        CommunicationEvent event = communicationEventRepository.findByTenantCodeAndEventNumber(tenantCode, eventNumber)
            .orElseThrow(() -> new NoSuchElementException(
                "Communication event not found for tenant/eventNumber: " + tenantCode + "/" + eventNumber
            ));
        CommunicationEventStatusType statusType = requireStatusType(tenantCode, command.statusCode());

        if (!identityActorLookup.actorExists(tenantCode, changedBy)) {
            throw new IllegalArgumentException("communication event status actor not found in tenant: " + tenantCode + "/" + changedBy);
        }

        String previousStatusCode = event.getStatusCode();
        String previousStatusName = event.getStatusName();
        event.changeStatus(statusType.getCode(), statusType.getName());
        CommunicationEvent saved = communicationEventRepository.save(event);
        if (!previousStatusCode.equals(saved.getStatusCode())) {
            communicationEventStatusChangeAuditRepository.save(
                CommunicationEventStatusChangeAudit.create(
                    saved.getId(),
                    previousStatusCode,
                    previousStatusName,
                    saved.getStatusCode(),
                    saved.getStatusName(),
                    tenantCode,
                    reason,
                    changedBy,
                    Instant.now(clock)
                )
            );
        }
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
        String statusCode,
        String purposeCode,
        String channel,
        String direction,
        String recordedBy
    ) {
        String normalizedTenantCode = normalizeRequired(tenantCode, "tenantCode").toUpperCase(Locale.ROOT);
        String normalizedStatusCode = normalizeOptionalCode(statusCode, "statusCode");
        String normalizedPurposeCode = normalizeOptionalCode(purposeCode, "purposeCode");
        CommunicationChannel normalizedChannel = parseOptionalChannel(channel);
        CommunicationDirection normalizedDirection = parseOptionalDirection(direction);
        String normalizedRecordedBy = normalizeOptionalActor(recordedBy);

        Specification<CommunicationEvent> specification = (root, query, criteriaBuilder) -> {
            var predicates = new ArrayList<Predicate>();
            predicates.add(criteriaBuilder.equal(root.get("tenantCode"), normalizedTenantCode));
            if (normalizedStatusCode != null) {
                predicates.add(criteriaBuilder.equal(root.get("statusCode"), normalizedStatusCode));
            }
            if (normalizedPurposeCode != null) {
                predicates.add(criteriaBuilder.equal(root.get("purposeCode"), normalizedPurposeCode));
            }
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

    @Override
    @Transactional(readOnly = true)
    public PageResult<CommunicationEventStatusChangeView> listStatusHistory(
        String tenantCode,
        String eventNumber,
        String changedBy,
        Instant changedAtFrom,
        Instant changedAtTo,
        PageQuery pageQuery
    ) {
        String normalizedTenantCode = normalizeRequired(tenantCode, "tenantCode").toUpperCase(Locale.ROOT);
        String normalizedEventNumber = normalizeRequired(eventNumber, "eventNumber").toUpperCase(Locale.ROOT);
        String normalizedChangedBy = normalizeOptionalActor(changedBy);

        CommunicationEvent event = communicationEventRepository.findByTenantCodeAndEventNumber(normalizedTenantCode, normalizedEventNumber)
            .orElseThrow(() -> new NoSuchElementException(
                "Communication event not found for tenant/eventNumber: " + normalizedTenantCode + "/" + normalizedEventNumber
            ));
        Page<CommunicationEventStatusChangeAudit> page = communicationEventStatusChangeAuditRepository.findHistoryFiltered(
            event.getId(),
            normalizedTenantCode,
            normalizedChangedBy,
            changedAtFrom,
            changedAtTo,
            pageQuery.toPageable(Sort.by(Sort.Direction.DESC, "changedAt"))
        );
        return PageResult.from(page).map(audit -> new CommunicationEventStatusChangeView(
            audit.getId(),
            event.getEventNumber(),
            audit.getPreviousStatusCode(),
            audit.getPreviousStatusName(),
            audit.getCurrentStatusCode(),
            audit.getCurrentStatusName(),
            audit.getTenantCode(),
            audit.getReason(),
            audit.getChangedBy(),
            audit.getChangedAt()
        ));
    }

    private CommunicationEventView toView(CommunicationEvent event) {
        return new CommunicationEventView(
            event.getId(),
            event.getEventNumber(),
            event.getTenantCode(),
            event.getStatusCode(),
            event.getStatusName(),
            event.getPurposeCode(),
            event.getPurposeName(),
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

    private CommunicationEventStatusType requireStatusType(String tenantCode, String statusCode) {
        String normalizedStatusCode = normalizeRequired(statusCode, "statusCode").toUpperCase(Locale.ROOT);
        return statusTypeRepository.findByTenantCodeAndCode(tenantCode, normalizedStatusCode)
            .orElseThrow(() -> new IllegalArgumentException(
                "communication event status type not found for tenant/code: " + tenantCode + "/" + normalizedStatusCode
            ));
    }

    private CommunicationEventPurposeType requirePurposeType(String tenantCode, String purposeCode) {
        String normalizedPurposeCode = normalizeRequired(purposeCode, "purposeCode").toUpperCase(Locale.ROOT);
        return purposeTypeRepository.findByTenantCodeAndCode(tenantCode, normalizedPurposeCode)
            .orElseThrow(() -> new IllegalArgumentException(
                "communication event purpose type not found for tenant/code: " + tenantCode + "/" + normalizedPurposeCode
            ));
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

    private static String normalizeOptionalCode(String value, String parameterName) {
        if (value == null) {
            return null;
        }
        if (value.isBlank()) {
            throw new IllegalArgumentException(parameterName + " query parameter must not be blank");
        }
        return value.trim().toUpperCase(Locale.ROOT);
    }
}
