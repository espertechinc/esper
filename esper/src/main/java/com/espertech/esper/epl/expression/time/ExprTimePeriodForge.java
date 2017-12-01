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

import com.espertech.esper.client.EPException;
import com.espertech.esper.client.EventBean;
import com.espertech.esper.client.util.TimePeriod;
import com.espertech.esper.codegen.base.CodegenBlock;
import com.espertech.esper.codegen.base.CodegenClassScope;
import com.espertech.esper.codegen.base.CodegenMethodScope;
import com.espertech.esper.codegen.model.expression.CodegenExpression;
import com.espertech.esper.epl.expression.codegen.ExprForgeCodegenSymbol;
import com.espertech.esper.codegen.base.CodegenMethodNode;
import com.espertech.esper.epl.expression.core.*;
import com.espertech.esper.metrics.instrumentation.InstrumentationHelper;
import com.espertech.esper.util.SimpleNumberCoercerFactory;

import java.math.BigDecimal;
import java.math.BigInteger;

import static com.espertech.esper.codegen.model.expression.CodegenExpressionBuilder.*;

/**
 * Expression representing a time period.
 * <p>
 * Child nodes to this expression carry the actual parts and must return a numeric value.
 */
public class ExprTimePeriodForge implements ExprForge {
    private final ExprTimePeriodImpl parent;
    private final boolean hasVariable;
    private final ExprTimePeriodAdder.TimePeriodAdder[] adders;
    private ExprEvaluator[] evaluators;

    public ExprTimePeriodForge(ExprTimePeriodImpl parent, boolean hasVariable, ExprTimePeriodAdder.TimePeriodAdder[] adders) {
        this.parent = parent;
        this.hasVariable = hasVariable;
        this.adders = adders;
    }

    public ExprTimePeriodEvalDeltaConst constEvaluator(ExprEvaluatorContext context) {
        if (evaluators == null) {
            evaluators = ExprNodeUtilityCore.getEvaluatorsNoCompile(parent.getChildNodes());
        }
        if (!parent.isHasMonth() && !parent.isHasYear()) {
            double seconds = evaluateAsSeconds(null, true, context);
            long msec = parent.getTimeAbacus().deltaForSecondsDouble(seconds);
            return new ExprTimePeriodEvalDeltaConstGivenDelta(msec);
        } else {
            int[] values = new int[adders.length];
            for (int i = 0; i < values.length; i++) {
                values[i] = ((Number) evaluators[i].evaluate(null, true, context)).intValue();
            }
            return new ExprTimePeriodEvalDeltaConstGivenCalAdd(adders, values, parent.getTimeZone(), parent.getTimeAbacus());
        }
    }

    public ExprTimePeriodEvalDeltaNonConst nonconstEvaluator() {
        if (!parent.isHasMonth() && !parent.isHasYear()) {
            return new ExprTimePeriodEvalDeltaNonConstMsec(this);
        } else {
            return new ExprTimePeriodEvalDeltaNonConstCalAdd(parent.getTimeZone(), this);
        }
    }

    public TimeAbacus getTimeAbacus() {
        return parent.getTimeAbacus();
    }

    public ExprEvaluator getExprEvaluator() {
        return new ExprEvaluator() {
            public Object evaluate(EventBean[] eventsPerStream, boolean isNewData, ExprEvaluatorContext context) {
                throw new IllegalStateException("Time-Period expression must be evaluated via any of " + ExprTimePeriod.class.getSimpleName() + " interface methods");
            }

        };
    }

    public CodegenExpression evaluateCodegen(Class requiredType, CodegenMethodScope codegenMethodScope, ExprForgeCodegenSymbol exprSymbol, CodegenClassScope codegenClassScope) {
        throw new IllegalStateException("Time period evaluator does not have a code representation");
    }

    public ExprForgeComplexityEnum getComplexity() {
        return ExprForgeComplexityEnum.NOT_APPLICABLE;
    }

    public Class getEvaluationType() {
        return Double.class;
    }

    public boolean isHasVariable() {
        return hasVariable;
    }

    protected ExprTimePeriodAdder.TimePeriodAdder[] getAdders() {
        return adders;
    }

    public ExprTimePeriodImpl getForgeRenderable() {
        return parent;
    }

