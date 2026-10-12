package com.miuxuer.eventrush.utils;


import cn.hutool.core.util.RandomUtil;
import org.springframework.util.DigestUtils;

import java.nio.charset.StandardCharsets;

/**
 * 密码加密工具，采用"随机盐 + MD5"的方式存储和校验密码，避免明文落库。
 * <p>
 * 加密后的密文格式为 {@code 盐@MD5(密码+盐)}，盐随密码一起保存，
 * 这样即使两个用户密码相同，密文也不同。
 * </p>
 *
 * @author miuxuer
 * @since 2026-10-12
 */
public class PasswordEncoder {

    /**
     * 对明文密码加密：随机生成 20 位盐，再拼接加密。
     *
     * @param password 明文密码
     * @return 形如 {@code 盐@加密串} 的密文
     */
    public static String encode(String password) {
        // 生成盐
        String salt = RandomUtil.randomString(20);
        // 加密
        return encode(password,salt);
    }

    /**
     * 用指定的盐对密码做 MD5 加密，供加密和校验时复用。
     *
     * @param password 明文密码
     * @param salt     盐值
     * @return 形如 {@code 盐@加密串} 的密文
     */
    private static String encode(String password, String salt) {
        // 加密
        return salt + "@" + DigestUtils.md5DigestAsHex((password + salt).getBytes(StandardCharsets.UTF_8));
    }

    /**
     * 校验明文密码与已加密的密码是否匹配。
     * <p>
     * 从密文中拆出盐，用同样的方式加密明文，再与密文整体比较。
     * </p>
     *
     * @param encodedPassword 数据库中保存的密文
     * @param rawPassword     用户本次输入的明文密码
     * @return true：密码正确；false：密码错误（密文或明文为 null 时也返回 false）
     * @throws RuntimeException 当密文格式不合法（不含分隔符 @）时抛出
     */
    public static Boolean matches(String encodedPassword, String rawPassword) {
        if (encodedPassword == null || rawPassword == null) {
            return false;
        }
        if(!encodedPassword.contains("@")){
            throw new RuntimeException("密码格式不正确！");
        }
        String[] arr = encodedPassword.split("@");
        // 获取盐
        String salt = arr[0];
        // 比较
        return encodedPassword.equals(encode(rawPassword, salt));
    }
}
