package com.retail.server.recommendation.semantic;

import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import java.util.regex.Pattern;

@Component
public class SearchKeywordExtractor {

    private static final Pattern SPEC_PATTERN = Pattern.compile(
            "\\d+(\\.\\d+)?\\s*(ml|毫升|l|升|g|克|kg|千克|斤|个|只|支|包|袋|盒|瓶|罐|片|枚)",
            Pattern.CASE_INSENSITIVE);
    private static final Pattern EXTRA_SPACE_PATTERN = Pattern.compile("\\s+");

    public String fromGoodsName(String goodsName, String categoryName) {
        if (!StringUtils.hasText(goodsName)) {
            return StringUtils.hasText(categoryName) ? categoryName.trim() : "";
        }

        String value = goodsName.trim();
        value = SPEC_PATTERN.matcher(value).replaceAll("");
        value = value.replaceAll("[()（）【】\\[\\]]", "");
        value = value.replaceAll("单个装|家庭装|组合装|促销装", "");
        value = EXTRA_SPACE_PATTERN.matcher(value).replaceAll("");
        if (value.length() > 12) {
            value = value.substring(0, 12);
        }
        return StringUtils.hasText(value) ? value : goodsName.trim();
    }
}
