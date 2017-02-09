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
package com.espertech.esper.core.service;

import com.espertech.esper.core.context.factory.StatementAgentInstanceFactoryResult;
import com.espertech.esper.dispatch.DispatchService;
import com.espertech.esper.timer.TimeSourceService;
import com.espertech.esper.util.StopCallback;

public interface EPStatementFactory {
    EPStatementSPI make(String expressionNoAnnotations, boolean isPattern, DispatchService dispatchService, StatementLifecycleSvcImpl statementLifecycleSvc, long timeLastStateChange, boolean preserveDispatchOrder, boolean isSpinLocks, long blockingTimeout, TimeSourceService timeSource, StatementMetadata statementMetadata, Object statementUserObject, StatementContext statementContext, boolean isFailed, boolean nameProvided);

    StopCallback makeStopMethod(StatementAgentInstanceFactoryResult startResult);
}
