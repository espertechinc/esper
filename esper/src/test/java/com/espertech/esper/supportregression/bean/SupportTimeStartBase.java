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

package com.espertech.esper.supportregression.bean;

import com.espertech.esper.client.util.DateTime;

import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.temporal.ChronoUnit;
import java.util.Calendar;
import java.util.Date;

public abstract class SupportTimeStartBase {

    private String key;
    private Long msecdateStart;
    private Date utildateStart;
    private Calendar caldateStart;
    private LocalDateTime ldtStart;
    private ZonedDateTime zdtStart;
    private Long msecdateEnd;
    private Date utildateEnd;
    private Calendar caldateEnd;
    private LocalDateTime ldtEnd;
    private ZonedDateTime zdtEnd;

    public SupportTimeStartBase(String key, String datestr, long duration) {
        this.key = key;

        if (datestr != null) {
            // expected : 2002-05-30T09:00:00.000
            long start = DateTime.parseDefaultMSec(datestr);
            long end = start + duration;

            this.msecdateStart = start;
            this.utildateStart = SupportDateTime.toDate(start);
            this.caldateStart = SupportDateTime.toCalendar(start);
            this.ldtStart = DateTime.parseDefaultLocalDateTime(datestr);
            this.zdtStart = ldtStart.atZone(ZoneId.systemDefault());
            this.msecdateEnd = end;
            this.utildateEnd = SupportDateTime.toDate(end);
            this.caldateEnd = SupportDateTime.toCalendar(end);
            this.ldtEnd = ldtStart.plus(duration, ChronoUnit.MILLIS);
            this.zdtEnd = ldtEnd.atZone(ZoneId.systemDefault());
        }
    }

    public Long getMsecdateStart() {
        return msecdateStart;
    }

    public Date getUtildateStart() {
        return utildateStart;
    }

    public Calendar getCaldateStart() {
        return caldateStart;
    }

    public Long getMsecdateEnd() {
        return msecdateEnd;
    }

    public Date getUtildateEnd() {
        return utildateEnd;
    }

    public Calendar getCaldateEnd() {
        return caldateEnd;
    }

    public LocalDateTime getLdtStart() {
        return ldtStart;
    }

    public ZonedDateTime getZdtStart() {
        return zdtStart;
    }

    public LocalDateTime getLdtEnd() {
        return ldtEnd;
    }

    public ZonedDateTime getZdtEnd() {
        return zdtEnd;
    }

    public String getKey() {
        return key;
    }

    public void setKey(String key) {
        this.key = key;
    }
}
