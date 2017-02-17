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
import com.espertech.esper.epl.datetime.eval.DatetimeMethodEnum;
import com.espertech.esper.epl.expression.core.*;
import com.espertech.esper.epl.expression.time.ExprTimePeriod;
import com.espertech.esper.epl.expression.time.ExprTimePeriodEvalDeltaConst;
import com.espertech.esper.epl.expression.time.ExprTimePeriodEvalDeltaNonConst;
import com.espertech.esper.epl.expression.time.TimeAbacus;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;

public class IntervalComputerFactory {

    public static IntervalComputer make(DatetimeMethodEnum method, List<ExprNode> expressions, TimeAbacus timeAbacus) throws ExprValidationException {
        ExprOptionalConstant[] parameters = getParameters(expressions, timeAbacus);

        if (method == DatetimeMethodEnum.BEFORE) {
            if (parameters.length == 0) {
                return new IntervalComputerBeforeNoParam();
            }
            IntervalStartEndParameterPair pair = IntervalStartEndParameterPair.fromParamsWithLongMaxEnd(parameters);
            if (pair.isConstant()) {
                return new IntervalComputerConstantBefore(pair);
            }
            return new IntervalComputerBeforeWithDeltaExpr(pair);
        } else if (method == DatetimeMethodEnum.AFTER) {
            if (parameters.length == 0) {
                return new IntervalComputerAfterNoParam();
            }
            IntervalStartEndParameterPair pair = IntervalStartEndParameterPair.fromParamsWithLongMaxEnd(parameters);
            if (pair.isConstant()) {
                return new IntervalComputerConstantAfter(pair);
            }
            return new IntervalComputerAfterWithDeltaExpr(pair);
        } else if (method == DatetimeMethodEnum.COINCIDES) {
            if (parameters.length == 0) {
                return new IntervalComputerCoincidesNoParam();
            }
            IntervalStartEndParameterPair pair = IntervalStartEndParameterPair.fromParamsWithSameEnd(parameters);
            if (pair.isConstant()) {
                return new IntervalComputerConstantCoincides(pair);
            }
            return new IntervalComputerCoincidesWithDeltaExpr(pair);
        } else if (method == DatetimeMethodEnum.DURING || method == DatetimeMethodEnum.INCLUDES) {
            if (parameters.length == 0) {
                if (method == DatetimeMethodEnum.DURING) {
                    return new IntervalComputerDuringNoParam();
                }
                return new IntervalComputerIncludesNoParam();
            }
            IntervalStartEndParameterPair pair = IntervalStartEndParameterPair.fromParamsWithSameEnd(parameters);
            if (parameters.length == 1) {
                return new IntervalComputerDuringAndIncludesThreshold(method == DatetimeMethodEnum.DURING, pair.getStart().getEvaluator());
            }
            if (parameters.length == 2) {
                return new IntervalComputerDuringAndIncludesMinMax(method == DatetimeMethodEnum.DURING, pair.getStart().getEvaluator(), pair.getEnd().getEvaluator());
            }
            return new IntervalComputerDuringMinMaxStartEnd(method == DatetimeMethodEnum.DURING, getEvaluators(expressions, timeAbacus));
        } else if (method == DatetimeMethodEnum.FINISHES) {
            if (parameters.length == 0) {
                return new IntervalComputerFinishesNoParam();
            }
            validateConstantThreshold("finishes", parameters[0]);
            return new IntervalComputerFinishesThreshold(parameters[0].getEvaluator());
        } else if (method == DatetimeMethodEnum.FINISHEDBY) {
            if (parameters.length == 0) {
                return new IntervalComputerFinishedByNoParam();
            }
            validateConstantThreshold("finishedby", parameters[0]);
            return new IntervalComputerFinishedByThreshold(parameters[0].getEvaluator());
        } else if (method == DatetimeMethodEnum.MEETS) {
            if (parameters.length == 0) {
                return new IntervalComputerMeetsNoParam();
            }
            validateConstantThreshold("meets", parameters[0]);
            return new IntervalComputerMeetsThreshold(parameters[0].getEvaluator());
        } else if (method == DatetimeMethodEnum.METBY) {
            if (parameters.length == 0) {
                return new IntervalComputerMetByNoParam();
            }
            validateConstantThreshold("metBy", parameters[0]);
            return new IntervalComputerMetByThreshold(parameters[0].getEvaluator());
        } else if (method == DatetimeMethodEnum.OVERLAPS || method == DatetimeMethodEnum.OVERLAPPEDBY) {
            if (parameters.length == 0) {
                if (method == DatetimeMethodEnum.OVERLAPS) {
                    return new IntervalComputerOverlapsNoParam();
                }
                return new IntervalComputerOverlappedByNoParam();
            }
            if (parameters.length == 1) {
                return new IntervalComputerOverlapsAndByThreshold(method == DatetimeMethodEnum.OVERLAPS, parameters[0].getEvaluator());
            }
            return new IntervalComputerOverlapsAndByMinMax(method == DatetimeMethodEnum.OVERLAPS, parameters[0].getEvaluator(), parameters[1].getEvaluator());
        } else if (method == DatetimeMethodEnum.STARTS) {
            if (parameters.length == 0) {
                return new IntervalComputerStartsNoParam();
            }
            validateConstantThreshold("starts", parameters[0]);
            return new IntervalComputerStartsThreshold(parameters[0].getEvaluator());
        } else if (method == DatetimeMethodEnum.STARTEDBY) {
            if (parameters.length == 0) {
                return new IntervalComputerStartedByNoParam();
            }
            validateConstantThreshold("startedBy", parameters[0]);
            return new IntervalComputerStartedByThreshold(parameters[0].getEvaluator());
        }
        throw new IllegalArgumentException("Unknown datetime method '" + method + "'");
    }

