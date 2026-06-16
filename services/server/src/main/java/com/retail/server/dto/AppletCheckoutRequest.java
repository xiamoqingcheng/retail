package com.retail.server.dto;

import lombok.Data;

import java.util.List;

@Data
public class AppletCheckoutRequest {

    private List<CartItem> items;

    @Data
    public static class CartItem {
        private Long id;
        private Integer count;
    }
}