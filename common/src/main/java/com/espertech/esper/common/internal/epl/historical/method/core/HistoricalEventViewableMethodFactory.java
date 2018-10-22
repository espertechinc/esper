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
package com.espertech.esper.common.internal.epl.historical.method.core;

import com.espertech.esper.common.internal.context.aifactory.core.ModuleIncidentals;
import com.espertech.esper.common.internal.context.util.AgentInstanceContext;
import com.espertech.esper.common.internal.context.util.StatementContext;
import com.espertech.esper.common.internal.epl.historical.common.HistoricalEventViewable;
import com.espertech.esper.common.internal.epl.historical.common.HistoricalEventViewableFactoryBase;
import com.espertech.esper.common.internal.epl.historical.method.poll.MethodConversionStrategy;
import com.espertech.esper.common.internal.epl.historical.method.poll.MethodTargetStrategyFactory;

public class HistoricalEventViewableMethodFactory extends HistoricalEventViewableFactoryBase {
    private String configurationName;
    private MethodTargetStrategyFactory targetStrategy;
    private MethodConversionStrategy conversionStrategy;

    public void ready(StatementContext statementContext, ModuleIncidentals moduleIncidentals, boolean recovery) {
    }

    public HistoricalEventViewable activate(AgentInstanceContext agentInstanceContext) {
        PollExecStrategyMethod strategy = new PollExecStrategyMethod(targetStrategy.make(agentInstanceContext), conversionStrategy);
        return new HistoricalEventViewableMethod(this, strategy, agentInstanceContext);
    }

    public String getConfigurationName() {
        return configurationName;
    }

    public void setConfigurationName(String configurationName) {
        this.configurationName = configurationName;
    }

    public void setTargetStrategy(MethodTargetStrategyFactory targetStrategy) {
        this.targetStrategy = targetStrategy;
    }

    public void setConversionStrategy(MethodConversionStrategy conversionStrategy) {
        this.conversionStrategy = conversionStrategy;
    }
}
