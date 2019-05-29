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

import java.util.Map;

public interface EventTypeResolvingBeanFactory {
    EventBean adapterForObjectArray(Object[] theEvent, String eventTypeName) throws EPException;

    EventBean adapterForBean(Object data, String eventTypeName);

    EventBean adapterForMap(Map<String, Object> map, String eventTypeName);

    EventBean adapterForXMLDOM(org.w3c.dom.Node node, String eventTypeName);

    EventBean adapterForAvro(Object avroGenericDataDotRecord, String eventTypeName);

    EventBean adapterForJson(String json, String eventTypeName);
}
