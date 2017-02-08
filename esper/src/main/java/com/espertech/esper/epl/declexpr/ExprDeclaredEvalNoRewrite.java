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

public class ExprDeclaredEvalNoRewrite extends ExprDeclaredEvalBase {

    public ExprDeclaredEvalNoRewrite(ExprEvaluator innerEvaluator, ExpressionDeclItem prototype, boolean isCache) {
        super(innerEvaluator, prototype, isCache);
    }

    public EventBean[] getEventsPerStreamRewritten(EventBean[] eventsPerStream) {
        return eventsPerStream;
    }
}