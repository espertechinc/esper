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
package com.espertech.esper.common.internal.epl.expression.time.adder;

import com.espertech.esper.common.internal.bytecodemodel.model.expression.CodegenExpression;

import java.util.Calendar;

public class TimePeriodAdderMonth implements TimePeriodAdder {
    private static final double MULTIPLIER = 30 * 24 * 60 * 60;

    public final static TimePeriodAdderMonth INSTANCE = new TimePeriodAdderMonth();

    public TimePeriodAdderMonth() {
    }

    public double compute(Double value) {
        return value * MULTIPLIER;
    }

    public void add(Calendar cal, int value) {
        cal.add(Calendar.MONTH, value);
    }

    public boolean isMicroseconds() {
        return false;
    }

    public CodegenExpression computeCodegen(CodegenExpression doubleValue) {
        return TimePeriodAdderUtil.computeCodegenTimesMultiplier(doubleValue, MULTIPLIER);
    }
}
