/**************************************************************************************
 * Copyright (C) 2006-2015 EsperTech Inc. All rights reserved.                        *
 * http://www.espertech.com/esper                                                          *
 * http://www.espertech.com                                                           *
 * ---------------------------------------------------------------------------------- *
 * The software in this package is published under the terms of the GPL license       *
 * a copy of which has been included with this distribution in the license.txt file.  *
 **************************************************************************************/
package com.espertech.esper.epl.expression.ops;

import com.espertech.esper.client.EventBean;
import com.espertech.esper.epl.expression.core.ExprEvaluator;
import com.espertech.esper.epl.expression.core.ExprEvaluatorContext;

public class ExprConcatNodeEvalThreadLocal implements ExprEvaluator
{
    private final ExprConcatNode parent;
    private final ExprEvaluator[] evaluators;

    private ThreadLocal<StringBuffer> localBuffer = new ThreadLocal<StringBuffer>() {
        @Override
        protected StringBuffer initialValue() {
            return new StringBuffer();
        }
    };

    public ExprConcatNodeEvalThreadLocal(ExprConcatNode parent, ExprEvaluator[] evaluators) {
        this.parent = parent;
        this.evaluators = evaluators;
    }

    public Object evaluate(EventBean[] eventsPerStream, boolean isNewData, ExprEvaluatorContext context) {
        StringBuffer buffer = localBuffer.get();
        buffer.delete(0, buffer.length());
        return ExprConcatNodeEvalWNew.evaluate(eventsPerStream, isNewData, context, buffer, evaluators, parent);
    }

    public Class getType() {
        return String.class;
    }
}
