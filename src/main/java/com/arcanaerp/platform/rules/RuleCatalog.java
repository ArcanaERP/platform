package com.arcanaerp.platform.rules;

import com.arcanaerp.platform.core.pagination.PageQuery;
import com.arcanaerp.platform.core.pagination.PageResult;

public interface RuleCatalog {

    RuleDefinitionView registerRuleDefinition(RegisterRuleDefinitionCommand command);

    RuleDefinitionView getRuleDefinition(String tenantCode, String code);

    PageResult<RuleDefinitionView> listRuleDefinitions(
        String tenantCode,
        PageQuery pageQuery,
        Boolean active,
        String appliesTo
    );
}
