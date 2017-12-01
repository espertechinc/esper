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
package com.espertech.esper.core.context.factory;

import com.espertech.esper.client.EventBean;
import com.espertech.esper.epl.expression.core.ExprNodeUtilityCore;
import com.espertech.esper.epl.expression.core.ExprEvaluatorContext;
import com.espertech.esper.epl.expression.core.ExprNode;
import com.espertech.esper.epl.join.base.JoinSetComposerDesc;
import com.espertech.esper.epl.join.plan.QueryGraph;
import com.espertech.esper.epl.named.NamedWindowTailViewInstance;
import com.espertech.esper.view.HistoricalEventViewable;
import com.espertech.esper.view.Viewable;

import java.lang.annotation.Annotation;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

public class StatementAgentInstancePostLoadSelect implements StatementAgentInstancePostLoad {
    private final Viewable[] streamViews;
    private final JoinSetComposerDesc joinSetComposer;
    private final NamedWindowTailViewInstance[] namedWindowTailViews;
    private final QueryGraph[] namedWindowPostloadFilters;
    private final List<ExprNode>[] namedWindowFilters;
    private final Annotation[] annotations;
    private final ExprEvaluatorContext exprEvaluatorContext;

    public StatementAgentInstancePostLoadSelect(Viewable[] streamViews, JoinSetComposerDesc joinSetComposer, NamedWindowTailViewInstance[] namedWindowTailViews, QueryGraph[] namedWindowPostloadFilters, List<ExprNode>[] namedWindowFilters, Annotation[] annotations, ExprEvaluatorContext exprEvaluatorContext) {
        this.streamViews = streamViews;
        this.joinSetComposer = joinSetComposer;
        this.namedWindowTailViews = namedWindowTailViews;
        this.namedWindowPostloadFilters = namedWindowPostloadFilters;
        this.namedWindowFilters = namedWindowFilters;
        this.annotations = annotations;
        this.exprEvaluatorContext = exprEvaluatorContext;
    }

    public void executePostLoad() {
        if (joinSetComposer == null || !joinSetComposer.getJoinSetComposer().allowsInit()) {
            return;
        }
        EventBean[][] events = new EventBean[streamViews.length][];
        for (int stream = 0; stream < streamViews.length; stream++) {
            Viewable streamView = streamViews[stream];
            if (streamView instanceof HistoricalEventViewable) {
                continue;
            }

            Collection<EventBean> eventsInWindow;
            if (namedWindowTailViews[stream] != null) {
                NamedWindowTailViewInstance nwtail = namedWindowTailViews[stream];
                Collection<EventBean> snapshot = nwtail.snapshotNoLock(namedWindowPostloadFilters[stream], annotations);
                if (namedWindowFilters[stream] != null) {
                    eventsInWindow = new ArrayList<EventBean>(snapshot.size());
                    ExprNodeUtilityCore.applyFilterExpressionsIterable(snapshot, namedWindowFilters[stream], exprEvaluatorContext, eventsInWindow);
                } else {
                    eventsInWindow = snapshot;
                }
            } else if (namedWindowFilters[stream] != null && !namedWindowFilters[stream].isEmpty()) {
                eventsInWindow = new ArrayDeque<EventBean>();
                ExprNodeUtilityCore.applyFilterExpressionsIterable(streamViews[stream], namedWindowFilters[stream], exprEvaluatorContext, eventsInWindow);
            } else {
                eventsInWindow = new ArrayDeque<EventBean>();
                for (EventBean aConsumerView : streamViews[stream]) {
                    eventsInWindow.add(aConsumerView);
                }
            }
            events[stream] = eventsInWindow.toArray(new EventBean[eventsInWindow.size()]);
        }
        joinSetComposer.getJoinSetComposer().init(events, exprEvaluatorContext);
    }

    public void acceptIndexVisitor(StatementAgentInstancePostLoadIndexVisitor visitor) {
        // no action
    }
}
