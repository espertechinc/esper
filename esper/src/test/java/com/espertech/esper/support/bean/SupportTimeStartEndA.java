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

import java.util.Calendar;
import java.util.Date;

public class SupportTimeStartEndA {

    private String key;
    private Long msecdateStart;
    private Date utildateStart;
    private Calendar caldateStart;
    private Long msecdateEnd;
    private Date utildateEnd;
    private Calendar caldateEnd;

    public SupportTimeStartEndA(String key, Long msecdateStart, Date utildateStart, Calendar caldateStart, Long msecdateEnd, Date utildateEnd, Calendar caldateEnd) {
        this.key = key;
        this.msecdateStart = msecdateStart;
        this.utildateStart = utildateStart;
        this.caldateStart = caldateStart;
        this.msecdateEnd = msecdateEnd;
        this.utildateEnd = utildateEnd;
        this.caldateEnd = caldateEnd;
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

    public String getKey() {
        return key;
    }

    public void setKey(String key) {
        this.key = key;
    }

    public static SupportTimeStartEndA make(String key, String datestr, long duration) {
        if (datestr == null) {
            return new SupportTimeStartEndA(key, null, null, null, null, null, null);
        }
        // expected : 2002-05-30T09:00:00.000
        long start = DateTime.parseDefaultMSec(datestr);
        long end = start + duration;

        return new SupportTimeStartEndA(key, start, SupportDateTime.toDate(start), SupportDateTime.toCalendar(start),
                end, SupportDateTime.toDate(end), SupportDateTime.toCalendar(end));
    }
}
