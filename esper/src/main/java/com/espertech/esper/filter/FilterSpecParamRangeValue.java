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
import com.espertech.esper.pattern.MatchedEventMap;
import com.espertech.esper.util.MetaDefItem;

import java.io.Serializable;

/**
 * Interface for range-type filter parameters for type checking and to obtain the filter values for endpoints based
 * on prior results.
 */
public interface FilterSpecParamRangeValue extends MetaDefItem, Serializable {
    /**
     * Returns the filter value representing the endpoint.
     *
     * @param matchedEvents        is the prior results
     * @param exprEvaluatorContext eval context
     * @return filter value
     */
    public Object getFilterValue(MatchedEventMap matchedEvents, ExprEvaluatorContext exprEvaluatorContext);
}
