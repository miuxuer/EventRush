package com.miuxuer.eventrush.utils;

import cn.hutool.core.util.StrUtil;

/**
 * 基于 {@link RegexPatterns} 的格式校验工具，方法统一返回"是否不合法"，方便直接用作参数校验条件。
 *
 * @author miuxuer
 * @since 2026-10-12
 */
public class RegexUtils {

    /**
     * 判断手机号格式是否不合法。
     *
     * @param phone 要校验的手机号
     * @return true：格式不合法（需要拦截）；false：格式合法
     */
    public static boolean isPhoneInvalid(String phone){
        return mismatch(phone, RegexPatterns.PHONE_REGEX);
    }

    /**
     * 判断邮箱格式是否不合法。
     *
     * @param email 要校验的邮箱
     * @return true：格式不合法（需要拦截）；false：格式合法
     */
    public static boolean isEmailInvalid(String email){
        return mismatch(email, RegexPatterns.EMAIL_REGEX);
    }

    /**
     * 判断验证码格式是否不合法。
     *
     * @param code 要校验的验证码
     * @return true：格式不合法（需要拦截）；false：格式合法
     */
    public static boolean isCodeInvalid(String code){
        return mismatch(code, RegexPatterns.VERIFY_CODE_REGEX);
    }

    // 校验是否不符合正则格式
    private static boolean mismatch(String str, String regex){
        if (StrUtil.isBlank(str)) {
            return true;
        }
        return !str.matches(regex);
    }
}
