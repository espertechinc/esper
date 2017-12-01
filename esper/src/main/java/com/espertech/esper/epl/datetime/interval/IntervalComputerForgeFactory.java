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

import com.espertech.esper.client.EventBean;
import com.espertech.esper.codegen.base.CodegenBlock;
import com.espertech.esper.codegen.base.CodegenClassScope;
import com.espertech.esper.codegen.base.CodegenMethodNode;
import com.espertech.esper.codegen.base.CodegenMethodScope;
import com.espertech.esper.codegen.model.expression.CodegenExpression;
import com.espertech.esper.epl.datetime.eval.DatetimeMethodEnum;
import com.espertech.esper.epl.datetime.interval.deltaexpr.IntervalDeltaExprMSecConstForge;
import com.espertech.esper.epl.datetime.interval.deltaexpr.IntervalDeltaExprTimePeriodConstForge;
import com.espertech.esper.epl.datetime.interval.deltaexpr.IntervalDeltaExprTimePeriodNonConstForge;
import com.espertech.esper.epl.expression.codegen.ExprForgeCodegenSymbol;
import com.espertech.esper.epl.expression.core.*;
import com.espertech.esper.epl.expression.time.ExprTimePeriod;
import com.espertech.esper.epl.expression.time.ExprTimePeriodEvalDeltaConst;
import com.espertech.esper.epl.expression.time.ExprTimePeriodEvalDeltaNonConst;
import com.espertech.esper.epl.expression.time.TimeAbacus;
import com.espertech.esper.util.SimpleNumberCoercerFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;

import static com.espertech.esper.codegen.model.expression.CodegenExpressionBuilder.*;
import static com.espertech.esper.codegen.model.expression.CodegenExpressionRelational.CodegenRelational.*;

public class IntervalComputerForgeFactory {

    public static IntervalComputerForge make(DatetimeMethodEnum method, List<ExprNode> expressions, TimeAbacus timeAbacus) throws ExprValidationException {
        ExprOptionalConstantForge[] parameters = getParameters(expressions, timeAbacus);

        if (method == DatetimeMethodEnum.BEFORE) {
            if (parameters.length == 0) {
                return new IntervalComputerBeforeNoParamForge();
            }
            IntervalStartEndParameterPairForge pair = IntervalStartEndParameterPairForge.fromParamsWithLongMaxEnd(parameters);
            if (pair.isConstant()) {
                return new IntervalComputerConstantBefore(pair);
            }
            return new IntervalComputerBeforeWithDeltaExprForge(pair);
        } else if (method == DatetimeMethodEnum.AFTER) {
            if (parameters.length == 0) {
                return new IntervalComputerAfterNoParam();
            }
            IntervalStartEndParameterPairForge pair = IntervalStartEndParameterPairForge.fromParamsWithLongMaxEnd(parameters);
            if (pair.isConstant()) {
                return new IntervalComputerConstantAfter(pair);
            }
            return new IntervalComputerAfterWithDeltaExprForge(pair);
        } else if (method == DatetimeMethodEnum.COINCIDES) {
            if (parameters.length == 0) {
                return new IntervalComputerCoincidesNoParam();
            }
            IntervalStartEndParameterPairForge pair = IntervalStartEndParameterPairForge.fromParamsWithSameEnd(parameters);
            if (pair.isConstant()) {
                return new IntervalComputerConstantCoincides(pair);
            }
            return new IntervalComputerCoincidesWithDeltaExprForge(pair);
        } else if (method == DatetimeMethodEnum.DURING || method == DatetimeMethodEnum.INCLUDES) {
            if (parameters.length == 0) {
                if (method == DatetimeMethodEnum.DURING) {
                    return new IntervalComputerDuringNoParam();
                }
                return new IntervalComputerIncludesNoParam();
            }
            IntervalStartEndParameterPairForge pair = IntervalStartEndParameterPairForge.fromParamsWithSameEnd(parameters);
            if (parameters.length == 1) {
                return new IntervalComputerDuringAndIncludesThresholdForge(method == DatetimeMethodEnum.DURING, pair.getStart().getForge());
            }
            if (parameters.length == 2) {
                return new IntervalComputerDuringAndIncludesMinMax(method == DatetimeMethodEnum.DURING, pair.getStart().getForge(), pair.getEnd().getForge());
            }
            return new IntervalComputerDuringMinMaxStartEndForge(method == DatetimeMethodEnum.DURING, getEvaluators(expressions, timeAbacus));
        } else if (method == DatetimeMethodEnum.FINISHES) {
            if (parameters.length == 0) {
                return new IntervalComputerFinishesNoParam();
            }
            validateConstantThreshold("finishes", parameters[0]);
            return new IntervalComputerFinishesThresholdForge(parameters[0].getForge());
        } else if (method == DatetimeMethodEnum.FINISHEDBY) {
            if (parameters.length == 0) {
                return new IntervalComputerFinishedByNoParam();
            }
            validateConstantThreshold("finishedby", parameters[0]);
            return new IntervalComputerFinishedByThresholdForge(parameters[0].getForge());
        } else if (method == DatetimeMethodEnum.MEETS) {
            if (parameters.length == 0) {
                return new IntervalComputerMeetsNoParam();
            }
            validateConstantThreshold("meets", parameters[0]);
            return new IntervalComputerMeetsThresholdForge(parameters[0].getForge());
        } else if (method == DatetimeMethodEnum.METBY) {
            if (parameters.length == 0) {
                return new IntervalComputerMetByNoParam();
            }
            validateConstantThreshold("metBy", parameters[0]);
            return new IntervalComputerMetByThresholdForge(parameters[0].getForge());
        } else if (method == DatetimeMethodEnum.OVERLAPS || method == DatetimeMethodEnum.OVERLAPPEDBY) {
            if (parameters.length == 0) {
                if (method == DatetimeMethodEnum.OVERLAPS) {
                    return new IntervalComputerOverlapsNoParam();
                }
                return new IntervalComputerOverlappedByNoParam();
            }
            if (parameters.length == 1) {
                return new IntervalComputerOverlapsAndByThreshold(method == DatetimeMethodEnum.OVERLAPS, parameters[0].getForge());
            }
            return new IntervalComputerOverlapsAndByMinMaxForge(method == DatetimeMethodEnum.OVERLAPS, parameters[0].getForge(), parameters[1].getForge());
        } else if (method == DatetimeMethodEnum.STARTS) {
            if (parameters.length == 0) {
                return new IntervalComputerStartsNoParam();
            }
            validateConstantThreshold("starts", parameters[0]);
            return new IntervalComputerStartsThresholdForge(parameters[0].getForge());
        } else if (method == DatetimeMethodEnum.STARTEDBY) {
            if (parameters.length == 0) {
                return new IntervalComputerStartedByNoParam();
            }
            validateConstantThreshold("startedBy", parameters[0]);
            return new IntervalComputerStartedByThresholdForge(parameters[0].getForge());
        }
        throw new IllegalArgumentException("Unknown datetime method '" + method + "'");
    }

    private static void validateConstantThreshold(String method, ExprOptionalConstantForge param) throws ExprValidationException {
        if (param.getOptionalConstant() != null && (param.getOptionalConstant()).longValue() < 0) {
            throw new ExprValidationException("The " + method + " date-time method does not allow negative threshold value");
        }
    }

    private static ExprOptionalConstantForge[] getParameters(List<ExprNode> expressions, TimeAbacus timeAbacus) {
        ExprOptionalConstantForge[] parameters = new ExprOptionalConstantForge[expressions.size() - 1];
        for (int i = 1; i < expressions.size(); i++) {
            parameters[i - 1] = getExprOrConstant(expressions.get(i), timeAbacus);
        }
        return parameters;
    }

    private static IntervalDeltaExprForge[] getEvaluators(List<ExprNode> expressions, TimeAbacus timeAbacus) {
        IntervalDeltaExprForge[] parameters = new IntervalDeltaExprForge[expressions.size() - 1];
        for (int i = 1; i < expressions.size(); i++) {
            parameters[i - 1] = getExprOrConstant(expressions.get(i), timeAbacus).getForge();
        }
        return parameters;
    }

    private static ExprOptionalConstantForge getExprOrConstant(ExprNode exprNode, final TimeAbacus timeAbacus) {
        if (exprNode instanceof ExprTimePeriod) {
            final ExprTimePeriod timePeriod = (ExprTimePeriod) exprNode;
            if (!timePeriod.isHasMonth() && !timePeriod.isHasYear()) {
                // no-month and constant
                if (exprNode.isConstantResult()) {
                    double sec = timePeriod.evaluateAsSeconds(null, true, null);
                    long l = timeAbacus.deltaForSecondsDouble(sec);
                    return new ExprOptionalConstantForge(new IntervalDeltaExprMSecConstForge(l), l);
                } else {
                    return new ExprOptionalConstantForge(new IntervalDeltaExprTimePeriodNonConstForge(timePeriod, timeAbacus), null);
                }
            } else {
                // has-month and constant
                if (exprNode.isConstantResult()) {
                    ExprTimePeriodEvalDeltaConst timerPeriodConst = timePeriod.constEvaluator(null);
                    return new ExprOptionalConstantForge(new IntervalDeltaExprTimePeriodConstForge(timerPeriodConst), null);
                } else {
                    // has-month and not constant
                    final ExprTimePeriodEvalDeltaNonConst timerPeriodNonConst = timePeriod.nonconstEvaluator();
                    IntervalDeltaExprForge forge = new IntervalDeltaExprForge() {
                        public IntervalDeltaExprEvaluator makeEvaluator() {
                            return new IntervalDeltaExprEvaluator() {
                                public long evaluate(long reference, EventBean[] eventsPerStream, boolean isNewData, ExprEvaluatorContext context) {
                                    return timerPeriodNonConst.deltaAdd(reference, eventsPerStream, isNewData, context);
                                }
                            };
                        }

                        public CodegenExpression codegen(CodegenExpression reference, CodegenMethodScope codegenMethodScope, ExprForgeCodegenSymbol exprSymbol, CodegenClassScope codegenClassScope) {
                            return timerPeriodNonConst.deltaAddCodegen(reference, codegenMethodScope, exprSymbol, codegenClassScope);
                        }
                    };
                    return new ExprOptionalConstantForge(forge, null);
                }
            }
        } else if (ExprNodeUtilityCore.isConstantValueExpr(exprNode)) {
            ExprConstantNode constantNode = (ExprConstantNode) exprNode;
            long l = ((Number) constantNode.getConstantValue(null)).longValue();
            return new ExprOptionalConstantForge(new IntervalDeltaExprMSecConstForge(l), l);
        } else {
            final ExprForge forge = exprNode.getForge();
            IntervalDeltaExprForge eval = new IntervalDeltaExprForge() {
                public IntervalDeltaExprEvaluator makeEvaluator() {
                    final ExprEvaluator evaluator = forge.getExprEvaluator();
                    return new IntervalDeltaExprEvaluator() {
                        public long evaluate(long reference, EventBean[] eventsPerStream, boolean isNewData, ExprEvaluatorContext context) {
                            return ((Number) evaluator.evaluate(eventsPerStream, isNewData, context)).longValue();
                        }
                    };
                }

                public CodegenExpression codegen(CodegenExpression reference, CodegenMethodScope codegenMethodScope, ExprForgeCodegenSymbol exprSymbol, CodegenClassScope codegenClassScope) {
                    return SimpleNumberCoercerFactory.SimpleNumberCoercerLong.codegenLong(forge.evaluateCodegen(forge.getEvaluationType(), codegenMethodScope, exprSymbol, codegenClassScope), forge.getEvaluationType());
                }
            };
            return new ExprOptionalConstantForge(eval, null);
        }
    }

