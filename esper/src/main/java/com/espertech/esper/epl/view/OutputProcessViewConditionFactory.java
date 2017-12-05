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
package com.espertech.esper.epl.view;

import com.espertech.esper.client.EventType;
import com.espertech.esper.core.context.util.AgentInstanceContext;
import com.espertech.esper.core.service.StatementContext;
import com.espertech.esper.epl.core.resultset.core.ResultSetProcessor;
import com.espertech.esper.epl.core.resultset.core.ResultSetProcessorOutputConditionType;
import com.espertech.esper.epl.core.resultset.core.ResultSetProcessorHelperFactory;
import com.espertech.esper.epl.expression.time.ExprTimePeriod;
import com.espertech.esper.epl.spec.OutputLimitLimitType;
import com.espertech.esper.epl.spec.SelectClauseStreamSelectorEnum;

/**
 * A view that handles the "output snapshot" keyword in output rate stabilizing.
 */
public class OutputProcessViewConditionFactory extends OutputProcessViewDirectDistinctOrAfterFactory {
    private final OutputConditionFactory outputConditionFactory;
    private final int streamCount;
    private final ResultSetProcessorOutputConditionType conditionType;
    private final OutputLimitLimitType outputLimitLimitType;
    private final boolean terminable;
    private final boolean hasAfter;
    private final boolean isUnaggregatedUngrouped;
    private final SelectClauseStreamSelectorEnum selectClauseStreamSelectorEnum;
    private final ResultSetProcessorHelperFactory resultSetProcessorHelperFactory;

    public OutputProcessViewConditionFactory(StatementContext statementContext, OutputStrategyPostProcessFactory postProcessFactory, boolean distinct, ExprTimePeriod afterTimePeriod, Integer afterConditionNumberOfEvents, EventType resultEventType, OutputConditionFactory outputConditionFactory, int streamCount, ResultSetProcessorOutputConditionType conditionType, OutputLimitLimitType outputLimitLimitType, boolean terminable, boolean hasAfter, boolean isUnaggregatedUngrouped, SelectClauseStreamSelectorEnum selectClauseStreamSelectorEnum, ResultSetProcessorHelperFactory resultSetProcessorHelperFactory) {
        super(statementContext, postProcessFactory, resultSetProcessorHelperFactory, distinct, afterTimePeriod, afterConditionNumberOfEvents, resultEventType);
        this.outputConditionFactory = outputConditionFactory;
        this.streamCount = streamCount;
        this.conditionType = conditionType;
        this.outputLimitLimitType = outputLimitLimitType;
        this.terminable = terminable;
        this.hasAfter = hasAfter;
        this.isUnaggregatedUngrouped = isUnaggregatedUngrouped;
        this.selectClauseStreamSelectorEnum = selectClauseStreamSelectorEnum;
        this.resultSetProcessorHelperFactory = resultSetProcessorHelperFactory;
    }

    @Override
    public OutputProcessViewBase makeView(ResultSetProcessor resultSetProcessor, AgentInstanceContext agentInstanceContext) {

        // determine after-stuff
        boolean isAfterConditionSatisfied = true;
        Long afterConditionTime = null;
        if (afterConditionNumberOfEvents != null) {
            isAfterConditionSatisfied = false;
        } else if (afterTimePeriod != null) {
            isAfterConditionSatisfied = false;
            long delta = afterTimePeriod.nonconstEvaluator().deltaUseEngineTime(null, agentInstanceContext, agentInstanceContext.getTimeProvider());
            afterConditionTime = agentInstanceContext.getStatementContext().getTimeProvider().getTime() + delta;
        }

        if (conditionType == ResultSetProcessorOutputConditionType.SNAPSHOT) {
            if (super.postProcessFactory == null) {
                return new OutputProcessViewConditionSnapshot(resultSetProcessorHelperFactory, resultSetProcessor, afterConditionTime, afterConditionNumberOfEvents, isAfterConditionSatisfied, this, agentInstanceContext);
            }
            OutputStrategyPostProcess postProcess = postProcessFactory.make(agentInstanceContext);
            return new OutputProcessViewConditionSnapshotPostProcess(resultSetProcessorHelperFactory, resultSetProcessor, afterConditionTime, afterConditionNumberOfEvents, isAfterConditionSatisfied, this, agentInstanceContext, postProcess);
        } else if (conditionType == ResultSetProcessorOutputConditionType.POLICY_FIRST) {
            if (super.postProcessFactory == null) {
                return new OutputProcessViewConditionFirst(resultSetProcessorHelperFactory, resultSetProcessor, afterConditionTime, afterConditionNumberOfEvents, isAfterConditionSatisfied, this, agentInstanceContext);
            }
            OutputStrategyPostProcess postProcess = postProcessFactory.make(agentInstanceContext);
            return new OutputProcessViewConditionFirstPostProcess(resultSetProcessorHelperFactory, resultSetProcessor, afterConditionTime, afterConditionNumberOfEvents, isAfterConditionSatisfied, this, agentInstanceContext, postProcess);
        } else if (conditionType == ResultSetProcessorOutputConditionType.POLICY_LASTALL_UNORDERED) {
            if (super.postProcessFactory == null) {
                return new OutputProcessViewConditionLastAllUnord(resultSetProcessorHelperFactory, resultSetProcessor, afterConditionTime, afterConditionNumberOfEvents, isAfterConditionSatisfied, this, agentInstanceContext);
            }
            OutputStrategyPostProcess postProcess = postProcessFactory.make(agentInstanceContext);
            return new OutputProcessViewConditionLastAllUnordPostProcessAll(resultSetProcessorHelperFactory, resultSetProcessor, afterConditionTime, afterConditionNumberOfEvents, isAfterConditionSatisfied, this, agentInstanceContext, postProcess);
        } else {
            if (super.postProcessFactory == null) {
                return new OutputProcessViewConditionDefault(resultSetProcessorHelperFactory, resultSetProcessor, afterConditionTime, afterConditionNumberOfEvents, isAfterConditionSatisfied, this, agentInstanceContext, streamCount > 1);
            }
            OutputStrategyPostProcess postProcess = postProcessFactory.make(agentInstanceContext);
            return new OutputProcessViewConditionDefaultPostProcess(resultSetProcessor, afterConditionTime, afterConditionNumberOfEvents, isAfterConditionSatisfied, this, agentInstanceContext, postProcess, streamCount > 1, resultSetProcessorHelperFactory);
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

    public boolean isHasAfter() {
        return hasAfter;
    }

    public boolean isUnaggregatedUngrouped() {
        return isUnaggregatedUngrouped;
    }

    public SelectClauseStreamSelectorEnum getSelectClauseStreamSelectorEnum() {
        return selectClauseStreamSelectorEnum;
    }
}
