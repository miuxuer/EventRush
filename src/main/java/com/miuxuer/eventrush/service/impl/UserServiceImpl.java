package com.miuxuer.eventrush.service.impl;

import cn.hutool.core.bean.BeanUtil;
import cn.hutool.core.bean.copier.CopyOptions;
import cn.hutool.core.lang.UUID;
import cn.hutool.core.util.RandomUtil;
import cn.hutool.core.util.StrUtil;
import com.baomidou.mybatisplus.spring.service.impl.ServiceImpl;
import com.miuxuer.eventrush.dto.LoginFormDTO;
import com.miuxuer.eventrush.dto.Result;
import com.miuxuer.eventrush.dto.UserDTO;
import com.miuxuer.eventrush.entity.User;
import com.miuxuer.eventrush.mapper.UserMapper;
import com.miuxuer.eventrush.service.IUserService;
import com.miuxuer.eventrush.utils.RegexUtils;
import com.miuxuer.eventrush.utils.UserHolder;
import jakarta.servlet.http.HttpSession;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.connection.BitFieldSubCommands;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;

import static com.miuxuer.eventrush.utils.RedisConstants.LOGIN_CODE_KEY;
import static com.miuxuer.eventrush.utils.RedisConstants.LOGIN_CODE_TTL;
import static com.miuxuer.eventrush.utils.RedisConstants.LOGIN_USER_KEY;
import static com.miuxuer.eventrush.utils.RedisConstants.LOGIN_USER_TTL;
import static com.miuxuer.eventrush.utils.RedisConstants.USER_SIGN_KEY;
import static com.miuxuer.eventrush.utils.SystemConstants.USER_NICK_NAME_PREFIX;

/**
 * 用户服务实现。
 *
 * <p>登录态没有用 Session，而是「Redis + token」：登录时生成一个随机 token，
 * 把用户信息以 Hash 的形式存进 Redis 并设置有效期；后续请求带上这个 token，
 * 由 {@link com.miuxuer.eventrush.utils.RefreshTokenInterceptor} 查 Redis 还原出用户。</p>
 *
 * <p>这样做的好处是服务端无状态，多台机器部署也能共享登录态。</p>
 *
 * @author miuxuer
 * @since 2026-10-12
 */
@Service
@Slf4j
public class UserServiceImpl extends ServiceImpl<UserMapper, User> implements IUserService {

    @Autowired
    private StringRedisTemplate stringRedisTemplate;

    /**
     * 发送短信验证码。
     *
     * <p>校验手机号格式后生成 6 位数字验证码存进 Redis，key 是 {@code login:code:手机号}。
     * 教程项目里没有接真实短信通道，验证码直接打日志，方便本地调试。</p>
     *
     * @param phone   手机号
     * @param session 当前会话，登录态已改用 Redis，这里保留参数只是为了兼容调用方
     * @return 成功返回 ok；手机号格式不对返回失败提示
     */
    @Override
    public Result sendCode(String phone, HttpSession session) {
        // 校验手机号
        if (RegexUtils.isPhoneInvalid(phone)) {
            return Result.fail("手机号格式错误");
        }
        // 生成验证码并保存到 redis
        String code = RandomUtil.randomNumbers(6);
        stringRedisTemplate.opsForValue().set(LOGIN_CODE_KEY + phone, code, LOGIN_CODE_TTL, TimeUnit.MINUTES);

        // 模拟发送短信，实际环境下这里调用短信服务商接口
        log.debug("发送验证码成功，验证码为：{}", code);

        return Result.ok();
    }

    /**
     * 手机号 + 验证码登录，首次登录自动注册。
     *
     * @param loginForm 登录表单
     * @param session   当前会话，同上
     * @return 成功时 data 为登录令牌 token
     */
    @Override
    public Result login(LoginFormDTO loginForm, HttpSession session) {
        // 1. 校验手机号
        String phone = loginForm.getPhone();
        if (RegexUtils.isPhoneInvalid(phone)) {
            return Result.fail("手机号格式错误");
        }

        // 2. 校验验证码，从 redis 里取出发给这个手机号的那条
        String cacheCode = stringRedisTemplate.opsForValue().get(LOGIN_CODE_KEY + phone);
        String code = loginForm.getCode();
        if (cacheCode == null || !cacheCode.equals(code)) {
            return Result.fail("验证码错误");
        }

        // 3. 按手机号查用户，没有就自动注册一个
        User user = query().eq("phone", phone).one();
        if (user == null) {
            user = createUserWithPhone(phone);
        }

        // 4. 生成令牌。用 UUID 而不是自增，避免被猜到别人的 token
        String token = UUID.randomUUID().toString(true);

        // 5. 把用户信息转成 Hash 存进 Redis。
        //    Redis 的 Hash 只接受字符串，所以这里把每个字段的值都 toString 一遍；
        //    null 值要先过滤掉，否则 setFieldValueEditor 会先把它变成字符串 "null"。
        UserDTO userDTO = BeanUtil.copyProperties(user, UserDTO.class);
        Map<String, Object> userMap = BeanUtil.beanToMap(userDTO, new HashMap<>(),
                CopyOptions.create()
                        .setIgnoreNullValue(true)
                        .setFieldValueEditor((fieldName, fieldValue) -> fieldValue == null ? null : fieldValue.toString()));

        String tokenKey = LOGIN_USER_KEY + token;
        stringRedisTemplate.opsForHash().putAll(tokenKey, userMap);
        // 设置登录态有效期，后续每次请求由拦截器续期
        stringRedisTemplate.expire(tokenKey, LOGIN_USER_TTL, TimeUnit.MINUTES);

        return Result.ok(token);
    }