    private static void validateConstantThreshold(String method, ExprOptionalConstant param) throws ExprValidationException {
        if (param.getOptionalConstant() != null && (param.getOptionalConstant()).longValue() < 0) {
            throw new ExprValidationException("The " + method + " date-time method does not allow negative threshold value");
        }
    }

    private static ExprOptionalConstant[] getParameters(List<ExprNode> expressions, TimeAbacus timeAbacus) {
        ExprOptionalConstant[] parameters = new ExprOptionalConstant[expressions.size() - 1];
        for (int i = 1; i < expressions.size(); i++) {
            parameters[i - 1] = getExprOrConstant(expressions.get(i), timeAbacus);
        }
        return parameters;
    }

    private static IntervalDeltaExprEvaluator[] getEvaluators(List<ExprNode> expressions, TimeAbacus timeAbacus) {
        IntervalDeltaExprEvaluator[] parameters = new IntervalDeltaExprEvaluator[expressions.size() - 1];
        for (int i = 1; i < expressions.size(); i++) {
            parameters[i - 1] = getExprOrConstant(expressions.get(i), timeAbacus).getEvaluator();
        }
        return parameters;
    }

    private static ExprOptionalConstant getExprOrConstant(ExprNode exprNode, final TimeAbacus timeAbacus) {
        if (exprNode instanceof ExprTimePeriod) {
            final ExprTimePeriod timePeriod = (ExprTimePeriod) exprNode;
            if (!timePeriod.isHasMonth() && !timePeriod.isHasYear()) {
                // no-month and constant
                if (exprNode.isConstantResult()) {
                    double sec = timePeriod.evaluateAsSeconds(null, true, null);
                    final long l = timeAbacus.deltaForSecondsDouble(sec);
                    IntervalDeltaExprEvaluator eval = new IntervalDeltaExprEvaluator() {
                        public long evaluate(long reference, EventBean[] eventsPerStream, boolean isNewData, ExprEvaluatorContext context) {
                            return l;
                        }
                    };
                    return new ExprOptionalConstant(eval, l);
                } else {
                    // no-month and not constant
                    IntervalDeltaExprEvaluator eval = new IntervalDeltaExprEvaluator() {
                        public long evaluate(long reference, EventBean[] eventsPerStream, boolean isNewData, ExprEvaluatorContext context) {
                            double sec = timePeriod.evaluateAsSeconds(eventsPerStream, isNewData, context);
                            return timeAbacus.deltaForSecondsDouble(sec);
                        }
                    };
                    return new ExprOptionalConstant(eval, null);
                }
            } else {
                // has-month and constant
                if (exprNode.isConstantResult()) {
                    final ExprTimePeriodEvalDeltaConst timerPeriodConst = timePeriod.constEvaluator(null);
                    IntervalDeltaExprEvaluator eval = new IntervalDeltaExprEvaluator() {
                        public long evaluate(long reference, EventBean[] eventsPerStream, boolean isNewData, ExprEvaluatorContext context) {
                            return timerPeriodConst.deltaAdd(reference);
                        }
                    };
                    return new ExprOptionalConstant(eval, null);
                } else {
                    // has-month and not constant
                    final ExprTimePeriodEvalDeltaNonConst timerPeriodNonConst = timePeriod.nonconstEvaluator();
                    IntervalDeltaExprEvaluator eval = new IntervalDeltaExprEvaluator() {
                        public long evaluate(long reference, EventBean[] eventsPerStream, boolean isNewData, ExprEvaluatorContext context) {
                            return timerPeriodNonConst.deltaAdd(reference, eventsPerStream, isNewData, context);
                        }
                    };
                    return new ExprOptionalConstant(eval, null);
                }
            }
        } else if (ExprNodeUtility.isConstantValueExpr(exprNode)) {
            ExprConstantNode constantNode = (ExprConstantNode) exprNode;
            final long l = ((Number) constantNode.getConstantValue(null)).longValue();
            IntervalDeltaExprEvaluator eval = new IntervalDeltaExprEvaluator() {
                public long evaluate(long reference, EventBean[] eventsPerStream, boolean isNewData, ExprEvaluatorContext context) {
                    return l;
                }
            };
            return new ExprOptionalConstant(eval, l);
        } else {
            final ExprEvaluator evaluator = exprNode.getExprEvaluator();
            IntervalDeltaExprEvaluator eval = new IntervalDeltaExprEvaluator() {
                public long evaluate(long reference, EventBean[] eventsPerStream, boolean isNewData, ExprEvaluatorContext context) {
                    return ((Number) evaluator.evaluate(eventsPerStream, isNewData, context)).longValue();
                }
            };
            return new ExprOptionalConstant(eval, null);
        }
    }

