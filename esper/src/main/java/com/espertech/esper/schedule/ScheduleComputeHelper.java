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
package com.espertech.esper.schedule;

import com.espertech.esper.epl.expression.time.TimeAbacus;
import com.espertech.esper.type.CronOperatorEnum;
import com.espertech.esper.type.CronParameter;
import com.espertech.esper.type.ScheduleUnit;
import com.espertech.esper.util.ExecutionPathDebugLog;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Calendar;
import java.util.Date;
import java.util.SortedSet;
import java.util.TimeZone;

/**
 * For a crontab-like schedule, this class computes the next occurance given a start time and a specification of
 * what the schedule looks like.
 * The resolution at which this works is at the second level. The next occurance
 * is always at least 1 second ahead.
 * The class implements an algorithm that starts at the highest precision (seconds) and
 * continues to the lowest precicion (month). For each precision level the
 * algorithm looks at the list of valid values and finds a value for each that is equal to or greater then
 * the valid values supplied. If no equal or
 * greater value was supplied, it will reset all higher precision elements to its minimum value.
 */
public final class ScheduleComputeHelper {
    private static final Logger log = LoggerFactory.getLogger(ScheduleComputeHelper.class);

    private final static int[] DAY_OF_WEEK_ARRAY = new int[]{Calendar.SUNDAY,
        Calendar.MONDAY, Calendar.TUESDAY, Calendar.WEDNESDAY, Calendar.THURSDAY,
        Calendar.FRIDAY, Calendar.SATURDAY};

    /**
     * Computes the next lowest date in milliseconds based on a specification and the
     * from-time passed in.
     *
     * @param spec              defines the schedule
     * @param afterTimeInMillis defines the start time
     * @param timeZone          time zone
     * @param timeAbacus time abacus
     * @return a long date millisecond value for the next schedule occurance matching the spec
     */
    public static long computeNextOccurance(ScheduleSpec spec, long afterTimeInMillis, TimeZone timeZone, TimeAbacus timeAbacus) {
        if ((ExecutionPathDebugLog.isDebugEnabled) && (log.isDebugEnabled())) {
            log.debug(".computeNextOccurance Computing next occurance, afterTimeInMillis=" + (new Date(afterTimeInMillis)) +
                    "  as long=" + afterTimeInMillis +
                    "  spec=" + spec);
        }


        // Add the minimum resolution to the start time to ensure we don't get the same exact time
        if (spec.getUnitValues().containsKey(ScheduleUnit.SECONDS)) {
            afterTimeInMillis += timeAbacus.getOneSecond();
        } else {
            afterTimeInMillis += 60 * timeAbacus.getOneSecond();
        }

        return compute(spec, afterTimeInMillis, timeZone, timeAbacus);
    }

    /**
     * Computes the next lowest date in milliseconds based on a specification and the
     * from-time passed in and returns the delta from the current time.
     *
     * @param spec              defines the schedule
     * @param afterTimeInMillis defines the start time
     * @param timeZone          time zone
     * @param timeAbacus time abacus
     * @return a long millisecond value representing the delta between current time and the next schedule occurance matching the spec
     */
    public static long computeDeltaNextOccurance(ScheduleSpec spec, long afterTimeInMillis, TimeZone timeZone, TimeAbacus timeAbacus) {
        return computeNextOccurance(spec, afterTimeInMillis, timeZone, timeAbacus) - afterTimeInMillis;
    }

