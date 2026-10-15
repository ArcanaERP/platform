package com.arcanaerp.platform.agreements.internal;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.Instant;
import java.util.UUID;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "loyalty_program_codes")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
class LoyaltyProgramCode {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(length = 255)
    private String identifier;

    @Column(length = 255)
    private String name;

    @Column(length = 255)
    private String description;

    @Column(nullable = false, updatable = false)
    private Instant createdAt;

    static LoyaltyProgramCode create(
        String identifier,
        String name,
        String description,
        Instant createdAt
    ) {
        if (createdAt == null) {
            throw new IllegalArgumentException("createdAt is required");
        }
        LoyaltyProgramCode code = new LoyaltyProgramCode();
        code.identifier = normalizeOptional(identifier);
        code.name = normalizeOptional(name);
        code.description = normalizeOptional(description);
        code.createdAt = createdAt;
        return code;
    }

    private static String normalizeOptional(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        return value.trim();
    }
}
