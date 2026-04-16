package com.arcanaerp.platform.identity.internal;

import com.arcanaerp.platform.core.pagination.PageQuery;
import com.arcanaerp.platform.core.pagination.PageResult;
import com.arcanaerp.platform.identity.IdentityActorLookup;
import com.arcanaerp.platform.identity.RegisterUserCommand;
import com.arcanaerp.platform.identity.UpdateUserCommand;
import com.arcanaerp.platform.identity.UserDirectory;
import com.arcanaerp.platform.identity.UserView;
import java.time.Clock;
import java.time.Instant;
import java.util.HashMap;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.Set;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional
@RequiredArgsConstructor
class UserDirectoryService implements UserDirectory, IdentityActorLookup {

    private final TenantRepository tenantRepository;
    private final RoleRepository roleRepository;
    private final UserAccountRepository userAccountRepository;
    private final Clock clock;

    @Override
    public UserView registerUser(RegisterUserCommand command) {
        String tenantCode = normalizeRequired(command.tenantCode(), "tenantCode").toUpperCase();
        String tenantName = normalizeRequired(command.tenantName(), "tenantName");
        String roleCode = normalizeRequired(command.roleCode(), "roleCode").toUpperCase();
        String roleName = normalizeRequired(command.roleName(), "roleName");
        String normalizedEmail = normalizeEmail(command.email());
        String displayName = normalizeRequired(command.displayName(), "displayName");
        Instant now = Instant.now(clock);

        Tenant tenant = tenantRepository.findByCode(tenantCode)
            .orElseGet(() -> tenantRepository.save(Tenant.create(tenantCode, tenantName, now)));

        Role role = roleRepository.findByTenantIdAndCode(tenant.getId(), roleCode)
            .orElseGet(() -> roleRepository.save(Role.create(tenant.getId(), roleCode, roleName, now)));

        if (userAccountRepository.findByTenantIdAndEmail(tenant.getId(), normalizedEmail).isPresent()) {
            throw new IllegalArgumentException("User email already exists in tenant: " + normalizedEmail);
        }

        UserAccount user = userAccountRepository.save(
            UserAccount.create(tenant.getId(), role.getId(), normalizedEmail, displayName, now)
        );
        return toView(user, tenant, role);
    }

    @Override
    @Transactional
    public UserView updateUser(UpdateUserCommand command) {
        UUID normalizedUserId = parseRequiredUserId(command.userId());
        String normalizedDisplayName = normalizeRequired(command.displayName(), "displayName");

        UserAccount user = userAccountRepository.findById(normalizedUserId)
            .orElseThrow(() -> new NoSuchElementException("User not found: " + normalizedUserId));
        user.update(normalizedDisplayName, command.active());
        UserAccount saved = userAccountRepository.save(user);
        Tenant tenant = tenantRepository.findById(saved.getTenantId())
            .orElseThrow(() -> new NoSuchElementException("Tenant not found for user: " + normalizedUserId));
        Role role = roleRepository.findById(saved.getRoleId())
            .orElseThrow(() -> new NoSuchElementException("Role not found for user: " + normalizedUserId));
        return toView(saved, tenant, role);
    }

    @Override
    @Transactional(readOnly = true)
    public UserView userById(String userId) {
        UUID normalizedUserId = parseRequiredUserId(userId);

        UserAccount user = userAccountRepository.findById(normalizedUserId)
            .orElseThrow(() -> new NoSuchElementException("User not found: " + normalizedUserId));
        Tenant tenant = tenantRepository.findById(user.getTenantId())
            .orElseThrow(() -> new NoSuchElementException("Tenant not found for user: " + normalizedUserId));
        Role role = roleRepository.findById(user.getRoleId())
            .orElseThrow(() -> new NoSuchElementException("Role not found for user: " + normalizedUserId));
        return toView(user, tenant, role);
    }

    @Override
    @Transactional(readOnly = true)
    public PageResult<UserView> listUsers(PageQuery pageQuery) {
        Page<UserAccount> users = userAccountRepository.findAll(
            pageQuery.toPageable(Sort.by(Sort.Direction.DESC, "createdAt"))
        );

        Set<UUID> tenantIds = users.stream().map(UserAccount::getTenantId).collect(java.util.stream.Collectors.toSet());
        Set<UUID> roleIds = users.stream().map(UserAccount::getRoleId).collect(java.util.stream.Collectors.toSet());
        Map<UUID, Tenant> tenantsById = new HashMap<>();
        Map<UUID, Role> rolesById = new HashMap<>();
        tenantRepository.findAllById(tenantIds).forEach(tenant -> tenantsById.put(tenant.getId(), tenant));
        roleRepository.findAllById(roleIds).forEach(role -> rolesById.put(role.getId(), role));

        return PageResult.from(users)
            .map(user -> toView(user, tenantsById.get(user.getTenantId()), rolesById.get(user.getRoleId())));
    }

    @Override
    @Transactional(readOnly = true)
    public boolean actorExists(String tenantCode, String actorEmail) {
        if (tenantCode == null || tenantCode.isBlank() || actorEmail == null || actorEmail.isBlank()) {
            return false;
        }
        try {
            String normalizedTenantCode = normalizeRequired(tenantCode, "tenantCode").toUpperCase();
            String normalizedEmail = normalizeEmail(actorEmail);
            return tenantRepository.findByCode(normalizedTenantCode)
                .map(tenant -> userAccountRepository.existsByTenantIdAndEmail(tenant.getId(), normalizedEmail))
                .orElse(false);
        } catch (IllegalArgumentException ignored) {
            return false;
        }
    }

    private UserView toView(UserAccount user, Tenant tenant, Role role) {
        return new UserView(
            user.getId(),
            user.getTenantId(),
            tenant == null ? null : tenant.getCode(),
            tenant == null ? null : tenant.getName(),
            user.getRoleId(),
            role == null ? null : role.getCode(),
            role == null ? null : role.getName(),
            user.getEmail(),
            user.getDisplayName(),
            user.isActive(),
            user.getCreatedAt()
        );
    }

    private static String normalizeRequired(String value, String fieldName) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException(fieldName + " is required");
        }
        return value.trim();
    }

    private static String normalizeEmail(String email) {
        String normalized = normalizeRequired(email, "email").toLowerCase();
        if (!normalized.contains("@")) {
            throw new IllegalArgumentException("email is invalid");
        }
        return normalized;
    }

    private static UUID parseRequiredUserId(String userId) {
        try {
            return UUID.fromString(normalizeRequired(userId, "userId"));
        } catch (IllegalArgumentException ex) {
            throw new IllegalArgumentException("userId is invalid");
        }
    }
}
