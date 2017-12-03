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

import com.espertech.esper.epl.core.engineimport.EngineImportService;
import com.espertech.esper.epl.expression.core.ExprEvaluatorContext;
import com.espertech.esper.epl.expression.core.ExprFilterSpecLookupable;

import java.lang.annotation.Annotation;

/**
 * This class represents a range filter parameter in an {@link FilterSpecCompiled} filter specification.
 */
public final class FilterSpecParamRange extends FilterSpecParam {
    private final FilterSpecParamFilterForEval min;
    private final FilterSpecParamFilterForEval max;
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
    public FilterSpecParamRange(ExprFilterSpecLookupable lookupable, FilterOperator filterOperator, FilterSpecParamFilterForEval min, FilterSpecParamFilterForEval max)
            throws IllegalArgumentException {
        super(lookupable, filterOperator);
        this.min = min;
        this.max = max;

        if (!(filterOperator.isRangeOperator()) && (!(filterOperator.isInvertedRangeOperator()))) {
            throw new IllegalArgumentException("Illegal filter operator " + filterOperator + " supplied to " +
                    "range filter parameter");
        }
    }

    public final Object getFilterValue(MatchedEventMap matchedEvents, ExprEvaluatorContext exprEvaluatorContext, EngineImportService engineImportService, Annotation[] annotations) {
        if (lookupable.getReturnType() == String.class) {
            return new StringRange((String) min.getFilterValue(matchedEvents, exprEvaluatorContext), (String) max.getFilterValue(matchedEvents, exprEvaluatorContext));
        }
        Double begin = (Double) min.getFilterValue(matchedEvents, exprEvaluatorContext);
        Double end = (Double) max.getFilterValue(matchedEvents, exprEvaluatorContext);
        return new DoubleRange(begin, end);
    }

    /**
     * Returns the lower endpoint.
     *
     * @return lower endpoint
     */
    public FilterSpecParamFilterForEval getMin() {
        return min;
    }

    /**
     * Returns the upper endpoint.
     *
     * @return upper endpoint
     */
    public FilterSpecParamFilterForEval getMax() {
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

        return this.min.equals(other.min) &&
                (this.max.equals(other.max));
    }

    public int hashCode() {
        int result = super.hashCode();
        result = 31 * result + (min != null ? min.hashCode() : 0);
        result = 31 * result + (max != null ? max.hashCode() : 0);
        return result;
    }
}