    /**
     * After.
     */
    public static class IntervalComputerConstantAfter extends IntervalComputerConstantBase implements IntervalComputerForge, IntervalComputerEval {

        public IntervalComputerConstantAfter(IntervalStartEndParameterPairForge pair) {
            super(pair, true);
        }

        public IntervalComputerEval makeComputerEval() {
            return this;
        }

        public CodegenExpression codegen(CodegenExpression leftStart, CodegenExpression leftEnd, CodegenExpression rightStart, CodegenExpression rightEnd, CodegenMethodScope codegenMethodScope, ExprForgeCodegenSymbol exprSymbol, CodegenClassScope codegenClassScope) {
            return staticMethod(IntervalComputerConstantAfter.class, "computeIntervalAfter", leftStart, rightEnd, constant(start), constant(end));
        }

        public Boolean compute(long leftStart, long leftEnd, long rightStart, long rightEnd, EventBean[] eventsPerStream, boolean newData, ExprEvaluatorContext context) {
            return computeIntervalAfter(leftStart, rightEnd, start, end);
        }

        public static boolean computeIntervalAfter(long leftStart, long rightEnd, long start, long end) {
            long delta = leftStart - rightEnd;
            return start <= delta && delta <= end;
        }
    }

    public static class IntervalComputerAfterWithDeltaExprForge implements IntervalComputerForge {

        private final IntervalDeltaExprForge start;
        private final IntervalDeltaExprForge finish;

        public IntervalComputerAfterWithDeltaExprForge(IntervalStartEndParameterPairForge pair) {
            this.start = pair.getStart().getForge();
            this.finish = pair.getEnd().getForge();
        }

        public IntervalComputerEval makeComputerEval() {
            return new IntervalComputerAfterWithDeltaExprEval(start.makeEvaluator(), finish.makeEvaluator());
        }

        public CodegenExpression codegen(CodegenExpression leftStart, CodegenExpression leftEnd, CodegenExpression rightStart, CodegenExpression rightEnd, CodegenMethodScope codegenMethodScope, ExprForgeCodegenSymbol exprSymbol, CodegenClassScope codegenClassScope) {
            return IntervalComputerAfterWithDeltaExprEval.codegen(this, leftStart, leftEnd, rightStart, rightEnd, codegenMethodScope, exprSymbol, codegenClassScope);
        }
    }

    public static class IntervalComputerAfterWithDeltaExprEval implements IntervalComputerEval {

        private final IntervalDeltaExprEvaluator start;
        private final IntervalDeltaExprEvaluator finish;

        public IntervalComputerAfterWithDeltaExprEval(IntervalDeltaExprEvaluator start, IntervalDeltaExprEvaluator finish) {
            this.start = start;
            this.finish = finish;
        }

        public Boolean compute(long leftStart, long leftEnd, long rightStart, long rightEnd, EventBean[] eventsPerStream, boolean newData, ExprEvaluatorContext context) {
            long rangeStartDelta = start.evaluate(rightStart, eventsPerStream, newData, context);
            long rangeEndDelta = finish.evaluate(rightStart, eventsPerStream, newData, context);
            if (rangeStartDelta > rangeEndDelta) {
                return IntervalComputerConstantAfter.computeIntervalAfter(leftStart, rightEnd, rangeEndDelta, rangeStartDelta);
            } else {
                return IntervalComputerConstantAfter.computeIntervalAfter(leftStart, rightEnd, rangeStartDelta, rangeEndDelta);
            }
        }

        public static CodegenExpression codegen(IntervalComputerAfterWithDeltaExprForge forge, CodegenExpression leftStart, CodegenExpression leftEnd, CodegenExpression rightStart, CodegenExpression rightEnd, CodegenMethodScope codegenMethodScope, ExprForgeCodegenSymbol exprSymbol, CodegenClassScope codegenClassScope) {
            CodegenMethodNode methodNode = codegenMethodScope.makeChild(boolean.class, IntervalComputerAfterWithDeltaExprEval.class, codegenClassScope).addParam(IntervalForgeCodegenNames.PARAMS);

            CodegenBlock block = methodNode.getBlock()
                    .declareVar(long.class, "rangeStartDelta", forge.start.codegen(IntervalForgeCodegenNames.REF_RIGHTSTART, methodNode, exprSymbol, codegenClassScope))
                    .declareVar(long.class, "rangeEndDelta", forge.finish.codegen(IntervalForgeCodegenNames.REF_RIGHTSTART, methodNode, exprSymbol, codegenClassScope));
            block.ifCondition(relational(ref("rangeStartDelta"), GT, ref("rangeEndDelta")))
                    .blockReturn(staticMethod(IntervalComputerConstantAfter.class, "computeIntervalAfter", IntervalForgeCodegenNames.REF_LEFTSTART, IntervalForgeCodegenNames.REF_RIGHTEND, ref("rangeEndDelta"), ref("rangeStartDelta")));
            block.methodReturn(staticMethod(IntervalComputerConstantAfter.class, "computeIntervalAfter", IntervalForgeCodegenNames.REF_LEFTSTART, IntervalForgeCodegenNames.REF_RIGHTEND, ref("rangeStartDelta"), ref("rangeEndDelta")));
            return localMethod(methodNode, leftStart, leftEnd, rightStart, rightEnd);
        }
    }

    public static class IntervalComputerAfterNoParam implements IntervalComputerForge, IntervalComputerEval {

        public Boolean compute(long leftStart, long leftEnd, long rightStart, long rightEnd, EventBean[] eventsPerStream, boolean newData, ExprEvaluatorContext context) {
            return leftStart > rightEnd;
        }

        public IntervalComputerEval makeComputerEval() {
            return this;
        }

        public CodegenExpression codegen(CodegenExpression leftStart, CodegenExpression leftEnd, CodegenExpression rightStart, CodegenExpression rightEnd, CodegenMethodScope codegenMethodScope, ExprForgeCodegenSymbol exprSymbol, CodegenClassScope codegenClassScope) {
            return relational(leftStart, GT, rightEnd);
        }
    }

    /**
     * Before.
     */
    public static class IntervalComputerConstantBefore extends IntervalComputerConstantBase implements IntervalComputerForge, IntervalComputerEval {

        public IntervalComputerConstantBefore(IntervalStartEndParameterPairForge pair) {
            super(pair, true);
        }

        public IntervalComputerEval makeComputerEval() {
            return this;
        }

        public CodegenExpression codegen(CodegenExpression leftStart, CodegenExpression leftEnd, CodegenExpression rightStart, CodegenExpression rightEnd, CodegenMethodScope codegenMethodScope, ExprForgeCodegenSymbol exprSymbol, CodegenClassScope codegenClassScope) {
            return staticMethod(IntervalComputerConstantBefore.class, "computeIntervalBefore", leftEnd, rightStart, constant(start), constant(end));
        }

        public Boolean compute(long leftStart, long leftEnd, long rightStart, long rightEnd, EventBean[] eventsPerStream, boolean newData, ExprEvaluatorContext context) {
            return computeIntervalBefore(leftEnd, rightStart, start, end);
        }

        /**
         * NOTE: Code-generation-invoked method, method name and parameter order matters
         *
         * @param leftEnd left end
         * @param right   right
         * @param start   start
         * @param end     end
         * @return flag
         */
        public static boolean computeIntervalBefore(long leftEnd, long right, long start, long end) {
            long delta = right - leftEnd;
            return start <= delta && delta <= end;
        }
    }

    public static class IntervalComputerBeforeWithDeltaExprForge implements IntervalComputerForge {

        protected final IntervalDeltaExprForge start;
        protected final IntervalDeltaExprForge finish;

        public IntervalComputerBeforeWithDeltaExprForge(IntervalStartEndParameterPairForge pair) {
            this.start = pair.getStart().getForge();
            this.finish = pair.getEnd().getForge();
        }

        public IntervalComputerEval makeComputerEval() {
            return new IntervalComputerBeforeWithDeltaExprEval(start.makeEvaluator(), finish.makeEvaluator());
        }

        public CodegenExpression codegen(CodegenExpression leftStart, CodegenExpression leftEnd, CodegenExpression rightStart, CodegenExpression rightEnd, CodegenMethodScope codegenMethodScope, ExprForgeCodegenSymbol exprSymbol, CodegenClassScope codegenClassScope) {
            return IntervalComputerBeforeWithDeltaExprEval.codegen(this, leftStart, leftEnd, rightStart, rightEnd, codegenMethodScope, exprSymbol, codegenClassScope);
        }
    }

    public static class IntervalComputerBeforeWithDeltaExprEval implements IntervalComputerEval {

        private final IntervalDeltaExprEvaluator start;
        private final IntervalDeltaExprEvaluator finish;

        public IntervalComputerBeforeWithDeltaExprEval(IntervalDeltaExprEvaluator start, IntervalDeltaExprEvaluator finish) {
            this.start = start;
            this.finish = finish;
        }

        public Boolean compute(long leftStart, long leftEnd, long rightStart, long rightEnd, EventBean[] eventsPerStream, boolean newData, ExprEvaluatorContext context) {
            long rangeStartDelta = start.evaluate(leftEnd, eventsPerStream, newData, context);
            long rangeEndDelta = finish.evaluate(leftEnd, eventsPerStream, newData, context);
            if (rangeStartDelta > rangeEndDelta) {
                return IntervalComputerConstantBefore.computeIntervalBefore(leftEnd, rightStart, rangeEndDelta, rangeStartDelta);
            }
            return IntervalComputerConstantBefore.computeIntervalBefore(leftEnd, rightStart, rangeStartDelta, rangeEndDelta);
        }

