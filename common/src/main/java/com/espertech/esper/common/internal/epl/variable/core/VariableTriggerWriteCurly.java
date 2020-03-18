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
package com.espertech.esper.common.internal.epl.variable.core;

import com.espertech.esper.common.internal.epl.expression.core.ExprEvaluator;

public class VariableTriggerWriteCurly extends VariableTriggerWrite {
    private String variableName;
    private ExprEvaluator expression;

    public String getVariableName() {
        return variableName;
    }

    public void setVariableName(String variableName) {
        this.variableName = variableName;
    }

    public ExprEvaluator getExpression() {
        return expression;
    }

    public void setExpression(ExprEvaluator expression) {
        this.expression = expression;
    }
}
