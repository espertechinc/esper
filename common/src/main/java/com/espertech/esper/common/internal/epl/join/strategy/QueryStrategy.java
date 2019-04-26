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
package com.espertech.esper.common.internal.epl.join.strategy;

import com.espertech.esper.common.client.EventBean;
import com.espertech.esper.common.internal.collection.MultiKeyArrayOfKeys;
import com.espertech.esper.common.internal.epl.expression.core.ExprEvaluatorContext;

import java.util.Set;

/**
 * Encapsulates the strategy use to resolve the events for a stream into a tuples of events in a join.
 */
public interface QueryStrategy {
    /**
     * Look up events returning tuples of joined events.
     *
     * @param lookupEvents         - events to use to perform the join
     * @param joinSet              - result join tuples of events
     * @param exprEvaluatorContext expression evaluation context
     */
    public void lookup(EventBean[] lookupEvents, Set<MultiKeyArrayOfKeys<EventBean>> joinSet, ExprEvaluatorContext exprEvaluatorContext);
}
