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

import com.espertech.esper.client.EventPropertyGetter;
import com.espertech.esper.epl.expression.core.ExprEvaluatorContext;
import com.espertech.esper.filterspec.FilterSpecParamFilterForEval;
import com.espertech.esper.filterspec.MatchedEventMap;

public class FilterForEvalContextPropString implements FilterSpecParamFilterForEval {

    private static final long serialVersionUID = 5250506869921316777L;
    private transient final EventPropertyGetter getter;
    private final String propertyName;

    public FilterForEvalContextPropString(EventPropertyGetter getter, String propertyName) {
        this.getter = getter;
        this.propertyName = propertyName;
    }

    public Object getFilterValue(MatchedEventMap matchedEvents, ExprEvaluatorContext exprEvaluatorContext) {
        if (exprEvaluatorContext.getContextProperties() == null) {
            return null;
        }
        return getter.get(exprEvaluatorContext.getContextProperties());
    }

    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        FilterForEvalContextPropString that = (FilterForEvalContextPropString) o;

        return propertyName.equals(that.propertyName);
    }

    public int hashCode() {
        return propertyName.hashCode();
    }
}
