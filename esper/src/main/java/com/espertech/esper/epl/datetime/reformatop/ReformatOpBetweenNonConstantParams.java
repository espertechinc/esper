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
import com.espertech.esper.epl.expression.core.ExprEvaluator;
import com.espertech.esper.epl.expression.core.ExprEvaluatorContext;
import com.espertech.esper.epl.expression.core.ExprNode;
import com.espertech.esper.epl.expression.core.ExprValidationException;
import com.espertech.esper.epl.expression.dot.ExprDotNodeFilterAnalyzerInput;
import com.espertech.esper.epl.expression.dot.ExprDotNodeFilterAnalyzerInputProp;
import com.espertech.esper.epl.expression.dot.ExprDotNodeFilterAnalyzerInputStream;

import java.time.LocalDateTime;
import java.time.ZonedDateTime;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.TimeZone;

public class ReformatOpBetweenNonConstantParams implements ReformatOp {

    private final ExprNode start;
    private final ExprEvaluator startEval;
    private final DatetimeLongCoercer startCoercer;
    private final ExprNode end;
    private final ExprEvaluator endEval;
    private final DatetimeLongCoercer secondCoercer;
    private final TimeZone timeZone;

    private boolean includeBoth;
    private Boolean includeLow;
    private Boolean includeHigh;
    private ExprEvaluator evalIncludeLow;
    private ExprEvaluator evalIncludeHigh;

    public ReformatOpBetweenNonConstantParams(List<ExprNode> parameters, TimeZone timeZone)
            throws ExprValidationException {
        this.timeZone = timeZone;
        start = parameters.get(0);
        startEval = start.getExprEvaluator();
        startCoercer = DatetimeLongCoercerFactory.getCoercer(startEval.getType(), timeZone);
        end = parameters.get(1);
        endEval = end.getExprEvaluator();
        secondCoercer = DatetimeLongCoercerFactory.getCoercer(endEval.getType(), timeZone);

        if (parameters.size() == 2) {
            includeBoth = true;
            includeLow = true;
            includeHigh = true;
        } else {
            if (parameters.get(2).isConstantResult()) {
                includeLow = getBooleanValue(parameters.get(2));
            } else {
                evalIncludeLow = parameters.get(2).getExprEvaluator();
            }
            if (parameters.get(3).isConstantResult()) {
                includeHigh = getBooleanValue(parameters.get(3));
            } else {
                evalIncludeHigh = parameters.get(3).getExprEvaluator();
            }
            if (includeLow != null && includeHigh != null && includeLow && includeHigh) {
                includeBoth = true;
            }
        }
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
        return evaluateInternal(ts, eventsPerStream, newData, exprEvaluatorContext);
    }

    public Object evaluate(Date d, EventBean[] eventsPerStream, boolean newData, ExprEvaluatorContext exprEvaluatorContext) {
        if (d == null) {
            return null;
        }
        return evaluateInternal(d.getTime(), eventsPerStream, newData, exprEvaluatorContext);
    }

    public Object evaluate(Calendar cal, EventBean[] eventsPerStream, boolean newData, ExprEvaluatorContext exprEvaluatorContext) {
        if (cal == null) {
            return null;
        }
        return evaluateInternal(cal.getTimeInMillis(), eventsPerStream, newData, exprEvaluatorContext);
    }

    public Object evaluate(LocalDateTime ldt, EventBean[] eventsPerStream, boolean newData, ExprEvaluatorContext exprEvaluatorContext) {
        return evaluateInternal(DatetimeLongCoercerLocalDateTime.coerce(ldt, timeZone), eventsPerStream, newData, exprEvaluatorContext);
    }

    public Object evaluate(ZonedDateTime zdt, EventBean[] eventsPerStream, boolean newData, ExprEvaluatorContext exprEvaluatorContext) {
        return evaluateInternal(DatetimeLongCoercerZonedDateTime.coerce(zdt), eventsPerStream, newData, exprEvaluatorContext);
    }

    public Class getReturnType() {
        return Boolean.class;
    }

    public Object evaluateInternal(long ts, EventBean[] eventsPerStream, boolean newData, ExprEvaluatorContext exprEvaluatorContext) {
        Object firstObj = startEval.evaluate(eventsPerStream, newData, exprEvaluatorContext);
        if (firstObj == null) {
            return null;
        }
        Object secondObj = endEval.evaluate(eventsPerStream, newData, exprEvaluatorContext);
        if (secondObj == null) {
            return null;
        }
        long first = startCoercer.coerce(firstObj);
        long second = secondCoercer.coerce(secondObj);
        if (includeBoth) {
            if (first <= second) {
                return first <= ts && ts <= second;
            } else {
                return second <= ts && ts <= first;
            }
        } else {

            boolean includeLowEndpoint;
            if (includeLow != null) {
                includeLowEndpoint = includeLow;
            } else {
                Object value = evalIncludeLow.evaluate(eventsPerStream, newData, exprEvaluatorContext);
                if (value == null) {
                    return null;
                }
                includeLowEndpoint = (Boolean) value;

            }

            boolean includeHighEndpoint;
            if (includeHigh != null) {
                includeHighEndpoint = includeHigh;
            } else {
                Object value = evalIncludeHigh.evaluate(eventsPerStream, newData, exprEvaluatorContext);
                if (value == null) {
                    return null;
                }
                includeHighEndpoint = (Boolean) value;

            }

            if (includeLowEndpoint) {
                if (ts < first) {
                    return false;
                }
            } else {
                if (ts <= first) {
                    return false;
                }
            }

            if (includeHighEndpoint) {
                if (ts > second) {
                    return false;
                }
            } else {
                if (ts >= second) {
                    return false;
                }
            }

            return true;
        }
    }

    public ExprDotNodeFilterAnalyzerDesc getFilterDesc(EventType[] typesPerStream, DatetimeMethodEnum currentMethod, List<ExprNode> currentParameters, ExprDotNodeFilterAnalyzerInput inputDesc) {
        if (includeLow == null || includeHigh == null) {
            return null;
        }

        int targetStreamNum;
        String targetProperty;
        if (inputDesc instanceof ExprDotNodeFilterAnalyzerInputStream) {
            ExprDotNodeFilterAnalyzerInputStream targetStream = (ExprDotNodeFilterAnalyzerInputStream) inputDesc;
            targetStreamNum = targetStream.getStreamNum();
            EventType targetType = typesPerStream[targetStreamNum];
            targetProperty = targetType.getStartTimestampPropertyName();
        } else if (inputDesc instanceof ExprDotNodeFilterAnalyzerInputProp) {
            ExprDotNodeFilterAnalyzerInputProp targetStream = (ExprDotNodeFilterAnalyzerInputProp) inputDesc;
            targetStreamNum = targetStream.getStreamNum();
            targetProperty = targetStream.getPropertyName();
        } else {
            return null;
        }

        return new ExprDotNodeFilterAnalyzerDTBetweenDesc(typesPerStream, targetStreamNum, targetProperty, start, end, includeLow, includeHigh);
    }
}
