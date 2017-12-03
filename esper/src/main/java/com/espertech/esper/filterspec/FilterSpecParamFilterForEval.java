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

import com.espertech.esper.epl.expression.core.ExprEvaluatorContext;

import java.io.Serializable;

public interface FilterSpecParamFilterForEval extends Serializable {
    /**
     * Returns the filter value representing the endpoint.
     *
     * @param matchedEvents        is the prior results
     * @param exprEvaluatorContext eval context
     * @return filter value
     */
    public Object getFilterValue(MatchedEventMap matchedEvents, ExprEvaluatorContext exprEvaluatorContext);
}
