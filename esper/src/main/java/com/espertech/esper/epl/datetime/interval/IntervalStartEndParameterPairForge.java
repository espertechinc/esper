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

public class IntervalStartEndParameterPairForge {
    private final ExprOptionalConstantForge start;
    private final ExprOptionalConstantForge end;

    public IntervalStartEndParameterPairForge(ExprOptionalConstantForge start, ExprOptionalConstantForge end) {
        this.start = start;
        this.end = end;
    }

    public IntervalStartEndParameterPairEval makeEval() {
        return new IntervalStartEndParameterPairEval(start.makeEval(), end.makeEval());
    }

    public static IntervalStartEndParameterPairForge fromParamsWithSameEnd(ExprOptionalConstantForge[] parameters) {
        ExprOptionalConstantForge start = parameters[0];
        ExprOptionalConstantForge end;
        if (parameters.length == 1) {
            end = start;
        } else {
            end = parameters[1];
        }
        return new IntervalStartEndParameterPairForge(start, end);
    }

    public static IntervalStartEndParameterPairForge fromParamsWithLongMaxEnd(ExprOptionalConstantForge[] parameters) {
        ExprOptionalConstantForge start = parameters[0];
        ExprOptionalConstantForge end;
        if (parameters.length == 1) {
            end = ExprOptionalConstantForge.make(Long.MAX_VALUE);
        } else {
            end = parameters[1];
        }
        return new IntervalStartEndParameterPairForge(start, end);
    }

    public ExprOptionalConstantForge getStart() {
        return start;
    }

    public ExprOptionalConstantForge getEnd() {
        return end;
    }

    public boolean isConstant() {
        return start.getOptionalConstant() != null && end.getOptionalConstant() != null;
    }

}
