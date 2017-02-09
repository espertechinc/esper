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

import com.espertech.esper.core.context.util.StatementAgentInstanceUtil;
import com.espertech.esper.core.service.StatementContext;
import com.espertech.esper.util.StopCallback;

import java.util.List;

/**
 * Method to call to stop an EPStatement.
 */
public class EPStatementStopMethodImpl implements EPStatementStopMethod {
    private final StatementContext statementContext;
    private final StopCallback[] stopCallbacks;

    public EPStatementStopMethodImpl(StatementContext statementContext, List<StopCallback> stopCallbacks) {
        this.statementContext = statementContext;
        this.stopCallbacks = stopCallbacks.toArray(new StopCallback[stopCallbacks.size()]);
    }

    public void stop() {
        for (StopCallback stopCallback : stopCallbacks) {
            StatementAgentInstanceUtil.stopSafe(stopCallback, statementContext);
        }
    }
}
