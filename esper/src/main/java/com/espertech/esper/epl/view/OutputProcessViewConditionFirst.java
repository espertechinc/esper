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
import com.espertech.esper.epl.spec.SelectClauseStreamSelectorEnum;
import com.espertech.esper.event.EventBeanUtility;
import com.espertech.esper.util.AuditPath;
import com.espertech.esper.util.ExecutionPathDebugLog;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;

/**
 * Handles output rate limiting for FIRST, only applicable with a having-clause and no group-by clause.
 * <p>
 * Without having-clause the order of processing won't matter therefore its handled by the
 * {@link OutputProcessViewConditionDefault}. With group-by the {@link ResultSetProcessor} handles the per-group first criteria.
 */
public class OutputProcessViewConditionFirst extends OutputProcessViewBaseWAfter {
    private final OutputProcessViewConditionFactory parent;
    private final OutputCondition outputCondition;

    // Posted events in ordered form (for applying to aggregates) and summarized per type
    // Using ArrayList as random access is a requirement.
    private List<UniformPair<EventBean[]>> viewEventsList = new ArrayList<UniformPair<EventBean[]>>();
    private List<UniformPair<Set<MultiKey<EventBean>>>> joinEventsSet = new ArrayList<UniformPair<Set<MultiKey<EventBean>>>>();
    private boolean witnessedFirst;

    private static final Logger log = LoggerFactory.getLogger(OutputProcessViewConditionFirst.class);

    public OutputProcessViewConditionFirst(ResultSetProcessorHelperFactory resultSetProcessorHelperFactory, ResultSetProcessor resultSetProcessor, Long afterConditionTime, Integer afterConditionNumberOfEvents, boolean afterConditionSatisfied, OutputProcessViewConditionFactory parent, AgentInstanceContext agentInstanceContext) {
        super(resultSetProcessorHelperFactory, agentInstanceContext, resultSetProcessor, afterConditionTime, afterConditionNumberOfEvents, afterConditionSatisfied);
        this.parent = parent;

        OutputCallback outputCallback = getCallbackToLocal(parent.getStreamCount());
        this.outputCondition = parent.getOutputConditionFactory().make(agentInstanceContext, outputCallback);
    }

