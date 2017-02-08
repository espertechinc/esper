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
package com.espertech.esper.rowregex;

public class MatchRecognizeStatePoolStmtSvc {

    private final MatchRecognizeStatePoolEngineSvc engineSvc;
    private final MatchRecognizeStatePoolStmtHandler stmtHandler;

    public MatchRecognizeStatePoolStmtSvc(MatchRecognizeStatePoolEngineSvc engineSvc, MatchRecognizeStatePoolStmtHandler stmtHandler) {
        this.engineSvc = engineSvc;
        this.stmtHandler = stmtHandler;
    }

    public MatchRecognizeStatePoolEngineSvc getEngineSvc() {
        return engineSvc;
    }

    public MatchRecognizeStatePoolStmtHandler getStmtHandler() {
        return stmtHandler;
    }
}
