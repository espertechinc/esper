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
package com.espertech.esper.core.service;

import com.espertech.esper.client.EventBean;
import com.espertech.esper.client.StatementAwareUpdateListener;
import com.espertech.esper.client.UpdateListener;
import com.espertech.esper.collection.MultiKeyUntyped;
import com.espertech.esper.collection.UniformPair;
import com.espertech.esper.core.thread.OutboundUnitRunnable;
import com.espertech.esper.core.thread.ThreadingOption;
import com.espertech.esper.core.thread.ThreadingService;
import com.espertech.esper.epl.expression.core.ExprEvaluator;
import com.espertech.esper.epl.expression.core.ExprEvaluatorContext;
import com.espertech.esper.epl.metric.MetricReportingPath;
import com.espertech.esper.epl.metric.MetricReportingService;
import com.espertech.esper.epl.metric.MetricReportingServiceSPI;
import com.espertech.esper.epl.metric.StatementMetricHandle;
import com.espertech.esper.event.EventBeanUtility;
import com.espertech.esper.event.NaturalEventBean;
import com.espertech.esper.metrics.instrumentation.InstrumentationHelper;
import com.espertech.esper.util.AuditPath;
import com.espertech.esper.util.ExecutionPathDebugLog;
import com.espertech.esper.view.ViewSupport;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;

/**
 * Implements tracking of statement listeners and subscribers for a given statement
 * such as to efficiently dispatch in situations where 0, 1 or more listeners
 * are attached and/or 0 or 1 subscriber (such as iteration-only statement).
 */
public class StatementResultServiceImpl implements StatementResultService {
    private final static Logger log = LoggerFactory.getLogger(StatementResultServiceImpl.class);

    private final String statementName;
    private final StatementLifecycleSvc statementLifecycleSvc;
    private final MetricReportingService metricReportingService;
    private final ThreadingService threadingService;

    // Part of the statement context
    private EPStatementSPI epStatement;
    private EPServiceProviderSPI epServiceProvider;
    private boolean isInsertInto;
    private boolean isPattern;
    private boolean isDistinct;
    private boolean isForClause;
    private StatementMetricHandle statementMetricHandle;

    private boolean forClauseDelivery = false;
    private ExprEvaluator[] groupDeliveryExpressions;
    private ExprEvaluatorContext exprEvaluatorContext;

    // For natural delivery derived out of select-clause expressions
    private Class[] selectClauseTypes;
    private String[] selectClauseColumnNames;

    // Listeners and subscribers and derived information
    private EPStatementListenerSet statementListenerSet;
    private boolean isMakeNatural;
    private boolean isMakeSynthetic;
    private ResultDeliveryStrategy statementResultNaturalStrategy;

    private Set<StatementResultListener> statementOutputHooks;

    /**
     * Buffer for holding dispatchable events.
     */
    protected ThreadLocal<ArrayDeque<UniformPair<EventBean[]>>> lastResults = new ThreadLocal<ArrayDeque<UniformPair<EventBean[]>>>() {
        protected synchronized ArrayDeque<UniformPair<EventBean[]>> initialValue() {
            return new ArrayDeque<UniformPair<EventBean[]>>();
        }
    };

    /**
     * Ctor.
     *
     * @param statementLifecycleSvc  handles persistence for statements
     * @param metricReportingService for metrics reporting
     * @param threadingService       for outbound threading
     * @param statementName          statement name
     */
    public StatementResultServiceImpl(String statementName,
                                      StatementLifecycleSvc statementLifecycleSvc,
                                      MetricReportingServiceSPI metricReportingService,
                                      ThreadingService threadingService) {
        log.debug(".ctor");
        this.statementName = statementName;
        this.statementLifecycleSvc = statementLifecycleSvc;
        this.metricReportingService = metricReportingService;
        if (metricReportingService != null) {
            this.statementOutputHooks = metricReportingService.getStatementOutputHooks();
        } else {
            this.statementOutputHooks = Collections.EMPTY_SET;
        }
        this.threadingService = threadingService;
    }

    public void setContext(EPStatementSPI epStatement, EPServiceProviderSPI epServiceProvider,
                           boolean isInsertInto,
                           boolean isPattern,
                           boolean isDistinct,
                           boolean isForClause,
                           StatementMetricHandle statementMetricHandle) {
        this.epStatement = epStatement;
        this.epServiceProvider = epServiceProvider;
        this.isInsertInto = isInsertInto;
        this.isPattern = isPattern;
        this.isDistinct = isDistinct;
        this.isForClause = isForClause;
        isMakeSynthetic = isInsertInto || isPattern || isDistinct || isForClause;
        this.statementMetricHandle = statementMetricHandle;
    }

