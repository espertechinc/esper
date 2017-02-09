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
import com.espertech.esper.epl.datetime.eval.*;
import com.espertech.esper.epl.expression.core.ExprEvaluatorContext;
import com.espertech.esper.epl.expression.core.ExprNode;
import com.espertech.esper.epl.expression.core.ExprValidationException;
import com.espertech.esper.epl.expression.dot.ExprDotNodeFilterAnalyzerInput;

import java.time.LocalDateTime;
import java.time.ZonedDateTime;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.TimeZone;

public class ReformatOpBetweenConstantParams implements ReformatOp {

    private long first;
    private long second;
    private TimeZone timeZone;

    public ReformatOpBetweenConstantParams(List<ExprNode> parameters, TimeZone timeZone) throws ExprValidationException {
        this.timeZone = timeZone;

        long paramFirst = getLongValue(parameters.get(0));
        long paramSecond = getLongValue(parameters.get(1));

        if (paramFirst > paramSecond) {
            this.second = paramFirst;
            this.first = paramSecond;
        } else {
            this.first = paramFirst;
            this.second = paramSecond;
        }
        if (parameters.size() > 2) {
            if (!getBooleanValue(parameters.get(2))) {
                first = first + 1;
            }
            if (!getBooleanValue(parameters.get(3))) {
                second = second - 1;
            }
        }
    }

    private long getLongValue(ExprNode exprNode)
            throws ExprValidationException {
        Object value = exprNode.getExprEvaluator().evaluate(null, true, null);
        if (value == null) {
            throw new ExprValidationException("Date-time method 'between' requires non-null parameter values");
        }
        return DatetimeLongCoercerFactory.getCoercer(value.getClass(), timeZone).coerce(value);
    }

    private boolean getBooleanValue(ExprNode exprNode)
            throws ExprValidationException {
        Object value = exprNode.getExprEvaluator().evaluate(null, true, null);
        if (value == null) {
            throw new ExprValidationException("Date-time method 'between' requires non-null parameter values");
        }
        return (Boolean) value;
    }

    public Object evaluate(Long ts, EventBean[] eventsPerStream, boolean newData, ExprEvaluatorContext exprEvaluatorContext) {
        if (ts == null) {
            return null;
        }
        return evaluateInternal(ts);
    }

    public Object evaluate(Date d, EventBean[] eventsPerStream, boolean newData, ExprEvaluatorContext exprEvaluatorContext) {
        if (d == null) {
            return null;
        }
        return evaluateInternal(d.getTime());
    }

    public Object evaluate(Calendar cal, EventBean[] eventsPerStream, boolean newData, ExprEvaluatorContext exprEvaluatorContext) {
        if (cal == null) {
            return null;
        }
        return evaluateInternal(cal.getTimeInMillis());
    }

    public Object evaluate(LocalDateTime ldt, EventBean[] eventsPerStream, boolean newData, ExprEvaluatorContext exprEvaluatorContext) {
        if (ldt == null) {
            return null;
        }
        return evaluateInternal(DatetimeLongCoercerLocalDateTime.coerce(ldt, timeZone));
    }

    public Object evaluate(ZonedDateTime zdt, EventBean[] eventsPerStream, boolean newData, ExprEvaluatorContext exprEvaluatorContext) {
        if (zdt == null) {
            return null;
        }
        return evaluateInternal(DatetimeLongCoercerZonedDateTime.coerce(zdt));
    }

    public Object evaluateInternal(long ts) {
        return first <= ts && ts <= second;
    }

    public Class getReturnType() {
        return Boolean.class;
    }

    public ExprDotNodeFilterAnalyzerDesc getFilterDesc(EventType[] typesPerStream, DatetimeMethodEnum currentMethod, List<ExprNode> currentParameters, ExprDotNodeFilterAnalyzerInput inputDesc) {
        return null;
    }
}
