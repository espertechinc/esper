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
package com.espertech.esper.common.internal.context.aifactory.ontrigger.onsplit;

import com.espertech.esper.common.client.EventBean;
import com.espertech.esper.common.internal.context.util.AgentInstanceContext;
import com.espertech.esper.common.internal.context.util.EPStatementHandle;
import com.espertech.esper.common.internal.context.util.InternalEventRouter;
import com.espertech.esper.common.internal.epl.expression.core.ExprEvaluatorContext;
import com.espertech.esper.common.internal.epl.resultset.core.ResultSetProcessor;
import com.espertech.esper.common.internal.epl.table.core.TableInstance;
import com.espertech.esper.common.internal.metrics.instrumentation.InstrumentationCommon;

/**
 * Handler for split-stream evaluating the all where-clauses and their matching select-clauses.
 */
public class RouteResultViewHandlerAll extends RouteResultViewHandlerBase {
    public RouteResultViewHandlerAll(EPStatementHandle epStatementHandle, InternalEventRouter internalEventRouter, TableInstance[] tableStateInstances, OnSplitItemEval[] items, ResultSetProcessor[] processors, AgentInstanceContext agentInstanceContext) {
        super(epStatementHandle, internalEventRouter, tableStateInstances, items, processors, agentInstanceContext);
    }

    public boolean handle(EventBean theEvent, ExprEvaluatorContext exprEvaluatorContext) {
        InstrumentationCommon instrumentationCommon = agentInstanceContext.getInstrumentationProvider();
        instrumentationCommon.qSplitStream(true, theEvent, items.length);

        boolean isHandled = false;
        for (int i = 0; i < items.length; i++) {
            OnSplitItemEval currentItem = items[i];
            eventsPerStream[0] = theEvent;

            // handle no-contained-event evaluation
            if (currentItem.getPropertyEvaluator() == null) {
                isHandled |= processAllCurrentEvent(i, exprEvaluatorContext);
            } else {
                // handle contained-event evaluation
                EventBean[] containeds = currentItem.getPropertyEvaluator().getProperty(eventsPerStream[0], exprEvaluatorContext);
                if (containeds == null || containeds.length == 0) {
                    continue;
                }

                for (EventBean contained : containeds) {
                    eventsPerStream[0] = contained;
                    isHandled |= processAllCurrentEvent(i, exprEvaluatorContext);
                }
            }
        }

        instrumentationCommon.aSplitStream(true, isHandled);
        return isHandled;
    }

    private boolean processAllCurrentEvent(int index, ExprEvaluatorContext exprEvaluatorContext) {
        boolean pass = checkWhereClauseCurrentEvent(index, exprEvaluatorContext);
        if (!pass) {
            return false;
        }
        return mayRouteCurrentEvent(index, exprEvaluatorContext);
    }
}
