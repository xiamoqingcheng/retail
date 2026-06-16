package com.retail.server.dto;

import com.fasterxml.jackson.annotation.JsonAlias;

public record AppletUserBehaviorRequest(
        @JsonAlias({"event_type", "eventType"})
        String eventType,
        @JsonAlias({"goods_id", "goodsId"})
        Long goodsId,
        String keyword,
        Integer quantity
) {
}
