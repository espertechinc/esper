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
package com.espertech.esper.filterspec;

import com.espertech.esper.client.EventType;

import java.io.StringWriter;

/**
 * Contains the filter criteria to sift through events. The filter criteria are the event class to look for and
 * a set of parameters (property names, operators and constant/range values).
 */
public interface FilterValueSet {
    /**
     * Returns type of event to filter for.
     *
     * @return event type
     */
    public EventType getEventType();

    /**
     * Returns list of filter parameters.
     *
     * @return list of filter params
     */
    public FilterValueSetParam[][] getParameters();

    public void appendTo(StringWriter writer);
}
