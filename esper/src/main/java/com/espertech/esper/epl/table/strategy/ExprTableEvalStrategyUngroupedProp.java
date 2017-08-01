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

public class ExprTableEvalStrategyUngroupedProp extends ExprTableEvalStrategyUngroupedBase implements ExprTableAccessEvalStrategy {

    private final int propertyIndex;
    private final ExprEnumerationGivenEvent optionalEnumEval;

    public ExprTableEvalStrategyUngroupedProp(TableAndLockProviderUngrouped provider, int propertyIndex, ExprEnumerationGivenEvent optionalEnumEval) {
        super(provider);
        this.propertyIndex = propertyIndex;
        this.optionalEnumEval = optionalEnumEval;
    }

    public Object evaluate(EventBean[] eventsPerStream, boolean isNewData, ExprEvaluatorContext context) {
        ObjectArrayBackedEventBean event = lockTableReadAndGet(context);
        if (event == null) {
            return null;
        }
        return event.getProperties()[propertyIndex];
    }

    public Collection<EventBean> evaluateGetROCollectionEvents(EventBean[] eventsPerStream, boolean isNewData, ExprEvaluatorContext context) {
        ObjectArrayBackedEventBean event = lockTableReadAndGet(context);
        if (event == null) {
            return null;
        }
        return optionalEnumEval.evaluateEventGetROCollectionEvents(event, context);
    }

    public EventBean evaluateGetEventBean(EventBean[] eventsPerStream, boolean isNewData, ExprEvaluatorContext context) {
        ObjectArrayBackedEventBean event = lockTableReadAndGet(context);
        if (event == null) {
            return null;
        }
        return optionalEnumEval.evaluateEventGetEventBean(event, context);
    }

    public Collection evaluateGetROCollectionScalar(EventBean[] eventsPerStream, boolean isNewData, ExprEvaluatorContext context) {
        ObjectArrayBackedEventBean event = lockTableReadAndGet(context);
        if (event == null) {
            return null;
        }
        return optionalEnumEval.evaluateEventGetROCollectionScalar(event, context);
    }

    public Object[] evaluateTypableSingle(EventBean[] eventsPerStream, boolean isNewData, ExprEvaluatorContext context) {
        return null;
    }
}
