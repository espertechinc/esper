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
import com.espertech.esper.epl.expression.core.*;
import com.espertech.esper.epl.rettype.ClassEPType;
import com.espertech.esper.epl.rettype.EPType;
import com.espertech.esper.util.JavaClassHelper;

import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.time.format.DateTimeFormatter;
import java.util.Calendar;
import java.util.Date;

public class CalendarOpUtil {

    protected static Integer getInt(ExprEvaluator expr, EventBean[] eventsPerStream, boolean isNewData, ExprEvaluatorContext context) {
        Object result = expr.evaluate(eventsPerStream, isNewData, context);
        if (result == null) {
            return null;
        }
        return (Integer) result;
    }

    public static CalendarFieldEnum getEnum(String methodName, ExprNode exprNode) throws ExprValidationException {
        String message = validateConstant(methodName, exprNode);
        if (message != null) {
            message += ", " + getValidFieldNamesMessage();
            throw new ExprValidationException(message);
        }
        String fieldname = (String) exprNode.getForge().getExprEvaluator().evaluate(null, true, null);
        CalendarFieldEnum fieldNum = CalendarFieldEnum.fromString(fieldname);
        if (fieldNum == null) {
            throw new ExprValidationException(getMessage(methodName) + " datetime-field name '" + fieldname + "' is not recognized, " + getValidFieldNamesMessage());
        }
        return fieldNum;
    }

    public static Object getFormatter(EPType inputType, String methodName, ExprNode exprNode, ExprEvaluatorContext exprEvaluatorContext) throws ExprValidationException {
        if (!(inputType instanceof ClassEPType)) {
            throw new ExprValidationException(getMessage(methodName) + " requires a datetime input value but received " + inputType);
        }

        ClassEPType input = (ClassEPType) inputType;
        Object format = ExprNodeUtilityCore.evaluateValidationTimeNoStreams(exprNode.getForge().getExprEvaluator(), exprEvaluatorContext, "date format");
        if (format == null) {
            throw new ExprValidationException(getMessage(methodName) + " invalid null format object");
        }

        // handle legacy date
        if (JavaClassHelper.getBoxedType(input.getType()) == Long.class ||
                JavaClassHelper.isSubclassOrImplementsInterface(input.getType(), Date.class) ||
                JavaClassHelper.isSubclassOrImplementsInterface(input.getType(), Calendar.class)) {

            if (format instanceof DateFormat) {
                return format;
            }
            if (format instanceof String) {
                try {
                    return new SimpleDateFormat((String) format);
                } catch (RuntimeException ex) {
                    throw new ExprValidationException(getMessage(methodName) + " invalid format string (SimpleDateFormat): " + ex.getMessage(), ex);
                }
            }
            throw getFailedExpected(methodName, DateFormat.class, format);
        }

        // handle jdk8 date
        if (format instanceof DateTimeFormatter) {
            return format;
        }
        if (format instanceof String) {
            try {
                return DateTimeFormatter.ofPattern((String) format);
            } catch (RuntimeException ex) {
                throw new ExprValidationException(getMessage(methodName) + " invalid format string (DateTimeFormatter): " + ex.getMessage(), ex);
            }
        }
        throw getFailedExpected(methodName, DateTimeFormatter.class, format);
    }

    private static ExprValidationException getFailedExpected(String methodName, Class expected, Object received) {
        return new ExprValidationException(getMessage(methodName) + " invalid format, expected string-format or " + expected.getSimpleName() + " but received " + JavaClassHelper.getClassNameFullyQualPretty(received.getClass()));
    }

    private static String validateConstant(String methodName, ExprNode exprNode) {
        if (ExprNodeUtilityCore.isConstantValueExpr(exprNode)) {
            return null;
        }
        return getMessage(methodName) + " requires a constant string-type parameter as its first parameter";
    }

    private static String getMessage(String methodName) {
        return "Date-time enumeration method '" + methodName + "'";
    }

    private static String getValidFieldNamesMessage() {
        return "valid field names are '" + CalendarFieldEnum.getValidList() + "'";
    }
}
