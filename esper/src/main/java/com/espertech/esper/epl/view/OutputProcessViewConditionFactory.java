/**************************************************************************************
 * Copyright (C) 2006-2015 EsperTech Inc. All rights reserved.                        *
 * http://www.espertech.com/esper                                                          *
 * http://www.espertech.com                                                           *
 * ---------------------------------------------------------------------------------- *
 * The software in this package is published under the terms of the GPL license       *
 * a copy of which has been included with this distribution in the license.txt file.  *
 **************************************************************************************/
package com.espertech.esper.epl.view;

import com.espertech.esper.client.EventType;
import com.espertech.esper.core.context.util.AgentInstanceContext;
import com.espertech.esper.core.service.StatementContext;
import com.espertech.esper.epl.core.ResultSetProcessor;
import com.espertech.esper.epl.expression.time.ExprTimePeriod;
import com.espertech.esper.epl.spec.OutputLimitLimitType;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

/**
 * A view that handles the "output snapshot" keyword in output rate stabilizing.
 */
public class OutputProcessViewConditionFactory extends OutputProcessViewDirectDistinctOrAfterFactory
{
    private static final Log log = LogFactory.getLog(OutputProcessViewConditionFactory.class);

    private final OutputConditionFactory outputConditionFactory;
    private final int streamCount;
    private final ConditionType conditionType;
    private final OutputLimitLimitType outputLimitLimitType;
    private final boolean terminable;

    public OutputProcessViewConditionFactory(StatementContext statementContext, OutputStrategyPostProcessFactory postProcessFactory, boolean distinct, ExprTimePeriod afterTimePeriod, Integer afterConditionNumberOfEvents, EventType resultEventType, OutputConditionFactory outputConditionFactory, int streamCount, ConditionType conditionType, OutputLimitLimitType outputLimitLimitType, boolean terminable) {
        super(statementContext, postProcessFactory, distinct, afterTimePeriod, afterConditionNumberOfEvents, resultEventType);
        this.outputConditionFactory = outputConditionFactory;
        this.streamCount = streamCount;
        this.conditionType = conditionType;
        this.outputLimitLimitType = outputLimitLimitType;
        this.terminable = terminable;
    }

    @Override
    public OutputProcessViewBase makeView(ResultSetProcessor resultSetProcessor, AgentInstanceContext agentInstanceContext) {

        // determine after-stuff
        boolean isAfterConditionSatisfied = true;
        Long afterConditionTime = null;
        if (afterConditionNumberOfEvents != null)
        {
            isAfterConditionSatisfied = false;
        }
        else if (afterTimePeriod != null)
        {
            isAfterConditionSatisfied = false;
            long delta = afterTimePeriod.nonconstEvaluator().deltaMillisecondsUseEngineTime(null, agentInstanceContext);
            afterConditionTime = agentInstanceContext.getStatementContext().getTimeProvider().getTime() + delta;
        }

        if (conditionType == ConditionType.SNAPSHOT) {
            if (super.postProcessFactory == null) {
                return new OutputProcessViewConditionSnapshot(resultSetProcessor, afterConditionTime, afterConditionNumberOfEvents, isAfterConditionSatisfied, this, agentInstanceContext);
            }
            OutputStrategyPostProcess postProcess = postProcessFactory.make(agentInstanceContext);
            return new OutputProcessViewConditionSnapshotPostProcess(resultSetProcessor, afterConditionTime, afterConditionNumberOfEvents, isAfterConditionSatisfied, this, agentInstanceContext, postProcess);
        }
        if (conditionType == ConditionType.POLICY_FIRST) {
            if (super.postProcessFactory == null) {
                return new OutputProcessViewConditionFirst(resultSetProcessor, afterConditionTime, afterConditionNumberOfEvents, isAfterConditionSatisfied, this, agentInstanceContext);
            }
            OutputStrategyPostProcess postProcess = postProcessFactory.make(agentInstanceContext);
            return new OutputProcessViewConditionFirstPostProcess(resultSetProcessor, afterConditionTime, afterConditionNumberOfEvents, isAfterConditionSatisfied, this, agentInstanceContext, postProcess);
        }
        else {
            if (super.postProcessFactory == null) {
                return new OutputProcessViewConditionDefault(resultSetProcessor, afterConditionTime, afterConditionNumberOfEvents, isAfterConditionSatisfied, this, agentInstanceContext);
            }
            OutputStrategyPostProcess postProcess = postProcessFactory.make(agentInstanceContext);
            return new OutputProcessViewConditionDefaultPostProcess(resultSetProcessor, afterConditionTime, afterConditionNumberOfEvents, isAfterConditionSatisfied, this, agentInstanceContext, postProcess);
        }
    }

    public OutputConditionFactory getOutputConditionFactory() {
        return outputConditionFactory;
    }

    public int getStreamCount() {
        return streamCount;
    }

    public OutputLimitLimitType getOutputLimitLimitType() {
        return outputLimitLimitType;
    }

    public boolean isTerminable() {
        return terminable;
    }

    public static enum ConditionType {
        SNAPSHOT,
        POLICY_FIRST,
        POLICY_NONFIRST
    }
}
