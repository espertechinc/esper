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

import com.espertech.esper.epl.expression.core.ExprEvaluatorContext;
import com.espertech.esper.filterspec.FilterSpecParamFilterForEvalDouble;
import com.espertech.esper.filterspec.MatchedEventMap;

/**
 * A Double-typed value as a filter parameter representing a range.
 */
public class FilterForEvalConstantDouble implements FilterSpecParamFilterForEvalDouble {
    private final double doubleValue;
    private static final long serialVersionUID = -7724314003290299382L;

    /**
     * Ctor.
     *
     * @param doubleValue is the value of the range endpoint
     */
    public FilterForEvalConstantDouble(double doubleValue) {
        this.doubleValue = doubleValue;
    }

    public final Double getFilterValue(MatchedEventMap matchedEvents, ExprEvaluatorContext exprEvaluatorContext) {
        return doubleValue;
    }

    public Double getFilterValueDouble(MatchedEventMap matchedEvents, ExprEvaluatorContext exprEvaluatorContext) {
        return doubleValue;
    }

    /**
     * Returns the constant value.
     *
     * @return constant
     */
    public double getDoubleValue() {
        return doubleValue;
    }

    public final String toString() {
        return Double.toString(doubleValue);
    }

    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }

        if (!(obj instanceof FilterForEvalConstantDouble)) {
            return false;
        }

        FilterForEvalConstantDouble other = (FilterForEvalConstantDouble) obj;
        return other.doubleValue == this.doubleValue;
    }

    public int hashCode() {
        long temp = doubleValue != +0.0d ? Double.doubleToLongBits(doubleValue) : 0L;
        return (int) (temp ^ (temp >>> 32));
    }
}
