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
package com.espertech.esper.common.internal.epl.fafquery.processor;

import com.espertech.esper.common.client.EventBean;
import com.espertech.esper.common.internal.epl.expression.core.ExprEvaluator;
import com.espertech.esper.common.internal.epl.expression.core.ExprEvaluatorContext;
import com.espertech.esper.common.internal.epl.historical.database.core.PollExecStrategyDBQuery;
import com.espertech.esper.common.internal.util.CollectionUtil;

import java.util.Collection;

public abstract class FireAndForgetProcessorDBExecBase {
    protected PollExecStrategyDBQuery poll;
    private final ExprEvaluator lookupValuesEval;

    public FireAndForgetProcessorDBExecBase(PollExecStrategyDBQuery poll, ExprEvaluator lookupValuesEval) {
        this.poll = poll;
        this.lookupValuesEval = lookupValuesEval;
    }

    protected final Collection<EventBean> doPoll(ExprEvaluatorContext exprEvaluatorContext) {
        exprEvaluatorContext.getVariableManagementService().setLocalVersion();
        Object lookupValues = null;
        if (lookupValuesEval != null) {
            lookupValues = lookupValuesEval.evaluate(CollectionUtil.EVENTBEANARRAY_EMPTY, true, exprEvaluatorContext);
        }
        return poll.poll(lookupValues, exprEvaluatorContext);
    }
}
