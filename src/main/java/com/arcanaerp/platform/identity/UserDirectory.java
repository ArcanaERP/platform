package com.arcanaerp.platform.identity;

import java.util.List;

public interface UserDirectory {

    UserView registerUser(RegisterUserCommand command);

    List<UserView> listUsers();
}
