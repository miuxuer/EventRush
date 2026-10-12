/*
 EventRush 赛事演出抢票平台 - 建库建表脚本
 适用 MySQL 5.7 / 8.0

 执行方式：
   mysql -uroot -p < eventrush.sql

 或者先手动建库再执行：
   CREATE DATABASE eventrush DEFAULT CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci;
*/

SET NAMES utf8mb4;
SET FOREIGN_KEY_CHECKS = 0;

CREATE DATABASE IF NOT EXISTS `eventrush` DEFAULT CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci;
USE `eventrush`;

-- ----------------------------
-- 演出分类：首页顶部的分类导航
-- ----------------------------
DROP TABLE IF EXISTS `tb_event_category`;
CREATE TABLE `tb_event_category` (
  `id` bigint UNSIGNED NOT NULL AUTO_INCREMENT COMMENT '主键',
  `name` varchar(32) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL DEFAULT NULL COMMENT '分类名称',
  `icon` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL DEFAULT NULL COMMENT '分类图标',
  `sort` int UNSIGNED NULL DEFAULT NULL COMMENT '展示顺序',
  `create_time` timestamp NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_time` timestamp NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`id`) USING BTREE
) ENGINE = InnoDB AUTO_INCREMENT = 11 CHARACTER SET = utf8mb4 COLLATE = utf8mb4_general_ci COMMENT = '演出分类表' ROW_FORMAT = Compact;

INSERT INTO `tb_event_category` VALUES (1, '演唱会', '/types/yanchanghui.png', 1, '2026-01-05 10:17:47', '2026-01-05 10:24:31');
INSERT INTO `tb_event_category` VALUES (2, '音乐节', '/types/yinyuejie.png', 2, '2026-01-05 10:18:27', '2026-01-05 10:24:31');
INSERT INTO `tb_event_category` VALUES (3, '话剧歌剧', '/types/huaju.png', 3, '2026-01-05 10:18:48', '2026-01-05 10:24:31');
INSERT INTO `tb_event_category` VALUES (4, '音乐剧', '/types/yinyueju.png', 4, '2026-01-05 10:19:04', '2026-01-05 10:24:31');
INSERT INTO `tb_event_category` VALUES (5, '体育赛事', '/types/tiyu.png', 5, '2026-01-05 10:19:27', '2026-01-05 10:24:31');
INSERT INTO `tb_event_category` VALUES (6, '相声曲艺', '/types/xiangsheng.png', 6, '2026-01-05 10:19:35', '2026-01-05 10:24:31');
INSERT INTO `tb_event_category` VALUES (7, '儿童亲子', '/types/ertong.png', 7, '2026-01-05 10:19:53', '2026-01-05 10:24:31');
INSERT INTO `tb_event_category` VALUES (8, '舞蹈芭蕾', '/types/wudao.png', 8, '2026-01-05 10:20:02', '2026-01-05 10:24:31');
INSERT INTO `tb_event_category` VALUES (9, '展览市集', '/types/zhanlan.png', 9, '2026-01-05 10:20:08', '2026-01-05 10:24:31');
INSERT INTO `tb_event_category` VALUES (10, '脱口秀', '/types/tuokouxiu.png', 10, '2026-01-05 10:21:46', '2026-01-05 10:24:31');

-- ----------------------------
-- 演出：赛事、演唱会、话剧等演出项目。带经纬度，用于按距离搜索
-- ----------------------------
DROP TABLE IF EXISTS `tb_event`;
CREATE TABLE `tb_event` (
  `id` bigint UNSIGNED NOT NULL AUTO_INCREMENT COMMENT '主键',
  `name` varchar(128) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL COMMENT '演出名称',
  `category_id` bigint UNSIGNED NOT NULL COMMENT '演出分类id',
  `images` varchar(1024) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL COMMENT '演出图片，多个图片以","隔开',
  `area` varchar(128) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL DEFAULT NULL COMMENT '演出所在城区，例如钱江新城',
  `address` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL COMMENT '演出场馆地址',
  `x` double UNSIGNED NOT NULL COMMENT '经度',
  `y` double UNSIGNED NOT NULL COMMENT '纬度',
  `avg_price` bigint UNSIGNED NULL DEFAULT NULL COMMENT '票价均价，取整数，单位元',
  `sold` int UNSIGNED NOT NULL DEFAULT 0 COMMENT '已售张数',
  `comments` int UNSIGNED NOT NULL DEFAULT 0 COMMENT '观演笔记数量',
  `score` int UNSIGNED NOT NULL DEFAULT 0 COMMENT '评分，1~5分，乘10保存，避免小数',
  `show_time` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL DEFAULT NULL COMMENT '演出时间，例如 2026-05-01 19:30',
  `create_time` timestamp NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_time` timestamp NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`id`) USING BTREE,
  INDEX `idx_category`(`category_id`) USING BTREE
) ENGINE = InnoDB AUTO_INCREMENT = 15 CHARACTER SET = utf8mb4 COLLATE = utf8mb4_general_ci COMMENT = '演出项目表' ROW_FORMAT = Compact;

