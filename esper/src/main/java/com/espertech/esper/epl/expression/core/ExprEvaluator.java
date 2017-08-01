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

/**
 * Interface for evaluating of an event tuple.
 */
public interface ExprEvaluator {
    /**
     * Evaluate event tuple and return result.
     *
     * @param eventsPerStream - event tuple
     * @param isNewData       - indicates whether we are dealing with new data (istream) or old data (rstream)
     * @param context         context for expression evaluation
     * @return evaluation result, a boolean value for OR/AND-type evalution nodes.
     */
    public Object evaluate(EventBean[] eventsPerStream, boolean isNewData, ExprEvaluatorContext context);
}
