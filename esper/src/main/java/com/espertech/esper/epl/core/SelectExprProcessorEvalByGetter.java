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
package com.espertech.esper.epl.core;

import com.espertech.esper.client.EventBean;
import com.espertech.esper.client.EventPropertyGetter;
import com.espertech.esper.epl.expression.core.ExprEvaluator;
import com.espertech.esper.epl.expression.core.ExprEvaluatorContext;

public class SelectExprProcessorEvalByGetter implements ExprEvaluator {
    private final int streamNum;
    private final EventPropertyGetter getter;
    private final Class returnType;

    public SelectExprProcessorEvalByGetter(int streamNum, EventPropertyGetter getter, Class returnType) {
        this.streamNum = streamNum;
        this.getter = getter;
        this.returnType = returnType;
    }

    public Object evaluate(EventBean[] eventsPerStream, boolean isNewData, ExprEvaluatorContext exprEvaluatorContext) {
        EventBean streamEvent = eventsPerStream[streamNum];
        if (streamEvent == null) {
            return null;
        }
        return getter.get(streamEvent);
    }

    public Class getType() {
        return returnType;
    }
}
