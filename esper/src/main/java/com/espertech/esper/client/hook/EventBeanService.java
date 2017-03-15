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
import org.w3c.dom.Node;

import java.util.Map;

/**
 * Services for obtaining {@link EventType} information and constructing {@link EventBean} events.
 */
public interface EventBeanService {
    /**
     * Look up an event type by name,
     * @param eventTypeName to look up
     * @return event type or null if not found
     */
    EventType getExistsTypeByName(String eventTypeName);

    /**
     * Construct an event bean for a given bean (Object, POJO) using the class of the object to determine the Bean-only event type (not for Map/Object-Array/Avro/XML events)
     * @param theEvent event underlying
     * @return event bean
     */
    EventBean adapterForBean(Object theEvent);

    /**
     * Construct an event bean for a given bean (Object, POJO) and given the Bean-event-type
     * @param bean event underlying
     * @param eventType event type (Bean only)
     * @return event bean
     */
    EventBean adapterForTypedBean(Object bean, EventType eventType);

    /**
     * Construct an event bean for a given Avro GenericData.Record using the event type name to look up the Avro event type
     * @param avroGenericDataDotRecord event underlying
     * @param eventTypeName name of the Avro event type
     * @return event bean
     */
    EventBean adapterForAvro(Object avroGenericDataDotRecord, String eventTypeName);

    /**
     * Construct an event bean for a given Avro GenericData.Record and given the Avro-event-type
     * @param avroGenericDataDotRecord event underlying
     * @param eventType event type (Avro only)
     * @return event bean
     */
    EventBean adapterForTypedAvro(Object avroGenericDataDotRecord, EventType eventType);

    /**
     * Construct an event bean for a given Map using the event type name to look up the Map event type
     * @param theEvent event underlying
     * @param eventTypeName name of the Map event type
     * @return event bean
     */
    EventBean adapterForMap(Map<String, Object> theEvent, String eventTypeName);

    /**
     * Construct an event bean for a given Map and given the Map-event-type
     * @param properties event underlying
     * @param eventType event type (Map only)
     * @return event bean
     */
    EventBean adapterForTypedMap(Map<String, Object> properties, EventType eventType);

    /**
     * Construct an event bean for a given Object-Array using the event type name to look up the Object-Array event type
     * @param theEvent event underlying
     * @param eventTypeName name of the Object-Array event type
     * @return event bean
     */
    EventBean adapterForObjectArray(Object[] theEvent, String eventTypeName);

    /**
     * Construct an event bean for a given Object-Array and given the Object-Array-event-type
     * @param props event underlying
     * @param eventType event type (Object-array only)
     * @return event bean
     */
    EventBean adapterForTypedObjectArray(Object[] props, EventType eventType);

    /**
     * Construct an event bean for a given XML-DOM using the node root node name to look up the XML-DOM event type
     * @param node event underlying
     * @return event bean
     */
    EventBean adapterForDOM(Node node);

    /**
     * Construct an event bean for a given Node and given the XML-event-type
     * @param node event underlying
     * @param eventType event type (XML only)
     * @return event bean
     */
    EventBean adapterForTypedDOM(Node node, EventType eventType);

    /**
     * Construct an event bean for a given object and event type, wherein it is assumed that the object matches the event type
     * @param theEvent event underlying
     * @param eventType event type
     * @return event bean
     */
    EventBean adapterForType(Object theEvent, EventType eventType);
}