    public ExprEvaluator[] getEvaluators() {
        if (evaluators == null) {
            evaluators = ExprNodeUtilityCore.getEvaluatorsNoCompile(parent.getChildNodes());
        }
        return evaluators;
    }

    public double evaluateAsSeconds(EventBean[] eventsPerStream, boolean newData, ExprEvaluatorContext context) {
        if (InstrumentationHelper.ENABLED) {
            InstrumentationHelper.get().qExprTimePeriod(parent);
        }
        if (evaluators == null) {
            evaluators = ExprNodeUtilityCore.getEvaluatorsNoCompile(parent.getChildNodes());
        }
        double seconds = 0;
        for (int i = 0; i < adders.length; i++) {
            Double result = eval(evaluators[i], eventsPerStream, newData, context);
            if (result == null) {
                if (InstrumentationHelper.ENABLED) {
                    InstrumentationHelper.get().aExprTimePeriod(null);
                }
                throw makeTimePeriodParamNullException(ExprNodeUtilityCore.toExpressionStringMinPrecedenceSafe(this.parent));
            }
            seconds += adders[i].compute(result);
        }
        if (InstrumentationHelper.ENABLED) {
            InstrumentationHelper.get().aExprTimePeriod(seconds);
        }
        return seconds;
    }

    public CodegenExpression evaluateAsSecondsCodegen(CodegenMethodScope codegenMethodScope, ExprForgeCodegenSymbol exprSymbol, CodegenClassScope codegenClassScope) {
        CodegenMethodNode methodNode = codegenMethodScope.makeChild(double.class, ExprTimePeriodForge.class, codegenClassScope);

        CodegenBlock block = methodNode.getBlock()
                .declareVar(double.class, "seconds", constant(0))
                .declareVarNoInit(Double.class, "result");
        for (int i = 0; i < parent.getChildNodes().length; i++) {
            ExprForge forge = parent.getChildNodes()[i].getForge();
            Class evaluationType = forge.getEvaluationType();
            block.assignRef("result", SimpleNumberCoercerFactory.SimpleNumberCoercerDouble.codegenDoubleMayNullBoxedIncludeBig(forge.evaluateCodegen(evaluationType, methodNode, exprSymbol, codegenClassScope), evaluationType, methodNode, codegenClassScope));
            block.ifRefNull("result").blockThrow(staticMethod(ExprTimePeriodForge.class, "makeTimePeriodParamNullException", constant(ExprNodeUtilityCore.toExpressionStringMinPrecedenceSafe(this.parent))));
            block.assignRef("seconds", op(ref("seconds"), "+", adders[i].computeCodegen(ref("result"))));
        }
        block.methodReturn(ref("seconds"));
        return localMethod(methodNode);
    }

    private Double eval(ExprEvaluator expr, EventBean[] events, boolean isNewData, ExprEvaluatorContext exprEvaluatorContext) {
        Object value = expr.evaluate(events, isNewData, exprEvaluatorContext);
        if (value == null) {
            return null;
        }
        if (value instanceof BigDecimal) {
            return ((Number) value).doubleValue();
        }
        if (value instanceof BigInteger) {
            return ((Number) value).doubleValue();
        }
        return ((Number) value).doubleValue();
    }

    public TimePeriod evaluateGetTimePeriod(EventBean[] eventsPerStream, boolean newData, ExprEvaluatorContext context) {
        if (evaluators == null) {
            evaluators = ExprNodeUtilityCore.getEvaluatorsNoCompile(parent.getChildNodes());
        }

        int exprCtr = 0;

        Integer year = null;
        if (parent.isHasYear()) {
            year = getInt(evaluators[exprCtr++].evaluate(eventsPerStream, newData, context));
        }

        Integer month = null;
        if (parent.isHasMonth()) {
            month = getInt(evaluators[exprCtr++].evaluate(eventsPerStream, newData, context));
        }

        Integer week = null;
        if (parent.isHasWeek()) {
            week = getInt(evaluators[exprCtr++].evaluate(eventsPerStream, newData, context));
        }

        Integer day = null;
        if (parent.isHasDay()) {
            day = getInt(evaluators[exprCtr++].evaluate(eventsPerStream, newData, context));
        }

        Integer hours = null;
        if (parent.isHasHour()) {
            hours = getInt(evaluators[exprCtr++].evaluate(eventsPerStream, newData, context));
        }

        Integer minutes = null;
        if (parent.isHasMinute()) {
            minutes = getInt(evaluators[exprCtr++].evaluate(eventsPerStream, newData, context));
        }

        Integer seconds = null;
        if (parent.isHasSecond()) {
            seconds = getInt(evaluators[exprCtr++].evaluate(eventsPerStream, newData, context));
        }

        Integer milliseconds = null;
        if (parent.isHasMillisecond()) {
            milliseconds = getInt(evaluators[exprCtr++].evaluate(eventsPerStream, newData, context));
        }

        Integer microseconds = null;
        if (parent.isHasMicrosecond()) {
            microseconds = getInt(evaluators[exprCtr].evaluate(eventsPerStream, newData, context));
        }
        return new TimePeriod(year, month, week, day, hours, minutes, seconds, milliseconds, microseconds);
    }

