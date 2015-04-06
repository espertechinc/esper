/*
 * *************************************************************************************
 *  Copyright (C) 2006-2015 EsperTech, Inc. All rights reserved.                       *
 *  http://www.espertech.com/esper                                                     *
 *  http://www.espertech.com                                                           *
 *  ---------------------------------------------------------------------------------- *
 *  The software in this package is published under the terms of the GPL license       *
 *  a copy of which has been included with this distribution in the license.txt file.  *
 * *************************************************************************************
 */

package com.espertech.esper.epl.datetime.eval;

import com.espertech.esper.client.EventBean;
import com.espertech.esper.client.EventPropertyGetter;
import com.espertech.esper.client.EventType;
import com.espertech.esper.epl.rettype.EPType;
import com.espertech.esper.epl.rettype.EPTypeHelper;
import com.espertech.esper.epl.datetime.calop.CalendarOp;
import com.espertech.esper.epl.datetime.interval.IntervalOp;
import com.espertech.esper.epl.datetime.reformatop.ReformatOp;
import com.espertech.esper.epl.expression.dot.ExprDotEval;
import com.espertech.esper.epl.expression.dot.ExprDotEvalVisitor;
import com.espertech.esper.epl.expression.core.ExprEvaluatorContext;
import com.espertech.esper.util.JavaClassHelper;

import java.util.Calendar;
import java.util.Date;
import java.util.List;

public class ExprDotEvalDT implements ExprDotEval
{
    private final EPType returnType;
    private final DTLocalEvaluator evaluator;

    public ExprDotEvalDT(List<CalendarOp> calendarOps, ReformatOp reformatOp, IntervalOp intervalOp, Class inputType, EventType inputEventType) {
        this.evaluator = getEvaluator(calendarOps, inputType, inputEventType, reformatOp, intervalOp);

        if (intervalOp != null) {
            returnType = EPTypeHelper.singleValue(Boolean.class);
        }
        else if (reformatOp != null) {
            returnType = EPTypeHelper.singleValue(reformatOp.getReturnType());
        }
        else {  // only calendar ops
            if (inputEventType != null) {
                returnType = EPTypeHelper.singleValue(inputEventType.getPropertyType(inputEventType.getStartTimestampPropertyName()));
            }
            else {
                returnType = EPTypeHelper.singleValue(inputType);
            }
        }
    }

    public EPType getTypeInfo() {
        return returnType;
    }

    public void visit(ExprDotEvalVisitor visitor) {
        visitor.visitDateTime();
    }

