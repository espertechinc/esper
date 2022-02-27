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
import com.espertech.esper.common.client.type.EPTypeClass;
import com.espertech.esper.common.internal.event.arr.ObjectArrayEventType;
import com.espertech.esper.common.internal.event.avro.AvroSchemaEventType;
import com.espertech.esper.common.internal.event.bean.core.BeanEventType;
import com.espertech.esper.common.internal.event.json.core.JsonEventType;
import com.espertech.esper.common.internal.event.map.MapEventType;
import com.espertech.esper.common.internal.event.xml.BaseXMLEventType;
import org.w3c.dom.Node;

import java.util.Map;

public interface EventBeanTypedEventFactory {
    EPTypeClass EPTYPE = new EPTypeClass(EventBeanTypedEventFactory.class);

    String ADAPTERFORTYPEDMAP = "adapterForTypedMap";
    String ADAPTERFORTYPEDOBJECTARRAY = "adapterForTypedObjectArray";
    String ADAPTERFORTYPEDBEAN = "adapterForTypedBean";
    String ADAPTERFORTYPEDDOM = "adapterForTypedDOM";
    String ADAPTERFORTYPEDAVRO = "adapterForTypedAvro";
    String ADAPTERFORTYPEDWRAPPER = "adapterForTypedWrapper";
    String ADAPTERFORTYPEDJSON = "adapterForTypedJson";

    MappedEventBean adapterForTypedMap(Map<String, Object> value, EventType eventType);

    ObjectArrayBackedEventBean adapterForTypedObjectArray(Object[] value, EventType eventType);

    EventBean adapterForTypedBean(Object value, EventType eventType);

    EventBean adapterForTypedDOM(Node value, EventType eventType);

    EventBean adapterForTypedAvro(Object avroGenericDataDotRecord, EventType eventType);

    EventBean adapterForTypedWrapper(EventBean decoratedUnderlying, Map<String, Object> map, EventType wrapperEventType);

    EventBean adapterForTypedJson(Object underlying, EventType eventType);

    default EventBean adapterForGivenType(Object value, EventType eventType) {
        if (eventType instanceof BeanEventType) {
            return adapterForTypedBean(value, eventType);
        } else if (eventType instanceof MapEventType) {
            return adapterForTypedMap((Map) value, eventType);
        } else if (eventType instanceof ObjectArrayEventType) {
            return adapterForTypedObjectArray((Object[]) value, eventType);
        } else if (eventType instanceof JsonEventType) {
            return adapterForTypedJson(value, eventType);
        } else if (eventType instanceof AvroSchemaEventType) {
            return adapterForTypedAvro(value, eventType);
        } else if (eventType instanceof BaseXMLEventType) {
            return adapterForTypedDOM((Node) value, eventType);
        } else if (eventType instanceof WrapperEventType) {
            throw new UnsupportedOperationException("EventBean allocation for wrapper event types without the decorated event type is not supported");
        }
        throw new UnsupportedOperationException("Event type " + eventType.getName() + " of type " + eventType.getClass().getSimpleName() + " is not a recognized type");
    }
}
