package com.retail.server.dto;

import lombok.Data;

@Data
public class CreateAdminUserRequest {

    private String username;
    private String password;
}
