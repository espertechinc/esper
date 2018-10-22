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
package com.espertech.esper.common.internal.epl.subselect;

import com.espertech.esper.common.client.EventBean;
import com.espertech.esper.common.internal.context.util.AgentInstanceContext;
import com.espertech.esper.common.internal.epl.index.base.EventTable;
import com.espertech.esper.common.internal.event.core.FlushedEventBuffer;
import com.espertech.esper.common.internal.view.util.BufferObserver;

/**
 * Observer to a buffer that is filled by a subselect view when it posts events,
 * to be added and removed from indexes.
 */
public class SubselectBufferObserver implements BufferObserver {
    private final EventTable[] eventIndex;
    private final AgentInstanceContext agentInstanceContext;

    /**
     * Ctor.
     *
     * @param eventIndex           index to update
     * @param agentInstanceContext agent instance context
     */
    public SubselectBufferObserver(EventTable[] eventIndex, AgentInstanceContext agentInstanceContext) {
        this.eventIndex = eventIndex;
        this.agentInstanceContext = agentInstanceContext;
    }

    public void newData(int streamId, FlushedEventBuffer newEventBuffer, FlushedEventBuffer oldEventBuffer) {
        EventBean[] newData = newEventBuffer.getAndFlush();
        EventBean[] oldData = oldEventBuffer.getAndFlush();
        for (EventTable table : eventIndex) {
            table.addRemove(newData, oldData, agentInstanceContext);
        }
    }
}