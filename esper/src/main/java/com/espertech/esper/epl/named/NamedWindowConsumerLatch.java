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
package com.espertech.esper.epl.named;

import com.espertech.esper.core.context.util.EPStatementAgentInstanceHandle;

import java.util.List;
import java.util.Map;

public abstract class NamedWindowConsumerLatch {
    private final NamedWindowDeltaData deltaData;
    private final Map<EPStatementAgentInstanceHandle, List<NamedWindowConsumerView>> dispatchTo;

    public abstract void await();

    public abstract void done();

    public abstract NamedWindowConsumerLatch getEarlier();

    public NamedWindowConsumerLatch(NamedWindowDeltaData deltaData, Map<EPStatementAgentInstanceHandle, List<NamedWindowConsumerView>> dispatchTo) {
        this.deltaData = deltaData;
        this.dispatchTo = dispatchTo;
    }

    public NamedWindowDeltaData getDeltaData() {
        return deltaData;
    }

    public Map<EPStatementAgentInstanceHandle, List<NamedWindowConsumerView>> getDispatchTo() {
        return dispatchTo;
    }
}
