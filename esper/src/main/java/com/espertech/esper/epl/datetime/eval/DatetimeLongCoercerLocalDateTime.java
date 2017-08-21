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
package com.espertech.esper.epl.datetime.eval;

import com.espertech.esper.codegen.base.CodegenClassScope;
import com.espertech.esper.codegen.base.CodegenMember;
import com.espertech.esper.codegen.model.expression.CodegenExpression;
import com.espertech.esper.codegen.model.expression.CodegenExpressionBuilder;

import java.time.LocalDateTime;
import java.util.TimeZone;

import static com.espertech.esper.codegen.model.expression.CodegenExpressionBuilder.exprDotMethod;
import static com.espertech.esper.codegen.model.expression.CodegenExpressionBuilder.exprDotMethodChain;

public class DatetimeLongCoercerLocalDateTime implements DatetimeLongCoercer {
    private final TimeZone timeZone;

    public DatetimeLongCoercerLocalDateTime(TimeZone timeZone) {
        this.timeZone = timeZone;
    }

    public long coerce(Object date) {
        return coerceLDTToMilliWTimezone((LocalDateTime) date, timeZone);
    }

    /**
     * NOTE: Code-generation-invoked method, method name and parameter order matters
     * @param ldt ldt
     * @param timeZone tz
     * @return millis
     */
    public static long coerceLDTToMilliWTimezone(LocalDateTime ldt, TimeZone timeZone) {
        return ldt.atZone(timeZone.toZoneId()).toInstant().toEpochMilli();
    }

    public CodegenExpression codegen(CodegenExpression value, Class valueType, CodegenClassScope codegenClassScope) {
        if (valueType != LocalDateTime.class) {
            throw new IllegalStateException("Expected a LocalDateTime type");
        }
        CodegenMember tz = codegenClassScope.makeAddMember(TimeZone.class, timeZone);
        return exprDotMethodChain(value).add("atZone", exprDotMethod(CodegenExpressionBuilder.member(tz.getMemberId()), "toZoneId")).add("toInstant").add("toEpochMilli");
    }
}
