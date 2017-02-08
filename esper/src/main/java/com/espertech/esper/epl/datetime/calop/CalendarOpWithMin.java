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

import com.espertech.esper.client.EventBean;
import com.espertech.esper.epl.expression.core.ExprEvaluatorContext;

import java.time.LocalDateTime;
import java.time.ZonedDateTime;
import java.time.temporal.ValueRange;
import java.util.Calendar;

public class CalendarOpWithMin implements CalendarOp {

    private final CalendarFieldEnum fieldName;

    public CalendarOpWithMin(CalendarFieldEnum fieldName) {
        this.fieldName = fieldName;
    }

    public void evaluate(Calendar cal, EventBean[] eventsPerStream, boolean isNewData, ExprEvaluatorContext context) {
        cal.set(fieldName.getCalendarField(), cal.getActualMinimum(fieldName.getCalendarField()));
    }

    public LocalDateTime evaluate(LocalDateTime ldt, EventBean[] eventsPerStream, boolean isNewData, ExprEvaluatorContext context) {
        ValueRange range = ldt.range(fieldName.getChronoField());
        return ldt.with(fieldName.getChronoField(), range.getMinimum());
    }

    public ZonedDateTime evaluate(ZonedDateTime zdt, EventBean[] eventsPerStream, boolean isNewData, ExprEvaluatorContext context) {
        ValueRange range = zdt.range(fieldName.getChronoField());
        return zdt.with(fieldName.getChronoField(), range.getMinimum());
    }
}
