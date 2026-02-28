package com.arcanaerp.platform.identity.internal;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.time.Instant;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class IdentityDomainTest {

    @Test
    void tenantCreateNormalizesCodeAndName() {
        Tenant tenant = Tenant.create("  acme  ", "  Acme Corp  ", Instant.parse("2026-02-28T00:00:00Z"));

        assertThat(tenant.getCode()).isEqualTo("ACME");
        assertThat(tenant.getName()).isEqualTo("Acme Corp");
    }

    @Test
    void roleCreateRequiresTenantId() {
        assertThatThrownBy(() ->
            Role.create(null, "admin", "Administrator", Instant.parse("2026-02-28T00:00:00Z"))
        )
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessage("tenantId is required");
    }

    @Test
    void userCreateNormalizesEmailAndValidatesFormat() {
        UserAccount user = UserAccount.create(
            UUID.randomUUID(),
            UUID.randomUUID(),
            "  USER@Acme.COM ",
            "Ops User",
            Instant.parse("2026-02-28T00:00:00Z")
        );
        assertThat(user.getEmail()).isEqualTo("user@acme.com");

        assertThatThrownBy(() ->
            UserAccount.create(
                UUID.randomUUID(),
                UUID.randomUUID(),
                "not-an-email",
                "Ops User",
                Instant.parse("2026-02-28T00:00:00Z")
            )
        )
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessage("email is invalid");
    }
}