    /**
     * After.
     */
    public static class IntervalComputerConstantAfter extends IntervalComputerConstantBase implements IntervalComputer {

        public IntervalComputerConstantAfter(IntervalStartEndParameterPair pair) {
            super(pair, true);
        }

        public Boolean compute(long leftStart, long leftEnd, long rightStart, long rightEnd, EventBean[] eventsPerStream, boolean newData, ExprEvaluatorContext context) {
            return computeInternal(leftStart, leftEnd, rightStart, rightEnd, start, end);
        }

        public static Boolean computeInternal(long leftStart, long leftEnd, long rightStart, long rightEnd, long start, long end) {
            long delta = leftStart - rightEnd;
            return start <= delta && delta <= end;
        }
    }

    public static class IntervalComputerAfterWithDeltaExpr implements IntervalComputer {

        private final IntervalDeltaExprEvaluator start;
        private final IntervalDeltaExprEvaluator finish;

        public IntervalComputerAfterWithDeltaExpr(IntervalStartEndParameterPair pair) {
            this.start = pair.getStart().getEvaluator();
            this.finish = pair.getEnd().getEvaluator();
        }

        public Boolean compute(long leftStart, long leftEnd, long rightStart, long rightEnd, EventBean[] eventsPerStream, boolean newData, ExprEvaluatorContext context) {
            long rangeStartDelta = start.evaluate(rightStart, eventsPerStream, newData, context);
            long rangeEndDelta = finish.evaluate(rightStart, eventsPerStream, newData, context);
            if (rangeStartDelta > rangeEndDelta) {
                return IntervalComputerConstantAfter.computeInternal(leftStart, leftEnd, rightStart, rightEnd, rangeEndDelta, rangeStartDelta);
            } else {
                return IntervalComputerConstantAfter.computeInternal(leftStart, leftEnd, rightStart, rightEnd, rangeStartDelta, rangeEndDelta);
            }
        }
    }

