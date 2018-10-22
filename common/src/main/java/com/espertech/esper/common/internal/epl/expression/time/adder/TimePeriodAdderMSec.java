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

public class TimePeriodAdderMSec implements TimePeriodAdder {
    public double compute(Double value) {
        return value / 1000d;
    }

    public final static TimePeriodAdderMSec INSTANCE = new TimePeriodAdderMSec();

    private TimePeriodAdderMSec() {
    }

    public void add(Calendar cal, int value) {
        cal.add(Calendar.MILLISECOND, value);
    }

    public boolean isMicroseconds() {
        return false;
    }

    public CodegenExpression computeCodegen(CodegenExpression doubleValue) {
        return op(doubleValue, "/", constant(1000d));
    }

}
