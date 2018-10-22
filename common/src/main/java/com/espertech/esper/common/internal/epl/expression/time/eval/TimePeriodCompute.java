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

public interface TimePeriodCompute {
    long deltaAdd(long fromTime, EventBean[] eventsPerStream, boolean isNewData, ExprEvaluatorContext context);

    long deltaSubtract(long fromTime, EventBean[] eventsPerStream, boolean isNewData, ExprEvaluatorContext context);

    TimePeriodDeltaResult deltaAddWReference(long fromTime, long reference, EventBean[] eventsPerStream, boolean isNewData, ExprEvaluatorContext context);

    long deltaUseRuntimeTime(EventBean[] eventsPerStream, ExprEvaluatorContext context, TimeProvider timeProvider);

    TimePeriodProvide getNonVariableProvide(ExprEvaluatorContext context);
}
