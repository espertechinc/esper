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
import com.espertech.esper.epl.expression.core.ExprEvaluator;
import com.espertech.esper.epl.expression.core.ExprEvaluatorContext;
import net.sf.cglib.reflect.FastMethod;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ExprDotMethodEvalNoDuckUnderlying extends ExprDotMethodEvalNoDuck {
    private static final Logger log = LoggerFactory.getLogger(ExprDotMethodEvalNoDuckUnderlying.class);

    public ExprDotMethodEvalNoDuckUnderlying(String statementName, FastMethod method, ExprEvaluator[] parameters) {
        super(statementName, method, parameters);
    }

    public Object evaluate(Object target, EventBean[] eventsPerStream, boolean isNewData, ExprEvaluatorContext exprEvaluatorContext) {
        if (target == null) {
            return null;
        }
        if (!(target instanceof EventBean)) {
            log.warn("Expected EventBean return value but received '" + target.getClass().getName() + "' for statement " + super.statementName);
            return null;
        }
        EventBean bean = (EventBean) target;
        return super.evaluate(bean.getUnderlying(), eventsPerStream, isNewData, exprEvaluatorContext);
    }
}