    public DTLocalEvaluator getEvaluator(List<CalendarOp> calendarOps, Class inputType, EventType inputEventType, ReformatOp reformatOp, IntervalOp intervalOp) {
        if (inputEventType == null) {
            if (reformatOp != null) {
                if (JavaClassHelper.isSubclassOrImplementsInterface(inputType, Calendar.class)) {
                    if (calendarOps.isEmpty()) {
                        return new DTLocalEvaluatorCalReformat(reformatOp);
                    }
                    return new DTLocalEvaluatorCalOpsReformat(calendarOps, reformatOp);
                }
                else if (JavaClassHelper.isSubclassOrImplementsInterface(inputType, Date.class)) {
                    if (calendarOps.isEmpty()) {
                        return new DTLocalEvaluatorDateReformat(reformatOp);
                    }
                    return new DTLocalEvaluatorDateOpsReformat(calendarOps, reformatOp);
                }
                else if (JavaClassHelper.getBoxedType(inputType) == Long.class) {
                    if (calendarOps.isEmpty()) {
                        return new DTLocalEvaluatorLongReformat(reformatOp);
                    }
                    return new DTLocalEvaluatorLongOpsReformat(calendarOps, reformatOp);
                }
            }
            else if (intervalOp != null) {
                if (JavaClassHelper.isSubclassOrImplementsInterface(inputType, Calendar.class)) {
                    if (calendarOps.isEmpty()) {
                        return new DTLocalEvaluatorCalInterval(intervalOp);
                    }
                    return new DTLocalEvaluatorCalOpsInterval(calendarOps, intervalOp);
                }
                else if (JavaClassHelper.isSubclassOrImplementsInterface(inputType, Date.class)) {
                    if (calendarOps.isEmpty()) {
                        return new DTLocalEvaluatorDateInterval(intervalOp);
                    }
                    return new DTLocalEvaluatorDateOpsInterval(calendarOps, intervalOp);
                }
                else if (JavaClassHelper.getBoxedType(inputType) == Long.class) {
                    if (calendarOps.isEmpty()) {
                        return new DTLocalEvaluatorLongInterval(intervalOp);
                    }
                    return new DTLocalEvaluatorLongOpsInterval(calendarOps, intervalOp);
                }
            }
            else { // only calendar ops, nothing else
                if (JavaClassHelper.isSubclassOrImplementsInterface(inputType, Calendar.class)) {
                    return new DTLocalEvaluatorCalOpsCal(calendarOps);
                }
                else if (JavaClassHelper.isSubclassOrImplementsInterface(inputType, Date.class)) {
                    return new DTLocalEvaluatorCalOpsDate(calendarOps);
                }
                else if (JavaClassHelper.getBoxedType(inputType) == Long.class) {
                    return new DTLocalEvaluatorCalOpsLong(calendarOps);
                }
            }
            throw new IllegalArgumentException("Invalid input type '" + inputType + "'");
        }

        EventPropertyGetter getter = inputEventType.getGetter(inputEventType.getStartTimestampPropertyName());
        Class getterResultType = inputEventType.getPropertyType(inputEventType.getStartTimestampPropertyName());

        if (reformatOp != null) {
            DTLocalEvaluator inner = getEvaluator(calendarOps, getterResultType, null, reformatOp, null);
            return new DTLocalEvaluatorBeanReformat(getter, inner);
        }
        if (intervalOp == null) {   // only calendar ops
            DTLocalEvaluator inner = getEvaluator(calendarOps, getterResultType, null, null, null);
            return new DTLocalEvaluatorBeanCalOps(getter, inner);
        }

        // have interval ops but no end timestamp
        if (inputEventType.getEndTimestampPropertyName() == null) {
            DTLocalEvaluator inner = getEvaluator(calendarOps, getterResultType, null, null, intervalOp);
            return new DTLocalEvaluatorBeanIntervalNoEndTS(getter, inner);
        }

        // interval ops and have end timestamp
        EventPropertyGetter getterEndTimestamp = inputEventType.getGetter(inputEventType.getEndTimestampPropertyName());
        DTLocalEvaluatorIntervalComp inner = (DTLocalEvaluatorIntervalComp) getEvaluator(calendarOps, getterResultType, null, null, intervalOp);
        return new DTLocalEvaluatorBeanIntervalWithEnd(getter, getterEndTimestamp, inner);
    }

    public Object evaluate(Object target, EventBean[] eventsPerStream, boolean isNewData, ExprEvaluatorContext exprEvaluatorContext) {
        if (target == null) {
            return null;
        }
        return evaluator.evaluate(target, eventsPerStream, isNewData, exprEvaluatorContext);
    }

    protected static void evaluateCalOps(List<CalendarOp> calendarOps, Calendar cal, EventBean[] eventsPerStream, boolean isNewData, ExprEvaluatorContext exprEvaluatorContext) {
        for (CalendarOp calendarOp : calendarOps) {
            calendarOp.evaluate(cal, eventsPerStream, isNewData, exprEvaluatorContext);
        }
    }

    private static interface DTLocalEvaluator {
        public Object evaluate(Object target, EventBean[] eventsPerStream, boolean isNewData, ExprEvaluatorContext exprEvaluatorContext);
    }

    private abstract static class DTLocalEvaluatorReformatBase implements DTLocalEvaluator {
        protected final ReformatOp reformatOp;

        protected DTLocalEvaluatorReformatBase(ReformatOp reformatOp) {
            this.reformatOp = reformatOp;
        }
    }

    private abstract static class DTLocalEvaluatorCalopReformatBase implements DTLocalEvaluator {
        protected final List<CalendarOp> calendarOps;
        protected final ReformatOp reformatOp;

        protected DTLocalEvaluatorCalopReformatBase(List<CalendarOp> calendarOps, ReformatOp reformatOp) {
            this.calendarOps = calendarOps;
            this.reformatOp = reformatOp;
        }
    }

    private static class DTLocalEvaluatorCalReformat extends DTLocalEvaluatorReformatBase {
        private DTLocalEvaluatorCalReformat(ReformatOp reformatOp) {
            super(reformatOp);
        }

        public Object evaluate(Object target, EventBean[] eventsPerStream, boolean isNewData, ExprEvaluatorContext exprEvaluatorContext) {
            return reformatOp.evaluate((Calendar) target, eventsPerStream, isNewData, exprEvaluatorContext);
        }
    }