INSERT INTO `tb_event` VALUES (1, '陈奕迅 FEAR and DREAMS 世界巡回演唱会·杭州站', 1, '/imgs/events/eason1.jpg,/imgs/events/eason2.jpg,/imgs/events/eason3.jpg', '奥体中心', '杭州市滨江区飞虹路1号杭州奥体中心体育场', 120.22913, 30.20672, 880, 38621, 1284, 49, '2026-05-01 19:30', '2026-01-06 10:10:39', '2026-01-18 17:32:19');
INSERT INTO `tb_event` VALUES (2, '五月天 诺亚方舟 25 周年巡回演唱会·杭州站', 1, '/imgs/events/mayday1.jpg,/imgs/events/mayday2.jpg', '黄龙', '杭州市西湖区黄龙路1号黄龙体育中心体育场', 120.13100, 30.26490, 780, 41205, 1632, 48, '2026-06-13 19:00', '2026-01-06 19:00:13', '2026-01-18 16:12:26');
INSERT INTO `tb_event` VALUES (3, '2026 杭州草莓音乐节', 2, '/imgs/events/strawberry1.jpg,/imgs/events/strawberry2.jpg,/imgs/events/strawberry3.jpg', '钱江新城', '杭州市萧山区奔竞大道353号杭州国际博览中心', 120.24750, 30.23300, 420, 23860, 906, 47, '2026-04-18 13:00', '2026-01-06 19:10:05', '2026-01-18 16:12:42');
INSERT INTO `tb_event` VALUES (4, '开心麻花爆笑舞台剧《乌龙山伯爵》', 3, '/imgs/events/mahua1.jpg,/imgs/events/mahua2.jpg', '钱江新城', '杭州市上城区之江路1号杭州大剧院', 120.21550, 30.24700, 280, 12640, 512, 47, '2026-03-21 19:30', '2026-01-06 19:17:15', '2026-01-18 16:12:51');
INSERT INTO `tb_event` VALUES (5, '音乐剧《剧院魅影》中文版', 4, '/imgs/events/phantom1.jpg,/imgs/events/phantom2.jpg,/imgs/events/phantom3.jpg', '钱江新城', '杭州市上城区之江路1号杭州大剧院歌剧院', 120.21580, 30.24730, 580, 15980, 734, 49, '2026-05-20 19:30', '2026-01-06 19:20:58', '2026-01-18 16:13:01');
INSERT INTO `tb_event` VALUES (6, 'CBA 常规赛 浙江广厦 VS 辽宁本钢', 5, '/imgs/events/cba1.jpg,/imgs/events/cba2.jpg', '奥体中心', '杭州市滨江区飞虹路1号杭州奥体中心体育馆', 120.22860, 30.20710, 260, 9860, 341, 46, '2026-03-08 19:35', '2026-01-06 19:24:53', '2026-01-18 16:13:09');
INSERT INTO `tb_event` VALUES (7, '2026 中国足球协会超级杯', 5, '/imgs/events/supercup1.jpg,/imgs/events/supercup2.jpg', '黄龙', '杭州市西湖区黄龙路1号黄龙体育中心体育场', 120.13070, 30.26520, 180, 21470, 623, 45, '2026-02-28 15:30', '2026-01-06 19:40:52', '2026-01-18 16:13:19');
INSERT INTO `tb_event` VALUES (8, '德云社 郭德纲·于谦相声专场·杭州', 6, '/imgs/events/deyunshe1.jpg,/imgs/events/deyunshe2.jpg', '武林广场', '杭州市下城区武林广场29号杭州剧院', 120.16560, 30.26420, 380, 7420, 288, 48, '2026-04-11 19:30', '2026-01-06 19:51:06', '2026-01-18 16:13:25');
INSERT INTO `tb_event` VALUES (9, '孟京辉经典戏剧《恋爱的犀牛》', 3, '/imgs/events/rhino1.jpg,/imgs/events/rhino2.jpg', '城西', '杭州市西湖区教工路18号杭州蜂巢剧场', 120.14260, 30.28240, 220, 5680, 246, 47, '2026-03-14 19:30', '2026-01-06 19:53:59', '2026-01-18 16:13:34');
INSERT INTO `tb_event` VALUES (10, '亲子音乐剧《小王子》', 7, '/imgs/events/littleprince1.jpg,/imgs/events/littleprince2.jpg', '钱江新城', '杭州市上城区之江路1号杭州大剧院音乐厅', 120.21530, 30.24680, 160, 4310, 176, 46, '2026-03-29 14:30', '2026-01-07 10:25:16', '2026-01-07 10:25:16');
INSERT INTO `tb_event` VALUES (11, '上海芭蕾舞团 经典版《天鹅湖》', 8, '/imgs/events/swan1.jpg,/imgs/events/swan2.jpg', '拱墅', '杭州市拱墅区运河天地文化广场杭州运河大剧院', 120.14200, 30.32200, 480, 6120, 198, 49, '2026-05-09 19:30', '2026-01-07 10:29:02', '2026-01-07 10:39:00');
INSERT INTO `tb_event` VALUES (12, 'teamLab 无界艺术展·杭州', 9, '/imgs/events/teamlab1.jpg,/imgs/events/teamlab2.jpg', '钱江新城', '杭州市萧山区奔竞大道353号杭州国际博览中心3号馆', 120.24700, 30.23340, 199, 28640, 1024, 48, '2026-02-01 10:00', '2026-01-07 10:34:34', '2026-01-07 10:34:34');
INSERT INTO `tb_event` VALUES (13, '单立人喜剧节·杭州站', 10, '/imgs/events/danliren1.jpg,/imgs/events/danliren2.jpg', '湖滨', '杭州市上城区东坡路10号杭州东坡大剧院', 120.16800, 30.25300, 180, 5120, 209, 47, '2026-04-25 19:30', '2026-01-07 10:38:54', '2026-01-07 10:40:04');
INSERT INTO `tb_event` VALUES (14, '杭州爱乐乐团 2026 春季音乐会', 3, '/imgs/events/hzpo1.jpg,/imgs/events/hzpo2.jpg', '钱江新城', '杭州市上城区之江路1号杭州大剧院音乐厅', 120.21510, 30.24660, 320, 3240, 142, 49, '2026-03-06 19:30', '2026-01-07 10:48:54', '2026-01-07 10:48:54');