    public static class IntervalComputerAfterNoParam implements IntervalComputer {

        public Boolean compute(long leftStart, long leftEnd, long rightStart, long rightEnd, EventBean[] eventsPerStream, boolean newData, ExprEvaluatorContext context) {
            return leftStart > rightEnd;
        }
    }

    /**
     * Before.
     */
    public static class IntervalComputerConstantBefore extends IntervalComputerConstantBase implements IntervalComputer {

        public IntervalComputerConstantBefore(IntervalStartEndParameterPair pair) {
            super(pair, true);
        }

        public Boolean compute(long leftStart, long leftEnd, long rightStart, long rightEnd, EventBean[] eventsPerStream, boolean newData, ExprEvaluatorContext context) {
            return computeInternal(leftStart, leftEnd, rightStart, start, end);
        }

        public static Boolean computeInternal(long left, long leftEnd, long right, long start, long end) {
            long delta = right - leftEnd;
            return start <= delta && delta <= end;
        }
    }

    public static class IntervalComputerBeforeWithDeltaExpr implements IntervalComputer {

        private final IntervalDeltaExprEvaluator start;
        private final IntervalDeltaExprEvaluator finish;

        public IntervalComputerBeforeWithDeltaExpr(IntervalStartEndParameterPair pair) {
            this.start = pair.getStart().getEvaluator();
            this.finish = pair.getEnd().getEvaluator();
        }

        public Boolean compute(long leftStart, long leftEnd, long rightStart, long rightEnd, EventBean[] eventsPerStream, boolean newData, ExprEvaluatorContext context) {
            long rangeStartDelta = start.evaluate(leftEnd, eventsPerStream, newData, context);
            long rangeEndDelta = finish.evaluate(leftEnd, eventsPerStream, newData, context);
            if (rangeStartDelta > rangeEndDelta) {
                return IntervalComputerConstantBefore.computeInternal(leftStart, leftEnd, rightStart, rangeEndDelta, rangeStartDelta);
            } else {
                return IntervalComputerConstantBefore.computeInternal(leftStart, leftEnd, rightStart, rangeStartDelta, rangeEndDelta);
            }
        }
    }

    public static class IntervalComputerBeforeNoParam implements IntervalComputer {

        public Boolean compute(long leftStart, long leftEnd, long rightStart, long rightEnd, EventBean[] eventsPerStream, boolean newData, ExprEvaluatorContext context) {
            return leftEnd < rightStart;
        }
    }

    /**
     * Coincides.
     */
    public static class IntervalComputerConstantCoincides implements IntervalComputer {

        protected final long start;
        protected final long end;

        public IntervalComputerConstantCoincides(IntervalStartEndParameterPair pair) throws ExprValidationException {
            start = pair.getStart().getOptionalConstant();
            end = pair.getEnd().getOptionalConstant();
            if (start < 0 || end < 0) {
                throw new ExprValidationException("The coincides date-time method does not allow negative start and end values");
            }
        }

        public Boolean compute(long leftStart, long leftEnd, long rightStart, long rightEnd, EventBean[] eventsPerStream, boolean newData, ExprEvaluatorContext context) {
            return computeInternal(leftStart, leftEnd, rightStart, rightEnd, start, end);
        }

        public static Boolean computeInternal(long left, long leftEnd, long right, long rightEnd, long startThreshold, long endThreshold) {
            return Math.abs(left - right) <= startThreshold &&
                    Math.abs(leftEnd - rightEnd) <= endThreshold;
        }
    }

    public static class IntervalComputerCoincidesWithDeltaExpr implements IntervalComputer {

        private static final Logger log = LoggerFactory.getLogger(IntervalComputerCoincidesWithDeltaExpr.class);

        private final IntervalDeltaExprEvaluator start;
        private final IntervalDeltaExprEvaluator finish;

        public IntervalComputerCoincidesWithDeltaExpr(IntervalStartEndParameterPair pair) {
            this.start = pair.getStart().getEvaluator();
            this.finish = pair.getEnd().getEvaluator();
        }