    private static class DTLocalEvaluatorCalOpsReformat extends DTLocalEvaluatorCalopReformatBase {
        private DTLocalEvaluatorCalOpsReformat(List<CalendarOp> calendarOps, ReformatOp reformatOp) {
            super(calendarOps, reformatOp);
        }

        public Object evaluate(Object target, EventBean[] eventsPerStream, boolean isNewData, ExprEvaluatorContext exprEvaluatorContext) {
            Calendar cal = (Calendar) ((Calendar) target).clone();
            evaluateCalOps(calendarOps, cal, eventsPerStream, isNewData, exprEvaluatorContext);
            return reformatOp.evaluate(cal, eventsPerStream, isNewData, exprEvaluatorContext);
        }
    }

    private static class DTLocalEvaluatorDateReformat extends DTLocalEvaluatorReformatBase {
        private DTLocalEvaluatorDateReformat(ReformatOp reformatOp) {
            super(reformatOp);
        }

        public Object evaluate(Object target, EventBean[] eventsPerStream, boolean isNewData, ExprEvaluatorContext exprEvaluatorContext) {
            return reformatOp.evaluate((Date) target, eventsPerStream, isNewData, exprEvaluatorContext);
        }
    }

    private static class DTLocalEvaluatorDateOpsReformat extends DTLocalEvaluatorCalopReformatBase {
        private DTLocalEvaluatorDateOpsReformat(List<CalendarOp> calendarOps, ReformatOp reformatOp) {
            super(calendarOps, reformatOp);
        }

        public Object evaluate(Object target, EventBean[] eventsPerStream, boolean isNewData, ExprEvaluatorContext exprEvaluatorContext) {
            Calendar cal = Calendar.getInstance();
            cal.setTimeInMillis(((Date) target).getTime());
            evaluateCalOps(calendarOps, cal, eventsPerStream, isNewData, exprEvaluatorContext);
            return reformatOp.evaluate(cal, eventsPerStream, isNewData, exprEvaluatorContext);
        }
    }

    private static class DTLocalEvaluatorLongReformat extends DTLocalEvaluatorReformatBase {
        private DTLocalEvaluatorLongReformat(ReformatOp reformatOp) {
            super(reformatOp);
        }

        public Object evaluate(Object target, EventBean[] eventsPerStream, boolean isNewData, ExprEvaluatorContext exprEvaluatorContext) {
            return reformatOp.evaluate((Long) target, eventsPerStream, isNewData, exprEvaluatorContext);
        }
    }

    private static class DTLocalEvaluatorLongOpsReformat extends DTLocalEvaluatorCalopReformatBase {
        private DTLocalEvaluatorLongOpsReformat(List<CalendarOp> calendarOps, ReformatOp reformatOp) {
            super(calendarOps, reformatOp);
        }

        public Object evaluate(Object target, EventBean[] eventsPerStream, boolean isNewData, ExprEvaluatorContext exprEvaluatorContext) {
            Calendar cal = Calendar.getInstance();
            cal.setTimeInMillis((Long) target);
            evaluateCalOps(calendarOps, cal, eventsPerStream, isNewData, exprEvaluatorContext);
            return reformatOp.evaluate(cal, eventsPerStream, isNewData, exprEvaluatorContext);
        }
    }

    /**
     * Interval methods.
     */
    private interface DTLocalEvaluatorIntervalComp {
        public Object evaluate(Object startTimestamp, Object endTimestamp, EventBean[] eventsPerStream, boolean isNewData, ExprEvaluatorContext exprEvaluatorContext);
    }

    private abstract static class DTLocalEvaluatorIntervalBase implements DTLocalEvaluator, DTLocalEvaluatorIntervalComp {
        protected final IntervalOp intervalOp;

        protected DTLocalEvaluatorIntervalBase(IntervalOp intervalOp) {
            this.intervalOp = intervalOp;
        }
    }

    private abstract static class DTLocalEvaluatorCalOpsIntervalBase implements DTLocalEvaluator, DTLocalEvaluatorIntervalComp {
        protected final List<CalendarOp> calendarOps;
        protected final IntervalOp intervalOp;

        protected DTLocalEvaluatorCalOpsIntervalBase(List<CalendarOp> calendarOps, IntervalOp intervalOp) {
            this.calendarOps = calendarOps;
            this.intervalOp = intervalOp;
        }
    }