    private static long compute(ScheduleSpec spec, long afterTimeInMillis, TimeZone timeZone, TimeAbacus timeAbacus) {
        long remainderMicros = -1;
        while (true) {
            Calendar after;
            if (spec.getOptionalTimeZone() != null) {
                after = Calendar.getInstance(TimeZone.getTimeZone(spec.getOptionalTimeZone()));
            } else {
                after = Calendar.getInstance(timeZone);
            }
            long remainder = timeAbacus.calendarSet(afterTimeInMillis, after);
            if (remainderMicros == -1) {
                remainderMicros = remainder;
            }

            ScheduleCalendar result = new ScheduleCalendar();
            result.setMilliseconds(after.get(Calendar.MILLISECOND));

            SortedSet<Integer> minutesSet = spec.getUnitValues().get(ScheduleUnit.MINUTES);
            SortedSet<Integer> hoursSet = spec.getUnitValues().get(ScheduleUnit.HOURS);
            SortedSet<Integer> monthsSet = spec.getUnitValues().get(ScheduleUnit.MONTHS);
            SortedSet<Integer> secondsSet = null;
            boolean isSecondsSpecified = false;

            if (spec.getUnitValues().containsKey(ScheduleUnit.SECONDS)) {
                isSecondsSpecified = true;
                secondsSet = spec.getUnitValues().get(ScheduleUnit.SECONDS);
            }

            if (isSecondsSpecified) {
                result.setSecond(nextValue(secondsSet, after.get(Calendar.SECOND)));
                if (result.getSecond() == -1) {
                    result.setSecond(nextValue(secondsSet, 0));
                    after.add(Calendar.MINUTE, 1);
                }
            }

            result.setMinute(nextValue(minutesSet, after.get(Calendar.MINUTE)));
            if (result.getMinute() != after.get(Calendar.MINUTE)) {
                result.setSecond(nextValue(secondsSet, 0));
            }
            if (result.getMinute() == -1) {
                result.setMinute(nextValue(minutesSet, 0));
                after.add(Calendar.HOUR_OF_DAY, 1);
            }

            result.setHour(nextValue(hoursSet, after.get(Calendar.HOUR_OF_DAY)));
            if (result.getHour() != after.get(Calendar.HOUR_OF_DAY)) {
                result.setSecond(nextValue(secondsSet, 0));
                result.setMinute(nextValue(minutesSet, 0));
            }
            if (result.getHour() == -1) {
                result.setHour(nextValue(hoursSet, 0));
                after.add(Calendar.DAY_OF_MONTH, 1);
            }

            // This call may change second, minute and/or hour parameters
            // They may be reset to minimum values if the day rolled
            result.setDayOfMonth(determineDayOfMonth(spec, after, result));

            boolean dayMatchRealDate = false;
            while (!dayMatchRealDate) {
                if (checkDayValidInMonth(timeZone, result.getDayOfMonth(), after.get(Calendar.MONTH), after.get(Calendar.YEAR))) {
                    dayMatchRealDate = true;
                } else {
                    after.add(Calendar.MONTH, 1);
                }
            }

            int currentMonth = after.get(Calendar.MONTH) + 1;
            result.setMonth(nextValue(monthsSet, currentMonth));
            if (result.getMonth() != currentMonth) {
                result.setSecond(nextValue(secondsSet, 0));
                result.setMinute(nextValue(minutesSet, 0));
                result.setHour(nextValue(hoursSet, 0));
                result.setDayOfMonth(determineDayOfMonth(spec, after, result));
            }
            if (result.getMonth() == -1) {
                result.setMonth(nextValue(monthsSet, 0));
                after.add(Calendar.YEAR, 1);
            }

            // Perform a last valid date check, if failing, try to compute a new date based on this altered after date
            int year = after.get(Calendar.YEAR);
            if (!(checkDayValidInMonth(timeZone, result.getDayOfMonth(), result.getMonth() - 1, year))) {
                afterTimeInMillis = timeAbacus.calendarGet(after, remainder);
                continue;
            }

            return getTime(result, after.get(Calendar.YEAR), spec.getOptionalTimeZone(), timeZone, timeAbacus, remainder);
        }
    }