    public int getNumChangesetRows() {
        return Math.max(viewEventsList.size(), joinEventsSet.size());
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

    /**
     * The update method is called if the view does not participate in a join.
     *
     * @param newData - new events
     * @param oldData - old events
     */
    public void update(EventBean[] newData, EventBean[] oldData) {
        if ((ExecutionPathDebugLog.isDebugEnabled) && (log.isDebugEnabled())) {
            log.debug(".update Received update, " +
                    "  newData.length==" + ((newData == null) ? 0 : newData.length) +
                    "  oldData.length==" + ((oldData == null) ? 0 : oldData.length));
        }

        if (!super.checkAfterCondition(newData, parent.getStatementContext())) {
            return;
        }

        if (!witnessedFirst) {
            boolean isGenerateSynthetic = parent.getStatementResultService().isMakeSynthetic();

            // Process the events and get the result
            viewEventsList.add(new UniformPair<EventBean[]>(newData, oldData));
            UniformPair<EventBean[]> newOldEvents = resultSetProcessor.processOutputLimitedView(viewEventsList, isGenerateSynthetic);
            viewEventsList.clear();

            if (!hasRelevantResults(newOldEvents)) {
                return;
            }

            witnessedFirst = true;

            if (parent.isDistinct()) {
                newOldEvents.setFirst(EventBeanUtility.getDistinctByProp(newOldEvents.getFirst(), parent.getEventBeanReader()));
                newOldEvents.setSecond(EventBeanUtility.getDistinctByProp(newOldEvents.getSecond(), parent.getEventBeanReader()));
            }

            boolean isGenerateNatural = parent.getStatementResultService().isMakeNatural();
            if ((!isGenerateSynthetic) && (!isGenerateNatural)) {
                if (AuditPath.isAuditEnabled) {
                    OutputStrategyUtil.indicateEarlyReturn(parent.getStatementContext(), newOldEvents);
                }
                return;
            }

            output(true, newOldEvents);
        } else {
            viewEventsList.add(new UniformPair<EventBean[]>(newData, oldData));
            resultSetProcessor.processOutputLimitedView(viewEventsList, false);
            viewEventsList.clear();
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

        if (!super.checkAfterCondition(newEvents, parent.getStatementContext())) {
            return;
        }

        // add the incoming events to the event batches
        if (!witnessedFirst) {
            addToChangeSet(joinEventsSet, newEvents, oldEvents);
            boolean isGenerateSynthetic = parent.getStatementResultService().isMakeSynthetic();
            UniformPair<EventBean[]> newOldEvents = resultSetProcessor.processOutputLimitedJoin(joinEventsSet, isGenerateSynthetic);
            joinEventsSet.clear();

            if (!hasRelevantResults(newOldEvents)) {
                return;
            }

            witnessedFirst = true;

            if (parent.isDistinct()) {
                newOldEvents.setFirst(EventBeanUtility.getDistinctByProp(newOldEvents.getFirst(), parent.getEventBeanReader()));
                newOldEvents.setSecond(EventBeanUtility.getDistinctByProp(newOldEvents.getSecond(), parent.getEventBeanReader()));
            }

            boolean isGenerateNatural = parent.getStatementResultService().isMakeNatural();
            if ((!isGenerateSynthetic) && (!isGenerateNatural)) {
                if (AuditPath.isAuditEnabled) {
                    OutputStrategyUtil.indicateEarlyReturn(parent.getStatementContext(), newOldEvents);
                }
                return;
            }

            output(true, newOldEvents);
        } else {
            addToChangeSet(joinEventsSet, newEvents, oldEvents);

            // Process the events and get the result
            resultSetProcessor.processOutputLimitedJoin(joinEventsSet, false);
            joinEventsSet.clear();
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
        witnessedFirst = false;
    }

    private void output(boolean forceUpdate, UniformPair<EventBean[]> results) {
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
        witnessedFirst = false;
    }

    private OutputCallback getCallbackToLocal(int streamCount) {
        // single stream means no join
        // multiple streams means a join
        if (streamCount == 1) {
            return new OutputCallback() {
                public void continueOutputProcessing(boolean doOutput, boolean forceUpdate) {
                    OutputProcessViewConditionFirst.this.continueOutputProcessingView(doOutput, forceUpdate);
                }
            };
        } else {
            return new OutputCallback() {
                public void continueOutputProcessing(boolean doOutput, boolean forceUpdate) {
                    OutputProcessViewConditionFirst.this.continueOutputProcessingJoin(doOutput, forceUpdate);
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

    private boolean hasRelevantResults(UniformPair<EventBean[]> newOldEvents) {
        if (newOldEvents == null) {
            return false;
        }
        if (parent.getSelectClauseStreamSelectorEnum() == SelectClauseStreamSelectorEnum.ISTREAM_ONLY) {
            if (newOldEvents.getFirst() == null) {
                return false; // nothing to indicate
            }
        } else if (parent.getSelectClauseStreamSelectorEnum() == SelectClauseStreamSelectorEnum.RSTREAM_ISTREAM_BOTH) {
            if (newOldEvents.getFirst() == null && newOldEvents.getSecond() == null) {
                return false; // nothing to indicate
            }
        } else {
            if (newOldEvents.getSecond() == null) {
                return false; // nothing to indicate
            }
        }
        return true;
    }

    private static void addToChangeSet(List<UniformPair<Set<MultiKey<EventBean>>>> joinEventsSet, Set<MultiKey<EventBean>> newEvents, Set<MultiKey<EventBean>> oldEvents) {
        Set<MultiKey<EventBean>> copyNew;
        if (newEvents != null) {
            copyNew = new LinkedHashSet<MultiKey<EventBean>>(newEvents);
        } else {
            copyNew = new LinkedHashSet<MultiKey<EventBean>>();
        }

        Set<MultiKey<EventBean>> copyOld;
        if (oldEvents != null) {
            copyOld = new LinkedHashSet<MultiKey<EventBean>>(oldEvents);
        } else {
            copyOld = new LinkedHashSet<MultiKey<EventBean>>();
        }
        joinEventsSet.add(new UniformPair<Set<MultiKey<EventBean>>>(copyNew, copyOld));
    }
}
