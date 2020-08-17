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
import com.espertech.esper.common.client.EventType;
import com.espertech.esper.common.internal.collection.MultiKeyArrayOfKeys;
import com.espertech.esper.common.internal.collection.UniformPair;
import com.espertech.esper.common.internal.context.util.AgentInstanceContext;
import com.espertech.esper.common.internal.context.util.AgentInstanceStopServices;
import com.espertech.esper.common.internal.context.util.StatementResultService;
import com.espertech.esper.common.internal.epl.expression.core.ExprEvaluatorContext;
import com.espertech.esper.common.internal.epl.output.condition.OutputCondition;
import com.espertech.esper.common.internal.epl.resultset.core.ResultSetProcessor;
import com.espertech.esper.common.internal.metrics.instrumentation.InstrumentationCommon;

import java.util.Iterator;
import java.util.Set;

public class OutputProcessViewDirectSimpleImpl extends OutputProcessView {
    private final ResultSetProcessor resultSetProcessor;
    private final AgentInstanceContext agentInstanceContext;

    public OutputProcessViewDirectSimpleImpl(ResultSetProcessor resultSetProcessor, AgentInstanceContext agentInstanceContext) {
        this.resultSetProcessor = resultSetProcessor;
        this.agentInstanceContext = agentInstanceContext;
    }

    public EventType getEventType() {
        return resultSetProcessor.getResultEventType();
    }

    public void update(EventBean[] newData, EventBean[] oldData) {
        InstrumentationCommon instrumentationCommon = agentInstanceContext.getInstrumentationProvider();
        instrumentationCommon.qOutputProcessNonBuffered(newData, oldData);

        StatementResultService statementResultService = agentInstanceContext.getStatementResultService();
        boolean isGenerateSynthetic = statementResultService.isMakeSynthetic();
        boolean isGenerateNatural = statementResultService.isMakeNatural();
        UniformPair<EventBean[]> newOldEvents = resultSetProcessor.processViewResult(newData, oldData, isGenerateSynthetic);
        if (!isGenerateSynthetic && !isGenerateNatural) {
            return;
        }
        if (child != null) {
            if (newOldEvents != null) {
                if (newOldEvents.getFirst() != null || newOldEvents.getSecond() != null) {
                    child.newResult(newOldEvents);
                } else if (newData == null && oldData == null) {
                    child.newResult(newOldEvents);
                }
            } else {
                if (newData == null && oldData == null) {
                    child.newResult(newOldEvents);
                }
            }
        }

        instrumentationCommon.aOutputProcessNonBuffered();
    }

    public void process(Set<MultiKeyArrayOfKeys<EventBean>> newData, Set<MultiKeyArrayOfKeys<EventBean>> oldData, ExprEvaluatorContext exprEvaluatorContext) {
        InstrumentationCommon instrumentationCommon = agentInstanceContext.getInstrumentationProvider();
        instrumentationCommon.qOutputProcessNonBufferedJoin(newData, oldData);

        StatementResultService statementResultService = agentInstanceContext.getStatementResultService();
        boolean isGenerateSynthetic = statementResultService.isMakeSynthetic();
        boolean isGenerateNatural = statementResultService.isMakeNatural();
        UniformPair<EventBean[]> newOldEvents = resultSetProcessor.processJoinResult(newData, oldData, isGenerateSynthetic);
        if (!isGenerateSynthetic && !isGenerateNatural) {
            return;
        }
        if (newOldEvents == null) {
            return;
        }
        if (newOldEvents.getFirst() != null || newOldEvents.getSecond() != null) {
            child.newResult(newOldEvents);
        } else if (newData == null && oldData == null) {
            child.newResult(newOldEvents);
        }

        instrumentationCommon.aOutputProcessNonBufferedJoin();
    }

    public Iterator iterator() {
        return OutputStrategyUtil.getIterator(joinExecutionStrategy, resultSetProcessor, parentView, false, null);
    }

    public int getNumChangesetRows() {
        return 0;
    }

    public OutputCondition getOptionalOutputCondition() {
        return null;
    }

    public void stop(AgentInstanceStopServices svc) {
    }

    public void terminated() {
    }
}
