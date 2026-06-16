package com.retail.server.controller.applet;

import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.retail.server.common.Result;
import com.retail.server.context.UserContext;
import com.retail.server.dto.AppletRechargeRequest;
import com.retail.server.entity.WechatUser;
import com.retail.server.exception.BusinessException;
import com.retail.server.mapper.WechatUserMapper;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.math.BigDecimal;
import java.util.HashMap;
import java.util.Map;

/**
 * 小程序用户控制器（余额/个人信息）。
 */
@RestController
@RequestMapping("/api/applet/user")
public class AppletUserController {

    private final WechatUserMapper wechatUserMapper;

    public AppletUserController(WechatUserMapper wechatUserMapper) {
        this.wechatUserMapper = wechatUserMapper;
    }

    /**
     * 查询当前用户信息（含余额）。
     */
    @GetMapping("/info")
    public Result<Map<String, Object>> info() {
        Long userId = currentUserId();
        WechatUser user = wechatUserMapper.selectById(userId);
        if (user == null) {
            throw new BusinessException(404, "用户不存在");
        }

        Map<String, Object> data = new HashMap<>();
        data.put("userId", user.getId());
        data.put("nickname", user.getNickname());
        data.put("avatarUrl", user.getAvatarUrl());
        data.put("balance", user.getBalance() != null ? user.getBalance() : BigDecimal.ZERO);
        return Result.success(data);
    }

    /**
     * 余额充值。
     */
    @PostMapping("/recharge")
    public Result<Map<String, Object>> recharge(@RequestBody AppletRechargeRequest request) {
        Long userId = currentUserId();
        if (request == null || request.getAmount() == null) {
            throw new BusinessException(400, "充值金额不能为空");
        }

        BigDecimal amount = request.getAmount();
        if (amount.compareTo(BigDecimal.ZERO) <= 0) {
            throw new BusinessException(400, "充值金额必须大于0");
        }
        if (amount.compareTo(new BigDecimal("10000")) > 0) {
            throw new BusinessException(400, "单次充值不能超过10000");
        }

        // 原子更新余额，避免并发覆盖（SET balance = balance + ? WHERE id = ?）
        wechatUserMapper.update(null, new LambdaUpdateWrapper<WechatUser>()
                .eq(WechatUser::getId, userId)
                .setSql("balance = balance + {0}", amount));

        // 更新后重新查询，确保返回准确的余额
        BigDecimal newBalance = getCurrentBalance(userId);

        Map<String, Object> data = new HashMap<>();
        data.put("balance", newBalance);
        data.put("rechargeAmount", amount);
        return Result.success("充值成功", data);
    }

    private Long currentUserId() {
        Long userId = UserContext.getCurrentUserId();
        if (userId == null || userId < 1) {
            throw new BusinessException(401, "未登录或Token无效");
        }
        return userId;
    }

    private BigDecimal getCurrentBalance(Long userId) {
        WechatUser u = wechatUserMapper.selectById(userId);
        return u != null && u.getBalance() != null ? u.getBalance() : BigDecimal.ZERO;
    }
}
