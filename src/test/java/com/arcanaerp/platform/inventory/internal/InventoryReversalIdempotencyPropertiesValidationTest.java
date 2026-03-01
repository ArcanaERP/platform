package com.arcanaerp.platform.inventory.internal;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;
import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.autoconfigure.context.ConfigurationPropertiesAutoConfiguration;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.context.annotation.Configuration;

class InventoryReversalIdempotencyPropertiesValidationTest {

    private static final String TTL_VALIDATION_MESSAGE =
        "arcanaerp.inventory.reversal-idempotency.pending-claim-ttl must be positive";

    private final ApplicationContextRunner contextRunner = new ApplicationContextRunner()
        .withConfiguration(AutoConfigurations.of(ConfigurationPropertiesAutoConfiguration.class))
        .withUserConfiguration(PropertiesConfig.class);

    @Test
    void rejectsZeroPendingClaimTtl() {
        contextRunner
            .withPropertyValues("arcanaerp.inventory.reversal-idempotency.pending-claim-ttl=PT0S")
            .run(context -> {
                assertThat(context).hasFailed();
                assertThat(context.getStartupFailure()).isNotNull();
                assertThat(rootCause(context.getStartupFailure()))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessage(TTL_VALIDATION_MESSAGE);
            });
    }

    @Test
    void rejectsNegativePendingClaimTtl() {
        contextRunner
            .withPropertyValues("arcanaerp.inventory.reversal-idempotency.pending-claim-ttl=-PT1M")
            .run(context -> {
                assertThat(context).hasFailed();
                assertThat(context.getStartupFailure()).isNotNull();
                assertThat(rootCause(context.getStartupFailure()))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessage(TTL_VALIDATION_MESSAGE);
            });
    }

    private static Throwable rootCause(Throwable throwable) {
        Throwable current = throwable;
        while (current.getCause() != null) {
            current = current.getCause();
        }
        return current;
    }

    @Configuration(proxyBeanMethods = false)
    @EnableConfigurationProperties(InventoryReversalIdempotencyProperties.class)
    static class PropertiesConfig {}
}
