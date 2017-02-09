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
import com.espertech.esper.epl.expression.core.ExprEvaluator;
import com.espertech.esper.epl.expression.core.ExprEvaluatorContext;
import com.espertech.esper.metrics.instrumentation.InstrumentationHelper;

import java.util.Iterator;
import java.util.Set;

/**
 * Processes join tuple set by filtering out tuples.
 */
public class JoinSetFilter implements JoinSetProcessor {
    private ExprEvaluator filterExprNode;

    /**
     * Ctor.
     *
     * @param filterExprNode - filter tree
     */
    public JoinSetFilter(ExprEvaluator filterExprNode) {
        this.filterExprNode = filterExprNode;
    }

    public void process(Set<MultiKey<EventBean>> newEvents, Set<MultiKey<EventBean>> oldEvents, ExprEvaluatorContext exprEvaluatorContext) {
        // Filter
        if (filterExprNode != null) {
            if (InstrumentationHelper.ENABLED) {
                if ((newEvents != null && newEvents.size() > 0) || (oldEvents != null && oldEvents.size() > 0)) {
                    InstrumentationHelper.get().qJoinExecFilter();
                    filter(filterExprNode, newEvents, true, exprEvaluatorContext);
                    if (oldEvents != null) {
                        filter(filterExprNode, oldEvents, false, exprEvaluatorContext);
                    }
                    InstrumentationHelper.get().aJoinExecFilter(newEvents, oldEvents);
                }
                return;
            }

            filter(filterExprNode, newEvents, true, exprEvaluatorContext);
            if (oldEvents != null) {
                filter(filterExprNode, oldEvents, false, exprEvaluatorContext);
            }
        }
    }

    /**
     * Filter event by applying the filter nodes evaluation method.
     *
     * @param filterExprNode       - top node of the filter expression tree.
     * @param events               - set of tuples of events
     * @param isNewData            - true to indicate filter new data (istream) and not old data (rstream)
     * @param exprEvaluatorContext expression evaluation context
     */
    protected static void filter(ExprEvaluator filterExprNode, Set<MultiKey<EventBean>> events, boolean isNewData, ExprEvaluatorContext exprEvaluatorContext) {
        for (Iterator<MultiKey<EventBean>> it = events.iterator(); it.hasNext(); ) {
            MultiKey<EventBean> key = it.next();
            EventBean[] eventArr = key.getArray();

            Boolean matched = (Boolean) filterExprNode.evaluate(eventArr, isNewData, exprEvaluatorContext);
            if ((matched == null) || (!matched)) {
                it.remove();
            }
        }
    }
}
