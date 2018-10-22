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

import com.espertech.esper.common.client.EPException;
import com.espertech.esper.common.client.EventBean;
import com.espertech.esper.common.client.EventType;
import com.espertech.esper.common.client.hook.expr.EventBeanService;
import com.espertech.esper.common.internal.collection.PathException;
import com.espertech.esper.common.internal.collection.PathRegistry;
import com.espertech.esper.common.internal.event.eventtyperepo.EventTypeRepositoryImpl;
import org.w3c.dom.Node;

import java.util.Map;

public class EventBeanServiceImpl implements EventBeanService {
    private final EventTypeRepositoryImpl eventTypeRepositoryPreconfigured;
    private final PathRegistry<String, EventType> pathEventTypes;
    private final EventBeanTypedEventFactory typedEventFactory;

    public EventBeanServiceImpl(EventTypeRepositoryImpl eventTypeRepositoryPreconfigured, PathRegistry<String, EventType> pathEventTypes, EventBeanTypedEventFactory typedEventFactory) {
        this.eventTypeRepositoryPreconfigured = eventTypeRepositoryPreconfigured;
        this.pathEventTypes = pathEventTypes;
        this.typedEventFactory = typedEventFactory;
    }

    public EventBean adapterForMap(Map<String, Object> theEvent, String eventTypeName) {
        EventType eventType = findType(eventTypeName);
        return typedEventFactory.adapterForTypedMap(theEvent, eventType);
    }

    public EventBean adapterForBean(Object theEvent, String eventTypeName) {
        EventType eventType = findType(eventTypeName);
        return typedEventFactory.adapterForTypedBean(theEvent, eventType);
    }

    public EventBean adapterForAvro(Object avroGenericDataDotRecord, String eventTypeName) {
        EventType eventType = findType(eventTypeName);
        return typedEventFactory.adapterForTypedAvro(avroGenericDataDotRecord, eventType);
    }

    public EventBean adapterForObjectArray(Object[] theEvent, String eventTypeName) {
        EventType eventType = findType(eventTypeName);
        return typedEventFactory.adapterForTypedObjectArray(theEvent, eventType);
    }

    public EventBean adapterForDOM(Node node, String eventTypeName) {
        EventType eventType = findType(eventTypeName);
        return typedEventFactory.adapterForTypedDOM(node, eventType);
    }

    public EventType getExistsTypeByName(String eventTypeName) {
        return findTypeMayNull(eventTypeName);
    }

    public EventBean adapterForTypedBean(Object bean, EventType eventType) {
        return typedEventFactory.adapterForTypedBean(bean, eventType);
    }

    public EventBean adapterForTypedAvro(Object avroGenericDataDotRecord, EventType eventType) {
        return typedEventFactory.adapterForTypedAvro(avroGenericDataDotRecord, eventType);
    }

    public EventBean adapterForTypedMap(Map<String, Object> properties, EventType eventType) {
        return typedEventFactory.adapterForTypedMap(properties, eventType);
    }

    public EventBean adapterForTypedObjectArray(Object[] props, EventType eventType) {
        return typedEventFactory.adapterForTypedObjectArray(props, eventType);
    }

    public EventBean adapterForTypedDOM(Node node, EventType eventType) {
        return typedEventFactory.adapterForTypedDOM(node, eventType);
    }

    private EventType findType(String eventTypeName) {
        EventType eventType = findTypeMayNull(eventTypeName);
        if (eventType == null) {
            throw new EPException("Failed to find event type '" + eventTypeName + "'");
        }
        return eventType;
    }

    private EventType findTypeMayNull(String eventTypeName) {
        EventType eventType = eventTypeRepositoryPreconfigured.getTypeByName(eventTypeName);
        if (eventType != null) {
            return eventType;
        }

        try {
            eventType = pathEventTypes.getAnyModuleExpectSingle(eventTypeName, null).getFirst();
        } catch (PathException ex) {
            throw new EPException("Failed to obtain event type '" + eventTypeName + "': " + ex.getMessage(), ex);
        }
        return eventType;
    }
}
