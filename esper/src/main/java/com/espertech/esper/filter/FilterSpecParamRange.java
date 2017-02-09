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

/**
 * This class represents a range filter parameter in an {@link FilterSpecCompiled} filter specification.
 */
public final class FilterSpecParamRange extends FilterSpecParam {
    private final FilterSpecParamRangeValue min;
    private final FilterSpecParamRangeValue max;
    private static final long serialVersionUID = -3381167844631490119L;

    /**
     * Constructor.
     *
     * @param lookupable     is the lookupable
     * @param filterOperator is the type of range operator
     * @param min            is the begin point of the range
     * @param max            is the end point of the range
     * @throws IllegalArgumentException if an operator was supplied that does not take a double range value
     */
    public FilterSpecParamRange(FilterSpecLookupable lookupable, FilterOperator filterOperator, FilterSpecParamRangeValue min, FilterSpecParamRangeValue max)
            throws IllegalArgumentException {
        super(lookupable, filterOperator);
        this.min = min;
        this.max = max;

        if (!(filterOperator.isRangeOperator()) && (!(filterOperator.isInvertedRangeOperator()))) {
            throw new IllegalArgumentException("Illegal filter operator " + filterOperator + " supplied to " +
                    "range filter parameter");
        }
    }

    public final Object getFilterValue(MatchedEventMap matchedEvents, AgentInstanceContext agentInstanceContext) {
        if (lookupable.getReturnType() == String.class) {
            return new StringRange((String) min.getFilterValue(matchedEvents, agentInstanceContext), (String) max.getFilterValue(matchedEvents, agentInstanceContext));
        }
        Double begin = (Double) min.getFilterValue(matchedEvents, agentInstanceContext);
        Double end = (Double) max.getFilterValue(matchedEvents, agentInstanceContext);
        return new DoubleRange(begin, end);
    }

    /**
     * Returns the lower endpoint.
     *
     * @return lower endpoint
     */
    public FilterSpecParamRangeValue getMin() {
        return min;
    }

    /**
     * Returns the upper endpoint.
     *
     * @return upper endpoint
     */
    public FilterSpecParamRangeValue getMax() {
        return max;
    }

    public final String toString() {
        return super.toString() + "  range=(min=" + min.toString() + ",max=" + max.toString() + ')';
    }

    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }

        if (!(obj instanceof FilterSpecParamRange)) {
            return false;
        }

        FilterSpecParamRange other = (FilterSpecParamRange) obj;
        if (!super.equals(other)) {
            return false;
        }

        if (this.min.equals(other.min) &&
                (this.max.equals(other.max))) {
            return true;
        }
        return false;
    }

    public int hashCode() {
        int result = super.hashCode();
        result = 31 * result + (min != null ? min.hashCode() : 0);
        result = 31 * result + (max != null ? max.hashCode() : 0);
        return result;
    }
}
