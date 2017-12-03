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
package com.espertech.esper.filter;

import com.espertech.esper.filterspec.FilterValueSet;

import java.io.StringWriter;

/**
 * Entry to a {@link FilterSet} filter set taken from a {@link FilterService}.
 */
public class FilterSetEntry {
    private FilterHandle handle;
    private FilterValueSet filterValueSet;

    /**
     * Ctor.
     *
     * @param handle         handle
     * @param filterValueSet values
     */
    public FilterSetEntry(FilterHandle handle, FilterValueSet filterValueSet) {
        this.handle = handle;
        this.filterValueSet = filterValueSet;
    }

    /**
     * Returns the handle.
     *
     * @return handle
     */
    public FilterHandle getHandle() {
        return handle;
    }

    /**
     * Returns filters.
     *
     * @return filters
     */
    public FilterValueSet getFilterValueSet() {
        return filterValueSet;
    }

    public void appendTo(StringWriter writer) {
        filterValueSet.appendTo(writer);
    }
}