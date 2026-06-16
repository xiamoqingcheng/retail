package com.retail.server.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.retail.server.entity.Camera;
import org.apache.ibatis.annotations.Mapper;

/**
 * 摄像头数据访问层。
 */
@Mapper
public interface CameraMapper extends BaseMapper<Camera> {
}