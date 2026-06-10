package com.retail.server.service;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.IService;
import com.retail.server.dto.AdminUserInfoDTO;
import com.retail.server.entity.User;

public interface UserService extends IService<User> {

    String login(String username, String password);

    AdminUserInfoDTO getAdminUserInfo(Long userId);

    void changePassword(Long userId, String oldPassword, String newPassword);

    Page<AdminUserInfoDTO> pageAdminUsers(int page, int size);

    AdminUserInfoDTO createAdminUser(String username, String password);

    void updateAdminUserStatus(Long id, Integer status);

    void deleteAdminUser(Long operatorId, Long targetId);
}