    private static class DTLocalEvaluatorCalInterval extends DTLocalEvaluatorIntervalBase {
        private DTLocalEvaluatorCalInterval(IntervalOp intervalOp) {
            super(intervalOp);
        }

        public Object evaluate(Object target, EventBean[] eventsPerStream, boolean isNewData, ExprEvaluatorContext exprEvaluatorContext) {
            long time = ((Calendar) target).getTimeInMillis();
            return intervalOp.evaluate(time, time, eventsPerStream, isNewData, exprEvaluatorContext);
        }

        public Object evaluate(Object startTimestamp, Object endTimestamp, EventBean[] eventsPerStream, boolean isNewData, ExprEvaluatorContext exprEvaluatorContext) {
            long start = ((Calendar) startTimestamp).getTimeInMillis();
            long end = ((Calendar) endTimestamp).getTimeInMillis();
            return intervalOp.evaluate(start, end, eventsPerStream, isNewData, exprEvaluatorContext);
        }
    }

    private static class DTLocalEvaluatorCalOpsInterval extends DTLocalEvaluatorCalOpsIntervalBase {
        private DTLocalEvaluatorCalOpsInterval(List<CalendarOp> calendarOps, IntervalOp intervalOp) {
            super(calendarOps, intervalOp);
        }

        public Object evaluate(Object target, EventBean[] eventsPerStream, boolean isNewData, ExprEvaluatorContext exprEvaluatorContext) {
            Calendar cal = (Calendar) ((Calendar) target).clone();
            evaluateCalOps(calendarOps, cal, eventsPerStream, isNewData, exprEvaluatorContext);
            long time = cal.getTimeInMillis();
            return intervalOp.evaluate(time, time, eventsPerStream, isNewData, exprEvaluatorContext);
        }

        public Object evaluate(Object startTimestamp, Object endTimestamp, EventBean[] eventsPerStream, boolean isNewData, ExprEvaluatorContext exprEvaluatorContext) {
            long startLong = ((Calendar) startTimestamp).getTimeInMillis();
            long endLong = ((Calendar) endTimestamp).getTimeInMillis();
            Calendar cal = Calendar.getInstance();
            cal.setTimeInMillis(startLong);
            evaluateCalOps(calendarOps, cal, eventsPerStream, isNewData, exprEvaluatorContext);
            long startTime = cal.getTimeInMillis();
            long endTime = startTime + (endLong - startLong);
            return intervalOp.evaluate(startTime, endTime, eventsPerStream, isNewData, exprEvaluatorContext);
        }
    }

    private static class DTLocalEvaluatorDateInterval extends DTLocalEvaluatorIntervalBase {
        private DTLocalEvaluatorDateInterval(IntervalOp intervalOp) {
            super(intervalOp);
        }

        public Object evaluate(Object target, EventBean[] eventsPerStream, boolean isNewData, ExprEvaluatorContext exprEvaluatorContext) {
            long time = ((Date) target).getTime();
            return intervalOp.evaluate(time, time, eventsPerStream, isNewData, exprEvaluatorContext);
        }

        public Object evaluate(Object startTimestamp, Object endTimestamp, EventBean[] eventsPerStream, boolean isNewData, ExprEvaluatorContext exprEvaluatorContext) {
            long start = ((Date) startTimestamp).getTime();
            long end = ((Date) endTimestamp).getTime();
            return intervalOp.evaluate(start, end, eventsPerStream, isNewData, exprEvaluatorContext);
        }
    }

    private static class DTLocalEvaluatorDateOpsInterval extends DTLocalEvaluatorCalOpsIntervalBase {
        private DTLocalEvaluatorDateOpsInterval(List<CalendarOp> calendarOps, IntervalOp intervalOp) {
            super(calendarOps, intervalOp);
        }

        public Object evaluate(Object target, EventBean[] eventsPerStream, boolean isNewData, ExprEvaluatorContext exprEvaluatorContext) {
            Calendar cal = Calendar.getInstance();
            cal.setTimeInMillis(((Date) target).getTime());
            evaluateCalOps(calendarOps, cal, eventsPerStream, isNewData, exprEvaluatorContext);
            long time = cal.getTimeInMillis();
            return intervalOp.evaluate(time, time, eventsPerStream, isNewData, exprEvaluatorContext);
        }