    /**
     * 退出登录。
     *
     * <p>登录态存在 Redis 里，所以登出只需要把 token 对应的那个 key 删掉，
     * 前端再拿这个 token 来请求就会被登录拦截器挡回去。</p>
     *
     * <p>token 为空说明本来就没登录，直接返回成功即可，不用报错。</p>
     *
     * @param token 请求头里带的登录令牌
     * @return 固定返回 ok
     */
    @Override
    public Result logout(String token) {
        if (StrUtil.isBlank(token)) {
            return Result.ok();
        }
        stringRedisTemplate.delete(LOGIN_USER_KEY + token);
        return Result.ok();
    }

    /**
     * 用户签到。
     *
     * <p>用 BitMap 存：key 是 {@code sign:用户id:yyyyMM}，这个月的第几天就把第几个 bit 置为 1。
     * 一个月最多 31 天，也就是 4 个字节，非常省空间。</p>
     *
     * @return 无
     */
    @Override
    public Result sign() {
        // 获取用户
        Long userId = UserHolder.getUser().getId();
        // 获取日期
        LocalDateTime now = LocalDateTime.now();
        // 拼接key
        String keySuffix = now.format(DateTimeFormatter.ofPattern(":yyyyMM"));
        String key = USER_SIGN_KEY + userId + keySuffix;
        // 今天是本月的第几天，bit 下标从 0 开始，所以要减一
        int dayOfMonth = now.getDayOfMonth();
        // 写入redis
        stringRedisTemplate.opsForValue().setBit(key, dayOfMonth - 1, true);

        return Result.ok();
    }

    /**
     * 统计当前用户本月截至今天连续签到了多少天。
     *
     * <p>先取出本月从 1 号到今天为止的所有 bit 组成的整数，然后从最低位（也就是今天）
     * 开始往前逐位判断，遇到 0 就说明断签了。</p>
     *
     * @return 连续签到天数，没签过返回 0
     */
    @Override
    public Result signCount() {
        Long userId = UserHolder.getUser().getId();
        LocalDateTime now = LocalDateTime.now();
        // key 的格式和签到接口保持一致
        String keySuffix = now.format(DateTimeFormatter.ofPattern(":yyyyMM"));
        String key = USER_SIGN_KEY + userId + keySuffix;
        int dayOfMonth = now.getDayOfMonth();

        // 取出本月 1 号到今天的 bit，返回的是一个十进制数
        List<Long> result = stringRedisTemplate.opsForValue().bitField(
                key, BitFieldSubCommands.create()
                        .get(BitFieldSubCommands.BitFieldType.unsigned(dayOfMonth)).valueAt(0)
        );
        if (result == null || result.isEmpty()) {
            // 一次都没签过
            return Result.ok(0);
        }
        Long num = result.get(0);
        if (num == null || num == 0) {
            return Result.ok(0);
        }

        int count = 0;
        // 从最低位开始逐位往前数，连续为 1 的个数就是连续签到天数
        while (true) {
            if ((num & 1) == 1) {
                count++;
            } else {
                // 碰到 0 说明断了
                break;
            }
            // 无符号右移一位，继续看下一位
            num >>>= 1;
        }

        return Result.ok(count);
    }

    /**
     * 首次登录时按手机号注册一个用户，昵称用前缀加随机串。
     *
     * @param phone 手机号
     * @return 新建并已落库的用户
     */
    private User createUserWithPhone(String phone) {
        User user = User.builder()
                .phone(phone)
                .nickName(USER_NICK_NAME_PREFIX + RandomUtil.randomString(10))
                .build();
        save(user);
        return user;
    }
}
