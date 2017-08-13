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
package com.espertech.esper.epl.datetime.reformatop;

import com.espertech.esper.client.EventBean;
import com.espertech.esper.codegen.core.CodegenBlock;
import com.espertech.esper.codegen.core.CodegenContext;
import com.espertech.esper.codegen.core.CodegenMember;
import com.espertech.esper.codegen.core.CodegenMethodId;
import com.espertech.esper.codegen.model.expression.CodegenExpression;
import com.espertech.esper.codegen.model.method.CodegenParamSetExprPremade;
import com.espertech.esper.epl.datetime.eval.DatetimeLongCoercer;
import com.espertech.esper.epl.datetime.eval.DatetimeLongCoercerLocalDateTime;
import com.espertech.esper.epl.datetime.eval.DatetimeLongCoercerZonedDateTime;
import com.espertech.esper.epl.expression.core.ExprEvaluator;
import com.espertech.esper.epl.expression.core.ExprEvaluatorContext;
import com.espertech.esper.epl.expression.core.ExprForge;
import com.espertech.esper.epl.expression.core.ExprNode;

import java.time.LocalDateTime;
import java.time.ZonedDateTime;
import java.util.Calendar;
import java.util.Date;
import java.util.TimeZone;

import static com.espertech.esper.codegen.model.expression.CodegenExpressionBuilder.*;
import static com.espertech.esper.codegen.model.expression.CodegenExpressionRelational.CodegenRelational.*;

public class ReformatBetweenNonConstantParamsForgeOp implements ReformatOp {

    private final ReformatBetweenNonConstantParamsForge forge;
    private final ExprEvaluator startEval;
    private final ExprEvaluator endEval;
    private final ExprEvaluator evalIncludeLow;
    private final ExprEvaluator evalIncludeHigh;

    public ReformatBetweenNonConstantParamsForgeOp(ReformatBetweenNonConstantParamsForge forge, ExprEvaluator startEval, ExprEvaluator endEval, ExprEvaluator evalIncludeLow, ExprEvaluator evalIncludeHigh) {
        this.forge = forge;
        this.startEval = startEval;
        this.endEval = endEval;
        this.evalIncludeLow = evalIncludeLow;
        this.evalIncludeHigh = evalIncludeHigh;
    }

    public Object evaluate(Long ts, EventBean[] eventsPerStream, boolean newData, ExprEvaluatorContext exprEvaluatorContext) {
        if (ts == null) {
            return null;
        }
        return evaluateInternal(ts, eventsPerStream, newData, exprEvaluatorContext);
    }

    public static CodegenExpression codegenLong(ReformatBetweenNonConstantParamsForge forge, CodegenExpression inner, CodegenParamSetExprPremade params, CodegenContext context) {
        return codegenLongInternal(forge, inner, params, context);
    }

    public Object evaluate(Date d, EventBean[] eventsPerStream, boolean newData, ExprEvaluatorContext exprEvaluatorContext) {
        if (d == null) {
            return null;
        }
        return evaluateInternal(d.getTime(), eventsPerStream, newData, exprEvaluatorContext);
    }

    public static CodegenExpression codegenDate(ReformatBetweenNonConstantParamsForge forge, CodegenExpression inner, CodegenParamSetExprPremade params, CodegenContext context) {
        CodegenMethodId method = context.addMethod(Boolean.class, ReformatBetweenNonConstantParamsForgeOp.class).add(Date.class, "d").add(params).begin()
                .ifRefNullReturnNull("d")
                .methodReturn(codegenLongInternal(forge, exprDotMethod(ref("d"), "getTime"), params, context));
        return localMethodBuild(method).pass(inner).passAll(params).call();
    }

    public Object evaluate(Calendar cal, EventBean[] eventsPerStream, boolean newData, ExprEvaluatorContext exprEvaluatorContext) {
        if (cal == null) {
            return null;
        }
        return evaluateInternal(cal.getTimeInMillis(), eventsPerStream, newData, exprEvaluatorContext);
    }

