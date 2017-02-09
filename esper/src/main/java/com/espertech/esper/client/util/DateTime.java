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
package com.espertech.esper.client.util;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Calendar;
import java.util.Date;

/**
 * Utility class for date-time functions.
 */
public class DateTime {

    /**
     * The default date-time format.
     */
    public static final String DEFAULT_XMLLIKE_DATE_FORMAT = "yyyy-MM-dd'T'HH:mm:ss.SSS";

    /**
     * The default date-time format with time zone.
     */
    public static final String DEFAULT_XMLLIKE_DATE_FORMAT_WITH_ZONE = "yyyy-MM-dd'T'HH:mm:ss.SSSZ";

    private static final Logger log = LoggerFactory.getLogger(DateTime.class);

    /**
     * Returns a calendar from a given string using the default SimpleDateFormat for parsing.
     *
     * @param datestring to parse
     * @return calendar
     */
    public static Calendar toCalendar(String datestring) {
        return parseGetCal(datestring, new SimpleDateFormat());
    }

    /**
     * Returns a calendar from a given string using the provided format.
     *
     * @param datestring to parse
     * @param format     to use for parsing
     * @return calendar
     */
    public static Calendar toCalendar(String datestring, String format) {
        Date d = parse(datestring, format);
        Calendar cal = Calendar.getInstance();
        cal.setTimeInMillis(d.getTime());
        return cal;
    }

    /**
     * Returns a date from a given string using the default SimpleDateFormat for parsing.
     *
     * @param datestring to parse
     * @return date object
     */
    public static Date toDate(String datestring) {
        return parse(datestring);
    }

    /**
     * Returns a date from a given string using the provided format.
     *
     * @param datestring to parse
     * @param format     to use for parsing
     * @return date object
     */
    public static Date toDate(String datestring, String format) {
        return parse(datestring, format);
    }

    /**
     * Returns a long-millisecond value from a given string using the default SimpleDateFormat for parsing.
     *
     * @param datestring to parse
     * @return long msec
     */
    public static Long toMillisec(String datestring) {
        Date date = parse(datestring);
        if (date == null) {
            return null;
        }
        return date.getTime();
    }

    /**
     * Returns a long-millisecond value from a given string using the provided format.
     *
     * @param datestring to parse
     * @param format     to use for parsing
     * @return long msec
     */
    public static Long toMillisec(String datestring, String format) {
        Date date = parse(datestring, format);
        if (date == null) {
            return null;
        }
        return date.getTime();
    }

    /**
     * Print the provided date object using the default date format {@link #DEFAULT_XMLLIKE_DATE_FORMAT}
     *
     * @param date should be long, Date or Calendar
     * @return date string
     */
    public static String print(Object date) {
        return print(date, new SimpleDateFormat(DEFAULT_XMLLIKE_DATE_FORMAT));
    }

    /**
     * Print the provided date object using the default date format {@link #DEFAULT_XMLLIKE_DATE_FORMAT}
     *
     * @param date should be long, Date or Calendar
     * @return date string
     */
    public static String printWithZone(Object date) {
        return print(date, new SimpleDateFormat(DEFAULT_XMLLIKE_DATE_FORMAT_WITH_ZONE));
    }

    private static String print(Object date, SimpleDateFormat sdf) {
        if (date instanceof Long) {
            return sdf.format(new Date((Long) date));
        }
        if (date instanceof Date) {
            return sdf.format((Date) date);
        }
        if (date instanceof Calendar) {
            return sdf.format(((Calendar) date).getTime());
        }
        throw new IllegalArgumentException("Date format for type '" + date.getClass() + "' not possible");
    }

    /**
     * Parse the date-time string using {@link #DEFAULT_XMLLIKE_DATE_FORMAT}.
     *
     * @param dateTime date-time string
     * @return milliseconds
     */
    public static long parseDefaultMSec(String dateTime) {
        return parse(dateTime, new SimpleDateFormat(DEFAULT_XMLLIKE_DATE_FORMAT)).getTime();
    }

    /**
     * Parse the date-time string using {@link #DEFAULT_XMLLIKE_DATE_FORMAT}.
     *
     * @param dateTime date-time string
     * @return LocalDateTime
     */
    public static LocalDateTime parseDefaultLocalDateTime(String dateTime) {
        return LocalDateTime.parse(dateTime, DateTimeFormatter.ofPattern(DEFAULT_XMLLIKE_DATE_FORMAT));
    }

    /**
     * Parse the date-time string using {@link #DEFAULT_XMLLIKE_DATE_FORMAT} assume System default time zone
     *
     * @param dateTime date-time string
     * @return ZonedDateTime
     */
    public static ZonedDateTime parseDefaultZonedDateTime(String dateTime) {
        return parseDefaultLocalDateTime(dateTime).atZone(ZoneId.systemDefault());
    }

    /**
     * Parse the date-time string using {@link #DEFAULT_XMLLIKE_DATE_FORMAT_WITH_ZONE}.
     *
     * @param dateTimeWithZone date-time string
     * @return milliseconds
     */
    public static long parseDefaultMSecWZone(String dateTimeWithZone) {
        return parse(dateTimeWithZone, new SimpleDateFormat(DEFAULT_XMLLIKE_DATE_FORMAT_WITH_ZONE)).getTime();
    }

    /**
     * Parse the date-time string using {@link #DEFAULT_XMLLIKE_DATE_FORMAT}.
     *
     * @param dateTime date-time string
     * @return date
     */
    public static Date parseDefaultDate(String dateTime) {
        return parse(dateTime, new SimpleDateFormat(DEFAULT_XMLLIKE_DATE_FORMAT));
    }

    /**
     * Parse the date-time string using {@link #DEFAULT_XMLLIKE_DATE_FORMAT}.
     *
     * @param dateTime date-time string
     * @return calendar
     */
    public static Calendar parseDefaultCal(String dateTime) {
        Calendar cal = Calendar.getInstance();
        cal.setTimeInMillis(parseDefaultMSec(dateTime));
        return cal;
    }

    private static Date parse(String str) {
        return parse(str, new SimpleDateFormat());
    }

    private static Date parse(String str, String format) {
        SimpleDateFormat sdf;
        try {
            sdf = new SimpleDateFormat(format);
        } catch (Exception ex) {
            log.warn("Error in date format '" + str + "': " + ex.getMessage(), ex);
            return null;
        }
        return parse(str, sdf);
    }

    private static Date parse(String str, SimpleDateFormat format) {
        Date d;
        try {
            d = format.parse(str);
        } catch (ParseException e) {
            log.warn("Error parsing date '" + str + "' according to format '" + format.toPattern() + "': " + e.getMessage(), e);
            return null;
        }
        return d;
    }

    private static Calendar parseGetCal(String str, SimpleDateFormat format) {
        Date d = parse(str, format);
        if (d == null) {
            return null;
        }
        Calendar cal = Calendar.getInstance();
        cal.setTimeInMillis(d.getTime());
        return cal;
    }
}
