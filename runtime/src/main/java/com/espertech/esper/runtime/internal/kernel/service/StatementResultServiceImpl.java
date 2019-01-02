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
package com.espertech.esper.runtime.internal.kernel.service;

import com.espertech.esper.common.client.EventBean;
import com.espertech.esper.common.internal.collection.UniformPair;
import com.espertech.esper.common.internal.context.module.StatementInformationalsRuntime;
import com.espertech.esper.common.internal.context.util.StatementContext;
import com.espertech.esper.common.internal.context.util.StatementDispatchTLEntry;
import com.espertech.esper.common.internal.context.util.StatementResultService;
import com.espertech.esper.common.internal.epl.expression.core.ExprEvaluator;
import com.espertech.esper.common.internal.event.core.EventBeanUtility;
import com.espertech.esper.common.internal.event.core.NaturalEventBean;
import com.espertech.esper.common.internal.metrics.stmtmetrics.StatementMetricHandle;
import com.espertech.esper.runtime.client.EPSubscriberException;
import com.espertech.esper.runtime.client.UpdateListener;
import com.espertech.esper.runtime.internal.kernel.statement.EPStatementListenerSet;
import com.espertech.esper.runtime.internal.kernel.statement.EPStatementSPI;
import com.espertech.esper.runtime.internal.kernel.thread.OutboundUnitRunnable;
import com.espertech.esper.runtime.internal.metrics.instrumentation.InstrumentationHelper;
import com.espertech.esper.runtime.internal.subscriber.ResultDeliveryStrategy;
import com.espertech.esper.runtime.internal.subscriber.ResultDeliveryStrategyFactory;
import com.espertech.esper.runtime.internal.subscriber.ResultDeliveryStrategyInvalidException;
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

    private final StatementInformationalsRuntime statementInformationals;
    private final EPServicesContext epServicesContext;
    private final boolean outboundThreading;

    // Part of the statement context
    private EPStatementSPI epStatement;
    private EPRuntimeSPI runtime;
    private StatementMetricHandle statementMetricHandle;

    private boolean forClauseDelivery = false;
    private ExprEvaluator groupDeliveryExpressions;

    // For natural delivery derived out of select-clause expressions
    private Class[] selectClauseTypes;
    private String[] selectClauseColumnNames;

    // Listeners and subscribers and derived information
    private EPStatementListenerSet statementListenerSet;
    private boolean isMakeNatural;
    private boolean isMakeSynthetic;
    private ResultDeliveryStrategy statementResultNaturalStrategy;

    /**
     * Buffer for holding dispatchable events.
     */
    protected ThreadLocal<StatementDispatchTLEntry> statementDispatchTL = new ThreadLocal<StatementDispatchTLEntry>() {
        protected synchronized StatementDispatchTLEntry initialValue() {
            return new StatementDispatchTLEntry();
        }
    };

    public StatementResultServiceImpl(StatementInformationalsRuntime statementInformationals,
                                      EPServicesContext epServicesContext) {
        this.statementInformationals = statementInformationals;
        this.epServicesContext = epServicesContext;
        this.outboundThreading = epServicesContext.getThreadingService().isOutboundThreading();
        isMakeSynthetic = statementInformationals.isAlwaysSynthesizeOutputEvents();
    }

    public void setContext(EPStatementSPI epStatement, EPRuntimeSPI runtime) {
        this.epStatement = epStatement;
        this.runtime = runtime;
        this.statementMetricHandle = epStatement.getStatementContext().getEpStatementHandle().getMetricsHandle();
    }

    public void setSelectClause(Class[] selectClauseTypes, String[] selectClauseColumnNames,
                                boolean forClauseDelivery, ExprEvaluator groupDeliveryExpressions) {
        this.selectClauseTypes = selectClauseTypes;
        this.selectClauseColumnNames = selectClauseColumnNames;
        this.forClauseDelivery = forClauseDelivery;
        this.groupDeliveryExpressions = groupDeliveryExpressions;
    }

    public ThreadLocal<StatementDispatchTLEntry> getDispatchTL() {
        return statementDispatchTL;
    }

    public int getStatementId() {
        return epStatement.getStatementContext().getStatementId();
    }

    public boolean isMakeSynthetic() {
        return isMakeSynthetic;
    }

    public boolean isMakeNatural() {
        return isMakeNatural;
    }

    public String getStatementName() {
        return epStatement.getName();
    }

    public EPStatementListenerSet getStatementListenerSet() {
        return statementListenerSet;
    }

    public void setUpdateListeners(EPStatementListenerSet updateListeners, boolean isRecovery) {
        // indicate that listeners were updated for potential persistence of listener set, once the statement context is known
        if (epStatement != null) {
            if (!isRecovery) {
                StatementContext stmtCtx = epStatement.getStatementContext();
                epServicesContext.getEpServicesHA().getListenerRecoveryService().put(stmtCtx.getStatementId(), stmtCtx.getStatementName(), updateListeners.getListeners());
            }
        }

        this.statementListenerSet = updateListeners;

        isMakeNatural = statementListenerSet.getSubscriber() != null;
        isMakeSynthetic = !(statementListenerSet.getListeners().length == 0) || statementInformationals.isAlwaysSynthesizeOutputEvents();

        if (statementListenerSet.getSubscriber() == null) {
            statementResultNaturalStrategy = null;
            isMakeNatural = false;
            return;
        }

        try {
            statementResultNaturalStrategy = ResultDeliveryStrategyFactory.create(epStatement, statementListenerSet.getSubscriber(), statementListenerSet.getSubscriberMethodName(),
                selectClauseTypes, selectClauseColumnNames, runtime.getURI(), runtime.getServicesContext().getClasspathImportServiceRuntime());
            isMakeNatural = true;
        } catch (ResultDeliveryStrategyInvalidException ex) {
            throw new EPSubscriberException(ex.getMessage(), ex);
        }
    }

    // Called by OutputProcessView
    public void indicate(UniformPair<EventBean[]> results, StatementDispatchTLEntry dispatchTLEntry) {
        if (results != null) {
            if (statementMetricHandle.isEnabled()) {
                int numIStream = (results.getFirst() != null) ? results.getFirst().length : 0;
                int numRStream = (results.getSecond() != null) ? results.getSecond().length : 0;
                epServicesContext.getMetricReportingService().accountOutput(statementMetricHandle, numIStream, numRStream, epStatement, runtime);
            }

            ArrayDeque<UniformPair<EventBean[]>> lastResults = dispatchTLEntry.getResults();
            if ((results.getFirst() != null) && (results.getFirst().length != 0)) {
                lastResults.add(results);
            } else if ((results.getSecond() != null) && (results.getSecond().length != 0)) {
                lastResults.add(results);
            }
        }
    }

    public void execute(StatementDispatchTLEntry dispatchTLEntry) {
        ArrayDeque<UniformPair<EventBean[]>> dispatches = dispatchTLEntry.getResults();

        UniformPair<EventBean[]> events = EventBeanUtility.flattenList(dispatches);

        if (InstrumentationHelper.ENABLED) {
            InstrumentationHelper.get().qStatementResultExecute(events, epStatement.getDeploymentId(), epStatement.getStatementId(), epStatement.getName(), Thread.currentThread().getId());
            InstrumentationHelper.get().aStatementResultExecute();
        }

        if (outboundThreading) {
            epServicesContext.getThreadingService().submitOutbound(new OutboundUnitRunnable(events, this));
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
        if (groupDeliveryExpressions == null) {
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

    public void clearDeliveriesRemoveStream(EventBean[] removedEvents) {
        StatementDispatchTLEntry entry = getDispatchTL().get();
        Iterator<UniformPair<EventBean[]>> it = entry.getResults().iterator();
        while (it.hasNext()) {
            UniformPair<EventBean[]> pair = it.next();
            if (pair.getSecond() == null) {
                continue;
            }
            boolean containsDeleted = false;
            for (EventBean removedEvent : removedEvents) {
                for (EventBean dispatchEvent : pair.getSecond()) {
                    if (removedEvent == dispatchEvent) {
                        containsDeleted = true;
                        break;
                    }
                }
                if (containsDeleted) {
                    break;
                }
            }
            if (containsDeleted) {
                it.remove();
            }
        }
        if (!entry.getResults().isEmpty()) {
            return;
        }
        entry.setDispatchWaiting(false);
        epServicesContext.getDispatchService().removeAll(epStatement.getDispatchChildView());
    }

    public EPServicesContext getEpServicesContext() {
        return epServicesContext;
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

            eventsPerStream[0] = evalEvent;
            Object key = groupDeliveryExpressions.evaluate(eventsPerStream, true, epStatement.getStatementContext());

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
                listener.update(newEventArr, oldEventArr, epStatement, runtime);
            } catch (Throwable t) {
                String message = "Unexpected exception invoking listener update method on listener class '" + listener.getClass().getSimpleName() +
                    "' : " + t.getClass().getSimpleName() + " : " + t.getMessage();
                log.error(message, t);
            }
        }
    }
}
