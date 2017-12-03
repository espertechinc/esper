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
import com.espertech.esper.filterspec.FilterSpecParamFilterForEvalDouble;
import com.espertech.esper.filterspec.MatchedEventMap;

public class FilterForEvalContextPropDouble implements FilterSpecParamFilterForEvalDouble {

    private static final long serialVersionUID = 7421157470374398746L;
    private transient final EventPropertyGetter getter;
    private final String propertyName;

    public FilterForEvalContextPropDouble(EventPropertyGetter getter, String propertyName) {
        this.getter = getter;
        this.propertyName = propertyName;
    }

    public Double getFilterValue(MatchedEventMap matchedEvents, ExprEvaluatorContext exprEvaluatorContext) {
        if (exprEvaluatorContext.getContextProperties() == null) {
            return null;
        }
        Object object = getter.get(exprEvaluatorContext.getContextProperties());
        if (object == null) {
            return null;
        }
        Number value = (Number) object;
        return value.doubleValue();
    }

    public Double getFilterValueDouble(MatchedEventMap matchedEvents, ExprEvaluatorContext exprEvaluatorContext) {
        return getFilterValue(matchedEvents, exprEvaluatorContext);
    }

    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        FilterForEvalContextPropDouble that = (FilterForEvalContextPropDouble) o;

        return propertyName.equals(that.propertyName);
    }

    public int hashCode() {
        return propertyName.hashCode();
    }
}
