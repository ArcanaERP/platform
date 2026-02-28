package com.arcanaerp.platform.identity.web;

import com.arcanaerp.platform.identity.RegisterUserCommand;
import com.arcanaerp.platform.identity.UserDirectory;
import com.arcanaerp.platform.identity.UserView;
import jakarta.validation.Valid;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
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

    @GetMapping
    public List<UserResponse> listUsers() {
        return userDirectory.listUsers().stream().map(this::toResponse).toList();
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
