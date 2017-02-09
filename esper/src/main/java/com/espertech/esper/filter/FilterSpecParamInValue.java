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
 * Denotes a value for use by the in-keyword within a list of values
 */
public interface FilterSpecParamInValue extends MetaDefItem, Serializable {
    /**
     * Returns the actual value to filter for from prior matching events
     *
     * @param matchedEvents    is a map of matching events
     * @param evaluatorContext eval context
     * @return filter-for value
     */
    public Object getFilterValue(MatchedEventMap matchedEvents, ExprEvaluatorContext evaluatorContext);

    public Class getReturnType();

    public boolean constant();
}
