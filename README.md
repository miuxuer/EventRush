# EventRush

赛事演出抢票平台。按分类浏览演出，看附近有哪些场馆在演，抢票下单，发观演笔记，关注别人。

后端单体应用，Spring Boot 4 + MyBatis-Plus + Redis + RabbitMQ + MySQL。

## 技术栈

| | |
|---|---|
| 语言 / 构架 | Java 17、Spring Boot 4.0.8 |
| 持久层 | MyBatis-Plus 3.5.17、MySQL |
| 缓存 / 分布式 | Redis（Lettuce）、Redisson |
| 消息队列 | RabbitMQ（Spring AMQP） |
| 其他 | Hutool、Lombok |

## 抢票是怎么做的

抢票的难点只有一个：一瞬间涌进来几万个请求，数据库扛不住，但票又不能卖超。这里把一次抢票拆成同步和异步两段。

同步段只做资格判定，全程在 Redis 里。客户端请求进来后执行 `seckill.lua`，脚本里依次做四件事：看余票够不够、看这个人是不是已经抢过、扣一张余票、把这个用户记进「已抢名单」。四步写在同一个脚本里是为了保证原子性，否则两个请求交错执行就会出现超卖或者一个人抢到两张。

校验没过就直接返回，提示「票已售罄」或者「不能重复抢票」。校验过了才生成订单号，把一条消息丢进 RabbitMQ，然后立刻把订单号返回给前端。这一步不碰数据库，所以响应很快。

异步段在消费者线程上。消费者拿到消息后加一把 Redisson 分布式锁（锁的粒度是用户），然后在事务里做三件事：再查一次这个用户有没有订单、用 `where stock > 0` 扣减库存、写入订单表。三件事任何一步失败整单回滚，不会出现扣了票没生成订单的情况。

队列用的是直连交换机加单队列，消费者手动 ACK。处理失败的先带上重试次数重新投递，超过 3 次就 nack 进死信队列，避免消息无声无息地消失。死信队列那边目前只打日志，等人工处理。

数据库里的库存和 Redis 里的余票都要扣，看着有点重复。这么设计是因为 Redis 那层负责挡住大部分流量，数据库那层是最终的账，两个都对得上才算数。

## 其他功能

演出缓存用了逻辑过期方案。缓存里存一份数据加一个逻辑过期时间，物理上不设 TTL。逻辑时间到了之后，返回旧数据的同时由一个后台线程去重建，请求不会被阻塞。`EventServiceImpl` 里另外两个 `queryWithPassThrough` 和 `queryWithMutex` 也保留着，分别是缓存空值和互斥锁两种解法，可以对比着看。

按距离查演出走 Redis 的 GEO。把同一分类下所有演出的经纬度写进一个 GEO key，查询时按坐标搜半径 5000 米并按距离排序。GEO 本身不支持物理分页，所以是查出来之后在内存里截取。

登录态不用 Session，用 token + Redis。登录时生成随机 token，把用户信息以 Hash 存进 Redis，后续请求带上 `authorization` 请求头，拦截器查 Redis 还原用户并给登录态续期。服务端因此是无状态的。

关注关系存两份，数据库存一份，Redis 的 Set 存一份。Set 那份只为了算共同关注，两个人的关注列表求交集就行，不用去数据库里做两次关联查询。

观演笔记的点赞用 ZSet 存，member 是用户 id，score 是点赞时间。用 ZSet 而不是 Set 是为了能按点赞顺序取出前几名。关注流用的是推模式：作者发笔记时遍历粉丝，把笔记 id 写进每个粉丝的收件箱 ZSet。翻页不用页码而用「上一页最后一条的时间戳 + 偏移量」当游标，因为在一边翻页一边有新内容的情况下页码分页会重复或漏数据。

签到用 BitMap，key 是「用户 + 年月」，一个月只占 4 个字节。

## 数据库表

| 表 | 说明 |
|---|---|
| `tb_event` | 演出项目，带经纬度，供 GEO 搜索 |
| `tb_event_category` | 演出分类 |
| `tb_ticket` | 票档，一个演出的不同座位区域 |
| `tb_ticket_sale` | 票档的余票和开票时间，与票档一对一 |
| `tb_ticket_order` | 抢票订单，主键是 Redis 生成的全局唯一 id |
| `tb_event_post` | 观演笔记 |
| `tb_event_post_comment` | 笔记评论，只支持两级 |
| `tb_follow` | 关注关系 |
| `tb_user` / `tb_user_info` | 用户和用户详情 |
| `tb_sign` | 签到记录 |

## 接口

### 演出

