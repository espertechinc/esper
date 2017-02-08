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
import com.espertech.esper.client.EventPropertyGetterMapped;
import com.espertech.esper.epl.expression.core.ExprEvaluator;
import com.espertech.esper.epl.expression.core.ExprEvaluatorContext;

public class PropertyExprEvaluatorNonLambdaMapped implements ExprEvaluator {

    private final int streamId;
    private final EventPropertyGetterMapped mappedGetter;
    private final ExprEvaluator paramEval;
    private final Class returnType;

    public PropertyExprEvaluatorNonLambdaMapped(int streamId, EventPropertyGetterMapped mappedGetter, ExprEvaluator paramEval, Class returnType) {
        this.streamId = streamId;
        this.mappedGetter = mappedGetter;
        this.paramEval = paramEval;
        this.returnType = returnType;
    }

    public Object evaluate(EventBean[] eventsPerStream, boolean isNewData, ExprEvaluatorContext context) {
        String key = (String) paramEval.evaluate(eventsPerStream, isNewData, context);
        EventBean eventInQuestion = eventsPerStream[streamId];
        if (eventInQuestion == null) {
            return null;
        }
        return mappedGetter.get(eventInQuestion, key);
    }

    public Class getType() {
        return returnType;
    }

}
