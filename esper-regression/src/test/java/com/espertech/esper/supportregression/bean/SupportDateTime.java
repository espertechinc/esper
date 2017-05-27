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
package com.espertech.esper.supportregression.bean;

import com.espertech.esper.client.util.DateTime;

import java.text.SimpleDateFormat;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Calendar;
import java.util.Date;

public class SupportDateTime {

    private String key;
    private Long longdate;
    private Date utildate;
    private Calendar caldate;
    private LocalDateTime localdate;
    private ZonedDateTime zoneddate;

    public SupportDateTime(Long longdate, Date utildate, Calendar caldate, LocalDateTime localdate, ZonedDateTime zoneddate) {
        this.longdate = longdate;
        this.utildate = utildate;
        this.caldate = caldate;
        this.localdate = localdate;
        this.zoneddate = zoneddate;
    }

    public Long getLongdate() {
        return longdate;
    }

    public Date getUtildate() {
        return utildate;
    }

    public Calendar getCaldate() {
        return caldate;
    }

    public LocalDateTime getLocaldate() {
        return localdate;
    }

    public ZonedDateTime getZoneddate() {
        return zoneddate;
    }

    public String getKey() {
        return key;
    }

    public void setKey(String key) {
        this.key = key;
    }

    public static Calendar toCalendar(long value) {
        Calendar cal = Calendar.getInstance();
        cal.setTimeInMillis(value);
        return cal;
    }

    public static Date toDate(long value) {
        Calendar cal = Calendar.getInstance();
        cal.setTimeInMillis(value);
        return cal.getTime();
    }

    public static SupportDateTime make(String datestr) {
        if (datestr == null) {
            return new SupportDateTime(null, null, null, null, null);
        }
        // expected : 2002-05-30T09:00:00
        Date date = DateTime.parseDefaultDate(datestr);
        Calendar cal = Calendar.getInstance();
        cal.setTimeInMillis(date.getTime());
        cal.set(Calendar.MILLISECOND, 0);
        LocalDateTime localDateTime = LocalDateTime.parse(datestr, DateTimeFormatter.ofPattern(DateTime.DEFAULT_XMLLIKE_DATE_FORMAT));
        ZonedDateTime zonedDateTime = localDateTime.atZone(ZoneId.systemDefault());
        return new SupportDateTime(date.getTime(), date, cal, localDateTime, zonedDateTime);
    }

    public static SupportDateTime make(String key, String datestr) {
        SupportDateTime bean = make(datestr);
        bean.setKey(key);
        return bean;
    }

    public static Object getValueCoerced(String expectedTime, String format) {
        long msec = DateTime.parseDefaultMSec(expectedTime);
        return coerce(msec, format);
    }

    private static Object coerce(long time, String format) {
        if (format.equalsIgnoreCase("long")) {
            return time;
        } else if (format.equalsIgnoreCase("util")) {
            return new Date(time);
        } else if (format.equalsIgnoreCase("cal")) {
            Calendar cal = Calendar.getInstance();
            cal.setTimeInMillis(time);
            return cal;
        } else if (format.equalsIgnoreCase("ldt")) {
            return LocalDateTime.ofInstant(Instant.ofEpochMilli(time), ZoneId.systemDefault());
        } else if (format.equalsIgnoreCase("zdt")) {
            return ZonedDateTime.ofInstant(Instant.ofEpochMilli(time), ZoneId.systemDefault());
        } else if (format.equalsIgnoreCase("sdf")) {
            Calendar cal = Calendar.getInstance();
            cal.setTimeInMillis(time);
            SimpleDateFormat sdf = new SimpleDateFormat();
            return sdf.format(cal.getTime());
        } else if (format.equalsIgnoreCase("dtf_isodt")) {
            LocalDateTime date = LocalDateTime.ofInstant(Instant.ofEpochMilli(time), ZoneId.systemDefault());
            return DateTimeFormatter.ISO_DATE_TIME.format(date);
        } else if (format.equalsIgnoreCase("dtf_isozdt")) {
            ZonedDateTime date = ZonedDateTime.ofInstant(Instant.ofEpochMilli(time), ZoneId.systemDefault());
            return DateTimeFormatter.ISO_ZONED_DATE_TIME.format(date);
        } else if (format.equalsIgnoreCase("null")) {
            return null;
        } else {
            throw new RuntimeException("Unrecognized format abbreviation '" + format + "'");
        }
    }

    public static Object[] getArrayCoerced(String expectedTime, String... desc) {
        Object[] result = new Object[desc.length];
        long msec = DateTime.parseDefaultMSec(expectedTime);
        for (int i = 0; i < desc.length; i++) {
            result[i] = coerce(msec, desc[i]);
        }
        return result;
    }

    public static Object[] getArrayCoerced(String[] expectedTimes, String desc) {
        Object[] result = new Object[expectedTimes.length];
        for (int i = 0; i < expectedTimes.length; i++) {
            long msec = DateTime.parseDefaultMSec(expectedTimes[i]);
            result[i] = coerce(msec, desc);
        }
        return result;
    }
}
