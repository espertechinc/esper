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
package com.espertech.esper.common.internal.epl.output.core;

import com.espertech.esper.common.client.EventBean;
import com.espertech.esper.common.internal.collection.MultiKeyArrayOfKeys;
import com.espertech.esper.common.internal.collection.UniformPair;
import com.espertech.esper.common.internal.context.util.AgentInstanceContext;
import com.espertech.esper.common.internal.context.util.AgentInstanceStopServices;
import com.espertech.esper.common.internal.epl.expression.core.ExprEvaluatorContext;
import com.espertech.esper.common.internal.epl.output.condition.OutputCallback;
import com.espertech.esper.common.internal.epl.output.condition.OutputCondition;
import com.espertech.esper.common.internal.epl.output.view.OutputProcessViewAfterState;
import com.espertech.esper.common.internal.epl.output.view.OutputProcessViewBaseWAfter;
import com.espertech.esper.common.internal.epl.output.view.OutputProcessViewConditionFactory;
import com.espertech.esper.common.internal.epl.resultset.core.ResultSetProcessor;
import com.espertech.esper.common.internal.util.ExecutionPathDebugLog;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.Set;

/**
 * A view that handles the "output snapshot" keyword in output rate stabilizing.
 */
public class OutputProcessViewConditionSnapshot extends OutputProcessViewBaseWAfter {
    private final OutputProcessViewConditionFactory parent;

    private final OutputCondition outputCondition;

    private static final Logger log = LoggerFactory.getLogger(OutputProcessViewConditionSnapshot.class);

    public OutputProcessViewConditionSnapshot(ResultSetProcessor resultSetProcessor, Long afterConditionTime, Integer afterConditionNumberOfEvents, boolean afterConditionSatisfied, OutputProcessViewConditionFactory parent, AgentInstanceContext agentInstanceContext) {
        super(agentInstanceContext, resultSetProcessor, afterConditionTime, afterConditionNumberOfEvents, afterConditionSatisfied);
        this.parent = parent;

        OutputCallback outputCallback = getCallbackToLocal(parent.getStreamCount());
        this.outputCondition = parent.getOutputConditionFactory().instantiateOutputCondition(agentInstanceContext, outputCallback);
    }

    @Override
    public void stop(AgentInstanceStopServices services) {
        super.stop(services);
        outputCondition.stopOutputCondition();
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

    /**
     * The update method is called if the view does not participate in a join.
     *
     * @param newData - new events
     * @param oldData - old events
     */
    public void update(EventBean[] newData, EventBean[] oldData) {
        resultSetProcessor.applyViewResult(newData, oldData);

        if (!super.checkAfterCondition(newData, agentInstanceContext.getStatementContext())) {
            return;
        }

        // add the incoming events to the event batches
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
        resultSetProcessor.applyJoinResult(newEvents, oldEvents);

        if (!super.checkAfterCondition(newEvents, agentInstanceContext.getStatementContext())) {
            return;
        }

        int newEventsSize = 0;
        if (newEvents != null) {
            // add the incoming events to the event batches
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
        EventBean[] newEvents = null;
        EventBean[] oldEvents = null;

        Iterator<EventBean> it = this.iterator();
        if (it.hasNext()) {
            ArrayList<EventBean> snapshot = new ArrayList<EventBean>();
            while (it.hasNext()) {
                EventBean event = it.next();
                snapshot.add(event);
            }
            newEvents = snapshot.toArray(new EventBean[snapshot.size()]);
            oldEvents = null;
        }

        UniformPair<EventBean[]> newOldEvents = new UniformPair<EventBean[]>(newEvents, oldEvents);

        if (doOutput) {
            output(forceUpdate, newOldEvents);
        }
    }

    public void output(boolean forceUpdate, UniformPair<EventBean[]> results) {
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
        continueOutputProcessingView(doOutput, forceUpdate);
    }

    private OutputCallback getCallbackToLocal(int streamCount) {
        // single stream means no join
        // multiple streams means a join
        if (streamCount == 1) {
            return new OutputCallback() {
                public void continueOutputProcessing(boolean doOutput, boolean forceUpdate) {
                    OutputProcessViewConditionSnapshot.this.continueOutputProcessingView(doOutput, forceUpdate);
                }
            };
        } else {
            return new OutputCallback() {
                public void continueOutputProcessing(boolean doOutput, boolean forceUpdate) {
                    OutputProcessViewConditionSnapshot.this.continueOutputProcessingJoin(doOutput, forceUpdate);
                }
            };
        }
    }

    @Override
    public Iterator<EventBean> iterator() {
        return OutputStrategyUtil.getIterator(joinExecutionStrategy, resultSetProcessor, parentView, parent.isDistinct(), parent.getDistinctKeyGetter());
    }

    public void terminated() {
        if (parent.isTerminable()) {
            outputCondition.terminated();
        }
    }
}
