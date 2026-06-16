package com.retail.server.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.retail.server.entity.Report;
import org.apache.ibatis.annotations.Mapper;

/**
 * 销售报表记录数据访问层。
 */
@Mapper
public interface ReportMapper extends BaseMapper<Report> {
}
