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
package com.espertech.esper.common.internal.context.aifactory.createschema;

import com.espertech.esper.common.client.EPException;
import com.espertech.esper.common.client.EventType;
import com.espertech.esper.common.client.util.NameAccessModifier;
import com.espertech.esper.common.internal.context.aifactory.core.StatementAgentInstanceFactory;
import com.espertech.esper.common.internal.context.aifactory.core.StatementAgentInstanceFactoryResult;
import com.espertech.esper.common.internal.context.airegistry.AIRegistryRequirements;
import com.espertech.esper.common.internal.context.util.*;
import com.espertech.esper.common.internal.view.core.Viewable;
import com.espertech.esper.common.internal.view.core.ViewableDefaultImpl;

public class StatementAgentInstanceFactoryCreateSchema implements StatementAgentInstanceFactory {

    private EventType eventType;
    private Viewable viewable;

    public void setEventType(EventType eventType) {
        this.eventType = eventType;
        this.viewable = new ViewableDefaultImpl(eventType);
    }

    public EventType getStatementEventType() {
        return viewable.getEventType();
    }

    public void statementCreate(StatementContext statementContext) {
        if (eventType.getMetadata().getAccessModifier() == NameAccessModifier.PRECONFIGURED) {
            throw new EPException("Unexpected visibility of value " + NameAccessModifier.PRECONFIGURED);
        }
    }

    public void statementDestroy(StatementContext statementContext) {
    }

    public StatementAgentInstanceFactoryResult newContext(AgentInstanceContext agentInstanceContext, boolean isRecoveringResilient) {
        return new StatementAgentInstanceFactoryCreateSchemaResult(viewable, AgentInstanceStopCallbackNoAction.INSTANCE, agentInstanceContext);
    }

    public AIRegistryRequirements getRegistryRequirements() {
        return AIRegistryRequirements.noRequirements();
    }

    public StatementAgentInstanceLock obtainAgentInstanceLock(StatementContext statementContext, int agentInstanceId) {
        return AgentInstanceUtil.newLock(statementContext);
    }
}
