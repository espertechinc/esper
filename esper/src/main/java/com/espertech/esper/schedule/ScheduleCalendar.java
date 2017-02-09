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

/**
 * Calendar class for use in scheduling, specifically for use in computing the next invocation time.
 */
public class ScheduleCalendar {
    private int milliseconds;
    private int second;
    private int minute;
    private int hour;
    private int dayOfMonth;
    private int month;

    ScheduleCalendar(int milliseconds, int second, int minute, int hour, int dayOfMonth, int month) {
        this.milliseconds = milliseconds;
        this.second = second;
        this.minute = minute;
        this.hour = hour;
        this.dayOfMonth = dayOfMonth;
        this.month = month;
    }

    ScheduleCalendar() {
    }

    int getMilliseconds() {
        return milliseconds;
    }

    void setMilliseconds(int milliseconds) {
        this.milliseconds = milliseconds;
    }

    int getSecond() {
        return second;
    }

    void setSecond(int second) {
        this.second = second;
    }

    int getMinute() {
        return minute;
    }

    void setMinute(int minute) {
        this.minute = minute;
    }

    int getHour() {
        return hour;
    }

    void setHour(int hour) {
        this.hour = hour;
    }

    int getDayOfMonth() {
        return dayOfMonth;
    }

    void setDayOfMonth(int dayOfMonth) {
        this.dayOfMonth = dayOfMonth;
    }

    int getMonth() {
        return month;
    }

    void setMonth(int month) {
        this.month = month;
    }
}

