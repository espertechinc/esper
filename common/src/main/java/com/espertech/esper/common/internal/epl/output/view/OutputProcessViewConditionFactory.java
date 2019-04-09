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
package com.espertech.esper.common.internal.epl.output.view;

import com.espertech.esper.common.client.EventType;
import com.espertech.esper.common.internal.compile.stage1.spec.SelectClauseStreamSelectorEnum;
import com.espertech.esper.common.internal.context.util.AgentInstanceContext;
import com.espertech.esper.common.internal.epl.output.condition.OutputConditionFactory;
import com.espertech.esper.common.internal.epl.output.condition.OutputProcessViewConditionDefaultPostProcess;
import com.espertech.esper.common.internal.epl.output.core.OutputProcessView;
import com.espertech.esper.common.internal.epl.output.core.OutputProcessViewConditionLastAllUnordPostProcessAll;
import com.espertech.esper.common.internal.epl.output.core.OutputProcessViewConditionSnapshot;
import com.espertech.esper.common.internal.epl.resultset.core.ResultSetProcessor;
import com.espertech.esper.common.internal.epl.resultset.core.ResultSetProcessorOutputConditionType;

/**
 * A view that handles the "output snapshot" keyword in output rate stabilizing.
 */
public class OutputProcessViewConditionFactory extends OutputProcessViewDirectDistinctOrAfterFactory {
    private final OutputConditionFactory outputConditionFactory;
    private final int streamCount;
    private final ResultSetProcessorOutputConditionType conditionType;
    private final boolean terminable;
    private final boolean hasAfter;
    private final boolean isUnaggregatedUngrouped;
    private final SelectClauseStreamSelectorEnum selectClauseStreamSelectorEnum;
    private final EventType[] eventTypes;

    public OutputProcessViewConditionFactory(OutputProcessViewConditionSpec spec) {
        super(spec.getPostProcessFactory(), spec.isDistinct(), spec.getDistinctKeyGetter(), spec.getAfterTimePeriod(), spec.getAfterConditionNumberOfEvents(), spec.getResultEventType());
        this.outputConditionFactory = spec.getOutputConditionFactory();
        this.streamCount = spec.getStreamCount();
        this.conditionType = spec.getConditionType();
        this.terminable = spec.isTerminable();
        this.hasAfter = spec.isHasAfter();
        this.isUnaggregatedUngrouped = spec.isUnaggregatedUngrouped();
        this.selectClauseStreamSelectorEnum = spec.getSelectClauseStreamSelector();
        this.eventTypes = spec.getEventTypes();
    }

    @Override
    public OutputProcessView makeView(ResultSetProcessor resultSetProcessor, AgentInstanceContext agentInstanceContext) {

        // determine after-stuff
        boolean isAfterConditionSatisfied = true;
        Long afterConditionTime = null;
        if (afterConditionNumberOfEvents != null) {
            isAfterConditionSatisfied = false;
        } else if (afterTimePeriod != null) {
            isAfterConditionSatisfied = false;
            long time = agentInstanceContext.getTimeProvider().getTime();
            long delta = afterTimePeriod.deltaAdd(time, null, true, agentInstanceContext);
            afterConditionTime = time + delta;
        }

        if (conditionType == ResultSetProcessorOutputConditionType.SNAPSHOT) {
            if (super.postProcessFactory == null) {
                return new OutputProcessViewConditionSnapshot(resultSetProcessor, afterConditionTime, afterConditionNumberOfEvents, isAfterConditionSatisfied, this, agentInstanceContext);
            }
            OutputStrategyPostProcess postProcess = postProcessFactory.make(agentInstanceContext);
            return new OutputProcessViewConditionSnapshotPostProcess(resultSetProcessor, afterConditionTime, afterConditionNumberOfEvents, isAfterConditionSatisfied, this, agentInstanceContext, postProcess);
        } else if (conditionType == ResultSetProcessorOutputConditionType.POLICY_FIRST) {
            if (super.postProcessFactory == null) {
                return new OutputProcessViewConditionFirst(resultSetProcessor, afterConditionTime, afterConditionNumberOfEvents, isAfterConditionSatisfied, this, agentInstanceContext);
            }
            OutputStrategyPostProcess postProcess = postProcessFactory.make(agentInstanceContext);
            return new OutputProcessViewConditionFirstPostProcess(resultSetProcessor, afterConditionTime, afterConditionNumberOfEvents, isAfterConditionSatisfied, this, agentInstanceContext, postProcess);
        } else if (conditionType == ResultSetProcessorOutputConditionType.POLICY_LASTALL_UNORDERED) {
            if (super.postProcessFactory == null) {
                return new OutputProcessViewConditionLastAllUnord(resultSetProcessor, afterConditionTime, afterConditionNumberOfEvents, isAfterConditionSatisfied, this, agentInstanceContext);
            }
            OutputStrategyPostProcess postProcess = postProcessFactory.make(agentInstanceContext);
            return new OutputProcessViewConditionLastAllUnordPostProcessAll(resultSetProcessor, afterConditionTime, afterConditionNumberOfEvents, isAfterConditionSatisfied, this, agentInstanceContext, postProcess);
        } else {
            if (super.postProcessFactory == null) {
                return new OutputProcessViewConditionDefault(resultSetProcessor, afterConditionTime, afterConditionNumberOfEvents, isAfterConditionSatisfied, this, agentInstanceContext, streamCount > 1, eventTypes);
            }
            OutputStrategyPostProcess postProcess = postProcessFactory.make(agentInstanceContext);
            return new OutputProcessViewConditionDefaultPostProcess(resultSetProcessor, afterConditionTime, afterConditionNumberOfEvents, isAfterConditionSatisfied, this, agentInstanceContext, postProcess, streamCount > 1, eventTypes);
        }
    }

    public OutputConditionFactory getOutputConditionFactory() {
        return outputConditionFactory;
    }

    public int getStreamCount() {
        return streamCount;
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
