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
package com.espertech.esper.supportunit.guard;

import com.espertech.esper.pattern.PatternAgentInstanceContext;
import com.espertech.esper.pattern.guard.Quitable;

public class SupportQuitable implements Quitable {
    private final PatternAgentInstanceContext patternContext;

    public int quitCounter = 0;

    public SupportQuitable(PatternAgentInstanceContext patternContext) {
        this.patternContext = patternContext;
    }

    public void guardQuit() {
        quitCounter++;
    }

    public int getAndResetQuitCounter() {
        return quitCounter;
    }

    @Override
    public PatternAgentInstanceContext getContext() {
        return patternContext;
    }
}
