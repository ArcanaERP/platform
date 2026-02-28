package com.arcanaerp.platform.identity;

import com.arcanaerp.platform.core.pagination.PageQuery;
import com.arcanaerp.platform.core.pagination.PageResult;

public interface UserDirectory {

    UserView registerUser(RegisterUserCommand command);

    PageResult<UserView> listUsers(PageQuery pageQuery);
}
