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
package com.espertech.esper.runtime.internal.filtersvcimpl;

import com.espertech.esper.common.client.EventType;
import com.espertech.esper.common.internal.filterspec.FilterValueSetParam;
import com.espertech.esper.common.internal.filtersvc.FilterHandle;
import com.espertech.esper.common.internal.filtersvc.FilterService;

import java.io.StringWriter;

/**
 * Entry to a {@link FilterSet} filter set taken from a {@link FilterService}.
 */
public class FilterSetEntry {
    private FilterHandle handle;
    private EventType eventType;
    private FilterValueSetParam[][] valueSet;

    public FilterSetEntry(FilterHandle handle, EventType eventType, FilterValueSetParam[][] valueSet) {
        this.handle = handle;
        this.eventType = eventType;
        this.valueSet = valueSet;
    }

    /**
     * Returns the handle.
     *
     * @return handle
     */
    public FilterHandle getHandle() {
        return handle;
    }

    public EventType getEventType() {
        return eventType;
    }

    public FilterValueSetParam[][] getValueSet() {
        return valueSet;
    }

    public void appendTo(StringWriter writer) {
        writer.append(eventType.getName());
        writer.append("(");
        String delimiter = "";
        for (FilterValueSetParam[] param : valueSet) {
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