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
import com.espertech.esper.common.internal.event.arr.ObjectArrayEventBean;
import com.espertech.esper.common.internal.event.bean.core.BeanEventBean;
import com.espertech.esper.common.internal.event.json.core.JsonEventBean;
import com.espertech.esper.common.internal.event.map.MapEventBean;
import com.espertech.esper.common.internal.event.xml.XMLEventBean;
import org.w3c.dom.Node;

import java.util.Map;

public class EventBeanTypedEventFactoryCompileTime implements EventBeanTypedEventFactory {
    public final static EventBeanTypedEventFactoryCompileTime INSTANCE = new EventBeanTypedEventFactoryCompileTime();

    private EventBeanTypedEventFactoryCompileTime() {
    }

    public MappedEventBean adapterForTypedMap(Map<String, Object> value, EventType eventType) {
        return new MapEventBean(value, eventType);
    }

    public ObjectArrayBackedEventBean adapterForTypedObjectArray(Object[] value, EventType eventType) {
        return new ObjectArrayEventBean(value, eventType);
    }

    public EventBean adapterForTypedBean(Object value, EventType eventType) {
        return new BeanEventBean(value, eventType);
    }

    public EventBean adapterForTypedDOM(Node value, EventType eventType) {
        return new XMLEventBean(value, eventType);
    }

    public EventBean adapterForTypedJson(Object underlying, EventType eventType) {
        return new JsonEventBean(underlying, eventType);
    }

    public EventBean adapterForTypedAvro(Object avroGenericDataDotRecord, EventType eventType) {
        throw getUnsupported();
    }

    public EventBean adapterForTypedWrapper(EventBean decoratedUnderlying, Map<String, Object> map, EventType wrapperEventType) {
        return new WrapperEventBean(decoratedUnderlying, map, wrapperEventType);
    }

    private IllegalStateException getUnsupported() {
        return new IllegalStateException("Event bean generation not supported at compile time");
    }
}
