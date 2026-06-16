package com.retail.server.controller.applet;

import com.retail.server.common.Result;
import com.retail.server.context.UserContext;
import com.retail.server.dto.AppletUserBehaviorRequest;
import com.retail.server.exception.BusinessException;
import com.retail.server.recommendation.UserBehaviorEventType;
import com.retail.server.service.UserBehaviorEventService;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/applet/recommend")
public class AppletRecommendController {

    private final UserBehaviorEventService userBehaviorEventService;

    public AppletRecommendController(UserBehaviorEventService userBehaviorEventService) {
        this.userBehaviorEventService = userBehaviorEventService;
    }

    @PostMapping("/behavior")
    public Result<Void> recordBehavior(@RequestBody AppletUserBehaviorRequest request) {
        Long userId = UserContext.getCurrentUserId();
        if (userId == null || userId < 1) {
            throw new BusinessException(401, "未登录或Token无效");
        }
        if (request == null || !StringUtils.hasText(request.eventType())) {
            throw new BusinessException(400, "eventType 不能为空");
        }

        String eventType = request.eventType().trim().toUpperCase();
        if (UserBehaviorEventType.SEARCH.equals(eventType)) {
            userBehaviorEventService.recordSearch(userId, request.keyword());
        } else if (UserBehaviorEventType.VIEW.equals(eventType)) {
            userBehaviorEventService.recordView(userId, request.goodsId());
        } else {
            throw new BusinessException(400, "eventType 不支持客户端直接上报");
        }

        return Result.success("记录成功", null);
    }
}
