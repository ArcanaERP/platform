package com.arcanaerp.platform.workeffort.internal;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.Table;
import java.time.Instant;
import java.util.UUID;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Table(
    name = "party_skills",
    indexes = {
        @Index(name = "idx_party_skills_party_skill", columnList = "partyId,skillTypeId")
    }
)
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
class PartySkill {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    private Long partyId;

    private Long skillTypeId;

    @Column(nullable = false, updatable = false)
    private Instant createdAt;

    static PartySkill create(Long partyId, Long skillTypeId, Instant createdAt) {
        if (createdAt == null) {
            throw new IllegalArgumentException("createdAt is required");
        }
        PartySkill skill = new PartySkill();
        skill.partyId = partyId;
        skill.skillTypeId = skillTypeId;
        skill.createdAt = createdAt;
        return skill;
    }
}
