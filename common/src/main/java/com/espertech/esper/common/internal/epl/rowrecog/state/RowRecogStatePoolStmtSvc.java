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
package com.espertech.esper.common.internal.epl.rowrecog.state;

public class RowRecogStatePoolStmtSvc {

    private final RowRecogStatePoolRuntimeSvc runtimeSvc;
    private final RowRecogStatePoolStmtHandler stmtHandler;

    public RowRecogStatePoolStmtSvc(RowRecogStatePoolRuntimeSvc runtimeSvc, RowRecogStatePoolStmtHandler stmtHandler) {
        this.runtimeSvc = runtimeSvc;
        this.stmtHandler = stmtHandler;
    }

    public RowRecogStatePoolRuntimeSvc getRuntimeSvc() {
        return runtimeSvc;
    }

    public RowRecogStatePoolStmtHandler getStmtHandler() {
        return stmtHandler;
    }
}
