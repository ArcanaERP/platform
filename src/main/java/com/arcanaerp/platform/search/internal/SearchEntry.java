package com.arcanaerp.platform.search.internal;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import java.time.Instant;
import java.util.UUID;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Table(
    name = "search_entries",
    uniqueConstraints = @UniqueConstraint(name = "uk_search_entries_tenant_entry_number", columnNames = {"tenantCode", "entryNumber"}),
    indexes = {
        @Index(name = "idx_search_entries_tenant_created_at", columnList = "tenantCode,createdAt"),
        @Index(name = "idx_search_entries_tenant_category", columnList = "tenantCode,category")
    }
)
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
class SearchEntry {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(nullable = false, length = 64)
    private String tenantCode;

    @Column(nullable = false, length = 64)
    private String entryNumber;

    @Column(nullable = false, length = 255)
    private String title;

    @Column(nullable = false, length = 1000)
    private String snippet;

    @Column(nullable = false, length = 64)
    private String category;

    @Column(nullable = false, length = 64)
    private String targetType;

    @Column(nullable = false, length = 128)
    private String targetIdentifier;

    @Column(nullable = false, length = 1024)
    private String targetUri;

    @Column(nullable = false, updatable = false)
    private Instant createdAt;

    private SearchEntry(
        UUID id,
        String tenantCode,
        String entryNumber,
        String title,
        String snippet,
        String category,
        String targetType,
        String targetIdentifier,
        String targetUri,
        Instant createdAt
    ) {
        this.id = id;
        this.tenantCode = tenantCode;
        this.entryNumber = entryNumber;
        this.title = title;
        this.snippet = snippet;
        this.category = category;
        this.targetType = targetType;
        this.targetIdentifier = targetIdentifier;
        this.targetUri = targetUri;
        this.createdAt = createdAt;
    }

    static SearchEntry create(
        String tenantCode,
        String entryNumber,
        String title,
        String snippet,
        String category,
        String targetType,
        String targetIdentifier,
        String targetUri,
        Instant createdAt
    ) {
        if (createdAt == null) {
            throw new IllegalArgumentException("createdAt is required");
        }
        return new SearchEntry(
            null,
            normalizeRequired(tenantCode, "tenantCode").toUpperCase(),
            normalizeRequired(entryNumber, "entryNumber").toUpperCase(),
            normalizeRequired(title, "title"),
            normalizeRequired(snippet, "snippet"),
            normalizeRequired(category, "category").toUpperCase(),
            normalizeRequired(targetType, "targetType").toUpperCase(),
            normalizeRequired(targetIdentifier, "targetIdentifier"),
            normalizeRequired(targetUri, "targetUri"),
            createdAt
        );
    }

    private static String normalizeRequired(String value, String fieldName) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException(fieldName + " is required");
        }
        return value.trim();
    }
}
