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

import com.espertech.esper.client.EPException;
import com.espertech.esper.client.EventBean;
import com.espertech.esper.epl.datetime.eval.DatetimeMethodEnum;
import com.espertech.esper.epl.expression.core.ExprEvaluatorContext;

import java.time.LocalDateTime;
import java.time.ZonedDateTime;
import java.util.Calendar;

public class CalendarOpRound implements CalendarOp {

    private final CalendarFieldEnum fieldName;
    private final int code;

    public CalendarOpRound(CalendarFieldEnum fieldName, DatetimeMethodEnum method) {
        this.fieldName = fieldName;
        if (method == DatetimeMethodEnum.ROUNDCEILING) {
            code = ApacheCommonsDateUtils.MODIFY_CEILING;
        } else if (method == DatetimeMethodEnum.ROUNDFLOOR) {
            code = ApacheCommonsDateUtils.MODIFY_TRUNCATE;
        } else if (method == DatetimeMethodEnum.ROUNDHALF) {
            code = ApacheCommonsDateUtils.MODIFY_ROUND;
        } else {
            throw new IllegalArgumentException("Unrecognized method '" + method + "'");
        }
    }

    public void evaluate(Calendar cal, EventBean[] eventsPerStream, boolean isNewData, ExprEvaluatorContext context) {
        ApacheCommonsDateUtils.modify(cal, fieldName.getCalendarField(), code);
    }

    public LocalDateTime evaluate(LocalDateTime ldt, EventBean[] eventsPerStream, boolean isNewData, ExprEvaluatorContext context) {
        if (code == ApacheCommonsDateUtils.MODIFY_TRUNCATE) {
            return ldt.truncatedTo(fieldName.getChronoUnit());
        } else if (code == ApacheCommonsDateUtils.MODIFY_CEILING) {
            return ldt.plus(1, fieldName.getChronoUnit()).truncatedTo(fieldName.getChronoUnit());
        } else {
            throw new EPException("Round-half operation not supported for LocalDateTime");
        }
    }

    public ZonedDateTime evaluate(ZonedDateTime zdt, EventBean[] eventsPerStream, boolean isNewData, ExprEvaluatorContext context) {
        if (code == ApacheCommonsDateUtils.MODIFY_TRUNCATE) {
            return zdt.truncatedTo(fieldName.getChronoUnit());
        } else if (code == ApacheCommonsDateUtils.MODIFY_CEILING) {
            return zdt.plus(1, fieldName.getChronoUnit()).truncatedTo(fieldName.getChronoUnit());
        } else {
            throw new EPException("Round-half operation not supported for ZonedDateTime");
        }
    }
}
