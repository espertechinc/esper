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
import com.espertech.esper.client.annotation.AuditEnum;
import com.espertech.esper.collection.UniformPair;
import com.espertech.esper.core.context.util.AgentInstanceContext;
import com.espertech.esper.core.service.EPStatementHandle;
import com.espertech.esper.core.service.InternalEventRouter;
import com.espertech.esper.core.start.EPStatementStartMethodOnTriggerItem;
import com.espertech.esper.epl.core.resultset.core.ResultSetProcessor;
import com.espertech.esper.epl.expression.core.ExprEvaluator;
import com.espertech.esper.epl.expression.core.ExprEvaluatorContext;
import com.espertech.esper.epl.table.mgmt.TableStateInstance;
import com.espertech.esper.metrics.instrumentation.InstrumentationHelper;
import com.espertech.esper.util.AuditPath;

public abstract class RouteResultViewHandlerBase implements RouteResultViewHandler {
    protected final InternalEventRouter internalEventRouter;
    private final TableStateInstance[] tableStateInstances;
    protected final EPStatementStartMethodOnTriggerItem[] items;
    protected final EPStatementHandle epStatementHandle;
    protected final ResultSetProcessor[] processors;
    protected final ExprEvaluator[] whereClauses;
    protected final EventBean[] eventsPerStream = new EventBean[1];
    protected final AgentInstanceContext agentInstanceContext;
    protected final boolean audit;

    public RouteResultViewHandlerBase(EPStatementHandle epStatementHandle, InternalEventRouter internalEventRouter, TableStateInstance[] tableStateInstances, EPStatementStartMethodOnTriggerItem[] items, ResultSetProcessor[] processors, ExprEvaluator[] whereClauses, AgentInstanceContext agentInstanceContext) {
        this.internalEventRouter = internalEventRouter;
        this.tableStateInstances = tableStateInstances;
        this.items = items;
        this.epStatementHandle = epStatementHandle;
        this.processors = processors;
        this.whereClauses = whereClauses;
        this.agentInstanceContext = agentInstanceContext;
        this.audit = AuditEnum.INSERT.getAudit(agentInstanceContext.getAnnotations()) != null;
    }

    boolean checkWhereClauseCurrentEvent(int index, ExprEvaluatorContext exprEvaluatorContext) {
        boolean pass = true;

        if (whereClauses[index] != null) {
            if (InstrumentationHelper.ENABLED) {
                InstrumentationHelper.get().qSplitStreamWhere(index);
            }
            Boolean passEvent = (Boolean) whereClauses[index].evaluate(eventsPerStream, true, exprEvaluatorContext);
            if ((passEvent == null) || (!passEvent)) {
                pass = false;
            }
            if (InstrumentationHelper.ENABLED) {
                InstrumentationHelper.get().aSplitStreamWhere(pass);
            }
        }

        return pass;
    }

    boolean mayRouteCurrentEvent(int index, ExprEvaluatorContext exprEvaluatorContext) {
        if (InstrumentationHelper.ENABLED) {
            InstrumentationHelper.get().qSplitStreamRoute(index);
        }
        UniformPair<EventBean[]> result = processors[index].processViewResult(eventsPerStream, null, false);
        boolean routed = false;
        if ((result != null) && (result.getFirst() != null) && (result.getFirst().length > 0)) {
            route(result.getFirst()[0], index, exprEvaluatorContext);
            routed = true;
        }
        if (InstrumentationHelper.ENABLED) {
            InstrumentationHelper.get().aSplitStreamRoute();
        }
        return routed;
    }

    private void route(EventBean routed, int index, ExprEvaluatorContext exprEvaluatorContext) {
        if (audit) {
            AuditPath.auditInsertInto(agentInstanceContext.getEngineURI(), agentInstanceContext.getStatementName(), routed);
        }
        TableStateInstance tableStateInstance = tableStateInstances[index];
        if (tableStateInstance != null) {
            tableStateInstance.addEventUnadorned(routed);
        } else {
            boolean isNamedWindowInsert = items[index].isNamedWindowInsert();
            internalEventRouter.route(routed, epStatementHandle, agentInstanceContext.getStatementContext().getInternalEventEngineRouteDest(), exprEvaluatorContext, isNamedWindowInsert);
        }
    }
}
