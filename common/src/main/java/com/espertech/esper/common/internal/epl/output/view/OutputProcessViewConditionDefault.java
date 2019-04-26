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
import com.espertech.esper.common.client.EventType;
import com.espertech.esper.common.internal.collection.MultiKeyArrayOfKeys;
import com.espertech.esper.common.internal.collection.UniformPair;
import com.espertech.esper.common.internal.context.util.AgentInstanceContext;
import com.espertech.esper.common.internal.context.util.AgentInstanceStopServices;
import com.espertech.esper.common.internal.context.util.StatementResultService;
import com.espertech.esper.common.internal.epl.expression.core.ExprEvaluatorContext;
import com.espertech.esper.common.internal.epl.output.condition.OutputCallback;
import com.espertech.esper.common.internal.epl.output.condition.OutputCondition;
import com.espertech.esper.common.internal.epl.output.core.OutputProcessViewWithDeltaSet;
import com.espertech.esper.common.internal.epl.output.core.OutputStrategyUtil;
import com.espertech.esper.common.internal.epl.resultset.core.ResultSetProcessor;
import com.espertech.esper.common.internal.event.core.EventBeanUtility;
import com.espertech.esper.common.internal.metrics.audit.AuditPath;
import com.espertech.esper.common.internal.metrics.instrumentation.InstrumentationCommon;

import java.util.Iterator;
import java.util.LinkedHashSet;
import java.util.Set;

/**
 * A view that prepares output events, batching incoming
 * events and invoking the result set processor as necessary.
 * <p>
 * Handles output rate limiting or stabilizing.
 */
public class OutputProcessViewConditionDefault extends OutputProcessViewBaseWAfter implements OutputProcessViewWithDeltaSet {
    private final OutputProcessViewConditionFactory parent;
    private final OutputCondition outputCondition;
    private final OutputProcessViewConditionDeltaSet deltaSet;

    public OutputProcessViewConditionDefault(ResultSetProcessor resultSetProcessor, Long afterConditionTime, Integer afterConditionNumberOfEvents, boolean afterConditionSatisfied, OutputProcessViewConditionFactory parent, AgentInstanceContext agentInstanceContext, boolean isJoin, EventType[] eventTypes) {
        super(agentInstanceContext, resultSetProcessor, afterConditionTime, afterConditionNumberOfEvents, afterConditionSatisfied);
        this.parent = parent;

        OutputCallback outputCallback = getCallbackToLocal(parent.getStreamCount());
        this.outputCondition = parent.getOutputConditionFactory().instantiateOutputCondition(agentInstanceContext, outputCallback);
        this.deltaSet = agentInstanceContext.getResultSetProcessorHelperFactory().makeOutputConditionChangeSet(eventTypes, agentInstanceContext);
    }

    public int getNumChangesetRows() {
        return deltaSet.getNumChangesetRows();
    }

    public OutputProcessViewConditionDeltaSet getOptionalDeltaSet() {
        return deltaSet;
    }

    public OutputCondition getOptionalOutputCondition() {
        return outputCondition;
    }

    public OutputProcessViewAfterState getOptionalAfterConditionState() {
        return null;
    }

    /**
     * The update method is called if the view does not participate in a join.
     *
     * @param newData - new events
     * @param oldData - old events
     */
    public void update(EventBean[] newData, EventBean[] oldData) {
        InstrumentationCommon instrumentationCommon = agentInstanceContext.getInstrumentationProvider();
        instrumentationCommon.qOutputProcessWCondition(newData, oldData);

        // add the incoming events to the event batches
        if (parent.isHasAfter()) {
            boolean afterSatisfied = super.checkAfterCondition(newData, agentInstanceContext.getStatementContext());
            if (!afterSatisfied) {
                if (!parent.isUnaggregatedUngrouped()) {
                    deltaSet.addView(new UniformPair<EventBean[]>(newData, oldData));
                }
                instrumentationCommon.aOutputProcessWCondition(true);
                return;
            } else {
                deltaSet.addView(new UniformPair<EventBean[]>(newData, oldData));
            }
        } else {
            deltaSet.addView(new UniformPair<EventBean[]>(newData, oldData));
        }

        int newDataLength = 0;
        int oldDataLength = 0;
        if (newData != null) {
            newDataLength = newData.length;
        }
        if (oldData != null) {
            oldDataLength = oldData.length;
        }

        instrumentationCommon.qOutputRateConditionUpdate(newDataLength, oldDataLength);
        outputCondition.updateOutputCondition(newDataLength, oldDataLength);
        instrumentationCommon.aOutputRateConditionUpdate();

        instrumentationCommon.aOutputProcessWCondition(false);
    }

