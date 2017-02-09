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
package com.espertech.esper.core.start;

import com.espertech.esper.client.EventType;
import com.espertech.esper.core.context.util.AgentInstanceContext;

import java.util.Collection;

public abstract class FireAndForgetProcessor {
    public abstract EventType getEventTypeResultSetProcessor();

    public abstract String getContextName();

    public abstract FireAndForgetInstance getProcessorInstance(AgentInstanceContext agentInstanceContext);

    public abstract FireAndForgetInstance getProcessorInstanceContextById(int agentInstanceId);

    public abstract FireAndForgetInstance getProcessorInstanceNoContext();

    public abstract Collection<Integer> getProcessorInstancesAll();

    public abstract String getNamedWindowOrTableName();

    public abstract boolean isVirtualDataWindow();

    public abstract String[][] getUniqueIndexes(FireAndForgetInstance processorInstance);

    public abstract EventType getEventTypePublic();
}