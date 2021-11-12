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

import java.util.Collection;

public class FireAndForgetProcessorDBExecUnprepared extends FireAndForgetProcessorDBExecBase {

    public FireAndForgetProcessorDBExecUnprepared(PollExecStrategyDBQuery poll, ExprEvaluator lookupValuesEval) {
        super(poll, lookupValuesEval);
    }

    public Collection<EventBean> performQuery(ExprEvaluatorContext exprEvaluatorContext) {
        poll.start();
        try {
            return doPoll(exprEvaluatorContext);
        } finally {
            poll.done();
            poll.destroy();
        }
    }
}
