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
package com.espertech.esper.epl.datetime.reformatop;

import com.espertech.esper.client.EventBean;
import com.espertech.esper.client.EventType;
import com.espertech.esper.epl.datetime.eval.DatetimeLongCoercerLocalDateTime;
import com.espertech.esper.epl.datetime.eval.DatetimeLongCoercerZonedDateTime;
import com.espertech.esper.epl.datetime.eval.DatetimeMethodEnum;
import com.espertech.esper.epl.datetime.eval.ExprDotNodeFilterAnalyzerDesc;
import com.espertech.esper.epl.expression.core.ExprEvaluatorContext;
import com.espertech.esper.epl.expression.core.ExprNode;
import com.espertech.esper.epl.expression.dot.ExprDotNodeFilterAnalyzerInput;
import com.espertech.esper.epl.expression.time.TimeAbacus;

import java.time.LocalDateTime;
import java.time.ZonedDateTime;
import java.util.*;

public class ReformatOpToCalendar implements ReformatOp {

    private final TimeZone timeZone;
    private final TimeAbacus timeAbacus;

    public ReformatOpToCalendar(TimeZone timeZone, TimeAbacus timeAbacus) {
        this.timeZone = timeZone;
        this.timeAbacus = timeAbacus;
    }

    public Object evaluate(Long ts, EventBean[] eventsPerStream, boolean newData, ExprEvaluatorContext exprEvaluatorContext) {
        Calendar cal = Calendar.getInstance(timeZone);
        timeAbacus.calendarSet(ts, cal);
        return cal;
    }

    public Object evaluate(Date d, EventBean[] eventsPerStream, boolean newData, ExprEvaluatorContext exprEvaluatorContext) {
        Calendar cal = Calendar.getInstance(timeZone);
        cal.setTimeInMillis(d.getTime());
        return cal;
    }

    public Object evaluate(Calendar cal, EventBean[] eventsPerStream, boolean newData, ExprEvaluatorContext exprEvaluatorContext) {
        return cal;
    }

    public Object evaluate(LocalDateTime ldt, EventBean[] eventsPerStream, boolean newData, ExprEvaluatorContext exprEvaluatorContext) {
        Calendar cal = GregorianCalendar.getInstance(timeZone);
        cal.setTimeInMillis(DatetimeLongCoercerLocalDateTime.coerce(ldt, timeZone));
        return cal;
    }

    public Object evaluate(ZonedDateTime zdt, EventBean[] eventsPerStream, boolean newData, ExprEvaluatorContext exprEvaluatorContext) {
        Calendar cal = GregorianCalendar.getInstance(timeZone);
        cal.setTimeInMillis(DatetimeLongCoercerZonedDateTime.coerce(zdt));
        return cal;
    }

    public Class getReturnType() {
        return Calendar.class;
    }

    public ExprDotNodeFilterAnalyzerDesc getFilterDesc(EventType[] typesPerStream, DatetimeMethodEnum currentMethod, List<ExprNode> currentParameters, ExprDotNodeFilterAnalyzerInput inputDesc) {
        return null;
    }
}
