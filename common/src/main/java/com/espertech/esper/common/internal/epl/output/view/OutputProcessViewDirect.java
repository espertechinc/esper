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
import com.espertech.esper.common.internal.epl.output.condition.OutputCondition;
import com.espertech.esper.common.internal.epl.output.core.OutputProcessView;
import com.espertech.esper.common.internal.epl.output.core.OutputStrategyUtil;
import com.espertech.esper.common.internal.epl.resultset.core.ResultSetProcessor;
import com.espertech.esper.common.internal.metrics.audit.AuditPath;
import com.espertech.esper.common.internal.statement.dispatch.UpdateDispatchView;

import java.util.Iterator;
import java.util.Set;

/**
 * Output process view that does not enforce any output policies and may simply
 * hand over events to child views, does not handle distinct.
 */
public class OutputProcessViewDirect extends OutputProcessView {
    private final AgentInstanceContext agentInstanceContext;
    private final ResultSetProcessor resultSetProcessor;

    public OutputProcessViewDirect(AgentInstanceContext agentInstanceContext, ResultSetProcessor resultSetProcessor) {
        this.agentInstanceContext = agentInstanceContext;
        this.resultSetProcessor = resultSetProcessor;
    }

    public int getNumChangesetRows() {
        return 0;
    }

    public OutputCondition getOptionalOutputCondition() {
        return null;
    }

    public EventType getEventType() {
        return resultSetProcessor.getResultEventType();
    }

    /**
     * The update method is called if the view does not participate in a join.
     *
     * @param newData - new events
     * @param oldData - old events
     */
    public void update(EventBean[] newData, EventBean[] oldData) {

        StatementResultService statementResultService = agentInstanceContext.getStatementResultService();
        boolean isGenerateSynthetic = statementResultService.isMakeSynthetic();
        boolean isGenerateNatural = statementResultService.isMakeNatural();

        UniformPair<EventBean[]> newOldEvents = resultSetProcessor.processViewResult(newData, oldData, isGenerateSynthetic);

        if ((!isGenerateSynthetic) && (!isGenerateNatural)) {
            if (AuditPath.isAuditEnabled) {
                OutputStrategyUtil.indicateEarlyReturn(agentInstanceContext.getStatementContext(), newOldEvents);
            }
            return;
        }

        boolean forceOutput = false;
        if ((newData == null) && (oldData == null) &&
                ((newOldEvents == null) || (newOldEvents.getFirst() == null && newOldEvents.getSecond() == null))) {
            forceOutput = true;
        }

        // Child view can be null in replay from named window
        if (child != null) {
            postProcess(forceOutput, newOldEvents, child);
        }
    }

    /**
     * This process (update) method is for participation in a join.
     *
     * @param newEvents - new events
     * @param oldEvents - old events
     */
    public void process(Set<MultiKeyArrayOfKeys<EventBean>> newEvents, Set<MultiKeyArrayOfKeys<EventBean>> oldEvents, ExprEvaluatorContext exprEvaluatorContext) {
        StatementResultService statementResultService = agentInstanceContext.getStatementResultService();
        boolean isGenerateSynthetic = statementResultService.isMakeSynthetic();
        boolean isGenerateNatural = statementResultService.isMakeNatural();

        UniformPair<EventBean[]> newOldEvents = resultSetProcessor.processJoinResult(newEvents, oldEvents, isGenerateSynthetic);

        if ((!isGenerateSynthetic) && (!isGenerateNatural)) {
            if (AuditPath.isAuditEnabled) {
                OutputStrategyUtil.indicateEarlyReturn(agentInstanceContext.getStatementContext(), newOldEvents);
            }
            return;
        }

        if (newOldEvents == null) {
            return;
        }

        // Child view can be null in replay from named window
        if (child != null) {
            postProcess(false, newOldEvents, child);
        }
    }

    protected void postProcess(boolean force, UniformPair<EventBean[]> newOldEvents, UpdateDispatchView childView) {
        OutputStrategyUtil.output(force, newOldEvents, childView);
    }

    public Iterator<EventBean> iterator() {
        return OutputStrategyUtil.getIterator(joinExecutionStrategy, resultSetProcessor, parentView, false, null);
    }

    public void terminated() {
        // Not applicable
    }

    public void stop(AgentInstanceStopServices services) {
    }
}
