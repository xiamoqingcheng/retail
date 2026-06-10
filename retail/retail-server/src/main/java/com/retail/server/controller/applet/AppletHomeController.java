package com.retail.server.controller.applet;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.retail.server.common.Result;
import com.retail.server.context.UserContext;
import com.retail.server.entity.Ad;
import com.retail.server.exception.BusinessException;
import com.retail.server.mapper.AdMapper;
import com.retail.server.mapper.GoodsMapper;
import com.retail.server.vo.AppletAdVO;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.util.CollectionUtils;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.time.Duration;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.ThreadLocalRandom;

/**
 * 小程序首页控制器。
 */
@RestController
@RequestMapping("/api/applet/home")
public class AppletHomeController {

    private static final int STATUS_ENABLED = 1;
    private static final String RECOMMEND_KEY_PREFIX = "recommend:user:";
    private static final int RECOMMEND_POOL_SIZE = 50;
    private static final Duration RECOMMEND_TTL = Duration.ofMinutes(10);

    private final AdMapper adMapper;
    private final GoodsMapper goodsMapper;
    private final StringRedisTemplate stringRedisTemplate;

    public AppletHomeController(
            AdMapper adMapper,
            GoodsMapper goodsMapper,
            StringRedisTemplate stringRedisTemplate) {
        this.adMapper = adMapper;
        this.goodsMapper = goodsMapper;
        this.stringRedisTemplate = stringRedisTemplate;
    }

    /**
     * 查询首页轮播图列表。
     */
    @GetMapping("/ads")
    public Result<List<AppletAdVO>> listAds() {
        List<Ad> ads = adMapper.selectList(new LambdaQueryWrapper<Ad>()
                .eq(Ad::getStatus, STATUS_ENABLED)
                .orderByAsc(Ad::getSortOrder)
                .orderByAsc(Ad::getId));

        List<AppletAdVO> data = ads.stream()
                .map(item -> new AppletAdVO(item.getImageUrl(), item.getLinkUrl()))
                .toList();

        return Result.success(data);
    }

    /**
     * 推荐流接口：按 offset + k 分页返回推荐商品 ID。
     */
    @GetMapping("/recommend")
    public Result<List<Long>> recommend(
            @RequestParam(defaultValue = "0") Integer offset,
            @RequestParam(defaultValue = "10") Integer k) {
        if (offset == null || offset < 0) {
            throw new BusinessException(400, "offset 必须大于等于 0");
        }
        if (k == null || k < 1) {
            throw new BusinessException(400, "k 必须大于 0");
        }

        Long userId = UserContext.getCurrentUserId();
        if (userId == null || userId < 1) {
            throw new BusinessException(401, "未登录或Token无效");
        }

        String redisKey = RECOMMEND_KEY_PREFIX + userId;
        if (offset == 0) {
            refreshRecommendFlow(redisKey);
        }

        Long total = stringRedisTemplate.opsForList().size(redisKey);
        if (total == null || total == 0 || offset >= total) {
            return new Result<>(204, "没有更多数据", List.of());
        }

        long end = (long) offset + k - 1;
        List<String> idStrings = stringRedisTemplate.opsForList().range(redisKey, offset, end);
        if (CollectionUtils.isEmpty(idStrings)) {
            return new Result<>(204, "没有更多数据", List.of());
        }

        List<Long> ids = idStrings.stream()
                .map(this::safeParseLong)
                .filter(Objects::nonNull)
                .toList();
        if (ids.isEmpty()) {
            return new Result<>(204, "没有更多数据", List.of());
        }

        return Result.success(ids);
    }

    private void refreshRecommendFlow(String redisKey) {
        long total = goodsMapper.countActiveGoods();
        if (total == 0) {
            stringRedisTemplate.delete(redisKey);
            return;
        }

        int limit = Math.min(RECOMMEND_POOL_SIZE, (int) total);
        int maxOffset = (int) total - limit;
        int offset = maxOffset > 0 ? ThreadLocalRandom.current().nextInt(maxOffset + 1) : 0;
        List<Long> goodsIds = goodsMapper.selectGoodsIdsByOffset(limit, offset);

        stringRedisTemplate.delete(redisKey);
        if (CollectionUtils.isEmpty(goodsIds)) {
            return;
        }

        List<String> cacheIds = goodsIds.stream()
                .map(String::valueOf)
                .toList();

        stringRedisTemplate.opsForList().rightPushAll(redisKey, cacheIds);
        stringRedisTemplate.expire(redisKey, RECOMMEND_TTL);
    }

    private Long safeParseLong(String value) {
        if (value == null) {
            return null;
        }
        try {
            return Long.parseLong(value);
        } catch (NumberFormatException ex) {
            return null;
        }
    }
}
