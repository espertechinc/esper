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
package com.espertech.esper.supportregression.event;

import com.espertech.esper.client.EventType;
import com.espertech.esper.core.support.SupportEventAdapterService;

import java.util.Map;

public class SupportEventTypeFactory {
    public static EventType createBeanType(Class clazz, String name) {
        return SupportEventAdapterService.getService().addBeanType(name, clazz, false, false, false);
    }

    public static EventType createBeanType(Class clazz) {
        return SupportEventAdapterService.getService().addBeanType(clazz.getName(), clazz, false, false, false);
    }

    public static EventType createMapType(Map<String, Object> map) {
        return SupportEventAdapterService.getService().createAnonymousMapType("test", map, true);
    }
}
