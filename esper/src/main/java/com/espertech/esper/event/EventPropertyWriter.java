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
 * Writer for a single property value to an event.
 */
public interface EventPropertyWriter {
    /**
     * Value to write to a property.
     *
     * @param value  value to write
     * @param target property to write to
     */
    public void write(Object value, EventBean target);
}
