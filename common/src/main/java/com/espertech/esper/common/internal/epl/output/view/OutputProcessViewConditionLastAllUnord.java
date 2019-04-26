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

import com.espertech.esper.common.client.EventBean;
import com.espertech.esper.common.internal.collection.MultiKeyArrayOfKeys;
import com.espertech.esper.common.internal.collection.UniformPair;
import com.espertech.esper.common.internal.context.util.AgentInstanceContext;
import com.espertech.esper.common.internal.context.util.AgentInstanceStopServices;
import com.espertech.esper.common.internal.epl.expression.core.ExprEvaluatorContext;
import com.espertech.esper.common.internal.epl.output.condition.OutputCallback;
import com.espertech.esper.common.internal.epl.output.condition.OutputCondition;
import com.espertech.esper.common.internal.epl.output.core.OutputStrategyUtil;
import com.espertech.esper.common.internal.epl.resultset.core.ResultSetProcessor;
import com.espertech.esper.common.internal.event.core.EventBeanUtility;
import com.espertech.esper.common.internal.util.ExecutionPathDebugLog;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Iterator;
import java.util.Set;

/**
 * Handles output rate limiting for LAST and without order-by.
 */
public class OutputProcessViewConditionLastAllUnord extends OutputProcessViewBaseWAfter {
    private final OutputProcessViewConditionFactory parent;
    private final OutputCondition outputCondition;

    private static final Logger log = LoggerFactory.getLogger(OutputProcessViewConditionLastAllUnord.class);

    public OutputProcessViewConditionLastAllUnord(ResultSetProcessor resultSetProcessor, Long afterConditionTime, Integer afterConditionNumberOfEvents, boolean afterConditionSatisfied, OutputProcessViewConditionFactory parent, AgentInstanceContext agentInstanceContext) {
        super(agentInstanceContext, resultSetProcessor, afterConditionTime, afterConditionNumberOfEvents, afterConditionSatisfied);
        this.parent = parent;

        OutputCallback outputCallback = getCallbackToLocal(parent.getStreamCount());
        this.outputCondition = parent.getOutputConditionFactory().instantiateOutputCondition(agentInstanceContext, outputCallback);
    }

    public int getNumChangesetRows() {
        return 0;
    }

    public OutputCondition getOptionalOutputCondition() {
        return outputCondition;
    }

    public OutputProcessViewAfterState getOptionalAfterConditionState() {
        return null;
    }

    public void update(EventBean[] newData, EventBean[] oldData) {
        if ((ExecutionPathDebugLog.isDebugEnabled) && (log.isDebugEnabled())) {
            log.debug(".update Received update, " +
                    "  newData.length==" + ((newData == null) ? 0 : newData.length) +
                    "  oldData.length==" + ((oldData == null) ? 0 : oldData.length));
        }

        boolean isGenerateSynthetic = agentInstanceContext.getStatementResultService().isMakeSynthetic();
        resultSetProcessor.processOutputLimitedLastAllNonBufferedView(newData, oldData, isGenerateSynthetic);

        if (!super.checkAfterCondition(newData, agentInstanceContext.getStatementContext())) {
            return;
        }

        int newDataLength = 0;
        int oldDataLength = 0;
        if (newData != null) {
            newDataLength = newData.length;
        }
        if (oldData != null) {
            oldDataLength = oldData.length;
        }

        outputCondition.updateOutputCondition(newDataLength, oldDataLength);
    }

    /**
     * This process (update) method is for participation in a join.
     *
     * @param newEvents - new events
     * @param oldEvents - old events
     */
    public void process(Set<MultiKeyArrayOfKeys<EventBean>> newEvents, Set<MultiKeyArrayOfKeys<EventBean>> oldEvents, ExprEvaluatorContext exprEvaluatorContext) {
        if ((ExecutionPathDebugLog.isDebugEnabled) && (log.isDebugEnabled())) {
            log.debug(".process Received update, " +
                    "  newData.length==" + ((newEvents == null) ? 0 : newEvents.size()) +
                    "  oldData.length==" + ((oldEvents == null) ? 0 : oldEvents.size()));
        }

        boolean isGenerateSynthetic = agentInstanceContext.getStatementResultService().isMakeSynthetic();
        resultSetProcessor.processOutputLimitedLastAllNonBufferedJoin(newEvents, oldEvents, isGenerateSynthetic);

        if (!super.checkAfterCondition(newEvents, agentInstanceContext.getStatementContext())) {
            return;
        }

        int newEventsSize = 0;
        if (newEvents != null) {
            newEventsSize = newEvents.size();
        }

        int oldEventsSize = 0;
        if (oldEvents != null) {
            oldEventsSize = oldEvents.size();
        }
        outputCondition.updateOutputCondition(newEventsSize, oldEventsSize);
    }

