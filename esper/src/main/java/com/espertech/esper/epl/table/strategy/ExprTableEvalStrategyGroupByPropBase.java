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
import com.espertech.esper.epl.expression.core.ExprEnumerationGivenEvent;
import com.espertech.esper.epl.expression.table.ExprTableAccessEvalStrategy;
import com.espertech.esper.event.ObjectArrayBackedEventBean;

import java.util.Collection;

public abstract class ExprTableEvalStrategyGroupByPropBase extends ExprTableEvalStrategyGroupByBase implements ExprTableAccessEvalStrategy {

    private final int propertyIndex;
    private final ExprEnumerationGivenEvent optionalEnumEval;

    protected ExprTableEvalStrategyGroupByPropBase(TableAndLockProviderGrouped provider, int propertyIndex, ExprEnumerationGivenEvent optionalEnumEval) {
        super(provider);
        this.propertyIndex = propertyIndex;
        this.optionalEnumEval = optionalEnumEval;
    }

    public Object evaluateInternal(Object groupKey, ExprEvaluatorContext context) {
        ObjectArrayBackedEventBean row = lockTableReadAndGet(groupKey, context);
        if (row == null) {
            return null;
        }
        return row.getProperties()[propertyIndex];
    }

    public Collection<EventBean> evaluateGetROCollectionEventsInternal(Object groupKey, ExprEvaluatorContext context) {
        ObjectArrayBackedEventBean row = lockTableReadAndGet(groupKey, context);
        if (row == null) {
            return null;
        }
        return optionalEnumEval.evaluateEventGetROCollectionEvents(row, context);
    }

    public EventBean evaluateGetEventBeanInternal(Object groupKey, ExprEvaluatorContext context) {
        ObjectArrayBackedEventBean row = lockTableReadAndGet(groupKey, context);
        if (row == null) {
            return null;
        }
        return optionalEnumEval.evaluateEventGetEventBean(row, context);
    }

    public Collection evaluateGetROCollectionScalarInternal(Object groupKey, ExprEvaluatorContext context) {
        ObjectArrayBackedEventBean row = lockTableReadAndGet(groupKey, context);
        if (row == null) {
            return null;
        }
        return optionalEnumEval.evaluateEventGetROCollectionScalar(row, context);
    }

    public Object[] evaluateTypableSingle(EventBean[] eventsPerStream, boolean isNewData, ExprEvaluatorContext context) {
        return null;
    }
}
