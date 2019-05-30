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

public class ObjectToAMQPCollectorContext {
    private final AMQPEmitter emitter;
    private final EventType eventType;
    private Object object;

    public ObjectToAMQPCollectorContext(AMQPEmitter emitter, EventType eventType, Object object) {
        this.emitter = emitter;
        this.eventType = eventType;
        this.object = object;
    }

    public Object getObject() {
        return object;
    }

    public void setObject(Object object) {
        this.object = object;
    }

    public AMQPEmitter getEmitter() {
        return emitter;
    }

    public EventType getEventType() {
        return eventType;
    }
}
