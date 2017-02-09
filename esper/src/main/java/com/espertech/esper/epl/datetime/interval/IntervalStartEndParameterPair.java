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

public class IntervalStartEndParameterPair {
    private final ExprOptionalConstant start;
    private final ExprOptionalConstant end;

    private IntervalStartEndParameterPair(ExprOptionalConstant start, ExprOptionalConstant end) {
        this.start = start;
        this.end = end;
    }

    public static IntervalStartEndParameterPair fromParamsWithSameEnd(ExprOptionalConstant[] parameters) {
        ExprOptionalConstant start = parameters[0];
        ExprOptionalConstant end;
        if (parameters.length == 1) {
            end = start;
        } else {
            end = parameters[1];
        }
        return new IntervalStartEndParameterPair(start, end);
    }

    public static IntervalStartEndParameterPair fromParamsWithLongMaxEnd(ExprOptionalConstant[] parameters) {
        ExprOptionalConstant start = parameters[0];
        ExprOptionalConstant end;
        if (parameters.length == 1) {
            end = ExprOptionalConstant.make(Long.MAX_VALUE);
        } else {
            end = parameters[1];
        }
        return new IntervalStartEndParameterPair(start, end);
    }

    public ExprOptionalConstant getStart() {
        return start;
    }

    public ExprOptionalConstant getEnd() {
        return end;
    }

    public boolean isConstant() {
        return start.getOptionalConstant() != null && end.getOptionalConstant() != null;
    }

}
