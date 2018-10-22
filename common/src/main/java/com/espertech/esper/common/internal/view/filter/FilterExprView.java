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
package com.espertech.esper.common.internal.view.filter;

import com.espertech.esper.common.client.EventBean;
import com.espertech.esper.common.client.EventType;
import com.espertech.esper.common.internal.epl.expression.core.ExprEvaluator;
import com.espertech.esper.common.internal.epl.expression.core.ExprEvaluatorContext;
import com.espertech.esper.common.internal.metrics.instrumentation.InstrumentationCommon;
import com.espertech.esper.common.internal.view.core.ViewSupport;

import java.util.Iterator;

/**
 * Simple filter view filtering events using a filter expression tree.
 */
public class FilterExprView extends ViewSupport {
    private final ExprEvaluator exprEvaluator;
    private final ExprEvaluatorContext exprEvaluatorContext;
    private final String whereClauseEvaluatorTextForAudit;

    /**
     * Ctor.
     *
     * @param exprEvaluator                    - Filter expression evaluation impl
     * @param exprEvaluatorContext             context for expression evalauation
     * @param whereClauseEvaluatorTextForAudit text or null if no-audit
     */
    public FilterExprView(ExprEvaluator exprEvaluator, ExprEvaluatorContext exprEvaluatorContext, String whereClauseEvaluatorTextForAudit) {
        this.exprEvaluator = exprEvaluator;
        this.exprEvaluatorContext = exprEvaluatorContext;
        this.whereClauseEvaluatorTextForAudit = whereClauseEvaluatorTextForAudit;
    }

    public EventType getEventType() {
        return parent.getEventType();
    }

    public Iterator<EventBean> iterator() {
        return new FilterExprViewIterator(parent.iterator(), exprEvaluator, exprEvaluatorContext);
    }

    public void update(EventBean[] newData, EventBean[] oldData) {
        InstrumentationCommon instrumentationCommon = exprEvaluatorContext.getInstrumentationProvider();
        instrumentationCommon.qWhereClauseFilter(whereClauseEvaluatorTextForAudit, newData, oldData);

        EventBean[] filteredNewData = filterEvents(exprEvaluator, newData, true, exprEvaluatorContext);
        EventBean[] filteredOldData = filterEvents(exprEvaluator, oldData, false, exprEvaluatorContext);

        instrumentationCommon.aWhereClauseFilter(filteredNewData, filteredOldData);

        if ((filteredNewData != null) || (filteredOldData != null)) {
            instrumentationCommon.qWhereClauseIR(filteredNewData, filteredOldData);
            child.update(filteredNewData, filteredOldData);
            instrumentationCommon.aWhereClauseIR();
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

        InstrumentationCommon instrumentationCommon = exprEvaluatorContext.getInstrumentationProvider();

        EventBean[] evalEventArr = new EventBean[1];
        boolean[] passResult = new boolean[events.length];
        int passCount = 0;

        for (int i = 0; i < events.length; i++) {
            evalEventArr[0] = events[i];
            instrumentationCommon.qWhereClauseFilterEval(i, events[i], isNewData);
            Boolean pass = (Boolean) exprEvaluator.evaluate(evalEventArr, isNewData, exprEvaluatorContext);
            instrumentationCommon.aWhereClauseFilterEval(pass);
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
