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

public class EventTypeIndexBuilderValueIndexesPair implements FilterServiceEntry {
    private final FilterValueSet filterValueSet;
    private final EventTypeIndexBuilderIndexLookupablePair[][] indexPairs;

    public EventTypeIndexBuilderValueIndexesPair(FilterValueSet filterValueSet, EventTypeIndexBuilderIndexLookupablePair[][] indexPairs) {
        this.filterValueSet = filterValueSet;
        this.indexPairs = indexPairs;
    }

    public FilterValueSet getFilterValueSet() {
        return filterValueSet;
    }

    public EventTypeIndexBuilderIndexLookupablePair[][] getIndexPairs() {
        return indexPairs;
    }
}
