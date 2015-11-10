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
import com.espertech.esper.epl.view.OutputConditionTimeFactory;
import com.espertech.esper.epl.view.OutputProcessViewConditionDeltaSet;
import com.espertech.esper.epl.view.OutputProcessViewConditionDeltaSetImpl;

public class ResultSetProcessorHelperFactoryImpl implements ResultSetProcessorHelperFactory {
    public ResultSetProcessorSimpleOutputLastHelper makeSimpleAndLast(ResultSetProcessorSimpleFactory prototype, ResultSetProcessorSimple simple, AgentInstanceContext agentInstanceContext) {
        return new ResultSetProcessorSimpleOutputLastHelperImpl(simple);
    }

    public OutputProcessViewConditionDeltaSet makeOutputConditionChangeSet(boolean isJoin, AgentInstanceContext agentInstanceContext) {
        return new OutputProcessViewConditionDeltaSetImpl(isJoin);
    }

    public OutputConditionFactory makeOutputConditionTime(ExprTimePeriod timePeriodExpr, boolean isStartConditionOnCreation) {
        return new OutputConditionTimeFactory(timePeriodExpr, isStartConditionOnCreation);
    }
}
