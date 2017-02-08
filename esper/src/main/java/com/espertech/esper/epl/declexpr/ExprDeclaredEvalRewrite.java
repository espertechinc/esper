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
import com.espertech.esper.epl.spec.ExpressionDeclItem;

public class ExprDeclaredEvalRewrite extends ExprDeclaredEvalBase {
    private final int[] streamAssignments;

    public ExprDeclaredEvalRewrite(ExprEvaluator innerEvaluator, ExpressionDeclItem prototype, boolean isCache, int[] streamAssignments) {
        super(innerEvaluator, prototype, isCache);
        this.streamAssignments = streamAssignments;
    }

    public EventBean[] getEventsPerStreamRewritten(EventBean[] eventsPerStream) {

        // rewrite streams
        EventBean[] events = new EventBean[streamAssignments.length];
        for (int i = 0; i < streamAssignments.length; i++) {
            events[i] = eventsPerStream[streamAssignments[i]];
        }

        return events;
    }
}