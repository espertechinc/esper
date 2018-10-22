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
import com.espertech.esper.common.client.annotation.AuditEnum;
import com.espertech.esper.common.internal.collection.UniformPair;
import com.espertech.esper.common.internal.compile.stage1.spec.SelectClauseStreamSelectorEnum;
import com.espertech.esper.common.internal.context.util.AgentInstanceContext;
import com.espertech.esper.common.internal.epl.expression.core.ExprEvaluatorContext;
import com.espertech.esper.common.internal.epl.table.core.TableEvalLockUtil;
import com.espertech.esper.common.internal.epl.table.core.TableInstance;
import com.espertech.esper.common.internal.event.core.NaturalEventBean;
import com.espertech.esper.common.internal.statement.dispatch.UpdateDispatchView;

/**
 * An output strategy that handles routing (insert-into) and stream selection.
 */
public class OutputStrategyPostProcess {
    private final OutputStrategyPostProcessFactory parent;
    private final AgentInstanceContext agentInstanceContext;
    private final TableInstance tableInstance;
    private final boolean audit;

    public OutputStrategyPostProcess(OutputStrategyPostProcessFactory parent, AgentInstanceContext agentInstanceContext, TableInstance tableInstance) {
        this.parent = parent;
        this.agentInstanceContext = agentInstanceContext;
        this.tableInstance = tableInstance;
        this.audit = AuditEnum.INSERT.getAudit(agentInstanceContext.getAnnotations()) != null;
    }

    public void output(boolean forceUpdate, UniformPair<EventBean[]> result, UpdateDispatchView finalView) {
        EventBean[] newEvents = result != null ? result.getFirst() : null;
        EventBean[] oldEvents = result != null ? result.getSecond() : null;

        // route first
        if (parent.isRoute()) {
            if ((newEvents != null) && (parent.getInsertIntoStreamSelector().isSelectsIStream())) {
                route(newEvents, agentInstanceContext);
            }

            if ((oldEvents != null) && (parent.getInsertIntoStreamSelector().isSelectsRStream())) {
                route(oldEvents, agentInstanceContext);
            }
        }

        // discard one side of results
        if (parent.getSelectStreamDirEnum() == SelectClauseStreamSelectorEnum.RSTREAM_ONLY) {
            newEvents = oldEvents;
            oldEvents = null;
        } else if (parent.getSelectStreamDirEnum() == SelectClauseStreamSelectorEnum.ISTREAM_ONLY) {
            oldEvents = null;   // since the insert-into may require rstream
        }

        // dispatch
        if (newEvents != null || oldEvents != null) {
            finalView.newResult(new UniformPair<EventBean[]>(newEvents, oldEvents));
        } else if (forceUpdate) {
            finalView.newResult(new UniformPair<EventBean[]>(null, null));
        }
    }

    private void route(EventBean[] events, ExprEvaluatorContext exprEvaluatorContext) {
        for (EventBean routed : events) {
            if (routed instanceof NaturalEventBean) {
                NaturalEventBean natural = (NaturalEventBean) routed;
                if (audit) {
                    agentInstanceContext.getAuditProvider().insert(natural.getOptionalSynthetic(), agentInstanceContext);
                }
                if (tableInstance != null) {
                    TableEvalLockUtil.obtainLockUnless(tableInstance.getTableLevelRWLock().writeLock(), exprEvaluatorContext);
                    tableInstance.addEventUnadorned(natural.getOptionalSynthetic());
                } else {
                    agentInstanceContext.getInternalEventRouter().route(natural.getOptionalSynthetic(), agentInstanceContext, parent.isAddToFront());
                }
            } else {
                if (audit) {
                    agentInstanceContext.getAuditProvider().insert(routed, agentInstanceContext);
                }
                if (tableInstance != null) {
                    TableEvalLockUtil.obtainLockUnless(tableInstance.getTableLevelRWLock().writeLock(), exprEvaluatorContext);
                    tableInstance.addEventUnadorned(routed);
                } else {
                    agentInstanceContext.getInternalEventRouter().route(routed, agentInstanceContext, parent.isAddToFront());
                }
            }
        }
    }
}
