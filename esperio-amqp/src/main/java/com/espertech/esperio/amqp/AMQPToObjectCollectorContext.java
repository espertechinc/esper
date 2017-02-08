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

import com.espertech.esper.dataflow.interfaces.EPDataFlowEmitter;
import com.rabbitmq.client.QueueingConsumer;

public class AMQPToObjectCollectorContext {
    private final EPDataFlowEmitter emitter;
    private byte[] bytes;
    private QueueingConsumer.Delivery delivery;

    public AMQPToObjectCollectorContext(EPDataFlowEmitter emitter, byte[] bytes, QueueingConsumer.Delivery delivery) {
        this.emitter = emitter;
        this.bytes = bytes;
        this.delivery = delivery;
    }

    public byte[] getBytes() {
        return bytes;
    }

    public void setBytes(byte[] bytes) {
        this.bytes = bytes;
    }

    public EPDataFlowEmitter getEmitter() {
        return emitter;
    }

    public QueueingConsumer.Delivery getDelivery() {
        return delivery;
    }

    public void setDelivery(QueueingConsumer.Delivery delivery) {
        this.delivery = delivery;
    }
}
