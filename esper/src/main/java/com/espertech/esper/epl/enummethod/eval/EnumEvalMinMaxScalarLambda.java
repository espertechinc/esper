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
package com.espertech.esper.epl.enummethod.eval;

import com.espertech.esper.client.EventBean;
import com.espertech.esper.epl.expression.core.ExprEvaluator;
import com.espertech.esper.epl.expression.core.ExprEvaluatorContext;
import com.espertech.esper.event.arr.ObjectArrayEventBean;
import com.espertech.esper.event.arr.ObjectArrayEventType;

import java.util.Collection;

public class EnumEvalMinMaxScalarLambda extends EnumEvalBase implements EnumEval {

    private final boolean max;
    private final ObjectArrayEventType resultEventType;

    public EnumEvalMinMaxScalarLambda(ExprEvaluator innerExpression, int streamCountIncoming, boolean max, ObjectArrayEventType resultEventType) {
        super(innerExpression, streamCountIncoming);
        this.max = max;
        this.resultEventType = resultEventType;
    }

    public Object evaluateEnumMethod(EventBean[] eventsLambda, Collection target, boolean isNewData, ExprEvaluatorContext context) {
        Comparable minKey = null;

        ObjectArrayEventBean resultEvent = new ObjectArrayEventBean(new Object[1], resultEventType);
        Collection<Object> coll = (Collection<Object>) target;
        for (Object next : coll) {

            resultEvent.getProperties()[0] = next;
            eventsLambda[streamNumLambda] = resultEvent;

            Object comparable = innerExpression.evaluate(eventsLambda, isNewData, context);
            if (comparable == null) {
                continue;
            }

            if (minKey == null) {
                minKey = (Comparable) comparable;
            } else {
                if (max) {
                    if (minKey.compareTo(comparable) < 0) {
                        minKey = (Comparable) comparable;
                    }
                } else {
                    if (minKey.compareTo(comparable) > 0) {
                        minKey = (Comparable) comparable;
                    }
                }
            }
        }

        return minKey;
    }
}
