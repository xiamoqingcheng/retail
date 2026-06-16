package com.retail.server.controller;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.retail.server.common.Result;
import com.retail.server.entity.GoodsCategory;
import com.retail.server.mapper.GoodsCategoryMapper;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/admin/category")
public class AdminCategoryController {

    private final GoodsCategoryMapper goodsCategoryMapper;

    public AdminCategoryController(GoodsCategoryMapper goodsCategoryMapper) {
        this.goodsCategoryMapper = goodsCategoryMapper;
    }

    @GetMapping("/list")
    public Result<java.util.List<GoodsCategory>> list() {
        return Result.success(goodsCategoryMapper.selectList(
            new LambdaQueryWrapper<GoodsCategory>().orderByAsc(GoodsCategory::getSortOrder)));
    }

    @PostMapping
    public Result<GoodsCategory> create(@RequestBody Map<String, Object> body) {
        String name = (String) body.get("name");
        Object sortObj = body.get("sortOrder");
        Integer sortOrder = sortObj instanceof Number n ? n.intValue() : 0;
        GoodsCategory cat = new GoodsCategory();
        cat.setName(name);
        cat.setSortOrder(sortOrder);
        cat.setStatus(1);
        goodsCategoryMapper.insert(cat);
        return Result.success("创建成功", cat);
    }

    @PutMapping("/{id}")
    public Result<Void> update(@PathVariable Long id, @RequestBody Map<String, Object> body) {
        GoodsCategory cat = goodsCategoryMapper.selectById(id);
        if (cat == null) return Result.fail(404, "分类不存在");
        if (body.containsKey("name")) cat.setName((String) body.get("name"));
        if (body.containsKey("sortOrder") && body.get("sortOrder") instanceof Number n)
            cat.setSortOrder(n.intValue());
        if (body.containsKey("status") && body.get("status") instanceof Number n)
            cat.setStatus(n.intValue());
        goodsCategoryMapper.updateById(cat);
        return Result.success("更新成功", null);
    }

    @DeleteMapping("/{id}")
    public Result<Void> delete(@PathVariable Long id) {
        goodsCategoryMapper.deleteById(id);
        return Result.success("删除成功", null);
    }
}
