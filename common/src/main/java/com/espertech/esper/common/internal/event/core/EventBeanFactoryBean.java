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
import com.espertech.esper.common.client.EventType;

public class EventBeanFactoryBean implements EventBeanFactory {

    private final EventType type;
    private final EventBeanTypedEventFactory eventBeanTypedEventFactory;

    public EventBeanFactoryBean(EventType type, EventBeanTypedEventFactory eventBeanTypedEventFactory) {
        this.type = type;
        this.eventBeanTypedEventFactory = eventBeanTypedEventFactory;
    }

    public EventBean wrap(Object underlying) {
        return eventBeanTypedEventFactory.adapterForTypedBean(underlying, type);
    }

    public Class getUnderlyingType() {
        return type.getUnderlyingType();
    }
}
