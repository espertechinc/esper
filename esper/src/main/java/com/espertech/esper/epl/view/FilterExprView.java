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
package com.espertech.esper.epl.view;

import com.espertech.esper.client.EventBean;
import com.espertech.esper.client.EventType;
import com.espertech.esper.epl.expression.core.ExprEvaluator;
import com.espertech.esper.epl.expression.core.ExprEvaluatorContext;
import com.espertech.esper.epl.expression.core.ExprNode;
import com.espertech.esper.metrics.instrumentation.InstrumentationHelper;
import com.espertech.esper.view.ViewSupport;

import java.util.Iterator;

/**
 * Simple filter view filtering events using a filter expression tree.
 */
public class FilterExprView extends ViewSupport {
    private final ExprNode exprNode;
    private final ExprEvaluator exprEvaluator;
    private final ExprEvaluatorContext exprEvaluatorContext;

    /**
     * Ctor.
     *
     * @param exprEvaluator        - Filter expression evaluation impl
     * @param exprEvaluatorContext context for expression evalauation
     * @param exprNode             node
     */
    public FilterExprView(ExprNode exprNode, ExprEvaluator exprEvaluator, ExprEvaluatorContext exprEvaluatorContext) {
        this.exprNode = exprNode;
        this.exprEvaluator = exprEvaluator;
        this.exprEvaluatorContext = exprEvaluatorContext;
    }

    public EventType getEventType() {
        return parent.getEventType();
    }

    public Iterator<EventBean> iterator() {
        return new FilterExprViewIterator(parent.iterator(), exprEvaluator, exprEvaluatorContext);
    }

    public void update(EventBean[] newData, EventBean[] oldData) {
        if (InstrumentationHelper.ENABLED) {
            InstrumentationHelper.get().qWhereClauseFilter(exprNode, newData, oldData);
        }

        EventBean[] filteredNewData = filterEvents(exprEvaluator, newData, true, exprEvaluatorContext);
        EventBean[] filteredOldData = filterEvents(exprEvaluator, oldData, false, exprEvaluatorContext);

        if (InstrumentationHelper.ENABLED) {
            InstrumentationHelper.get().aWhereClauseFilter(filteredNewData, filteredOldData);
        }

        if ((filteredNewData != null) || (filteredOldData != null)) {
            if (InstrumentationHelper.ENABLED) {
                InstrumentationHelper.get().qWhereClauseIR(filteredNewData, filteredOldData);
            }
            this.updateChildren(filteredNewData, filteredOldData);
            if (InstrumentationHelper.ENABLED) {
                InstrumentationHelper.get().aWhereClauseIR();
            }
        }
    }

    /**
     * Filters events using the supplied evaluator.
     *
     * @param exprEvaluator        - evaluator to use
     * @param events               - events to filter
     * @param isNewData            - true to indicate filter new data (istream) and not old data (rstream)
     * @param exprEvaluatorContext context for expression evalauation
     * @return filtered events, or null if no events got through the filter
     */
    private EventBean[] filterEvents(ExprEvaluator exprEvaluator, EventBean[] events, boolean isNewData, ExprEvaluatorContext exprEvaluatorContext) {
        if (events == null) {
            return null;
        }

        EventBean[] evalEventArr = new EventBean[1];
        boolean[] passResult = new boolean[events.length];
        int passCount = 0;

        for (int i = 0; i < events.length; i++) {
            evalEventArr[0] = events[i];
            if (InstrumentationHelper.ENABLED) {
                InstrumentationHelper.get().qWhereClauseFilterEval(i, events[i], isNewData);
            }
            Boolean pass = (Boolean) exprEvaluator.evaluate(evalEventArr, isNewData, exprEvaluatorContext);
            if (InstrumentationHelper.ENABLED) {
                InstrumentationHelper.get().aWhereClauseFilterEval(pass);
            }
            if ((pass != null) && pass) {
                passResult[i] = true;
                passCount++;
            }
        }

        if (passCount == 0) {
            return null;
        }
        if (passCount == events.length) {
            return events;
        }

        EventBean[] resultArray = new EventBean[passCount];
        int count = 0;
        for (int i = 0; i < passResult.length; i++) {
            if (passResult[i]) {
                resultArray[count] = events[i];
                count++;
            }
        }
        return resultArray;
    }
}
