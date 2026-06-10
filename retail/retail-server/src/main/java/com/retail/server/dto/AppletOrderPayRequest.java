package com.retail.server.dto;

import lombok.Data;

@Data
public class AppletOrderPayRequest {

    private Long orderId;
    private String paymentMethod = "balance";
}