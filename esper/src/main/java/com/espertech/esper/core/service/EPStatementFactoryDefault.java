/**************************************************************************************
 * Copyright (C) 2006-2015 EsperTech Inc. All rights reserved.                        *
 * http://www.espertech.com/esper                                                          *
 * http://www.espertech.com                                                           *
 * ---------------------------------------------------------------------------------- *
 * The software in this package is published under the terms of the GPL license       *
 * a copy of which has been included with this distribution in the license.txt file.  *
 **************************************************************************************/
package com.espertech.esper.core.service;

import com.espertech.esper.dispatch.DispatchService;
import com.espertech.esper.timer.TimeSourceService;

public class EPStatementFactoryDefault implements EPStatementFactory
{
    public EPStatementSPI make(String expressionNoAnnotations, boolean isPattern, DispatchService dispatchService, StatementLifecycleSvcImpl statementLifecycleSvc, long timeLastStateChange, boolean preserveDispatchOrder, boolean isSpinLocks, long blockingTimeout, TimeSourceService timeSource, StatementMetadata statementMetadata, Object statementUserObject, StatementContext statementContext, boolean isFailed, boolean nameProvided) {
        return new EPStatementImpl(expressionNoAnnotations, isPattern, dispatchService, statementLifecycleSvc, timeLastStateChange, preserveDispatchOrder, isSpinLocks, blockingTimeout, timeSource, statementMetadata, statementUserObject, statementContext, isFailed, nameProvided);
    }
}
