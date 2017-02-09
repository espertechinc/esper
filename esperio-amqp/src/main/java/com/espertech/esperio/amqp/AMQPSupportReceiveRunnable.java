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

import com.rabbitmq.client.Channel;
import com.rabbitmq.client.Connection;
import com.rabbitmq.client.ConnectionFactory;
import com.rabbitmq.client.QueueingConsumer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class AMQPSupportReceiveRunnable implements Runnable {

    private static final Logger log = LoggerFactory.getLogger(AMQPSupportReceiveRunnable.class);

    private final String hostName;
    private final String queueName;
    private final long waitMSecNextMsg;
    private final AMQPSupportReceiveCallback callback;

    private boolean shutdown;

    public AMQPSupportReceiveRunnable(String hostName, String queueName, long waitMSecNextMsg, AMQPSupportReceiveCallback callback) {
        this.hostName = hostName;
        this.queueName = queueName;
        this.waitMSecNextMsg = waitMSecNextMsg;
        this.callback = callback;
    }

    public void run() {
        try {
            ConnectionFactory factory = new ConnectionFactory();
            factory.setHost(hostName);
            Connection connection = factory.newConnection();
            Channel channel = connection.createChannel();

            // java.lang.String queue, boolean durable, boolean exclusive, boolean autoDelete, java.util.Map<java.lang.String,java.lang.Object> arguments
            channel.queueDeclare(queueName, false, false, true, null);

            log.info("Start receiving messages");

            QueueingConsumer consumer = new QueueingConsumer(channel);
            channel.basicConsume(queueName, true, consumer);

            int count = 0;
            while (true) {
                final QueueingConsumer.Delivery msg = consumer.nextDelivery(waitMSecNextMsg);
                if (msg == null) {
                    continue;
                }
                final byte[] bytes = msg.getBody();
                callback.handleMessage(bytes);

                if (isShutdown()) {
                    break;
                }
            }

            log.info("Completed publishing messages: " + count + " messages");
        } catch (Exception ex) {
            log.error("Error attaching to AMQP: " + ex.getMessage(), ex);
        }
    }

    public boolean isShutdown() {
        return shutdown;
    }

    public void setShutdown(boolean shutdown) {
        this.shutdown = shutdown;
    }
}