    public void setSelectClause(Class[] selectClauseTypes, String[] selectClauseColumnNames,
                                boolean forClauseDelivery, ExprEvaluator[] groupDeliveryExpressions, ExprEvaluatorContext exprEvaluatorContext) {
        if ((selectClauseTypes == null) || (selectClauseTypes.length == 0)) {
            throw new IllegalArgumentException("Invalid null or zero-element list of select clause expression types");
        }
        if ((selectClauseColumnNames == null) || (selectClauseColumnNames.length == 0)) {
            throw new IllegalArgumentException("Invalid null or zero-element list of select clause column names");
        }
        this.selectClauseTypes = selectClauseTypes;
        this.selectClauseColumnNames = selectClauseColumnNames;
        this.forClauseDelivery = forClauseDelivery;
        this.exprEvaluatorContext = exprEvaluatorContext;
        this.groupDeliveryExpressions = groupDeliveryExpressions;
    }

    public int getStatementId() {
        return epStatement.getStatementId();
    }

    public boolean isMakeSynthetic() {
        return isMakeSynthetic;
    }

    public boolean isMakeNatural() {
        return isMakeNatural;
    }

    public String getStatementName() {
        return statementName;
    }

    public EPStatementListenerSet getStatementListenerSet() {
        return statementListenerSet;
    }

    public void setUpdateListeners(EPStatementListenerSet updateListeners, boolean isRecovery) {
        // indicate that listeners were updated for potential persistence of listener set, once the statement context is known
        if (epStatement != null) {
            this.statementLifecycleSvc.updatedListeners(epStatement, updateListeners, isRecovery);
        }

        this.statementListenerSet = updateListeners;

        isMakeNatural = statementListenerSet.getSubscriber() != null;
        isMakeSynthetic = !(statementListenerSet.getListeners().length == 0 && statementListenerSet.getStmtAwareListeners().length == 0)
                || isPattern || isInsertInto || isDistinct | isForClause;

        if (statementListenerSet.getSubscriber() == null) {
            statementResultNaturalStrategy = null;
            isMakeNatural = false;
            return;
        }

        statementResultNaturalStrategy = ResultDeliveryStrategyFactory.create(epStatement, statementListenerSet.getSubscriber(), statementListenerSet.getSubscriberMethodName(),
                selectClauseTypes, selectClauseColumnNames, epServiceProvider.getURI(), epServiceProvider.getEngineImportService());
        isMakeNatural = true;
    }

    // Called by OutputProcessView
    public void indicate(UniformPair<EventBean[]> results) {
        if (results != null) {
            if ((MetricReportingPath.isMetricsEnabled) && (statementMetricHandle.isEnabled())) {
                int numIStream = (results.getFirst() != null) ? results.getFirst().length : 0;
                int numRStream = (results.getSecond() != null) ? results.getSecond().length : 0;
                this.metricReportingService.accountOutput(statementMetricHandle, numIStream, numRStream);
            }

            if ((results.getFirst() != null) && (results.getFirst().length != 0)) {
                lastResults.get().add(results);
            } else if ((results.getSecond() != null) && (results.getSecond().length != 0)) {
                lastResults.get().add(results);
            }
        }
    }

    public void execute() {
        ArrayDeque<UniformPair<EventBean[]>> dispatches = lastResults.get();

        UniformPair<EventBean[]> events = EventBeanUtility.flattenList(dispatches);

        if (ExecutionPathDebugLog.isDebugEnabled && log.isDebugEnabled()) {
            ViewSupport.dumpUpdateParams(".execute", events);
        }

        if (InstrumentationHelper.ENABLED) {
            InstrumentationHelper.get().qaStatementResultExecute(events, epStatement.getStatementId(), epStatement.getName(), exprEvaluatorContext.getAgentInstanceId(), Thread.currentThread().getId());
        }

        if ((ThreadingOption.isThreadingEnabled) && (threadingService.isOutboundThreading())) {
            threadingService.submitOutbound(new OutboundUnitRunnable(events, this));
        } else {
            processDispatch(events);
        }

        dispatches.clear();
    }

    /**
     * Indicate an outbound result.
     *
     * @param events to indicate
     */
    public void processDispatch(UniformPair<EventBean[]> events) {
        // Plain all-events delivery
        if (!forClauseDelivery) {
            dispatchInternal(events);
            return;
        }

        // Discrete delivery
        if ((groupDeliveryExpressions == null) || (groupDeliveryExpressions.length == 0)) {
            UniformPair<EventBean[]> todeliver = new UniformPair<EventBean[]>(null, null);
            if (events != null) {
                if (events.getFirst() != null) {
                    for (EventBean theEvent : events.getFirst()) {
                        todeliver.setFirst(new EventBean[]{theEvent});
                        dispatchInternal(todeliver);
                    }
                    todeliver.setFirst(null);
                }
                if (events.getSecond() != null) {
                    for (EventBean theEvent : events.getSecond()) {
                        todeliver.setSecond(new EventBean[]{theEvent});
                        dispatchInternal(todeliver);
                    }
                    todeliver.setSecond(null);
                }
            }
            return;
        }

        // Grouped delivery
        Map<Object, UniformPair<EventBean[]>> groups;
        try {
            groups = getGroupedResults(events);
        } catch (RuntimeException ex) {
            log.error("Unexpected exception evaluating grouped-delivery expressions: " + ex.getMessage() + ", delivering ungrouped", ex);
            dispatchInternal(events);
            return;
        }

        // Deliver each group separately
        for (Map.Entry<Object, UniformPair<EventBean[]>> group : groups.entrySet()) {
            dispatchInternal(group.getValue());
        }
    }

