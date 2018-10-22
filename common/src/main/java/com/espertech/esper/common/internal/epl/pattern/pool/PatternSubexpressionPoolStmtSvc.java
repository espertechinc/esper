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
package com.espertech.esper.common.internal.epl.pattern.pool;

public class PatternSubexpressionPoolStmtSvc {

    private final PatternSubexpressionPoolRuntimeSvc runtimeSvc;
    private final PatternSubexpressionPoolStmtHandler stmtHandler;

    public PatternSubexpressionPoolStmtSvc(PatternSubexpressionPoolRuntimeSvc runtimeSvc, PatternSubexpressionPoolStmtHandler stmtHandler) {
        this.runtimeSvc = runtimeSvc;
        this.stmtHandler = stmtHandler;
    }

    public PatternSubexpressionPoolRuntimeSvc getRuntimeSvc() {
        return runtimeSvc;
    }

    public PatternSubexpressionPoolStmtHandler getStmtHandler() {
        return stmtHandler;
    }
}
