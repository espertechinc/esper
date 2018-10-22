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
import com.espertech.esper.common.client.annotation.AuditEnum;
import com.espertech.esper.common.internal.collection.UniformPair;
import com.espertech.esper.common.internal.context.util.AgentInstanceContext;
import com.espertech.esper.common.internal.context.util.EPStatementHandle;
import com.espertech.esper.common.internal.context.util.InternalEventRouter;
import com.espertech.esper.common.internal.epl.expression.core.ExprEvaluator;
import com.espertech.esper.common.internal.epl.expression.core.ExprEvaluatorContext;
import com.espertech.esper.common.internal.epl.resultset.core.ResultSetProcessor;
import com.espertech.esper.common.internal.epl.table.core.TableInstance;

public abstract class RouteResultViewHandlerBase implements RouteResultViewHandler {
    protected final InternalEventRouter internalEventRouter;
    private final TableInstance[] tableStateInstances;
    protected final OnSplitItemEval[] items;
    protected final EPStatementHandle epStatementHandle;
    protected final ResultSetProcessor[] processors;
    protected final EventBean[] eventsPerStream = new EventBean[1];
    protected final AgentInstanceContext agentInstanceContext;
    protected final boolean audit;

    public RouteResultViewHandlerBase(EPStatementHandle epStatementHandle, InternalEventRouter internalEventRouter, TableInstance[] tableStateInstances, OnSplitItemEval[] items, ResultSetProcessor[] processors, AgentInstanceContext agentInstanceContext) {
        this.internalEventRouter = internalEventRouter;
        this.tableStateInstances = tableStateInstances;
        this.items = items;
        this.epStatementHandle = epStatementHandle;
        this.processors = processors;
        this.agentInstanceContext = agentInstanceContext;
        this.audit = AuditEnum.INSERT.getAudit(agentInstanceContext.getAnnotations()) != null;
    }

    boolean checkWhereClauseCurrentEvent(int index, ExprEvaluatorContext exprEvaluatorContext) {
        boolean pass = true;

        ExprEvaluator itemWhereClause = items[index].getWhereClause();
        if (itemWhereClause != null) {
            agentInstanceContext.getInstrumentationProvider().qSplitStreamWhere(index);
            Boolean passEvent = (Boolean) itemWhereClause.evaluate(eventsPerStream, true, exprEvaluatorContext);
            if ((passEvent == null) || (!passEvent)) {
                pass = false;
            }
            agentInstanceContext.getInstrumentationProvider().aSplitStreamWhere(pass);
        }

        return pass;
    }

    boolean mayRouteCurrentEvent(int index, ExprEvaluatorContext exprEvaluatorContext) {
        agentInstanceContext.getInstrumentationProvider().qSplitStreamRoute(index);
        UniformPair<EventBean[]> result = processors[index].processViewResult(eventsPerStream, null, false);
        boolean routed = false;
        if ((result != null) && (result.getFirst() != null) && (result.getFirst().length > 0)) {
            route(result.getFirst()[0], index, exprEvaluatorContext);
            routed = true;
        }
        agentInstanceContext.getInstrumentationProvider().aSplitStreamRoute();
        return routed;
    }

    private void route(EventBean routed, int index, ExprEvaluatorContext exprEvaluatorContext) {
        if (audit) {
            exprEvaluatorContext.getAuditProvider().insert(routed, exprEvaluatorContext);
        }
        TableInstance tableStateInstance = tableStateInstances[index];
        if (tableStateInstance != null) {
            tableStateInstance.addEventUnadorned(routed);
        } else {
            boolean isNamedWindowInsert = items[index].isNamedWindowInsert();
            agentInstanceContext.getInternalEventRouter().route(routed, agentInstanceContext, isNamedWindowInsert);
        }
    }
}
