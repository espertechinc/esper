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
import com.espertech.esper.core.service.UpdateDispatchView;
import com.espertech.esper.epl.core.resultset.core.ResultSetProcessor;
import com.espertech.esper.epl.expression.core.ExprEvaluatorContext;
import com.espertech.esper.metrics.instrumentation.InstrumentationHelper;
import com.espertech.esper.util.AuditPath;

import java.util.Iterator;
import java.util.Set;

/**
 * Output process view that does not enforce any output policies and may simply
 * hand over events to child views, does not handle distinct.
 */
public class OutputProcessViewDirect extends OutputProcessViewBase {
    private final OutputProcessViewDirectFactory parent;

    public OutputProcessViewDirect(ResultSetProcessor resultSetProcessor, OutputProcessViewDirectFactory parent) {
        super(resultSetProcessor);
        this.parent = parent;
    }

    public int getNumChangesetRows() {
        return 0;
    }

    public OutputCondition getOptionalOutputCondition() {
        return null;
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
        if (InstrumentationHelper.ENABLED) {
            InstrumentationHelper.get().qOutputProcessNonBuffered(newData, oldData);
        }

        boolean isGenerateSynthetic = parent.getStatementResultService().isMakeSynthetic();
        boolean isGenerateNatural = parent.getStatementResultService().isMakeNatural();

        UniformPair<EventBean[]> newOldEvents = resultSetProcessor.processViewResult(newData, oldData, isGenerateSynthetic);

        if ((!isGenerateSynthetic) && (!isGenerateNatural)) {
            if (AuditPath.isAuditEnabled) {
                OutputStrategyUtil.indicateEarlyReturn(parent.getStatementContext(), newOldEvents);
            }
            if (InstrumentationHelper.ENABLED) {
                InstrumentationHelper.get().aOutputProcessNonBuffered();
            }
            return;
        }

        boolean forceOutput = false;
        if ((newData == null) && (oldData == null) &&
                ((newOldEvents == null) || (newOldEvents.getFirst() == null && newOldEvents.getSecond() == null))) {
            forceOutput = true;
        }

        // Child view can be null in replay from named window
        if (childView != null) {
            postProcess(forceOutput, newOldEvents, childView);
        }

        if (InstrumentationHelper.ENABLED) {
            InstrumentationHelper.get().aOutputProcessNonBuffered();
        }
    }

    /**
     * This process (update) method is for participation in a join.
     *
     * @param newEvents - new events
     * @param oldEvents - old events
     */
    public void process(Set<MultiKey<EventBean>> newEvents, Set<MultiKey<EventBean>> oldEvents, ExprEvaluatorContext exprEvaluatorContext) {
        if (InstrumentationHelper.ENABLED) {
            InstrumentationHelper.get().qOutputProcessNonBufferedJoin(newEvents, oldEvents);
        }

        boolean isGenerateSynthetic = parent.getStatementResultService().isMakeSynthetic();
        boolean isGenerateNatural = parent.getStatementResultService().isMakeNatural();

        UniformPair<EventBean[]> newOldEvents = resultSetProcessor.processJoinResult(newEvents, oldEvents, isGenerateSynthetic);

        if ((!isGenerateSynthetic) && (!isGenerateNatural)) {
            if (AuditPath.isAuditEnabled) {
                OutputStrategyUtil.indicateEarlyReturn(parent.getStatementContext(), newOldEvents);
            }
            if (InstrumentationHelper.ENABLED) {
                InstrumentationHelper.get().aOutputProcessNonBufferedJoin();
            }
            return;
        }

        if (newOldEvents == null) {
            if (InstrumentationHelper.ENABLED) {
                InstrumentationHelper.get().aOutputProcessNonBufferedJoin();
            }
            return;
        }

        // Child view can be null in replay from named window
        if (childView != null) {
            postProcess(false, newOldEvents, childView);
        }

        if (InstrumentationHelper.ENABLED) {
            InstrumentationHelper.get().aOutputProcessNonBufferedJoin();
        }
    }

    protected void postProcess(boolean force, UniformPair<EventBean[]> newOldEvents, UpdateDispatchView childView) {
        OutputStrategyUtil.output(force, newOldEvents, childView);
    }

    public Iterator<EventBean> iterator() {
        return OutputStrategyUtil.getIterator(joinExecutionStrategy, resultSetProcessor, parentView, false);
    }

    public void terminated() {
        // Not applicable
    }

    public void stop() {
        // Not applicable
    }
}
