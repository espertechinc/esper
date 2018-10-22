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
package com.espertech.esper.common.internal.epl.expression.time.eval;

import com.espertech.esper.common.client.EventBean;
import com.espertech.esper.common.internal.epl.expression.core.ExprEvaluatorContext;
import com.espertech.esper.common.internal.schedule.TimeProvider;

public class TimePeriodComputeConstGivenDeltaEval implements TimePeriodCompute, TimePeriodProvide {
    private final long timeDelta;

    public TimePeriodComputeConstGivenDeltaEval(long timeDelta) {
        this.timeDelta = timeDelta;
    }

    public long deltaAdd(long fromTime, EventBean[] eventsPerStream, boolean isNewData, ExprEvaluatorContext context) {
        return timeDelta;
    }

    public long deltaSubtract(long fromTime, EventBean[] eventsPerStream, boolean isNewData, ExprEvaluatorContext context) {
        return timeDelta;
    }

    public TimePeriodDeltaResult deltaAddWReference(long fromTime, long reference, EventBean[] eventsPerStream, boolean isNewData, ExprEvaluatorContext context) {
        return new TimePeriodDeltaResult(TimePeriodUtil.deltaAddWReference(fromTime, reference, timeDelta), reference);
    }

    public long deltaUseRuntimeTime(EventBean[] eventsPerStream, ExprEvaluatorContext context, TimeProvider timeProvider) {
        return timeDelta;
    }

    public TimePeriodProvide getNonVariableProvide(ExprEvaluatorContext context) {
        return this;
    }
}
