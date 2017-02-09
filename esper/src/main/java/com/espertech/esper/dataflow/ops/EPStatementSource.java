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
package com.espertech.esper.dataflow.ops;

import com.espertech.esper.client.*;
import com.espertech.esper.client.dataflow.EPDataFlowEPStatementFilter;
import com.espertech.esper.client.dataflow.EPDataFlowIRStreamCollector;
import com.espertech.esper.client.dataflow.EPDataFlowIRStreamCollectorContext;
import com.espertech.esper.client.dataflow.EPDataFlowSignal;
import com.espertech.esper.core.service.StatementLifecycleEvent;
import com.espertech.esper.core.service.StatementLifecycleObserver;
import com.espertech.esper.core.service.StatementLifecycleSvc;
import com.espertech.esper.dataflow.annotations.DataFlowContext;
import com.espertech.esper.dataflow.annotations.DataFlowOpParameter;
import com.espertech.esper.dataflow.annotations.DataFlowOperator;
import com.espertech.esper.dataflow.interfaces.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.HashMap;
import java.util.Map;
import java.util.Queue;
import java.util.concurrent.LinkedBlockingQueue;

@DataFlowOperator
public class EPStatementSource implements DataFlowSourceOperator, DataFlowOpLifecycle, StatementLifecycleObserver {
    private static final Logger log = LoggerFactory.getLogger(EPStatementSource.class);

    @DataFlowOpParameter
    private String statementName;

    @DataFlowOpParameter
    private EPDataFlowEPStatementFilter statementFilter;

    @DataFlowOpParameter
    private EPDataFlowIRStreamCollector collector;

    @DataFlowContext
    private EPDataFlowEmitter graphContext;

    private StatementLifecycleSvc statementLifecycleSvc;
    private Map<EPStatement, StatementAwareUpdateListener> listeners = new HashMap<EPStatement, StatementAwareUpdateListener>();
    private LinkedBlockingQueue<Object> emittables = new LinkedBlockingQueue<Object>();
    private boolean submitEventBean;

    private ThreadLocal<EPDataFlowIRStreamCollectorContext> collectorDataTL = new ThreadLocal<EPDataFlowIRStreamCollectorContext>() {
        protected synchronized EPDataFlowIRStreamCollectorContext initialValue() {
            return null;
        }
    };

    public DataFlowOpInitializeResult initialize(DataFlowOpInitializateContext context) throws Exception {

        if (context.getOutputPorts().size() != 1) {
            throw new IllegalArgumentException("EPStatementSource operator requires one output stream but produces " + context.getOutputPorts().size() + " streams");
        }

        if (statementName == null && statementFilter == null) {
            throw new EPException("Failed to find required 'statementName' or 'statementFilter' parameter");
        }
        if (statementName != null && statementFilter != null) {
            throw new EPException("Both 'statementName' or 'statementFilter' parameters were provided, only either one is expected");
        }

        DataFlowOpOutputPort portZero = context.getOutputPorts().get(0);
        if (portZero != null && portZero.getOptionalDeclaredType() != null && portZero.getOptionalDeclaredType().isWildcard()) {
            submitEventBean = true;
        }

        statementLifecycleSvc = context.getServicesContext().getStatementLifecycleSvc();
        return null;
    }

    public void next() throws InterruptedException {
        Object next = emittables.take();
        if (next instanceof EPDataFlowSignal) {
            EPDataFlowSignal signal = (EPDataFlowSignal) next;
            graphContext.submitSignal(signal);
        } else if (next instanceof PortAndMessagePair) {
            PortAndMessagePair pair = (PortAndMessagePair) next;
            graphContext.submitPort(pair.getPort(), pair.getMessage());
        } else {
            graphContext.submit(next);
        }
    }

    public synchronized void open(DataFlowOpOpenContext openContext) {
        // start observing statement management
        statementLifecycleSvc.addObserver(this);

        if (statementName != null) {
            EPStatement stmt = statementLifecycleSvc.getStatementByName(statementName);
            if (stmt != null) {
                addStatement(stmt);
            }
        } else {
            String[] statements = statementLifecycleSvc.getStatementNames();
            for (String name : statements) {
                EPStatement stmt = statementLifecycleSvc.getStatementByName(name);
                if (statementFilter.pass(stmt)) {
                    addStatement(stmt);
                }
            }
        }
    }

