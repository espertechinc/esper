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
import com.espertech.esper.epl.rettype.EPTypeHelper;
import com.espertech.esper.metrics.instrumentation.InstrumentationHelper;

public class ExprDotEvalStreamMethod implements ExprEvaluator {
    private final ExprDotNode dotNode;
    private final int streamNumber;
    private final ExprDotEval[] evaluators;

    public ExprDotEvalStreamMethod(ExprDotNode dotNode, int streamNumber, ExprDotEval[] evaluators) {
        this.dotNode = dotNode;
        this.streamNumber = streamNumber;
        this.evaluators = evaluators;
    }

    public Class getType() {
        return EPTypeHelper.getNormalizedClass(evaluators[evaluators.length - 1].getTypeInfo());
    }

    public Object evaluate(EventBean[] eventsPerStream, boolean isNewData, ExprEvaluatorContext exprEvaluatorContext) {
        if (InstrumentationHelper.ENABLED) {
            InstrumentationHelper.get().qExprStreamUndMethod(dotNode);
        }

        // get underlying event
        EventBean theEvent = eventsPerStream[streamNumber];
        if (theEvent == null) {
            if (InstrumentationHelper.ENABLED) {
                InstrumentationHelper.get().aExprStreamUndMethod(null);
            }
            return null;
        }
        Object inner = theEvent.getUnderlying();

        if (InstrumentationHelper.ENABLED) {
            InstrumentationHelper.get().qExprDotChain(EPTypeHelper.singleValue(theEvent.getEventType().getUnderlyingType()), inner, evaluators);
        }
        inner = ExprDotNodeUtility.evaluateChain(evaluators, inner, eventsPerStream, isNewData, exprEvaluatorContext);
        if (InstrumentationHelper.ENABLED) {
            InstrumentationHelper.get().aExprDotChain();
        }

        if (InstrumentationHelper.ENABLED) {
            InstrumentationHelper.get().aExprStreamUndMethod(inner);
        }
        return inner;
    }
}
