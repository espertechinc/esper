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
package com.espertech.esper.common.internal.epl.table.strategy;

import com.espertech.esper.common.client.EventBean;
import com.espertech.esper.common.internal.epl.expression.core.ExprEvaluatorContext;
import com.espertech.esper.common.internal.event.core.ObjectArrayBackedEventBean;

import java.util.Collection;

public class ExprTableEvalStrategyGroupedTopLevel extends ExprTableEvalStrategyGroupedBase {

    public ExprTableEvalStrategyGroupedTopLevel(TableAndLockProviderGrouped provider, ExprTableEvalStrategyFactory factory) {
        super(provider, factory);
    }

    public Object evaluate(EventBean[] eventsPerStream, boolean isNewData, ExprEvaluatorContext exprEvaluatorContext) {
        ObjectArrayBackedEventBean row = getRow(eventsPerStream, isNewData, exprEvaluatorContext);
        if (row == null) {
            return null;
        }
        return ExprTableEvalStrategyUtil.evalMap(row, ExprTableEvalStrategyUtil.getRow(row), factory.getTable().getMetaData().getColumns(), eventsPerStream, isNewData, exprEvaluatorContext);
    }

    public Object[] evaluateTypableSingle(EventBean[] eventsPerStream, boolean isNewData, ExprEvaluatorContext context) {
        ObjectArrayBackedEventBean row = getRow(eventsPerStream, isNewData, context);
        if (row == null) {
            return null;
        }
        return ExprTableEvalStrategyUtil.evalTypable(row, ExprTableEvalStrategyUtil.getRow(row), factory.getTable().getMetaData().getColumns(), eventsPerStream, isNewData, context);
    }

    public Collection<EventBean> evaluateGetROCollectionEvents(EventBean[] eventsPerStream, boolean isNewData, ExprEvaluatorContext context) {
        return null;
    }

    public EventBean evaluateGetEventBean(EventBean[] eventsPerStream, boolean isNewData, ExprEvaluatorContext context) {
        return null;
    }

    public Collection evaluateGetROCollectionScalar(EventBean[] eventsPerStream, boolean isNewData, ExprEvaluatorContext context) {
        return null;
    }
}