    /**
     * Called once the output condition has been met.
     * Invokes the result set processor.
     * Used for non-join event data.
     *
     * @param doOutput    - true if the batched events should actually be output as well as processed, false if they should just be processed
     * @param forceUpdate - true if output should be made even when no updating events have arrived
     */
    protected void continueOutputProcessingView(boolean doOutput, boolean forceUpdate) {
        if ((ExecutionPathDebugLog.isDebugEnabled) && (log.isDebugEnabled())) {
            log.debug(".continueOutputProcessingView");
        }

        boolean isGenerateSynthetic = agentInstanceContext.getStatementResultService().isMakeSynthetic();
        UniformPair<EventBean[]> newOldEvents = resultSetProcessor.continueOutputLimitedLastAllNonBufferedView(isGenerateSynthetic);

        continueOutputProcessingViewAndJoin(doOutput, forceUpdate, newOldEvents);
    }

    protected void output(boolean forceUpdate, UniformPair<EventBean[]> results) {
        // Child view can be null in replay from named window
        if (child != null) {
            OutputStrategyUtil.output(forceUpdate, results, child);
        }
    }

    /**
     * Called once the output condition has been met.
     * Invokes the result set processor.
     * Used for join event data.
     *
     * @param doOutput    - true if the batched events should actually be output as well as processed, false if they should just be processed
     * @param forceUpdate - true if output should be made even when no updating events have arrived
     */
    protected void continueOutputProcessingJoin(boolean doOutput, boolean forceUpdate) {
        if ((ExecutionPathDebugLog.isDebugEnabled) && (log.isDebugEnabled())) {
            log.debug(".continueOutputProcessingJoin");
        }

        boolean isGenerateSynthetic = agentInstanceContext.getStatementResultService().isMakeSynthetic();
        UniformPair<EventBean[]> newOldEvents = resultSetProcessor.continueOutputLimitedLastAllNonBufferedJoin(isGenerateSynthetic);

        continueOutputProcessingViewAndJoin(doOutput, forceUpdate, newOldEvents);
    }

    private OutputCallback getCallbackToLocal(int streamCount) {
        // single stream means no join
        // multiple streams means a join
        if (streamCount == 1) {
            return new OutputCallback() {
                public void continueOutputProcessing(boolean doOutput, boolean forceUpdate) {
                    OutputProcessViewConditionLastAllUnord.this.continueOutputProcessingView(doOutput, forceUpdate);
                }
            };
        } else {
            return new OutputCallback() {
                public void continueOutputProcessing(boolean doOutput, boolean forceUpdate) {
                    OutputProcessViewConditionLastAllUnord.this.continueOutputProcessingJoin(doOutput, forceUpdate);
                }
            };
        }
    }

    public Iterator<EventBean> iterator() {
        return OutputStrategyUtil.getIterator(joinExecutionStrategy, resultSetProcessor, parentView, parent.isDistinct(), parent.getDistinctKeyGetter());
    }

    public void terminated() {
        if (parent.isTerminable()) {
            outputCondition.terminated();
        }
    }

    @Override
    public void stop(AgentInstanceStopServices services) {
        super.stop(services);
        outputCondition.stopOutputCondition();
    }

    private void continueOutputProcessingViewAndJoin(boolean doOutput, boolean forceUpdate, UniformPair<EventBean[]> newOldEvents) {

        if (parent.isDistinct() && newOldEvents != null) {
            newOldEvents.setFirst(EventBeanUtility.getDistinctByProp(newOldEvents.getFirst(), parent.getDistinctKeyGetter()));
            newOldEvents.setSecond(EventBeanUtility.getDistinctByProp(newOldEvents.getSecond(), parent.getDistinctKeyGetter()));
        }

        if (doOutput) {
            output(forceUpdate, newOldEvents);
        }
    }
}
