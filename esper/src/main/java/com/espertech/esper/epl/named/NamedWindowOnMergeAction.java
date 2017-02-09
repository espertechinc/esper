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
package com.espertech.esper.epl.named;

import com.espertech.esper.client.EventBean;
import com.espertech.esper.collection.OneEventCollection;
import com.espertech.esper.epl.expression.core.ExprEvaluator;
import com.espertech.esper.epl.expression.core.ExprEvaluatorContext;

public abstract class NamedWindowOnMergeAction {

    private final ExprEvaluator optionalFilter;

    protected NamedWindowOnMergeAction(ExprEvaluator optionalFilter) {
        this.optionalFilter = optionalFilter;
    }

    public boolean isApplies(EventBean[] eventsPerStream, ExprEvaluatorContext context) {
        if (optionalFilter == null) {
            return true;
        }
        Object result = optionalFilter.evaluate(eventsPerStream, true, context);
        return result != null && (Boolean) result;
    }

    public abstract void apply(EventBean matchingEvent, EventBean[] eventsPerStream, OneEventCollection newData, OneEventCollection oldData, ExprEvaluatorContext exprEvaluatorContext);

    public abstract String getName();
}
