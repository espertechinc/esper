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
 * Handler for split-stream evaluating the first where-clause matching select-clause.
 */
public class RouteResultViewHandlerFirst extends RouteResultViewHandlerBase {
    public RouteResultViewHandlerFirst(EPStatementHandle epStatementHandle, InternalEventRouter internalEventRouter, TableInstance[] tableInstances, OnSplitItemEval[] items, ResultSetProcessor[] processors, AgentInstanceContext agentInstanceContext) {
        super(epStatementHandle, internalEventRouter, tableInstances, items, processors, agentInstanceContext);
    }

    public boolean handle(EventBean theEvent, ExprEvaluatorContext exprEvaluatorContext) {
        InstrumentationCommon instrumentationCommon = agentInstanceContext.getInstrumentationProvider();
        instrumentationCommon.qSplitStream(false, theEvent, items.length);

        int index = -1;

        for (int i = 0; i < items.length; i++) {
            OnSplitItemEval item = items[i];
            eventsPerStream[0] = theEvent;

            // handle no contained-event evaluation
            if (item.getPropertyEvaluator() == null) {
                boolean pass = checkWhereClauseCurrentEvent(i, exprEvaluatorContext);
                if (pass) {
                    index = i;
                    break;
                }
            } else {
                // need to get contained events first
                EventBean[] containeds = items[i].getPropertyEvaluator().getProperty(eventsPerStream[0], exprEvaluatorContext);
                if (containeds == null || containeds.length == 0) {
                    continue;
                }

                for (EventBean contained : containeds) {
                    eventsPerStream[0] = contained;
                    boolean pass = checkWhereClauseCurrentEvent(i, exprEvaluatorContext);
                    if (pass) {
                        index = i;
                        break;
                    }
                }

                if (index != -1) {
                    break;
                }
            }
        }

        if (index != -1) {
            mayRouteCurrentEvent(index, exprEvaluatorContext);
        }
        boolean handled = index != -1;
        instrumentationCommon.aSplitStream(false, handled);
        return handled;
    }
}
