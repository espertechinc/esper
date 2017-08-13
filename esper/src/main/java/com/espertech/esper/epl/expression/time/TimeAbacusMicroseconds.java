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

import com.espertech.esper.codegen.core.CodegenContext;
import com.espertech.esper.codegen.core.CodegenMethodId;
import com.espertech.esper.codegen.model.expression.CodegenExpression;
import com.espertech.esper.codegen.model.expression.CodegenExpressionRef;
import com.espertech.esper.util.JavaClassHelper;

import java.util.Calendar;
import java.util.Date;

import static com.espertech.esper.codegen.model.expression.CodegenExpressionBuilder.*;

public class TimeAbacusMicroseconds implements TimeAbacus {
    public final static TimeAbacusMicroseconds INSTANCE = new TimeAbacusMicroseconds();
    private static final long serialVersionUID = -1581886702966700798L;

    private TimeAbacusMicroseconds() {
    }

    public long deltaForSecondsDouble(double seconds) {
        return Math.round(1000000d * seconds);
    }

    public CodegenExpression deltaForSecondsDoubleCodegen(CodegenExpressionRef sec, CodegenContext context) {
        return staticMethod(Math.class, "round", op(constant(1000000d), "*", sec));
    }

    public long deltaForSecondsNumber(Number timeInSeconds) {
        if (JavaClassHelper.isFloatingPointNumber(timeInSeconds)) {
            return deltaForSecondsDouble(timeInSeconds.doubleValue());
        }
        return 1000000 * timeInSeconds.longValue();
    }

    public long calendarSet(long fromTime, Calendar cal) {
        long millis = fromTime / 1000;
        cal.setTimeInMillis(millis);
        return fromTime - millis * 1000;
    }

    public CodegenExpression calendarSetCodegen(CodegenExpression startLong, CodegenExpression cal, CodegenContext context) {
        CodegenMethodId method = context.addMethod(long.class, TimeAbacusMicroseconds.class).add(long.class, "fromTime").add(Calendar.class, "cal").begin()
                .declareVar(long.class, "millis", op(ref("fromTime"), "/", constant(1000)))
                .expression(exprDotMethod(ref("cal"), "setTimeInMillis", ref("millis")))
                .methodReturn(op(ref("fromTime"), "-", op(ref("millis"), "*", constant(1000))));
        return localMethodBuild(method).pass(startLong).pass(cal).call();
    }

    public long calendarGet(Calendar cal, long remainder) {
        return cal.getTimeInMillis() * 1000 + remainder;
    }

    public CodegenExpression calendarGetCodegen(CodegenExpression cal, CodegenExpression startRemainder, CodegenContext context) {
        return op(op(exprDotMethod(cal, "getTimeInMillis"), "*", constant(1000)), "+", startRemainder);
    }

    public long getOneSecond() {
        return 1000000;
    }

    public Date toDate(long ts) {
        return new Date(ts / 1000);
    }

    public CodegenExpression toDateCodegen(CodegenExpression ts) {
        return newInstance(Date.class, op(ts, "/", constant(1000)));
    }
}
