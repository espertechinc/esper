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
package com.espertech.esper.common.internal.context.aifactory.createclass;

import com.espertech.esper.common.client.EventType;
import com.espertech.esper.common.internal.context.aifactory.core.StatementAgentInstanceFactory;
import com.espertech.esper.common.internal.context.aifactory.core.StatementAgentInstanceFactoryResult;
import com.espertech.esper.common.internal.context.airegistry.AIRegistryRequirements;
import com.espertech.esper.common.internal.context.util.*;
import com.espertech.esper.common.internal.view.core.Viewable;
import com.espertech.esper.common.internal.view.core.ViewableDefaultImpl;

public class StatementAgentInstanceFactoryCreateClass implements StatementAgentInstanceFactory {

    private String className;
    private Viewable viewable;

    public void setStatementEventType(EventType eventType) {
        this.viewable = new ViewableDefaultImpl(eventType);
    }

    public void setClassName(String className) {
        this.className = className;
    }

    public String getClassName() {
        return className;
    }

    public EventType getStatementEventType() {
        return viewable.getEventType();
    }

    public void statementCreate(StatementContext statementContext) {
    }

    public void statementDestroy(StatementContext statementContext) {
    }

    public StatementAgentInstanceFactoryResult newContext(AgentInstanceContext agentInstanceContext, boolean isRecoveringResilient) {
        return new StatementAgentInstanceFactoryCreateClassResult(viewable, AgentInstanceMgmtCallbackNoAction.INSTANCE, agentInstanceContext);
    }

    public AIRegistryRequirements getRegistryRequirements() {
        return AIRegistryRequirements.noRequirements();
    }

    public StatementAgentInstanceLock obtainAgentInstanceLock(StatementContext statementContext, int agentInstanceId) {
        return AgentInstanceUtil.newLock(statementContext);
    }
}
