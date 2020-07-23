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
package com.espertech.esper.common.internal.epl.subselect;

import com.espertech.esper.common.client.EPException;
import com.espertech.esper.common.client.EventBean;
import com.espertech.esper.common.client.EventType;
import com.espertech.esper.common.client.type.EPTypeClass;
import com.espertech.esper.common.internal.context.util.AgentInstanceContext;
import com.espertech.esper.common.internal.context.util.AgentInstanceMgmtCallback;
import com.espertech.esper.common.internal.context.util.AgentInstanceStopServices;
import com.espertech.esper.common.internal.context.util.AgentInstanceTransferServices;
import com.espertech.esper.common.internal.epl.agg.core.AggregationService;
import com.espertech.esper.common.internal.epl.agg.core.AggregationServiceFactory;
import com.espertech.esper.common.internal.epl.expression.core.ExprEvaluator;
import com.espertech.esper.common.internal.epl.expression.core.ExprEvaluatorContext;
import com.espertech.esper.common.internal.epl.expression.core.ExprNodeUtilityEvaluate;
import com.espertech.esper.common.internal.epl.expression.prior.PriorEvalStrategy;
import com.espertech.esper.common.internal.epl.index.base.EventTable;
import com.espertech.esper.common.internal.epl.index.base.EventTableFactory;
import com.espertech.esper.common.internal.epl.index.base.EventTableFactoryFactory;
import com.espertech.esper.common.internal.epl.index.base.EventTableIndexService;
import com.espertech.esper.common.internal.epl.join.querygraph.QueryGraph;
import com.espertech.esper.common.internal.epl.lookup.LookupStrategyDesc;
import com.espertech.esper.common.internal.epl.lookup.SubordTableLookupStrategy;
import com.espertech.esper.common.internal.epl.lookup.SubordTableLookupStrategyFactory;
import com.espertech.esper.common.internal.epl.lookup.SubordTableLookupStrategyNullRow;
import com.espertech.esper.common.internal.epl.namedwindow.core.NamedWindow;
import com.espertech.esper.common.internal.epl.namedwindow.core.NamedWindowInstance;
import com.espertech.esper.common.internal.epl.namedwindow.core.NamedWindowTailViewInstance;
import com.espertech.esper.common.internal.epl.prior.PriorHelper;
import com.espertech.esper.common.internal.view.access.ViewResourceDelegateDesc;
import com.espertech.esper.common.internal.view.core.*;
import com.espertech.esper.common.internal.view.previous.PreviousGetterStrategy;
import com.espertech.esper.common.internal.view.util.BufferView;

import java.lang.annotation.Annotation;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;

public class SubSelectStrategyFactoryLocalViewPreloaded implements SubSelectStrategyFactory {
    public final static EPTypeClass EPTYPE = new EPTypeClass(SubSelectStrategyFactoryLocalViewPreloaded.class);

    private int subqueryNumber;
    private ViewFactory[] viewFactories;
    private ViewResourceDelegateDesc viewResourceDelegate;
    private EventTableFactoryFactory eventTableFactoryFactory;
    private EventTableFactory eventTableFactory;
    private SubordTableLookupStrategyFactory lookupStrategyFactory;
    private AggregationServiceFactory aggregationServiceFactory;
    private boolean correlatedSubquery;
    private ExprEvaluator groupKeyEval;
    private ExprEvaluator filterExprEval;
    private EventTableIndexService eventTableIndexService;
    private NamedWindow namedWindow;
    private ExprEvaluator namedWindowFilterExpr;
    private QueryGraph namedWindowFilterQueryGraph;

    public void setSubqueryNumber(int subqueryNumber) {
        this.subqueryNumber = subqueryNumber;
    }

    public void setViewFactories(ViewFactory[] viewFactories) {
        this.viewFactories = viewFactories;
    }

    public void setViewResourceDelegate(ViewResourceDelegateDesc viewResourceDelegate) {
        this.viewResourceDelegate = viewResourceDelegate;
    }

    public void setEventTableFactoryFactory(EventTableFactoryFactory eventTableFactoryFactory) {
        this.eventTableFactoryFactory = eventTableFactoryFactory;
    }

