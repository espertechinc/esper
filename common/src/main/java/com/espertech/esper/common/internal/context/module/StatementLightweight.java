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
package com.espertech.esper.common.internal.context.module;

import com.espertech.esper.common.internal.context.util.StatementContext;
import com.espertech.esper.common.internal.context.util.StatementResultService;

public class StatementLightweight {
    private final StatementProvider statementProvider;
    private final StatementInformationalsRuntime statementInformationals;
    private final StatementResultService statementResultService;
    private final StatementContext statementContext;

    public StatementLightweight(StatementProvider statementProvider, StatementInformationalsRuntime statementInformationals, StatementResultService statementResultService, StatementContext statementContext) {
        this.statementProvider = statementProvider;
        this.statementInformationals = statementInformationals;
        this.statementResultService = statementResultService;
        this.statementContext = statementContext;
    }

    public StatementInformationalsRuntime getStatementInformationals() {
        return statementInformationals;
    }

    public StatementResultService getStatementResultService() {
        return statementResultService;
    }

    public StatementProvider getStatementProvider() {
        return statementProvider;
    }

    public StatementContext getStatementContext() {
        return statementContext;
    }
}
