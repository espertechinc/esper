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
import com.espertech.esper.common.internal.metrics.instrumentation.InstrumentationCommon;

import java.util.List;

public class InfraOnMergeMatch {
    private ExprEvaluator optionalCond;
    private List<InfraOnMergeAction> actions;

    public InfraOnMergeMatch(ExprEvaluator optionalCond, List<InfraOnMergeAction> actions) {
        this.optionalCond = optionalCond;
        this.actions = actions;
    }

    public boolean isApplies(EventBean[] eventsPerStream, ExprEvaluatorContext context) {
        if (optionalCond == null) {
            return true;
        }

        Object result = optionalCond.evaluate(eventsPerStream, true, context);
        return result != null && (Boolean) result;
    }

    public void applyNamedWindow(EventBean matchingEvent, EventBean[] eventsPerStream, OneEventCollection newData, OneEventCollection oldData, AgentInstanceContext agentInstanceContext) {
        InstrumentationCommon instrumentationCommon = agentInstanceContext.getInstrumentationProvider();
        instrumentationCommon.qInfraMergeWhenThenActions(actions.size());

        int count = -1;
        for (InfraOnMergeAction action : actions) {
            count++;
            instrumentationCommon.qInfraMergeWhenThenActionItem(count, action.getName());

            boolean applies = action.isApplies(eventsPerStream, agentInstanceContext);
            if (applies) {
                action.apply(matchingEvent, eventsPerStream, newData, oldData, agentInstanceContext);
            }

            instrumentationCommon.aInfraMergeWhenThenActionItem(applies);
        }

        instrumentationCommon.aInfraMergeWhenThenActions();
    }

    public void applyTable(EventBean matchingEvent,
                           EventBean[] eventsPerStream,
                           TableInstance stateInstance,
                           OnExprViewTableChangeHandler changeHandlerAdded,
                           OnExprViewTableChangeHandler changeHandlerRemoved,
                           AgentInstanceContext agentInstanceContext) {
        InstrumentationCommon instrumentationCommon = agentInstanceContext.getInstrumentationProvider();
        instrumentationCommon.qInfraMergeWhenThenActions(actions.size());

        int count = -1;
        for (InfraOnMergeAction action : actions) {
            count++;
            instrumentationCommon.qInfraMergeWhenThenActionItem(count, action.getName());

            boolean applies = action.isApplies(eventsPerStream, agentInstanceContext);
            if (applies) {
                action.apply(matchingEvent, eventsPerStream, stateInstance, changeHandlerAdded, changeHandlerRemoved, agentInstanceContext);
            }

            instrumentationCommon.aInfraMergeWhenThenActionItem(applies);
        }

        instrumentationCommon.aInfraMergeWhenThenActions();
    }
}