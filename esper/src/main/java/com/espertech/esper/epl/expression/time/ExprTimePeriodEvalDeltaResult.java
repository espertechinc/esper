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
package com.espertech.esper.epl.expression.time;

public class ExprTimePeriodEvalDeltaResult {
    private final long delta;
    private final long lastReference;

    public ExprTimePeriodEvalDeltaResult(long delta, long lastReference) {
        this.delta = delta;
        this.lastReference = lastReference;
    }

    public long getDelta() {
        return delta;
    }

    public long getLastReference() {
        return lastReference;
    }
}
