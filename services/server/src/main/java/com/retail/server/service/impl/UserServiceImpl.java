package com.retail.server.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.retail.server.dto.AdminUserInfoDTO;
import com.retail.server.entity.User;
import com.retail.server.exception.BusinessException;
import com.retail.server.mapper.UserMapper;
import com.retail.server.security.JwtTokenProvider;
import com.retail.server.service.UserService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.time.Duration;
import java.util.Map;

/**
 * 用户业务实现。
 */
@Slf4j
@Service
public class UserServiceImpl extends ServiceImpl<UserMapper, User> implements UserService {

    private static final String TOKEN_KEY_PREFIX = "auth:token:";
    private final BCryptPasswordEncoder passwordEncoder = new BCryptPasswordEncoder();

    private final StringRedisTemplate stringRedisTemplate;
    private final JwtTokenProvider jwtTokenProvider;

    @Value("${jwt.expire-minutes:120}")
    private long jwtExpireMinutes;

    public UserServiceImpl(StringRedisTemplate stringRedisTemplate, JwtTokenProvider jwtTokenProvider) {
        this.stringRedisTemplate = stringRedisTemplate;
        this.jwtTokenProvider = jwtTokenProvider;
    }

    /**
     * 登录校验。
     */
    @Override
    public String login(String username, String password) {
        if (!StringUtils.hasText(username) || !StringUtils.hasText(password)) {
            throw new BusinessException(400, "用户名或密码不能为空");
        }

        User user = baseMapper.selectOne(new LambdaQueryWrapper<User>()
                .eq(User::getUsername, username)
                .last("LIMIT 1"));

        if (user == null) {
            throw new BusinessException(401, "账号不存在");
        }

        // 优先 BCrypt 比对，存储为明文时回退到直接比较并自动迁移至 BCrypt
        String stored = user.getPassword();
        if (stored != null && (stored.startsWith("$2a$") || stored.startsWith("$2b$"))) {
            if (!passwordEncoder.matches(password, stored)) {
                throw new BusinessException(401, "密码错误");
            }
        } else {
            if (!password.equals(stored)) {
                throw new BusinessException(401, "密码错误");
            }
            log.info("用户 {} 明文密码验证成功，自动迁移至 BCrypt", username);
            user.setPassword(passwordEncoder.encode(password));
            baseMapper.updateById(user);
        }

        String token = generateToken(user);
        String role = user.getRole() != null ? user.getRole() : "admin";
        String redisKey = TOKEN_KEY_PREFIX + role + ":" + user.getId();
        try {
            stringRedisTemplate.opsForValue().set(redisKey, token, Duration.ofMinutes(jwtExpireMinutes));
        } catch (Exception ex) {
            log.warn("Redis 连接失败，Token 将不写入缓存。建议启动 Redis 以启用完整鉴权链路。", ex);
        }
        return token;
    }

    private String generateToken(User user) {
        return jwtTokenProvider.generateToken(user.getId().toString(),
                Map.of("username", user.getUsername(), "role", user.getRole() != null ? user.getRole() : "admin"));
    }

    @Override
    public AdminUserInfoDTO getAdminUserInfo(Long userId) {
        User user = baseMapper.selectById(userId);
        if (user == null) {
            throw new BusinessException(404, "用户不存在");
        }
        return AdminUserInfoDTO.builder()
                .id(user.getId())
                .username(user.getUsername())
                .role(user.getRole())
                .status(user.getStatus())
                .createTime(user.getCreateTime())
                .build();
    }

    @Override
    public void changePassword(Long userId, String oldPassword, String newPassword) {
        if (!StringUtils.hasText(oldPassword) || !StringUtils.hasText(newPassword)) {
            throw new BusinessException(400, "密码不能为空");
        }
        if (newPassword.length() < 6) {
            throw new BusinessException(400, "新密码长度不能少于6位");
        }

        User user = baseMapper.selectById(userId);
        if (user == null) {
            throw new BusinessException(404, "用户不存在");
        }

        String stored = user.getPassword();
        boolean matched;
        if (stored != null && (stored.startsWith("$2a$") || stored.startsWith("$2b$"))) {
            matched = passwordEncoder.matches(oldPassword, stored);
        } else {
            matched = oldPassword.equals(stored);
        }
        if (!matched) {
            throw new BusinessException(400, "原密码错误");
        }

        user.setPassword(passwordEncoder.encode(newPassword));
        baseMapper.updateById(user);
        log.info("用户 {} 修改密码成功", user.getUsername());
    }

    @Override
    public Page<AdminUserInfoDTO> pageAdminUsers(int page, int size) {
        Page<User> userPage = baseMapper.selectPage(
                new Page<>(page, size),
                new LambdaQueryWrapper<User>()
                        .eq(User::getRole, "admin")
                        .orderByDesc(User::getCreateTime)
        );
        return (Page<AdminUserInfoDTO>) userPage.convert(user -> AdminUserInfoDTO.builder()
                .id(user.getId())
                .username(user.getUsername())
                .role(user.getRole())
                .status(user.getStatus())
                .createTime(user.getCreateTime())
                .build());
    }

    @Override
    public AdminUserInfoDTO createAdminUser(String username, String password) {
        if (!StringUtils.hasText(username) || !StringUtils.hasText(password)) {
            throw new BusinessException(400, "用户名和密码不能为空");
        }
        if (password.length() < 6) {
            throw new BusinessException(400, "密码长度不能少于6位");
        }

        User exist = baseMapper.selectOne(new LambdaQueryWrapper<User>()
                .eq(User::getUsername, username)
                .last("LIMIT 1"));
        if (exist != null) {
            throw new BusinessException(400, "用户名已存在");
        }

        User user = User.builder()
                .username(username)
                .password(passwordEncoder.encode(password))
                .role("admin")
                .status(1)
                .build();
        baseMapper.insert(user);

        return AdminUserInfoDTO.builder()
                .id(user.getId())
                .username(user.getUsername())
                .role(user.getRole())
                .status(user.getStatus())
                .createTime(user.getCreateTime())
                .build();
    }

    @Override
    public void updateAdminUserStatus(Long id, Integer status) {
        User user = baseMapper.selectById(id);
        if (user == null) {
            throw new BusinessException(404, "用户不存在");
        }
        user.setStatus(status);
        baseMapper.updateById(user);
    }

    @Override
    public void deleteAdminUser(Long operatorId, Long targetId) {
        if (operatorId != null && operatorId.equals(targetId)) {
            throw new BusinessException(400, "不能删除自己的账户");
        }
        User user = baseMapper.selectById(targetId);
        if (user == null) {
            throw new BusinessException(404, "用户不存在");
        }
        if (!"admin".equals(user.getRole())) {
            throw new BusinessException(400, "只能删除管理员账户");
        }
        baseMapper.deleteById(targetId);
        log.info("管理员 {} 删除了账户 {} ({})", operatorId, targetId, user.getUsername());
    }
}
