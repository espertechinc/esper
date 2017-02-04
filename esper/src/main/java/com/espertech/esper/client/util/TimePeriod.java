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

package com.espertech.esper.client.util;

import java.io.Serializable;

public class TimePeriod implements Serializable {
    private static final long serialVersionUID = 1897460581178729620L;
    private Integer years;
    private Integer months;
    private Integer weeks;
    private Integer days;
    private Integer hours;
    private Integer minutes;
    private Integer seconds;
    private Integer milliseconds;
    private Integer microseconds;

    public TimePeriod(Integer years, Integer months, Integer weeks, Integer days, Integer hours, Integer minutes, Integer seconds, Integer milliseconds, Integer microseconds) {
        this.years = years;
        this.months = months;
        this.weeks = weeks;
        this.days = days;
        this.hours = hours;
        this.minutes = minutes;
        this.seconds = seconds;
        this.milliseconds = milliseconds;
        this.microseconds = microseconds;
    }

    public TimePeriod() {
    }

    public Integer getYears() {
        return years;
    }

    public Integer getMonths() {
        return months;
    }

    public Integer getWeeks() {
        return weeks;
    }

    public Integer getDays() {
        return days;
    }

    public Integer getHours() {
        return hours;
    }

    public Integer getMinutes() {
        return minutes;
    }

    public Integer getSeconds() {
        return seconds;
    }

    public Integer getMilliseconds() {
        return milliseconds;
    }

    public Integer getMicroseconds() {
        return microseconds;
    }

    public void setYears(Integer years) {
        this.years = years;
    }

    public void setMonths(Integer months) {
        this.months = months;
    }

    public void setWeeks(Integer weeks) {
        this.weeks = weeks;
    }

    public void setDays(Integer days) {
        this.days = days;
    }

    public void setHours(Integer hours) {
        this.hours = hours;
    }

    public void setMinutes(Integer minutes) {
        this.minutes = minutes;
    }

    public void setSeconds(Integer seconds) {
        this.seconds = seconds;
    }

    public void setMilliseconds(Integer milliseconds) {
        this.milliseconds = milliseconds;
    }

    public void setMicroseconds(Integer microseconds) {
        this.microseconds = microseconds;
    }

    public TimePeriod years(Integer years) {
        this.years = years;
        return this;
    }

    public TimePeriod months(Integer months) {
        this.months = months;
        return this;
    }

    public TimePeriod weeks(Integer weeks) {
        this.weeks = weeks;
        return this;
    }

    public TimePeriod days(Integer days) {
        this.days = days;
        return this;
    }

    public TimePeriod hours(Integer hours) {
        this.hours = hours;
        return this;
    }

    public TimePeriod min(Integer minutes) {
        this.minutes = minutes;
        return this;
    }

    public TimePeriod sec(Integer seconds) {
        this.seconds = seconds;
        return this;
    }

    public TimePeriod millis(Integer milliseconds) {
        this.milliseconds = milliseconds;
        return this;
    }

    public TimePeriod micros(Integer microseconds) {
        this.microseconds = microseconds;
        return this;
    }

    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        TimePeriod that = (TimePeriod) o;

        if (days != null ? !days.equals(that.days) : that.days != null) return false;
        if (hours != null ? !hours.equals(that.hours) : that.hours != null) return false;
        if (milliseconds != null ? !milliseconds.equals(that.milliseconds) : that.milliseconds != null) return false;
        if (microseconds != null ? !microseconds.equals(that.microseconds) : that.microseconds != null) return false;
        if (minutes != null ? !minutes.equals(that.minutes) : that.minutes != null) return false;
        if (months != null ? !months.equals(that.months) : that.months != null) return false;
        if (seconds != null ? !seconds.equals(that.seconds) : that.seconds != null) return false;
        if (weeks != null ? !weeks.equals(that.weeks) : that.weeks != null) return false;
        if (years != null ? !years.equals(that.years) : that.years != null) return false;

        return true;
    }

    public int hashCode() {
        int result = years != null ? years.hashCode() : 0;
        result = 31 * result + (months != null ? months.hashCode() : 0);
        result = 31 * result + (weeks != null ? weeks.hashCode() : 0);
        result = 31 * result + (days != null ? days.hashCode() : 0);
        result = 31 * result + (hours != null ? hours.hashCode() : 0);
        result = 31 * result + (minutes != null ? minutes.hashCode() : 0);
        result = 31 * result + (seconds != null ? seconds.hashCode() : 0);
        result = 31 * result + (milliseconds != null ? milliseconds.hashCode() : 0);
        result = 31 * result + (microseconds != null ? microseconds.hashCode() : 0);
        return result;
    }

    public String toStringISO8601() {
        StringBuilder buf = new StringBuilder();
        if (years != null) {
            append(buf, years, "Y");
        }
        if (months != null) {
            append(buf, months, "M");
        }
        if (weeks != null) {
            append(buf, weeks, "W");
        }
        if (days != null) {
            append(buf, days, "D");
        }
        if (hours != null || minutes != null || seconds != null) {
            buf.append("T");
            if (hours != null) {
                append(buf, hours, "H");
            }
            if (minutes != null) {
                append(buf, minutes, "M");
            }
            if (seconds != null) {
                append(buf, seconds, "S");
            }
        }
        return buf.toString();
    }

    public Integer largestAbsoluteValue() {
        Integer absMax = null;
        if (years != null && (absMax == null || Math.abs(years) > absMax)) {
            absMax = Math.abs(years);
        }
        if (months != null && (absMax == null || Math.abs(months) > absMax)) {
            absMax = Math.abs(months);
        }
        if (weeks != null && (absMax == null || Math.abs(weeks) > absMax)) {
            absMax = Math.abs(weeks);
        }
        if (days != null && (absMax == null || Math.abs(days) > absMax)) {
            absMax = Math.abs(days);
        }
        if (hours != null && (absMax == null || Math.abs(hours) > absMax)) {
            absMax = Math.abs(hours);
        }
        if (minutes != null && (absMax == null || Math.abs(minutes) > absMax)) {
            absMax = Math.abs(minutes);
        }
        if (seconds != null && (absMax == null || Math.abs(seconds) > absMax)) {
            absMax = Math.abs(seconds);
        }
        if (milliseconds != null && (absMax == null || Math.abs(milliseconds) > absMax)) {
            absMax = Math.abs(milliseconds);
        }
        if (microseconds != null && (absMax == null || Math.abs(microseconds) > absMax)) {
            absMax = Math.abs(microseconds);
        }
        return absMax;
    }

    private void append(StringBuilder buf, Integer units, String unit) {
        buf.append(Integer.toString(units));
        buf.append(unit);
    }
}
