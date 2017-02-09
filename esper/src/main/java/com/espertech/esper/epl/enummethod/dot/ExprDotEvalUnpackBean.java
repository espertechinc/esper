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
package com.espertech.esper.epl.enummethod.dot;

import com.espertech.esper.client.EventBean;
import com.espertech.esper.client.EventType;
import com.espertech.esper.epl.expression.core.ExprEvaluatorContext;
import com.espertech.esper.epl.expression.dot.ExprDotEval;
import com.espertech.esper.epl.expression.dot.ExprDotEvalVisitor;
import com.espertech.esper.epl.rettype.EPType;
import com.espertech.esper.epl.rettype.EPTypeHelper;

public class ExprDotEvalUnpackBean implements ExprDotEval {

    private final EPType returnType;

    public ExprDotEvalUnpackBean(EventType lambdaType) {
        returnType = EPTypeHelper.singleValue(lambdaType.getUnderlyingType());
    }

    public Object evaluate(Object target, EventBean[] eventsPerStream, boolean isNewData, ExprEvaluatorContext exprEvaluatorContext) {
        if (target == null) {
            return null;
        }
        EventBean theEvent = (EventBean) target;
        return theEvent.getUnderlying();
    }

    public EPType getTypeInfo() {
        return returnType;
    }

    public void visit(ExprDotEvalVisitor visitor) {
        visitor.visitUnderlyingEvent();
    }
}
