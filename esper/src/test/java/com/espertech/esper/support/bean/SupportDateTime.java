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

package com.espertech.esper.support.bean;

import com.espertech.esper.client.util.DateTime;

import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;

public class SupportDateTime {

    private String key;
    private Long msecdate;
    private Date utildate;
    private Calendar caldate;

    public SupportDateTime(String key, Long msecdate, Date utildate, Calendar caldate) {
        this.key = key;
        this.msecdate = msecdate;
        this.utildate = utildate;
        this.caldate = caldate;
    }

    public SupportDateTime(Long msecdate, Date utildate, Calendar caldate) {
        this.msecdate = msecdate;
        this.utildate = utildate;
        this.caldate = caldate;
    }

    public Long getMsecdate() {
        return msecdate;
    }

    public Date getUtildate() {
        return utildate;
    }

    public Calendar getCaldate() {
        return caldate;
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
            return new SupportDateTime(null, null, null);
        }
        // expected : 2002-05-30T09:00:00
        Date date = DateTime.parseDefaultDate(datestr);
        Calendar cal = Calendar.getInstance();
        cal.setTimeInMillis(date.getTime());
        cal.set(Calendar.MILLISECOND, 0);
        return new SupportDateTime(date.getTime(), date, cal);
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

    private static Object coerce(long msec, String format) {
        if (format.equalsIgnoreCase("msec")) {
            return msec;
        }
        else if (format.equalsIgnoreCase("util")) {
            return new Date(msec);
        }
        else if (format.equalsIgnoreCase("cal")) {
            Calendar cal = Calendar.getInstance();
            cal.setTimeInMillis(msec);
            return cal;
        }
        else if (format.equalsIgnoreCase("sdf")) {
            Calendar cal = Calendar.getInstance();
            cal.setTimeInMillis(msec);
            SimpleDateFormat sdf = new SimpleDateFormat();
            return sdf.format(cal.getTime());
        }
        else if (format.equalsIgnoreCase("null")) {
            return null;
        }
        else {
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
