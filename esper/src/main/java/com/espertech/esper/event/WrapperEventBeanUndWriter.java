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
package com.espertech.esper.event;

import com.espertech.esper.client.EventBean;

/**
 * Writer for values to a wrapper event.
 */
public class WrapperEventBeanUndWriter implements EventBeanWriter {
    private final EventBeanWriter undWriter;

    /**
     * Ctor.
     *
     * @param undWriter writer to the underlying object
     */
    public WrapperEventBeanUndWriter(EventBeanWriter undWriter) {
        this.undWriter = undWriter;
    }

    public void write(Object[] values, EventBean theEvent) {
        DecoratingEventBean wrappedEvent = (DecoratingEventBean) theEvent;
        EventBean eventWrapped = wrappedEvent.getUnderlyingEvent();
        undWriter.write(values, eventWrapped);
    }
}
