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
package com.espertech.esper.epl.datetime.calop;

import java.io.StringWriter;
import java.time.temporal.ChronoField;
import java.time.temporal.ChronoUnit;
import java.util.Calendar;
import java.util.Locale;

public enum CalendarFieldEnum {
    MILLISEC(Calendar.MILLISECOND, "msec,millisecond,milliseconds", ChronoField.MILLI_OF_SECOND, ChronoUnit.MILLIS),
    SECOND(Calendar.SECOND, "sec,second,seconds", ChronoField.SECOND_OF_MINUTE, ChronoUnit.SECONDS),
    MINUTE(Calendar.MINUTE, "min,minute,minutes", ChronoField.MINUTE_OF_HOUR, ChronoUnit.MINUTES),
    HOUR(Calendar.HOUR_OF_DAY, "hour,hours", ChronoField.HOUR_OF_DAY, ChronoUnit.HOURS),
    DAY(Calendar.DATE, "day,days", ChronoField.DAY_OF_MONTH, ChronoUnit.DAYS),
    MONTH(Calendar.MONTH, "month,months", ChronoField.MONTH_OF_YEAR, ChronoUnit.MONTHS),
    WEEK(Calendar.WEEK_OF_YEAR, "week,weeks", ChronoField.ALIGNED_WEEK_OF_YEAR, ChronoUnit.WEEKS),
    YEAR(Calendar.YEAR, "year,years", ChronoField.YEAR, ChronoUnit.YEARS);

    private final int calendarField;
    private final String[] names;
    private final ChronoField chronoField;
    private final ChronoUnit chronoUnit;

    CalendarFieldEnum(int calendarField, String names, ChronoField chronoField, ChronoUnit chronoUnit) {
        this.calendarField = calendarField;
        this.names = names.split(",");
        this.chronoField = chronoField;
        this.chronoUnit = chronoUnit;
    }

    public static String getValidList() {
        StringWriter writer = new StringWriter();
        String delimiter = "";
        for (CalendarFieldEnum v : CalendarFieldEnum.values()) {
            for (String name : v.names) {
                writer.append(delimiter);
                writer.append(name);
                delimiter = ",";
            }
        }
        return writer.toString();
    }

    public int getCalendarField() {
        return calendarField;
    }

    public String[] getNames() {
        return names;
    }

    public ChronoUnit getChronoUnit() {
        return chronoUnit;
    }

    public static CalendarFieldEnum fromString(String field) {
        String compareTo = field.trim().toLowerCase(Locale.ENGLISH);
        for (CalendarFieldEnum v : CalendarFieldEnum.values()) {
            for (String name : v.names) {
                if (name.equals(compareTo)) {
                    return v;
                }
            }
        }
        return null;
    }

    public ChronoField getChronoField() {
        return chronoField;
    }
}

