package com.retail.server.service.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.retail.server.entity.Goods;
import com.retail.server.mapper.GoodsMapper;
import com.retail.server.service.GoodsService;
import org.springframework.stereotype.Service;

/**
 * 商品业务实现层。
 */
@Service
public class GoodsServiceImpl extends ServiceImpl<GoodsMapper, Goods> implements GoodsService {
}