    public synchronized void observe(StatementLifecycleEvent theEvent) {
        EPStatement stmt = theEvent.getStatement();
        if (theEvent.getEventType() == StatementLifecycleEvent.LifecycleEventType.STATECHANGE) {
            if (theEvent.getStatement().isStopped() || theEvent.getStatement().isDestroyed()) {
                StatementAwareUpdateListener listener = listeners.remove(stmt);
                if (listener != null) {
                    stmt.removeListener(listener);
                }
            }
            if (theEvent.getStatement().isStarted()) {
                if (statementFilter == null) {
                    if (theEvent.getStatement().getName().equals(statementName)) {
                        addStatement(stmt);
                    }
                } else {
                    if (statementFilter.pass(stmt)) {
                        addStatement(stmt);
                    }
                }
            }
        }
    }

    public void close(DataFlowOpCloseContext openContext) {
        for (Map.Entry<EPStatement, StatementAwareUpdateListener> entry : listeners.entrySet()) {
            try {
                entry.getKey().removeListener(entry.getValue());
            } catch (Exception ex) {
                log.debug("Exception encountered removing listener: " + ex.getMessage(), ex);
                // possible
            }
        }
        listeners.clear();
    }

    private void addStatement(EPStatement stmt) {
        // statement may be added already
        if (listeners.containsKey(stmt)) {
            return;
        }

        // attach listener
        StatementAwareUpdateListener listener;
        if (collector == null) {
            listener = new EmitterUpdateListener(emittables, submitEventBean);
        } else {
            LocalEmitter emitterForCollector = new LocalEmitter(emittables);
            listener = new EmitterCollectorUpdateListener(collector, emitterForCollector, collectorDataTL, submitEventBean);
        }
        stmt.addListener(listener);

        // save listener instance
        listeners.put(stmt, listener);
    }

    public static class EmitterUpdateListener implements StatementAwareUpdateListener {
        private final Queue<Object> queue;
        private final boolean submitEventBean;

        public EmitterUpdateListener(Queue<Object> queue, boolean submitEventBean) {
            this.queue = queue;
            this.submitEventBean = submitEventBean;
        }

        public void update(EventBean[] newEvents, EventBean[] oldEvents, EPStatement statement, EPServiceProvider epServiceProvider) {
            if (newEvents != null) {
                for (EventBean newEvent : newEvents) {
                    if (submitEventBean) {
                        queue.add(newEvent);
                    } else {
                        Object underlying = newEvent.getUnderlying();
                        queue.add(underlying);
                    }
                }
            }
        }
    }

    public static class EmitterCollectorUpdateListener implements StatementAwareUpdateListener {
        private final EPDataFlowIRStreamCollector collector;
        private final LocalEmitter emitterForCollector;
        private final ThreadLocal<EPDataFlowIRStreamCollectorContext> collectorDataTL;
        private final boolean submitEventBean;

        public EmitterCollectorUpdateListener(EPDataFlowIRStreamCollector collector, LocalEmitter emitterForCollector, ThreadLocal<EPDataFlowIRStreamCollectorContext> collectorDataTL, boolean submitEventBean) {
            this.collector = collector;
            this.emitterForCollector = emitterForCollector;
            this.collectorDataTL = collectorDataTL;
            this.submitEventBean = submitEventBean;
        }

        public void update(EventBean[] newEvents, EventBean[] oldEvents, EPStatement statement, EPServiceProvider epServiceProvider) {

            EPDataFlowIRStreamCollectorContext holder = collectorDataTL.get();
            if (holder == null) {
                holder = new EPDataFlowIRStreamCollectorContext(emitterForCollector, submitEventBean, newEvents, oldEvents, statement, epServiceProvider);
                collectorDataTL.set(holder);
            } else {
                holder.setEpServiceProvider(epServiceProvider);
                holder.setStatement(statement);
                holder.setOldEvents(oldEvents);
                holder.setNewEvents(newEvents);
            }

            collector.collect(holder);
        }
    }

    public static class LocalEmitter implements EPDataFlowEmitter {

        private final LinkedBlockingQueue<Object> queue;

        public LocalEmitter(LinkedBlockingQueue<Object> queue) {
            this.queue = queue;
        }

        public void submit(Object object) {
            queue.add(object);
        }

        public void submitSignal(EPDataFlowSignal signal) {
            queue.add(signal);
        }

        public void submitPort(int portNumber, Object object) {
            queue.add(object);
        }
    }

    public static class PortAndMessagePair {
        private final int port;
        private final Object message;

        public PortAndMessagePair(int port, Object message) {
            this.port = port;
            this.message = message;
        }

        public int getPort() {
            return port;
        }

        public Object getMessage() {
            return message;
        }
    }
}
