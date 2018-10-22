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
package com.espertech.esper.common.internal.context.aifactory.core;

import com.espertech.esper.common.client.EventType;
import com.espertech.esper.common.internal.context.airegistry.AIRegistryRequirements;
import com.espertech.esper.common.internal.context.util.AgentInstanceContext;
import com.espertech.esper.common.internal.context.util.StatementAgentInstanceLock;
import com.espertech.esper.common.internal.context.util.StatementContext;
import com.espertech.esper.common.internal.context.util.UndeployPreconditionException;

public interface StatementAgentInstanceFactory {
    EventType getStatementEventType();

    StatementAgentInstanceFactoryResult newContext(AgentInstanceContext agentInstanceContext, boolean isRecoveringResilient);

    void statementCreate(StatementContext statementContext);

    void statementDestroy(StatementContext statementContext);

    AIRegistryRequirements getRegistryRequirements();

    default void statementDestroyPreconditions(StatementContext statementContext) throws UndeployPreconditionException {
    }

    StatementAgentInstanceLock obtainAgentInstanceLock(StatementContext statementContext, int agentInstanceId);
}
