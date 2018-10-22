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
import com.espertech.esper.common.internal.context.util.AgentInstanceContext;
import com.espertech.esper.common.internal.context.util.StatementContext;
import com.espertech.esper.common.internal.epl.namedwindow.core.NamedWindow;
import com.espertech.esper.common.internal.epl.namedwindow.core.NamedWindowInstance;

public class FireAndForgetProcessorNamedWindow extends FireAndForgetProcessor {
    private NamedWindow namedWindow;

    public void setNamedWindow(NamedWindow namedWindow) {
        this.namedWindow = namedWindow;
    }

    public EventType getEventTypeResultSetProcessor() {
        return namedWindow.getRootView().getEventType();
    }

    public String getContextName() {
        return namedWindow.getRootView().getContextName();
    }

    public FireAndForgetInstance getProcessorInstance(AgentInstanceContext agentInstanceContext) {
        NamedWindowInstance instance;
        if (agentInstanceContext != null) {
            instance = namedWindow.getNamedWindowInstance(agentInstanceContext);
        } else {
            instance = namedWindow.getNamedWindowInstanceNoContext();
        }
        if (instance != null) {
            return new FireAndForgetInstanceNamedWindow(instance);
        }
        return null;
    }

    public String getContextDeploymentId() {
        return namedWindow.getStatementContext().getContextRuntimeDescriptor().getContextDeploymentId();
    }

    public FireAndForgetInstance getProcessorInstanceContextById(int agentInstanceId) {
        NamedWindowInstance instance = namedWindow.getNamedWindowInstance(agentInstanceId);
        if (instance != null) {
            return new FireAndForgetInstanceNamedWindow(instance);
        }
        return null;
    }

    public FireAndForgetInstance getProcessorInstanceNoContext() {
        return getProcessorInstance(null);
    }

    public String getNamedWindowOrTableName() {
        return namedWindow.getName();
    }

    public String[][] getUniqueIndexes(FireAndForgetInstance processorInstance) {
        throw new UnsupportedOperationException();
    }

    public EventType getEventTypePublic() {
        return namedWindow.getRootView().getEventType();
    }

    public StatementContext getStatementContext() {
        return namedWindow.getStatementContext();
    }
}
