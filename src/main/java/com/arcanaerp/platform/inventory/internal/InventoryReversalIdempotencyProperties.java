package com.arcanaerp.platform.inventory.internal;

import jakarta.annotation.PostConstruct;
import java.time.Duration;
import lombok.Getter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Component
@Getter
@ConfigurationProperties(prefix = "arcanaerp.inventory.reversal-idempotency")
class InventoryReversalIdempotencyProperties {

    private Duration pendingClaimTtl = Duration.ofMinutes(5);

    void setPendingClaimTtl(Duration pendingClaimTtl) {
        this.pendingClaimTtl = pendingClaimTtl;
    }

    @PostConstruct
    void validate() {
        if (pendingClaimTtl == null || pendingClaimTtl.isZero() || pendingClaimTtl.isNegative()) {
            throw new IllegalArgumentException("arcanaerp.inventory.reversal-idempotency.pending-claim-ttl must be positive");
        }
    }
}
