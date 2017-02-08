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
package com.espertech.esper.epl.table.strategy;

import com.espertech.esper.client.EventBean;
import com.espertech.esper.epl.expression.core.ExprEvaluatorContext;
import com.espertech.esper.epl.expression.table.ExprTableAccessEvalStrategy;
import com.espertech.esper.event.ObjectArrayBackedEventBean;

import java.util.Collection;

public abstract class ExprTableEvalStrategyGroupByMethodBase extends ExprTableEvalStrategyGroupByBase implements ExprTableAccessEvalStrategy {

    private final int index;

    protected ExprTableEvalStrategyGroupByMethodBase(TableAndLockProviderGrouped provider, int index) {
        super(provider);
        this.index = index;
    }

    protected Object evaluateInternal(Object groupKey, ExprEvaluatorContext context) {
        ObjectArrayBackedEventBean row = lockTableReadAndGet(groupKey, context);
        if (row == null) {
            return null;
        }
        return ExprTableEvalStrategyUtil.evalMethodGetValue(ExprTableEvalStrategyUtil.getRow(row), index);
    }

    public Object[] evaluateTypableSingle(EventBean[] eventsPerStream, boolean isNewData, ExprEvaluatorContext context) {
        return null;
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
