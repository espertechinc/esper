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
package com.espertech.esper.common.internal.context.aifactory.createexpression;

import com.espertech.esper.common.client.EventType;
import com.espertech.esper.common.internal.context.aifactory.core.StatementAgentInstanceFactory;
import com.espertech.esper.common.internal.context.aifactory.core.StatementAgentInstanceFactoryResult;
import com.espertech.esper.common.internal.context.airegistry.AIRegistryRequirements;
import com.espertech.esper.common.internal.context.util.*;
import com.espertech.esper.common.internal.view.core.Viewable;
import com.espertech.esper.common.internal.view.core.ViewableDefaultImpl;

public class StatementAgentInstanceFactoryCreateExpression implements StatementAgentInstanceFactory {

    private String expressionName;
    private Viewable viewable;

    public void setStatementEventType(EventType eventType) {
        this.viewable = new ViewableDefaultImpl(eventType);
    }

    public void setExpressionName(String expressionName) {
        this.expressionName = expressionName;
    }

    public EventType getStatementEventType() {
        return viewable.getEventType();
    }

    public void statementCreate(StatementContext statementContext) {
    }

    public void statementDestroy(StatementContext statementContext) {
    }

    public StatementAgentInstanceFactoryResult newContext(AgentInstanceContext agentInstanceContext, boolean isRecoveringResilient) {
        return new StatementAgentInstanceFactoryCreateExpressionResult(viewable, AgentInstanceStopCallbackNoAction.INSTANCE, agentInstanceContext);
    }

    public AIRegistryRequirements getRegistryRequirements() {
        return AIRegistryRequirements.noRequirements();
    }

    public StatementAgentInstanceLock obtainAgentInstanceLock(StatementContext statementContext, int agentInstanceId) {
        return AgentInstanceUtil.newLock(statementContext);
    }
}
