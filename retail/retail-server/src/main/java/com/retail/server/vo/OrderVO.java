package com.retail.server.vo;

import com.retail.server.entity.Goods;
import com.retail.server.entity.OrderItem;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class OrderVO {

    private Long id;
    private String status;
    private BigDecimal totalAmount;
    private LocalDateTime createTime;
    private List<OrderGoodsVO> goods;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class OrderGoodsVO {
        private Long goodsId;
        private String goodsName;
        private BigDecimal goodsPrice;
        private Integer quantity;
        private String goodsImage;

        public static OrderGoodsVO from(OrderItem item, Goods goods) {
            return OrderGoodsVO.builder()
                    .goodsId(item.getGoodsId())
                    .goodsName(item.getGoodsName() != null ? item.getGoodsName()
                            : (goods != null ? goods.getName() : "未知商品"))
                    .goodsPrice(item.getPrice())
                    .quantity(item.getQuantity())
                    .goodsImage(goods != null ? goods.getImageUrl() : null)
                    .build();
        }
    }
}
