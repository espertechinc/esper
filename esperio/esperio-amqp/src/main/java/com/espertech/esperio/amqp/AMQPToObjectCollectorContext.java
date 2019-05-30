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

import com.espertech.esper.common.client.EventType;
import com.espertech.esper.common.internal.epl.dataflow.interfaces.EPDataFlowEmitter;

public class AMQPToObjectCollectorContext {
    private final EPDataFlowEmitter emitter;
    private byte[] bytes;
    private QueueingConsumer.Delivery delivery;
    private EventType outputEventType;

    public AMQPToObjectCollectorContext(EPDataFlowEmitter emitter, byte[] bytes, QueueingConsumer.Delivery delivery, EventType outputEventType) {
        this.emitter = emitter;
        this.bytes = bytes;
        this.delivery = delivery;
        this.outputEventType = outputEventType;
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

    public EventType getOutputEventType() {
        return outputEventType;
    }
}