    public CodegenExpression evaluateGetTimePeriodCodegen(CodegenMethodScope codegenMethodScope, ExprForgeCodegenSymbol exprSymbol, CodegenClassScope codegenClassScope) {
        CodegenMethodNode methodNode = codegenMethodScope.makeChild(TimePeriod.class, ExprTimePeriodForge.class, codegenClassScope);

        CodegenBlock block = methodNode.getBlock();
        int counter = 0;
        counter += evaluateGetTimePeriodCodegenField(block, "year", parent.isHasYear(), counter, methodNode, exprSymbol, codegenClassScope);
        counter += evaluateGetTimePeriodCodegenField(block, "month", parent.isHasMonth(), counter, methodNode, exprSymbol, codegenClassScope);
        counter += evaluateGetTimePeriodCodegenField(block, "week", parent.isHasWeek(), counter, methodNode, exprSymbol, codegenClassScope);
        counter += evaluateGetTimePeriodCodegenField(block, "day", parent.isHasDay(), counter, methodNode, exprSymbol, codegenClassScope);
        counter += evaluateGetTimePeriodCodegenField(block, "hours", parent.isHasHour(), counter, methodNode, exprSymbol, codegenClassScope);
        counter += evaluateGetTimePeriodCodegenField(block, "minutes", parent.isHasMinute(), counter, methodNode, exprSymbol, codegenClassScope);
        counter += evaluateGetTimePeriodCodegenField(block, "seconds", parent.isHasSecond(), counter, methodNode, exprSymbol, codegenClassScope);
        counter += evaluateGetTimePeriodCodegenField(block, "milliseconds", parent.isHasMillisecond(), counter, methodNode, exprSymbol, codegenClassScope);
        evaluateGetTimePeriodCodegenField(block, "microseconds", parent.isHasMicrosecond(), counter, methodNode, exprSymbol, codegenClassScope);
        block.methodReturn(newInstance(TimePeriod.class, ref("year"), ref("month"), ref("week"), ref("day"), ref("hours"), ref("minutes"), ref("seconds"), ref("milliseconds"), ref("microseconds")));
        return localMethod(methodNode);
    }

    /**
     * NOTE: Code-generation-invoked method, method name and parameter order matters
     * @param expressionText text
     * @return exception
     */
    public static EPException makeTimePeriodParamNullException(String expressionText) {
        return new EPException("Failed to evaluate time period, received a null value for '" + expressionText + "'");
    }

    private int evaluateGetTimePeriodCodegenField(CodegenBlock block, String variable, boolean present, int counter, CodegenMethodScope codegenMethodScope, ExprForgeCodegenSymbol exprSymbol, CodegenClassScope codegenClassScope) {
        if (!present) {
            block.declareVar(Integer.class, variable, constantNull());
            return 0;
        }
        ExprForge forge = parent.getChildNodes()[counter].getForge();
        Class evaluationType = forge.getEvaluationType();
        block.declareVar(Integer.class, variable, SimpleNumberCoercerFactory.SimpleNumberCoercerInt.coerceCodegenMayNull(forge.evaluateCodegen(evaluationType, codegenMethodScope, exprSymbol, codegenClassScope), forge.getEvaluationType(), codegenMethodScope, codegenClassScope));
        return 1;
    }

    private Integer getInt(Object evaluated) {
        if (evaluated == null) {
            return null;
        }
        return ((Number) evaluated).intValue();
    }
}
