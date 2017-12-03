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
import com.espertech.esper.filterspec.FilterSpecParamFilterForEval;
import com.espertech.esper.filterspec.MatchedEventMap;

/**
 * A String-typed value as a filter parameter representing a range.
 */
public class FilterForEvalConstantString implements FilterSpecParamFilterForEval {
    private static final long serialVersionUID = -2813440284912349247L;

    private final String theStringValue;

    /**
     * Ctor.
     *
     * @param theStringValue is the value of the range endpoint
     */
    public FilterForEvalConstantString(String theStringValue) {
        this.theStringValue = theStringValue;
    }

    public final String getFilterValue(MatchedEventMap matchedEvents, ExprEvaluatorContext exprEvaluatorContext) {
        return theStringValue;
    }

    public final String toString() {
        return theStringValue;
    }

    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        FilterForEvalConstantString that = (FilterForEvalConstantString) o;

        if (theStringValue != null ? !theStringValue.equals(that.theStringValue) : that.theStringValue != null)
            return false;

        return true;
    }

    public int hashCode() {
        return theStringValue != null ? theStringValue.hashCode() : 0;
    }
}