        public static CodegenExpression codegen(IntervalComputerBeforeWithDeltaExprForge forge, CodegenExpression leftStart, CodegenExpression leftEnd, CodegenExpression rightStart, CodegenExpression rightEnd, CodegenMethodScope codegenMethodScope, ExprForgeCodegenSymbol exprSymbol, CodegenClassScope codegenClassScope) {
            CodegenMethodNode methodNode = codegenMethodScope.makeChild(boolean.class, IntervalComputerBeforeWithDeltaExprEval.class, codegenClassScope).addParam(IntervalForgeCodegenNames.PARAMS);

            CodegenBlock block = methodNode.getBlock()
                    .declareVar(long.class, "rangeStartDelta", forge.start.codegen(IntervalForgeCodegenNames.REF_LEFTEND, methodNode, exprSymbol, codegenClassScope))
                    .declareVar(long.class, "rangeEndDelta", forge.finish.codegen(IntervalForgeCodegenNames.REF_LEFTEND, methodNode, exprSymbol, codegenClassScope));
            block.ifCondition(relational(ref("rangeStartDelta"), GT, ref("rangeEndDelta")))
                    .blockReturn(staticMethod(IntervalComputerConstantBefore.class, "computeIntervalBefore", IntervalForgeCodegenNames.REF_LEFTEND, IntervalForgeCodegenNames.REF_RIGHTSTART, ref("rangeEndDelta"), ref("rangeStartDelta")));
            block.methodReturn(staticMethod(IntervalComputerConstantBefore.class, "computeIntervalBefore", IntervalForgeCodegenNames.REF_LEFTEND, IntervalForgeCodegenNames.REF_RIGHTSTART, ref("rangeStartDelta"), ref("rangeEndDelta")));
            return localMethod(methodNode, leftStart, leftEnd, rightStart, rightEnd);
        }
    }

    public static class IntervalComputerBeforeNoParamForge implements IntervalComputerForge, IntervalComputerEval {

        public IntervalComputerEval makeComputerEval() {
            return this;
        }

        public CodegenExpression codegen(CodegenExpression leftStart, CodegenExpression leftEnd, CodegenExpression rightStart, CodegenExpression rightEnd, CodegenMethodScope codegenMethodScope, ExprForgeCodegenSymbol exprSymbol, CodegenClassScope codegenClassScope) {
            return relational(leftEnd, LT, rightStart);
        }

        public Boolean compute(long leftStart, long leftEnd, long rightStart, long rightEnd, EventBean[] eventsPerStream, boolean newData, ExprEvaluatorContext context) {
            return leftEnd < rightStart;
        }
    }

    /**
     * Coincides.
     */
    public static class IntervalComputerConstantCoincides implements IntervalComputerForge, IntervalComputerEval {

        protected final long start;
        protected final long end;

        public IntervalComputerConstantCoincides(IntervalStartEndParameterPairForge pair) throws ExprValidationException {
            start = pair.getStart().getOptionalConstant();
            end = pair.getEnd().getOptionalConstant();
            if (start < 0 || end < 0) {
                throw new ExprValidationException("The coincides date-time method does not allow negative start and end values");
            }
        }

        public IntervalComputerEval makeComputerEval() {
            return this;
        }

        public CodegenExpression codegen(CodegenExpression leftStart, CodegenExpression leftEnd, CodegenExpression rightStart, CodegenExpression rightEnd, CodegenMethodScope codegenMethodScope, ExprForgeCodegenSymbol exprSymbol, CodegenClassScope codegenClassScope) {
            return staticMethod(IntervalComputerConstantCoincides.class, "computeIntervalCoincides", leftStart, leftEnd, rightStart, rightEnd, constant(start), constant(end));
        }

        public Boolean compute(long leftStart, long leftEnd, long rightStart, long rightEnd, EventBean[] eventsPerStream, boolean newData, ExprEvaluatorContext context) {
            return computeIntervalCoincides(leftStart, leftEnd, rightStart, rightEnd, start, end);
        }

        /**
         * NOTE: Code-generation-invoked method, method name and parameter order matters
         *
         * @param left           left start
         * @param leftEnd        left end
         * @param right          right start
         * @param rightEnd       right end
         * @param startThreshold start th
         * @param endThreshold   end th
         * @return flag
         */
        public static boolean computeIntervalCoincides(long left, long leftEnd, long right, long rightEnd, long startThreshold, long endThreshold) {
            return Math.abs(left - right) <= startThreshold &&
                    Math.abs(leftEnd - rightEnd) <= endThreshold;
        }
    }

    public static class IntervalComputerCoincidesWithDeltaExprForge implements IntervalComputerForge {

        private final IntervalDeltaExprForge start;
        private final IntervalDeltaExprForge finish;

        public IntervalComputerCoincidesWithDeltaExprForge(IntervalStartEndParameterPairForge pair) {
            this.start = pair.getStart().getForge();
            this.finish = pair.getEnd().getForge();
        }

        public IntervalComputerEval makeComputerEval() {
            return new IntervalComputerCoincidesWithDeltaExprEval(start.makeEvaluator(), finish.makeEvaluator());
        }

        public CodegenExpression codegen(CodegenExpression leftStart, CodegenExpression leftEnd, CodegenExpression rightStart, CodegenExpression rightEnd, CodegenMethodScope codegenMethodScope, ExprForgeCodegenSymbol exprSymbol, CodegenClassScope codegenClassScope) {
            return IntervalComputerCoincidesWithDeltaExprEval.codegen(this, leftStart, leftEnd, rightStart, rightEnd, codegenMethodScope, exprSymbol, codegenClassScope);
        }
    }

    public static class IntervalComputerCoincidesWithDeltaExprEval implements IntervalComputerEval {

        private static final Logger log = LoggerFactory.getLogger(IntervalComputerCoincidesWithDeltaExprForge.class);
        public final static String METHOD_WARNCOINCIDESTARTENDLESSZERO = "warnCoincideStartEndLessZero";

        private final IntervalDeltaExprEvaluator start;
        private final IntervalDeltaExprEvaluator finish;

        public IntervalComputerCoincidesWithDeltaExprEval(IntervalDeltaExprEvaluator start, IntervalDeltaExprEvaluator finish) {
            this.start = start;
            this.finish = finish;
        }

        public Boolean compute(long leftStart, long leftEnd, long rightStart, long rightEnd, EventBean[] eventsPerStream, boolean newData, ExprEvaluatorContext context) {
            long startValue = start.evaluate(Math.min(leftStart, rightStart), eventsPerStream, newData, context);
            long endValue = finish.evaluate(Math.min(leftEnd, rightEnd), eventsPerStream, newData, context);

            if (startValue < 0 || endValue < 0) {
                log.warn("The coincides date-time method does not allow negative start and end values");
                return null;
            }

            return IntervalComputerConstantCoincides.computeIntervalCoincides(leftStart, leftEnd, rightStart, rightEnd, startValue, endValue);
        }

        public static CodegenExpression codegen(IntervalComputerCoincidesWithDeltaExprForge forge, CodegenExpression leftStart, CodegenExpression leftEnd, CodegenExpression rightStart, CodegenExpression rightEnd, CodegenMethodScope codegenMethodScope, ExprForgeCodegenSymbol exprSymbol, CodegenClassScope codegenClassScope) {
            CodegenMethodNode methodNode = codegenMethodScope.makeChild(Boolean.class, IntervalComputerCoincidesWithDeltaExprEval.class, codegenClassScope).addParam(IntervalForgeCodegenNames.PARAMS);

            CodegenBlock block = methodNode.getBlock()
                    .declareVar(long.class, "startValue", forge.start.codegen(staticMethod(Math.class, "min", IntervalForgeCodegenNames.REF_LEFTSTART, IntervalForgeCodegenNames.REF_RIGHTSTART), methodNode, exprSymbol, codegenClassScope))
                    .declareVar(long.class, "endValue", forge.finish.codegen(staticMethod(Math.class, "min", IntervalForgeCodegenNames.REF_LEFTEND, IntervalForgeCodegenNames.REF_RIGHTEND), methodNode, exprSymbol, codegenClassScope));
            block.ifCondition(or(relational(ref("startValue"), LT, constant(0)), relational(ref("endValue"), LT, constant(0))))
                    .staticMethod(IntervalComputerCoincidesWithDeltaExprEval.class, METHOD_WARNCOINCIDESTARTENDLESSZERO)
                    .blockReturn(constantNull());
            block.methodReturn(staticMethod(IntervalComputerConstantCoincides.class, "computeIntervalCoincides", IntervalForgeCodegenNames.REF_LEFTSTART, IntervalForgeCodegenNames.REF_LEFTEND, IntervalForgeCodegenNames.REF_RIGHTSTART, IntervalForgeCodegenNames.REF_RIGHTEND, ref("startValue"), ref("endValue")));
            return localMethod(methodNode, leftStart, leftEnd, rightStart, rightEnd);
        }

        /**
         * NOTE: Code-generation-invoked method, method name and parameter order matters
         */
        public static void warnCoincideStartEndLessZero() {
            log.warn("The coincides date-time method does not allow negative start and end values");
        }
    }

    public static class IntervalComputerCoincidesNoParam implements IntervalComputerForge, IntervalComputerEval {

        public IntervalComputerEval makeComputerEval() {
            return this;
        }

        public CodegenExpression codegen(CodegenExpression leftStart, CodegenExpression leftEnd, CodegenExpression rightStart, CodegenExpression rightEnd, CodegenMethodScope codegenMethodScope, ExprForgeCodegenSymbol exprSymbol, CodegenClassScope codegenClassScope) {
            return and(equalsIdentity(leftStart, rightStart), equalsIdentity(leftEnd, rightEnd));
        }

        public Boolean compute(long leftStart, long leftEnd, long rightStart, long rightEnd, EventBean[] eventsPerStream, boolean newData, ExprEvaluatorContext context) {
            return leftStart == rightStart && leftEnd == rightEnd;
        }
    }

    /**
     * During And Includes.
     */
    public static class IntervalComputerDuringNoParam implements IntervalComputerForge, IntervalComputerEval {

        public Boolean compute(long leftStart, long leftEnd, long rightStart, long rightEnd, EventBean[] eventsPerStream, boolean newData, ExprEvaluatorContext context) {
            return rightStart < leftStart && leftEnd < rightEnd;
        }

        public IntervalComputerEval makeComputerEval() {
            return this;
        }

        public CodegenExpression codegen(CodegenExpression leftStart, CodegenExpression leftEnd, CodegenExpression rightStart, CodegenExpression rightEnd, CodegenMethodScope codegenMethodScope, ExprForgeCodegenSymbol exprSymbol, CodegenClassScope codegenClassScope) {
            return and(relational(rightStart, LT, leftStart), relational(leftEnd, LT, rightEnd));
        }
    }

    public static class IntervalComputerIncludesNoParam implements IntervalComputerForge, IntervalComputerEval {

        public IntervalComputerEval makeComputerEval() {
            return this;
        }

        public Boolean compute(long leftStart, long leftEnd, long rightStart, long rightEnd, EventBean[] eventsPerStream, boolean newData, ExprEvaluatorContext context) {
            return leftStart < rightStart && rightEnd < leftEnd;
        }

        public CodegenExpression codegen(CodegenExpression leftStart, CodegenExpression leftEnd, CodegenExpression rightStart, CodegenExpression rightEnd, CodegenMethodScope codegenMethodScope, ExprForgeCodegenSymbol exprSymbol, CodegenClassScope codegenClassScope) {
            return and(relational(leftStart, LT, rightStart), relational(rightEnd, LT, leftEnd));
        }
    }

