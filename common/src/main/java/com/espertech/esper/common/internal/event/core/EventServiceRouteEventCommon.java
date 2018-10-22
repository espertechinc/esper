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

import org.w3c.dom.Node;

import java.util.Map;

public interface EventServiceRouteEventCommon {
    void routeEventObjectArray(Object[] event, String eventTypeName);

    void routeEventBean(Object event, String eventTypeName);

    void routeEventMap(Map<String, Object> event, String eventTypeName);

    void routeEventXMLDOM(Node node, String eventTypeName);

    void routeEventAvro(Object avroGenericDataDotRecord, String eventTypeName);
}
