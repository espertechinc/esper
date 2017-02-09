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
package com.espertech.esper.epl.lookup;

import com.espertech.esper.client.EventBean;
import com.espertech.esper.epl.expression.core.ExprEvaluatorContext;

import java.util.Collection;

/**
 * Strategy for looking up, in some sort of table or index, or a set of events, potentially based on the
 * events properties, and returning a set of matched events.
 */
public interface SubordTableLookupStrategy {
    /**
     * Returns matched events for a set of events to look up for. Never returns an empty result set,
     * always returns null to indicate no results.
     *
     * @param events  to look up
     * @param context context
     * @return set of matching events, or null if none matching
     */
    public Collection<EventBean> lookup(EventBean[] events, ExprEvaluatorContext context);

    public String toQueryPlan();

    public LookupStrategyDesc getStrategyDesc();
}
