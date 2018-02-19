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

/**
 * A no-latch implementation of a latch for use in guaranteeing delivery between
 * a named window delta result and consumable by another statement.
 */
public class NamedWindowConsumerLatchNone extends NamedWindowConsumerLatch {
    public NamedWindowConsumerLatchNone(NamedWindowDeltaData deltaData, Map<EPStatementAgentInstanceHandle, List<NamedWindowConsumerView>> dispatchTo) {
        super(deltaData, dispatchTo);
    }

    public void await() {
    }

    public Thread getCurrentThread() {
        return Thread.currentThread();
    }

    public void done() {
    }

    public NamedWindowConsumerLatch getEarlier() {
        return null;
    }
}
