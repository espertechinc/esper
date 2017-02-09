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
package com.espertech.esper.epl.join.base;

import com.espertech.esper.client.EventBean;
import com.espertech.esper.collection.MultiKey;
import com.espertech.esper.epl.expression.core.ExprEvaluatorContext;

import java.util.Set;

/**
 * Processes a join result set constisting of sets of tuples of events.
 */
public interface JoinSetProcessor {
    /**
     * Process join result set.
     *
     * @param newEvents            - set of event tuples representing new data
     * @param oldEvents            - set of event tuples representing old data
     * @param exprEvaluatorContext expression evaluation context
     */
    public void process(Set<MultiKey<EventBean>> newEvents, Set<MultiKey<EventBean>> oldEvents, ExprEvaluatorContext exprEvaluatorContext);
}
