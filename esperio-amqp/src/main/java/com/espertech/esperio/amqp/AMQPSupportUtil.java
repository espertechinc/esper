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

import java.io.IOException;

public class AMQPSupportUtil {

    private static final Logger log = LoggerFactory.getLogger(AMQPSupportUtil.class);

    public static int drainQueue(String hostName, String queueName) {
        Connection connection = null;
        Channel channel = null;

        try {
            ConnectionFactory factory = new ConnectionFactory();
            factory.setHost(hostName);
            connection = factory.newConnection();
            channel = connection.createChannel();

            // java.lang.String queue, boolean durable, boolean exclusive, boolean autoDelete, java.util.Map<java.lang.String,java.lang.Object> arguments
            channel.queueDeclare(queueName, false, false, true, null);

            QueueingConsumer consumer = new QueueingConsumer(channel);
            channel.basicConsume(queueName, true, consumer);

            int count = 0;
            while (true) {
                final QueueingConsumer.Delivery msg = consumer.nextDelivery(1);
                if (msg == null) {
                    return count;
                }
            }
        } catch (Exception ex) {
            log.error("Error attaching to AMQP: " + ex.getMessage(), ex);
        } finally {
            if (channel != null) {
                try {
                    channel.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
            if (connection != null) {
                try {
                    connection.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }
        return -1;
    }
}
