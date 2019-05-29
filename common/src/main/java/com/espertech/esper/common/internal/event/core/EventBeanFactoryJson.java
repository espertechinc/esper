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
import com.espertech.esper.common.client.EventBeanFactory;
import com.espertech.esper.common.internal.event.json.core.JsonEventType;

public class EventBeanFactoryJson implements EventBeanFactory {
    private final JsonEventType type;
    private final EventBeanTypedEventFactory factory;

    public EventBeanFactoryJson(JsonEventType type, EventBeanTypedEventFactory factory) {
        this.type = type;
        this.factory = factory;
    }

    public EventBean wrap(Object underlying) {
        Object und = type.parse((String) underlying);
        return factory.adapterForTypedJson(und, type);
    }

    public Class getUnderlyingType() {
        return type.getUnderlyingType();
    }
}
