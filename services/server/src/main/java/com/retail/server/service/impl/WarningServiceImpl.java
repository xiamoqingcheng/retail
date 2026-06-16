package com.retail.server.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.retail.server.dto.AdminWarningDTO;
import com.retail.server.entity.Goods;
import com.retail.server.entity.Warning;
import com.retail.server.mapper.GoodsMapper;
import com.retail.server.mapper.WarningMapper;
import com.retail.server.service.WarningService;
import org.springframework.stereotype.Service;
import org.springframework.util.CollectionUtils;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.SequencedCollection;
import java.util.Set;

/**
 * 库存告警业务实现。
 */
@Service
public class WarningServiceImpl extends ServiceImpl<WarningMapper, Warning> implements WarningService {

	private final GoodsMapper goodsMapper;

	public WarningServiceImpl(GoodsMapper goodsMapper) {
		this.goodsMapper = goodsMapper;
	}

	@Override
	public List<AdminWarningDTO> listPendingWarnings() {
		LambdaQueryWrapper<Warning> queryWrapper = new LambdaQueryWrapper<>();
		queryWrapper.eq(Warning::getStatus, 0)
				.orderByDesc(Warning::getCreateTime);

		List<Warning> warnings = this.list(queryWrapper);
		if (CollectionUtils.isEmpty(warnings)) {
			return List.of();
		}

		Set<Long> seenGoodsIds = new HashSet<>();
		SequencedCollection<Long> orderedGoodsIds = new ArrayList<>();
		for (Warning warning : warnings) {
			if (warning == null || warning.getGoodsId() == null) {
				continue;
			}
			if (seenGoodsIds.add(warning.getGoodsId())) {
				orderedGoodsIds.add(warning.getGoodsId());
			}
		}

		Map<Long, Integer> stockMap = new HashMap<>();
		if (!orderedGoodsIds.isEmpty()) {
			List<Goods> goodsList = goodsMapper.selectBatchIds(new ArrayList<>(orderedGoodsIds));
			for (Goods goods : goodsList) {
				if (goods != null && goods.getId() != null) {
					stockMap.put(goods.getId(), goods.getStock());
				}
			}
		}

		SequencedCollection<AdminWarningDTO> result = new ArrayList<>();
		for (Warning warning : warnings) {
			if (warning == null) {
				continue;
			}
			result.add(new AdminWarningDTO(
					warning.getId(),
					warning.getGoodsId(),
					stockMap.getOrDefault(warning.getGoodsId(), 0),
					warning.getWarningMsg(),
					warning.getStatus(),
					warning.getCreateTime()
			));
		}

		return List.copyOf(result);
	}
}
