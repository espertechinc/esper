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
package com.espertech.esper.supportregression.timer;

import com.espertech.esper.supportregression.bean.SupportTimeStartBase;
import com.espertech.esper.supportregression.bean.SupportTimeStartEndA;

import java.util.function.Function;

public enum SupportDateTimeFieldType {
    MSEC("long", SupportTimeStartBase::getlongdateEnd),
    DATE("java.util.Date", SupportTimeStartBase::getUtildateEnd),
    CAL("java.util.Calendar", SupportTimeStartBase::getCaldateEnd),
    LDT("java.time.LocalDateTime", SupportTimeStartBase::getLdtEnd),
    ZDT("java.time.ZonedDateTime", SupportTimeStartBase::getZdtEnd);

    private final String type;
    private final Function<SupportTimeStartEndA, Object> endDateTimeProvider;

    SupportDateTimeFieldType(String type, Function<SupportTimeStartEndA, Object> endDateTimeProvider) {
        this.type = type;
        this.endDateTimeProvider = endDateTimeProvider;
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
