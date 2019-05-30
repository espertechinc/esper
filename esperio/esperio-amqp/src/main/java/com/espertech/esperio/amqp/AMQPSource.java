/*
 ***************************************************************************************
 *  Copyright (C) 2006 EsperTech, Inc. All rights reserved.                            *
 *  http://www.espertech.com/esper                                                     *
 *  http://www.espertech.com                                                           *
 *  ---------------------------------------------------------------------------------- *
 *  The software in this package is published under the terms of the GPL license       *
 *  a copy of which has been included with this distribution in the license.txt file.  *
 ***************************************************************************************
 */
package com.espertech.esperio.amqp;

import com.espertech.esper.common.client.EPException;
import com.espertech.esper.common.client.EventType;
import com.espertech.esper.common.client.dataflow.annotations.DataFlowContext;
import com.espertech.esper.common.internal.epl.dataflow.interfaces.DataFlowOpCloseContext;
import com.espertech.esper.common.internal.epl.dataflow.interfaces.DataFlowOpOpenContext;
import com.espertech.esper.common.internal.epl.dataflow.interfaces.DataFlowSourceOperator;
import com.espertech.esper.common.internal.epl.dataflow.interfaces.EPDataFlowEmitter;
import com.rabbitmq.client.AMQP;
import com.rabbitmq.client.Channel;
import com.rabbitmq.client.Connection;
import com.rabbitmq.client.ConnectionFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.concurrent.TimeoutException;

public class AMQPSource implements DataFlowSourceOperator {

    private static final Logger log = LoggerFactory.getLogger(AMQPSource.class);

    private final AMQPSettingsSourceValues settings;
    private final EventType outputEventType;

    private transient Connection connection;
    private transient Channel channel;
    private transient QueueingConsumer consumer;
    private transient String consumerTag;

    @DataFlowContext
    protected EPDataFlowEmitter graphContext;

    private ThreadLocal<AMQPToObjectCollectorContext> collectorDataTL = new ThreadLocal<AMQPToObjectCollectorContext>() {
        protected synchronized AMQPToObjectCollectorContext initialValue() {
            return null;
        }
    };

    public AMQPSource(AMQPSettingsSourceValues settings, EventType outputEventType) {
        this.settings = settings;
        this.outputEventType = outputEventType;
    }

    public void open(DataFlowOpOpenContext openContext) {
        log.info("Opening AMQP, settings are: " + settings.toString());

        try {
            final ConnectionFactory connectionFactory = new ConnectionFactory();
            connectionFactory.setHost(settings.getHost());
            if (settings.getPort() > -1) {
                connectionFactory.setPort(settings.getPort());
            }
            if (settings.getUsername() != null) {
                connectionFactory.setUsername(settings.getUsername());
            }
            if (settings.getPassword() != null) {
                connectionFactory.setPassword(settings.getPassword());
            }
            if (settings.getVhost() != null) {
                connectionFactory.setVirtualHost(settings.getVhost());
            }

            try {
                connection = connectionFactory.newConnection();
            } catch (TimeoutException e) {
                throw new EPException("Time-out getting a connection: " + e.getMessage(), e);
            }
            channel = connection.createChannel();

            channel.basicQos(settings.getPrefetchCount());
            if (settings.getExchange() != null) {
                channel.exchangeDeclarePassive(settings.getExchange());
            }

            final AMQP.Queue.DeclareOk queue;
            if (settings.getQueueName() == null || settings.getQueueName().trim().length() == 0) {
                queue = channel.queueDeclare();
            } else {
                // java.lang.String queue,boolean durable,boolean exclusive,boolean autoDelete,java.util.Map<java.lang.String,java.lang.Object> arguments) throws java.io.IOException
                queue = channel.queueDeclare(settings.getQueueName(), settings.isDeclareDurable(), settings.isDeclareExclusive(), settings.isDeclareAutoDelete(), settings.getDeclareAdditionalArgs());
            }
            if (settings.getExchange() != null && settings.getRoutingKey() != null) {
                channel.queueBind(queue.getQueue(), settings.getExchange(), settings.getRoutingKey());
            }

            final String queueName = queue.getQueue();
            log.info("AMQP consuming queue " + queueName + (settings.isLogMessages() ? " with logging" : ""));

            consumer = new QueueingConsumer(channel);
            consumerTag = channel.basicConsume(queueName, settings.isConsumeAutoAck(), consumer);
        } catch (IOException e) {
            String message = "AMQP source setup failed: " + e.getMessage();
            log.error(message, e);
            throw new EPException(message, e);
        }
    }

    public void next() throws InterruptedException {
        if (consumer == null) {
            log.warn("Consumer not started");
        } else {
            final QueueingConsumer.Delivery msg = consumer.nextDelivery(settings.getWaitMSecNextMsg());
            if (msg == null) {
                if (settings.isLogMessages() && log.isDebugEnabled()) {
                    log.debug("No message received");
                }
                return;
            }
            final byte[] bytes = msg.getBody();

            if (settings.isLogMessages() && log.isDebugEnabled()) {
                log.debug("Received " + bytes.length + " bytes, to be processed by " + settings.getCollector());
            }

            AMQPToObjectCollectorContext holder = collectorDataTL.get();
            if (holder == null) {
                holder = new AMQPToObjectCollectorContext(graphContext, bytes, msg, outputEventType);
                collectorDataTL.set(holder);
            } else {
                holder.setBytes(bytes);
                holder.setDelivery(msg);
            }

            settings.getCollector().collect(holder);
        }
    }

    public void close(DataFlowOpCloseContext openContext) {
        try {
            if (channel != null) {
                if (consumerTag != null) {
                    channel.basicCancel(consumerTag);
                }

                try {
                    channel.close();
                } catch (TimeoutException e) {
                    log.info("TIme-out closing channel: " + e.getMessage(), e);
                }
            }
        } catch (IOException e) {
            log.warn("Error closing AMQP channel", e);
        }

        try {
            if (connection != null) {
                connection.close();
            }
        } catch (IOException e) {
            log.warn("Error closing AMQP connection", e);
        }
    }
}
