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

import com.espertech.esper.util.SerializerUtil;
import com.rabbitmq.client.Channel;
import com.rabbitmq.client.Connection;
import com.rabbitmq.client.ConnectionFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.List;

public class AMQPSupportSendRunnable implements Runnable {

    private static final Logger log = LoggerFactory.getLogger(AMQPSupportSendRunnable.class);

    private final String hostName;
    private final String queueName;
    private final List<Object> events;
    private final long msecSleepTime;

    private boolean shutdown;

    public AMQPSupportSendRunnable(String hostName, String queueName, List<Object> events, long msecSleepTime) {
        this.hostName = hostName;
        this.queueName = queueName;
        this.events = new ArrayList<Object>(events);
        this.msecSleepTime = msecSleepTime;
    }

    public boolean isShutdown() {
        return shutdown;
    }

    public void setShutdown(boolean shutdown) {
        this.shutdown = shutdown;
    }

    public void run() {
        try {
            ConnectionFactory factory = new ConnectionFactory();
            factory.setHost(hostName);
            Connection connection = factory.newConnection();
            Channel channel = connection.createChannel();

            // java.lang.String queue, boolean durable, boolean exclusive, boolean autoDelete, java.util.Map<java.lang.String,java.lang.Object> arguments
            channel.queueDeclare(queueName, false, false, true, null);

            log.info("Start publishing messages: " + events.size() + " messages");

            int count = 0;
            while (true) {
                if (events.isEmpty()) {
                    break;
                }

                Object next = events.remove(0);
                byte[] bytes = SerializerUtil.objectToByteArr(next);
                channel.basicPublish("", queueName, null, bytes);
                count++;

                log.info("Publishing message #" + count + ": " + next);
                Thread.sleep(msecSleepTime);

                if (isShutdown()) {
                    break;
                }
            }

            log.info("Completed publishing messages: " + count + " messages");
        } catch (Exception ex) {
            log.error("Error attaching to AMQP: " + ex.getMessage(), ex);
        }
    }
}