    public static class IntervalComputerDuringAndIncludesThresholdForge implements IntervalComputerForge {

        private final boolean during;
        private final IntervalDeltaExprForge threshold;

        public IntervalComputerDuringAndIncludesThresholdForge(boolean during, IntervalDeltaExprForge threshold) {
            this.during = during;
            this.threshold = threshold;
        }

        public IntervalComputerEval makeComputerEval() {
            return new IntervalComputerDuringAndIncludesThresholdEval(during, threshold.makeEvaluator());
        }

        public CodegenExpression codegen(CodegenExpression leftStart, CodegenExpression leftEnd, CodegenExpression rightStart, CodegenExpression rightEnd, CodegenMethodScope codegenMethodScope, ExprForgeCodegenSymbol exprSymbol, CodegenClassScope codegenClassScope) {
            return IntervalComputerDuringAndIncludesThresholdEval.codegen(this, leftStart, leftEnd, rightStart, rightEnd, codegenMethodScope, exprSymbol, codegenClassScope);
        }
    }

    public static class IntervalComputerDuringAndIncludesThresholdEval implements IntervalComputerEval {

        private final boolean during;
        private final IntervalDeltaExprEvaluator threshold;

        public IntervalComputerDuringAndIncludesThresholdEval(boolean during, IntervalDeltaExprEvaluator threshold) {
            this.during = during;
            this.threshold = threshold;
        }

        public Boolean compute(long leftStart, long leftEnd, long rightStart, long rightEnd, EventBean[] eventsPerStream, boolean newData, ExprEvaluatorContext context) {

            long thresholdValue = threshold.evaluate(leftStart, eventsPerStream, newData, context);

            if (during) {
                long deltaStart = leftStart - rightStart;
                if (deltaStart <= 0 || deltaStart > thresholdValue) {
                    return false;
                }

                long deltaEnd = rightEnd - leftEnd;
                return !(deltaEnd <= 0 || deltaEnd > thresholdValue);
            } else {
                long deltaStart = rightStart - leftStart;
                if (deltaStart <= 0 || deltaStart > thresholdValue) {
                    return false;
                }

                long deltaEnd = leftEnd - rightEnd;
                return !(deltaEnd <= 0 || deltaEnd > thresholdValue);
            }
        }

        public static CodegenExpression codegen(IntervalComputerDuringAndIncludesThresholdForge forge, CodegenExpression leftStart, CodegenExpression leftEnd, CodegenExpression rightStart, CodegenExpression rightEnd, CodegenMethodScope codegenMethodScope, ExprForgeCodegenSymbol exprSymbol, CodegenClassScope codegenClassScope) {
            CodegenMethodNode methodNode = codegenMethodScope.makeChild(boolean.class, IntervalComputerDuringAndIncludesThresholdEval.class, codegenClassScope).addParam(IntervalForgeCodegenNames.PARAMS);

            CodegenBlock block = methodNode.getBlock()
                    .declareVar(long.class, "thresholdValue", forge.threshold.codegen(IntervalForgeCodegenNames.REF_LEFTSTART, methodNode, exprSymbol, codegenClassScope));

            if (forge.during) {
                block.declareVar(long.class, "deltaStart", op(IntervalForgeCodegenNames.REF_LEFTSTART, "-", IntervalForgeCodegenNames.REF_RIGHTSTART))
                        .ifConditionReturnConst(or(relational(ref("deltaStart"), LE, constant(0)), relational(ref("deltaStart"), GT, ref("thresholdValue"))), false)
                        .declareVar(long.class, "deltaEnd", op(IntervalForgeCodegenNames.REF_RIGHTEND, "-", IntervalForgeCodegenNames.REF_LEFTEND))
                        .methodReturn(not(or(relational(ref("deltaEnd"), LE, constant(0)), relational(ref("deltaEnd"), GT, ref("thresholdValue")))));
            } else {
                block.declareVar(long.class, "deltaStart", op(IntervalForgeCodegenNames.REF_RIGHTSTART, "-", IntervalForgeCodegenNames.REF_LEFTSTART))
                        .ifConditionReturnConst(or(relational(ref("deltaStart"), LE, constant(0)), relational(ref("deltaStart"), GT, ref("thresholdValue"))), false)
                        .declareVar(long.class, "deltaEnd", op(IntervalForgeCodegenNames.REF_LEFTEND, "-", IntervalForgeCodegenNames.REF_RIGHTEND))
                        .methodReturn(not(or(relational(ref("deltaEnd"), LE, constant(0)), relational(ref("deltaEnd"), GT, ref("thresholdValue")))));
            }
            return localMethod(methodNode, leftStart, leftEnd, rightStart, rightEnd);
        }
    }

    public static class IntervalComputerDuringAndIncludesMinMax implements IntervalComputerForge {

        private final boolean during;
        private final IntervalDeltaExprForge minEval;
        private final IntervalDeltaExprForge maxEval;

        public IntervalComputerDuringAndIncludesMinMax(boolean during, IntervalDeltaExprForge minEval, IntervalDeltaExprForge maxEval) {
            this.during = during;
            this.minEval = minEval;
            this.maxEval = maxEval;
        }

        public IntervalComputerEval makeComputerEval() {
            return new IntervalComputerDuringAndIncludesMinMaxEval(during, minEval.makeEvaluator(), maxEval.makeEvaluator());
        }

        public CodegenExpression codegen(CodegenExpression leftStart, CodegenExpression leftEnd, CodegenExpression rightStart, CodegenExpression rightEnd, CodegenMethodScope codegenMethodScope, ExprForgeCodegenSymbol exprSymbol, CodegenClassScope codegenClassScope) {
            return IntervalComputerDuringAndIncludesMinMaxEval.codegen(this, leftStart, leftEnd, rightStart, rightEnd, codegenMethodScope, exprSymbol, codegenClassScope);
        }
    }

    public static class IntervalComputerDuringAndIncludesMinMaxEval implements IntervalComputerEval {

        private final boolean during;
        private final IntervalDeltaExprEvaluator minEval;
        private final IntervalDeltaExprEvaluator maxEval;

        public IntervalComputerDuringAndIncludesMinMaxEval(boolean during, IntervalDeltaExprEvaluator minEval, IntervalDeltaExprEvaluator maxEval) {
            this.during = during;
            this.minEval = minEval;
            this.maxEval = maxEval;
        }

        public Boolean compute(long leftStart, long leftEnd, long rightStart, long rightEnd, EventBean[] eventsPerStream, boolean newData, ExprEvaluatorContext context) {
            long min = minEval.evaluate(leftStart, eventsPerStream, newData, context);
            long max = maxEval.evaluate(rightEnd, eventsPerStream, newData, context);
            if (during) {
                return computeIntervalDuring(leftStart, leftEnd, rightStart, rightEnd, min, max, min, max);
            } else {
                return computeIntervalIncludes(leftStart, leftEnd, rightStart, rightEnd, min, max, min, max);
            }
        }

        public static CodegenExpression codegen(IntervalComputerDuringAndIncludesMinMax forge, CodegenExpression leftStart, CodegenExpression leftEnd, CodegenExpression rightStart, CodegenExpression rightEnd, CodegenMethodScope codegenMethodScope, ExprForgeCodegenSymbol exprSymbol, CodegenClassScope codegenClassScope) {
            CodegenMethodNode methodNode = codegenMethodScope.makeChild(boolean.class, IntervalComputerDuringAndIncludesMinMaxEval.class, codegenClassScope).addParam(IntervalForgeCodegenNames.PARAMS);

            CodegenBlock block = methodNode.getBlock()
                    .declareVar(long.class, "min", forge.minEval.codegen(IntervalForgeCodegenNames.REF_LEFTSTART, methodNode, exprSymbol, codegenClassScope))
                    .declareVar(long.class, "max", forge.maxEval.codegen(IntervalForgeCodegenNames.REF_RIGHTEND, methodNode, exprSymbol, codegenClassScope));
            block.methodReturn(staticMethod(IntervalComputerDuringAndIncludesMinMaxEval.class,
                    forge.during ? "computeIntervalDuring" : "computeIntervalIncludes",
                    IntervalForgeCodegenNames.REF_LEFTSTART, IntervalForgeCodegenNames.REF_LEFTEND, IntervalForgeCodegenNames.REF_RIGHTSTART, IntervalForgeCodegenNames.REF_RIGHTEND, ref("min"), ref("max"), ref("min"), ref("max")));
            return localMethod(methodNode, leftStart, leftEnd, rightStart, rightEnd);
        }

        public static boolean computeIntervalDuring(long left, long leftEnd, long right, long rightEnd,
                                                    long startMin, long startMax, long endMin, long endMax) {
            if (startMin <= 0) {
                startMin = 1;
            }
            long deltaStart = left - right;
            if (deltaStart < startMin || deltaStart > startMax) {
                return false;
            }

            long deltaEnd = rightEnd - leftEnd;
            return !(deltaEnd < endMin || deltaEnd > endMax);
        }

        public static boolean computeIntervalIncludes(long left, long leftEnd, long right, long rightEnd,
                                                      long startMin, long startMax, long endMin, long endMax) {
            if (startMin <= 0) {
                startMin = 1;
            }
            long deltaStart = right - left;
            if (deltaStart < startMin || deltaStart > startMax) {
                return false;
            }

            long deltaEnd = leftEnd - rightEnd;
            return !(deltaEnd < endMin || deltaEnd > endMax);
        }
    }

    public static class IntervalComputerDuringMinMaxStartEndForge implements IntervalComputerForge {

        private final boolean during;
        private final IntervalDeltaExprForge minStartEval;
        private final IntervalDeltaExprForge maxStartEval;
        private final IntervalDeltaExprForge minEndEval;
        private final IntervalDeltaExprForge maxEndEval;

        public IntervalComputerDuringMinMaxStartEndForge(boolean during, IntervalDeltaExprForge[] parameters) {
            this.during = during;
            minStartEval = parameters[0];
            maxStartEval = parameters[1];
            minEndEval = parameters[2];
            maxEndEval = parameters[3];
        }

        public IntervalComputerEval makeComputerEval() {
            return new IntervalComputerDuringMinMaxStartEndEval(during, minStartEval.makeEvaluator(), maxStartEval.makeEvaluator(), minEndEval.makeEvaluator(), maxEndEval.makeEvaluator());
        }

        public CodegenExpression codegen(CodegenExpression leftStart, CodegenExpression leftEnd, CodegenExpression rightStart, CodegenExpression rightEnd, CodegenMethodScope codegenMethodScope, ExprForgeCodegenSymbol exprSymbol, CodegenClassScope codegenClassScope) {
            return IntervalComputerDuringMinMaxStartEndEval.codegen(this, leftStart, leftEnd, rightStart, rightEnd, codegenMethodScope, exprSymbol, codegenClassScope);
        }
    }

    public static class IntervalComputerDuringMinMaxStartEndEval implements IntervalComputerEval {

