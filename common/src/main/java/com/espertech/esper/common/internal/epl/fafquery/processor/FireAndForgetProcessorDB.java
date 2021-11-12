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
package com.espertech.esper.common.internal.epl.fafquery.processor;

import com.espertech.esper.common.client.EventType;
import com.espertech.esper.common.client.type.EPTypeClass;
import com.espertech.esper.common.internal.context.util.AgentInstanceContext;
import com.espertech.esper.common.internal.context.util.StatementContext;
import com.espertech.esper.common.internal.context.util.StatementContextRuntimeServices;
import com.espertech.esper.common.internal.epl.expression.core.ExprEvaluatorContext;
import com.espertech.esper.common.internal.epl.historical.database.core.HistoricalEventViewableDatabaseFactory;
import com.espertech.esper.common.internal.epl.historical.database.core.PollExecStrategyDBQuery;

public class FireAndForgetProcessorDB extends FireAndForgetProcessor {
    public final static EPTypeClass EPTYPE = new EPTypeClass(FireAndForgetProcessorDB.class);

    private HistoricalEventViewableDatabaseFactory factory;

    public void setFactory(HistoricalEventViewableDatabaseFactory factory) {
        this.factory = factory;
    }

    public HistoricalEventViewableDatabaseFactory getFactory() {
        return factory;
    }

    public FireAndForgetProcessorDBExecUnprepared unprepared(ExprEvaluatorContext exprEvaluatorContext, StatementContextRuntimeServices services) {
        PollExecStrategyDBQuery poll = factory.activateFireAndForget(exprEvaluatorContext, services);
        return new FireAndForgetProcessorDBExecUnprepared(poll, factory.getEvaluator());
    }

    public FireAndForgetProcessorDBExecPrepared prepared(ExprEvaluatorContext exprEvaluatorContext, StatementContextRuntimeServices services) {
        PollExecStrategyDBQuery poll = factory.activateFireAndForget(exprEvaluatorContext, services);
        return new FireAndForgetProcessorDBExecPrepared(poll, factory.getEvaluator());
    }

    public EventType getEventTypeResultSetProcessor() {
        return factory.getEventType();
    }

    public String getContextName() {
        return null;
    }

    public FireAndForgetInstance getProcessorInstance(AgentInstanceContext agentInstanceContext) {
        throw new IllegalStateException("Not available");
    }

    public String getContextDeploymentId() {
        throw new IllegalStateException("Not available");
    }

    public FireAndForgetInstance getProcessorInstanceContextById(int agentInstanceId) {
        throw new IllegalStateException("Not available");
    }

    public FireAndForgetInstance getProcessorInstanceNoContext() {
        throw new IllegalStateException("Not available");
    }

    public boolean isVirtualDataWindow() {
        return false;
    }

    public EventType getEventTypePublic() {
        return factory.getEventType();
    }

    public StatementContext getStatementContext() {
        throw new IllegalStateException("Not available");
    }
}
