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
import com.espertech.esper.epl.named.NamedWindowProcessor;
import com.espertech.esper.epl.named.NamedWindowProcessorInstance;

import java.util.Collection;

public class FireAndForgetProcessorNamedWindow extends FireAndForgetProcessor {
    private final NamedWindowProcessor namedWindowProcessor;

    protected FireAndForgetProcessorNamedWindow(NamedWindowProcessor namedWindowProcessor) {
        this.namedWindowProcessor = namedWindowProcessor;
    }

    public NamedWindowProcessor getNamedWindowProcessor() {
        return namedWindowProcessor;
    }

    public EventType getEventTypeResultSetProcessor() {
        return namedWindowProcessor.getNamedWindowType();
    }

    public EventType getEventTypePublic() {
        return namedWindowProcessor.getNamedWindowType();
    }

    public String getContextName() {
        return namedWindowProcessor.getContextName();
    }

    public FireAndForgetInstance getProcessorInstance(AgentInstanceContext agentInstanceContext) {
        NamedWindowProcessorInstance processorInstance = namedWindowProcessor.getProcessorInstance(agentInstanceContext);
        if (processorInstance != null) {
            return new FireAndForgetInstanceNamedWindow(processorInstance);
        }
        return null;
    }

    public FireAndForgetInstance getProcessorInstanceContextById(int agentInstanceId) {
        NamedWindowProcessorInstance processorInstance = namedWindowProcessor.getProcessorInstance(agentInstanceId);
        if (processorInstance != null) {
            return new FireAndForgetInstanceNamedWindow(processorInstance);
        }
        return null;
    }

    public FireAndForgetInstance getProcessorInstanceNoContext() {
        NamedWindowProcessorInstance processorInstance = namedWindowProcessor.getProcessorInstanceNoContext();
        if (processorInstance == null) {
            return null;
        }
        return new FireAndForgetInstanceNamedWindow(processorInstance);
    }

    public Collection<Integer> getProcessorInstancesAll() {
        return namedWindowProcessor.getProcessorInstancesAll();
    }

    public String getNamedWindowOrTableName() {
        return namedWindowProcessor.getNamedWindowName();
    }

    public boolean isVirtualDataWindow() {
        return namedWindowProcessor.isVirtualDataWindow();
    }

    public String[][] getUniqueIndexes(FireAndForgetInstance processorInstance) {
        if (processorInstance == null) {
            return new String[0][];
        }
        return namedWindowProcessor.getUniqueIndexes();
    }
}
