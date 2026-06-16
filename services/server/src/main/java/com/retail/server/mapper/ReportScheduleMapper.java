package com.retail.server.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.retail.server.entity.ReportSchedule;
import org.apache.ibatis.annotations.Mapper;

/**
 * 定时报表调度配置数据访问层。
 */
@Mapper
public interface ReportScheduleMapper extends BaseMapper<ReportSchedule> {
}
