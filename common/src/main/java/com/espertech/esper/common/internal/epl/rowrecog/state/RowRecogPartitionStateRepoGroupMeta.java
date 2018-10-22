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
package com.espertech.esper.common.internal.epl.rowrecog.state;

import com.espertech.esper.common.client.EventBean;
import com.espertech.esper.common.internal.context.util.AgentInstanceContext;
import com.espertech.esper.common.internal.epl.expression.core.ExprEvaluator;

public class RowRecogPartitionStateRepoGroupMeta {
    private final boolean hasInterval;
    private final ExprEvaluator partitionExpression;
    private final AgentInstanceContext agentInstanceContext;
    private final EventBean[] eventsPerStream = new EventBean[1];

    public RowRecogPartitionStateRepoGroupMeta(boolean hasInterval, ExprEvaluator partitionExpression, AgentInstanceContext agentInstanceContext) {
        this.hasInterval = hasInterval;
        this.partitionExpression = partitionExpression;
        this.agentInstanceContext = agentInstanceContext;
    }

    public boolean isHasInterval() {
        return hasInterval;
    }

    public ExprEvaluator getPartitionExpression() {
        return partitionExpression;
    }

    public AgentInstanceContext getAgentInstanceContext() {
        return agentInstanceContext;
    }

    public EventBean[] getEventsPerStream() {
        return eventsPerStream;
    }
}