    /**
     * This process (update) method is for participation in a join.
     *
     * @param newEvents - new events
     * @param oldEvents - old events
     */
    public void process(Set<MultiKeyArrayOfKeys<EventBean>> newEvents, Set<MultiKeyArrayOfKeys<EventBean>> oldEvents, ExprEvaluatorContext exprEvaluatorContext) {
        InstrumentationCommon instrumentationCommon = agentInstanceContext.getInstrumentationProvider();
        instrumentationCommon.qOutputProcessWConditionJoin(newEvents, oldEvents);

        // add the incoming events to the event batches
        if (parent.isHasAfter()) {
            boolean afterSatisfied = super.checkAfterCondition(newEvents, agentInstanceContext.getStatementContext());
            if (!afterSatisfied) {
                if (!parent.isUnaggregatedUngrouped()) {
                    addToChangeset(newEvents, oldEvents, deltaSet);
                }
                instrumentationCommon.aOutputProcessWConditionJoin(true);
                return;
            } else {
                addToChangeset(newEvents, oldEvents, deltaSet);
            }
        } else {
            addToChangeset(newEvents, oldEvents, deltaSet);
        }

        int newEventsSize = 0;
        if (newEvents != null) {
            newEventsSize = newEvents.size();
        }

        int oldEventsSize = 0;
        if (oldEvents != null) {
            oldEventsSize = oldEvents.size();
        }

        instrumentationCommon.qOutputRateConditionUpdate(newEventsSize, oldEventsSize);
        outputCondition.updateOutputCondition(newEventsSize, oldEventsSize);
        instrumentationCommon.aOutputRateConditionUpdate();

        instrumentationCommon.aOutputProcessWConditionJoin(false);
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
        agentInstanceContext.getInstrumentationProvider().qOutputRateConditionOutputNow();

        StatementResultService statementResultService = agentInstanceContext.getStatementResultService();
        boolean isGenerateSynthetic = statementResultService.isMakeSynthetic();
        boolean isGenerateNatural = statementResultService.isMakeNatural();

        // Process the events and get the result
        UniformPair<EventBean[]> newOldEvents = resultSetProcessor.processOutputLimitedView(deltaSet.getViewEventsSet(), isGenerateSynthetic);

        if (parent.isDistinct() && newOldEvents != null) {
            newOldEvents.setFirst(EventBeanUtility.getDistinctByProp(newOldEvents.getFirst(), parent.getDistinctKeyGetter()));
            newOldEvents.setSecond(EventBeanUtility.getDistinctByProp(newOldEvents.getSecond(), parent.getDistinctKeyGetter()));
        }

        if ((!isGenerateSynthetic) && (!isGenerateNatural)) {
            resetEventBatches();
            agentInstanceContext.getInstrumentationProvider().aOutputRateConditionOutputNow(false);
            return;
        }

        if (doOutput) {
            output(forceUpdate, newOldEvents);
        }
        resetEventBatches();

        agentInstanceContext.getInstrumentationProvider().aOutputRateConditionOutputNow(true);
    }

    protected void output(boolean forceUpdate, UniformPair<EventBean[]> results) {
        // Child view can be null in replay from named window
        if (child != null) {
            OutputStrategyUtil.output(forceUpdate, results, child);
        }
    }

    @Override
    public void stop(AgentInstanceStopServices services) {
        super.stop(services);
        deltaSet.destroy();
        outputCondition.stopOutputCondition();
    }

    private void resetEventBatches() {
        deltaSet.clear();
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
        agentInstanceContext.getInstrumentationProvider().qOutputRateConditionOutputNow();

        StatementResultService statementResultService = agentInstanceContext.getStatementResultService();
        boolean isGenerateSynthetic = statementResultService.isMakeSynthetic();
        boolean isGenerateNatural = statementResultService.isMakeNatural();

        // Process the events and get the result
        UniformPair<EventBean[]> newOldEvents = resultSetProcessor.processOutputLimitedJoin(deltaSet.getJoinEventsSet(), isGenerateSynthetic);

        if (parent.isDistinct() && newOldEvents != null) {
            newOldEvents.setFirst(EventBeanUtility.getDistinctByProp(newOldEvents.getFirst(), parent.getDistinctKeyGetter()));
            newOldEvents.setSecond(EventBeanUtility.getDistinctByProp(newOldEvents.getSecond(), parent.getDistinctKeyGetter()));
        }

        if ((!isGenerateSynthetic) && (!isGenerateNatural)) {
            if (AuditPath.isAuditEnabled) {
                OutputStrategyUtil.indicateEarlyReturn(agentInstanceContext.getStatementContext(), newOldEvents);
            }
            resetEventBatches();
            agentInstanceContext.getInstrumentationProvider().aOutputRateConditionOutputNow(false);
            return;
        }

        if (doOutput) {
            output(forceUpdate, newOldEvents);
        }
        resetEventBatches();

        agentInstanceContext.getInstrumentationProvider().aOutputRateConditionOutputNow(true);
    }

    private OutputCallback getCallbackToLocal(int streamCount) {
        // single stream means no join
        // multiple streams means a join
        if (streamCount == 1) {
            return new OutputCallback() {
                public void continueOutputProcessing(boolean doOutput, boolean forceUpdate) {
                    OutputProcessViewConditionDefault.this.continueOutputProcessingView(doOutput, forceUpdate);
                }
            };
        } else {
            return new OutputCallback() {
                public void continueOutputProcessing(boolean doOutput, boolean forceUpdate) {
                    OutputProcessViewConditionDefault.this.continueOutputProcessingJoin(doOutput, forceUpdate);
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

    private static void addToChangeset(Set<MultiKeyArrayOfKeys<EventBean>> newEvents, Set<MultiKeyArrayOfKeys<EventBean>> oldEvents, OutputProcessViewConditionDeltaSet joinEventsSet) {
        // add the incoming events to the event batches
        Set<MultiKeyArrayOfKeys<EventBean>> copyNew;
        if (newEvents != null) {
            copyNew = new LinkedHashSet<MultiKeyArrayOfKeys<EventBean>>(newEvents);
        } else {
            copyNew = new LinkedHashSet<MultiKeyArrayOfKeys<EventBean>>();
        }

        Set<MultiKeyArrayOfKeys<EventBean>> copyOld;
        if (oldEvents != null) {
            copyOld = new LinkedHashSet<MultiKeyArrayOfKeys<EventBean>>(oldEvents);
        } else {
            copyOld = new LinkedHashSet<MultiKeyArrayOfKeys<EventBean>>();
        }

        joinEventsSet.addJoin(new UniformPair<Set<MultiKeyArrayOfKeys<EventBean>>>(copyNew, copyOld));
    }
}
