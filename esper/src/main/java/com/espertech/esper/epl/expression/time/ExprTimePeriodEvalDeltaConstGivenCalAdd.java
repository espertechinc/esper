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

import com.espertech.esper.codegen.base.CodegenBlock;
import com.espertech.esper.codegen.base.CodegenClassScope;
import com.espertech.esper.codegen.base.CodegenMember;
import com.espertech.esper.codegen.base.CodegenMethodScope;
import com.espertech.esper.codegen.model.expression.CodegenExpression;
import com.espertech.esper.codegen.model.expression.CodegenExpressionRef;
import com.espertech.esper.codegen.base.CodegenMethodNode;
import com.espertech.esper.epl.expression.core.ExprEvaluatorContext;

import java.util.Calendar;
import java.util.TimeZone;

import static com.espertech.esper.codegen.model.expression.CodegenExpressionBuilder.*;

public class ExprTimePeriodEvalDeltaConstGivenCalAdd implements ExprTimePeriodEvalDeltaConst, ExprTimePeriodEvalDeltaConstFactory {
    private final Calendar cal;
    private final ExprTimePeriodAdder.TimePeriodAdder[] adders;
    private final int[] added;
    private final TimeAbacus timeAbacus;
    private final int indexMicroseconds;
    private final TimeZone timeZone;

    public ExprTimePeriodEvalDeltaConstGivenCalAdd(ExprTimePeriodAdder.TimePeriodAdder[] adders, int[] added, TimeZone timeZone, TimeAbacus timeAbacus) {
        this.adders = adders;
        this.added = added;
        this.cal = Calendar.getInstance(timeZone);
        this.timeAbacus = timeAbacus;
        this.indexMicroseconds = ExprTimePeriodUtil.findIndexMicroseconds(adders);
        this.timeZone = timeZone;
    }

    public ExprTimePeriodEvalDeltaConst make(String validateMsgName, String validateMsgValue, ExprEvaluatorContext exprEvaluatorContext, TimeAbacus timeAbacus) {
        return this;
    }

    public boolean equalsTimePeriod(ExprTimePeriodEvalDeltaConst otherComputation) {
        if (otherComputation instanceof ExprTimePeriodEvalDeltaConstGivenCalAdd) {
            ExprTimePeriodEvalDeltaConstGivenCalAdd other = (ExprTimePeriodEvalDeltaConstGivenCalAdd) otherComputation;
            if (other.adders.length != adders.length) {
                return false;
            }
            for (int i = 0; i < adders.length; i++) {
                if (added[i] != other.added[i] || adders[i].getClass() != other.adders[i].getClass()) {
                    return false;
                }
            }
            return true;
        }
        return false;
    }

    public synchronized long deltaAdd(long fromTime) {
        long target = addSubtract(fromTime, 1);
        return target - fromTime;
    }

    public CodegenExpression deltaAddCodegen(CodegenExpression reference, CodegenMethodScope codegenMethodScope, CodegenClassScope codegenClassScope) {
        CodegenMethodNode method = codegenMethodScope.makeChild(long.class, ExprTimePeriodEvalDeltaConstGivenCalAdd.class, codegenClassScope).addParam(long.class, "fromTime").getBlock()
                .declareVar(long.class, "target", addSubtractCodegen(ref("fromTime"), constant(1), codegenMethodScope, codegenClassScope))
                .methodReturn(op(ref("target"), "-", ref("fromTime")));
        return localMethodBuild(method).pass(reference).call();
    }

    public synchronized long deltaSubtract(long fromTime) {
        long target = addSubtract(fromTime, -1);
        return fromTime - target;
    }

    public ExprTimePeriodEvalDeltaResult deltaAddWReference(long fromTime, long reference) {
        // find the next-nearest reference higher then the current time, compute delta, return reference one lower
        if (reference > fromTime) {
            while (reference > fromTime) {
                reference = reference - deltaSubtract(reference);
            }
        }

        long next = reference;
        long last;
        do {
            last = next;
            next = next + deltaAdd(last);
        }
        while (next <= fromTime);
        return new ExprTimePeriodEvalDeltaResult(next - fromTime, last);
    }

    private long addSubtract(long fromTime, int factor) {
        long remainder = timeAbacus.calendarSet(fromTime, cal);
        for (int i = 0; i < adders.length; i++) {
            adders[i].add(cal, factor * added[i]);
        }
        long result = timeAbacus.calendarGet(cal, remainder);
        if (indexMicroseconds != -1) {
            result += factor * added[indexMicroseconds];
        }
        return result;
    }

    private CodegenExpression addSubtractCodegen(CodegenExpressionRef fromTime, CodegenExpression factor, CodegenMethodScope codegenMethodScope, CodegenClassScope codegenClassScope) {
        CodegenMember tz = codegenClassScope.makeAddMember(TimeZone.class, timeZone);
        CodegenBlock block = codegenMethodScope.makeChild(long.class, ExprTimePeriodEvalDeltaConstGivenCalAdd.class, codegenClassScope).addParam(long.class, "fromTime").addParam(int.class, "factor").getBlock()
                .declareVar(Calendar.class, "cal", staticMethod(Calendar.class, "getInstance", member(tz.getMemberId())))
                .declareVar(long.class, "remainder", timeAbacus.calendarSetCodegen(ref("fromTime"), ref("cal"), codegenMethodScope, codegenClassScope));
        for (int i = 0; i < adders.length; i++) {
            block.expression(adders[i].addCodegen(ref("cal"), op(ref("factor"), "*", constant(added[i]))));
        }
        block.declareVar(long.class, "result", timeAbacus.calendarGetCodegen(ref("cal"), ref("remainder"), codegenClassScope));
        if (indexMicroseconds != -1) {
            block.assignRef("result", op(ref("result"), "+", op(ref("factor"), "*", constant(added[indexMicroseconds]))));
        }
        CodegenMethodNode method = block.methodReturn(ref("result"));
        return localMethodBuild(method).pass(fromTime).pass(factor).call();
    }
}