        private final boolean during;
        private final IntervalDeltaExprEvaluator minStartEval;
        private final IntervalDeltaExprEvaluator maxStartEval;
        private final IntervalDeltaExprEvaluator minEndEval;
        private final IntervalDeltaExprEvaluator maxEndEval;

        public IntervalComputerDuringMinMaxStartEndEval(boolean during, IntervalDeltaExprEvaluator minStartEval, IntervalDeltaExprEvaluator maxStartEval, IntervalDeltaExprEvaluator minEndEval, IntervalDeltaExprEvaluator maxEndEval) {
            this.during = during;
            this.minStartEval = minStartEval;
            this.maxStartEval = maxStartEval;
            this.minEndEval = minEndEval;
            this.maxEndEval = maxEndEval;
        }

        public Boolean compute(long leftStart, long leftEnd, long rightStart, long rightEnd, EventBean[] eventsPerStream, boolean newData, ExprEvaluatorContext context) {

            long minStart = minStartEval.evaluate(rightStart, eventsPerStream, newData, context);
            long maxStart = maxStartEval.evaluate(rightStart, eventsPerStream, newData, context);
            long minEnd = minEndEval.evaluate(rightEnd, eventsPerStream, newData, context);
            long maxEnd = maxEndEval.evaluate(rightEnd, eventsPerStream, newData, context);

            if (during) {
                return IntervalComputerDuringAndIncludesMinMaxEval.computeIntervalDuring(leftStart, leftEnd, rightStart, rightEnd, minStart, maxStart, minEnd, maxEnd);
            } else {
                return IntervalComputerDuringAndIncludesMinMaxEval.computeIntervalIncludes(leftStart, leftEnd, rightStart, rightEnd, minStart, maxStart, minEnd, maxEnd);
            }
        }

        public static CodegenExpression codegen(IntervalComputerDuringMinMaxStartEndForge forge, CodegenExpression leftStart, CodegenExpression leftEnd, CodegenExpression rightStart, CodegenExpression rightEnd, CodegenMethodScope codegenMethodScope, ExprForgeCodegenSymbol exprSymbol, CodegenClassScope codegenClassScope) {
            CodegenMethodNode methodNode = codegenMethodScope.makeChild(boolean.class, IntervalComputerDuringMinMaxStartEndEval.class, codegenClassScope).addParam(IntervalForgeCodegenNames.PARAMS);

            CodegenBlock block = methodNode.getBlock()
                    .declareVar(long.class, "minStart", forge.minStartEval.codegen(IntervalForgeCodegenNames.REF_RIGHTSTART, methodNode, exprSymbol, codegenClassScope))
                    .declareVar(long.class, "maxStart", forge.maxStartEval.codegen(IntervalForgeCodegenNames.REF_RIGHTSTART, methodNode, exprSymbol, codegenClassScope))
                    .declareVar(long.class, "minEnd", forge.minEndEval.codegen(IntervalForgeCodegenNames.REF_RIGHTEND, methodNode, exprSymbol, codegenClassScope))
                    .declareVar(long.class, "maxEnd", forge.maxEndEval.codegen(IntervalForgeCodegenNames.REF_RIGHTEND, methodNode, exprSymbol, codegenClassScope));
            block.methodReturn(staticMethod(IntervalComputerDuringAndIncludesMinMaxEval.class,
                    forge.during ? "computeIntervalDuring" : "computeIntervalIncludes",
                    IntervalForgeCodegenNames.REF_LEFTSTART, IntervalForgeCodegenNames.REF_LEFTEND, IntervalForgeCodegenNames.REF_RIGHTSTART, IntervalForgeCodegenNames.REF_RIGHTEND, ref("minStart"), ref("maxStart"), ref("minEnd"), ref("maxEnd")));
            return localMethod(methodNode, leftStart, leftEnd, rightStart, rightEnd);
        }
    }

    /**
     * Finishes.
     */
    public static class IntervalComputerFinishesNoParam implements IntervalComputerForge, IntervalComputerEval {

        public Boolean compute(long leftStart, long leftEnd, long rightStart, long rightEnd, EventBean[] eventsPerStream, boolean newData, ExprEvaluatorContext context) {
            return rightStart < leftStart && (leftEnd == rightEnd);
        }

        public IntervalComputerEval makeComputerEval() {
            return this;
        }

        public CodegenExpression codegen(CodegenExpression leftStart, CodegenExpression leftEnd, CodegenExpression rightStart, CodegenExpression rightEnd, CodegenMethodScope codegenMethodScope, ExprForgeCodegenSymbol exprSymbol, CodegenClassScope codegenClassScope) {
            return and(relational(rightStart, LT, leftStart), equalsIdentity(leftEnd, rightEnd));
        }
    }

    public static class IntervalComputerFinishesThresholdForge implements IntervalComputerForge {
        private static final Logger log = LoggerFactory.getLogger(IntervalComputerFinishesThresholdForge.class);

        private final IntervalDeltaExprForge thresholdExpr;

        public IntervalComputerFinishesThresholdForge(IntervalDeltaExprForge thresholdExpr) {
            this.thresholdExpr = thresholdExpr;
        }

        public IntervalComputerEval makeComputerEval() {
            return new IntervalComputerFinishesThresholdEval(thresholdExpr.makeEvaluator());
        }

        public CodegenExpression codegen(CodegenExpression leftStart, CodegenExpression leftEnd, CodegenExpression rightStart, CodegenExpression rightEnd, CodegenMethodScope codegenMethodScope, ExprForgeCodegenSymbol exprSymbol, CodegenClassScope codegenClassScope) {
            return IntervalComputerFinishesThresholdEval.codegen(this, leftStart, leftEnd, rightStart, rightEnd, codegenMethodScope, exprSymbol, codegenClassScope);
        }
    }

    public static class IntervalComputerFinishesThresholdEval implements IntervalComputerEval {
        private static final Logger log = LoggerFactory.getLogger(IntervalComputerFinishesThresholdForge.class);
        public final static String METHOD_LOGWARNINGINTERVALFINISHTHRESHOLD = "logWarningIntervalFinishThreshold";

        private final IntervalDeltaExprEvaluator thresholdExpr;

        public IntervalComputerFinishesThresholdEval(IntervalDeltaExprEvaluator thresholdExpr) {
            this.thresholdExpr = thresholdExpr;
        }

        public Boolean compute(long leftStart, long leftEnd, long rightStart, long rightEnd, EventBean[] eventsPerStream, boolean newData, ExprEvaluatorContext context) {

            long threshold = thresholdExpr.evaluate(Math.min(leftEnd, rightEnd), eventsPerStream, newData, context);

            if (threshold < 0) {
                logWarningIntervalFinishThreshold();
                return null;
            }

            if (rightStart >= leftStart) {
                return false;
            }
            long delta = Math.abs(leftEnd - rightEnd);
            return delta <= threshold;
        }

        /**
         * NOTE: Code-generation-invoked method, method name and parameter order matters
         */
        public static void logWarningIntervalFinishThreshold() {
            log.warn("The 'finishes' date-time method does not allow negative threshold");
        }

        public static CodegenExpression codegen(IntervalComputerFinishesThresholdForge forge, CodegenExpression leftStart, CodegenExpression leftEnd, CodegenExpression rightStart, CodegenExpression rightEnd, CodegenMethodScope codegenMethodScope, ExprForgeCodegenSymbol exprSymbol, CodegenClassScope codegenClassScope) {
            CodegenMethodNode methodNode = codegenMethodScope.makeChild(Boolean.class, IntervalComputerFinishesThresholdEval.class, codegenClassScope).addParam(IntervalForgeCodegenNames.PARAMS);

            methodNode.getBlock()
                    .declareVar(long.class, "threshold", forge.thresholdExpr.codegen(staticMethod(Math.class, "min", IntervalForgeCodegenNames.REF_LEFTEND, IntervalForgeCodegenNames.REF_RIGHTEND), methodNode, exprSymbol, codegenClassScope))
                    .ifCondition(relational(ref("threshold"), LT, constant(0)))
                    .staticMethod(IntervalComputerFinishesThresholdEval.class, METHOD_LOGWARNINGINTERVALFINISHTHRESHOLD)
                    .blockReturn(constantNull())
                    .ifConditionReturnConst(relational(IntervalForgeCodegenNames.REF_RIGHTSTART, GE, IntervalForgeCodegenNames.REF_LEFTSTART), false)
                    .declareVar(long.class, "delta", staticMethod(Math.class, "abs", op(IntervalForgeCodegenNames.REF_LEFTEND, "-", IntervalForgeCodegenNames.REF_RIGHTEND)))
                    .methodReturn(relational(ref("delta"), LE, ref("threshold")));
            return localMethod(methodNode, leftStart, leftEnd, rightStart, rightEnd);
        }
    }

    /**
     * Finishes-By.
     */
    public static class IntervalComputerFinishedByNoParam implements IntervalComputerForge, IntervalComputerEval {

        public Boolean compute(long leftStart, long leftEnd, long rightStart, long rightEnd, EventBean[] eventsPerStream, boolean newData, ExprEvaluatorContext context) {
            return leftStart < rightStart && (leftEnd == rightEnd);
        }

        public IntervalComputerEval makeComputerEval() {
            return this;
        }

        public CodegenExpression codegen(CodegenExpression leftStart, CodegenExpression leftEnd, CodegenExpression rightStart, CodegenExpression rightEnd, CodegenMethodScope codegenMethodScope, ExprForgeCodegenSymbol exprSymbol, CodegenClassScope codegenClassScope) {
            return and(relational(leftStart, LT, rightStart), equalsIdentity(leftEnd, rightEnd));
        }
    }

    public static class IntervalComputerFinishedByThresholdForge implements IntervalComputerForge {

        private final IntervalDeltaExprForge thresholdExpr;

        public IntervalComputerFinishedByThresholdForge(IntervalDeltaExprForge thresholdExpr) {
            this.thresholdExpr = thresholdExpr;
        }

        public IntervalComputerEval makeComputerEval() {
            return new IntervalComputerFinishedByThresholdEval(thresholdExpr.makeEvaluator());
        }

        public CodegenExpression codegen(CodegenExpression leftStart, CodegenExpression leftEnd, CodegenExpression rightStart, CodegenExpression rightEnd, CodegenMethodScope codegenMethodScope, ExprForgeCodegenSymbol exprSymbol, CodegenClassScope codegenClassScope) {
            return IntervalComputerFinishedByThresholdEval.codegen(this, leftStart, leftEnd, rightStart, rightEnd, codegenMethodScope, exprSymbol, codegenClassScope);
        }
    }

    public static class IntervalComputerFinishedByThresholdEval implements IntervalComputerEval {

        private static final Logger log = LoggerFactory.getLogger(IntervalComputerFinishedByThresholdForge.class);
        public final static String METHOD_LOGWARNINGINTERVALFINISHEDBYTHRESHOLD = "logWarningIntervalFinishedByThreshold";