    private Map<Object, UniformPair<EventBean[]>> getGroupedResults(UniformPair<EventBean[]> events) {
        if (events == null) {
            return Collections.emptyMap();
        }
        Map<Object, UniformPair<EventBean[]>> groups = new LinkedHashMap<Object, UniformPair<EventBean[]>>();
        EventBean[] eventsPerStream = new EventBean[1];
        getGroupedResults(groups, events.getFirst(), true, eventsPerStream);
        getGroupedResults(groups, events.getSecond(), false, eventsPerStream);
        return groups;
    }

    private void getGroupedResults(Map<Object, UniformPair<EventBean[]>> groups, EventBean[] events, boolean insertStream, EventBean[] eventsPerStream) {
        if (events == null) {
            return;
        }

        for (EventBean theEvent : events) {

            EventBean evalEvent = theEvent;
            if (evalEvent instanceof NaturalEventBean) {
                evalEvent = ((NaturalEventBean) evalEvent).getOptionalSynthetic();
            }

            Object key;
            eventsPerStream[0] = evalEvent;
            if (groupDeliveryExpressions.length == 1) {
                key = groupDeliveryExpressions[0].evaluate(eventsPerStream, true, exprEvaluatorContext);
            } else {
                Object[] keys = new Object[groupDeliveryExpressions.length];
                for (int i = 0; i < groupDeliveryExpressions.length; i++) {
                    keys[i] = groupDeliveryExpressions[i].evaluate(eventsPerStream, true, exprEvaluatorContext);
                }
                key = new MultiKeyUntyped(keys);
            }

            UniformPair<EventBean[]> groupEntry = groups.get(key);
            if (groupEntry == null) {
                if (insertStream) {
                    groupEntry = new UniformPair<EventBean[]>(new EventBean[]{theEvent}, null);
                } else {
                    groupEntry = new UniformPair<EventBean[]>(null, new EventBean[]{theEvent});
                }
                groups.put(key, groupEntry);
            } else {
                if (insertStream) {
                    if (groupEntry.getFirst() == null) {
                        groupEntry.setFirst(new EventBean[]{theEvent});
                    } else {
                        groupEntry.setFirst(EventBeanUtility.addToArray(groupEntry.getFirst(), theEvent));
                    }
                } else {
                    if (groupEntry.getSecond() == null) {
                        groupEntry.setSecond(new EventBean[]{theEvent});
                    } else {
                        groupEntry.setSecond(EventBeanUtility.addToArray(groupEntry.getSecond(), theEvent));
                    }
                }
            }
        }
    }

    private void dispatchInternal(UniformPair<EventBean[]> events) {
        if (statementResultNaturalStrategy != null) {
            statementResultNaturalStrategy.execute(events);
        }

        EventBean[] newEventArr = events != null ? events.getFirst() : null;
        EventBean[] oldEventArr = events != null ? events.getSecond() : null;

        for (UpdateListener listener : statementListenerSet.getListeners()) {
            try {
                listener.update(newEventArr, oldEventArr);
            } catch (Throwable t) {
                String message = "Unexpected exception invoking listener update method on listener class '" + listener.getClass().getSimpleName() +
                        "' : " + t.getClass().getSimpleName() + " : " + t.getMessage();
                log.error(message, t);
            }
        }
        if (statementListenerSet.getStmtAwareListeners().length > 0) {
            for (StatementAwareUpdateListener listener : statementListenerSet.getStmtAwareListeners()) {
                try {
                    listener.update(newEventArr, oldEventArr, epStatement, epServiceProvider);
                } catch (Throwable t) {
                    String message = "Unexpected exception invoking listener update method on listener class '" + listener.getClass().getSimpleName() +
                            "' : " + t.getClass().getSimpleName() + " : " + t.getMessage();
                    log.error(message, t);
                }
            }
        }
        if ((AuditPath.isAuditEnabled) && (!statementOutputHooks.isEmpty())) {
            for (StatementResultListener listener : statementOutputHooks) {
                listener.update(newEventArr, oldEventArr, epStatement.getName(), epStatement, epServiceProvider);
            }
        }
    }

    /**
     * Dispatches when the statement is stopped any remaining results.
     */
    public void dispatchOnStop() {
        ArrayDeque<UniformPair<EventBean[]>> dispatches = lastResults.get();
        if (dispatches.isEmpty()) {
            return;
        }
        execute();

        lastResults = new ThreadLocal<ArrayDeque<UniformPair<EventBean[]>>>() {
            protected synchronized ArrayDeque<UniformPair<EventBean[]>> initialValue() {
                return new ArrayDeque<UniformPair<EventBean[]>>();
            }
        };
    }
}
