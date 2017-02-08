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

import java.io.Serializable;
import java.util.Calendar;

public class TimerScheduleSpec implements Serializable {
    private static final long serialVersionUID = -8587555098972452213L;
    private final Calendar optionalDate;
    private final Long optionalRemainder;
    private final Long optionalRepeatCount;
    private final TimePeriod optionalTimePeriod;

    public TimerScheduleSpec(Calendar optionalDate, Long optionalRemainder, Long optionalRepeatCount, TimePeriod optionalTimePeriod) {
        this.optionalDate = optionalDate;
        this.optionalRemainder = optionalRemainder;
        this.optionalRepeatCount = optionalRepeatCount;
        this.optionalTimePeriod = optionalTimePeriod;
    }

    public Calendar getOptionalDate() {
        return optionalDate;
    }

    public Long getOptionalRepeatCount() {
        return optionalRepeatCount;
    }

    public TimePeriod getOptionalTimePeriod() {
        return optionalTimePeriod;
    }

    public Long getOptionalRemainder() {
        return optionalRemainder;
    }
}
