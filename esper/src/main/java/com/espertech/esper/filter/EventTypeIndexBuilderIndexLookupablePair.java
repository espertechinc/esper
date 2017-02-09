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

public class EventTypeIndexBuilderIndexLookupablePair {
    private final FilterParamIndexBase index;
    private final Object lookupable;

    public EventTypeIndexBuilderIndexLookupablePair(FilterParamIndexBase index, Object lookupable) {
        this.index = index;
        this.lookupable = lookupable;
    }

    public FilterParamIndexBase getIndex() {
        return index;
    }

    public Object getLookupable() {
        return lookupable;
    }
}
