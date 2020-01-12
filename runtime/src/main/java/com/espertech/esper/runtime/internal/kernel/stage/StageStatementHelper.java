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
package com.espertech.esper.runtime.internal.kernel.stage;

import com.espertech.esper.common.internal.context.util.StatementContext;
import com.espertech.esper.runtime.internal.kernel.service.EPServicesEvaluation;

public class StageStatementHelper {

    public static void updateStatement(StatementContext statementContext, EPServicesEvaluation svc) {
        statementContext.setFilterService(svc.getFilterService());
        statementContext.setSchedulingService(svc.getSchedulingService());
        statementContext.setInternalEventRouteDest(svc.getInternalEventRouteDest());
    }
}