    /*
     * Determine the next valid day of month based on the given specification of valid days in month and
     * valid days in week. If both days in week and days in month are supplied, the days are OR-ed.
     */
    private static int determineDayOfMonth(ScheduleSpec spec,
                                           Calendar after,
                                           ScheduleCalendar result) {
        SortedSet<Integer> daysOfMonthSet = spec.getUnitValues().get(ScheduleUnit.DAYS_OF_MONTH);
        SortedSet<Integer> daysOfWeekSet = spec.getUnitValues().get(ScheduleUnit.DAYS_OF_WEEK);
        SortedSet<Integer> secondsSet = spec.getUnitValues().get(ScheduleUnit.SECONDS);
        SortedSet<Integer> minutesSet = spec.getUnitValues().get(ScheduleUnit.MINUTES);
        SortedSet<Integer> hoursSet = spec.getUnitValues().get(ScheduleUnit.HOURS);

        int dayOfMonth;

        // If days of week is a wildcard, just go by days of month
        if (spec.getOptionalDayOfMonthOperator() != null || spec.getOptionalDayOfWeekOperator() != null) {
            boolean isWeek = false;
            CronParameter op = spec.getOptionalDayOfMonthOperator();
            if (spec.getOptionalDayOfMonthOperator() == null) {
                op = spec.getOptionalDayOfWeekOperator();
                isWeek = true;
            }

            // may return the current day or a future day in the same month,
            // and may advance the "after" date to the next month
            int currentYYMMDD = getTimeYYYYMMDD(after);
            increaseAfterDayOfMonthSpecialOp(op.getOperator(), op.getDay(), op.getMonth(), isWeek, after);
            int rolledYYMMDD = getTimeYYYYMMDD(after);

            // if rolled then reset time portion
            if (rolledYYMMDD > currentYYMMDD) {
                result.setSecond(nextValue(secondsSet, 0));
                result.setMinute(nextValue(minutesSet, 0));
                result.setHour(nextValue(hoursSet, 0));
                return after.get(Calendar.DAY_OF_MONTH);
            } else if (rolledYYMMDD < currentYYMMDD) {
                // rolling backwards is not allowed
                throw new IllegalStateException("Failed to evaluate special date op, rolled date less then current date");
            } else {
                Calendar work = (Calendar) after.clone();
                work.set(Calendar.SECOND, result.getSecond());
                work.set(Calendar.MINUTE, result.getMinute());
                work.set(Calendar.HOUR_OF_DAY, result.getHour());
                if (!work.after(after)) {    // new date is not after current date, so bump
                    after.add(Calendar.DAY_OF_MONTH, 1);
                    result.setSecond(nextValue(secondsSet, 0));
                    result.setMinute(nextValue(minutesSet, 0));
                    result.setHour(nextValue(hoursSet, 0));
                    increaseAfterDayOfMonthSpecialOp(op.getOperator(), op.getDay(), op.getMonth(), isWeek, after);
                }
                return after.get(Calendar.DAY_OF_MONTH);
            }
        } else if (daysOfWeekSet == null) {
            dayOfMonth = nextValue(daysOfMonthSet, after.get(Calendar.DAY_OF_MONTH));
            if (dayOfMonth != after.get(Calendar.DAY_OF_MONTH)) {
                result.setSecond(nextValue(secondsSet, 0));
                result.setMinute(nextValue(minutesSet, 0));
                result.setHour(nextValue(hoursSet, 0));
            }
            if (dayOfMonth == -1) {
                dayOfMonth = nextValue(daysOfMonthSet, 0);
                after.add(Calendar.MONTH, 1);
            }
        } else if (daysOfMonthSet == null) {
            // If days of weeks is not a wildcard and days of month is a wildcard, go by days of week only
            // Loop to find the next day of month that works for the specified day of week values
            while (true) {
                dayOfMonth = after.get(Calendar.DAY_OF_MONTH);
                int dayOfWeek = after.get(Calendar.DAY_OF_WEEK) - 1;

                // If the day matches neither the day of month nor the day of week
                if (!daysOfWeekSet.contains(dayOfWeek)) {
                    result.setSecond(nextValue(secondsSet, 0));
                    result.setMinute(nextValue(minutesSet, 0));
                    result.setHour(nextValue(hoursSet, 0));
                    after.add(Calendar.DAY_OF_MONTH, 1);
                } else {
                    break;
                }
            }
        } else {
            // Both days of weeks and days of month are not a wildcard
            // Loop to find the next day of month that works for either day of month  OR   day of week
            while (true) {
                dayOfMonth = after.get(Calendar.DAY_OF_MONTH);
                int dayOfWeek = after.get(Calendar.DAY_OF_WEEK) - 1;

                // If the day matches neither the day of month nor the day of week
                if ((!daysOfWeekSet.contains(dayOfWeek)) &&
                        (!daysOfMonthSet.contains(dayOfMonth))) {
                    result.setSecond(nextValue(secondsSet, 0));
                    result.setMinute(nextValue(minutesSet, 0));
                    result.setHour(nextValue(hoursSet, 0));
                    after.add(Calendar.DAY_OF_MONTH, 1);
                } else {
                    break;
                }
            }
        }

        return dayOfMonth;
    }

