package com.retail.server.dto;

import lombok.Data;

import java.math.BigDecimal;

@Data
public class AppletRechargeRequest {

    private BigDecimal amount;
}