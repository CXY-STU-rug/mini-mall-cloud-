package com.minimall.order.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.minimall.common.core.domain.Result;
import com.minimall.common.core.exception.BusinessException;
import com.minimall.order.client.ProductFeignClient;
import com.minimall.order.config.RabbitMQConfig;
import com.minimall.order.dto.SeckillActivityDTO;
import com.minimall.order.entity.SeckillActivity;
import com.minimall.order.entity.SeckillOrder;
import com.minimall.order.mapper.SeckillActivityMapper;
import com.minimall.order.mapper.SeckillOrderMapper;
import com.minimall.order.service.ISeckillActivityService;
import com.minimall.order.vo.SeckillActivityVO;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 秒杀活动服务实现 (G3.8 - 微服务版)
 *
 * ════════════════════════════════════════════════════════════════
 * vs 单体差异:
 *   ① userId 不读 UserContext, 从方法参数传入
 *   ② productService → ProductFeignClient (跨服务查商品)
 *   ③ 商品数据返 Map<String, Object> (order 引不到 Product entity)
 *   ④ 其他逻辑 0 改动 (Lua + MQ 通路同单体)
 *
 * 4 方法:
 *   publishActivity        简单 CRUD + 业务校验
 *   listActiveActivities   批量查 + Feign × N
 *   seckill                ⭐⭐⭐ 全 G3.8 最核心: 5 步 + Lua + MQ
 *   querySeckillResult     3 态轮询查询
 * ════════════════════════════════════════════════════════════════
 */
