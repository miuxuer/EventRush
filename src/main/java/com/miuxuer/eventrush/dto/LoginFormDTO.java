package com.miuxuer.eventrush.dto;

import lombok.Data;

/**
 * 登录请求参数，接收前端提交的登录表单。
 * <p>
 * 支持两种登录方式：验证码登录只填 phone + code，密码登录只填 phone + password，
 * 由 Service 层根据某个字段是否为空来决定走哪条分支。
 * </p>
 *
 * @author miuxuer
 * @since 2026-10-12
 */
@Data
public class LoginFormDTO {

    /**
     * 手机号
     */
    private String phone;

    /**
     * 短信验证码，验证码登录时使用
     */
    private String code;

    /**
     * 密码，密码登录时使用
     */
    private String password;
}