| 方法 | 路径 | 说明 |
|---|---|---|
| GET | `/event/{id}` | 演出详情 |
| POST | `/event` | 新增演出 |
| PUT | `/event` | 修改演出 |
| GET | `/event/of/category` | 按分类查，带 x/y 参数则按距离排序 |
| GET | `/event/of/name` | 按名称模糊搜索 |
| GET | `/category/list` | 全部演出分类 |

### 票档与抢票

| 方法 | 路径 | 说明 |
|---|---|---|
| GET | `/ticket/list/{eventId}` | 某演出的全部票档 |
| POST | `/ticket` | 新增普通票档 |
| POST | `/ticket/seckill` | 新增抢票票档 |
| POST | `/ticket-order/seckill/{id}` | 抢票，返回订单号 |

### 观演笔记

| 方法 | 路径 | 说明 |
|---|---|---|
| GET | `/post/hot` | 热门笔记 |
| GET | `/post/{id}` | 笔记详情 |
| POST | `/post` | 发布笔记 |
| PUT | `/post/like/{id}` | 点赞 / 取消点赞 |
| GET | `/post/likes/{id}` | 前 5 个点赞用户 |
| GET | `/post/of/me` | 我发的笔记 |
| GET | `/post/of/user` | 指定用户的笔记 |
| GET | `/post/of/follow` | 关注流 |

### 评论

| 方法 | 路径 | 说明 |
|---|---|---|
| POST | `/post-comments` | 发表评论或回复 |
| GET | `/post-comments/of/{postId}` | 笔记的评论列表 |
| DELETE | `/post-comments/{id}` | 删除自己的评论 |

### 关注与用户

| 方法 | 路径 | 说明 |
|---|---|---|
| PUT | `/follow/{id}/{isFollow}` | 关注 / 取关 |
| GET | `/follow/or/not/{id}` | 是否已关注 |
| GET | `/follow/common/{id}` | 共同关注 |
| POST | `/user/code` | 发送验证码 |
| POST | `/user/login` | 登录（自动注册） |
| POST | `/user/logout` | 退出登录 |
| GET | `/user/me` | 当前登录用户 |
| GET | `/user/{id}` | 按 id 查用户 |
| POST | `/user/sign` | 签到 |
| GET | `/user/sign/count` | 连续签到天数 |

需要登录的接口从 `authorization` 请求头里读 token。`/event/**`、`/ticket/**`、`/category/**`、`/post/hot` 这些是公开的，具体的白名单在 `MvcConfig` 里。

## 跑起来

需要 MySQL、Redis、RabbitMQ 三样。

先用 MySQL 建库导数据：

```
mysql -uroot -p < src/main/resources/db/eventrush.sql
```

Redis 用默认配置就行，本地 6379 不用改。

RabbitMQ 需要是 3.13 或以上。交换机、队列、死信队列都是在 `RabbitMQConfig` 里声明并自动创建的，不用去控制台手动建。但 `application.yaml` 里 `spring.rabbitmq.host` 现在是占位值，要改成自己 RabbitMQ 的地址：

```yaml
spring:
  rabbitmq:
    host: 192.168.56.101   # 改成自己的
    port: 5672
    username: guest
    password: guest
```

MySQL 的连接信息也在同一个文件里，默认是 `root/123456`。

启动：

```
mvn spring-boot:run
```

服务跑在 8081。

### 关于图片

`SystemConstants.IMAGE_UPLOAD_DIR` 指向一个本地目录，默认值是我机器上的路径。图片上传接口把文件写到那里，再由 Nginx 挂载对外访问。本地不配 Nginx 的话，上传接口会报错，其它功能不受影响。

## 测试

`EventRushApplicationTests` 里是几个基础测试：缓存预热、id 生成器压测、把演出坐标导入 Redis。

`TicketConcurrencyTest` 是抢票的并发测试，代替 JMeter。它造真实的 token 走完整链路（请求头 → 拦截器 → Redis → 业务），验证两件事：同一个人并发 200 次只能有 1 单；200 个人抢 100 张票恰好卖出 100 张。跑之前 RabbitMQ 必须起着，因为抢票是异步的，测试要等消费者处理完。

```
mvn test -Dtest=TicketConcurrencyTest
```

两个测试都会改数据库和 Redis 里的数据，别在有真实数据的环境上跑。

## 一些说明

验证码没有接真实短信通道，直接打在应用日志里。

图片上传目录用的是绝对路径，换机器要改。

死信队列里的消息目前只能人工看日志处理，没有做重投。

`tb_sign` 表建了但没在用，签到数据实际存在 Redis 的 BitMap 里，这张表留给以后需要签到明细时用。

订单表里有 `pay_type`、`status`、`pay_time` 这些字段，但支付、取消订单的接口还没有，现在抢到票就直接是未支付状态。
