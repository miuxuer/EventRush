package com.miuxuer.eventrush.utils;

/**
 * 系统级常量。
 *
 * @author miuxuer
 * @since 2026-10-12
 */
public class SystemConstants {

    /** 图片上传的本地目录，由 Nginx 挂载后对外提供访问，部署时按实际情况修改 */
    public static final String IMAGE_UPLOAD_DIR = "D:\\dev\\nginx-1.18.0\\html\\eventrush\\imgs";

    /** 自动注册用户时昵称的前缀，后面拼一段随机串 */
    public static final String USER_NICK_NAME_PREFIX = "user_";

    /** 默认分页大小 */
    public static final int DEFAULT_PAGE_SIZE = 5;

    /** 单页最多返回多少条，防止前端传一个很大的页码把库拖垮 */
    public static final int MAX_PAGE_SIZE = 10;
}
