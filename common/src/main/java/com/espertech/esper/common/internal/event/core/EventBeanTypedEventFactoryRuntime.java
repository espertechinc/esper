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
import com.espertech.esper.common.internal.event.avro.EventTypeAvroHandler;
import com.espertech.esper.common.internal.event.bean.core.BeanEventBean;
import com.espertech.esper.common.internal.event.json.core.JsonEventBean;
import com.espertech.esper.common.internal.event.map.MapEventBean;
import com.espertech.esper.common.internal.event.xml.XMLEventBean;
import com.espertech.esper.common.internal.util.CollectionUtil;
import org.w3c.dom.Node;

import java.util.HashMap;
import java.util.Map;

public class EventBeanTypedEventFactoryRuntime implements EventBeanTypedEventFactory {
    private final EventTypeAvroHandler eventTypeAvroHandler;

    public EventBeanTypedEventFactoryRuntime(EventTypeAvroHandler eventTypeAvroHandler) {
        this.eventTypeAvroHandler = eventTypeAvroHandler;
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

    public EventBean adapterForTypedAvro(Object avroGenericDataDotRecord, EventType eventType) {
        return eventTypeAvroHandler.adapterForTypeAvro(avroGenericDataDotRecord, eventType);
    }

    public EventBean adapterForTypedWrapper(EventBean decoratedUnderlying, Map<String, Object> map, EventType wrapperEventType) {
        if (decoratedUnderlying instanceof DecoratingEventBean) {
            DecoratingEventBean wrapper = (DecoratingEventBean) decoratedUnderlying;
            if (!wrapper.getDecoratingProperties().isEmpty()) {
                if (map.isEmpty()) {
                    map = new HashMap<>(CollectionUtil.capacityHashMap(wrapper.getDecoratingProperties().size()));
                }
                map.putAll(wrapper.getDecoratingProperties());
            }
            return new WrapperEventBean(wrapper.getUnderlyingEvent(), map, wrapperEventType);
        } else {
            return new WrapperEventBean(decoratedUnderlying, map, wrapperEventType);
        }
    }

    public EventBean adapterForTypedJson(Object underlying, EventType eventType) {
        return new JsonEventBean(underlying, eventType);
    }
}
