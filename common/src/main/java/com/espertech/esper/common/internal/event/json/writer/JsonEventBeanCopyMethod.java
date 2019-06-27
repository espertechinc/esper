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
package com.espertech.esper.common.internal.event.json.writer;

import com.espertech.esper.common.client.EventBean;
import com.espertech.esper.common.internal.event.core.EventBeanCopyMethod;
import com.espertech.esper.common.internal.event.core.EventBeanTypedEventFactory;
import com.espertech.esper.common.internal.event.json.core.JsonEventType;

/**
 * Copy method for Json-underlying events.
 */
public class JsonEventBeanCopyMethod implements EventBeanCopyMethod {
    private final JsonEventType eventType;
    private final EventBeanTypedEventFactory eventBeanTypedEventFactory;

    public JsonEventBeanCopyMethod(JsonEventType eventType, EventBeanTypedEventFactory eventBeanTypedEventFactory) {
        this.eventType = eventType;
        this.eventBeanTypedEventFactory = eventBeanTypedEventFactory;
    }

    public EventBean copy(EventBean theEvent) {
        Object source = theEvent.getUnderlying();
        Object copy = eventType.getDelegateFactory().copy(source);
        return eventBeanTypedEventFactory.adapterForTypedJson(copy, eventType);
    }
}
