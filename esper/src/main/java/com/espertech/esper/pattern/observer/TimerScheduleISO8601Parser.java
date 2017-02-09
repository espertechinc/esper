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
package com.espertech.esper.pattern.observer;

import com.espertech.esper.client.util.TimePeriod;
import com.espertech.esper.schedule.ScheduleParameterException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.xml.datatype.DatatypeConfigurationException;
import java.util.Calendar;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Factory for ISO8601 repeating interval observers that indicate truth when a time point was reached.
 */
public class TimerScheduleISO8601Parser {
    private static final Logger log = LoggerFactory.getLogger(TimerScheduleISO8601Parser.class);

    public static TimerScheduleSpec parse(String iso) throws ScheduleParameterException {
        if (iso == null) {
            throw new ScheduleParameterException("Received a null value");
        }
        iso = iso.trim();
        if (iso.isEmpty()) {
            throw new ScheduleParameterException("Received an empty string");
        }

        String[] split = iso.split("/");

        Long optionalRepeats = null;
        Calendar optionalDate = null;
        TimePeriod optionalTimePeriod = null;

        try {
            if (iso.equals("/")) {
                throw new ScheduleParameterException("Invalid number of parts");
            }
            if (iso.endsWith("/")) {
                throw new ScheduleParameterException("Missing the period part");
            }

            if (split.length == 3) {
                optionalRepeats = parseRepeat(split[0]);
                optionalDate = parseDate(split[1]);
                optionalTimePeriod = parsePeriod(split[2]);
            } else if (split.length == 2) {
                // there are two forms:
                // partial-form-1: "R<?>/P<period>"
                // partial-form-2: "<date>/P<period>"
                if (split[0].isEmpty()) {
                    throw new ScheduleParameterException("Expected either a recurrence or a date but received an empty string");
                }
                if (split[0].charAt(0) == 'R') {
                    optionalRepeats = parseRepeat(split[0]);
                } else {
                    optionalDate = parseDate(split[0]);
                }
                optionalTimePeriod = parsePeriod(split[1]);
            } else if (split.length == 1) {
                // there are two forms:
                // just date: "<date>"
                // just period: "P<period>"
                if (split[0].charAt(0) == 'P') {
                    optionalTimePeriod = parsePeriod(split[0]);
                } else {
                    optionalDate = parseDate(split[0]);
                }
            }
        } catch (Exception ex) {
            throw new ScheduleParameterException("Failed to parse '" + iso + "': " + ex.getMessage(), ex);
        }

        // parse repeating interval
        return new TimerScheduleSpec(optionalDate, null, optionalRepeats, optionalTimePeriod);
    }

    public static Calendar parseDate(String dateText) throws ScheduleParameterException {
        try {
            return javax.xml.datatype.DatatypeFactory.newInstance().newXMLGregorianCalendar(dateText).toGregorianCalendar();
        } catch (RuntimeException e) {
            String message = "Exception parsing date '" + dateText + "', the date is not a supported ISO 8601 date";
            log.debug(message, e);
            throw new ScheduleParameterException(message);
        } catch (DatatypeConfigurationException e) {
            throw new ScheduleParameterException("Exception parsing date '" + dateText + "': " + e.getMessage(), e);
        }
    }

    private static long parseRepeat(String repeat) throws ScheduleParameterException {
        if (repeat.charAt(0) != 'R') {
            throw new ScheduleParameterException("Invalid repeat '" + repeat + "', expecting 'R' but received '" + repeat.charAt(0) + "'");
        }
        long numRepeats = -1;
        if (repeat.length() > 1) {
            try {
                numRepeats = Long.parseLong(repeat.substring(1));
            } catch (RuntimeException ex) {
                String message = "Invalid repeat '" + repeat + "', expecting an long-typed value but received '" + repeat.substring(1) + "'";
                log.debug(message, ex);
                throw new ScheduleParameterException(message);
            }
        }
        return numRepeats;
    }

    private static TimePeriod parsePeriod(String period) throws ScheduleParameterException {
        Pattern p = Pattern.compile("P((\\d+Y)?(\\d+M)?(\\d+W)?(\\d+D)?)?(T(\\d+H)?(\\d+M)?(\\d+S)?)?");
        Matcher matcher = p.matcher(period);
        if (!matcher.matches()) {
            throw new ScheduleParameterException("Invalid period '" + period + "'");
        }

        TimePeriod timePeriod = new TimePeriod();
        int indexOfT = period.indexOf("T");
        if (indexOfT < 1) {
            parsePeriodDatePart(period.substring(1), timePeriod);
        } else {
            parsePeriodDatePart(period.substring(1, indexOfT), timePeriod);
            parsePeriodTimePart(period.substring(indexOfT + 1), timePeriod);
        }

        Integer largestAbsolute = timePeriod.largestAbsoluteValue();
        if (largestAbsolute == null || largestAbsolute == 0) {
            throw new ScheduleParameterException("Invalid period '" + period + "'");
        }
        return timePeriod;
    }

    private static void parsePeriodDatePart(String datePart, TimePeriod timePeriod) throws ScheduleParameterException {
        Pattern pattern = Pattern.compile("(\\d+Y)?(\\d+M)?(\\d+W)?(\\d+D)?");
        Matcher matcher = pattern.matcher(datePart);
        if (!matcher.matches()) {
            throw new IllegalStateException();
        }
        for (int i = 0; i < matcher.groupCount(); i++) {
            String group = matcher.group(i + 1);
            if (group == null) {
            } else if (group.endsWith("Y")) {
                timePeriod.setYears(safeParsePrefixedInt(group));
            } else if (group.endsWith("M")) {
                timePeriod.setMonths(safeParsePrefixedInt(group));
            } else if (group.endsWith("D")) {
                timePeriod.setDays(safeParsePrefixedInt(group));
            } else if (group.endsWith("W")) {
                timePeriod.setWeeks(safeParsePrefixedInt(group));
            }
        }
    }

    private static Integer safeParsePrefixedInt(String group) {
        return Integer.parseInt(group.substring(0, group.length() - 1));
    }

    private static void parsePeriodTimePart(String timePart, TimePeriod timePeriod) throws ScheduleParameterException {
        Pattern pattern = Pattern.compile("(\\d+H)?(\\d+M)?(\\d+S)?");
        Matcher matcher = pattern.matcher(timePart);
        if (!matcher.matches()) {
            throw new IllegalStateException();
        }
        for (int i = 0; i < matcher.groupCount(); i++) {
            String group = matcher.group(i + 1);
            if (group == null) {
            } else if (group.endsWith("H")) {
                timePeriod.setHours(safeParsePrefixedInt(group));
            } else if (group.endsWith("M")) {
                timePeriod.setMinutes(safeParsePrefixedInt(group));
            } else if (group.endsWith("S")) {
                timePeriod.setSeconds(safeParsePrefixedInt(group));
            }
        }
    }
}