-- ----------------------------
-- 票档：一个演出下的不同座位区域，各自有售价和余票
-- type = 0 普通票档，可以直接购买；type = 1 抢票票档，走抢票流程
-- ----------------------------
DROP TABLE IF EXISTS `tb_ticket`;
CREATE TABLE `tb_ticket` (
  `id` bigint UNSIGNED NOT NULL AUTO_INCREMENT COMMENT '主键',
  `event_id` bigint UNSIGNED NULL DEFAULT NULL COMMENT '所属演出id',
  `title` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL COMMENT '票档标题，例如 内场票',
  `sub_title` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL DEFAULT NULL COMMENT '副标题',
  `rules` varchar(1024) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL DEFAULT NULL COMMENT '购票须知',
  `pay_value` bigint UNSIGNED NOT NULL COMMENT '售价，单位分。例如 88000 代表 880 元',
  `actual_value` bigint NOT NULL COMMENT '原价，单位分',
  `type` tinyint UNSIGNED NOT NULL DEFAULT 0 COMMENT '0 普通票档；1 抢票票档',
  `status` tinyint UNSIGNED NOT NULL DEFAULT 1 COMMENT '1 在售；2 下架；3 已过期',
  `create_time` timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_time` timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`id`) USING BTREE,
  INDEX `idx_event`(`event_id`) USING BTREE
) ENGINE = InnoDB AUTO_INCREMENT = 12 CHARACTER SET = utf8mb4 COLLATE = utf8mb4_general_ci COMMENT = '票档表' ROW_FORMAT = Compact;

INSERT INTO `tb_ticket` VALUES (1, 1, '陈奕迅演唱会·内场票', '舞台前区内场，一人一证一票', '实名制购票，入场需持本人身份证件\\n一单限购2张\\n不支持退换，可转赠', 128000, 158000, 1, 1, '2026-01-08 09:42:39', '2026-01-08 09:43:31');
INSERT INTO `tb_ticket` VALUES (2, 1, '陈奕迅演唱会·看台票', '二层看台，视野开阔', '实名制购票，入场需持本人身份证件\\n一单限购4张', 58000, 78000, 0, 1, '2026-01-08 09:45:12', '2026-01-08 09:45:12');
INSERT INTO `tb_ticket` VALUES (3, 2, '五月天演唱会·摇滚区', '内场摇滚区，无座', '实名制购票\\n一单限购2张\\n不建议儿童入场', 108000, 128000, 1, 1, '2026-01-08 10:02:18', '2026-01-08 10:02:18');
INSERT INTO `tb_ticket` VALUES (4, 3, '草莓音乐节·单日通票', '含当日全部舞台演出', '凭电子票二维码入场\\n一单限购6张\\n雨天照常举行', 42000, 52000, 0, 1, '2026-01-08 10:11:40', '2026-01-08 10:11:40');
INSERT INTO `tb_ticket` VALUES (5, 5, '剧院魅影·池座票', '一楼池座，最佳观演位置', '演出开始后谢绝入场\\n1.2米以下儿童谢绝入场', 88000, 108000, 1, 1, '2026-01-08 10:20:05', '2026-01-08 10:20:05');
INSERT INTO `tb_ticket` VALUES (6, 6, 'CBA 常规赛·乙级票', '看台乙级区域', '凭票入场，对号入座\\n一单限购4张', 26000, 32000, 0, 1, '2026-01-08 10:26:30', '2026-01-08 10:26:30');
INSERT INTO `tb_ticket` VALUES (7, 7, '超级杯·普通看台票', '东西看台', '凭票入场，对号入座\\n雨天照常举行', 18000, 24000, 0, 1, '2026-01-08 10:31:22', '2026-01-08 10:31:22');
INSERT INTO `tb_ticket` VALUES (8, 8, '德云社相声专场·一楼中区', '一楼中区，近距离观演', '演出期间请勿录音录像\\n一人一票，儿童全票', 38000, 48000, 1, 1, '2026-01-08 10:36:44', '2026-01-08 10:36:44');
INSERT INTO `tb_ticket` VALUES (9, 9, '恋爱的犀牛·普通票', '剧场通票，不对号', '演出开始后谢绝入场', 22000, 28000, 0, 1, '2026-01-08 10:41:09', '2026-01-08 10:41:09');
INSERT INTO `tb_ticket` VALUES (10, 12, 'teamLab 无界艺术展·全通票', '展期内任意一天可用', '凭电子票入场\\n1米以下儿童免票', 19900, 25900, 0, 1, '2026-01-08 10:46:51', '2026-01-08 10:46:51');
INSERT INTO `tb_ticket` VALUES (11, 13, '单立人喜剧节·单场票', '任选一场专场演出', '演出期间请勿录音录像\\n不建议16岁以下观众入场', 18000, 22000, 1, 1, '2026-01-08 10:52:13', '2026-01-08 10:52:13');

-- ----------------------------
-- 票档库存/开票时间：和票档是一对一关系
-- 抢票时先在 Redis 里扣减 stock，异步落库时再扣这里的 stock
-- ----------------------------
DROP TABLE IF EXISTS `tb_ticket_sale`;
CREATE TABLE `tb_ticket_sale` (
  `ticket_id` bigint UNSIGNED NOT NULL COMMENT '关联的票档id',
  `stock` int NOT NULL COMMENT '剩余可售张数',
  `create_time` timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `begin_time` timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '开抢时间',
  `end_time` timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '停售时间',
  `update_time` timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`ticket_id`) USING BTREE
) ENGINE = InnoDB CHARACTER SET = utf8mb4 COLLATE = utf8mb4_general_ci COMMENT = '票档库存表，与票档是一对一关系' ROW_FORMAT = Compact;

INSERT INTO `tb_ticket_sale` VALUES (1, 800, '2026-01-08 09:42:39', '2026-01-01 00:00:00', '2026-05-01 12:00:00', '2026-01-08 09:42:39');
INSERT INTO `tb_ticket_sale` VALUES (3, 1200, '2026-01-08 10:02:18', '2026-01-01 00:00:00', '2026-06-13 12:00:00', '2026-01-08 10:02:18');
INSERT INTO `tb_ticket_sale` VALUES (5, 600, '2026-01-08 10:20:05', '2026-01-01 00:00:00', '2026-05-20 12:00:00', '2026-01-08 10:20:05');
INSERT INTO `tb_ticket_sale` VALUES (8, 400, '2026-01-08 10:36:44', '2026-01-01 00:00:00', '2026-04-11 12:00:00', '2026-01-08 10:36:44');
INSERT INTO `tb_ticket_sale` VALUES (11, 300, '2026-01-08 10:52:13', '2026-01-01 00:00:00', '2026-04-25 12:00:00', '2026-01-08 10:52:13');

-- ----------------------------
-- 抢票订单
-- ----------------------------
DROP TABLE IF EXISTS `tb_ticket_order`;
CREATE TABLE `tb_ticket_order` (
  `id` bigint NOT NULL COMMENT '主键，由 Redis 生成的全局唯一id，不是自增',
  `user_id` bigint UNSIGNED NOT NULL COMMENT '抢票的用户id',
  `ticket_id` bigint UNSIGNED NOT NULL COMMENT '票档id',
  `pay_type` tinyint UNSIGNED NOT NULL DEFAULT 1 COMMENT '支付方式 1：余额支付；2：支付宝；3：微信',
  `status` tinyint UNSIGNED NOT NULL DEFAULT 1 COMMENT '订单状态，1：未支付；2：已支付；3：已核销；4：已取消；5：退款中；6：已退款',
  `create_time` timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '下单时间',
  `pay_time` timestamp NULL DEFAULT NULL COMMENT '支付时间',
  `use_time` timestamp NULL DEFAULT NULL COMMENT '核销时间',
  `refund_time` timestamp NULL DEFAULT NULL COMMENT '退款时间',
  `update_time` timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`id`) USING BTREE,
  INDEX `idx_user_ticket`(`user_id`, `ticket_id`) USING BTREE
) ENGINE = InnoDB CHARACTER SET = utf8mb4 COLLATE = utf8mb4_general_ci COMMENT = '抢票订单表' ROW_FORMAT = Compact;

-- ----------------------------
-- 观演笔记：用户看完演出后发的图文感受
-- ----------------------------
DROP TABLE IF EXISTS `tb_event_post`;
CREATE TABLE `tb_event_post` (
  `id` bigint UNSIGNED NOT NULL AUTO_INCREMENT COMMENT '主键',
  `event_id` bigint UNSIGNED NOT NULL COMMENT '关联的演出id',
  `user_id` bigint UNSIGNED NOT NULL COMMENT '作者id',
  `title` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '标题',
  `images` varchar(2048) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL COMMENT '现场照片，最多9张，多张以","隔开',
  `content` varchar(2048) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '正文',
  `liked` int UNSIGNED NULL DEFAULT 0 COMMENT '点赞数',
  `comments` int UNSIGNED NULL DEFAULT 0 COMMENT '评论数',
  `create_time` timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_time` timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`id`) USING BTREE,
  INDEX `idx_event`(`event_id`) USING BTREE
) ENGINE = InnoDB AUTO_INCREMENT = 8 CHARACTER SET = utf8mb4 COLLATE = utf8mb4_general_ci COMMENT = '观演笔记表' ROW_FORMAT = Compact;

