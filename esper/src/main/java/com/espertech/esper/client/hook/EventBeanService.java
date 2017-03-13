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
package com.espertech.esper.client.hook;

import com.espertech.esper.client.EventBean;
import com.espertech.esper.client.EventType;
import com.espertech.esper.event.EventAdapterException;
import org.w3c.dom.Node;

import java.util.Map;

public interface EventBeanService {
    EventType getExistsTypeByName(String eventTypeName);

    EventBean adapterForType(Object theEvent, EventType eventType);

    EventBean adapterForBean(Object theEvent);
    EventBean adapterForTypedBean(Object bean, EventType eventType);

    EventBean adapterForAvro(Object avroGenericDataDotRecord, String eventTypeName);
    EventBean adapterForTypedAvro(Object avroGenericDataDotRecord, EventType eventType);

    EventBean adapterForMap(Map<String, Object> theEvent, String eventTypeName) throws EventAdapterException;
    EventBean adapterForTypedMap(Map<String, Object> properties, EventType eventType);

    EventBean adapterForObjectArray(Object[] theEvent, String eventTypeName) throws EventAdapterException;
    EventBean adapterForTypedObjectArray(Object[] props, EventType resultEventType);

    EventBean adapterForDOM(Node node);
    EventBean adapterForTypedDOM(Node node, EventType eventType);
}
