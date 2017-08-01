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
package com.espertech.esper.epl.datetime.interval;

public class IntervalStartEndParameterPairEval {
    private final ExprOptionalConstantEval start;
    private final ExprOptionalConstantEval end;

    public IntervalStartEndParameterPairEval(ExprOptionalConstantEval start, ExprOptionalConstantEval end) {
        this.start = start;
        this.end = end;
    }

    public ExprOptionalConstantEval getStart() {
        return start;
    }

    public ExprOptionalConstantEval getEnd() {
        return end;
    }

    public boolean isConstant() {
        return start.getOptionalConstant() != null && end.getOptionalConstant() != null;
    }

}
