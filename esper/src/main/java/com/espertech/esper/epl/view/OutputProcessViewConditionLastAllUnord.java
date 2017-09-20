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

import com.espertech.esper.client.EventBean;
import com.espertech.esper.collection.MultiKey;
import com.espertech.esper.collection.UniformPair;
import com.espertech.esper.core.context.util.AgentInstanceContext;
import com.espertech.esper.epl.core.resultset.core.ResultSetProcessor;
import com.espertech.esper.epl.core.resultset.core.ResultSetProcessorHelperFactory;
import com.espertech.esper.epl.expression.core.ExprEvaluatorContext;
import com.espertech.esper.event.EventBeanUtility;
import com.espertech.esper.metrics.instrumentation.InstrumentationHelper;
import com.espertech.esper.util.ExecutionPathDebugLog;
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

    public OutputProcessViewConditionLastAllUnord(ResultSetProcessorHelperFactory resultSetProcessorHelperFactory, ResultSetProcessor resultSetProcessor, Long afterConditionTime, Integer afterConditionNumberOfEvents, boolean afterConditionSatisfied, OutputProcessViewConditionFactory parent, AgentInstanceContext agentInstanceContext) {
        super(resultSetProcessorHelperFactory, agentInstanceContext, resultSetProcessor, afterConditionTime, afterConditionNumberOfEvents, afterConditionSatisfied);
        this.parent = parent;

        OutputCallback outputCallback = getCallbackToLocal(parent.getStreamCount());
        this.outputCondition = parent.getOutputConditionFactory().make(agentInstanceContext, outputCallback);
    }

    public int getNumChangesetRows() {
        return 0;
    }

    public OutputCondition getOptionalOutputCondition() {
        return outputCondition;
    }

    public OutputProcessViewConditionDeltaSet getOptionalDeltaSet() {
        return null;
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

        boolean isGenerateSynthetic = parent.getStatementResultService().isMakeSynthetic();
        resultSetProcessor.processOutputLimitedLastAllNonBufferedView(newData, oldData, isGenerateSynthetic);

        if (!super.checkAfterCondition(newData, parent.getStatementContext())) {
            if (InstrumentationHelper.ENABLED) {
                InstrumentationHelper.get().aOutputProcessWCondition(false);
            }
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
    public void process(Set<MultiKey<EventBean>> newEvents, Set<MultiKey<EventBean>> oldEvents, ExprEvaluatorContext exprEvaluatorContext) {
        if ((ExecutionPathDebugLog.isDebugEnabled) && (log.isDebugEnabled())) {
            log.debug(".process Received update, " +
                    "  newData.length==" + ((newEvents == null) ? 0 : newEvents.size()) +
                    "  oldData.length==" + ((oldEvents == null) ? 0 : oldEvents.size()));
        }

        boolean isGenerateSynthetic = parent.getStatementResultService().isMakeSynthetic();
        resultSetProcessor.processOutputLimitedLastAllNonBufferedJoin(newEvents, oldEvents, isGenerateSynthetic);

        if (!super.checkAfterCondition(newEvents, parent.getStatementContext())) {
            if (InstrumentationHelper.ENABLED) {
                InstrumentationHelper.get().aOutputProcessWCondition(false);
            }
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

        boolean isGenerateSynthetic = parent.getStatementResultService().isMakeSynthetic();
        UniformPair<EventBean[]> newOldEvents = resultSetProcessor.continueOutputLimitedLastAllNonBufferedView(isGenerateSynthetic);

        continueOutputProcessingViewAndJoin(doOutput, forceUpdate, newOldEvents);
    }

    protected void output(boolean forceUpdate, UniformPair<EventBean[]> results) {
        // Child view can be null in replay from named window
        if (childView != null) {
            OutputStrategyUtil.output(forceUpdate, results, childView);
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

        boolean isGenerateSynthetic = parent.getStatementResultService().isMakeSynthetic();
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
        return OutputStrategyUtil.getIterator(joinExecutionStrategy, resultSetProcessor, parentView, parent.isDistinct());
    }

    public void terminated() {
        if (parent.isTerminable()) {
            outputCondition.terminated();
        }
    }

    private void continueOutputProcessingViewAndJoin(boolean doOutput, boolean forceUpdate, UniformPair<EventBean[]> newOldEvents) {

        if (parent.isDistinct() && newOldEvents != null) {
            newOldEvents.setFirst(EventBeanUtility.getDistinctByProp(newOldEvents.getFirst(), parent.getEventBeanReader()));
            newOldEvents.setSecond(EventBeanUtility.getDistinctByProp(newOldEvents.getSecond(), parent.getEventBeanReader()));
        }

        if (doOutput) {
            output(forceUpdate, newOldEvents);
        }

        if (InstrumentationHelper.ENABLED) {
            InstrumentationHelper.get().aOutputRateConditionOutputNow(true);
        }
    }
}
