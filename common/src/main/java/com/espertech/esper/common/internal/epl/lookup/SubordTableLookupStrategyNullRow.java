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
package com.espertech.esper.common.internal.epl.lookup;

import com.espertech.esper.common.client.EventBean;
import com.espertech.esper.common.internal.epl.expression.core.ExprEvaluatorContext;

import java.util.HashSet;
import java.util.Set;

public class SubordTableLookupStrategyNullRow implements SubordTableLookupStrategy {
    private final static Set<EventBean> SINGLE_NULL_ROW_EVENT_SET = new HashSet<>(4);

    public final static SubordTableLookupStrategyNullRow INSTANCE = new SubordTableLookupStrategyNullRow();

    static {
        SINGLE_NULL_ROW_EVENT_SET.add(null);
    }

    private SubordTableLookupStrategyNullRow() {
    }

    public Set<EventBean> lookup(EventBean[] events, ExprEvaluatorContext context) {
        return SINGLE_NULL_ROW_EVENT_SET;
    }

    public String toQueryPlan() {
        return this.getClass().getSimpleName();
    }

    public LookupStrategyDesc getStrategyDesc() {
        return new LookupStrategyDesc(LookupStrategyType.NULLROWS);
    }
}