    private static long getTime(ScheduleCalendar result, int year, String optionalTimeZone, TimeZone timeZone, TimeAbacus timeAbacus, long remainder) {
        Calendar calendar;
        if (optionalTimeZone != null) {
            calendar = Calendar.getInstance(TimeZone.getTimeZone(optionalTimeZone));
        } else {
            calendar = Calendar.getInstance(timeZone);
        }
        calendar.set(year, result.getMonth() - 1, result.getDayOfMonth(), result.getHour(), result.getMinute(), result.getSecond());
        calendar.set(Calendar.MILLISECOND, result.getMilliseconds());

        return timeAbacus.calendarGet(calendar, remainder);
    }

    /*
     * Check if this is a valid date.
     */
    private static boolean checkDayValidInMonth(TimeZone timeZone, int day, int month, int year) {
        try {
            Calendar calendar = Calendar.getInstance(timeZone);
            calendar.setLenient(false);
            calendar.set(Calendar.YEAR, year);
            calendar.set(Calendar.MONTH, month);
            calendar.set(Calendar.DAY_OF_MONTH, day);
            calendar.getTime();
        } catch (IllegalArgumentException e) {
            return false;
        }
        return true;
    }

    /*
     * Determine if in the supplied valueSet there is a value after the given start value.
     * Return -1 to indicate that there is no value after the given startValue.
     * If the valueSet passed is null it is treated as a wildcard and the same startValue is returned
     */
    private static int nextValue(SortedSet<Integer> valueSet, int startValue) {
        if (valueSet == null || valueSet.isEmpty()) {
            return startValue;
        }

        if (valueSet.contains(startValue)) {
            return startValue;
        }

        SortedSet<Integer> tailSet = valueSet.tailSet(startValue + 1);

        if (tailSet.isEmpty()) {
            return -1;
        } else {
            return tailSet.first();
        }
    }

    private static int getTimeYYYYMMDD(Calendar calendar) {
        return 10000 * calendar.get(Calendar.YEAR) + (calendar.get(Calendar.MONTH) + 1) * 100 + calendar.get(Calendar.DAY_OF_MONTH);
    }

    private static void increaseAfterDayOfMonthSpecialOp(CronOperatorEnum operator, Integer day, Integer month, boolean week, Calendar after) {
        DateChecker checker;
        if (operator == CronOperatorEnum.LASTDAY) {
            if (!week) {
                checker = new DateCheckerLastDayOfMonth(day, month);
            } else {
                if (day == null) {
                    checker = new DateCheckerLastDayOfWeek(month);
                } else {
                    checker = new DateCheckerLastSpecificDayWeek(day, month);
                }
            }
        } else if (operator == CronOperatorEnum.LASTWEEKDAY) {
            checker = new DateCheckerLastWeekday(day, month);
        } else {
            checker = new DateCheckerMonthWeekday(day, month);
        }

        int dayCount = 0;
        while (!checker.fits(after)) {
            after.add(Calendar.DAY_OF_MONTH, 1);
            dayCount++;
            if (dayCount > 10000) {
                throw new IllegalArgumentException("Invalid crontab expression: failed to find match day");
            }
        }
    }

    private interface DateChecker {
        public boolean fits(Calendar cal);
    }

    private static class DateCheckerLastSpecificDayWeek implements DateChecker {

        private final int dayCode;
        private final Integer month;

        private DateCheckerLastSpecificDayWeek(int day, Integer month) {
            if (day < 0 || day > 7) {
                throw new IllegalArgumentException("Last xx day of the month has to be a day of week (0-7)");
            }
            dayCode = DAY_OF_WEEK_ARRAY[day];
            this.month = month;
        }

        public boolean fits(Calendar cal) {
            if (dayCode != cal.get(Calendar.DAY_OF_WEEK)) {
                return false;
            }
            if (month != null && month != cal.get(Calendar.MONTH)) {
                return false;
            }
            // e.g. 31=Sun,30=Sat,29=Fri,28=Thu,27=Wed,26=Tue,25=Mon
            // e.g. 31-7 = 24
            return cal.get(Calendar.DAY_OF_MONTH) > cal.getActualMaximum(Calendar.DAY_OF_MONTH) - 7;
        }
    }

    private static class DateCheckerLastDayOfMonth implements DateChecker {
        private final Integer dayCode;
        private final Integer month;

        private DateCheckerLastDayOfMonth(Integer day, Integer month) {
            if (day != null) {
                if (day < 0 || day > 7) {
                    throw new IllegalArgumentException("Last xx day of the month has to be a day of week (0-7)");
                }
                dayCode = DAY_OF_WEEK_ARRAY[day];
            } else {
                dayCode = null;
            }
            this.month = month;
        }

