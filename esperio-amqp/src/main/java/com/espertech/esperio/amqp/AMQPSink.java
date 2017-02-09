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

import com.espertech.esper.dataflow.annotations.DataFlowOpPropertyHolder;
import com.espertech.esper.dataflow.annotations.DataFlowOperator;
import com.espertech.esper.dataflow.interfaces.*;
import com.rabbitmq.client.AMQP;
import com.rabbitmq.client.AMQP.BasicProperties.Builder;
import com.rabbitmq.client.Channel;
import com.rabbitmq.client.Connection;
import com.rabbitmq.client.ConnectionFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.Map;

@DataFlowOperator
public class AMQPSink implements DataFlowOpLifecycle {
    private static final Logger log = LoggerFactory.getLogger(AMQPSink.class);

    @DataFlowOpPropertyHolder
    private AMQPSettingsSink settings;

    private transient Connection connection;
    private transient Channel channel;
    private ThreadLocal<ObjectToAMQPCollectorContext> collectorDataTL = new ThreadLocal<ObjectToAMQPCollectorContext>() {
        protected synchronized ObjectToAMQPCollectorContext initialValue() {
            return null;
        }
    };

    public DataFlowOpInitializeResult initialize(DataFlowOpInitializateContext context) throws Exception {
        return null;
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

            connection = connectionFactory.newConnection();
            channel = connection.createChannel();

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
            log.info("AMQP producing queue is " + queueName + (settings.isLogMessages() ? " with logging" : ""));
        } catch (IOException e) {
            String message = "AMQP setup failed: " + e.getMessage();
            log.error(message, e);
            throw new RuntimeException(message, e);
        }
    }

    public void onInput(Object event) {

        ObjectToAMQPCollectorContext holder = collectorDataTL.get();
        if (holder == null) {
            if (settings.getExchange() != null && settings.getRoutingKey() != null) {
                if (log.isDebugEnabled()) {
                    log.debug("Using exchange " + settings.getExchange() + " routing-key " + settings.getRoutingKey());
                }
                holder = new ObjectToAMQPCollectorContext(new AMQPEmitter() {
                    public void send(byte[] bytes) {
                        try {
                            channel.basicPublish(settings.getExchange(), settings.getRoutingKey(), null, bytes);
                        } catch (IOException e) {
                            String message = "Failed to publish to AMQP: " + e.getMessage();
                            log.error(message, e);
                            throw new RuntimeException(message, e);
                        }
                    }

                    public void send(byte[] bytes, Map<String, Object> headers) {
                        try {
                            Builder builder = new Builder();
                            channel.basicPublish(settings.getExchange(), settings.getRoutingKey(), builder.headers(headers).build(), bytes);
                        } catch (IOException e) {
                            String message = "Failed to publish to AMQP: " + e.getMessage();
                            log.error(message, e);
                            throw new RuntimeException(message, e);
                        }
                    }

                }, event);
            } else {
                if (log.isDebugEnabled()) {
                    log.debug("Using queue " + settings.getQueueName());
                }
                holder = new ObjectToAMQPCollectorContext(new AMQPEmitter() {
                    public void send(byte[] bytes) {
                        try {
                            channel.basicPublish("", settings.getQueueName(), null, bytes);
                        } catch (IOException e) {
                            String message = "Failed to publish to AMQP: " + e.getMessage();
                            log.error(message, e);
                            throw new RuntimeException(message, e);
                        }
                    }

                    public void send(byte[] bytes, Map<String, Object> headers) {
                        try {
                            Builder builder = new Builder();
                            channel.basicPublish("", settings.getQueueName(), builder.headers(headers).build(), bytes);
                        } catch (IOException e) {
                            String message = "Failed to publish to AMQP: " + e.getMessage();
                            log.error(message, e);
                            throw new RuntimeException(message, e);
                        }
                    }

                }, event);
            }
            collectorDataTL.set(holder);
        } else {
            holder.setObject(event);
        }

        settings.getCollector().collect(holder);
    }

    public void close(DataFlowOpCloseContext openContext) {
        try {
            if (channel != null) {
                channel.close();
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
