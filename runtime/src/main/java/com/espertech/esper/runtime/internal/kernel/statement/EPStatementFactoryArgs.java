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
package com.espertech.esper.runtime.internal.kernel.statement;

import com.espertech.esper.common.internal.context.util.StatementContext;
import com.espertech.esper.common.internal.statement.dispatch.UpdateDispatchView;
import com.espertech.esper.runtime.internal.kernel.service.StatementResultServiceImpl;

public class EPStatementFactoryArgs {
    private final StatementContext statementContext;
    private final UpdateDispatchView dispatchChildView;
    private final StatementResultServiceImpl statementResultService;

    public EPStatementFactoryArgs(StatementContext statementContext, UpdateDispatchView dispatchChildView, StatementResultServiceImpl statementResultService) {
        this.statementContext = statementContext;
        this.dispatchChildView = dispatchChildView;
        this.statementResultService = statementResultService;
    }

    public StatementContext getStatementContext() {
        return statementContext;
    }

    public UpdateDispatchView getDispatchChildView() {
        return dispatchChildView;
    }

    public StatementResultServiceImpl getStatementResultService() {
        return statementResultService;
    }
}
