package com.retail.server.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.retail.server.entity.WechatUser;
import org.apache.ibatis.annotations.Mapper;

/**
 * 小程序用户数据访问层。
 */
@Mapper
public interface WechatUserMapper extends BaseMapper<WechatUser> {
}
