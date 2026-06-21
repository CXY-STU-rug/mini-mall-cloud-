-- ════════════════════════════════════════════════════════════
-- 秒杀预扣库存脚本 (从单体搬, 0 改动)
--
-- 输入:
--   KEYS[1] = seckill:stock:{activityId}    库存 key (String 数字)
--   KEYS[2] = seckill:bought:{activityId}   已购用户集合 key (Set)
--   ARGV[1] = userId                          当前抢购的用户 ID
--
-- 返回值:
--    1 = 抢购成功
--    0 = 已售罄
--   -1 = 该用户已抢过 (防一人多单)
--   -2 = 活动 key 不存在 (未预热, 一般是活动未开始)
--
-- 关键: 整个脚本在 Redis 单线程里【原子执行】, 中途不会被任何客户端打断
--       所以 "检查 → 判断 → 扣减 → 记录" 4 步永远是一气呵成的
--       不像 Java 代码: 检查时 stock=10, 扣减前别的线程已经把它扣到 0 了 (超卖)
-- ════════════════════════════════════════════════════════════

-- ① 检查活动是否预热 (库存 key 不存在 = 活动还没开始)
if redis.call('EXISTS', KEYS[1]) == 0 then
    return -2
end

-- ② 检查用户是否已抢过 (Set 里有 userId 就是抢过了)
if redis.call('SISMEMBER', KEYS[2], ARGV[1]) == 1 then
    return -1
end

-- ③ 检查库存 (Redis GET 返字符串, 必须 tonumber)
local stock = tonumber(redis.call('GET', KEYS[1]))
if stock <= 0 then
    return 0
end

-- ④ 原子扣库存 + 记录已购
redis.call('DECR', KEYS[1])         -- 库存 - 1
redis.call('SADD', KEYS[2], ARGV[1]) -- 把 userId 加进已购集合

return 1