        public Boolean compute(long leftStart, long leftEnd, long rightStart, long rightEnd, EventBean[] eventsPerStream, boolean newData, ExprEvaluatorContext context) {
            long startValue = start.evaluate(Math.min(leftStart, rightStart), eventsPerStream, newData, context);
            long endValue = finish.evaluate(Math.min(leftEnd, rightEnd), eventsPerStream, newData, context);

            if (startValue < 0 || endValue < 0) {
                log.warn("The coincides date-time method does not allow negative start and end values");
                return null;
            }

            return IntervalComputerConstantCoincides.computeInternal(leftStart, leftEnd, rightStart, rightEnd, startValue, endValue);
        }
    }

    public static class IntervalComputerCoincidesNoParam implements IntervalComputer {

        public Boolean compute(long leftStart, long leftEnd, long rightStart, long rightEnd, EventBean[] eventsPerStream, boolean newData, ExprEvaluatorContext context) {
            return leftStart == rightStart && leftEnd == rightEnd;
        }
    }

    /**
     * During And Includes.
     */
    public static class IntervalComputerDuringNoParam implements IntervalComputer {

        public Boolean compute(long leftStart, long leftEnd, long rightStart, long rightEnd, EventBean[] eventsPerStream, boolean newData, ExprEvaluatorContext context) {
            return rightStart < leftStart && leftEnd < rightEnd;
        }
    }

    public static class IntervalComputerIncludesNoParam implements IntervalComputer {

        public Boolean compute(long leftStart, long leftEnd, long rightStart, long rightEnd, EventBean[] eventsPerStream, boolean newData, ExprEvaluatorContext context) {
            return leftStart < rightStart && rightEnd < leftEnd;
        }
    }

    public static class IntervalComputerDuringAndIncludesThreshold implements IntervalComputer {

        private final boolean during;
        private final IntervalDeltaExprEvaluator threshold;

        public IntervalComputerDuringAndIncludesThreshold(boolean during, IntervalDeltaExprEvaluator threshold) {
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
    }

    public static class IntervalComputerDuringAndIncludesMinMax implements IntervalComputer {

        private final boolean during;
        private final IntervalDeltaExprEvaluator minEval;
        private final IntervalDeltaExprEvaluator maxEval;

        public IntervalComputerDuringAndIncludesMinMax(boolean during, IntervalDeltaExprEvaluator minEval, IntervalDeltaExprEvaluator maxEval) {
            this.during = during;
            this.minEval = minEval;
            this.maxEval = maxEval;
        }

        public Boolean compute(long leftStart, long leftEnd, long rightStart, long rightEnd, EventBean[] eventsPerStream, boolean newData, ExprEvaluatorContext context) {
            long min = minEval.evaluate(leftStart, eventsPerStream, newData, context);
            long max = maxEval.evaluate(rightEnd, eventsPerStream, newData, context);
            if (during) {
                return computeInternalDuring(leftStart, leftEnd, rightStart, rightEnd, min, max, min, max);
            } else {
                return computeInternalIncludes(leftStart, leftEnd, rightStart, rightEnd, min, max, min, max);
            }
        }

