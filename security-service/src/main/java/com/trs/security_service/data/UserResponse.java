package com.trs.security_service.data;

import lombok.Data;

@Data
public class UserResponse {

    private Long id;
    private String email;
    private String password;

}