    public void setNamedWindow(NamedWindow namedWindow) {
        this.namedWindow = namedWindow;
    }

    public void setLookupStrategyFactory(SubordTableLookupStrategyFactory lookupStrategyFactory) {
        this.lookupStrategyFactory = lookupStrategyFactory;
    }

    public void setAggregationServiceFactory(AggregationServiceFactory aggregationServiceFactory) {
        this.aggregationServiceFactory = aggregationServiceFactory;
    }

    public void setCorrelatedSubquery(boolean correlatedSubquery) {
        this.correlatedSubquery = correlatedSubquery;
    }

    public void setGroupKeyEval(ExprEvaluator groupKeyEval) {
        this.groupKeyEval = groupKeyEval;
    }

    public void setFilterExprEval(ExprEvaluator filterExprEval) {
        this.filterExprEval = filterExprEval;
    }

    public void setNamedWindowFilterExpr(ExprEvaluator namedWindowFilterExpr) {
        this.namedWindowFilterExpr = namedWindowFilterExpr;
    }

    public void setNamedWindowFilterQueryGraph(QueryGraph namedWindowFilterQueryGraph) {
        this.namedWindowFilterQueryGraph = namedWindowFilterQueryGraph;
    }

    public void ready(SubSelectStrategyFactoryContext subselectFactoryContext, EventType eventType) {
        EventType type = viewFactories.length == 0 ? eventType : viewFactories[viewFactories.length - 1].getEventType();
        eventTableFactory = eventTableFactoryFactory.create(type, subselectFactoryContext.getEventTableFactoryContext());
        eventTableIndexService = subselectFactoryContext.getEventTableIndexService();
    }

    public SubSelectStrategyRealization instantiate(Viewable viewableRoot, AgentInstanceContext agentInstanceContext, List<AgentInstanceMgmtCallback> stopCallbackList, int subqueryNumber, boolean isRecoveringResilient, Annotation[] annotations) {

        // create factory chain context to hold callbacks specific to "prior" and "prev"
        AgentInstanceViewFactoryChainContext viewFactoryChainContext = AgentInstanceViewFactoryChainContext.create(viewFactories, agentInstanceContext, viewResourceDelegate);
        ViewablePair viewables = ViewFactoryUtil.materialize(viewFactories, viewableRoot, viewFactoryChainContext, stopCallbackList);
        final Viewable subselectView = viewables.getLast();

        // make aggregation service
        AggregationService aggregationService = null;
        if (aggregationServiceFactory != null) {
            aggregationService = aggregationServiceFactory.makeService(agentInstanceContext, true, subqueryNumber, null);

            final AggregationService aggregationServiceStoppable = aggregationService;
            stopCallbackList.add(new AgentInstanceMgmtCallback() {
                public void stop(AgentInstanceStopServices services) {
                    aggregationServiceStoppable.stop();
                }

                public void transfer(AgentInstanceTransferServices services) {
                    // no action
                }
            });
        }

        // handle "prior" nodes and their strategies
        PriorEvalStrategy priorStrategy = PriorHelper.toStrategy(viewFactoryChainContext);

        // handle "previous" nodes and their strategies
        PreviousGetterStrategy previousGetter = viewFactoryChainContext.getPreviousNodeGetter();

        // handle aggregated and non-correlated queries: there is no strategy or index
        if (aggregationServiceFactory != null && !correlatedSubquery) {
            View aggregatorView;
            if (groupKeyEval == null) {
                if (filterExprEval == null) {
                    aggregatorView = new SubselectAggregatorViewUnfilteredUngrouped(aggregationService, filterExprEval, agentInstanceContext, null);
                } else {
                    aggregatorView = new SubselectAggregatorViewFilteredUngrouped(aggregationService, filterExprEval, agentInstanceContext, null);
                }
            } else {
                if (filterExprEval == null) {
                    aggregatorView = new SubselectAggregatorViewUnfilteredGrouped(aggregationService, filterExprEval, agentInstanceContext, groupKeyEval);
                } else {
                    aggregatorView = new SubselectAggregatorViewFilteredGrouped(aggregationService, filterExprEval, agentInstanceContext, groupKeyEval);
                }
            }
            subselectView.setChild(aggregatorView);

            if (namedWindow != null && eventTableIndexService.allowInitIndex(isRecoveringResilient)) {
                preloadFromNamedWindow(null, aggregatorView, agentInstanceContext, annotations);
            }

            return new SubSelectStrategyRealization(SubordTableLookupStrategyNullRow.INSTANCE, null, aggregationService, priorStrategy, previousGetter, subselectView, null);
        }

        // create index/holder table
        final EventTable[] index = eventTableFactory.makeEventTables(agentInstanceContext, subqueryNumber);
        stopCallbackList.add(new SubqueryIndexMgmtCallback(index));

        // create strategy
        SubordTableLookupStrategy strategy = lookupStrategyFactory.makeStrategy(index, agentInstanceContext, null);

        // handle unaggregated or correlated queries or
        SubselectAggregationPreprocessorBase subselectAggregationPreprocessor = null;
        if (aggregationServiceFactory != null) {
            if (groupKeyEval == null) {
                if (filterExprEval == null) {
                    subselectAggregationPreprocessor = new SubselectAggregationPreprocessorUnfilteredUngrouped(aggregationService, filterExprEval, null);
                } else {
                    subselectAggregationPreprocessor = new SubselectAggregationPreprocessorFilteredUngrouped(aggregationService, filterExprEval, null);
                }
            } else {
                if (filterExprEval == null) {
                    subselectAggregationPreprocessor = new SubselectAggregationPreprocessorUnfilteredGrouped(aggregationService, filterExprEval, groupKeyEval);
                } else {
                    subselectAggregationPreprocessor = new SubselectAggregationPreprocessorFilteredGrouped(aggregationService, filterExprEval, groupKeyEval);
                }
            }
        }

        // preload when allowed
        if (namedWindow != null && eventTableIndexService.allowInitIndex(isRecoveringResilient)) {
            preloadFromNamedWindow(index, subselectView, agentInstanceContext, annotations);
        }

        BufferView bufferView = new BufferView(subqueryNumber);
        bufferView.setObserver(new SubselectBufferObserver(index, agentInstanceContext));
        subselectView.setChild(bufferView);

        return new SubSelectStrategyRealization(strategy, subselectAggregationPreprocessor, aggregationService, priorStrategy, previousGetter, subselectView, index);
    }

