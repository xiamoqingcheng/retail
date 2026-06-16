package com.retail.server.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.retail.server.entity.Ad;
import org.apache.ibatis.annotations.Mapper;

/**
 * 轮播广告数据访问层。
 */
@Mapper
public interface AdMapper extends BaseMapper<Ad> {
}
