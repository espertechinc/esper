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

import com.espertech.esper.client.EventBean;
import com.espertech.esper.client.EventPropertyGetter;
import com.espertech.esper.client.EventType;
import com.espertech.esper.epl.datetime.calop.CalendarOp;
import com.espertech.esper.epl.datetime.interval.IntervalOp;
import com.espertech.esper.epl.datetime.reformatop.ReformatOp;
import com.espertech.esper.epl.expression.core.ExprEvaluatorContext;
import com.espertech.esper.epl.expression.dot.ExprDotEval;
import com.espertech.esper.epl.expression.dot.ExprDotEvalVisitor;
import com.espertech.esper.epl.expression.time.TimeAbacus;
import com.espertech.esper.epl.rettype.EPType;
import com.espertech.esper.epl.rettype.EPTypeHelper;
import com.espertech.esper.util.JavaClassHelper;

import java.time.LocalDateTime;
import java.time.ZonedDateTime;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.TimeZone;

public class ExprDotEvalDT implements ExprDotEval {
    private final EPType returnType;
    private final DTLocalEvaluator evaluator;

    public ExprDotEvalDT(List<CalendarOp> calendarOps, TimeZone timeZone, TimeAbacus timeAbacus, ReformatOp reformatOp, IntervalOp intervalOp, Class inputType, EventType inputEventType) {
        this.evaluator = getEvaluator(calendarOps, timeZone, timeAbacus, inputType, inputEventType, reformatOp, intervalOp);

        if (intervalOp != null) {
            returnType = EPTypeHelper.singleValue(Boolean.class);
        } else if (reformatOp != null) {
            returnType = EPTypeHelper.singleValue(reformatOp.getReturnType());
        } else {  // only calendar ops
            if (inputEventType != null) {
                returnType = EPTypeHelper.singleValue(inputEventType.getPropertyType(inputEventType.getStartTimestampPropertyName()));
            } else {
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

    public DTLocalEvaluator getEvaluator(List<CalendarOp> calendarOps, TimeZone timeZone, TimeAbacus timeAbacus, Class inputType, EventType inputEventType, ReformatOp reformatOp, IntervalOp intervalOp) {
        if (inputEventType == null) {
            if (reformatOp != null) {
                if (JavaClassHelper.isSubclassOrImplementsInterface(inputType, Calendar.class)) {
                    if (calendarOps.isEmpty()) {
                        return new DTLocalEvaluatorCalReformat(reformatOp);
                    }
                    return new DTLocalEvaluatorCalOpsReformat(calendarOps, reformatOp);
                } else if (JavaClassHelper.isSubclassOrImplementsInterface(inputType, Date.class)) {
                    if (calendarOps.isEmpty()) {
                        return new DTLocalEvaluatorDateReformat(reformatOp);
                    }
                    return new DTLocalEvaluatorDateOpsReformat(calendarOps, reformatOp, timeZone);
                } else if (JavaClassHelper.getBoxedType(inputType) == Long.class) {
                    if (calendarOps.isEmpty()) {
                        return new DTLocalEvaluatorLongReformat(reformatOp);
                    }
                    return new DTLocalEvaluatorLongOpsReformat(calendarOps, reformatOp, timeZone, timeAbacus);
                } else if (JavaClassHelper.isSubclassOrImplementsInterface(inputType, LocalDateTime.class)) {
                    if (calendarOps.isEmpty()) {
                        return new DTLocalEvaluatorLocalDateTimeReformat(reformatOp);
                    }
                    return new DTLocalEvaluatorLocalDateTimeOpsReformat(calendarOps, reformatOp);
                } else if (JavaClassHelper.isSubclassOrImplementsInterface(inputType, ZonedDateTime.class)) {
                    if (calendarOps.isEmpty()) {
                        return new DTLocalEvaluatorZonedDateTimeReformat(reformatOp);
                    }
                    return new DTLocalEvaluatorZonedDateTimeOpsReformat(calendarOps, reformatOp);
                }
            } else if (intervalOp != null) {
                if (JavaClassHelper.isSubclassOrImplementsInterface(inputType, Calendar.class)) {
                    if (calendarOps.isEmpty()) {
                        return new DTLocalEvaluatorCalInterval(intervalOp);
                    }
                    return new DTLocalEvaluatorCalOpsInterval(calendarOps, intervalOp, timeZone);
                } else if (JavaClassHelper.isSubclassOrImplementsInterface(inputType, Date.class)) {
                    if (calendarOps.isEmpty()) {
                        return new DTLocalEvaluatorDateInterval(intervalOp);
                    }
                    return new DTLocalEvaluatorDateOpsInterval(calendarOps, intervalOp, timeZone);
                } else if (JavaClassHelper.getBoxedType(inputType) == Long.class) {
                    if (calendarOps.isEmpty()) {
                        return new DTLocalEvaluatorLongInterval(intervalOp);
                    }
                    return new DTLocalEvaluatorLongOpsInterval(calendarOps, intervalOp, timeZone, timeAbacus);
                } else if (JavaClassHelper.isSubclassOrImplementsInterface(inputType, LocalDateTime.class)) {
                    if (calendarOps.isEmpty()) {
                        return new DTLocalEvaluatorLDTInterval(intervalOp, timeZone);
                    }
                    return new DTLocalEvaluatorLocalDateTimeOpsInterval(calendarOps, intervalOp, timeZone);
                } else if (JavaClassHelper.isSubclassOrImplementsInterface(inputType, ZonedDateTime.class)) {
                    if (calendarOps.isEmpty()) {
                        return new DTLocalEvaluatorZDTInterval(intervalOp);
                    }
                    return new DTLocalEvaluatorZonedDateTimeOpsInterval(calendarOps, intervalOp);
                }
            } else { // only calendar ops, nothing else
                if (JavaClassHelper.isSubclassOrImplementsInterface(inputType, Calendar.class)) {
                    return new DTLocalEvaluatorCalOpsCal(calendarOps);
                } else if (JavaClassHelper.isSubclassOrImplementsInterface(inputType, Date.class)) {
                    return new DTLocalEvaluatorCalOpsDate(calendarOps, timeZone);
                } else if (JavaClassHelper.getBoxedType(inputType) == Long.class) {
                    return new DTLocalEvaluatorCalOpsLong(calendarOps, timeZone, timeAbacus);
                } else if (JavaClassHelper.isSubclassOrImplementsInterface(inputType, LocalDateTime.class)) {
                    return new DTLocalEvaluatorCalOpsLocalDateTime(calendarOps);
                } else if (JavaClassHelper.isSubclassOrImplementsInterface(inputType, ZonedDateTime.class)) {
                    return new DTLocalEvaluatorCalOpsZonedDateTime(calendarOps);
                }
            }
            throw new IllegalArgumentException("Invalid input type '" + inputType + "'");
        }

        EventPropertyGetter getter = inputEventType.getGetter(inputEventType.getStartTimestampPropertyName());
        Class getterResultType = inputEventType.getPropertyType(inputEventType.getStartTimestampPropertyName());

        if (reformatOp != null) {
            DTLocalEvaluator inner = getEvaluator(calendarOps, timeZone, timeAbacus, getterResultType, null, reformatOp, null);
            return new DTLocalEvaluatorBeanReformat(getter, inner);
        }
        if (intervalOp == null) {   // only calendar ops
            DTLocalEvaluator inner = getEvaluator(calendarOps, timeZone, timeAbacus, getterResultType, null, null, null);
            return new DTLocalEvaluatorBeanCalOps(getter, inner);
        }

        // have interval ops but no end timestamp
        if (inputEventType.getEndTimestampPropertyName() == null) {
            DTLocalEvaluator inner = getEvaluator(calendarOps, timeZone, timeAbacus, getterResultType, null, null, intervalOp);
            return new DTLocalEvaluatorBeanIntervalNoEndTS(getter, inner);
        }

        // interval ops and have end timestamp
        EventPropertyGetter getterEndTimestamp = inputEventType.getGetter(inputEventType.getEndTimestampPropertyName());
        DTLocalEvaluatorIntervalComp inner = (DTLocalEvaluatorIntervalComp) getEvaluator(calendarOps, timeZone, timeAbacus, getterResultType, null, null, intervalOp);
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

    protected static LocalDateTime evaluateCalOps(List<CalendarOp> calendarOps, LocalDateTime ldt, EventBean[] eventsPerStream, boolean isNewData, ExprEvaluatorContext exprEvaluatorContext) {
        for (CalendarOp calendarOp : calendarOps) {
            ldt = calendarOp.evaluate(ldt, eventsPerStream, isNewData, exprEvaluatorContext);
        }
        return ldt;
    }

    protected static ZonedDateTime evaluateCalOps(List<CalendarOp> calendarOps, ZonedDateTime zdt, EventBean[] eventsPerStream, boolean isNewData, ExprEvaluatorContext exprEvaluatorContext) {
        for (CalendarOp calendarOp : calendarOps) {
            zdt = calendarOp.evaluate(zdt, eventsPerStream, isNewData, exprEvaluatorContext);
        }
        return zdt;
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

    private static class DTLocalEvaluatorLocalDateTimeReformat extends DTLocalEvaluatorReformatBase {
        private DTLocalEvaluatorLocalDateTimeReformat(ReformatOp reformatOp) {
            super(reformatOp);
        }

        public Object evaluate(Object target, EventBean[] eventsPerStream, boolean isNewData, ExprEvaluatorContext exprEvaluatorContext) {
            return reformatOp.evaluate((LocalDateTime) target, eventsPerStream, isNewData, exprEvaluatorContext);
        }
    }

    private static class DTLocalEvaluatorZonedDateTimeReformat extends DTLocalEvaluatorReformatBase {
        private DTLocalEvaluatorZonedDateTimeReformat(ReformatOp reformatOp) {
            super(reformatOp);
        }

        public Object evaluate(Object target, EventBean[] eventsPerStream, boolean isNewData, ExprEvaluatorContext exprEvaluatorContext) {
            return reformatOp.evaluate((ZonedDateTime) target, eventsPerStream, isNewData, exprEvaluatorContext);
        }
    }

    private static class DTLocalEvaluatorDateOpsReformat extends DTLocalEvaluatorCalopReformatBase {

        private final TimeZone timeZone;

        private DTLocalEvaluatorDateOpsReformat(List<CalendarOp> calendarOps, ReformatOp reformatOp, TimeZone timeZone) {
            super(calendarOps, reformatOp);
            this.timeZone = timeZone;
        }

        public Object evaluate(Object target, EventBean[] eventsPerStream, boolean isNewData, ExprEvaluatorContext exprEvaluatorContext) {
            Calendar cal = Calendar.getInstance(timeZone);
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

        private final TimeZone timeZone;
        private final TimeAbacus timeAbacus;

        private DTLocalEvaluatorLongOpsReformat(List<CalendarOp> calendarOps, ReformatOp reformatOp, TimeZone timeZone, TimeAbacus timeAbacus) {
            super(calendarOps, reformatOp);
            this.timeZone = timeZone;
            this.timeAbacus = timeAbacus;
        }

        public Object evaluate(Object target, EventBean[] eventsPerStream, boolean isNewData, ExprEvaluatorContext exprEvaluatorContext) {
            Calendar cal = Calendar.getInstance(timeZone);
            timeAbacus.calendarSet((Long) target, cal);
            evaluateCalOps(calendarOps, cal, eventsPerStream, isNewData, exprEvaluatorContext);
            return reformatOp.evaluate(cal, eventsPerStream, isNewData, exprEvaluatorContext);
        }
    }

    private static class DTLocalEvaluatorLocalDateTimeOpsReformat extends DTLocalEvaluatorCalopReformatBase {

        private DTLocalEvaluatorLocalDateTimeOpsReformat(List<CalendarOp> calendarOps, ReformatOp reformatOp) {
            super(calendarOps, reformatOp);
        }

        public Object evaluate(Object target, EventBean[] eventsPerStream, boolean isNewData, ExprEvaluatorContext exprEvaluatorContext) {
            LocalDateTime ldt = (LocalDateTime) target;
            ldt = evaluateCalOps(calendarOps, ldt, eventsPerStream, isNewData, exprEvaluatorContext);
            return reformatOp.evaluate(ldt, eventsPerStream, isNewData, exprEvaluatorContext);
        }
    }

    private static class DTLocalEvaluatorZonedDateTimeOpsReformat extends DTLocalEvaluatorCalopReformatBase {

        private DTLocalEvaluatorZonedDateTimeOpsReformat(List<CalendarOp> calendarOps, ReformatOp reformatOp) {
            super(calendarOps, reformatOp);
        }

        public Object evaluate(Object target, EventBean[] eventsPerStream, boolean isNewData, ExprEvaluatorContext exprEvaluatorContext) {
            ZonedDateTime zdt = (ZonedDateTime) target;
            zdt = evaluateCalOps(calendarOps, zdt, eventsPerStream, isNewData, exprEvaluatorContext);
            return reformatOp.evaluate(zdt, eventsPerStream, isNewData, exprEvaluatorContext);
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

    private static class DTLocalEvaluatorLDTInterval extends DTLocalEvaluatorIntervalBase {

        private final TimeZone timeZone;

        public DTLocalEvaluatorLDTInterval(IntervalOp intervalOp, TimeZone timeZone) {
            super(intervalOp);
            this.timeZone = timeZone;
        }

        public Object evaluate(Object target, EventBean[] eventsPerStream, boolean isNewData, ExprEvaluatorContext exprEvaluatorContext) {
            long time = DatetimeLongCoercerLocalDateTime.coerce((LocalDateTime) target, timeZone);
            return intervalOp.evaluate(time, time, eventsPerStream, isNewData, exprEvaluatorContext);
        }

        public Object evaluate(Object startTimestamp, Object endTimestamp, EventBean[] eventsPerStream, boolean isNewData, ExprEvaluatorContext exprEvaluatorContext) {
            long start = DatetimeLongCoercerLocalDateTime.coerce((LocalDateTime) startTimestamp, timeZone);
            long end = DatetimeLongCoercerLocalDateTime.coerce((LocalDateTime) endTimestamp, timeZone);
            return intervalOp.evaluate(start, end, eventsPerStream, isNewData, exprEvaluatorContext);
        }
    }

    private static class DTLocalEvaluatorZDTInterval extends DTLocalEvaluatorIntervalBase {

        public DTLocalEvaluatorZDTInterval(IntervalOp intervalOp) {
            super(intervalOp);
        }

        public Object evaluate(Object target, EventBean[] eventsPerStream, boolean isNewData, ExprEvaluatorContext exprEvaluatorContext) {
            long time = DatetimeLongCoercerZonedDateTime.coerce((ZonedDateTime) target);
            return intervalOp.evaluate(time, time, eventsPerStream, isNewData, exprEvaluatorContext);
        }

        public Object evaluate(Object startTimestamp, Object endTimestamp, EventBean[] eventsPerStream, boolean isNewData, ExprEvaluatorContext exprEvaluatorContext) {
            long start = DatetimeLongCoercerZonedDateTime.coerce((ZonedDateTime) startTimestamp);
            long end = DatetimeLongCoercerZonedDateTime.coerce((ZonedDateTime) endTimestamp);
            return intervalOp.evaluate(start, end, eventsPerStream, isNewData, exprEvaluatorContext);
        }
    }

    private static class DTLocalEvaluatorCalOpsInterval extends DTLocalEvaluatorCalOpsIntervalBase {
        private final TimeZone timeZone;

        private DTLocalEvaluatorCalOpsInterval(List<CalendarOp> calendarOps, IntervalOp intervalOp, TimeZone timeZone) {
            super(calendarOps, intervalOp);
            this.timeZone = timeZone;
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
            Calendar cal = Calendar.getInstance(timeZone);
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

        private final TimeZone timeZone;

        private DTLocalEvaluatorDateOpsInterval(List<CalendarOp> calendarOps, IntervalOp intervalOp, TimeZone timeZone) {
            super(calendarOps, intervalOp);
            this.timeZone = timeZone;
        }

        public Object evaluate(Object target, EventBean[] eventsPerStream, boolean isNewData, ExprEvaluatorContext exprEvaluatorContext) {
            Calendar cal = Calendar.getInstance(timeZone);
            cal.setTimeInMillis(((Date) target).getTime());
            evaluateCalOps(calendarOps, cal, eventsPerStream, isNewData, exprEvaluatorContext);
            long time = cal.getTimeInMillis();
            return intervalOp.evaluate(time, time, eventsPerStream, isNewData, exprEvaluatorContext);
        }

        public Object evaluate(Object startTimestamp, Object endTimestamp, EventBean[] eventsPerStream, boolean isNewData, ExprEvaluatorContext exprEvaluatorContext) {
            long startLong = ((Date) startTimestamp).getTime();
            long endLong = ((Date) endTimestamp).getTime();
            Calendar cal = Calendar.getInstance(timeZone);
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

        private final TimeZone timeZone;
        private final TimeAbacus timeAbacus;

        private DTLocalEvaluatorLongOpsInterval(List<CalendarOp> calendarOps, IntervalOp intervalOp, TimeZone timeZone, TimeAbacus timeAbacus) {
            super(calendarOps, intervalOp);
            this.timeZone = timeZone;
            this.timeAbacus = timeAbacus;
        }

        public Object evaluate(Object target, EventBean[] eventsPerStream, boolean isNewData, ExprEvaluatorContext exprEvaluatorContext) {
            Calendar cal = Calendar.getInstance(timeZone);
            long startRemainder = timeAbacus.calendarSet((Long) target, cal);
            evaluateCalOps(calendarOps, cal, eventsPerStream, isNewData, exprEvaluatorContext);
            long time = timeAbacus.calendarGet(cal, startRemainder);
            return intervalOp.evaluate(time, time, eventsPerStream, isNewData, exprEvaluatorContext);
        }

        public Object evaluate(Object startTimestamp, Object endTimestamp, EventBean[] eventsPerStream, boolean isNewData, ExprEvaluatorContext exprEvaluatorContext) {
            long startLong = (Long) startTimestamp;
            long endLong = (Long) endTimestamp;
            Calendar cal = Calendar.getInstance(timeZone);
            long startRemainder = timeAbacus.calendarSet(startLong, cal);
            evaluateCalOps(calendarOps, cal, eventsPerStream, isNewData, exprEvaluatorContext);
            long startTime = timeAbacus.calendarGet(cal, startRemainder);
            long endTime = startTime + (endLong - startLong);
            return intervalOp.evaluate(startTime, endTime, eventsPerStream, isNewData, exprEvaluatorContext);
        }
    }

    private static class DTLocalEvaluatorLocalDateTimeOpsInterval extends DTLocalEvaluatorCalOpsIntervalBase {

        private final TimeZone timeZone;

        private DTLocalEvaluatorLocalDateTimeOpsInterval(List<CalendarOp> calendarOps, IntervalOp intervalOp, TimeZone timeZone) {
            super(calendarOps, intervalOp);
            this.timeZone = timeZone;
        }

        public Object evaluate(Object target, EventBean[] eventsPerStream, boolean isNewData, ExprEvaluatorContext exprEvaluatorContext) {
            LocalDateTime ldt = (LocalDateTime) target;
            ldt = evaluateCalOps(calendarOps, ldt, eventsPerStream, isNewData, exprEvaluatorContext);
            long time = DatetimeLongCoercerLocalDateTime.coerce(ldt, timeZone);
            return intervalOp.evaluate(time, time, eventsPerStream, isNewData, exprEvaluatorContext);
        }

        public Object evaluate(Object startTimestamp, Object endTimestamp, EventBean[] eventsPerStream, boolean isNewData, ExprEvaluatorContext exprEvaluatorContext) {
            LocalDateTime start = (LocalDateTime) startTimestamp;
            LocalDateTime end = (LocalDateTime) endTimestamp;
            long deltaMSec = DatetimeLongCoercerLocalDateTime.coerce(end, timeZone) - DatetimeLongCoercerLocalDateTime.coerce(start, timeZone);
            start = evaluateCalOps(calendarOps, start, eventsPerStream, isNewData, exprEvaluatorContext);
            long startLong = DatetimeLongCoercerLocalDateTime.coerce(start, timeZone);
            long endTime = startLong + deltaMSec;
            return intervalOp.evaluate(startLong, endTime, eventsPerStream, isNewData, exprEvaluatorContext);
        }
    }

    private static class DTLocalEvaluatorZonedDateTimeOpsInterval extends DTLocalEvaluatorCalOpsIntervalBase {

        private DTLocalEvaluatorZonedDateTimeOpsInterval(List<CalendarOp> calendarOps, IntervalOp intervalOp) {
            super(calendarOps, intervalOp);
        }

        public Object evaluate(Object target, EventBean[] eventsPerStream, boolean isNewData, ExprEvaluatorContext exprEvaluatorContext) {
            ZonedDateTime zdt = (ZonedDateTime) target;
            zdt = evaluateCalOps(calendarOps, zdt, eventsPerStream, isNewData, exprEvaluatorContext);
            long time = DatetimeLongCoercerZonedDateTime.coerce(zdt);
            return intervalOp.evaluate(time, time, eventsPerStream, isNewData, exprEvaluatorContext);
        }

        public Object evaluate(Object startTimestamp, Object endTimestamp, EventBean[] eventsPerStream, boolean isNewData, ExprEvaluatorContext exprEvaluatorContext) {
            ZonedDateTime start = (ZonedDateTime) startTimestamp;
            ZonedDateTime end = (ZonedDateTime) endTimestamp;
            long deltaMSec = DatetimeLongCoercerZonedDateTime.coerce(end) - DatetimeLongCoercerZonedDateTime.coerce(start);
            start = evaluateCalOps(calendarOps, start, eventsPerStream, isNewData, exprEvaluatorContext);
            long startLong = DatetimeLongCoercerZonedDateTime.coerce(start);
            long endTime = startLong + deltaMSec;
            return intervalOp.evaluate(startLong, endTime, eventsPerStream, isNewData, exprEvaluatorContext);
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
            Object timestamp = getter.get((EventBean) target);
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
            Object timestamp = getter.get((EventBean) target);
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
            Object startTimestamp = getterStartTimestamp.get((EventBean) target);
            if (startTimestamp == null) {
                return null;
            }
            Object endTimestamp = getterEndTimestamp.get((EventBean) target);
            if (endTimestamp == null) {
                return null;
            }
            return inner.evaluate(startTimestamp, endTimestamp, eventsPerStream, isNewData, exprEvaluatorContext);
        }
    }

    private abstract class DTLocalEvaluatorCalOpsCalBase {

        protected final List<CalendarOp> calendarOps;

        private DTLocalEvaluatorCalOpsCalBase(List<CalendarOp> calendarOps) {
            this.calendarOps = calendarOps;
        }
    }

    private class DTLocalEvaluatorCalOpsLong extends DTLocalEvaluatorCalOpsCalBase implements DTLocalEvaluator {

        private final TimeZone timeZone;
        private final TimeAbacus timeAbacus;

        private DTLocalEvaluatorCalOpsLong(List<CalendarOp> calendarOps, TimeZone timeZone, TimeAbacus timeAbacus) {
            super(calendarOps);
            this.timeZone = timeZone;
            this.timeAbacus = timeAbacus;
        }

        public Object evaluate(Object target, EventBean[] eventsPerStream, boolean isNewData, ExprEvaluatorContext exprEvaluatorContext) {
            Long longValue = (Long) target;
            Calendar cal = Calendar.getInstance(timeZone);
            long remainder = timeAbacus.calendarSet(longValue, cal);

            evaluateCalOps(calendarOps, cal, eventsPerStream, isNewData, exprEvaluatorContext);

            return timeAbacus.calendarGet(cal, remainder);
        }
    }

    private class DTLocalEvaluatorCalOpsDate extends DTLocalEvaluatorCalOpsCalBase implements DTLocalEvaluator {

        private final TimeZone timeZone;

        private DTLocalEvaluatorCalOpsDate(List<CalendarOp> calendarOps, TimeZone timeZone) {
            super(calendarOps);
            this.timeZone = timeZone;
        }

        public Object evaluate(Object target, EventBean[] eventsPerStream, boolean isNewData, ExprEvaluatorContext exprEvaluatorContext) {
            Date dateValue = (Date) target;
            Calendar cal = Calendar.getInstance(timeZone);
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

    private class DTLocalEvaluatorCalOpsLocalDateTime extends DTLocalEvaluatorCalOpsCalBase implements DTLocalEvaluator {
        public DTLocalEvaluatorCalOpsLocalDateTime(List<CalendarOp> calendarOps) {
            super(calendarOps);
        }

        public Object evaluate(Object target, EventBean[] eventsPerStream, boolean isNewData, ExprEvaluatorContext exprEvaluatorContext) {
            LocalDateTime ldt = (LocalDateTime) target;
            return evaluateCalOps(calendarOps, ldt, eventsPerStream, isNewData, exprEvaluatorContext);
        }
    }

    private class DTLocalEvaluatorCalOpsZonedDateTime extends DTLocalEvaluatorCalOpsCalBase implements DTLocalEvaluator {
        private DTLocalEvaluatorCalOpsZonedDateTime(List<CalendarOp> calendarOps) {
            super(calendarOps);
        }

        public Object evaluate(Object target, EventBean[] eventsPerStream, boolean isNewData, ExprEvaluatorContext exprEvaluatorContext) {
            ZonedDateTime zdt = (ZonedDateTime) target;
            return evaluateCalOps(calendarOps, zdt, eventsPerStream, isNewData, exprEvaluatorContext);
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
            Object timestamp = getter.get((EventBean) target);
            if (timestamp == null) {
                return null;
            }
            return inner.evaluate(timestamp, eventsPerStream, isNewData, exprEvaluatorContext);
        }
    }
}
