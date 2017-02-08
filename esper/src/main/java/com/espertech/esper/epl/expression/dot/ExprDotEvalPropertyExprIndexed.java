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
import com.espertech.esper.client.EventPropertyGetterIndexed;
import com.espertech.esper.epl.expression.core.ExprEvaluator;
import com.espertech.esper.epl.expression.core.ExprEvaluatorContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ExprDotEvalPropertyExprIndexed extends ExprDotEvalPropertyExprBase {
    private static final Logger log = LoggerFactory.getLogger(ExprDotEvalPropertyExprIndexed.class);

    private final EventPropertyGetterIndexed indexedGetter;

    public ExprDotEvalPropertyExprIndexed(String statementName, String propertyName, int streamNum, ExprEvaluator exprEvaluator, Class propertyType, EventPropertyGetterIndexed indexedGetter) {
        super(statementName, propertyName, streamNum, exprEvaluator, propertyType);
        this.indexedGetter = indexedGetter;
    }

    public Object evaluate(EventBean[] eventsPerStream, boolean isNewData, ExprEvaluatorContext context) {
        EventBean eventInQuestion = eventsPerStream[super.streamNum];
        if (eventInQuestion == null) {
            return null;
        }
        Object index = exprEvaluator.evaluate(eventsPerStream, isNewData, context);
        if (index == null || (!(index instanceof Integer))) {
            log.warn(super.getWarningText("integer", index));
            return null;
        }
        return indexedGetter.get(eventInQuestion, (Integer) index);
    }
}
