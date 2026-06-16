package com.retail.server.controller.applet;

import com.retail.server.common.Result;
import com.retail.server.exception.BusinessException;
import com.retail.server.recommendation.service.HotSearchRecommendationService;
import com.retail.server.recommendation.service.SemanticGoodsSearchService;
import com.retail.server.vo.AppletSearchGoodsVO;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/applet/search")
public class AppletSearchController {

    private final SemanticGoodsSearchService semanticGoodsSearchService;
    private final HotSearchRecommendationService hotSearchRecommendationService;

    public AppletSearchController(
            SemanticGoodsSearchService semanticGoodsSearchService,
            HotSearchRecommendationService hotSearchRecommendationService) {
        this.semanticGoodsSearchService = semanticGoodsSearchService;
        this.hotSearchRecommendationService = hotSearchRecommendationService;
    }

    @GetMapping("/text")
    public Result<List<AppletSearchGoodsVO>> searchText(
            @RequestParam(required = false) String query,
            @RequestParam(required = false) String name,
            @RequestParam(required = false) Long categoryId,
            @RequestParam(defaultValue = "20") Integer size) {
        String keyword = StringUtils.hasText(query) ? query : name;
        if (!StringUtils.hasText(keyword)) {
            throw new BusinessException(400, "query 不能为空");
        }
        return Result.success(semanticGoodsSearchService.search(keyword.trim(), categoryId, size));
    }

    @GetMapping("/hot")
    public Result<List<String>> hotKeywords(
            @RequestParam(defaultValue = "8") Integer k,
            @RequestParam(defaultValue = "30") Integer m) {
        return Result.success(hotSearchRecommendationService.hotKeywords(k, m));
    }
}
