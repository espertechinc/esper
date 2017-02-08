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
package com.espertech.esper.epl.expression.core;

import com.espertech.esper.client.EventBean;

public class ExprNodeUtilExprEvalStreamNumUnd implements ExprEvaluator {
    private final int streamNum;
    private final Class returnType;

    public ExprNodeUtilExprEvalStreamNumUnd(int streamNum, Class returnType) {
        this.streamNum = streamNum;
        this.returnType = returnType;
    }

    public Object evaluate(EventBean[] eventsPerStream, boolean isNewData, ExprEvaluatorContext context) {
        return eventsPerStream[streamNum].getUnderlying();
    }

    public Class getType() {
        return returnType;
    }

}
