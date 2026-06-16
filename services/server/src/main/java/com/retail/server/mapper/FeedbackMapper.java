package com.retail.server.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.retail.server.entity.Feedback;
import org.apache.ibatis.annotations.Mapper;

/**
 * 用户反馈数据访问层。
 */
@Mapper
public interface FeedbackMapper extends BaseMapper<Feedback> {
}