        public static boolean computeInternalDuring(long left, long leftEnd, long right, long rightEnd,
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

        public static boolean computeInternalIncludes(long left, long leftEnd, long right, long rightEnd,
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

    public static class IntervalComputerDuringMinMaxStartEnd implements IntervalComputer {

        private final boolean during;
        private final IntervalDeltaExprEvaluator minStartEval;
        private final IntervalDeltaExprEvaluator maxStartEval;
        private final IntervalDeltaExprEvaluator minEndEval;
        private final IntervalDeltaExprEvaluator maxEndEval;

        public IntervalComputerDuringMinMaxStartEnd(boolean during, IntervalDeltaExprEvaluator[] parameters) {
            this.during = during;
            minStartEval = parameters[0];
            maxStartEval = parameters[1];
            minEndEval = parameters[2];
            maxEndEval = parameters[3];
        }

        public Boolean compute(long leftStart, long leftEnd, long rightStart, long rightEnd, EventBean[] eventsPerStream, boolean newData, ExprEvaluatorContext context) {

            long minStart = minStartEval.evaluate(rightStart, eventsPerStream, newData, context);
            long maxStart = maxStartEval.evaluate(rightStart, eventsPerStream, newData, context);
            long minEnd = minEndEval.evaluate(rightEnd, eventsPerStream, newData, context);
            long maxEnd = maxEndEval.evaluate(rightEnd, eventsPerStream, newData, context);

            if (during) {
                return IntervalComputerDuringAndIncludesMinMax.computeInternalDuring(leftStart, leftEnd, rightStart, rightEnd, minStart, maxStart, minEnd, maxEnd);
            } else {
                return IntervalComputerDuringAndIncludesMinMax.computeInternalIncludes(leftStart, leftEnd, rightStart, rightEnd, minStart, maxStart, minEnd, maxEnd);
            }
        }
    }

    /**
     * Finishes.
     */
    public static class IntervalComputerFinishesNoParam implements IntervalComputer {

        public Boolean compute(long leftStart, long leftEnd, long rightStart, long rightEnd, EventBean[] eventsPerStream, boolean newData, ExprEvaluatorContext context) {
            return rightStart < leftStart && (leftEnd == rightEnd);
        }
    }

    public static class IntervalComputerFinishesThreshold implements IntervalComputer {
        private static final Logger log = LoggerFactory.getLogger(IntervalComputerFinishesThreshold.class);

        private final IntervalDeltaExprEvaluator thresholdExpr;

        public IntervalComputerFinishesThreshold(IntervalDeltaExprEvaluator thresholdExpr) {
            this.thresholdExpr = thresholdExpr;
        }

        public Boolean compute(long leftStart, long leftEnd, long rightStart, long rightEnd, EventBean[] eventsPerStream, boolean newData, ExprEvaluatorContext context) {

            long threshold = thresholdExpr.evaluate(Math.min(leftEnd, rightEnd), eventsPerStream, newData, context);

            if (threshold < 0) {
                log.warn("The 'finishes' date-time method does not allow negative threshold");
                return null;
            }

            if (rightStart >= leftStart) {
                return false;
            }
            long delta = Math.abs(leftEnd - rightEnd);
            return delta <= threshold;
        }
    }

    /**
     * Finishes-By.
     */
    public static class IntervalComputerFinishedByNoParam implements IntervalComputer {

        public Boolean compute(long leftStart, long leftEnd, long rightStart, long rightEnd, EventBean[] eventsPerStream, boolean newData, ExprEvaluatorContext context) {
            return leftStart < rightStart && (leftEnd == rightEnd);
        }
    }

    public static class IntervalComputerFinishedByThreshold implements IntervalComputer {

        private static final Logger log = LoggerFactory.getLogger(IntervalComputerFinishedByThreshold.class);
        private final IntervalDeltaExprEvaluator thresholdExpr;

        public IntervalComputerFinishedByThreshold(IntervalDeltaExprEvaluator thresholdExpr) {
            this.thresholdExpr = thresholdExpr;
        }

        public Boolean compute(long leftStart, long leftEnd, long rightStart, long rightEnd, EventBean[] eventsPerStream, boolean newData, ExprEvaluatorContext context) {

            long threshold = thresholdExpr.evaluate(Math.min(rightEnd, leftEnd), eventsPerStream, newData, context);
            if (threshold < 0) {
                log.warn("The 'finishes' date-time method does not allow negative threshold");
                return null;
            }

            if (leftStart >= rightStart) {
                return false;
            }
            long delta = Math.abs(leftEnd - rightEnd);
            return delta <= threshold;
        }
    }

    /**
     * Meets.
     */
    public static class IntervalComputerMeetsNoParam implements IntervalComputer {

        public Boolean compute(long leftStart, long leftEnd, long rightStart, long rightEnd, EventBean[] eventsPerStream, boolean newData, ExprEvaluatorContext context) {
            return leftEnd == rightStart;
        }
    }

    public static class IntervalComputerMeetsThreshold implements IntervalComputer {

        private static final Logger log = LoggerFactory.getLogger(IntervalComputerMeetsThreshold.class);
        private final IntervalDeltaExprEvaluator thresholdExpr;

        public IntervalComputerMeetsThreshold(IntervalDeltaExprEvaluator thresholdExpr) {
            this.thresholdExpr = thresholdExpr;
        }

        public Boolean compute(long leftStart, long leftEnd, long rightStart, long rightEnd, EventBean[] eventsPerStream, boolean newData, ExprEvaluatorContext context) {
            long threshold = thresholdExpr.evaluate(Math.min(leftEnd, rightStart), eventsPerStream, newData, context);
            if (threshold < 0) {
                log.warn("The 'finishes' date-time method does not allow negative threshold");
                return null;
            }

            long delta = Math.abs(rightStart - leftEnd);
            return delta <= threshold;
        }
    }

    /**
     * Met-By.
     */
    public static class IntervalComputerMetByNoParam implements IntervalComputer {

        public Boolean compute(long leftStart, long leftEnd, long rightStart, long rightEnd, EventBean[] eventsPerStream, boolean newData, ExprEvaluatorContext context) {
            return rightEnd == leftStart;
        }
    }

    public static class IntervalComputerMetByThreshold implements IntervalComputer {

        private static final Logger log = LoggerFactory.getLogger(IntervalComputerMetByThreshold.class);
        private final IntervalDeltaExprEvaluator thresholdExpr;

        public IntervalComputerMetByThreshold(IntervalDeltaExprEvaluator thresholdExpr) {
            this.thresholdExpr = thresholdExpr;
        }

        public Boolean compute(long leftStart, long leftEnd, long rightStart, long rightEnd, EventBean[] eventsPerStream, boolean newData, ExprEvaluatorContext context) {

            long threshold = thresholdExpr.evaluate(Math.min(leftStart, rightEnd), eventsPerStream, newData, context);

            if (threshold < 0) {
                log.warn("The 'finishes' date-time method does not allow negative threshold");
                return null;
            }

            long delta = Math.abs(leftStart - rightEnd);
            return delta <= threshold;
        }
    }

    /**
     * Overlaps.
     */
    public static class IntervalComputerOverlapsNoParam implements IntervalComputer {

        public Boolean compute(long leftStart, long leftEnd, long rightStart, long rightEnd, EventBean[] eventsPerStream, boolean newData, ExprEvaluatorContext context) {
            return (leftStart < rightStart) &&
                    (rightStart < leftEnd) &&
                    (leftEnd < rightEnd);
        }
    }

    public static class IntervalComputerOverlapsAndByThreshold implements IntervalComputer {

        private final boolean overlaps;
        private final IntervalDeltaExprEvaluator thresholdExpr;

        public IntervalComputerOverlapsAndByThreshold(boolean overlaps, IntervalDeltaExprEvaluator thresholdExpr) {
            this.overlaps = overlaps;
            this.thresholdExpr = thresholdExpr;
        }

        public Boolean compute(long leftStart, long leftEnd, long rightStart, long rightEnd, EventBean[] eventsPerStream, boolean newData, ExprEvaluatorContext context) {

            if (overlaps) {
                long threshold = thresholdExpr.evaluate(leftStart, eventsPerStream, newData, context);
                return computeInternalOverlaps(leftStart, leftEnd, rightStart, rightEnd, 0, threshold);
            } else {
                long threshold = thresholdExpr.evaluate(rightStart, eventsPerStream, newData, context);
                return computeInternalOverlaps(rightStart, rightEnd, leftStart, leftEnd, 0, threshold);
            }
        }

        public static boolean computeInternalOverlaps(long left, long leftEnd, long right, long rightEnd, long min, long max) {
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

    public static class IntervalComputerOverlapsAndByMinMax implements IntervalComputer {

        private final boolean overlaps;
        private final IntervalDeltaExprEvaluator minEval;
        private final IntervalDeltaExprEvaluator maxEval;

        public IntervalComputerOverlapsAndByMinMax(boolean overlaps, IntervalDeltaExprEvaluator minEval, IntervalDeltaExprEvaluator maxEval) {
            this.overlaps = overlaps;
            this.minEval = minEval;
            this.maxEval = maxEval;
        }

        public Boolean compute(long leftStart, long leftEnd, long rightStart, long rightEnd, EventBean[] eventsPerStream, boolean newData, ExprEvaluatorContext context) {

            if (overlaps) {
                long minThreshold = minEval.evaluate(leftStart, eventsPerStream, newData, context);
                long maxThreshold = maxEval.evaluate(leftEnd, eventsPerStream, newData, context);
                return IntervalComputerOverlapsAndByThreshold.computeInternalOverlaps(leftStart, leftEnd, rightStart, rightEnd, minThreshold, maxThreshold);
            } else {
                long minThreshold = minEval.evaluate(rightStart, eventsPerStream, newData, context);
                long maxThreshold = maxEval.evaluate(rightEnd, eventsPerStream, newData, context);
                return IntervalComputerOverlapsAndByThreshold.computeInternalOverlaps(rightStart, rightEnd, leftStart, leftEnd, minThreshold, maxThreshold);
            }
        }
    }

    /**
     * OverlappedBy.
     */
    public static class IntervalComputerOverlappedByNoParam implements IntervalComputer {

        public Boolean compute(long leftStart, long leftEnd, long rightStart, long rightEnd, EventBean[] eventsPerStream, boolean newData, ExprEvaluatorContext context) {
            return (rightStart < leftStart) &&
                    (leftStart < rightEnd) &&
                    (rightEnd < leftEnd);
        }
    }

    /**
     * Starts.
     */
    public static class IntervalComputerStartsNoParam implements IntervalComputer {

        public Boolean compute(long leftStart, long leftEnd, long rightStart, long rightEnd, EventBean[] eventsPerStream, boolean newData, ExprEvaluatorContext context) {
            return (leftStart == rightStart) && (leftEnd < rightEnd);
        }
    }

    public static class IntervalComputerStartsThreshold implements IntervalComputer {

        private static final Logger log = LoggerFactory.getLogger(IntervalComputerStartsThreshold.class);

        private final IntervalDeltaExprEvaluator thresholdExpr;

        public IntervalComputerStartsThreshold(IntervalDeltaExprEvaluator thresholdExpr) {
            this.thresholdExpr = thresholdExpr;
        }

        public Boolean compute(long leftStart, long leftEnd, long rightStart, long rightEnd, EventBean[] eventsPerStream, boolean newData, ExprEvaluatorContext context) {

            long threshold = thresholdExpr.evaluate(Math.min(leftStart, rightStart), eventsPerStream, newData, context);
            if (threshold < 0) {
                log.warn("The 'finishes' date-time method does not allow negative threshold");
                return null;
            }

            long delta = Math.abs(leftStart - rightStart);
            return delta <= threshold && (leftEnd < rightEnd);
        }
    }

    /**
     * Started-by.
     */
    public static class IntervalComputerStartedByNoParam implements IntervalComputer {

        public Boolean compute(long leftStart, long leftEnd, long rightStart, long rightEnd, EventBean[] eventsPerStream, boolean newData, ExprEvaluatorContext context) {
            return (leftStart == rightStart) && (leftEnd > rightEnd);
        }
    }

    public static class IntervalComputerStartedByThreshold implements IntervalComputer {

        private static final Logger log = LoggerFactory.getLogger(IntervalComputerStartedByThreshold.class);

        private final IntervalDeltaExprEvaluator thresholdExpr;

        public IntervalComputerStartedByThreshold(IntervalDeltaExprEvaluator thresholdExpr) {
            this.thresholdExpr = thresholdExpr;
        }

        public Boolean compute(long leftStart, long leftEnd, long rightStart, long rightEnd, EventBean[] eventsPerStream, boolean newData, ExprEvaluatorContext context) {

            long threshold = thresholdExpr.evaluate(Math.min(leftStart, rightStart), eventsPerStream, newData, context);
            if (threshold < 0) {
                log.warn("The 'finishes' date-time method does not allow negative threshold");
                return null;
            }

            long delta = Math.abs(leftStart - rightStart);
            return delta <= threshold && (leftEnd > rightEnd);
        }
    }
}