        private final IntervalDeltaExprEvaluator thresholdExpr;

        public IntervalComputerFinishedByThresholdEval(IntervalDeltaExprEvaluator thresholdExpr) {
            this.thresholdExpr = thresholdExpr;
        }

        public Boolean compute(long leftStart, long leftEnd, long rightStart, long rightEnd, EventBean[] eventsPerStream, boolean newData, ExprEvaluatorContext context) {

            long threshold = thresholdExpr.evaluate(Math.min(rightEnd, leftEnd), eventsPerStream, newData, context);
            if (threshold < 0) {
                logWarningIntervalFinishedByThreshold();
                return null;
            }

            if (leftStart >= rightStart) {
                return false;
            }
            long delta = Math.abs(leftEnd - rightEnd);
            return delta <= threshold;
        }

        public static void logWarningIntervalFinishedByThreshold() {
            log.warn("The 'finishes' date-time method does not allow negative threshold");
        }

        public static CodegenExpression codegen(IntervalComputerFinishedByThresholdForge forge, CodegenExpression leftStart, CodegenExpression leftEnd, CodegenExpression rightStart, CodegenExpression rightEnd, CodegenMethodScope codegenMethodScope, ExprForgeCodegenSymbol exprSymbol, CodegenClassScope codegenClassScope) {
            CodegenMethodNode methodNode = codegenMethodScope.makeChild(Boolean.class, IntervalComputerFinishedByThresholdEval.class, codegenClassScope).addParam(IntervalForgeCodegenNames.PARAMS);

            methodNode.getBlock()
                    .declareVar(long.class, "threshold", forge.thresholdExpr.codegen(staticMethod(Math.class, "min", IntervalForgeCodegenNames.REF_RIGHTEND, IntervalForgeCodegenNames.REF_LEFTEND), methodNode, exprSymbol, codegenClassScope))
                    .ifCondition(relational(ref("threshold"), LT, constant(0)))
                    .staticMethod(IntervalComputerFinishedByThresholdEval.class, METHOD_LOGWARNINGINTERVALFINISHEDBYTHRESHOLD)
                    .blockReturn(constantNull())
                    .ifConditionReturnConst(relational(IntervalForgeCodegenNames.REF_LEFTSTART, GE, IntervalForgeCodegenNames.REF_RIGHTSTART), false)
                    .declareVar(long.class, "delta", staticMethod(Math.class, "abs", op(IntervalForgeCodegenNames.REF_LEFTEND, "-", IntervalForgeCodegenNames.REF_RIGHTEND)))
                    .methodReturn(relational(ref("delta"), LE, ref("threshold")));
            return localMethod(methodNode, leftStart, leftEnd, rightStart, rightEnd);
        }
    }

    /**
     * Meets.
     */
    public static class IntervalComputerMeetsNoParam implements IntervalComputerForge, IntervalComputerEval {

        public Boolean compute(long leftStart, long leftEnd, long rightStart, long rightEnd, EventBean[] eventsPerStream, boolean newData, ExprEvaluatorContext context) {
            return leftEnd == rightStart;
        }

        public IntervalComputerEval makeComputerEval() {
            return this;
        }

        public CodegenExpression codegen(CodegenExpression leftStart, CodegenExpression leftEnd, CodegenExpression rightStart, CodegenExpression rightEnd, CodegenMethodScope codegenMethodScope, ExprForgeCodegenSymbol exprSymbol, CodegenClassScope codegenClassScope) {
            return equalsIdentity(leftEnd, rightStart);
        }
    }

    public static class IntervalComputerMeetsThresholdForge implements IntervalComputerForge {

        private final IntervalDeltaExprForge thresholdExpr;

        public IntervalComputerMeetsThresholdForge(IntervalDeltaExprForge thresholdExpr) {
            this.thresholdExpr = thresholdExpr;
        }

        public IntervalComputerEval makeComputerEval() {
            return new IntervalComputerMeetsThresholdEval(thresholdExpr.makeEvaluator());
        }

        public CodegenExpression codegen(CodegenExpression leftStart, CodegenExpression leftEnd, CodegenExpression rightStart, CodegenExpression rightEnd, CodegenMethodScope codegenMethodScope, ExprForgeCodegenSymbol exprSymbol, CodegenClassScope codegenClassScope) {
            return IntervalComputerMeetsThresholdEval.codegen(this, leftStart, leftEnd, rightStart, rightEnd, codegenMethodScope, exprSymbol, codegenClassScope);
        }
    }

    public static class IntervalComputerMeetsThresholdEval implements IntervalComputerEval {

        private static final Logger log = LoggerFactory.getLogger(IntervalComputerMeetsThresholdForge.class);
        public final static String METHOD_LOGWARNINGINTERVALMEETSTHRESHOLD = "logWarningIntervalMeetsThreshold";

        private final IntervalDeltaExprEvaluator thresholdExpr;

        public IntervalComputerMeetsThresholdEval(IntervalDeltaExprEvaluator thresholdExpr) {
            this.thresholdExpr = thresholdExpr;
        }

        public Boolean compute(long leftStart, long leftEnd, long rightStart, long rightEnd, EventBean[] eventsPerStream, boolean newData, ExprEvaluatorContext context) {
            long threshold = thresholdExpr.evaluate(Math.min(leftEnd, rightStart), eventsPerStream, newData, context);
            if (threshold < 0) {
                logWarningIntervalMeetsThreshold();
                return null;
            }

            long delta = Math.abs(rightStart - leftEnd);
            return delta <= threshold;
        }

        /**
         * NOTE: Code-generation-invoked method, method name and parameter order matters
         */
        public static void logWarningIntervalMeetsThreshold() {
            log.warn("The 'meets' date-time method does not allow negative threshold");
        }

        public static CodegenExpression codegen(IntervalComputerMeetsThresholdForge forge, CodegenExpression leftStart, CodegenExpression leftEnd, CodegenExpression rightStart, CodegenExpression rightEnd, CodegenMethodScope codegenMethodScope, ExprForgeCodegenSymbol exprSymbol, CodegenClassScope codegenClassScope) {
            CodegenMethodNode methodNode = codegenMethodScope.makeChild(Boolean.class, IntervalComputerMeetsThresholdEval.class, codegenClassScope).addParam(IntervalForgeCodegenNames.PARAMS);

            methodNode.getBlock()
                    .declareVar(long.class, "threshold", forge.thresholdExpr.codegen(staticMethod(Math.class, "min", IntervalForgeCodegenNames.REF_LEFTEND, IntervalForgeCodegenNames.REF_RIGHTSTART), methodNode, exprSymbol, codegenClassScope))
                    .ifCondition(relational(ref("threshold"), LT, constant(0)))
                    .staticMethod(IntervalComputerMeetsThresholdEval.class, METHOD_LOGWARNINGINTERVALMEETSTHRESHOLD)
                    .blockReturn(constantNull())
                    .declareVar(long.class, "delta", staticMethod(Math.class, "abs", op(IntervalForgeCodegenNames.REF_RIGHTSTART, "-", IntervalForgeCodegenNames.REF_LEFTEND)))
                    .methodReturn(relational(ref("delta"), LE, ref("threshold")));
            return localMethod(methodNode, leftStart, leftEnd, rightStart, rightEnd);
        }
    }

    /**
     * Met-By.
     */
    public static class IntervalComputerMetByNoParam implements IntervalComputerForge, IntervalComputerEval {

        public Boolean compute(long leftStart, long leftEnd, long rightStart, long rightEnd, EventBean[] eventsPerStream, boolean newData, ExprEvaluatorContext context) {
            return rightEnd == leftStart;
        }

        public IntervalComputerEval makeComputerEval() {
            return this;
        }

        public CodegenExpression codegen(CodegenExpression leftStart, CodegenExpression leftEnd, CodegenExpression rightStart, CodegenExpression rightEnd, CodegenMethodScope codegenMethodScope, ExprForgeCodegenSymbol exprSymbol, CodegenClassScope codegenClassScope) {
            return equalsIdentity(rightEnd, leftStart);
        }
    }

    public static class IntervalComputerMetByThresholdForge implements IntervalComputerForge {

        private final IntervalDeltaExprForge thresholdExpr;

        public IntervalComputerMetByThresholdForge(IntervalDeltaExprForge thresholdExpr) {
            this.thresholdExpr = thresholdExpr;
        }

        public IntervalComputerEval makeComputerEval() {
            return new IntervalComputerMetByThresholdEval(thresholdExpr.makeEvaluator());
        }

        public CodegenExpression codegen(CodegenExpression leftStart, CodegenExpression leftEnd, CodegenExpression rightStart, CodegenExpression rightEnd, CodegenMethodScope codegenMethodScope, ExprForgeCodegenSymbol exprSymbol, CodegenClassScope codegenClassScope) {
            return IntervalComputerMetByThresholdEval.codegen(this, leftStart, leftEnd, rightStart, rightEnd, codegenMethodScope, exprSymbol, codegenClassScope);
        }
    }

    public static class IntervalComputerMetByThresholdEval implements IntervalComputerEval {

        private static final Logger log = LoggerFactory.getLogger(IntervalComputerMetByThresholdForge.class);
        public final static String METHOD_LOGWARNINGINTERVALMETBYTHRESHOLD = "logWarningIntervalMetByThreshold";
        private final IntervalDeltaExprEvaluator thresholdExpr;

        public IntervalComputerMetByThresholdEval(IntervalDeltaExprEvaluator thresholdExpr) {
            this.thresholdExpr = thresholdExpr;
        }

        public Boolean compute(long leftStart, long leftEnd, long rightStart, long rightEnd, EventBean[] eventsPerStream, boolean newData, ExprEvaluatorContext context) {

            long threshold = thresholdExpr.evaluate(Math.min(leftStart, rightEnd), eventsPerStream, newData, context);

            if (threshold < 0) {
                logWarningIntervalMetByThreshold();
                return null;
            }

            long delta = Math.abs(leftStart - rightEnd);
            return delta <= threshold;
        }

        /**
         * NOTE: Code-generation-invoked method, method name and parameter order matters
         */
        public static void logWarningIntervalMetByThreshold() {
            log.warn("The 'met-by' date-time method does not allow negative threshold");
        }

        public static CodegenExpression codegen(IntervalComputerMetByThresholdForge forge, CodegenExpression leftStart, CodegenExpression leftEnd, CodegenExpression rightStart, CodegenExpression rightEnd, CodegenMethodScope codegenMethodScope, ExprForgeCodegenSymbol exprSymbol, CodegenClassScope codegenClassScope) {
            CodegenMethodNode methodNode = codegenMethodScope.makeChild(Boolean.class, IntervalComputerMetByThresholdEval.class, codegenClassScope).addParam(IntervalForgeCodegenNames.PARAMS);

            methodNode.getBlock()
                    .declareVar(long.class, "threshold", forge.thresholdExpr.codegen(staticMethod(Math.class, "min", IntervalForgeCodegenNames.REF_LEFTSTART, IntervalForgeCodegenNames.REF_RIGHTEND), methodNode, exprSymbol, codegenClassScope))
                    .ifCondition(relational(ref("threshold"), LT, constant(0)))
                    .staticMethod(IntervalComputerMetByThresholdEval.class, METHOD_LOGWARNINGINTERVALMETBYTHRESHOLD)
                    .blockReturn(constantNull())
                    .declareVar(long.class, "delta", staticMethod(Math.class, "abs", op(IntervalForgeCodegenNames.REF_LEFTSTART, "-", IntervalForgeCodegenNames.REF_RIGHTEND)))
                    .methodReturn(relational(ref("delta"), LE, ref("threshold")));
            return localMethod(methodNode, leftStart, leftEnd, rightStart, rightEnd);
        }
    }

