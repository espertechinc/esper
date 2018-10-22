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
package com.espertech.esper.common.internal.epl.ontrigger;

import com.espertech.esper.common.client.EventBean;
import com.espertech.esper.common.internal.collection.OneEventCollection;
import com.espertech.esper.common.internal.context.util.AgentInstanceContext;
import com.espertech.esper.common.internal.epl.expression.core.ExprEvaluator;
import com.espertech.esper.common.internal.epl.expression.core.ExprEvaluatorContext;
import com.espertech.esper.common.internal.epl.table.core.TableInstance;

public abstract class InfraOnMergeAction {

    private final ExprEvaluator optionalFilter;

    protected InfraOnMergeAction(ExprEvaluator optionalFilter) {
        this.optionalFilter = optionalFilter;
    }

    public boolean isApplies(EventBean[] eventsPerStream, ExprEvaluatorContext context) {
        if (optionalFilter == null) {
            return true;
        }
        Object result = optionalFilter.evaluate(eventsPerStream, true, context);
        return result != null && (Boolean) result;
    }

    public abstract void apply(EventBean matchingEvent,
                               EventBean[] eventsPerStream,
                               OneEventCollection newData,
                               OneEventCollection oldData,
                               AgentInstanceContext agentInstanceContext);

    public abstract void apply(EventBean matchingEvent,
                               EventBean[] eventsPerStream,
                               TableInstance tableStateInstance,
                               OnExprViewTableChangeHandler changeHandlerAdded,
                               OnExprViewTableChangeHandler changeHandlerRemoved,
                               AgentInstanceContext agentInstanceContext);

    public abstract String getName();
}
