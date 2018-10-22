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
package com.espertech.esper.regressionlib.support.schedule;

import com.espertech.esper.regressionlib.support.bean.SupportTimeStartBase;
import com.espertech.esper.regressionlib.support.bean.SupportTimeStartEndA;

import java.time.LocalDateTime;
import java.time.ZonedDateTime;
import java.util.Calendar;
import java.util.Date;
import java.util.function.Function;

public enum SupportDateTimeFieldType {
    MSEC(long.class, "long", SupportTimeStartBase::getlongdateEnd),
    DATE(Date.class, "java.util.Date", SupportTimeStartBase::getUtildateEnd),
    CAL(Calendar.class, "java.util.Calendar", SupportTimeStartBase::getCaldateEnd),
    LDT(LocalDateTime.class, "java.time.LocalDateTime", SupportTimeStartBase::getLdtEnd),
    ZDT(ZonedDateTime.class, "java.time.ZonedDateTime", SupportTimeStartBase::getZdtEnd);

    private final Class clazz;
    private final String type;
    private final Function<SupportTimeStartEndA, Object> endDateTimeProvider;

    SupportDateTimeFieldType(Class clazz, String type, Function<SupportTimeStartEndA, Object> endDateTimeProvider) {
        this.clazz = clazz;
        this.type = type;
        this.endDateTimeProvider = endDateTimeProvider;
    }

    public Class getClazz() {
        return clazz;
    }

    public String getType() {
        return type;
    }

    public Object makeStart(String time) {
        return fromEndDate(SupportTimeStartEndA.make(null, time, 0));
    }

    public Object makeEnd(String time, long duration) {
        return fromEndDate(SupportTimeStartEndA.make(null, time, duration));
    }

    private Object fromEndDate(SupportTimeStartEndA holder) {
        return this.endDateTimeProvider.apply(holder);
    }
}
