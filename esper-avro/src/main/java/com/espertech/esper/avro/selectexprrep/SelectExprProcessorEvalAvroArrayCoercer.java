/*
 * *************************************************************************************
 *  Copyright (C) 2006-2015 EsperTech, Inc. All rights reserved.                       *
 *  http://www.espertech.com/esper                                                     *
 *  http://www.espertech.com                                                           *
 *  ---------------------------------------------------------------------------------- *
 *  The software in this package is published under the terms of the GPL license       *
 *  a copy of which has been included with this distribution in the license.txt file.  *
 * *************************************************************************************
 */

package com.espertech.esper.avro.selectexprrep;

import com.espertech.esper.client.EventBean;
import com.espertech.esper.epl.expression.core.ExprEvaluator;
import com.espertech.esper.epl.expression.core.ExprEvaluatorContext;
import com.espertech.esper.util.TypeWidener;

import java.util.Collection;

public class SelectExprProcessorEvalAvroArrayCoercer implements ExprEvaluator {
    private final ExprEvaluator eval;
    private final TypeWidener widener;

    public SelectExprProcessorEvalAvroArrayCoercer(ExprEvaluator eval, TypeWidener widener) {
        this.eval = eval;
        this.widener = widener;
    }

    public Object evaluate(EventBean[] eventsPerStream, boolean isNewData, ExprEvaluatorContext context) {
        Object result = eval.evaluate(eventsPerStream, isNewData, context);
        return widener.widen(result);
    }

    public Class getType() {
        return Collection.class;
    }
}
