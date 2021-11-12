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

import com.espertech.esper.common.client.EPException;
import com.espertech.esper.common.client.EventBean;
import com.espertech.esper.common.internal.epl.expression.core.ExprEvaluator;
import com.espertech.esper.common.internal.epl.expression.core.ExprEvaluatorContext;
import com.espertech.esper.common.internal.epl.historical.database.core.PollExecStrategyDBQuery;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Collection;

public class FireAndForgetProcessorDBExecPrepared extends FireAndForgetProcessorDBExecBase {
    private static final Logger log = LoggerFactory.getLogger(FireAndForgetProcessorDBExecPrepared.class);

    public FireAndForgetProcessorDBExecPrepared(PollExecStrategyDBQuery poll, ExprEvaluator lookupValuesEval) {
        super(poll, lookupValuesEval);
        poll.start();
    }

    public Collection<EventBean> performQuery(ExprEvaluatorContext exprEvaluatorContext) {
        if (poll == null) {
            throw new EPException("Prepared fire-and-forget query is already closed");
        }
        return doPoll(exprEvaluatorContext);
    }

    public void close() {
        if (poll != null) {
            try {
                poll.done();
            } catch (Throwable t) {
                log.error("Failed to return database poll resources: " + t.getMessage(), t);
            }
            try {
                poll.destroy();
            } catch (Throwable t) {
                log.error("Failed to destroy database poll resources: " + t.getMessage(), t);
            }
            poll = null;
        }
    }
}