INSERT INTO `tb_event_post` VALUES (1, 1, 2, '抢到内场票了！陈奕迅杭州站观演repo', '/imgs/posts/7/14/eason-live-01.jpg,/imgs/posts/4/10/eason-live-02.jpg,/imgs/posts/2/6/eason-live-03.jpg', '蹲了三个月的票终于抢到了，内场第七排，这个位置看台效果绝了。<br/>开场《富士山下》前奏一响全场直接炸，手机闪光灯亮成星海。<br/><br/>几个实用信息：<br/>1. 奥体中心体育场地铁6号线博览中心站出来走10分钟就到，建议提前一个半小时到，安检排队很长。<br/>2. 内场是站票，穿舒服的鞋。<br/>3. 场馆里不能带瓶装水，进去买矿泉水10块一瓶。<br/><br/>散场的时候人特别多，打车基本打不到，建议坐地铁或者往滨江方向走两站再叫车。', 326, 18, '2026-01-12 19:50:01', '2026-01-20 14:26:34');
INSERT INTO `tb_event_post` VALUES (2, 3, 2, '草莓音乐节杭州站攻略｜第一次去音乐节要准备什么', '/imgs/posts/4/7/strawberry-01.jpg,/imgs/posts/11/12/strawberry-02.jpg,/imgs/posts/4/1/strawberry-03.jpg', '第一次去音乐节，踩了不少坑，整理一下给后面的朋友。<br/><br/>穿搭：<br/>别穿裙子和高跟鞋，草地上站一天真的会废。运动鞋+裤子最稳。<br/><br/>必备：<br/>防晒霜（要能补涂的）、充电宝（拍一天视频手机撑不住）、一次性雨衣（下雨照常办，伞会被收）。<br/><br/>吃的：<br/>里面小吃挺多但价格翻倍，烤肠30一根。可以带点能量棒。<br/><br/>时间安排：<br/>主舞台的压轴一般在天黑之后，下午的场子人少可以慢慢逛。喜欢小众乐队的话早点去副舞台，能站到很近的位置。', 218, 12, '2026-01-13 20:57:49', '2026-01-19 09:21:39');
INSERT INTO `tb_event_post` VALUES (3, 4, 1, '开心麻花《乌龙山伯爵》｜笑到肚子疼的两个小时', '/imgs/posts/3/5/mahua-01.jpg', '杭州大剧院看的，位置在二楼包厢，视野比想象中好。<br/>剧情反转挺多的，中间有几段真的笑到停不下来。<br/>演员现场感很强，即兴接梗的部分比剧本本身还好看。<br/>散场后可以在后台通道等演员签名，工作人员不会赶人。', 96, 5, '2026-01-15 16:05:47', '2026-01-19 09:21:41');
INSERT INTO `tb_event_post` VALUES (4, 12, 5, 'teamLab无界艺术展｜出片率最高的几个展区', '/imgs/posts/9/2/teamlab-01.jpg,/imgs/posts/6/8/teamlab-02.jpg', '全通票199，展期到三月底，建议工作日去，周末人实在太多了。<br/><br/>必打卡：<br/>1. 镜像屋——整个房间都是镜子和灯，随便拍都好看，但人多的时候要排队。<br/>2. 光之瀑布——最适合拍剪影，穿浅色衣服效果好。<br/>3. 呼应森林——会随着人的移动变化的投影，互动性强。<br/><br/>小提示：穿白色或者浅色的衣服，深色会被背景吃掉。展馆里比较暗，手机记得开夜景模式。', 154, 9, '2026-01-17 11:32:10', '2026-01-20 15:02:33');

