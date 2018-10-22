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

public class VariableTriggerSetDesc {
    private final String variableName;
    private final ExprEvaluator evaluator;

    public VariableTriggerSetDesc(String variableName, ExprEvaluator evaluator) {
        this.variableName = variableName;
        this.evaluator = evaluator;
    }

    public String getVariableName() {
        return variableName;
    }

    public ExprEvaluator getEvaluator() {
        return evaluator;
    }
}
