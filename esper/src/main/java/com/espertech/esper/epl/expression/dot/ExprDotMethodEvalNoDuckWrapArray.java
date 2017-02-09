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

import com.espertech.esper.client.EventBean;
import com.espertech.esper.epl.enummethod.dot.ArrayWrappingCollection;
import com.espertech.esper.epl.expression.core.ExprEvaluator;
import com.espertech.esper.epl.expression.core.ExprEvaluatorContext;
import com.espertech.esper.epl.rettype.EPType;
import com.espertech.esper.epl.rettype.EPTypeHelper;
import net.sf.cglib.reflect.FastMethod;

public class ExprDotMethodEvalNoDuckWrapArray extends ExprDotMethodEvalNoDuck {
    public ExprDotMethodEvalNoDuckWrapArray(String statementName, FastMethod method, ExprEvaluator[] parameters) {
        super(statementName, method, parameters);
    }

    @Override
    public Object evaluate(Object target, EventBean[] eventsPerStream, boolean isNewData, ExprEvaluatorContext exprEvaluatorContext) {
        Object result = super.evaluate(target, eventsPerStream, isNewData, exprEvaluatorContext);
        if (result == null || !result.getClass().isArray()) {
            return null;
        }
        return new ArrayWrappingCollection(result);
    }

    @Override
    public EPType getTypeInfo() {
        return EPTypeHelper.collectionOfSingleValue(method.getReturnType().getComponentType());
    }
}