-- ----------------------------
-- 观演笔记的评论，只支持两级
-- parent_id = 0 是一级评论；回复时 parent_id 指向一级评论，answer_id 指向被回复的那条评论
-- ----------------------------
DROP TABLE IF EXISTS `tb_event_post_comment`;
CREATE TABLE `tb_event_post_comment` (
  `id` bigint UNSIGNED NOT NULL AUTO_INCREMENT COMMENT '主键',
  `user_id` bigint UNSIGNED NOT NULL COMMENT '评论者id',
  `post_id` bigint UNSIGNED NOT NULL COMMENT '所属笔记id',
  `parent_id` bigint UNSIGNED NOT NULL DEFAULT 0 COMMENT '一级评论id，一级评论本身为0',
  `answer_id` bigint UNSIGNED NOT NULL DEFAULT 0 COMMENT '被回复的那条评论id',
  `content` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL COMMENT '评论内容',
  `liked` int UNSIGNED NULL DEFAULT 0 COMMENT '点赞数',
  `status` tinyint UNSIGNED NULL DEFAULT 0 COMMENT '状态，0：正常，1：被举报，2：禁止查看',
  `create_time` timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_time` timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`id`) USING BTREE,
  INDEX `idx_post_parent`(`post_id`, `parent_id`) USING BTREE
) ENGINE = InnoDB AUTO_INCREMENT = 1 CHARACTER SET = utf8mb4 COLLATE = utf8mb4_general_ci COMMENT = '观演笔记评论表' ROW_FORMAT = Compact;

-- ----------------------------
-- 关注关系
-- ----------------------------
DROP TABLE IF EXISTS `tb_follow`;
CREATE TABLE `tb_follow` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '主键',
  `user_id` bigint UNSIGNED NOT NULL COMMENT '关注别人的用户id',
  `follow_user_id` bigint UNSIGNED NOT NULL COMMENT '被关注的用户id',
  `create_time` timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  PRIMARY KEY (`id`) USING BTREE,
  INDEX `idx_user`(`user_id`) USING BTREE
) ENGINE = InnoDB AUTO_INCREMENT = 1 CHARACTER SET = utf8mb4 COLLATE = utf8mb4_general_ci COMMENT = '关注表' ROW_FORMAT = Compact;

-- ----------------------------
-- 用户签到记录
-- 实际签到数据存在 Redis 的 BitMap 里，这张表留作后续需要明细时使用
-- ----------------------------
DROP TABLE IF EXISTS `tb_sign`;
CREATE TABLE `tb_sign` (
  `id` bigint UNSIGNED NOT NULL AUTO_INCREMENT COMMENT '主键',
  `user_id` bigint UNSIGNED NOT NULL COMMENT '用户id',
  `year` year NOT NULL COMMENT '签到的年',
  `month` tinyint NOT NULL COMMENT '签到的月',
  `date` date NOT NULL COMMENT '签到的日期',
  `is_backup` tinyint UNSIGNED NULL DEFAULT NULL COMMENT '是否补签',
  PRIMARY KEY (`id`) USING BTREE
) ENGINE = InnoDB AUTO_INCREMENT = 1 CHARACTER SET = utf8mb4 COLLATE = utf8mb4_general_ci COMMENT = '签到记录表' ROW_FORMAT = Compact;

-- ----------------------------
-- 用户
-- ----------------------------
DROP TABLE IF EXISTS `tb_user`;
CREATE TABLE `tb_user` (
  `id` bigint UNSIGNED NOT NULL AUTO_INCREMENT COMMENT '主键',
  `phone` varchar(11) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL COMMENT '手机号码',
  `password` varchar(128) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL DEFAULT '' COMMENT '密码，加密存储',
  `nick_name` varchar(32) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL DEFAULT '' COMMENT '昵称，默认是随机字符',
  `icon` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL DEFAULT '' COMMENT '头像',
  `create_time` timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_time` timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`id`) USING BTREE,
  UNIQUE INDEX `uniqe_key_phone`(`phone`) USING BTREE
) ENGINE = InnoDB AUTO_INCREMENT = 1001 CHARACTER SET = utf8mb4 COLLATE = utf8mb4_general_ci COMMENT = '用户表' ROW_FORMAT = Compact;

