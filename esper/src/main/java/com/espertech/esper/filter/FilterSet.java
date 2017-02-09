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

import java.io.StringWriter;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Holder object for a set of filters for one or more statements.
 */
public class FilterSet {
    private List<FilterSetEntry> filters;

    /**
     * Ctor.
     *
     * @param filters set of filters
     */
    public FilterSet(List<FilterSetEntry> filters) {
        this.filters = filters;
    }

    /**
     * Returns the filters.
     *
     * @return filters
     */
    public List<FilterSetEntry> getFilters() {
        return filters;
    }

    public String toString() {
        List<String> filterTexts = new ArrayList<String>();
        for (FilterSetEntry entry : filters) {
            StringWriter writer = new StringWriter();
            entry.appendTo(writer);
            filterTexts.add(writer.toString());
        }
        Collections.sort(filterTexts);

        StringWriter writer = new StringWriter();
        String delimiter = "";
        for (String filterText : filterTexts) {
            writer.append(delimiter);
            writer.append(filterText);
            delimiter = ",";
        }
        return writer.toString();
    }
}
