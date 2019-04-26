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
import com.espertech.esper.common.internal.context.util.StatementResultService;
import com.espertech.esper.common.internal.epl.expression.core.ExprEvaluatorContext;
import com.espertech.esper.common.internal.epl.output.condition.OutputCondition;
import com.espertech.esper.common.internal.epl.output.core.OutputStrategyUtil;
import com.espertech.esper.common.internal.epl.resultset.core.ResultSetProcessor;
import com.espertech.esper.common.internal.event.core.EventBeanUtility;
import com.espertech.esper.common.internal.metrics.audit.AuditPath;
import com.espertech.esper.common.internal.statement.dispatch.UpdateDispatchView;
import com.espertech.esper.common.internal.util.ExecutionPathDebugLog;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Iterator;
import java.util.Set;

/**
 * Output process view that does not enforce any output policies and may simply
 * hand over events to child views, but works with distinct and after-output policies
 */
public class OutputProcessViewDirectDistinctOrAfter extends OutputProcessViewBaseWAfter {
    private static final Logger log = LoggerFactory.getLogger(OutputProcessViewDirectDistinctOrAfter.class);

    private final OutputProcessViewDirectDistinctOrAfterFactory parent;

    public OutputProcessViewDirectDistinctOrAfter(AgentInstanceContext agentInstanceContext, ResultSetProcessor resultSetProcessor, Long afterConditionTime, Integer afterConditionNumberOfEvents, boolean afterConditionSatisfied, OutputProcessViewDirectDistinctOrAfterFactory parent) {
        super(agentInstanceContext, resultSetProcessor, afterConditionTime, afterConditionNumberOfEvents, afterConditionSatisfied);
        this.parent = parent;
    }

    public int getNumChangesetRows() {
        return 0;
    }

    public OutputCondition getOptionalOutputCondition() {
        return null;
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

        if (!super.checkAfterCondition(newOldEvents, agentInstanceContext.getStatementContext())) {
            return;
        }

        if (parent.isDistinct() && newOldEvents != null) {
            newOldEvents.setFirst(EventBeanUtility.getDistinctByProp(newOldEvents.getFirst(), parent.getDistinctKeyGetter()));
            newOldEvents.setSecond(EventBeanUtility.getDistinctByProp(newOldEvents.getSecond(), parent.getDistinctKeyGetter()));
        }

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
        if ((ExecutionPathDebugLog.isDebugEnabled) && (log.isDebugEnabled())) {
            log.debug(".process Received update, " +
                    "  newData.length==" + ((newEvents == null) ? 0 : newEvents.size()) +
                    "  oldData.length==" + ((oldEvents == null) ? 0 : oldEvents.size()));
        }

        StatementResultService statementResultService = agentInstanceContext.getStatementResultService();
        boolean isGenerateSynthetic = statementResultService.isMakeSynthetic();
        boolean isGenerateNatural = statementResultService.isMakeNatural();

        UniformPair<EventBean[]> newOldEvents = resultSetProcessor.processJoinResult(newEvents, oldEvents, isGenerateSynthetic);

        if (!checkAfterCondition(newOldEvents, agentInstanceContext.getStatementContext())) {
            return;
        }

        if (parent.isDistinct() && newOldEvents != null) {
            newOldEvents.setFirst(EventBeanUtility.getDistinctByProp(newOldEvents.getFirst(), parent.getDistinctKeyGetter()));
            newOldEvents.setSecond(EventBeanUtility.getDistinctByProp(newOldEvents.getSecond(), parent.getDistinctKeyGetter()));
        }

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
        return OutputStrategyUtil.getIterator(joinExecutionStrategy, resultSetProcessor, parentView, parent.isDistinct(), parent.getDistinctKeyGetter());
    }

    public void terminated() {
        // Not applicable
    }
}
