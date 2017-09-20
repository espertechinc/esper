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
package com.espertech.esper.view.internal;

import com.espertech.esper.client.EventBean;
import com.espertech.esper.core.context.util.AgentInstanceContext;
import com.espertech.esper.core.service.EPStatementHandle;
import com.espertech.esper.core.service.InternalEventRouter;
import com.espertech.esper.core.start.EPStatementStartMethodOnTriggerItem;
import com.espertech.esper.epl.core.resultset.core.ResultSetProcessor;
import com.espertech.esper.epl.expression.core.ExprEvaluator;
import com.espertech.esper.epl.expression.core.ExprEvaluatorContext;
import com.espertech.esper.epl.table.mgmt.TableStateInstance;
import com.espertech.esper.metrics.instrumentation.InstrumentationHelper;

/**
 * Handler for split-stream evaluating the all where-clauses and their matching select-clauses.
 */
public class RouteResultViewHandlerAll extends RouteResultViewHandlerBase {
    public RouteResultViewHandlerAll(EPStatementHandle epStatementHandle, InternalEventRouter internalEventRouter, TableStateInstance[] tableStateInstances, EPStatementStartMethodOnTriggerItem[] items, ResultSetProcessor[] processors, ExprEvaluator[] whereClauses, AgentInstanceContext agentInstanceContext) {
        super(epStatementHandle, internalEventRouter, tableStateInstances, items, processors, whereClauses, agentInstanceContext);
    }

    public boolean handle(EventBean theEvent, ExprEvaluatorContext exprEvaluatorContext) {
        if (InstrumentationHelper.ENABLED) {
            InstrumentationHelper.get().qSplitStream(true, theEvent, whereClauses);
        }

        boolean isHandled = false;
        for (int i = 0; i < whereClauses.length; i++) {
            EPStatementStartMethodOnTriggerItem currentItem = items[i];
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

        if (InstrumentationHelper.ENABLED) {
            InstrumentationHelper.get().aSplitStream(true, isHandled);
        }
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
