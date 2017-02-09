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
import com.espertech.esper.epl.expression.core.ExprEvaluator;
import com.espertech.esper.epl.expression.core.ExprEvaluatorContext;
import com.espertech.esper.epl.rettype.EPType;
import com.espertech.esper.epl.rettype.EPTypeHelper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.lang.reflect.Array;

public class ExprDotEvalArrayGet implements ExprDotEval {
    private static final Logger log = LoggerFactory.getLogger(ExprDotEvalArrayGet.class);

    private final EPType typeInfo;
    private final ExprEvaluator indexExpression;

    public ExprDotEvalArrayGet(ExprEvaluator index, Class componentType) {
        this.indexExpression = index;
        this.typeInfo = EPTypeHelper.singleValue(componentType);
    }

    public Object evaluate(Object target, EventBean[] eventsPerStream, boolean isNewData, ExprEvaluatorContext exprEvaluatorContext) {
        if (target == null) {
            return null;
        }

        Object index = indexExpression.evaluate(eventsPerStream, isNewData, exprEvaluatorContext);
        if (index == null) {
            return null;
        }
        if (!(index instanceof Integer)) {
            return null;
        }
        int indexNum = (Integer) index;

        if (Array.getLength(target) <= indexNum) {
            return null;
        }
        return Array.get(target, indexNum);
    }

    public EPType getTypeInfo() {
        return typeInfo;
    }

    public void visit(ExprDotEvalVisitor visitor) {
        visitor.visitArraySingleItemSource();
    }
}
