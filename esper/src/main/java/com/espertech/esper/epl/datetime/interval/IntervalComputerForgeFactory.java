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
import com.espertech.esper.codegen.core.CodegenBlock;
import com.espertech.esper.codegen.core.CodegenContext;
import com.espertech.esper.codegen.core.CodegenMethodId;
import com.espertech.esper.codegen.model.expression.CodegenExpression;
import com.espertech.esper.codegen.model.method.CodegenParamSetExprPremade;
import com.espertech.esper.codegen.model.method.CodegenParamSetIntervalNonPremade;
import com.espertech.esper.codegen.model.method.CodegenParamSetIntervalPremade;
import com.espertech.esper.epl.datetime.eval.DatetimeMethodEnum;
import com.espertech.esper.epl.datetime.interval.deltaexpr.IntervalDeltaExprMSecConstForge;
import com.espertech.esper.epl.datetime.interval.deltaexpr.IntervalDeltaExprTimePeriodConstForge;
import com.espertech.esper.epl.datetime.interval.deltaexpr.IntervalDeltaExprTimePeriodNonConstForge;
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

                        public CodegenExpression codegen(CodegenExpression reference, CodegenParamSetExprPremade params, CodegenContext context) {
                            return timerPeriodNonConst.deltaAddCodegen(reference, params, context);
                        }
                    };
                    return new ExprOptionalConstantForge(forge, null);
                }
            }
        } else if (ExprNodeUtility.isConstantValueExpr(exprNode)) {
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

                public CodegenExpression codegen(CodegenExpression reference, CodegenParamSetExprPremade params, CodegenContext context) {
                    return SimpleNumberCoercerFactory.SimpleNumberCoercerLong.codegenLong(forge.evaluateCodegen(params, context), forge.getEvaluationType());
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

        public CodegenExpression codegen(CodegenParamSetIntervalNonPremade interval, CodegenParamSetExprPremade params, CodegenContext context) {
            return staticMethod(IntervalComputerConstantAfter.class, "computeIntervalAfter", interval.leftStart(), interval.rightEnd(), constant(start), constant(end));
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

        public CodegenExpression codegen(CodegenParamSetIntervalNonPremade interval, CodegenParamSetExprPremade params, CodegenContext context) {
            return IntervalComputerAfterWithDeltaExprEval.codegen(this, interval, params, context);
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

        public static CodegenExpression codegen(IntervalComputerAfterWithDeltaExprForge forge, CodegenParamSetIntervalNonPremade interval, CodegenParamSetExprPremade params, CodegenContext context) {
            CodegenBlock block = context.addMethod(boolean.class, IntervalComputerAfterWithDeltaExprEval.class).add(CodegenParamSetIntervalPremade.INSTANCE).add(params).begin()
                    .declareVar(long.class, "rangeStartDelta", forge.start.codegen(ref("rightStart"), params, context))
                    .declareVar(long.class, "rangeEndDelta", forge.finish.codegen(ref("rightStart"), params, context));
            block.ifCondition(relational(ref("rangeStartDelta"), GT, ref("rangeEndDelta")))
                    .blockReturn(staticMethod(IntervalComputerConstantAfter.class, "computeIntervalAfter", ref("leftStart"), ref("rightEnd"), ref("rangeEndDelta"), ref("rangeStartDelta")));
            CodegenMethodId method = block.methodReturn(staticMethod(IntervalComputerConstantAfter.class, "computeIntervalAfter", ref("leftStart"), ref("rightEnd"), ref("rangeStartDelta"), ref("rangeEndDelta")));
            return localMethodBuild(method).passAll(interval).passAll(params).call();
        }
    }

    public static class IntervalComputerAfterNoParam implements IntervalComputerForge, IntervalComputerEval {

        public Boolean compute(long leftStart, long leftEnd, long rightStart, long rightEnd, EventBean[] eventsPerStream, boolean newData, ExprEvaluatorContext context) {
            return leftStart > rightEnd;
        }

        public IntervalComputerEval makeComputerEval() {
            return this;
        }

        public CodegenExpression codegen(CodegenParamSetIntervalNonPremade interval, CodegenParamSetExprPremade params, CodegenContext context) {
            return relational(interval.leftStart(), GT, interval.rightEnd());
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

        public CodegenExpression codegen(CodegenParamSetIntervalNonPremade interval, CodegenParamSetExprPremade params, CodegenContext context) {
            return staticMethod(IntervalComputerConstantBefore.class, "computeIntervalBefore", interval.leftEnd(), interval.rightStart(), constant(start), constant(end));
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

        public CodegenExpression codegen(CodegenParamSetIntervalNonPremade interval, CodegenParamSetExprPremade params, CodegenContext context) {
            return IntervalComputerBeforeWithDeltaExprEval.codegen(this, interval, params, context);
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

        public static CodegenExpression codegen(IntervalComputerBeforeWithDeltaExprForge forge, CodegenParamSetIntervalNonPremade interval, CodegenParamSetExprPremade params, CodegenContext context) {
            CodegenBlock block = context.addMethod(boolean.class, IntervalComputerBeforeWithDeltaExprEval.class).add(CodegenParamSetIntervalPremade.INSTANCE).add(params).begin()
                    .declareVar(long.class, "rangeStartDelta", forge.start.codegen(ref("leftEnd"), params, context))
                    .declareVar(long.class, "rangeEndDelta", forge.finish.codegen(ref("leftEnd"), params, context));
            block.ifCondition(relational(ref("rangeStartDelta"), GT, ref("rangeEndDelta")))
                    .blockReturn(staticMethod(IntervalComputerConstantBefore.class, "computeIntervalBefore", ref("leftEnd"), ref("rightStart"), ref("rangeEndDelta"), ref("rangeStartDelta")));
            CodegenMethodId method = block.methodReturn(staticMethod(IntervalComputerConstantBefore.class, "computeIntervalBefore", ref("leftEnd"), ref("rightStart"), ref("rangeStartDelta"), ref("rangeEndDelta")));
            return localMethodBuild(method).passAll(interval).passAll(params).call();
        }
    }

    public static class IntervalComputerBeforeNoParamForge implements IntervalComputerForge, IntervalComputerEval {

        public IntervalComputerEval makeComputerEval() {
            return this;
        }

        public CodegenExpression codegen(CodegenParamSetIntervalNonPremade interval, CodegenParamSetExprPremade params, CodegenContext context) {
            return relational(interval.leftEnd(), LT, interval.rightStart());
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

        public CodegenExpression codegen(CodegenParamSetIntervalNonPremade interval, CodegenParamSetExprPremade params, CodegenContext context) {
            return staticMethod(IntervalComputerConstantCoincides.class, "computeIntervalCoincides", interval.leftStart(), interval.leftEnd(), interval.rightStart(), interval.rightEnd(), constant(start), constant(end));
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

        public CodegenExpression codegen(CodegenParamSetIntervalNonPremade interval, CodegenParamSetExprPremade params, CodegenContext context) {
            return IntervalComputerCoincidesWithDeltaExprEval.codegen(this, interval, params, context);
        }
    }

    public static class IntervalComputerCoincidesWithDeltaExprEval implements IntervalComputerEval {

        private static final Logger log = LoggerFactory.getLogger(IntervalComputerCoincidesWithDeltaExprForge.class);

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

        public static CodegenExpression codegen(IntervalComputerCoincidesWithDeltaExprForge forge, CodegenParamSetIntervalNonPremade interval, CodegenParamSetExprPremade params, CodegenContext context) {
            CodegenParamSetIntervalPremade premade = CodegenParamSetIntervalPremade.INSTANCE;
            CodegenBlock block = context.addMethod(Boolean.class, IntervalComputerCoincidesWithDeltaExprEval.class).add(premade).add(params).begin()
                    .declareVar(long.class, "startValue", forge.start.codegen(staticMethod(Math.class, "min", premade.leftStart(), premade.rightStart()), params, context))
                    .declareVar(long.class, "endValue", forge.finish.codegen(staticMethod(Math.class, "min", premade.leftEnd(), premade.rightEnd()), params, context));
            block.ifCondition(or(relational(ref("startValue"), LT, constant(0)), relational(ref("endValue"), LT, constant(0))))
                    .expression(staticMethod(IntervalComputerCoincidesWithDeltaExprEval.class, "warnCoincideStartEndLessZero"))
                    .blockReturn(constantNull());
            CodegenMethodId method = block.methodReturn(staticMethod(IntervalComputerConstantCoincides.class, "computeIntervalCoincides", premade.leftStart(), premade.leftEnd(), premade.rightStart(), premade.rightEnd(), ref("startValue"), ref("endValue")));
            return localMethodBuild(method).passAll(interval).passAll(params).call();
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

        public CodegenExpression codegen(CodegenParamSetIntervalNonPremade interval, CodegenParamSetExprPremade params, CodegenContext context) {
            return and(equalsIdentity(interval.leftStart(), interval.rightStart()), equalsIdentity(interval.leftEnd(), interval.rightEnd()));
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

        public CodegenExpression codegen(CodegenParamSetIntervalNonPremade interval, CodegenParamSetExprPremade params, CodegenContext context) {
            return and(relational(interval.rightStart(), LT, interval.leftStart()), relational(interval.leftEnd(), LT, interval.rightEnd()));
        }
    }

    public static class IntervalComputerIncludesNoParam implements IntervalComputerForge, IntervalComputerEval {

        public IntervalComputerEval makeComputerEval() {
            return this;
        }

        public Boolean compute(long leftStart, long leftEnd, long rightStart, long rightEnd, EventBean[] eventsPerStream, boolean newData, ExprEvaluatorContext context) {
            return leftStart < rightStart && rightEnd < leftEnd;
        }

        public CodegenExpression codegen(CodegenParamSetIntervalNonPremade interval, CodegenParamSetExprPremade params, CodegenContext context) {
            return and(relational(interval.leftStart(), LT, interval.rightStart()), relational(interval.rightEnd(), LT, interval.leftEnd()));
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

        public CodegenExpression codegen(CodegenParamSetIntervalNonPremade interval, CodegenParamSetExprPremade params, CodegenContext context) {
            return IntervalComputerDuringAndIncludesThresholdEval.codegen(this, interval, params, context);
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

        public static CodegenExpression codegen(IntervalComputerDuringAndIncludesThresholdForge forge, CodegenParamSetIntervalNonPremade interval, CodegenParamSetExprPremade params, CodegenContext context) {
            CodegenParamSetIntervalPremade premade = CodegenParamSetIntervalPremade.INSTANCE;
            CodegenBlock block = context.addMethod(boolean.class, IntervalComputerDuringAndIncludesThresholdEval.class).add(premade).add(params).begin()
                    .declareVar(long.class, "thresholdValue", forge.threshold.codegen(premade.leftStart(), params, context));

            CodegenMethodId method;
            if (forge.during) {
                method = block.declareVar(long.class, "deltaStart", op(premade.leftStart(), "-", premade.rightStart()))
                        .ifConditionReturnConst(or(relational(ref("deltaStart"), LE, constant(0)), relational(ref("deltaStart"), GT, ref("thresholdValue"))), false)
                        .declareVar(long.class, "deltaEnd", op(premade.rightEnd(), "-", premade.leftEnd()))
                        .methodReturn(not(or(relational(ref("deltaEnd"), LE, constant(0)), relational(ref("deltaEnd"), GT, ref("thresholdValue")))));
            } else {
                method = block.declareVar(long.class, "deltaStart", op(premade.rightStart(), "-", premade.leftStart()))
                        .ifConditionReturnConst(or(relational(ref("deltaStart"), LE, constant(0)), relational(ref("deltaStart"), GT, ref("thresholdValue"))), false)
                        .declareVar(long.class, "deltaEnd", op(premade.leftEnd(), "-", premade.rightEnd()))
                        .methodReturn(not(or(relational(ref("deltaEnd"), LE, constant(0)), relational(ref("deltaEnd"), GT, ref("thresholdValue")))));
            }
            return localMethodBuild(method).passAll(interval).passAll(params).call();
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

        public CodegenExpression codegen(CodegenParamSetIntervalNonPremade interval, CodegenParamSetExprPremade params, CodegenContext context) {
            return IntervalComputerDuringAndIncludesMinMaxEval.codegen(this, interval, params, context);
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

        public static CodegenExpression codegen(IntervalComputerDuringAndIncludesMinMax forge, CodegenParamSetIntervalNonPremade interval, CodegenParamSetExprPremade params, CodegenContext context) {
            CodegenParamSetIntervalPremade premade = CodegenParamSetIntervalPremade.INSTANCE;
            CodegenBlock block = context.addMethod(boolean.class, IntervalComputerDuringAndIncludesMinMaxEval.class).add(premade).add(params).begin()
                    .declareVar(long.class, "min", forge.minEval.codegen(premade.leftStart(), params, context))
                    .declareVar(long.class, "max", forge.maxEval.codegen(premade.rightEnd(), params, context));
            CodegenMethodId method = block.methodReturn(staticMethod(IntervalComputerDuringAndIncludesMinMaxEval.class,
                    forge.during ? "computeIntervalDuring" : "computeIntervalIncludes",
                    premade.leftStart(), premade.leftEnd(), premade.rightStart(), premade.rightEnd(), ref("min"), ref("max"), ref("min"), ref("max")));
            return localMethodBuild(method).passAll(interval).passAll(params).call();
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

        public CodegenExpression codegen(CodegenParamSetIntervalNonPremade interval, CodegenParamSetExprPremade params, CodegenContext context) {
            return IntervalComputerDuringMinMaxStartEndEval.codegen(this, interval, params, context);
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

        public static CodegenExpression codegen(IntervalComputerDuringMinMaxStartEndForge forge, CodegenParamSetIntervalNonPremade interval, CodegenParamSetExprPremade params, CodegenContext context) {
            CodegenParamSetIntervalPremade premade = CodegenParamSetIntervalPremade.INSTANCE;
            CodegenBlock block = context.addMethod(boolean.class, IntervalComputerDuringMinMaxStartEndEval.class).add(premade).add(params).begin()
                    .declareVar(long.class, "minStart", forge.minStartEval.codegen(premade.rightStart(), params, context))
                    .declareVar(long.class, "maxStart", forge.maxStartEval.codegen(premade.rightStart(), params, context))
                    .declareVar(long.class, "minEnd", forge.minEndEval.codegen(premade.rightEnd(), params, context))
                    .declareVar(long.class, "maxEnd", forge.maxEndEval.codegen(premade.rightEnd(), params, context));
            CodegenMethodId method = block.methodReturn(staticMethod(IntervalComputerDuringAndIncludesMinMaxEval.class,
                    forge.during ? "computeIntervalDuring" : "computeIntervalIncludes",
                    premade.leftStart(), premade.leftEnd(), premade.rightStart(), premade.rightEnd(), ref("minStart"), ref("maxStart"), ref("minEnd"), ref("maxEnd")));
            return localMethodBuild(method).passAll(interval).passAll(params).call();
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

        public CodegenExpression codegen(CodegenParamSetIntervalNonPremade interval, CodegenParamSetExprPremade params, CodegenContext context) {
            return and(relational(interval.rightStart(), LT, interval.leftStart()), equalsIdentity(interval.leftEnd(), interval.rightEnd()));
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

        public CodegenExpression codegen(CodegenParamSetIntervalNonPremade interval, CodegenParamSetExprPremade params, CodegenContext context) {
            return IntervalComputerFinishesThresholdEval.codegen(this, interval, params, context);
        }
    }

    public static class IntervalComputerFinishesThresholdEval implements IntervalComputerEval {
        private static final Logger log = LoggerFactory.getLogger(IntervalComputerFinishesThresholdForge.class);

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

        public static CodegenExpression codegen(IntervalComputerFinishesThresholdForge forge, CodegenParamSetIntervalNonPremade interval, CodegenParamSetExprPremade params, CodegenContext context) {
            CodegenParamSetIntervalPremade premade = CodegenParamSetIntervalPremade.INSTANCE;
            CodegenMethodId method = context.addMethod(Boolean.class, IntervalComputerFinishesThresholdEval.class).add(premade).add(params).begin()
                    .declareVar(long.class, "threshold", forge.thresholdExpr.codegen(staticMethod(Math.class, "min", premade.leftEnd(), premade.rightEnd()), params, context))
                    .ifCondition(relational(ref("threshold"), LT, constant(0)))
                    .expression(staticMethod(IntervalComputerFinishesThresholdEval.class, "logWarningIntervalFinishThreshold"))
                    .blockReturn(constantNull())
                    .ifConditionReturnConst(relational(premade.rightStart(), GE, premade.leftStart()), false)
                    .declareVar(long.class, "delta", staticMethod(Math.class, "abs", op(premade.leftEnd(), "-", premade.rightEnd())))
                    .methodReturn(relational(ref("delta"), LE, ref("threshold")));
            return localMethodBuild(method).passAll(interval).passAll(params).call();
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

        public CodegenExpression codegen(CodegenParamSetIntervalNonPremade interval, CodegenParamSetExprPremade params, CodegenContext context) {
            return and(relational(interval.leftStart(), LT, interval.rightStart()), equalsIdentity(interval.leftEnd(), interval.rightEnd()));
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

        public CodegenExpression codegen(CodegenParamSetIntervalNonPremade interval, CodegenParamSetExprPremade params, CodegenContext context) {
            return IntervalComputerFinishedByThresholdEval.codegen(this, interval, params, context);
        }
    }

    public static class IntervalComputerFinishedByThresholdEval implements IntervalComputerEval {

        private static final Logger log = LoggerFactory.getLogger(IntervalComputerFinishedByThresholdForge.class);
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

        public static CodegenExpression codegen(IntervalComputerFinishedByThresholdForge forge, CodegenParamSetIntervalNonPremade interval, CodegenParamSetExprPremade params, CodegenContext context) {
            CodegenParamSetIntervalPremade premade = CodegenParamSetIntervalPremade.INSTANCE;
            CodegenMethodId method = context.addMethod(Boolean.class, IntervalComputerFinishedByThresholdEval.class).add(premade).add(params).begin()
                    .declareVar(long.class, "threshold", forge.thresholdExpr.codegen(staticMethod(Math.class, "min", premade.rightEnd(), premade.leftEnd()), params, context))
                    .ifCondition(relational(ref("threshold"), LT, constant(0)))
                    .expression(staticMethod(IntervalComputerFinishedByThresholdEval.class, "logWarningIntervalFinishedByThreshold"))
                    .blockReturn(constantNull())
                    .ifConditionReturnConst(relational(premade.leftStart(), GE, premade.rightStart()), false)
                    .declareVar(long.class, "delta", staticMethod(Math.class, "abs", op(premade.leftEnd(), "-", premade.rightEnd())))
                    .methodReturn(relational(ref("delta"), LE, ref("threshold")));
            return localMethodBuild(method).passAll(interval).passAll(params).call();
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

        public CodegenExpression codegen(CodegenParamSetIntervalNonPremade interval, CodegenParamSetExprPremade params, CodegenContext context) {
            return equalsIdentity(interval.leftEnd(), interval.rightStart());
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

        public CodegenExpression codegen(CodegenParamSetIntervalNonPremade interval, CodegenParamSetExprPremade params, CodegenContext context) {
            return IntervalComputerMeetsThresholdEval.codegen(this, interval, params, context);
        }
    }

    public static class IntervalComputerMeetsThresholdEval implements IntervalComputerEval {

        private static final Logger log = LoggerFactory.getLogger(IntervalComputerMeetsThresholdForge.class);
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

        public static CodegenExpression codegen(IntervalComputerMeetsThresholdForge forge, CodegenParamSetIntervalNonPremade interval, CodegenParamSetExprPremade params, CodegenContext context) {
            CodegenParamSetIntervalPremade premade = CodegenParamSetIntervalPremade.INSTANCE;
            CodegenMethodId method = context.addMethod(Boolean.class, IntervalComputerMeetsThresholdEval.class).add(premade).add(params).begin()
                    .declareVar(long.class, "threshold", forge.thresholdExpr.codegen(staticMethod(Math.class, "min", premade.leftEnd(), premade.rightStart()), params, context))
                    .ifCondition(relational(ref("threshold"), LT, constant(0)))
                    .expression(staticMethod(IntervalComputerMeetsThresholdEval.class, "logWarningIntervalMeetsThreshold"))
                    .blockReturn(constantNull())
                    .declareVar(long.class, "delta", staticMethod(Math.class, "abs", op(premade.rightStart(), "-", premade.leftEnd())))
                    .methodReturn(relational(ref("delta"), LE, ref("threshold")));
            return localMethodBuild(method).passAll(interval).passAll(params).call();
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

        public CodegenExpression codegen(CodegenParamSetIntervalNonPremade interval, CodegenParamSetExprPremade params, CodegenContext context) {
            return equalsIdentity(interval.rightEnd(), interval.leftStart());
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

        public CodegenExpression codegen(CodegenParamSetIntervalNonPremade interval, CodegenParamSetExprPremade params, CodegenContext context) {
            return IntervalComputerMetByThresholdEval.codegen(this, interval, params, context);
        }
    }

    public static class IntervalComputerMetByThresholdEval implements IntervalComputerEval {

        private static final Logger log = LoggerFactory.getLogger(IntervalComputerMetByThresholdForge.class);
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

        public static CodegenExpression codegen(IntervalComputerMetByThresholdForge forge, CodegenParamSetIntervalNonPremade interval, CodegenParamSetExprPremade params, CodegenContext context) {
            CodegenParamSetIntervalPremade premade = CodegenParamSetIntervalPremade.INSTANCE;
            CodegenMethodId method = context.addMethod(Boolean.class, IntervalComputerMetByThresholdEval.class).add(premade).add(params).begin()
                    .declareVar(long.class, "threshold", forge.thresholdExpr.codegen(staticMethod(Math.class, "min", premade.leftStart(), premade.rightEnd()), params, context))
                    .ifCondition(relational(ref("threshold"), LT, constant(0)))
                    .expression(staticMethod(IntervalComputerMetByThresholdEval.class, "logWarningIntervalMetByThreshold"))
                    .blockReturn(constantNull())
                    .declareVar(long.class, "delta", staticMethod(Math.class, "abs", op(premade.leftStart(), "-", premade.rightEnd())))
                    .methodReturn(relational(ref("delta"), LE, ref("threshold")));
            return localMethodBuild(method).passAll(interval).passAll(params).call();
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

        public CodegenExpression codegen(CodegenParamSetIntervalNonPremade interval, CodegenParamSetExprPremade params, CodegenContext context) {
            return and(
                    relational(interval.leftStart(), LT, interval.rightStart()),
                    relational(interval.rightStart(), LT, interval.leftEnd()),
                    relational(interval.leftEnd(), LT, interval.rightEnd()));
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

        public CodegenExpression codegen(CodegenParamSetIntervalNonPremade interval, CodegenParamSetExprPremade params, CodegenContext context) {
            return IntervalComputerOverlapsAndByThresholdEval.codegen(this, interval, params, context);
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

        public static CodegenExpression codegen(IntervalComputerOverlapsAndByThreshold forge, CodegenParamSetIntervalNonPremade interval, CodegenParamSetExprPremade params, CodegenContext context) {
            CodegenParamSetIntervalPremade premade = CodegenParamSetIntervalPremade.INSTANCE;
            CodegenBlock block = context.addMethod(boolean.class, IntervalComputerOverlapsAndByThresholdEval.class).add(premade).add(params).begin()
                    .declareVar(long.class, "threshold", forge.thresholdExpr.codegen(forge.overlaps ? premade.leftStart() : premade.rightStart(), params, context));
            CodegenMethodId method;
            if (forge.overlaps) {
                method = block.methodReturn(staticMethod(IntervalComputerOverlapsAndByThresholdEval.class, "computeIntervalOverlaps",
                        premade.leftStart(), premade.leftEnd(), premade.rightStart(), premade.rightEnd(), constant(0), ref("threshold")));
            } else {
                method = block.methodReturn(staticMethod(IntervalComputerOverlapsAndByThresholdEval.class, "computeIntervalOverlaps",
                        premade.rightStart(), premade.rightEnd(), premade.leftStart(), premade.leftEnd(), constant(0), ref("threshold")));

            }
            return localMethodBuild(method).passAll(interval).passAll(params).call();
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

        public CodegenExpression codegen(CodegenParamSetIntervalNonPremade interval, CodegenParamSetExprPremade params, CodegenContext context) {
            return IntervalComputerOverlapsAndByMinMaxEval.codegen(this, interval, params, context);
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

        public static CodegenExpression codegen(IntervalComputerOverlapsAndByMinMaxForge forge, CodegenParamSetIntervalNonPremade interval, CodegenParamSetExprPremade params, CodegenContext context) {
            CodegenParamSetIntervalPremade premade = CodegenParamSetIntervalPremade.INSTANCE;
            CodegenBlock block = context.addMethod(boolean.class, IntervalComputerOverlapsAndByMinMaxEval.class).add(premade).add(params).begin()
                    .declareVar(long.class, "minThreshold", forge.minEval.codegen(forge.overlaps ? premade.leftStart() : premade.rightStart(), params, context))
                    .declareVar(long.class, "maxThreshold", forge.maxEval.codegen(forge.overlaps ? premade.leftEnd() : premade.rightEnd(), params, context));
            CodegenMethodId method;
            if (forge.overlaps) {
                method = block.methodReturn(staticMethod(IntervalComputerOverlapsAndByThresholdEval.class, "computeIntervalOverlaps",
                        premade.leftStart(), premade.leftEnd(), premade.rightStart(), premade.rightEnd(), ref("minThreshold"), ref("maxThreshold")));
            } else {
                method = block.methodReturn(staticMethod(IntervalComputerOverlapsAndByThresholdEval.class, "computeIntervalOverlaps",
                        premade.rightStart(), premade.rightEnd(), premade.leftStart(), premade.leftEnd(), ref("minThreshold"), ref("maxThreshold")));
            }
            return localMethodBuild(method).passAll(interval).passAll(params).call();
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

        public CodegenExpression codegen(CodegenParamSetIntervalNonPremade interval, CodegenParamSetExprPremade params, CodegenContext context) {
            return and(
                    relational(interval.rightStart(), LT, interval.leftStart()),
                    relational(interval.leftStart(), LT, interval.rightEnd()),
                    relational(interval.rightEnd(), LT, interval.leftEnd()));
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

        public CodegenExpression codegen(CodegenParamSetIntervalNonPremade interval, CodegenParamSetExprPremade params, CodegenContext context) {
            return and(equalsIdentity(interval.leftStart(), interval.rightStart()), relational(interval.leftEnd(), LT, interval.rightEnd()));
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

        public CodegenExpression codegen(CodegenParamSetIntervalNonPremade interval, CodegenParamSetExprPremade params, CodegenContext context) {
            return IntervalComputerStartsThresholdEval.codegen(this, interval, params, context);
        }
    }

    public static class IntervalComputerStartsThresholdEval implements IntervalComputerEval {

        private static final Logger log = LoggerFactory.getLogger(IntervalComputerStartsThresholdEval.class);

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

        public static CodegenExpression codegen(IntervalComputerStartsThresholdForge forge, CodegenParamSetIntervalNonPremade interval, CodegenParamSetExprPremade params, CodegenContext context) {
            CodegenParamSetIntervalPremade premade = CodegenParamSetIntervalPremade.INSTANCE;
            CodegenMethodId method = context.addMethod(Boolean.class, IntervalComputerStartsThresholdEval.class).add(premade).add(params).begin()
                    .declareVar(long.class, "threshold", forge.thresholdExpr.codegen(staticMethod(Math.class, "min", premade.leftStart(), premade.rightStart()), params, context))
                    .ifCondition(relational(ref("threshold"), LT, constant(0)))
                    .expression(staticMethod(IntervalComputerStartsThresholdEval.class, "logWarningIntervalStartsThreshold"))
                    .blockReturn(constantNull())
                    .declareVar(long.class, "delta", staticMethod(Math.class, "abs", op(premade.leftStart(), "-", premade.rightStart())))
                    .methodReturn(and(relational(ref("delta"), LE, ref("threshold")), relational(premade.leftEnd(), LT, premade.rightEnd())));
            return localMethodBuild(method).passAll(interval).passAll(params).call();
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

        public CodegenExpression codegen(CodegenParamSetIntervalNonPremade interval, CodegenParamSetExprPremade params, CodegenContext context) {
            return and(equalsIdentity(interval.leftStart(), interval.rightStart()), relational(interval.leftEnd(), GT, interval.rightEnd()));
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

        public CodegenExpression codegen(CodegenParamSetIntervalNonPremade interval, CodegenParamSetExprPremade params, CodegenContext context) {
            return IntervalComputerStartedByThresholdEval.codegen(this, interval, params, context);
        }
    }

    public static class IntervalComputerStartedByThresholdEval implements IntervalComputerEval {

        private static final Logger log = LoggerFactory.getLogger(IntervalComputerStartedByThresholdForge.class);

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

        public static CodegenExpression codegen(IntervalComputerStartedByThresholdForge forge, CodegenParamSetIntervalNonPremade interval, CodegenParamSetExprPremade params, CodegenContext context) {
            CodegenParamSetIntervalPremade premade = CodegenParamSetIntervalPremade.INSTANCE;
            CodegenMethodId method = context.addMethod(Boolean.class, IntervalComputerStartedByThresholdEval.class).add(premade).add(params).begin()
                    .declareVar(long.class, "threshold", forge.thresholdExpr.codegen(staticMethod(Math.class, "min", premade.leftStart(), premade.rightStart()), params, context))
                    .ifCondition(relational(ref("threshold"), LT, constant(0)))
                    .expression(staticMethod(IntervalComputerStartedByThresholdEval.class, "logWarningIntervalStartedByThreshold"))
                    .blockReturn(constantNull())
                    .declareVar(long.class, "delta", staticMethod(Math.class, "abs", op(premade.leftStart(), "-", premade.rightStart())))
                    .methodReturn(and(relational(ref("delta"), LE, ref("threshold")), relational(premade.leftEnd(), GT, premade.rightEnd())));
            return localMethodBuild(method).passAll(interval).passAll(params).call();
        }
    }
}
