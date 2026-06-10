package com.retail.server.controller.applet;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.retail.server.common.Result;
import com.retail.server.entity.Camera;
import com.retail.server.entity.Goods;
import com.retail.server.exception.BusinessException;
import com.retail.server.mapper.CameraMapper;
import com.retail.server.mapper.GoodsMapper;
import com.retail.server.vo.AppletGoodsVO;
import org.springframework.util.CollectionUtils;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/applet")
public class AppletGoodsController {

    private final GoodsMapper goodsMapper;
    private final CameraMapper cameraMapper;

    public AppletGoodsController(GoodsMapper goodsMapper, CameraMapper cameraMapper) {
        this.goodsMapper = goodsMapper;
        this.cameraMapper = cameraMapper;
    }

    @PostMapping("/goods/listByIds")
    public Result<List<AppletGoodsVO>> listByIds(@RequestBody List<Long> ids) {
        if (CollectionUtils.isEmpty(ids)) {
            throw new BusinessException(400, "ids 不能为空");
        }

        List<Long> normalizedIds = ids.stream()
                .filter(Objects::nonNull)
                .filter(id -> id > 0)
                .distinct()
                .toList();

        if (normalizedIds.isEmpty()) {
            throw new BusinessException(400, "ids 不能为空");
        }

        List<Goods> goodsList = goodsMapper.selectBatchIds(normalizedIds);
        if (CollectionUtils.isEmpty(goodsList)) {
            return Result.success(List.of());
        }

        Map<Long, Goods> goodsMap = goodsList.stream()
                .collect(Collectors.toMap(Goods::getId, item -> item, (left, right) -> left, LinkedHashMap::new));

        Set<String> boundShelfSet = queryBoundShelfSet(goodsList);

        List<AppletGoodsVO> data = normalizedIds.stream()
                .map(goodsMap::get)
                .filter(Objects::nonNull)
                .map(goods -> new AppletGoodsVO(
                        goods.getId(),
                        goods.getName(),
                        goods.getPrice(),
                        goods.getStock(),
                        goods.getImageUrl(),
                        goods.getShelfId(),
                        resolveSortedShelfIds(goods.getShelfId(), boundShelfSet)
                ))
                .toList();

        return Result.success(data);
    }

    private Set<String> queryBoundShelfSet(List<Goods> goodsList) {
        Set<String> shelfCandidates = goodsList.stream()
                .map(Goods::getShelfId)
                .filter(StringUtils::hasText)
                .flatMap(value -> Arrays.stream(value.split(",")))
                .map(String::trim)
                .filter(StringUtils::hasText)
                .collect(Collectors.toSet());

        if (CollectionUtils.isEmpty(shelfCandidates)) {
            return Set.of();
        }

        LambdaQueryWrapper<Camera> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.in(Camera::getShelfId, shelfCandidates)
                .select(Camera::getShelfId);

        List<Camera> cameras = cameraMapper.selectList(queryWrapper);
        if (CollectionUtils.isEmpty(cameras)) {
            return Set.of();
        }

        return cameras.stream()
                .map(Camera::getShelfId)
                .filter(StringUtils::hasText)
                .collect(Collectors.toSet());
    }

    private List<String> resolveSortedShelfIds(String shelfIdText, Set<String> boundShelfSet) {
        if (!StringUtils.hasText(shelfIdText) || CollectionUtils.isEmpty(boundShelfSet)) {
            return List.of();
        }

        return Arrays.stream(shelfIdText.split(","))
                .map(String::trim)
                .filter(StringUtils::hasText)
                .filter(boundShelfSet::contains)
                .distinct()
                .sorted()
                .toList();
    }
}