    public static CodegenExpression codegenCal(ReformatBetweenNonConstantParamsForge forge, CodegenExpression inner, CodegenParamSetExprPremade params, CodegenContext context) {
        CodegenMethodId method = context.addMethod(Boolean.class, ReformatBetweenNonConstantParamsForgeOp.class).add(Calendar.class, "cal").add(params).begin()
                .ifRefNullReturnNull("cal")
                .methodReturn(codegenLongInternal(forge, exprDotMethod(ref("cal"), "getTimeInMillis"), params, context));
        return localMethodBuild(method).pass(inner).passAll(params).call();
    }

    public Object evaluate(LocalDateTime ldt, EventBean[] eventsPerStream, boolean newData, ExprEvaluatorContext exprEvaluatorContext) {
        return evaluateInternal(DatetimeLongCoercerLocalDateTime.coerceLDTToMilliWTimezone(ldt, forge.timeZone), eventsPerStream, newData, exprEvaluatorContext);
    }

    public static CodegenExpression codegenLDT(ReformatBetweenNonConstantParamsForge forge, CodegenExpression inner, CodegenParamSetExprPremade params, CodegenContext context) {
        CodegenMember tz = context.makeAddMember(TimeZone.class, forge.timeZone);
        return codegenLongInternal(forge, staticMethod(DatetimeLongCoercerLocalDateTime.class, "coerceLDTToMilliWTimezone", inner, member(tz.getMemberId())), params, context);
    }

    public Object evaluate(ZonedDateTime zdt, EventBean[] eventsPerStream, boolean newData, ExprEvaluatorContext exprEvaluatorContext) {
        return evaluateInternal(DatetimeLongCoercerZonedDateTime.coerceZDTToMillis(zdt), eventsPerStream, newData, exprEvaluatorContext);
    }

    public static CodegenExpression codegenZDT(ReformatBetweenNonConstantParamsForge forge, CodegenExpression inner, CodegenParamSetExprPremade params, CodegenContext context) {
        return codegenLongInternal(forge, staticMethod(DatetimeLongCoercerZonedDateTime.class, "coerceZDTToMillis", inner), params, context);
    }

    public Object evaluateInternal(long ts, EventBean[] eventsPerStream, boolean newData, ExprEvaluatorContext exprEvaluatorContext) {
        Object firstObj = startEval.evaluate(eventsPerStream, newData, exprEvaluatorContext);
        if (firstObj == null) {
            return null;
        }
        Object secondObj = endEval.evaluate(eventsPerStream, newData, exprEvaluatorContext);
        if (secondObj == null) {
            return null;
        }
        long first = forge.startCoercer.coerce(firstObj);
        long second = forge.secondCoercer.coerce(secondObj);
        if (forge.includeBoth) {
            if (first <= second) {
                return first <= ts && ts <= second;
            } else {
                return second <= ts && ts <= first;
            }
        } else {

            boolean includeLowEndpoint;
            if (forge.includeLow != null) {
                includeLowEndpoint = forge.includeLow;
            } else {
                Object value = evalIncludeLow.evaluate(eventsPerStream, newData, exprEvaluatorContext);
                if (value == null) {
                    return null;
                }
                includeLowEndpoint = (Boolean) value;
            }

            boolean includeHighEndpoint;
            if (forge.includeHigh != null) {
                includeHighEndpoint = forge.includeHigh;
            } else {
                Object value = evalIncludeHigh.evaluate(eventsPerStream, newData, exprEvaluatorContext);
                if (value == null) {
                    return null;
                }
                includeHighEndpoint = (Boolean) value;
            }

            return compareTimestamps(first, ts, second, includeLowEndpoint, includeHighEndpoint);
        }
    }

