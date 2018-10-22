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
package com.espertech.esper.common.internal.event.core;

import com.espertech.esper.common.client.EventBean;
import com.espertech.esper.common.client.EventType;
import org.w3c.dom.Node;

import java.util.Map;

public class EventBeanTypedEventFactoryCompileTime implements EventBeanTypedEventFactory {
    public final static EventBeanTypedEventFactoryCompileTime INSTANCE = new EventBeanTypedEventFactoryCompileTime();

    private EventBeanTypedEventFactoryCompileTime() {
    }

    public MappedEventBean adapterForTypedMap(Map<String, Object> value, EventType eventType) {
        throw getUnsupported();
    }

    public ObjectArrayBackedEventBean adapterForTypedObjectArray(Object[] value, EventType eventType) {
        throw getUnsupported();
    }

    public EventBean adapterForTypedBean(Object value, EventType eventType) {
        throw getUnsupported();
    }

    public EventBean adapterForTypedDOM(Node value, EventType eventType) {
        throw getUnsupported();
    }

    public EventBean adapterForTypedAvro(Object value, EventType eventType) {
        throw getUnsupported();
    }

    public EventBean adapterForTypedWrapper(EventBean decoratedUnderlying, Map<String, Object> map, EventType wrapperEventType) {
        throw getUnsupported();
    }

    private IllegalStateException getUnsupported() {
        return new IllegalStateException("Event bean generation not supported at compile time");
    }
}