        public Object evaluate(Object startTimestamp, Object endTimestamp, EventBean[] eventsPerStream, boolean isNewData, ExprEvaluatorContext exprEvaluatorContext) {
            long startLong = ((Date) startTimestamp).getTime();
            long endLong = ((Date) endTimestamp).getTime();
            Calendar cal = Calendar.getInstance();
            cal.setTimeInMillis(startLong);
            evaluateCalOps(calendarOps, cal, eventsPerStream, isNewData, exprEvaluatorContext);
            long startTime = cal.getTimeInMillis();
            long endTime = startTime + (endLong - startLong);
            return intervalOp.evaluate(startTime, endTime, eventsPerStream, isNewData, exprEvaluatorContext);
        }
    }

    private static class DTLocalEvaluatorLongInterval extends DTLocalEvaluatorIntervalBase {

        private DTLocalEvaluatorLongInterval(IntervalOp intervalOp) {
            super(intervalOp);
        }

        public Object evaluate(Object target, EventBean[] eventsPerStream, boolean isNewData, ExprEvaluatorContext exprEvaluatorContext) {
            long time = (Long) target;
            return intervalOp.evaluate(time, time, eventsPerStream, isNewData, exprEvaluatorContext);
        }

        public Object evaluate(Object startTimestamp, Object endTimestamp, EventBean[] eventsPerStream, boolean isNewData, ExprEvaluatorContext exprEvaluatorContext) {
            long startTime = (Long) startTimestamp;
            long endTime = (Long) endTimestamp;
            return intervalOp.evaluate(startTime, endTime, eventsPerStream, isNewData, exprEvaluatorContext);
        }
    }

    private static class DTLocalEvaluatorLongOpsInterval extends DTLocalEvaluatorCalOpsIntervalBase {

        private DTLocalEvaluatorLongOpsInterval(List<CalendarOp> calendarOps, IntervalOp intervalOp) {
            super(calendarOps, intervalOp);
        }

        public Object evaluate(Object target, EventBean[] eventsPerStream, boolean isNewData, ExprEvaluatorContext exprEvaluatorContext) {
            Calendar cal = Calendar.getInstance();
            cal.setTimeInMillis((Long) target);
            evaluateCalOps(calendarOps, cal, eventsPerStream, isNewData, exprEvaluatorContext);
            long time = cal.getTimeInMillis();
            return intervalOp.evaluate(time, time, eventsPerStream, isNewData, exprEvaluatorContext);
        }

        public Object evaluate(Object startTimestamp, Object endTimestamp, EventBean[] eventsPerStream, boolean isNewData, ExprEvaluatorContext exprEvaluatorContext) {
            long startLong = (Long) startTimestamp;
            long endLong = (Long) endTimestamp;
            Calendar cal = Calendar.getInstance();
            cal.setTimeInMillis(startLong);
            evaluateCalOps(calendarOps, cal, eventsPerStream, isNewData, exprEvaluatorContext);
            long startTime = cal.getTimeInMillis();
            long endTime = startTime + (endLong - startLong);
            return intervalOp.evaluate(startTime, endTime, eventsPerStream, isNewData, exprEvaluatorContext);
        }
    }

    private static class DTLocalEvaluatorBeanReformat implements DTLocalEvaluator {
        private final EventPropertyGetter getter;
        private final DTLocalEvaluator inner;

        private DTLocalEvaluatorBeanReformat(EventPropertyGetter getter, DTLocalEvaluator inner) {
            this.getter = getter;
            this.inner = inner;
        }

        public Object evaluate(Object target, EventBean[] eventsPerStream, boolean isNewData, ExprEvaluatorContext exprEvaluatorContext) {
            Object timestamp = getter.get((EventBean)target);
            if (timestamp == null) {
                return null;
            }
            return inner.evaluate(timestamp, eventsPerStream, isNewData, exprEvaluatorContext);
        }
    }

    private static class DTLocalEvaluatorBeanIntervalNoEndTS implements DTLocalEvaluator {
        private final EventPropertyGetter getter;
        private final DTLocalEvaluator inner;

        private DTLocalEvaluatorBeanIntervalNoEndTS(EventPropertyGetter getter, DTLocalEvaluator inner) {
            this.getter = getter;
            this.inner = inner;
        }

        public Object evaluate(Object target, EventBean[] eventsPerStream, boolean isNewData, ExprEvaluatorContext exprEvaluatorContext) {
            Object timestamp = getter.get((EventBean)target);
            if (timestamp == null) {
                return null;
            }
            return inner.evaluate(timestamp, eventsPerStream, isNewData, exprEvaluatorContext);
        }
    }

