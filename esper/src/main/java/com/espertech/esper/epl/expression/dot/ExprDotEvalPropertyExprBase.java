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
package com.espertech.esper.epl.expression.dot;

import com.espertech.esper.epl.expression.core.ExprEvaluator;
import com.espertech.esper.util.JavaClassHelper;

public abstract class ExprDotEvalPropertyExprBase implements ExprEvaluator {

    protected final String statementName;
    protected final String propertyName;
    protected final int streamNum;
    protected final ExprEvaluator exprEvaluator;
    private final Class propertyType;

    protected ExprDotEvalPropertyExprBase(String statementName, String propertyName, int streamNum, ExprEvaluator exprEvaluator, Class propertyType) {
        this.statementName = statementName;
        this.propertyName = propertyName;
        this.streamNum = streamNum;
        this.exprEvaluator = exprEvaluator;
        this.propertyType = propertyType;
    }

    public Class getType() {
        return propertyType;
    }

    protected String getWarningText(String expectedType, Object received) {
        return "Statement '" + statementName + "' property " + propertyName + " parameter expression expected a value of " +
                expectedType + " but received " + received == null ? "null" : JavaClassHelper.getClassNameFullyQualPretty(received.getClass());
    }
}
