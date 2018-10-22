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

/**
 * Copy method for underlying events.
 */
public class WrapperEventBeanUndCopyMethod implements EventBeanCopyMethod {
    private final WrapperEventType wrapperEventType;
    private final EventBeanTypedEventFactory eventAdapterService;
    private final EventBeanCopyMethod underlyingCopyMethod;

    /**
     * Ctor.
     *
     * @param wrapperEventType     wrapper type
     * @param eventAdapterService  for creating events
     * @param underlyingCopyMethod for copying the underlying event
     */
    public WrapperEventBeanUndCopyMethod(WrapperEventType wrapperEventType, EventBeanTypedEventFactory eventAdapterService, EventBeanCopyMethod underlyingCopyMethod) {
        this.wrapperEventType = wrapperEventType;
        this.eventAdapterService = eventAdapterService;
        this.underlyingCopyMethod = underlyingCopyMethod;
    }

    public EventBean copy(EventBean theEvent) {
        DecoratingEventBean decorated = (DecoratingEventBean) theEvent;
        EventBean decoratedUnderlying = decorated.getUnderlyingEvent();
        EventBean copiedUnderlying = underlyingCopyMethod.copy(decoratedUnderlying);
        if (copiedUnderlying == null) {
            return null;
        }
        return eventAdapterService.adapterForTypedWrapper(copiedUnderlying, decorated.getDecoratingProperties(), wrapperEventType);
    }
}