    /**
     * Overlaps.
     */
    public static class IntervalComputerOverlapsNoParam implements IntervalComputerForge, IntervalComputerEval {

        public Boolean compute(long leftStart, long leftEnd, long rightStart, long rightEnd, EventBean[] eventsPerStream, boolean newData, ExprEvaluatorContext context) {
            return (leftStart < rightStart) &&
                    (rightStart < leftEnd) &&
                    (leftEnd < rightEnd);
        }

        public IntervalComputerEval makeComputerEval() {
            return this;
        }

        public CodegenExpression codegen(CodegenExpression leftStart, CodegenExpression leftEnd, CodegenExpression rightStart, CodegenExpression rightEnd, CodegenMethodScope codegenMethodScope, ExprForgeCodegenSymbol exprSymbol, CodegenClassScope codegenClassScope) {
            return and(
                    relational(leftStart, LT, rightStart),
                    relational(rightStart, LT, leftEnd),
                    relational(leftEnd, LT, rightEnd));
        }
    }

    public static class IntervalComputerOverlapsAndByThreshold implements IntervalComputerForge {

        private final boolean overlaps;
        private final IntervalDeltaExprForge thresholdExpr;

        public IntervalComputerOverlapsAndByThreshold(boolean overlaps, IntervalDeltaExprForge thresholdExpr) {
            this.overlaps = overlaps;
            this.thresholdExpr = thresholdExpr;
        }

        public IntervalComputerEval makeComputerEval() {
            return new IntervalComputerOverlapsAndByThresholdEval(overlaps, thresholdExpr.makeEvaluator());
        }

        public CodegenExpression codegen(CodegenExpression leftStart, CodegenExpression leftEnd, CodegenExpression rightStart, CodegenExpression rightEnd, CodegenMethodScope codegenMethodScope, ExprForgeCodegenSymbol exprSymbol, CodegenClassScope codegenClassScope) {
            return IntervalComputerOverlapsAndByThresholdEval.codegen(this, leftStart, leftEnd, rightStart, rightEnd, codegenMethodScope, exprSymbol, codegenClassScope);
        }
    }

    public static class IntervalComputerOverlapsAndByThresholdEval implements IntervalComputerEval {

        private final boolean overlaps;
        private final IntervalDeltaExprEvaluator thresholdExpr;

        public IntervalComputerOverlapsAndByThresholdEval(boolean overlaps, IntervalDeltaExprEvaluator thresholdExpr) {
            this.overlaps = overlaps;
            this.thresholdExpr = thresholdExpr;
        }

        public Boolean compute(long leftStart, long leftEnd, long rightStart, long rightEnd, EventBean[] eventsPerStream, boolean newData, ExprEvaluatorContext context) {

            if (overlaps) {
                long threshold = thresholdExpr.evaluate(leftStart, eventsPerStream, newData, context);
                return computeIntervalOverlaps(leftStart, leftEnd, rightStart, rightEnd, 0, threshold);
            } else {
                long threshold = thresholdExpr.evaluate(rightStart, eventsPerStream, newData, context);
                return computeIntervalOverlaps(rightStart, rightEnd, leftStart, leftEnd, 0, threshold);
            }
        }

        public static CodegenExpression codegen(IntervalComputerOverlapsAndByThreshold forge, CodegenExpression leftStart, CodegenExpression leftEnd, CodegenExpression rightStart, CodegenExpression rightEnd, CodegenMethodScope codegenMethodScope, ExprForgeCodegenSymbol exprSymbol, CodegenClassScope codegenClassScope) {
            CodegenMethodNode methodNode = codegenMethodScope.makeChild(boolean.class, IntervalComputerOverlapsAndByThresholdEval.class, codegenClassScope).addParam(IntervalForgeCodegenNames.PARAMS);

            CodegenBlock block = methodNode.getBlock()
                    .declareVar(long.class, "threshold", forge.thresholdExpr.codegen(forge.overlaps ? IntervalForgeCodegenNames.REF_LEFTSTART : IntervalForgeCodegenNames.REF_RIGHTSTART, methodNode, exprSymbol, codegenClassScope));
            CodegenMethodNode method;
            if (forge.overlaps) {
                block.methodReturn(staticMethod(IntervalComputerOverlapsAndByThresholdEval.class, "computeIntervalOverlaps",
                        IntervalForgeCodegenNames.REF_LEFTSTART, IntervalForgeCodegenNames.REF_LEFTEND, IntervalForgeCodegenNames.REF_RIGHTSTART, IntervalForgeCodegenNames.REF_RIGHTEND, constant(0), ref("threshold")));
            } else {
                block.methodReturn(staticMethod(IntervalComputerOverlapsAndByThresholdEval.class, "computeIntervalOverlaps",
                        IntervalForgeCodegenNames.REF_RIGHTSTART, IntervalForgeCodegenNames.REF_RIGHTEND, IntervalForgeCodegenNames.REF_LEFTSTART, IntervalForgeCodegenNames.REF_LEFTEND, constant(0), ref("threshold")));
            }
            return localMethod(methodNode, leftStart, leftEnd, rightStart, rightEnd);
        }

        /**
         * NOTE: Code-generation-invoked method, method name and parameter order matters
         *
         * @param left     left start
         * @param leftEnd  left end
         * @param right    right start
         * @param rightEnd right end
         * @param min      min
         * @param max      max
         * @return flag
         */
        public static boolean computeIntervalOverlaps(long left, long leftEnd, long right, long rightEnd, long min, long max) {
            boolean match = (left < right) &&
                    (right < leftEnd) &&
                    (leftEnd < rightEnd);
            if (!match) {
                return false;
            }
            long delta = leftEnd - right;
            return min <= delta && delta <= max;
        }
    }

    public static class IntervalComputerOverlapsAndByMinMaxForge implements IntervalComputerForge {

        private final boolean overlaps;
        private final IntervalDeltaExprForge minEval;
        private final IntervalDeltaExprForge maxEval;

        public IntervalComputerOverlapsAndByMinMaxForge(boolean overlaps, IntervalDeltaExprForge minEval, IntervalDeltaExprForge maxEval) {
            this.overlaps = overlaps;
            this.minEval = minEval;
            this.maxEval = maxEval;
        }

        public IntervalComputerEval makeComputerEval() {
            return new IntervalComputerOverlapsAndByMinMaxEval(overlaps, minEval.makeEvaluator(), maxEval.makeEvaluator());
        }

        public CodegenExpression codegen(CodegenExpression leftStart, CodegenExpression leftEnd, CodegenExpression rightStart, CodegenExpression rightEnd, CodegenMethodScope codegenMethodScope, ExprForgeCodegenSymbol exprSymbol, CodegenClassScope codegenClassScope) {
            return IntervalComputerOverlapsAndByMinMaxEval.codegen(this, leftStart, leftEnd, rightStart, rightEnd, codegenMethodScope, exprSymbol, codegenClassScope);
        }
    }

    public static class IntervalComputerOverlapsAndByMinMaxEval implements IntervalComputerEval {

        private final boolean overlaps;
        private final IntervalDeltaExprEvaluator minEval;
        private final IntervalDeltaExprEvaluator maxEval;

        public IntervalComputerOverlapsAndByMinMaxEval(boolean overlaps, IntervalDeltaExprEvaluator minEval, IntervalDeltaExprEvaluator maxEval) {
            this.overlaps = overlaps;
            this.minEval = minEval;
            this.maxEval = maxEval;
        }

        public Boolean compute(long leftStart, long leftEnd, long rightStart, long rightEnd, EventBean[] eventsPerStream, boolean newData, ExprEvaluatorContext context) {

            if (overlaps) {
                long minThreshold = minEval.evaluate(leftStart, eventsPerStream, newData, context);
                long maxThreshold = maxEval.evaluate(leftEnd, eventsPerStream, newData, context);
                return IntervalComputerOverlapsAndByThresholdEval.computeIntervalOverlaps(leftStart, leftEnd, rightStart, rightEnd, minThreshold, maxThreshold);
            } else {
                long minThreshold = minEval.evaluate(rightStart, eventsPerStream, newData, context);
                long maxThreshold = maxEval.evaluate(rightEnd, eventsPerStream, newData, context);
                return IntervalComputerOverlapsAndByThresholdEval.computeIntervalOverlaps(rightStart, rightEnd, leftStart, leftEnd, minThreshold, maxThreshold);
            }
        }

        public static CodegenExpression codegen(IntervalComputerOverlapsAndByMinMaxForge forge, CodegenExpression leftStart, CodegenExpression leftEnd, CodegenExpression rightStart, CodegenExpression rightEnd, CodegenMethodScope codegenMethodScope, ExprForgeCodegenSymbol exprSymbol, CodegenClassScope codegenClassScope) {
            CodegenMethodNode methodNode = codegenMethodScope.makeChild(boolean.class, IntervalComputerOverlapsAndByMinMaxEval.class, codegenClassScope).addParam(IntervalForgeCodegenNames.PARAMS);

            CodegenBlock block = methodNode.getBlock()
                    .declareVar(long.class, "minThreshold", forge.minEval.codegen(forge.overlaps ? IntervalForgeCodegenNames.REF_LEFTSTART : IntervalForgeCodegenNames.REF_RIGHTSTART, methodNode, exprSymbol, codegenClassScope))
                    .declareVar(long.class, "maxThreshold", forge.maxEval.codegen(forge.overlaps ? IntervalForgeCodegenNames.REF_LEFTEND : IntervalForgeCodegenNames.REF_RIGHTEND, methodNode, exprSymbol, codegenClassScope));
            if (forge.overlaps) {
                block.methodReturn(staticMethod(IntervalComputerOverlapsAndByThresholdEval.class, "computeIntervalOverlaps",
                        IntervalForgeCodegenNames.REF_LEFTSTART, IntervalForgeCodegenNames.REF_LEFTEND, IntervalForgeCodegenNames.REF_RIGHTSTART, IntervalForgeCodegenNames.REF_RIGHTEND, ref("minThreshold"), ref("maxThreshold")));
            } else {
                block.methodReturn(staticMethod(IntervalComputerOverlapsAndByThresholdEval.class, "computeIntervalOverlaps",
                        IntervalForgeCodegenNames.REF_RIGHTSTART, IntervalForgeCodegenNames.REF_RIGHTEND, IntervalForgeCodegenNames.REF_LEFTSTART, IntervalForgeCodegenNames.REF_LEFTEND, ref("minThreshold"), ref("maxThreshold")));
            }
            return localMethod(methodNode, leftStart, leftEnd, rightStart, rightEnd);
        }
    }