INSERT INTO `tb_user` VALUES (1, '13686869696', '', '抢票不睡觉', '/imgs/icons/user1.jpg', '2026-01-06 10:27:19', '2026-01-17 16:04:00');
INSERT INTO `tb_user` VALUES (2, '13838411438', '', '可可今天看演出', '/imgs/icons/user2.jpg', '2026-01-06 15:14:39', '2026-01-18 19:58:04');
INSERT INTO `tb_user` VALUES (3, '13456789011', '', 'user_slxaxy2au9f3tanffaxr', '', '2026-01-07 12:07:53', '2026-01-07 12:07:53');
INSERT INTO `tb_user` VALUES (5, '13456789001', '', '内场第七排', '/imgs/icons/user5.png', '2026-01-07 16:11:33', '2026-01-20 09:09:20');
INSERT INTO `tb_user` VALUES (6, '13456762069', '', 'user_xn5wr3hpsv', '', '2026-01-07 17:54:10', '2026-01-07 17:54:10');
INSERT INTO `tb_user` VALUES (7, '13688668889', '', '追演出的打工人', '/imgs/icons/user7.jpg', '2026-01-08 10:50:47', '2026-01-19 14:22:11');
INSERT INTO `tb_user` VALUES (8, '13688668890', '', 'user_qcfr2k1lmi', '', '2026-01-08 10:50:47', '2026-01-08 10:50:47');
INSERT INTO `tb_user` VALUES (9, '13688668891', '', '话剧爱好者', '/imgs/icons/user9.jpg', '2026-01-08 10:50:47', '2026-01-20 11:03:45');
INSERT INTO `tb_user` VALUES (10, '13688668892', '', 'user_r62q62ijef', '', '2026-01-08 10:50:47', '2026-01-08 10:50:47');

