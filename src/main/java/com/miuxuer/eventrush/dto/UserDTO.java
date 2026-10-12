package com.miuxuer.eventrush.dto;

import lombok.Data;

/**
 * 精简的用户信息，用于登录后缓存在 Redis 并放入 ThreadLocal 作为当前登录态。
 * <p>
 * 只保留 id、昵称、头像这几个最常用的字段，避免把完整用户对象（含密码）带进会话。
 * </p>
 *
 * @author miuxuer
 * @since 2026-10-12
 */
@Data
public class UserDTO {

    /**
     * 用户 id
     */
    private Long id;

    /**
     * 昵称
     */
    private String nickName;

    /**
     * 用户头像
     */
    private String icon;
}
