package com.retail.server.controller.applet;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.retail.server.common.Result;
import com.retail.server.context.UserContext;
import com.retail.server.entity.Ad;
import com.retail.server.exception.BusinessException;
import com.retail.server.mapper.AdMapper;
import com.retail.server.recommendation.service.PersonalizedRecommendService;
import com.retail.server.vo.AppletAdVO;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * 小程序首页控制器。
 */
@RestController
@RequestMapping("/api/applet/home")
public class AppletHomeController {

    private static final int STATUS_ENABLED = 1;
    private static final int MAX_RECOMMEND_COUNT = 80;

    private final AdMapper adMapper;
    private final PersonalizedRecommendService personalizedRecommendService;

    public AppletHomeController(
            AdMapper adMapper,
            PersonalizedRecommendService personalizedRecommendService) {
        this.adMapper = adMapper;
        this.personalizedRecommendService = personalizedRecommendService;
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
        if (userId != null && userId < 1) {
            throw new BusinessException(401, "未登录或Token无效");
        }

        int count = Math.min(MAX_RECOMMEND_COUNT, offset + k);
        List<Long> flow = personalizedRecommendService.recommendGoodsIds(userId, count);
        if (flow.isEmpty() || offset >= flow.size()) {
            return new Result<>(204, "没有更多数据", List.of());
        }

        int end = Math.min(flow.size(), offset + k);
        List<Long> ids = flow.subList(offset, end);
        if (ids.isEmpty()) {
            return new Result<>(204, "没有更多数据", List.of());
        }

        return Result.success(ids);
    }
}
