package com.demo.toefl.dto;
import lombok.Data;

/**
 * DTO：接收前端登录、注册提交的json参数
 */
@Data
public class LoginDTO {
    private String username;
    private String password;
}
