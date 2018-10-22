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
package com.espertech.esper.regressionlib.support.extend.pattern;

import com.espertech.esper.common.internal.epl.pattern.guard.EventGuardVisitor;
import com.espertech.esper.common.internal.epl.pattern.guard.Guard;
import com.espertech.esper.common.internal.epl.pattern.guard.Quitable;
import com.espertech.esper.common.internal.filterspec.MatchedEventMap;

public class MyCountToPatternGuard implements Guard {
    private final int numCountTo;
    private final Quitable quitable;

    private int counter;

    public MyCountToPatternGuard(int numCountTo, Quitable quitable) {
        this.numCountTo = numCountTo;
        this.quitable = quitable;
    }

    public void startGuard() {
        counter = 0;
    }

    public void stopGuard() {
        // No action required when a sub-expression quits, or when the pattern is stopped
    }

    public boolean inspect(MatchedEventMap matchEvent) {
        counter++;
        if (counter > numCountTo) {
            quitable.guardQuit();
            return false;
        }
        return true;
    }

    public void accept(EventGuardVisitor visitor) {
        visitor.visitGuard(8);
    }
}
