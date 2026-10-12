-- 抢票资格校验脚本
-- 库存判断、一人一单判断、扣库存、记录下单用户这四步必须在 Redis 里原子完成，
-- 所以放在一段 Lua 脚本里执行，避免多条命令之间被其它请求插队。
--
-- 注意：真正的下单消息由 Java 侧在脚本返回 0 之后投递到 RabbitMQ，
-- Lua 里不做发消息的动作。

-- 1. 参数列表
-- 1.1. 票档id
local ticketId = ARGV[1]
-- 1.2. 用户id
local userId = ARGV[2]

-- 2. 数据key
-- 2.1. 余票key
local stockKey = 'ticket:stock:' .. ticketId
-- 2.2. 该票档已抢到票的用户集合key
local orderKey = 'ticket:order:' .. ticketId

-- 3. 脚本业务
-- 3.1. 判断余票是否充足
if(tonumber(redis.call('get', stockKey)) <= 0) then
    -- 没票了，返回1
    return 1
end
-- 3.2. 判断用户是否已经抢过该票档 SISMEMBER orderKey userId
if(redis.call('sismember', orderKey, userId) == 1) then
    -- 已经抢过，说明是重复下单，返回2
    return 2
end
-- 3.3. 扣减余票 incrby stockKey -1
redis.call('incrby', stockKey, -1)
-- 3.4. 记录抢到票的用户 sadd orderKey userId
redis.call('sadd', orderKey, userId)
-- 3.5. 返回0，表示校验通过，可以下单
return 0