    /**
     * OverlappedBy.
     */
    public static class IntervalComputerOverlappedByNoParam implements IntervalComputerForge, IntervalComputerEval {

        public Boolean compute(long leftStart, long leftEnd, long rightStart, long rightEnd, EventBean[] eventsPerStream, boolean newData, ExprEvaluatorContext context) {
            return (rightStart < leftStart) &&
                    (leftStart < rightEnd) &&
                    (rightEnd < leftEnd);
        }

        public IntervalComputerEval makeComputerEval() {
            return this;
        }

        public CodegenExpression codegen(CodegenExpression leftStart, CodegenExpression leftEnd, CodegenExpression rightStart, CodegenExpression rightEnd, CodegenMethodScope codegenMethodScope, ExprForgeCodegenSymbol exprSymbol, CodegenClassScope codegenClassScope) {
            return and(
                    relational(rightStart, LT, leftStart),
                    relational(leftStart, LT, rightEnd),
                    relational(rightEnd, LT, leftEnd));
        }
    }

    /**
     * Starts.
     */
    public static class IntervalComputerStartsNoParam implements IntervalComputerForge, IntervalComputerEval {

        public Boolean compute(long leftStart, long leftEnd, long rightStart, long rightEnd, EventBean[] eventsPerStream, boolean newData, ExprEvaluatorContext context) {
            return (leftStart == rightStart) && (leftEnd < rightEnd);
        }

        public IntervalComputerEval makeComputerEval() {
            return this;
        }

        public CodegenExpression codegen(CodegenExpression leftStart, CodegenExpression leftEnd, CodegenExpression rightStart, CodegenExpression rightEnd, CodegenMethodScope codegenMethodScope, ExprForgeCodegenSymbol exprSymbol, CodegenClassScope codegenClassScope) {
            return and(equalsIdentity(leftStart, rightStart), relational(leftEnd, LT, rightEnd));
        }
    }

    public static class IntervalComputerStartsThresholdForge implements IntervalComputerForge {

        private final IntervalDeltaExprForge thresholdExpr;

        public IntervalComputerStartsThresholdForge(IntervalDeltaExprForge thresholdExpr) {
            this.thresholdExpr = thresholdExpr;
        }

        public IntervalComputerEval makeComputerEval() {
            return new IntervalComputerStartsThresholdEval(thresholdExpr.makeEvaluator());
        }

        public CodegenExpression codegen(CodegenExpression leftStart, CodegenExpression leftEnd, CodegenExpression rightStart, CodegenExpression rightEnd, CodegenMethodScope codegenMethodScope, ExprForgeCodegenSymbol exprSymbol, CodegenClassScope codegenClassScope) {
            return IntervalComputerStartsThresholdEval.codegen(this, leftStart, leftEnd, rightStart, rightEnd, codegenMethodScope, exprSymbol, codegenClassScope);
        }
    }

    public static class IntervalComputerStartsThresholdEval implements IntervalComputerEval {

        private static final Logger log = LoggerFactory.getLogger(IntervalComputerStartsThresholdEval.class);
        public final static String METHOD_LOGWARNINGINTERVALSTARTSTHRESHOLD = "logWarningIntervalStartsThreshold";
        private final IntervalDeltaExprEvaluator thresholdExpr;

        public IntervalComputerStartsThresholdEval(IntervalDeltaExprEvaluator thresholdExpr) {
            this.thresholdExpr = thresholdExpr;
        }

        public Boolean compute(long leftStart, long leftEnd, long rightStart, long rightEnd, EventBean[] eventsPerStream, boolean newData, ExprEvaluatorContext context) {

            long threshold = thresholdExpr.evaluate(Math.min(leftStart, rightStart), eventsPerStream, newData, context);
            if (threshold < 0) {
                logWarningIntervalStartsThreshold();
                return null;
            }

            long delta = Math.abs(leftStart - rightStart);
            return delta <= threshold && (leftEnd < rightEnd);
        }

        /**
         * NOTE: Code-generation-invoked method, method name and parameter order matters
         */
        public static void logWarningIntervalStartsThreshold() {
            log.warn("The 'starts' date-time method does not allow negative threshold");
        }

        public static CodegenExpression codegen(IntervalComputerStartsThresholdForge forge, CodegenExpression leftStart, CodegenExpression leftEnd, CodegenExpression rightStart, CodegenExpression rightEnd, CodegenMethodScope codegenMethodScope, ExprForgeCodegenSymbol exprSymbol, CodegenClassScope codegenClassScope) {
            CodegenMethodNode methodNode = codegenMethodScope.makeChild(Boolean.class, IntervalComputerStartsThresholdEval.class, codegenClassScope).addParam(IntervalForgeCodegenNames.PARAMS);

            methodNode.getBlock()
                    .declareVar(long.class, "threshold", forge.thresholdExpr.codegen(staticMethod(Math.class, "min", IntervalForgeCodegenNames.REF_LEFTSTART, IntervalForgeCodegenNames.REF_RIGHTSTART), methodNode, exprSymbol, codegenClassScope))
                    .ifCondition(relational(ref("threshold"), LT, constant(0)))
                    .staticMethod(IntervalComputerStartsThresholdEval.class, METHOD_LOGWARNINGINTERVALSTARTSTHRESHOLD)
                    .blockReturn(constantNull())
                    .declareVar(long.class, "delta", staticMethod(Math.class, "abs", op(IntervalForgeCodegenNames.REF_LEFTSTART, "-", IntervalForgeCodegenNames.REF_RIGHTSTART)))
                    .methodReturn(and(relational(ref("delta"), LE, ref("threshold")), relational(IntervalForgeCodegenNames.REF_LEFTEND, LT, IntervalForgeCodegenNames.REF_RIGHTEND)));
            return localMethod(methodNode, leftStart, leftEnd, rightStart, rightEnd);
        }
    }

    /**
     * Started-by.
     */
    public static class IntervalComputerStartedByNoParam implements IntervalComputerForge, IntervalComputerEval {

        public Boolean compute(long leftStart, long leftEnd, long rightStart, long rightEnd, EventBean[] eventsPerStream, boolean newData, ExprEvaluatorContext context) {
            return (leftStart == rightStart) && (leftEnd > rightEnd);
        }

        public IntervalComputerEval makeComputerEval() {
            return this;
        }

        public CodegenExpression codegen(CodegenExpression leftStart, CodegenExpression leftEnd, CodegenExpression rightStart, CodegenExpression rightEnd, CodegenMethodScope codegenMethodScope, ExprForgeCodegenSymbol exprSymbol, CodegenClassScope codegenClassScope) {
            return and(equalsIdentity(leftStart, rightStart), relational(leftEnd, GT, rightEnd));
        }
    }

    public static class IntervalComputerStartedByThresholdForge implements IntervalComputerForge {

        private final IntervalDeltaExprForge thresholdExpr;

        public IntervalComputerStartedByThresholdForge(IntervalDeltaExprForge thresholdExpr) {
            this.thresholdExpr = thresholdExpr;
        }

        public IntervalComputerEval makeComputerEval() {
            return new IntervalComputerStartedByThresholdEval(thresholdExpr.makeEvaluator());
        }

        public CodegenExpression codegen(CodegenExpression leftStart, CodegenExpression leftEnd, CodegenExpression rightStart, CodegenExpression rightEnd, CodegenMethodScope codegenMethodScope, ExprForgeCodegenSymbol exprSymbol, CodegenClassScope codegenClassScope) {
            return IntervalComputerStartedByThresholdEval.codegen(this, leftStart, leftEnd, rightStart, rightEnd, codegenMethodScope, exprSymbol, codegenClassScope);
        }
    }

    public static class IntervalComputerStartedByThresholdEval implements IntervalComputerEval {

        private static final Logger log = LoggerFactory.getLogger(IntervalComputerStartedByThresholdForge.class);
        public final static String METHOD_LOGWARNINGINTERVALSTARTEDBYTHRESHOLD = "logWarningIntervalStartedByThreshold";

        private final IntervalDeltaExprEvaluator thresholdExpr;

        public IntervalComputerStartedByThresholdEval(IntervalDeltaExprEvaluator thresholdExpr) {
            this.thresholdExpr = thresholdExpr;
        }

        public Boolean compute(long leftStart, long leftEnd, long rightStart, long rightEnd, EventBean[] eventsPerStream, boolean newData, ExprEvaluatorContext context) {

            long threshold = thresholdExpr.evaluate(Math.min(leftStart, rightStart), eventsPerStream, newData, context);
            if (threshold < 0) {
                logWarningIntervalStartedByThreshold();
                return null;
            }

            long delta = Math.abs(leftStart - rightStart);
            return delta <= threshold && (leftEnd > rightEnd);
        }

        /**
         * NOTE: Code-generation-invoked method, method name and parameter order matters
         */
        public static void logWarningIntervalStartedByThreshold() {
            log.warn("The 'started-by' date-time method does not allow negative threshold");
        }

        public static CodegenExpression codegen(IntervalComputerStartedByThresholdForge forge, CodegenExpression leftStart, CodegenExpression leftEnd, CodegenExpression rightStart, CodegenExpression rightEnd, CodegenMethodScope codegenMethodScope, ExprForgeCodegenSymbol exprSymbol, CodegenClassScope codegenClassScope) {
            CodegenMethodNode methodNode = codegenMethodScope.makeChild(Boolean.class, IntervalComputerStartedByThresholdEval.class, codegenClassScope).addParam(IntervalForgeCodegenNames.PARAMS);

            methodNode.getBlock()
                    .declareVar(long.class, "threshold", forge.thresholdExpr.codegen(staticMethod(Math.class, "min", IntervalForgeCodegenNames.REF_LEFTSTART, IntervalForgeCodegenNames.REF_RIGHTSTART), methodNode, exprSymbol, codegenClassScope))
                    .ifCondition(relational(ref("threshold"), LT, constant(0)))
                    .staticMethod(IntervalComputerStartedByThresholdEval.class, METHOD_LOGWARNINGINTERVALSTARTEDBYTHRESHOLD)
                    .blockReturn(constantNull())
                    .declareVar(long.class, "delta", staticMethod(Math.class, "abs", op(IntervalForgeCodegenNames.REF_LEFTSTART, "-", IntervalForgeCodegenNames.REF_RIGHTSTART)))
                    .methodReturn(and(relational(ref("delta"), LE, ref("threshold")), relational(IntervalForgeCodegenNames.REF_LEFTEND, GT, IntervalForgeCodegenNames.REF_RIGHTEND)));
            return localMethod(methodNode, leftStart, leftEnd, rightStart, rightEnd);
        }
    }
}
