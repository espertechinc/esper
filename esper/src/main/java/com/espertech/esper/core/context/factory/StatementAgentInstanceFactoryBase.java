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
package com.espertech.esper.core.context.factory;

import com.espertech.esper.client.annotation.AuditEnum;
import com.espertech.esper.core.context.util.AgentInstanceContext;
import com.espertech.esper.util.AuditPath;
import com.espertech.esper.util.StopCallback;

import java.lang.annotation.Annotation;

public abstract class StatementAgentInstanceFactoryBase implements StatementAgentInstanceFactory {

    private final boolean audit;

    public StatementAgentInstanceFactoryBase(Annotation[] annotations) {
        audit = AuditEnum.CONTEXTPARTITION.getAudit(annotations) != null;
    }

    protected abstract StatementAgentInstanceFactoryResult newContextInternal(AgentInstanceContext agentInstanceContext, boolean isRecoveringResilient);

    public StatementAgentInstanceFactoryResult newContext(final AgentInstanceContext agentInstanceContext, boolean isRecoveringResilient) {
        if (!audit || agentInstanceContext.getAgentInstanceId() == -1) {
            return newContextInternal(agentInstanceContext, isRecoveringResilient);
        }

        AuditPath.auditContextPartition(agentInstanceContext.getEngineURI(), agentInstanceContext.getStatementName(), true, agentInstanceContext.getAgentInstanceId());
        StatementAgentInstanceFactoryResult result = newContextInternal(agentInstanceContext, isRecoveringResilient);
        final StopCallback stopCallback = result.getStopCallback();
        result.setStopCallback(new StopCallback() {
            public void stop() {
                AuditPath.auditContextPartition(agentInstanceContext.getEngineURI(), agentInstanceContext.getStatementName(), false, agentInstanceContext.getAgentInstanceId());
                stopCallback.stop();
            }
        });
        return result;
    }
}
