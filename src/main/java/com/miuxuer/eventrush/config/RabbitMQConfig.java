package com.miuxuer.eventrush.config;

import org.springframework.amqp.core.Binding;
import org.springframework.amqp.core.BindingBuilder;
import org.springframework.amqp.core.DirectExchange;
import org.springframework.amqp.core.Queue;
import org.springframework.amqp.core.QueueBuilder;
import org.springframework.amqp.support.converter.JacksonJsonMessageConverter;
import org.springframework.amqp.support.converter.MessageConverter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * RabbitMQ 配置：抢票下单消息的交换机、队列、绑定关系，以及死信队列。
 *
 * <p>整体结构：</p>
 * <pre>
 *   生产者 ──▶ ticket.order.exchange ──(routingKey: ticket.order)──▶ ticket.order.queue
 *                                                                        │
 *                                                                  消费者手动 ACK
 *                                                                        │
 *                                    重试超过 MAX_RETRY 次 ──▶ ticket.order.dlx ──▶ ticket.order.dlq
 * </pre>
 *
 * <p>队列声明成 durable（持久化），配合消费者的手动 ACK，RabbitMQ 重启或应用
 * 崩溃时消息都不会丢，这一点和原来的 Redis Stream 手动 ACK 的语义是对齐的。</p>
 *
 * @author miuxuer
 * @since 2026-10-12
 */
@Configuration
public class RabbitMQConfig {

    /** 下单消息的直连交换机 */
    public static final String TICKET_ORDER_EXCHANGE = "ticket.order.exchange";

    /** 下单消息队列 */
    public static final String TICKET_ORDER_QUEUE = "ticket.order.queue";

    /** 下单消息的路由键 */
    public static final String TICKET_ORDER_ROUTING_KEY = "ticket.order";

    /** 死信交换机 */
    public static final String TICKET_ORDER_DLX = "ticket.order.dlx";

    /** 死信队列：重试次数耗尽的消息落到这里，留待人工排查 */
    public static final String TICKET_ORDER_DLQ = "ticket.order.dlq";

    /** 死信路由键 */
    public static final String TICKET_ORDER_DLQ_ROUTING_KEY = "ticket.order.dlq";

    /** 消费失败后最多重试几次，超过就进死信队列 */
    public static final int MAX_RETRY = 3;

    /** 消息头：记录这条消息已经重试过几次 */
    public static final String RETRY_HEADER = "x-retry-count";

    /**
     * 下单消息交换机。
     * durable=true 表示交换机持久化，RabbitMQ 重启后不会丢。
     */
    @Bean
    public DirectExchange ticketOrderExchange() {
        return new DirectExchange(TICKET_ORDER_EXCHANGE, true, false);
    }

    /**
     * 下单消息队列。
     * 绑定死信交换机，消费端 nack 且不重新入队时，消息会被转发到死信队列。
     */
    @Bean
    public Queue ticketOrderQueue() {
        return QueueBuilder.durable(TICKET_ORDER_QUEUE)
                .deadLetterExchange(TICKET_ORDER_DLX)
                .deadLetterRoutingKey(TICKET_ORDER_DLQ_ROUTING_KEY)
                .build();
    }

    /**
     * 把下单队列绑定到直连交换机上。
     */
    @Bean
    public Binding ticketOrderBinding() {
        return BindingBuilder.bind(ticketOrderQueue())
                .to(ticketOrderExchange())
                .with(TICKET_ORDER_ROUTING_KEY);
    }

    /**
     * 死信交换机。
     */
    @Bean
    public DirectExchange ticketOrderDlx() {
        return new DirectExchange(TICKET_ORDER_DLX, true, false);
    }

    /**
     * 死信队列，不设消费者容错逻辑，只做兜底存放。
     */
    @Bean
    public Queue ticketOrderDlq() {
        return QueueBuilder.durable(TICKET_ORDER_DLQ).build();
    }

    /**
     * 把死信队列绑定到死信交换机上。
     */
    @Bean
    public Binding ticketOrderDlqBinding() {
        return BindingBuilder.bind(ticketOrderDlq())
                .to(ticketOrderDlx())
                .with(TICKET_ORDER_DLQ_ROUTING_KEY);
    }

    /**
     * 消息体用 JSON 序列化。
     *
     * <p>两个坑：</p>
     * <ol>
     *   <li>Spring Boot 4 默认只带 Jackson 3（tools.jackson），所以这里必须用
     *       {@link JacksonJsonMessageConverter}。用老的 {@code Jackson2JsonMessageConverter}
     *       会在运行时找不到 {@code com.fasterxml.jackson.databind.ObjectMapper} 直接报错。</li>
     *   <li>转换器默认只信任 {@code java.util} 和 {@code java.lang} 两个包，
     *       消费端反序列化到实体类时会抛「not in the trusted packages」。
     *       这里把实体所在的包追加进去（默认的两个不受影响）。</li>
     * </ol>
     *
     * @return JSON 消息转换器
     */
    @Bean
    public MessageConverter jacksonMessageConverter() {
        JacksonJsonMessageConverter converter = new JacksonJsonMessageConverter();
        converter.getJavaTypeMapper().addTrustedPackages("com.miuxuer.eventrush.entity");
        return converter;
    }
}
