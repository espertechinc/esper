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
package com.espertech.esper.common.internal.epl.join.base;

import com.espertech.esper.common.client.EventBean;
import com.espertech.esper.common.internal.context.util.AgentInstanceContext;
import com.espertech.esper.common.internal.context.util.EPStatementDispatch;
import com.espertech.esper.common.internal.event.core.FlushedEventBuffer;
import com.espertech.esper.common.internal.metrics.instrumentation.InstrumentationCommon;
import com.espertech.esper.common.internal.view.util.BufferObserver;

import java.util.HashMap;
import java.util.Map;

/**
 * This class reacts to any new data buffered by registring with the dispatch service.
 * When dispatched via execute, it takes the buffered events and hands these to the join execution strategy.
 */
public class JoinExecStrategyDispatchable implements BufferObserver, EPStatementDispatch {
    private final JoinExecutionStrategy joinExecutionStrategy;
    private final Map<Integer, FlushedEventBuffer> oldStreamBuffer;
    private final Map<Integer, FlushedEventBuffer> newStreamBuffer;
    private final int numStreams;
    private final AgentInstanceContext agentInstanceContext;

    private boolean hasNewData;

    public JoinExecStrategyDispatchable(JoinExecutionStrategy joinExecutionStrategy, int numStreams, AgentInstanceContext agentInstanceContext) {
        this.joinExecutionStrategy = joinExecutionStrategy;
        this.numStreams = numStreams;
        this.agentInstanceContext = agentInstanceContext;

        oldStreamBuffer = new HashMap<>();
        newStreamBuffer = new HashMap<>();
    }

    public void execute() {
        if (!hasNewData) {
            return;
        }
        hasNewData = false;

        EventBean[][] oldDataPerStream = new EventBean[numStreams][];
        EventBean[][] newDataPerStream = new EventBean[numStreams][];

        for (int i = 0; i < numStreams; i++) {
            oldDataPerStream[i] = getBufferData(oldStreamBuffer.get(i));
            newDataPerStream[i] = getBufferData(newStreamBuffer.get(i));
        }

        InstrumentationCommon instrumentationCommon = agentInstanceContext.getInstrumentationProvider();
        if (instrumentationCommon.activated()) {
            instrumentationCommon.qJoinDispatch(newDataPerStream, oldDataPerStream);
            joinExecutionStrategy.join(newDataPerStream, oldDataPerStream);
            instrumentationCommon.aJoinDispatch();
            return;
        }

        joinExecutionStrategy.join(newDataPerStream, oldDataPerStream);
    }

    private static EventBean[] getBufferData(FlushedEventBuffer buffer) {
        if (buffer == null) {
            return null;
        }
        return buffer.getAndFlush();
    }

    public void newData(int streamId, FlushedEventBuffer newEventBuffer, FlushedEventBuffer oldEventBuffer) {
        hasNewData = true;
        newStreamBuffer.put(streamId, newEventBuffer);
        oldStreamBuffer.put(streamId, oldEventBuffer);
    }
}