    private static class DTLocalEvaluatorBeanIntervalWithEnd implements DTLocalEvaluator {
        private final EventPropertyGetter getterStartTimestamp;
        private final EventPropertyGetter getterEndTimestamp;
        private final DTLocalEvaluatorIntervalComp inner;

        private DTLocalEvaluatorBeanIntervalWithEnd(EventPropertyGetter getterStartTimestamp, EventPropertyGetter getterEndTimestamp, DTLocalEvaluatorIntervalComp inner) {
            this.getterStartTimestamp = getterStartTimestamp;
            this.getterEndTimestamp = getterEndTimestamp;
            this.inner = inner;
        }

        public Object evaluate(Object target, EventBean[] eventsPerStream, boolean isNewData, ExprEvaluatorContext exprEvaluatorContext) {
            Object startTimestamp = getterStartTimestamp.get((EventBean)target);
            if (startTimestamp == null) {
                return null;
            }
            Object endTimestamp = getterEndTimestamp.get((EventBean)target);
            if (endTimestamp == null) {
                return null;
            }
            return inner.evaluate(startTimestamp, endTimestamp, eventsPerStream, isNewData, exprEvaluatorContext);
        }
    }

    private abstract class DTLocalEvaluatorCalOpsCalBase  {

        protected final List<CalendarOp> calendarOps;

        private DTLocalEvaluatorCalOpsCalBase(List<CalendarOp> calendarOps) {
            this.calendarOps = calendarOps;
        }
    }

    private class DTLocalEvaluatorCalOpsLong extends DTLocalEvaluatorCalOpsCalBase implements DTLocalEvaluator {

        private DTLocalEvaluatorCalOpsLong(List<CalendarOp> calendarOps) {
            super(calendarOps);
        }

        public Object evaluate(Object target, EventBean[] eventsPerStream, boolean isNewData, ExprEvaluatorContext exprEvaluatorContext) {
            Long longValue = (Long) target;
            Calendar cal = Calendar.getInstance();
            cal.setTimeInMillis(longValue);

            evaluateCalOps(calendarOps, cal, eventsPerStream, isNewData, exprEvaluatorContext);

            return cal.getTimeInMillis();
        }
    }

    private class DTLocalEvaluatorCalOpsDate extends DTLocalEvaluatorCalOpsCalBase implements DTLocalEvaluator {

        private DTLocalEvaluatorCalOpsDate(List<CalendarOp> calendarOps) {
            super(calendarOps);
        }

        public Object evaluate(Object target, EventBean[] eventsPerStream, boolean isNewData, ExprEvaluatorContext exprEvaluatorContext) {
            Date dateValue = (Date) target;
            Calendar cal = Calendar.getInstance();
            cal.setTimeInMillis(dateValue.getTime());

            evaluateCalOps(calendarOps, cal, eventsPerStream, isNewData, exprEvaluatorContext);

            return cal.getTime();
        }
    }

    private class DTLocalEvaluatorCalOpsCal extends DTLocalEvaluatorCalOpsCalBase implements DTLocalEvaluator {
        private DTLocalEvaluatorCalOpsCal(List<CalendarOp> calendarOps) {
            super(calendarOps);
        }

        public Object evaluate(Object target, EventBean[] eventsPerStream, boolean isNewData, ExprEvaluatorContext exprEvaluatorContext) {
            Calendar calValue = (Calendar) target;
            Calendar cal = (Calendar) calValue.clone();

            evaluateCalOps(calendarOps, cal, eventsPerStream, isNewData, exprEvaluatorContext);

            return cal;
        }
    }

    private static class DTLocalEvaluatorBeanCalOps implements DTLocalEvaluator {
        private final EventPropertyGetter getter;
        private final DTLocalEvaluator inner;

        private DTLocalEvaluatorBeanCalOps(EventPropertyGetter getter, DTLocalEvaluator inner) {
            this.getter = getter;
            this.inner = inner;
        }

        public Object evaluate(Object target, EventBean[] eventsPerStream, boolean isNewData, ExprEvaluatorContext exprEvaluatorContext) {
            Object timestamp = getter.get((EventBean)target);
            if (timestamp == null) {
                return null;
            }
            return inner.evaluate(timestamp, eventsPerStream, isNewData, exprEvaluatorContext);
        }
    }
}
