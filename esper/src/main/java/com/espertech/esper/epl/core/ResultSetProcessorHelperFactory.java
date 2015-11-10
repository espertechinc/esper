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

package com.espertech.esper.epl.core;

import com.espertech.esper.core.context.util.AgentInstanceContext;
import com.espertech.esper.epl.expression.time.ExprTimePeriod;
import com.espertech.esper.epl.view.OutputConditionFactory;
import com.espertech.esper.epl.view.OutputProcessViewConditionDeltaSet;

public interface ResultSetProcessorHelperFactory {
    ResultSetProcessorSimpleOutputLastHelper makeSimpleAndLast(ResultSetProcessorSimpleFactory prototype, ResultSetProcessorSimple simple, AgentInstanceContext agentInstanceContext);
    OutputProcessViewConditionDeltaSet makeOutputConditionChangeSet(boolean isJoin, AgentInstanceContext agentInstanceContext);
    OutputConditionFactory makeOutputConditionTime(ExprTimePeriod timePeriodExpr, boolean isStartConditionOnCreation);
}
