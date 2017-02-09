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
package com.espertech.esper.epl.expression.core;

public class ExprNodeUtilResolveExceptionHandlerDefault implements ExprNodeUtilResolveExceptionHandler {
    private final String resolvedExpression;
    private final boolean configuredAsSingleRow;

    public ExprNodeUtilResolveExceptionHandlerDefault(String resolvedExpression, boolean configuredAsSingleRow) {
        this.resolvedExpression = resolvedExpression;
        this.configuredAsSingleRow = configuredAsSingleRow;
    }

    public ExprValidationException handle(Exception e) {
        String message;
        if (configuredAsSingleRow) {
            message = e.getMessage();
        } else {
            message = "Failed to resolve '" + resolvedExpression + "' to a property, single-row function, aggregation function, script, stream or class name";
        }
        return new ExprValidationException(message, e);
    }
}
