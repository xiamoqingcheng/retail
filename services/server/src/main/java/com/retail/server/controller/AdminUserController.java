package com.retail.server.controller;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.retail.server.common.Result;
import com.retail.server.context.UserContext;
import com.retail.server.dto.AdminUserInfoDTO;
import com.retail.server.dto.ChangePasswordRequest;
import com.retail.server.dto.CreateAdminUserRequest;
import com.retail.server.service.UserService;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/admin/user")
public class AdminUserController {

    private final UserService userService;

    public AdminUserController(UserService userService) {
        this.userService = userService;
    }

    @GetMapping("/info")
    public Result<AdminUserInfoDTO> getUserInfo() {
        Long userId = UserContext.getCurrentUserId();
        AdminUserInfoDTO info = userService.getAdminUserInfo(userId);
        return Result.success(info);
    }

    @PutMapping("/password")
    public Result<Void> changePassword(@RequestBody ChangePasswordRequest request) {
        Long userId = UserContext.getCurrentUserId();
        userService.changePassword(userId, request.getOldPassword(), request.getNewPassword());
        return Result.success("密码修改成功", null);
    }

    @GetMapping("/page")
    public Result<Page<AdminUserInfoDTO>> pageAdminUsers(
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "10") int size) {
        Page<AdminUserInfoDTO> result = userService.pageAdminUsers(page, size);
        return Result.success(result);
    }

    @PostMapping
    public Result<AdminUserInfoDTO> createAdminUser(@RequestBody CreateAdminUserRequest request) {
        AdminUserInfoDTO user = userService.createAdminUser(request.getUsername(), request.getPassword());
        return Result.success("创建成功", user);
    }

    @PutMapping("/{id}/status")
    public Result<Void> updateStatus(@PathVariable Long id, @RequestBody Map<String, Integer> body) {
        Integer status = body.get("status");
        if (status == null || (status != 0 && status != 1)) {
            return Result.fail(400, "状态值无效");
        }
        userService.updateAdminUserStatus(id, status);
        return Result.success("状态更新成功", null);
    }

    @DeleteMapping("/{id}")
    public Result<Void> deleteAdminUser(@PathVariable Long id) {
        Long operatorId = UserContext.getCurrentUserId();
        userService.deleteAdminUser(operatorId, id);
        return Result.success("删除成功", null);
    }
}
