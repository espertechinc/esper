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

import com.espertech.esper.common.internal.bytecodemodel.base.CodegenClassScope;
import com.espertech.esper.common.internal.bytecodemodel.base.CodegenMethodScope;
import com.espertech.esper.common.internal.bytecodemodel.model.expression.CodegenExpression;

import static com.espertech.esper.common.internal.bytecodemodel.model.expression.CodegenExpressionBuilder.*;

public class TimePeriodAdderUtil {
    public static CodegenExpression computeCodegenTimesMultiplier(CodegenExpression doubleValue, double multiplier) {
        return op(doubleValue, "*", constant(multiplier));
    }

    public static CodegenExpression addCodegenCalendar(CodegenExpression cal, CodegenExpression value, int unit) {
        return exprDotMethod(cal, "add", constant(unit), value);
    }

    public static CodegenExpression makeArray(TimePeriodAdder[] adders, CodegenMethodScope parent, CodegenClassScope classScope) {
        CodegenExpression[] expressions = new CodegenExpression[adders.length];
        for (int i = 0; i < adders.length; i++) {
            expressions[i] = publicConstValue(adders[i].getClass(), "INSTANCE");
        }
        return newArrayWithInit(TimePeriodAdder.class, expressions);
    }
}
