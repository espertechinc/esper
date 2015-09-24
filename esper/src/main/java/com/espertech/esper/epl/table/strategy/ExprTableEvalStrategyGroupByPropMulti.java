/*
 * *************************************************************************************
 *  Copyright (C) 2006-2015 EsperTech, Inc. All rights reserved.                       *
 *  http://www.espertech.com/esper                                                     *
 *  http://www.espertech.com                                                           *
 *  ---------------------------------------------------------------------------------- *
 *  The software in this package is published under the terms of the GPL license       *
 *  a copy of which has been included with this distribution in the license.txt file.  *
 * *************************************************************************************
 */

package com.espertech.esper.epl.table.strategy;

import com.espertech.esper.client.EventBean;
import com.espertech.esper.epl.expression.core.ExprEvaluator;
import com.espertech.esper.epl.expression.core.ExprEvaluatorContext;
import com.espertech.esper.epl.expression.core.ExprEvaluatorEnumerationGivenEvent;
import com.espertech.esper.epl.table.mgmt.TableStateInstanceGrouped;

import java.util.Collection;
import java.util.concurrent.locks.Lock;

public class ExprTableEvalStrategyGroupByPropMulti extends ExprTableEvalStrategyGroupByPropBase {

    private final ExprEvaluator[] groupExpr;

    public ExprTableEvalStrategyGroupByPropMulti(Lock lock, TableStateInstanceGrouped grouped, int propertyIndex, ExprEvaluatorEnumerationGivenEvent optionalEnumEval, ExprEvaluator[] groupExpr) {
        super(lock, grouped, propertyIndex, optionalEnumEval);
        this.groupExpr = groupExpr;
    }

    public Object evaluate(EventBean[] eventsPerStream, boolean isNewData, ExprEvaluatorContext exprEvaluatorContext) {
        Object groupKey = ExprTableEvalStrategyGroupByAccessMulti.getKey(groupExpr, eventsPerStream, isNewData, exprEvaluatorContext);
        return evaluateInternal(groupKey, exprEvaluatorContext);
    }

    public Collection<EventBean> evaluateGetROCollectionEvents(EventBean[] eventsPerStream, boolean isNewData, ExprEvaluatorContext context) {
        Object groupKey = ExprTableEvalStrategyGroupByAccessMulti.getKey(groupExpr, eventsPerStream, isNewData, context);
        return evaluateGetROCollectionEventsInternal(groupKey, context);
    }

    public EventBean evaluateGetEventBean(EventBean[] eventsPerStream, boolean isNewData, ExprEvaluatorContext context) {
        Object groupKey = ExprTableEvalStrategyGroupByAccessMulti.getKey(groupExpr, eventsPerStream, isNewData, context);
        return evaluateGetEventBeanInternal(groupKey, context);
    }

    public Collection evaluateGetROCollectionScalar(EventBean[] eventsPerStream, boolean isNewData, ExprEvaluatorContext context) {
        Object groupKey = ExprTableEvalStrategyGroupByAccessMulti.getKey(groupExpr, eventsPerStream, isNewData, context);
        return evaluateGetROCollectionScalarInternal(groupKey, context);
    }
}
