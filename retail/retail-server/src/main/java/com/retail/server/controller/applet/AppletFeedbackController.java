package com.retail.server.controller.applet;

import com.retail.server.common.Result;
import com.retail.server.dto.AppletFeedbackRequest;
import com.retail.server.service.FeedbackService;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 小程序反馈控制器。
 */
@RestController
@RequestMapping("/api/applet/feedback")
public class AppletFeedbackController {

    private final FeedbackService feedbackService;

    public AppletFeedbackController(FeedbackService feedbackService) {
        this.feedbackService = feedbackService;
    }

    @PostMapping
    public Result<Long> submit(@RequestBody AppletFeedbackRequest request) {
        return Result.success("反馈已提交", feedbackService.submitAppletFeedback(request));
    }
}
