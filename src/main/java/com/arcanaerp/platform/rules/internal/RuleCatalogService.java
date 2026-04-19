package com.arcanaerp.platform.rules.internal;

import com.arcanaerp.platform.core.api.ConflictException;
import com.arcanaerp.platform.core.pagination.PageQuery;
import com.arcanaerp.platform.core.pagination.PageResult;
import com.arcanaerp.platform.rules.RegisterRuleDefinitionCommand;
import com.arcanaerp.platform.rules.RuleCatalog;
import com.arcanaerp.platform.rules.RuleDefinitionView;
import java.time.Clock;
import java.time.Instant;
import java.util.NoSuchElementException;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional
@RequiredArgsConstructor
class RuleCatalogService implements RuleCatalog {

    private final RuleDefinitionRepository ruleDefinitionRepository;
    private final Clock clock;

    @Override
    public RuleDefinitionView registerRuleDefinition(RegisterRuleDefinitionCommand command) {
        String tenantCode = normalizeRequired(command.tenantCode(), "tenantCode").toUpperCase();
        String code = normalizeRequired(command.code(), "code").toUpperCase();
        String name = normalizeRequired(command.name(), "name");
        String appliesTo = normalizeRequired(command.appliesTo(), "appliesTo").toUpperCase();
        String expression = normalizeRequired(command.expression(), "expression");
        String description = normalizeOptional(command.description());
        Instant now = Instant.now(clock);

        if (ruleDefinitionRepository.findByTenantCodeAndCode(tenantCode, code).isPresent()) {
            throw new ConflictException("Rule definition already exists for tenant/code: " + tenantCode + "/" + code);
        }

        RuleDefinition created = ruleDefinitionRepository.save(
            RuleDefinition.create(tenantCode, code, name, appliesTo, expression, description, command.active(), now)
        );
        return toView(created);
    }

    @Override
    @Transactional(readOnly = true)
    public RuleDefinitionView getRuleDefinition(String tenantCode, String code) {
        String normalizedTenantCode = normalizeRequired(tenantCode, "tenantCode").toUpperCase();
        String normalizedCode = normalizeRequired(code, "code").toUpperCase();
        RuleDefinition rule = ruleDefinitionRepository.findByTenantCodeAndCode(normalizedTenantCode, normalizedCode)
            .orElseThrow(() -> new NoSuchElementException(
                "Rule definition not found for tenant/code: " + normalizedTenantCode + "/" + normalizedCode
            ));
        return toView(rule);
    }

    @Override
    @Transactional(readOnly = true)
    public PageResult<RuleDefinitionView> listRuleDefinitions(
        String tenantCode,
        PageQuery pageQuery,
        Boolean active,
        String appliesTo
    ) {
        String normalizedTenantCode = normalizeRequired(tenantCode, "tenantCode").toUpperCase();
        String normalizedAppliesTo = appliesTo == null ? null : normalizeOptionalFilter(appliesTo, "appliesTo").toUpperCase();
        Page<RuleDefinition> page = findRules(normalizedTenantCode, active, normalizedAppliesTo, pageQuery);
        return PageResult.from(page).map(this::toView);
    }

    private Page<RuleDefinition> findRules(String tenantCode, Boolean active, String appliesTo, PageQuery pageQuery) {
        var pageable = pageQuery.toPageable(Sort.by(Sort.Direction.DESC, "createdAt"));
        if (active != null && appliesTo != null) {
            return ruleDefinitionRepository.findByTenantCodeAndActiveAndAppliesTo(tenantCode, active, appliesTo, pageable);
        }
        if (active != null) {
            return ruleDefinitionRepository.findByTenantCodeAndActive(tenantCode, active, pageable);
        }
        if (appliesTo != null) {
            return ruleDefinitionRepository.findByTenantCodeAndAppliesTo(tenantCode, appliesTo, pageable);
        }
        return ruleDefinitionRepository.findByTenantCode(tenantCode, pageable);
    }

    private RuleDefinitionView toView(RuleDefinition rule) {
        return new RuleDefinitionView(
            rule.getId(),
            rule.getTenantCode(),
            rule.getCode(),
            rule.getName(),
            rule.getAppliesTo(),
            rule.getExpression(),
            rule.getDescription(),
            rule.isActive(),
            rule.getCreatedAt()
        );
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

    private static String normalizeOptionalFilter(String value, String fieldName) {
        if (value.isBlank()) {
            throw new IllegalArgumentException(fieldName + " query parameter must not be blank");
        }
        return value.trim();
    }
}
