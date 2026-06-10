package com.retail.server.controller;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.retail.server.common.Result;
import com.retail.server.entity.Goods;
import com.retail.server.entity.GoodsCategory;
import com.retail.server.entity.InventoryLog;
import com.retail.server.exception.BusinessException;
import com.retail.server.mapper.GoodsCategoryMapper;
import com.retail.server.mapper.InventoryLogMapper;
import com.retail.server.service.GoodsService;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * 商品管理控制器。
 */
@RestController
@RequestMapping("/api/goods")
public class GoodsController {

    private final GoodsService goodsService;
    private final InventoryLogMapper inventoryLogMapper;
    private final GoodsCategoryMapper goodsCategoryMapper;

    public GoodsController(GoodsService goodsService, InventoryLogMapper inventoryLogMapper,
                           GoodsCategoryMapper goodsCategoryMapper) {
        this.goodsService = goodsService;
        this.inventoryLogMapper = inventoryLogMapper;
        this.goodsCategoryMapper = goodsCategoryMapper;
    }

    /**
     * 分页查询商品，支持按名称模糊搜索和分类筛选。
     */
    @GetMapping("/page")
    public Result<Page<Goods>> page(
            @RequestParam(defaultValue = "1") Long page,
            @RequestParam(defaultValue = "10") Long size,
            @RequestParam(required = false) String name,
            @RequestParam(required = false) Long categoryId,
            @RequestParam(required = false) String sortField,
            @RequestParam(required = false) String sortOrder) {
        if (page < 1 || size < 1) {
            throw new BusinessException(400, "分页参数必须大于 0");
        }

        Page<Goods> pageParam = new Page<>(page, size);
        LambdaQueryWrapper<Goods> queryWrapper = new LambdaQueryWrapper<>();
        if (StringUtils.hasText(name)) {
            queryWrapper.like(Goods::getName, name);
        }
        if (categoryId != null && categoryId > 0) {
            queryWrapper.eq(Goods::getCategoryId, categoryId);
        }

        // 动态排序
        boolean asc = "asc".equalsIgnoreCase(sortOrder);
        if ("price".equals(sortField)) {
            queryWrapper.orderBy(true, asc, Goods::getPrice);
        } else if ("stock".equals(sortField)) {
            queryWrapper.orderBy(true, asc, Goods::getStock);
        } else if ("shelfId".equals(sortField)) {
            queryWrapper.orderBy(true, asc, Goods::getShelfId);
        } else if ("categoryId".equals(sortField)) {
            queryWrapper.orderBy(true, asc, Goods::getCategoryId);
        } else {
            queryWrapper.orderByDesc(Goods::getUpdateTime);
        }

        goodsService.page(pageParam, queryWrapper);
        return Result.success(pageParam);
    }

    /**
     * 查询所有启用的商品分类。
     */
    @GetMapping("/categories")
    public Result<List<GoodsCategory>> categories() {
        List<GoodsCategory> list = goodsCategoryMapper.selectList(
                new LambdaQueryWrapper<GoodsCategory>()
                        .eq(GoodsCategory::getStatus, 1)
                        .orderByAsc(GoodsCategory::getSortOrder));
        return Result.success(list);
    }

    /**
     * 新增商品。
     */
    @PostMapping
    @Transactional(rollbackFor = Exception.class)
    public Result<Long> add(@RequestBody Goods goods) {
        if (goods == null) {
            throw new BusinessException(400, "商品信息不能为空");
        }

        goods.setId(null);
        boolean saved = goodsService.save(goods);
        if (!saved) {
            throw new BusinessException(500, "新增商品失败");
        }
        return Result.success("新增成功", goods.getId());
    }

    /**
     * 修改商品。
     */
    @PutMapping
    @Transactional(rollbackFor = Exception.class)
    public Result<Void> update(@RequestBody Goods goods) {
        if (goods == null || goods.getId() == null) {
            throw new BusinessException(400, "商品 ID 不能为空");
        }

        Goods existingGoods = goodsService.getById(goods.getId());
        if (existingGoods == null) {
            throw new BusinessException(404, "商品不存在或修改失败");
        }

        boolean updated = goodsService.updateById(goods);
        if (!updated) {
            throw new BusinessException(404, "商品不存在或修改失败");
        }

        Integer newStock = goods.getStock();
        Integer oldStock = existingGoods.getStock();
        int oldStockVal = oldStock != null ? oldStock : 0;
        if (newStock != null && newStock > oldStockVal) {
            InventoryLog inventoryLog = InventoryLog.builder()
                    .goodsId(goods.getId())
                    .changeAmount(newStock - oldStockVal)
                    .currentStock(newStock)
                    .type("RESTOCK")
                    .remark("管理端手动补货")
                    .build();

            if (inventoryLogMapper.insert(inventoryLog) != 1) {
                throw new BusinessException(500, "记录库存日志失败");
            }
        }

        return Result.success("修改成功", null);
    }

    /**
     * 删除商品（当前为物理删除）。
     */
    @DeleteMapping("/{id}")
    @Transactional(rollbackFor = Exception.class)
    public Result<Void> delete(@PathVariable Long id) {
        if (id == null || id < 1) {
            throw new BusinessException(400, "商品 ID 非法");
        }

        boolean removed = goodsService.removeById(id);
        if (!removed) {
            throw new BusinessException(404, "商品不存在或删除失败");
        }
        return Result.success("删除成功", null);
    }
}
