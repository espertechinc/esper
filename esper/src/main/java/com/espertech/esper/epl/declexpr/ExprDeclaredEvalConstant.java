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
package com.espertech.esper.epl.declexpr;

import com.espertech.esper.client.EventBean;
import com.espertech.esper.epl.expression.core.ExprEvaluator;
import com.espertech.esper.epl.expression.core.ExprEvaluatorContext;
import com.espertech.esper.epl.spec.ExpressionDeclItem;
import com.espertech.esper.metrics.instrumentation.InstrumentationHelper;

public class ExprDeclaredEvalConstant implements ExprEvaluator {
    private final Class returnType;
    private final ExpressionDeclItem prototype;
    private final Object value;

    public ExprDeclaredEvalConstant(Class returnType, ExpressionDeclItem prototype, Object value) {
        this.returnType = returnType;
        this.prototype = prototype;
        this.value = value;
    }

    public Class getType() {
        return returnType;
    }

    public Object evaluate(EventBean[] eventsPerStream, boolean isNewData, ExprEvaluatorContext context) {
        if (InstrumentationHelper.ENABLED) {
            InstrumentationHelper.get().qExprDeclared(prototype);
        }
        if (InstrumentationHelper.ENABLED) {
            InstrumentationHelper.get().aExprDeclared(value);
        }
        return value;
    }

}
