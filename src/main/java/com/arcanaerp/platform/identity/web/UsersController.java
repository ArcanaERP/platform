package com.arcanaerp.platform.identity.web;

import com.arcanaerp.platform.core.pagination.PageQuery;
import com.arcanaerp.platform.core.pagination.PageResult;
import com.arcanaerp.platform.identity.RegisterUserCommand;
import com.arcanaerp.platform.identity.UpdateUserCommand;
import com.arcanaerp.platform.identity.UserDirectory;
import com.arcanaerp.platform.identity.UserView;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/identity/users")
@RequiredArgsConstructor
public class UsersController {

    private final UserDirectory userDirectory;

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public UserResponse registerUser(@Valid @RequestBody CreateUserRequest request) {
        UserView created = userDirectory.registerUser(
            new RegisterUserCommand(
                request.tenantCode(),
                request.tenantName(),
                request.roleCode(),
                request.roleName(),
                request.email(),
                request.displayName()
            )
        );
        return toResponse(created);
    }

    @GetMapping("/{userId}")
    public UserResponse userById(@PathVariable String userId) {
        return toResponse(userDirectory.userById(userId));
    }

    @PatchMapping("/{userId}")
    public UserResponse updateUser(
        @PathVariable String userId,
        @Valid @RequestBody UpdateUserRequest request
    ) {
        return toResponse(userDirectory.updateUser(
            new UpdateUserCommand(userId, request.displayName(), request.active())
        ));
    }

    @GetMapping
    public PageResult<UserResponse> listUsers(
        @RequestParam(required = false) Integer page,
        @RequestParam(required = false) Integer size
    ) {
        return userDirectory.listUsers(PageQuery.of(page, size)).map(this::toResponse);
    }

    private UserResponse toResponse(UserView user) {
        return new UserResponse(
            user.id(),
            user.tenantId(),
            user.tenantCode(),
            user.tenantName(),
            user.roleId(),
            user.roleCode(),
            user.roleName(),
            user.email(),
            user.displayName(),
            user.active(),
            user.createdAt()
        );
    }
}
