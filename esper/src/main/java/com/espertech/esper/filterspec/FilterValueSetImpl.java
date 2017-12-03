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
import java.util.Arrays;

/**
 * Container for filter values for use by the filter service to filter and distribute incoming events.
 */
public class FilterValueSetImpl implements FilterValueSet {
    private final EventType eventType;
    private final FilterValueSetParam[][] parameters;

    /**
     * Ctor.
     *
     * @param eventType  - type of event to filter for
     * @param parameters - list of filter parameters
     */
    public FilterValueSetImpl(EventType eventType, FilterValueSetParam[][] parameters) {
        this.eventType = eventType;
        this.parameters = parameters;
    }

    /**
     * Returns event type to filter for.
     *
     * @return event type to filter for
     */
    public EventType getEventType() {
        return eventType;
    }

    /**
     * Returns list of filter parameters.
     *
     * @return list of filter parameters
     */
    public FilterValueSetParam[][] getParameters() {
        return parameters;
    }

    public String toString() {
        return "FilterValueSetImpl{" +
                "eventType=" + eventType.getName() +
                ", parameters=" + Arrays.toString(parameters) +
                '}';
    }

    public void appendTo(StringWriter writer) {
        writer.append(eventType.getName());
        writer.append("(");
        String delimiter = "";
        for (FilterValueSetParam[] param : parameters) {
            writer.append(delimiter);
            appendTo(writer, param);
            delimiter = " or ";
        }
        writer.append(")");
    }

    private void appendTo(StringWriter writer, FilterValueSetParam[] parameters) {
        String delimiter = "";
        for (FilterValueSetParam param : parameters) {
            writer.append(delimiter);
            param.appendTo(writer);
            delimiter = ",";
        }
    }
}
