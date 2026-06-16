package com.retail.server.controller.applet;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.retail.server.common.Result;
import com.retail.server.dto.AppletAuthLoginRequest;
import com.retail.server.entity.WechatUser;
import com.retail.server.exception.BusinessException;
import com.retail.server.mapper.WechatUserMapper;
import com.retail.server.security.JwtTokenProvider;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;

/**
 * 小程序鉴权控制器。
 */
@Slf4j
@RestController
@RequestMapping("/api/applet/auth")
public class AppletAuthController {

    private static final String TEST_CODE = "test_code";
    private static final String TEST_OPENID = "mock_openid_123";
    private static final String TOKEN_KEY_PREFIX = "auth:token:";
    private static final String APPLET_ROLE = "applet_customer";

    @Value("${applet.auth.test-mode:false}")
    private boolean testMode;

    @Value("${jwt.expire-minutes:120}")
    private long jwtExpireMinutes;

    private final WechatUserMapper wechatUserMapper;
    private final JwtTokenProvider jwtTokenProvider;
    private final StringRedisTemplate stringRedisTemplate;

    public AppletAuthController(WechatUserMapper wechatUserMapper, JwtTokenProvider jwtTokenProvider, StringRedisTemplate stringRedisTemplate) {
        this.wechatUserMapper = wechatUserMapper;
        this.jwtTokenProvider = jwtTokenProvider;
        this.stringRedisTemplate = stringRedisTemplate;
    }

    /**
     * 小程序登录接口。
     */
    @PostMapping("/login")
    @Transactional(rollbackFor = Exception.class)
    public Result<Map<String, Object>> login(@RequestBody AppletAuthLoginRequest request) {
        if (request == null || !StringUtils.hasText(request.getCode())) {
            throw new BusinessException(400, "code 不能为空");
        }

        String openid = resolveOpenidByCode(request.getCode().trim());
        WechatUser wechatUser = findOrCreateWechatUser(openid);
        String token = generateToken(wechatUser);

        String redisKey = TOKEN_KEY_PREFIX + APPLET_ROLE + ":" + wechatUser.getId();
        try {
            stringRedisTemplate.opsForValue().set(redisKey, token, Duration.ofMinutes(jwtExpireMinutes));
        } catch (Exception ex) {
            log.warn("Redis 连接失败，Token 将不写入缓存。", ex);
        }

        Map<String, Object> data = new HashMap<>();
        data.put("token", token);
        data.put("tokenType", "Bearer");
        data.put("userId", wechatUser.getId());
        data.put("openid", wechatUser.getOpenid());
        data.put("nickname", wechatUser.getNickname());
        data.put("avatarUrl", wechatUser.getAvatarUrl());
        data.put("balance", wechatUser.getBalance() != null ? wechatUser.getBalance() : java.math.BigDecimal.ZERO);

        return Result.success("登录成功", data);
    }

    private String resolveOpenidByCode(String code) {
        if (TEST_CODE.equals(code)) {
            if (!testMode) {
                log.warn("测试登录请求被拒绝，当前非测试环境。如需启用请设置 applet.auth.test-mode=true");
                throw new BusinessException(400, "测试登录仅在开发环境可用");
            }
            return TEST_OPENID;
        }

        // 演示环境仅保留 test_code，生产环境这里应调用微信 code2Session 接口。
        throw new BusinessException(400, "演示环境仅支持 code=test_code");
    }

    private WechatUser findOrCreateWechatUser(String openid) {
        WechatUser exists = wechatUserMapper.selectOne(new LambdaQueryWrapper<WechatUser>()
                .eq(WechatUser::getOpenid, openid)
                .last("LIMIT 1"));
        if (exists != null) {
            return exists;
        }

        WechatUser wechatUser = WechatUser.builder()
                .openid(openid)
                .nickname("微信用户")
                .avatarUrl("")
                .createTime(LocalDateTime.now())
                .build();

        int inserted = wechatUserMapper.insert(wechatUser);
        if (inserted != 1 || wechatUser.getId() == null) {
            throw new BusinessException(500, "小程序用户注册失败");
        }
        return wechatUser;
    }

    private String generateToken(WechatUser wechatUser) {
        return jwtTokenProvider.generateToken(wechatUser.getId().toString(),
                Map.of("openid", wechatUser.getOpenid(), "role", "applet_customer"));
    }
}