    private void preloadFromNamedWindow(EventTable[] eventIndex, Viewable subselectView, ExprEvaluatorContext exprEvaluatorContext, Annotation[] annotations) {

        NamedWindowInstance instance = namedWindow.getNamedWindowInstance(exprEvaluatorContext);
        if (instance == null) {
            throw new EPException("Named window '" + namedWindow.getName() + "' is associated to context '" + namedWindow.getStatementContext().getContextName() + "' that is not available for querying");
        }
        NamedWindowTailViewInstance consumerView = instance.getTailViewInstance();

        // preload view for stream
        Collection<EventBean> eventsInWindow;
        if (namedWindowFilterExpr != null) {
            Collection<EventBean> snapshot = consumerView.snapshotNoLock(namedWindowFilterQueryGraph, annotations);
            eventsInWindow = new ArrayList<>(snapshot.size());
            ExprNodeUtilityEvaluate.applyFilterExpressionIterable(snapshot.iterator(), namedWindowFilterExpr, exprEvaluatorContext, eventsInWindow);
        } else {
            eventsInWindow = new ArrayList<>();
            for (Iterator<EventBean> it = consumerView.iterator(); it.hasNext(); ) {
                eventsInWindow.add(it.next());
            }
        }
        EventBean[] newEvents = eventsInWindow.toArray(new EventBean[eventsInWindow.size()]);
        if (subselectView != null) {
            ((View) subselectView).update(newEvents, null);
        }
        if (eventIndex != null) {
            for (EventTable table : eventIndex) {
                table.add(newEvents, exprEvaluatorContext);  // fill index
            }
        }
    }

    public LookupStrategyDesc getLookupStrategyDesc() {
        return lookupStrategyFactory.getLookupStrategyDesc();
    }
}