@Service
public class SeckillActivityServiceImpl
        extends ServiceImpl<SeckillActivityMapper, SeckillActivity>
        implements ISeckillActivityService {

    @Autowired private ProductFeignClient productFeignClient;
    @Autowired private StringRedisTemplate stringRedisTemplate;   // Lua 用 String 版
    @Autowired private DefaultRedisScript<Long> seckillStockScript;
    @Autowired private RabbitTemplate rabbitTemplate;
    @Autowired private SeckillOrderMapper seckillOrderMapper;

    // ════════════════════════════════════════════════════════════
    // ① 管理员发布秒杀活动
    // ════════════════════════════════════════════════════════════
    @Override
    public Long publishActivity(SeckillActivityDTO dto) {
        // 参数校验
        if (dto.getProductId() == null || dto.getProductId() <= 0) {
            throw new BusinessException(400, "商品 ID 必填");
        }
        if (dto.getSeckillPrice() == null || dto.getSeckillPrice().compareTo(BigDecimal.ZERO) <= 0) {
            throw new BusinessException(400, "秒杀价必须大于 0");
        }
        if (dto.getStock() == null || dto.getStock() <= 0) {
            throw new BusinessException(400, "库存必须大于 0");
        }
        if (dto.getStartTime() == null || dto.getEndTime() == null) {
            throw new BusinessException(400, "活动时间必填");
        }
        if (dto.getStartTime().isBefore(LocalDateTime.now())) {
            throw new BusinessException(400, "开始时间不能早于现在");
        }
        if (dto.getEndTime().isBefore(dto.getStartTime())) {
            throw new BusinessException(400, "结束时间必须晚于开始时间");
        }

        // Feign 调 product 校验商品 + 拿原价
        Result<Map<String, Object>> resp = productFeignClient.getById(dto.getProductId());
        if (resp == null || resp.getCode() != 200 || resp.getData() == null) {
            throw new BusinessException(400, "商品不存在");
        }
        Map<String, Object> p = resp.getData();
        if (Integer.parseInt(p.get("status").toString()) == 0) {
            throw new BusinessException(400, "商品已下架");
        }
        // 秒杀价必须 < 原价
        BigDecimal originalPrice = new BigDecimal(p.get("price").toString());
        if (dto.getSeckillPrice().compareTo(originalPrice) >= 0) {
            throw new BusinessException(400, "秒杀价必须低于商品原价");
        }
        // 商品库存 ≥ 秒杀库存
        Integer productStock = Integer.parseInt(p.get("stock").toString());
        if (productStock < dto.getStock()) {
            throw new BusinessException(400, "商品库存不足以支撑秒杀");
        }

        // 入库
        SeckillActivity a = new SeckillActivity();
        a.setProductId(dto.getProductId());
        a.setSeckillPrice(dto.getSeckillPrice());
        a.setStock(dto.getStock());
        a.setStartTime(dto.getStartTime());
        a.setEndTime(dto.getEndTime());
        a.setStatus((byte) 0);   // 0 = 待开始
        this.save(a);
        return a.getId();
    }

    // ════════════════════════════════════════════════════════════
    // ② 查活动列表 (待开始 + 进行中, 排除已结束)
    // ════════════════════════════════════════════════════════════
    @Override
    public List<SeckillActivityVO> listActiveActivities() {
        QueryWrapper<SeckillActivity> w = new QueryWrapper<>();
        w.ne("status", 2).orderByAsc("start_time");   // status != 2 (已结束)
        List<SeckillActivity> activities = this.list(w);
        if (activities.isEmpty()) return new ArrayList<>();

        // 循环 Feign 查商品 (跟 G3.7 createOrder 一样, 性能差但简单)
        Map<Long, Map<String, Object>> productMap = new HashMap<>();
        for (SeckillActivity a : activities) {
            Result<Map<String, Object>> resp = productFeignClient.getById(a.getProductId());
            if (resp != null && resp.getCode() == 200 && resp.getData() != null) {
                productMap.put(a.getProductId(), resp.getData());
            }
        }

        List<SeckillActivityVO> result = new ArrayList<>();
        for (SeckillActivity a : activities) {
            Map<String, Object> p = productMap.get(a.getProductId());
            SeckillActivityVO vo = new SeckillActivityVO();
            vo.setId(a.getId());
            vo.setProductId(a.getProductId());
            vo.setProductName(p != null ? (String) p.get("name") : null);
            vo.setProductImage(p != null ? (String) p.get("coverImage") : null);
            vo.setOriginalPrice(p != null ? new BigDecimal(p.get("price").toString()) : null);
            vo.setSeckillPrice(a.getSeckillPrice());
            vo.setStock(a.getStock());
            vo.setStartTime(a.getStartTime());
            vo.setEndTime(a.getEndTime());
            vo.setStatus(a.getStatus());
            vo.setStatusDesc(statusDesc(a.getStatus()));
            result.add(vo);
        }
        return result;
    }

    // ════════════════════════════════════════════════════════════
    // ③ 秒杀核心 ⭐⭐⭐
    // ════════════════════════════════════════════════════════════
    @Override
    public String seckill(Long userId, Long activityId) {
        // ─── 第 1 步: 校验活动 ───
        SeckillActivity activity = this.getById(activityId);
        if (activity == null) throw new BusinessException(404, "活动不存在");
        LocalDateTime now = LocalDateTime.now();
        if (now.isBefore(activity.getStartTime())) {
            throw new BusinessException(400, "活动还未开始");
        }
        if (now.isAfter(activity.getEndTime())) {
            throw new BusinessException(400, "活动已结束");
        }

        // ─── 第 2 步: Redis 库存懒加载 ───
        // 第一次访问时把 DB 库存灌进 Redis, 后续 Lua 直接用 Redis 数字
        String stockKey  = "seckill:stock:"  + activityId;
        String boughtKey = "seckill:bought:" + activityId;
        if (Boolean.FALSE.equals(stringRedisTemplate.hasKey(stockKey))) {
            stringRedisTemplate.opsForValue().set(stockKey, activity.getStock().toString());
        }

        // ─── 第 3 步: ⭐ 调 Lua 原子脚本 ───
        // execute(脚本, KEYS list, ARGV...) — KEYS 是 List, ARGV 是可变参
        Long result = stringRedisTemplate.execute(
                seckillStockScript,
                Arrays.asList(stockKey, boughtKey),
                userId.toString()    // ARGV[1]
        );

        // ─── 第 4 步: 看 Lua 返回值分支 ───
        if (result == null)  throw new BusinessException(500, "系统错误");
        if (result == -2L)   throw new BusinessException(400, "活动未开始或未预热");
        if (result == -1L)   throw new BusinessException(400, "您已参与过此次秒杀");
        if (result == 0L)    throw new BusinessException(400, "已售罄");

        if (result == 1L) {
            // ─── 第 5 步: 抢到了, 发 MQ 异步生成订单 ───
            // 消息体 "activityId:userId", Listener 拆开后查 DB 写 seckill_order
            String msg = activityId + ":" + userId;
            rabbitTemplate.convertAndSend(
                    RabbitMQConfig.SECKILL_EXCHANGE,
                    RabbitMQConfig.SECKILL_ROUTING_KEY,
                    msg
            );
            return "抢购成功, 订单生成中, 请稍后查询";
        }
        throw new BusinessException(500, "未知错误");
    }

    // ════════════════════════════════════════════════════════════
    // ④ 查"我"的秒杀结果 (前端轮询用, 3 态)
    // ════════════════════════════════════════════════════════════
    @Override
    public Map<String, Object> querySeckillResult(Long userId, Long activityId) {
        Map<String, Object> result = new HashMap<>();

        // ─── 态 1: DB 已生成订单 → SUCCESS ───
        QueryWrapper<SeckillOrder> w = new QueryWrapper<>();
        w.eq("user_id", userId).eq("seckill_activity_id", activityId);
        SeckillOrder order = seckillOrderMapper.selectOne(w);
        if (order != null) {
            result.put("status", "SUCCESS");
            result.put("orderNo", order.getOrderNo());
            result.put("message", "下单成功, 请尽快支付");
            return result;
        }

        // ─── 态 2: DB 没有 → 查 Redis bought 集合 ───
        // 表示 Lua 已经抢到但 MQ 消费者还没把 DB 落地
        String boughtKey = "seckill:bought:" + activityId;
        Boolean isMember = stringRedisTemplate.opsForSet().isMember(boughtKey, userId.toString());
        if (Boolean.TRUE.equals(isMember)) {
            result.put("status", "PROCESSING");
            result.put("orderNo", null);
            result.put("message", "订单生成中, 请稍后再查");
            return result;
        }

        // ─── 态 3: 都没有 → 没抢到 ───
        result.put("status", "NOT_FOUND");
        result.put("orderNo", null);
        result.put("message", "未抢到, 请下次再来");
        return result;
    }
    @Override
    @Transactional(rollbackFor = Exception.class)
    public void paySeckillOrder(Long userId, String orderNo) {
        QueryWrapper<SeckillOrder> w = new QueryWrapper<>();
        w.eq("order_no", orderNo).eq("user_id", userId);
        SeckillOrder order = seckillOrderMapper.selectOne(w);

        if (order == null) {
            throw new BusinessException(404, "订单不存在");
        }
        if (order.getStatus() != 0) {
            throw new BusinessException(400, "订单状态异常，无法支付");
        }

        order.setStatus((byte) 1);
        order.setPayTime(LocalDateTime.now());
        seckillOrderMapper.updateById(order);
    }

    /** 状态码翻译成中文 */
    private String statusDesc(Byte status) {
        if (status == null) return "未知";
        switch (status) {
            case 0:  return "待开始";
            case 1:  return "进行中";
            case 2:  return "已结束";
            default: return "未知";
        }
    }
}
