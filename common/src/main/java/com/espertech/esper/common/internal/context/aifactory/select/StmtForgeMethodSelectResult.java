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
package com.espertech.esper.common.internal.context.aifactory.select;

import com.espertech.esper.common.client.EventType;
import com.espertech.esper.common.internal.compile.stage3.StmtForgeMethodResult;

public class StmtForgeMethodSelectResult {

    private final StmtForgeMethodResult forgeResult;
    private final EventType eventType;
    private final int numStreams;

    public StmtForgeMethodSelectResult(StmtForgeMethodResult forgeResult, EventType eventType, int numStreams) {
        this.forgeResult = forgeResult;
        this.eventType = eventType;
        this.numStreams = numStreams;
    }

    public StmtForgeMethodResult getForgeResult() {
        return forgeResult;
    }

    public EventType getEventType() {
        return eventType;
    }

    public int getNumStreams() {
        return numStreams;
    }
}
