package com.arcanaerp.platform.identity.internal;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Instant;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;

@DataJpaTest
class UserAccountRepositoryTest {

    @Autowired
    private TenantRepository tenantRepository;

    @Autowired
    private RoleRepository roleRepository;

    @Autowired
    private UserAccountRepository userAccountRepository;

    @Test
    void findsUserByTenantAndEmail() {
        Instant now = Instant.parse("2026-02-28T00:00:00Z");
        Tenant tenant = tenantRepository.save(Tenant.create("acme", "Acme Corp", now));
        Role role = roleRepository.save(Role.create(tenant.getId(), "admin", "Administrator", now));
        userAccountRepository.save(
            UserAccount.create(tenant.getId(), role.getId(), "ops@acme.com", "Ops User", now)
        );

        UserAccount user = userAccountRepository.findByTenantIdAndEmail(tenant.getId(), "ops@acme.com")
            .orElseThrow();

        assertThat(user.getDisplayName()).isEqualTo("Ops User");
        assertThat(user.getRoleId()).isEqualTo(role.getId());
    }
}