-- ----------------------------
-- 用户详情
-- ----------------------------
DROP TABLE IF EXISTS `tb_user_info`;
CREATE TABLE `tb_user_info` (
  `user_id` bigint UNSIGNED NOT NULL COMMENT '主键，用户id',
  `city` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL DEFAULT '' COMMENT '城市名称',
  `introduce` varchar(128) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL DEFAULT NULL COMMENT '个人介绍，不要超过128个字符',
  `fans` int UNSIGNED NULL DEFAULT 0 COMMENT '粉丝数量',
  `followee` int UNSIGNED NULL DEFAULT 0 COMMENT '关注的人的数量',
  `gender` tinyint UNSIGNED NULL DEFAULT 0 COMMENT '性别，0：男，1：女',
  `birthday` date NULL DEFAULT NULL COMMENT '生日',
  `credits` int UNSIGNED NULL DEFAULT 0 COMMENT '积分',
  `level` tinyint UNSIGNED NULL DEFAULT 0 COMMENT '会员级别，0~9级，0代表未开通会员',
  `create_time` timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_time` timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`user_id`) USING BTREE
) ENGINE = InnoDB CHARACTER SET = utf8mb4 COLLATE = utf8mb4_general_ci COMMENT = '用户详情表' ROW_FORMAT = Compact;

INSERT INTO `tb_user_info` VALUES (1, '杭州', '一年看二十场演出的普通人', 128, 46, 0, '1996-04-18', 3200, 2, '2026-01-06 10:27:19', '2026-01-17 16:04:00');
INSERT INTO `tb_user_info` VALUES (2, '杭州', '音乐节选手，主攻摇滚和民谣', 356, 88, 1, '1998-09-02', 5120, 3, '2026-01-06 15:14:39', '2026-01-18 19:58:04');
INSERT INTO `tb_user_info` VALUES (5, '杭州', '话剧和音乐剧都看，偶尔写写repo', 92, 31, 1, '1999-11-25', 1860, 1, '2026-01-07 16:11:33', '2026-01-20 09:09:20');
INSERT INTO `tb_user_info` VALUES (7, '宁波', '周末往返杭州看演出', 47, 62, 0, '1995-02-14', 940, 0, '2026-01-08 10:50:47', '2026-01-19 14:22:11');
INSERT INTO `tb_user_info` VALUES (9, '杭州', '剧场常客，喜欢小剧场', 210, 74, 1, '1997-06-30', 2680, 2, '2026-01-08 10:50:47', '2026-01-20 11:03:45');

SET FOREIGN_KEY_CHECKS = 1;
