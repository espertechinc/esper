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
import com.espertech.esper.pattern.MatchedEventMap;

public class RangeValueContextProp implements FilterSpecParamRangeValue {

    private static final long serialVersionUID = -3216208345920469926L;
    private transient final EventPropertyGetter getter;

    public RangeValueContextProp(EventPropertyGetter getter) {
        this.getter = getter;
    }

    public Object getFilterValue(MatchedEventMap matchedEvents, ExprEvaluatorContext exprEvaluatorContext) {
        if (exprEvaluatorContext.getContextProperties() == null) {
            return null;
        }
        Object object = getter.get(exprEvaluatorContext.getContextProperties());
        if (object == null) {
            return null;
        }

        if (object instanceof String) {
            return object;
        }

        Number value = (Number) object;
        return value.doubleValue();

    }
}
