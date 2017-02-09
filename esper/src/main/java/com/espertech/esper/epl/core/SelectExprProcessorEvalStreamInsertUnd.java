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
import com.espertech.esper.epl.expression.core.ExprEvaluator;
import com.espertech.esper.epl.expression.core.ExprEvaluatorContext;
import com.espertech.esper.epl.expression.core.ExprStreamUnderlyingNode;
import com.espertech.esper.metrics.instrumentation.InstrumentationHelper;

public class SelectExprProcessorEvalStreamInsertUnd implements ExprEvaluator {
    private final ExprStreamUnderlyingNode undNode;
    private final int streamNum;
    private final Class returnType;

    public SelectExprProcessorEvalStreamInsertUnd(ExprStreamUnderlyingNode undNode, int streamNum, Class returnType) {
        this.undNode = undNode;
        this.streamNum = streamNum;
        this.returnType = returnType;
    }

    public Object evaluate(EventBean[] eventsPerStream, boolean isNewData, ExprEvaluatorContext exprEvaluatorContext) {
        if (InstrumentationHelper.ENABLED) {
            InstrumentationHelper.get().qExprStreamUndSelectClause(undNode);
            EventBean event = eventsPerStream == null ? null : eventsPerStream[streamNum];
            InstrumentationHelper.get().aExprStreamUndSelectClause(event);
            return event;
        }
        return eventsPerStream == null ? null : eventsPerStream[streamNum];
    }

    public Class getType() {
        return returnType;
    }

    public int getStreamNum() {
        return streamNum;
    }
}
