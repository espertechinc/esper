/**************************************************************************************
 * Copyright (C) 2006-2015 EsperTech Inc. All rights reserved.                        *
 * http://www.espertech.com/esper                                                          *
 * http://www.espertech.com                                                           *
 * ---------------------------------------------------------------------------------- *
 * The software in this package is published under the terms of the GPL license       *
 * a copy of which has been included with this distribution in the license.txt file.  *
 **************************************************************************************/
package com.espertech.esper.epl.lookup;

import com.espertech.esper.client.EventBean;
import com.espertech.esper.epl.expression.core.ExprEvaluatorContext;

import java.util.HashSet;
import java.util.Set;

/**
 * Implementation for a table lookup strategy that returns exactly one row
 * but leaves that row as an undefined value.
 */
public class SubordTableLookupStrategyNullRow implements SubordTableLookupStrategy
{
    private static Set<EventBean> singleNullRowEventSet = new HashSet<EventBean>();

    static
    {
        singleNullRowEventSet.add(null);
    }

    public Set<EventBean> lookup(EventBean[] events, ExprEvaluatorContext context) {
        return singleNullRowEventSet;
    }

    public String toQueryPlan() {
        return this.getClass().getSimpleName();
    }

    public LookupStrategyDesc getStrategyDesc() {
        return new LookupStrategyDesc(LookupStrategyType.NULLROWS, null);
    }
}
