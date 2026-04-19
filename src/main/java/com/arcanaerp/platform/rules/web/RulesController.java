package com.arcanaerp.platform.rules.web;

import com.arcanaerp.platform.core.pagination.PageQuery;
import com.arcanaerp.platform.core.pagination.PageResult;
import com.arcanaerp.platform.rules.RegisterRuleDefinitionCommand;
import com.arcanaerp.platform.rules.RuleCatalog;
import com.arcanaerp.platform.rules.RuleDefinitionView;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/rules")
@RequiredArgsConstructor
public class RulesController {

    private final RuleCatalog ruleCatalog;

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public RuleDefinitionResponse registerRuleDefinition(@Valid @RequestBody CreateRuleDefinitionRequest request) {
        RuleDefinitionView created = ruleCatalog.registerRuleDefinition(
            new RegisterRuleDefinitionCommand(
                request.tenantCode(),
                request.code(),
                request.name(),
                request.appliesTo(),
                request.expression(),
                request.description(),
                request.active()
            )
        );
        return toResponse(created);
    }

    @GetMapping("/{code}")
    public RuleDefinitionResponse getRuleDefinition(
        @PathVariable String code,
        @RequestParam String tenantCode
    ) {
        return toResponse(ruleCatalog.getRuleDefinition(tenantCode, code));
    }

    @GetMapping
    public PageResult<RuleDefinitionResponse> listRuleDefinitions(
        @RequestParam String tenantCode,
        @RequestParam(required = false) Boolean active,
        @RequestParam(required = false) String appliesTo,
        @RequestParam(required = false) Integer page,
        @RequestParam(required = false) Integer size
    ) {
        return ruleCatalog.listRuleDefinitions(
            tenantCode,
            PageQuery.of(page, size),
            active,
            normalizeOptionalAppliesTo(appliesTo)
        ).map(this::toResponse);
    }

    private RuleDefinitionResponse toResponse(RuleDefinitionView view) {
        return new RuleDefinitionResponse(
            view.id(),
            view.tenantCode(),
            view.code(),
            view.name(),
            view.appliesTo(),
            view.expression(),
            view.description(),
            view.active(),
            view.createdAt()
        );
    }

    private static String normalizeOptionalAppliesTo(String appliesTo) {
        if (appliesTo == null) {
            return null;
        }
        if (appliesTo.isBlank()) {
            throw new IllegalArgumentException("appliesTo query parameter must not be blank");
        }
        return appliesTo.trim().toUpperCase();
    }
}
