package com.retail.server.service;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.IService;
import com.retail.server.dto.AdminFeedbackDTO;
import com.retail.server.dto.AppletFeedbackRequest;
import com.retail.server.entity.Feedback;

/**
 * 用户反馈业务层。
 */
public interface FeedbackService extends IService<Feedback> {

    Long submitAppletFeedback(AppletFeedbackRequest request);

    Page<AdminFeedbackDTO> pageAdminFeedback(int page, int size, String status, String feedbackType);

    AdminFeedbackDTO updateFeedbackStatus(Long id, String status, String reply);
}