    /**
     * NOTE: Code-generation-invoked method, method name and parameter order matters
     *
     * @param first               first
     * @param ts                  ts
     * @param second              second
     * @param includeLowEndpoint  flag
     * @param includeHighEndpoint flag
     * @return result
     */
    public static boolean compareTimestamps(long first, long ts, long second, boolean includeLowEndpoint, boolean includeHighEndpoint) {
        if (includeLowEndpoint) {
            if (ts < first) {
                return false;
            }
        } else {
            if (ts <= first) {
                return false;
            }
        }

        if (includeHighEndpoint) {
            if (ts > second) {
                return false;
            }
        } else {
            if (ts >= second) {
                return false;
            }
        }

        return true;
    }

    private static CodegenExpression codegenLongInternal(ReformatBetweenNonConstantParamsForge forge, CodegenExpression inner, CodegenParamSetExprPremade params, CodegenContext context) {
        CodegenBlock block = context.addMethod(Boolean.class, ReformatBetweenNonConstantParamsForgeOp.class).add(long.class, "ts").add(params).begin();
        codegenLongCoercion(block, "first", forge.start, forge.startCoercer, params, context);
        codegenLongCoercion(block, "second", forge.end, forge.secondCoercer, params, context);
        CodegenMethodId method;
        CodegenExpression first = ref("first");
        CodegenExpression second = ref("second");
        CodegenExpression ts = ref("ts");
        if (forge.includeBoth) {
            method = block.ifCondition(relational(first, LE, second))
                    .blockReturn(and(relational(first, LE, ts), relational(ts, LE, second)))
                    .methodReturn(and(relational(second, LE, ts), relational(ts, LE, first)));
        } else if (forge.includeLow != null && forge.includeHigh != null) {
            method = block.ifCondition(relational(ts, forge.includeLow ? LT : LE, first)).blockReturn(constantFalse())
                    .ifCondition(relational(ts, forge.includeHigh ? GT : GE, second)).blockReturn(constantFalse())
                    .methodReturn(constantTrue());
        } else {
            codegenBooleanEval(block, "includeLowEndpoint", forge.includeLow, forge.forgeIncludeLow, params, context);
            codegenBooleanEval(block, "includeLowHighpoint", forge.includeHigh, forge.forgeIncludeHigh, params, context);
            method = block.methodReturn(staticMethod(ReformatBetweenNonConstantParamsForgeOp.class, "compareTimestamps", first, ts, second, ref("includeLowEndpoint"), ref("includeLowHighpoint")));
        }
        return localMethodBuild(method).pass(inner).passAll(params).call();
    }

    private static void codegenBooleanEval(CodegenBlock block, String variable, Boolean preset, ExprForge forge, CodegenParamSetExprPremade params, CodegenContext context) {
        if (preset != null) {
            block.declareVar(boolean.class, variable, constant(preset));
            return;
        }
        if (forge.getEvaluationType() == boolean.class) {
            block.declareVar(boolean.class, variable, forge.evaluateCodegen(params, context));
            return;
        }
        String refname = variable + "Obj";
        block.declareVar(Boolean.class, refname, forge.evaluateCodegen(params, context))
                .ifRefNullReturnNull(refname)
                .declareVar(boolean.class, variable, ref(refname));
    }

    private static void codegenLongCoercion(CodegenBlock block, String variable, ExprNode assignment, DatetimeLongCoercer coercer, CodegenParamSetExprPremade params, CodegenContext context) {
        Class evaluationType = assignment.getForge().getEvaluationType();
        if (evaluationType == long.class) {
            block.declareVar(long.class, variable, assignment.getForge().evaluateCodegen(params, context));
            return;
        }
        String refname = variable + "Obj";
        block.declareVar(evaluationType, refname, assignment.getForge().evaluateCodegen(params, context));
        if (!evaluationType.isPrimitive()) {
            block.ifRefNullReturnNull(refname);
        }
        block.declareVar(long.class, variable, coercer.codegen(ref(refname), evaluationType, context));
    }
}
