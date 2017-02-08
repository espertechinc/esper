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
import com.espertech.esper.client.EventPropertyGetterMapped;
import com.espertech.esper.epl.expression.core.ExprEvaluator;
import com.espertech.esper.epl.expression.core.ExprEvaluatorContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ExprDotEvalPropertyExprMapped extends ExprDotEvalPropertyExprBase {
    private static final Logger log = LoggerFactory.getLogger(ExprDotEvalPropertyExprMapped.class);

    private final EventPropertyGetterMapped mappedGetter;

    public ExprDotEvalPropertyExprMapped(String statementName, String propertyName, int streamNum, ExprEvaluator exprEvaluator, Class propertyType, EventPropertyGetterMapped mappedGetter) {
        super(statementName, propertyName, streamNum, exprEvaluator, propertyType);
        this.mappedGetter = mappedGetter;
    }

    public Object evaluate(EventBean[] eventsPerStream, boolean isNewData, ExprEvaluatorContext context) {
        EventBean eventInQuestion = eventsPerStream[super.streamNum];
        if (eventInQuestion == null) {
            return null;
        }
        Object result = exprEvaluator.evaluate(eventsPerStream, isNewData, context);
        if (result != null && (!(result instanceof String))) {
            log.warn(super.getWarningText("string", result));
            return null;
        }
        return mappedGetter.get(eventInQuestion, (String) result);
    }
}
