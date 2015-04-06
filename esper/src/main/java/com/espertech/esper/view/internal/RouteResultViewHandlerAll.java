/*
 * *************************************************************************************
 *  Copyright (C) 2006-2015 EsperTech, Inc. All rights reserved.                       *
 *  http://www.espertech.com/esper                                                     *
 *  http://www.espertech.com                                                           *
 *  ---------------------------------------------------------------------------------- *
 *  The software in this package is published under the terms of the GPL license       *
 *  a copy of which has been included with this distribution in the license.txt file.  *
 * *************************************************************************************
 */

package com.espertech.esper.view.internal;

import com.espertech.esper.client.EventBean;
import com.espertech.esper.client.annotation.AuditEnum;
import com.espertech.esper.collection.UniformPair;
import com.espertech.esper.core.context.util.AgentInstanceContext;
import com.espertech.esper.core.service.EPStatementHandle;
import com.espertech.esper.core.service.InternalEventRouter;
import com.espertech.esper.epl.core.ResultSetProcessor;
import com.espertech.esper.epl.expression.core.ExprEvaluator;
import com.espertech.esper.epl.expression.core.ExprEvaluatorContext;
import com.espertech.esper.epl.table.mgmt.TableStateInstance;
import com.espertech.esper.metrics.instrumentation.InstrumentationHelper;
import com.espertech.esper.util.AuditPath;

/**
 * Handler for split-stream evaluating the all where-clauses and their matching select-clauses.
 */
public class RouteResultViewHandlerAll implements RouteResultViewHandler
{
    private final InternalEventRouter internalEventRouter;
    private final TableStateInstance[] tableStateInstances;
    private final boolean[] isNamedWindowInsert;
    private final EPStatementHandle epStatementHandle;
    private final ResultSetProcessor[] processors;
    private final ExprEvaluator[] whereClauses;
    private final EventBean[] eventsPerStream = new EventBean[1];
    private final AgentInstanceContext agentInstanceContext;
    private final boolean audit;

    /**
     * Ctor.
     * @param epStatementHandle handle
     * @param processors select clauses
     * @param whereClauses where clauses
     * @param agentInstanceContext agent instance context
     */
    public RouteResultViewHandlerAll(EPStatementHandle epStatementHandle, InternalEventRouter internalEventRouter, TableStateInstance[] tableStateInstances, boolean[] isNamedWindowInsert, ResultSetProcessor[] processors, ExprEvaluator[] whereClauses, AgentInstanceContext agentInstanceContext)
    {
        this.internalEventRouter = internalEventRouter;
        this.tableStateInstances = tableStateInstances;
        this.isNamedWindowInsert = isNamedWindowInsert;
        this.epStatementHandle = epStatementHandle;
        this.processors = processors;
        this.whereClauses = whereClauses;
        this.agentInstanceContext = agentInstanceContext;
        this.audit = AuditEnum.INSERT.getAudit(agentInstanceContext.getStatementContext().getAnnotations()) != null;
    }

    public boolean handle(EventBean theEvent, ExprEvaluatorContext exprEvaluatorContext)
    {
        if (InstrumentationHelper.ENABLED) { InstrumentationHelper.get().qSplitStream(true, theEvent, whereClauses);}

        eventsPerStream[0] = theEvent;
        boolean isHandled = false;

        for (int i = 0; i < whereClauses.length; i++)
        {
            Boolean pass = true;
            if (whereClauses[i] != null)
            {
                if (InstrumentationHelper.ENABLED) { InstrumentationHelper.get().qSplitStreamWhere(i);}
                Boolean passEvent = (Boolean) whereClauses[i].evaluate(eventsPerStream, true, exprEvaluatorContext);
                if ((passEvent == null) || (!passEvent))
                {
                    pass = false;
                }
                if (InstrumentationHelper.ENABLED) { InstrumentationHelper.get().aSplitStreamWhere(pass);}
            }

            if (pass)
            {
                if (InstrumentationHelper.ENABLED) { InstrumentationHelper.get().qSplitStreamRoute(i);}
                UniformPair<EventBean[]> result = processors[i].processViewResult(eventsPerStream, null, false);
                if ((result != null) && (result.getFirst() != null) && (result.getFirst().length > 0))
                {
                    isHandled = true;
                    EventBean eventRouted = result.getFirst()[0];
                    if (audit) {
                        AuditPath.auditInsertInto(agentInstanceContext.getEngineURI(), agentInstanceContext.getStatementName(), eventRouted);
                    }
                    if (tableStateInstances[i] != null) {
                        tableStateInstances[i].addEventUnadorned(eventRouted);
                    }
                    else {
                        internalEventRouter.route(eventRouted, epStatementHandle, agentInstanceContext.getStatementContext().getInternalEventEngineRouteDest(), exprEvaluatorContext, isNamedWindowInsert[i]);
                    }
                }
                if (InstrumentationHelper.ENABLED) { InstrumentationHelper.get().aSplitStreamRoute();}
            }
        }

        if (InstrumentationHelper.ENABLED) { InstrumentationHelper.get().aSplitStream(true, isHandled);}
        return isHandled;
    }
}
