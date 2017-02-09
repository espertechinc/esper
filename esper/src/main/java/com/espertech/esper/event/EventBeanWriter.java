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
 * Interface for writing a set of event properties to an event.
 */
public interface EventBeanWriter {
    /**
     * Write property values to the event.
     *
     * @param values   to write
     * @param theEvent to write to
     */
    public void write(Object[] values, EventBean theEvent);
}
