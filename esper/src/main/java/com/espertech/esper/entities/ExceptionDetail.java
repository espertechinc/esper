/*
 * *************************************************************************************
 *  Copyright (C) 2006-2015 EsperTech, Inc. All rights reserved.                       *
 *  http://www.espertech.com/esper                                                     *
 *  http://www.espertech.com                                                           *
 *  ---------------------------------------------------------------------------------- *
 *  The software in this package is published under the terms of the GPL license       *
 *  a copy of which has been included with this distribution in the license.txt file.  *
 * *************************************************************************************
 */

package com.espertech.esper.entities;

import java.io.PrintWriter;
import java.io.StringWriter;

public class ExceptionDetail {
    private String exceptionClass;
    private String exceptionMessage;
    private String exceptionStackTrace;
    private Integer line;
    private String expression;
    private String fieldName;

    public ExceptionDetail(String exceptionClass, String exceptionMessage, String exceptionStackTrace, Integer line, String expression, String fieldName) {
        this.exceptionClass = exceptionClass;
        this.exceptionMessage = exceptionMessage;
        this.exceptionStackTrace = exceptionStackTrace;
        this.line = line;
        this.expression = expression;
        this.fieldName = fieldName;
    }

    public ExceptionDetail() {
    }

    public String getExceptionClass() {
        return exceptionClass;
    }

    public String getExceptionMessage() {
        return exceptionMessage;
    }

    public String getExceptionStackTrace() {
        return exceptionStackTrace;
    }

    public Integer getLine() {
        return line;
    }

    public String getExpression() {
        return expression;
    }

    public String getFieldName() {
        return fieldName;
    }

    public void setExceptionClass(String exceptionClass) {
        this.exceptionClass = exceptionClass;
    }

    public void setExceptionMessage(String exceptionMessage) {
        this.exceptionMessage = exceptionMessage;
    }

    public void setExceptionStackTrace(String exceptionStackTrace) {
        this.exceptionStackTrace = exceptionStackTrace;
    }

    public void setLine(Integer line) {
        this.line = line;
    }

    public void setExpression(String expression) {
        this.expression = expression;
    }

    public void setFieldName(String fieldName) {
        this.fieldName = fieldName;
    }

    public static ExceptionDetail fromException(Throwable ex, Integer line, String expression, String fieldName) {
        return new ExceptionDetail(ex.getClass().getName(), ex.getMessage(),
                stackTraceToString(ex.getStackTrace()), line, expression, fieldName);
    }

    private static String stackTraceToString(StackTraceElement[] stackTrace) {
        StringWriter sw = new StringWriter();
        printStackTrace(stackTrace, new PrintWriter(sw));
        return sw.toString();
    }

    private static void printStackTrace(StackTraceElement[] stackTrace, PrintWriter pw) {
        for (StackTraceElement stackTraceEl : stackTrace) {
            pw.println(stackTraceEl);
        }
    }
}
