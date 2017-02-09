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

import com.espertech.esper.core.context.util.AgentInstanceContext;
import com.espertech.esper.pattern.MatchedEventMap;
import com.espertech.esper.util.MetaDefItem;

import java.io.Serializable;
import java.util.Collection;

/**
 * This class represents one filter parameter in an {@link FilterSpecCompiled} filter specification.
 * <p> Each filerting parameter has an attribute name and operator type.
 */
public abstract class FilterSpecParam implements MetaDefItem, Serializable {
    public final static FilterSpecParam[] EMPTY_PARAM_ARRAY = new FilterSpecParam[0];

    /**
     * The property name of the filter parameter.
     */
    protected final FilterSpecLookupable lookupable;

    private final FilterOperator filterOperator;
    private static final long serialVersionUID = -677137265660114030L;

    FilterSpecParam(FilterSpecLookupable lookupable, FilterOperator filterOperator) {
        this.lookupable = lookupable;
        this.filterOperator = filterOperator;
    }

    /**
     * Return the filter parameter constant to filter for.
     *
     * @param matchedEvents        is the prior results that can be used to determine filter parameters
     * @param agentInstanceContext context
     * @return filter parameter constant's value
     */
    public abstract Object getFilterValue(MatchedEventMap matchedEvents, AgentInstanceContext agentInstanceContext);

    public FilterSpecLookupable getLookupable() {
        return lookupable;
    }

    /**
     * Returns the filter operator type.
     *
     * @return filter operator type
     */
    public FilterOperator getFilterOperator() {
        return filterOperator;
    }


    public String toString() {
        return "FilterSpecParam" +
                " lookupable=" + lookupable +
                " filterOp=" + filterOperator;
    }

    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }

        if (!(obj instanceof FilterSpecParam)) {
            return false;
        }

        FilterSpecParam other = (FilterSpecParam) obj;
        if (!(this.lookupable.equals(other.lookupable))) {
            return false;
        }
        if (this.filterOperator != other.filterOperator) {
            return false;
        }
        return true;
    }

    public int hashCode() {
        int result;
        result = lookupable.hashCode();
        result = 31 * result + filterOperator.hashCode();
        return result;
    }

    public static FilterSpecParam[] toArray(Collection<FilterSpecParam> coll) {
        if (coll.isEmpty()) {
            return EMPTY_PARAM_ARRAY;
        }
        return coll.toArray(new FilterSpecParam[coll.size()]);
    }
}
