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
import com.espertech.esper.epl.expression.core.ExprEvaluator;
import com.espertech.esper.epl.expression.core.ExprEvaluatorContext;

import java.time.LocalDateTime;
import java.time.ZonedDateTime;
import java.util.Calendar;

public class CalendarOpSet implements CalendarOp {

    private final CalendarFieldEnum fieldName;
    private final ExprEvaluator valueExpr;

    public CalendarOpSet(CalendarFieldEnum fieldName, ExprEvaluator valueExpr) {
        this.fieldName = fieldName;
        this.valueExpr = valueExpr;
    }

    public void evaluate(Calendar cal, EventBean[] eventsPerStream, boolean isNewData, ExprEvaluatorContext context) {
        Integer value = CalendarOpUtil.getInt(valueExpr, eventsPerStream, isNewData, context);
        if (value == null) {
            return;
        }
        cal.set(fieldName.getCalendarField(), value);
    }

    public LocalDateTime evaluate(LocalDateTime ldt, EventBean[] eventsPerStream, boolean isNewData, ExprEvaluatorContext context) {
        Integer value = CalendarOpUtil.getInt(valueExpr, eventsPerStream, isNewData, context);
        if (value == null) {
            return ldt;
        }
        return ldt.with(fieldName.getChronoField(), value);
    }

    public ZonedDateTime evaluate(ZonedDateTime zdt, EventBean[] eventsPerStream, boolean isNewData, ExprEvaluatorContext context) {
        Integer value = CalendarOpUtil.getInt(valueExpr, eventsPerStream, isNewData, context);
        if (value == null) {
            return zdt;
        }
        return zdt.with(fieldName.getChronoField(), value);
    }
}
