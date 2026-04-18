package com.arcanaerp.platform.identity;

import com.arcanaerp.platform.core.pagination.PageQuery;
import com.arcanaerp.platform.core.pagination.PageResult;

public interface UserDirectory {

    UserView registerUser(RegisterUserCommand command);

    UserView updateUser(UpdateUserCommand command);

    UserView userById(String userId);

    PageResult<UserView> listUsers(PageQuery pageQuery, String tenantCode, String roleCode, Boolean active);
}
