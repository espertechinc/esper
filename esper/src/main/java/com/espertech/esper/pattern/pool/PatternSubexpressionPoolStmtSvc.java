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
package com.espertech.esper.pattern.pool;

public class PatternSubexpressionPoolStmtSvc {

    private final PatternSubexpressionPoolEngineSvc engineSvc;
    private final PatternSubexpressionPoolStmtHandler stmtHandler;

    public PatternSubexpressionPoolStmtSvc(PatternSubexpressionPoolEngineSvc engineSvc, PatternSubexpressionPoolStmtHandler stmtHandler) {
        this.engineSvc = engineSvc;
        this.stmtHandler = stmtHandler;
    }

    public PatternSubexpressionPoolEngineSvc getEngineSvc() {
        return engineSvc;
    }

    public PatternSubexpressionPoolStmtHandler getStmtHandler() {
        return stmtHandler;
    }
}
