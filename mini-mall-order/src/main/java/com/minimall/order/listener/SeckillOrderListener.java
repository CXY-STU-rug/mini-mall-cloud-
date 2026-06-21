package com.minimall.order.listener;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.minimall.order.config.RabbitMQConfig;
import com.minimall.order.entity.SeckillActivity;
import com.minimall.order.entity.SeckillOrder;
import com.minimall.order.mapper.SeckillOrderMapper;
import com.minimall.order.service.ISeckillActivityService;
import com.rabbitmq.client.Channel;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Random;

/**
 * 秒杀订单异步生成消费者 (G3.8 - 真业务版, 从单体搬, 替代 MqTestListener.onSeckillMessage)
 *
 * ════════════════════════════════════════════════════════════════
 * 触发链路 (G3.8 章节):
 *
 *   SeckillActivityServiceImpl.seckill()
 *           │ Lua 抢到 → rabbitTemplate.convertAndSend
 *           │ 消息体: "activityId:userId" 字符串
 *           ▼
 *   ┌─ seckill.queue ─┐
 *   └────────┬────────┘
 *            │ @RabbitListener
 *            ▼
 *   🎧 SeckillOrderListener.onSeckillMessage("1:5")
 *            │
 *            ▼
 *   ① 解析: activityId=1, userId=5
 *   ② 幂等查 seckill_order: 已下过 → ACK 跳过
 *   ③ 没下过 → 回查 activity 拿 productId / seckillPrice
 *   ④ 构造 SeckillOrder + INSERT
 *   ⑤ ACK
 *
 * 跟 OrderCloseListener 设计差异:
 *   ① 消息体: 字符串 "a:b" 而不是 Long (要传 2 个 ID)
 *   ② 幂等检查: 查 DB COUNT(*) 而不是看 status (因为这里还没有订单, 是要创建)
 *   ③ 失败处理: NACK 不 requeue (相同结果)
 * ════════════════════════════════════════════════════════════════
 */
@Component
public class SeckillOrderListener {

    private static final Logger log = LoggerFactory.getLogger(SeckillOrderListener.class);

    @Autowired private SeckillOrderMapper seckillOrderMapper;
    @Autowired private ISeckillActivityService seckillActivityService;

    @RabbitListener(queues = RabbitMQConfig.SECKILL_QUEUE)
    public void onSeckillMessage(String msg, Message message, Channel channel) throws IOException {
        long deliveryTag = message.getMessageProperties().getDeliveryTag();
        log.info("[MQ-Seckill] 收到秒杀消息 msg={}", msg);

        try {
            // ─── 第 1 步: 解析消息 ───
            // 消息体格式 "activityId:userId" → split + Long.valueOf
            String[] parts = msg.split(":");
            Long activityId = Long.valueOf(parts[0]);
            Long userId     = Long.valueOf(parts[1]);

            // ─── 第 2 步: 幂等检查 (防 MQ 重复投递导致同一用户多条订单) ───
            QueryWrapper<SeckillOrder> w = new QueryWrapper<>();
            w.eq("user_id", userId).eq("seckill_activity_id", activityId);
            Long count = seckillOrderMapper.selectCount(w);
            if (count > 0) {
                log.warn("[MQ-Seckill] 重复消息跳过 userId={} activityId={}", userId, activityId);
                channel.basicAck(deliveryTag, false);
                return;
            }

            // ─── 第 3 步: 回查活动拿 productId + seckillPrice ───
            SeckillActivity activity = seckillActivityService.getById(activityId);
            if (activity == null) {
                log.error("[MQ-Seckill] 活动不存在 activityId={}", activityId);
                channel.basicNack(deliveryTag, false, false);
                return;
            }

            // ─── 第 4 步: 生成订单号 + 入库 ───
            // 订单号格式同普通 Orders: yyyyMMddHHmmss + userId + 4 位随机
            String orderNo = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMddHHmmss"))
                    + userId + String.format("%04d", new Random().nextInt(10000));

            SeckillOrder order = new SeckillOrder();
            order.setOrderNo(orderNo);
            order.setUserId(userId);
            order.setSeckillActivityId(activityId);
            order.setProductId(activity.getProductId());
            order.setSeckillPrice(activity.getSeckillPrice());
            order.setStatus((byte) 0);   // 0 = 待支付
            seckillOrderMapper.insert(order);
            log.info("[MQ-Seckill] 秒杀订单生成 orderNo={} userId={}", orderNo, userId);

            // ─── 第 5 步: ACK ───
            channel.basicAck(deliveryTag, false);

        } catch (Exception e) {
            log.error("[MQ-Seckill] 处理失败 msg={}", msg, e);
            channel.basicNack(deliveryTag, false, false);
        }
    }
}
