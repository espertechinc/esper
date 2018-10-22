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

import static com.espertech.esper.common.internal.bytecodemodel.model.expression.CodegenExpressionBuilder.constant;
import static com.espertech.esper.common.internal.bytecodemodel.model.expression.CodegenExpressionBuilder.op;

public class TimePeriodAdderUSec implements TimePeriodAdder {
    public double compute(Double value) {
        return value / 1000000d;
    }

    public final static TimePeriodAdderUSec INSTANCE = new TimePeriodAdderUSec();

    private TimePeriodAdderUSec() {
    }

    public void add(Calendar cal, int value) {
        // no action : calendar does not add microseconds
    }

    public boolean isMicroseconds() {
        return true;
    }

    public CodegenExpression computeCodegen(CodegenExpression doubleValue) {
        return op(doubleValue, "/", constant(1000000d));
    }

}
