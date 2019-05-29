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

public interface EventBeanTypedEventFactory {
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
}