        public boolean fits(Calendar cal) {
            if (dayCode != null && dayCode != cal.get(Calendar.DAY_OF_WEEK)) {
                return false;
            }
            if (month != null && month != cal.get(Calendar.MONTH)) {
                return false;
            }
            return cal.get(Calendar.DAY_OF_MONTH) == cal.getActualMaximum(Calendar.DAY_OF_MONTH);
        }
    }

    private static class DateCheckerLastDayOfWeek implements DateChecker {
        private final Integer month;

        private DateCheckerLastDayOfWeek(Integer month) {
            this.month = month;
        }

        public boolean fits(Calendar cal) {
            if (month != null && month != cal.get(Calendar.MONTH)) {
                return false;
            }
            return cal.get(Calendar.DAY_OF_WEEK) == Calendar.SATURDAY;
        }
    }

    private static class DateCheckerLastWeekday implements DateChecker {
        private final Integer dayCode;
        private final Integer month;

        private DateCheckerLastWeekday(Integer day, Integer month) {
            if (day != null) {
                if (day < 0 || day > 7) {
                    throw new IllegalArgumentException("Last xx day of the month has to be a day of week (0-7)");
                }
                dayCode = DAY_OF_WEEK_ARRAY[day];
            } else {
                dayCode = null;
            }
            this.month = month;
        }

        public boolean fits(Calendar cal) {
            if (dayCode != null && dayCode != cal.get(Calendar.DAY_OF_WEEK)) {
                return false;
            }
            if (month != null && month != cal.get(Calendar.MONTH)) {
                return false;
            }
            if (!isWeekday(cal)) {
                return false;
            }
            int day = cal.get(Calendar.DAY_OF_MONTH);
            int max = cal.getActualMaximum(Calendar.DAY_OF_MONTH);
            if (day == max) {
                return true;
            }
            int dayOfWeek = cal.get(Calendar.DAY_OF_WEEK);
            return day >= max - 2 && dayOfWeek == Calendar.FRIDAY;
        }
    }

    public static class DateCheckerMonthWeekday implements DateChecker {
        private final Integer day;
        private final Integer month;

        private DateCheckerMonthWeekday(Integer day, Integer month) {
            if (day != null) {
                if (day < 1 || day > 31) {
                    throw new IllegalArgumentException("xx day of the month has to be a in range (1-31)");
                }
            }
            this.day = day;
            this.month = month;
        }

        public boolean fits(Calendar cal) {
            if (month != null && month != cal.get(Calendar.MONTH)) {
                return false;
            }
            if (!isWeekday(cal)) {
                return false;
            }
            if (day == null) {
                return true;
            }

            Calendar work = (Calendar) cal.clone();
            int target = computeNearestWeekdayDay(day, work);
            return cal.get(Calendar.DAY_OF_MONTH) == target;
        }

        private static int computeNearestWeekdayDay(int day, Calendar work) {
            int max = work.getActualMaximum(Calendar.DAY_OF_MONTH);
            if (day <= max) {
                work.set(Calendar.DAY_OF_MONTH, day);
            } else {
                work.set(Calendar.DAY_OF_MONTH, max);
            }

            if (isWeekday(work)) {
                return work.get(Calendar.DAY_OF_MONTH);
            }
            if (work.get(Calendar.DAY_OF_WEEK) == Calendar.SATURDAY) {
                if (work.get(Calendar.DAY_OF_MONTH) > 1) {
                    work.add(Calendar.DAY_OF_MONTH, -1);
                    return work.get(Calendar.DAY_OF_MONTH);
                } else {
                    work.add(Calendar.DAY_OF_MONTH, 2);
                    return work.get(Calendar.DAY_OF_MONTH);
                }
            } else {
                // handle Sunday
                if (max == work.get(Calendar.DAY_OF_MONTH)) {
                    work.add(Calendar.DAY_OF_MONTH, -2);
                    return work.get(Calendar.DAY_OF_MONTH);
                } else {
                    work.add(Calendar.DAY_OF_MONTH, 1);
                    return work.get(Calendar.DAY_OF_MONTH);
                }
            }
        }
    }

    private static boolean isWeekday(Calendar cal) {
        int dayOfWeek = cal.get(Calendar.DAY_OF_WEEK);
        return !(dayOfWeek < Calendar.MONDAY || dayOfWeek > Calendar.FRIDAY);
    }
}
