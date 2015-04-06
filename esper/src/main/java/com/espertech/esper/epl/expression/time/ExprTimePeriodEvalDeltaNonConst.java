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

package com.espertech.esper.epl.expression.time;

import com.espertech.esper.client.EventBean;
import com.espertech.esper.core.context.util.AgentInstanceContext;
import com.espertech.esper.epl.expression.core.ExprEvaluatorContext;

public interface ExprTimePeriodEvalDeltaNonConst
{
    public long deltaMillisecondsAdd(long currentTime, EventBean[] eventsPerStream, boolean isNewData, ExprEvaluatorContext context);
    public long deltaMillisecondsSubtract(long currentTime, EventBean[] eventsPerStream, boolean isNewData, ExprEvaluatorContext context);
    public long deltaMillisecondsUseEngineTime(EventBean[] eventsPerStream, AgentInstanceContext agentInstanceContext);
    public ExprTimePeriodEvalDeltaResult deltaMillisecondsAddWReference(long current, long reference, EventBean[] eventsPerStream, boolean isNewData, ExprEvaluatorContext context);
}
