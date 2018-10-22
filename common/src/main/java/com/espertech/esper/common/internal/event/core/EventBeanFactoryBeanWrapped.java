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

import java.util.Collections;

public class EventBeanFactoryBeanWrapped implements EventBeanFactory {

    private final EventType beanEventType;
    private final EventType wrapperEventType;
    private final EventBeanTypedEventFactory eventBeanTypedEventFactory;

    public EventBeanFactoryBeanWrapped(EventType beanEventType, EventType wrapperEventType, EventBeanTypedEventFactory eventBeanTypedEventFactory) {
        this.beanEventType = beanEventType;
        this.wrapperEventType = wrapperEventType;
        this.eventBeanTypedEventFactory = eventBeanTypedEventFactory;
    }

    public EventBean wrap(Object underlying) {
        EventBean bean = eventBeanTypedEventFactory.adapterForTypedBean(underlying, beanEventType);
        return eventBeanTypedEventFactory.adapterForTypedWrapper(bean, Collections.<String, Object>emptyMap(), wrapperEventType);
    }

    public Class getUnderlyingType() {
        return beanEventType.getUnderlyingType();
    }
}
