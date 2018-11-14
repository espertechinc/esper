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
package com.espertech.esper.common.internal.epl.dataflow.core;

import com.espertech.esper.common.client.dataflow.core.*;
import com.espertech.esper.common.internal.collection.Pair;
import com.espertech.esper.common.internal.context.aifactory.createdataflow.DataflowDesc;
import com.espertech.esper.common.internal.context.util.AgentInstanceContext;
import com.espertech.esper.common.internal.epl.dataflow.interfaces.DataFlowOpCloseContext;
import com.espertech.esper.common.internal.epl.dataflow.interfaces.DataFlowOpOpenContext;
import com.espertech.esper.common.internal.epl.dataflow.interfaces.DataFlowOperatorLifecycle;
import com.espertech.esper.common.internal.epl.dataflow.realize.OperatorStatisticsProvider;
import com.espertech.esper.common.internal.epl.dataflow.runnables.CompletionListener;
import com.espertech.esper.common.internal.epl.dataflow.runnables.GraphSourceRunnable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.atomic.AtomicInteger;

public class EPDataFlowInstanceImpl implements EPDataFlowInstance {
    private static final Logger log = LoggerFactory.getLogger(EPDataFlowInstanceImpl.class);

    private final Object dataFlowInstanceUserObject;
    private final String dataFlowInstanceId;
    private final OperatorStatisticsProvider statistics;
    private final DataflowDesc dataflowDesc;
    private final AgentInstanceContext agentInstanceContext;
    private final Map<Integer, Pair<Object, Boolean>> operators;
    private final List<GraphSourceRunnable> sourceRunnables;
    private final EPDataFlowInstanceStatistics statisticsProvider;
    private final Map<String, Object> parametersURIs;

    private EPDataFlowState state;
    private List<Thread> threads;
    private List<CountDownLatch> joinedThreadLatches;
    private Thread runCurrentThread;

    public EPDataFlowInstanceImpl(Object dataFlowInstanceUserObject, String dataFlowInstanceId, OperatorStatisticsProvider statistics, Map<Integer, Object> operators, List<GraphSourceRunnable> sourceRunnables, DataflowDesc dataflowDesc, AgentInstanceContext agentInstanceContext, EPDataFlowInstanceStatistics statisticsProvider, Map<String, Object> parametersURIs) {
        this.dataFlowInstanceUserObject = dataFlowInstanceUserObject;
        this.dataFlowInstanceId = dataFlowInstanceId;
        this.statistics = statistics;
        this.dataflowDesc = dataflowDesc;
        this.agentInstanceContext = agentInstanceContext;
        this.sourceRunnables = sourceRunnables;
        this.statisticsProvider = statisticsProvider;
        this.parametersURIs = parametersURIs;

        setState(EPDataFlowState.INSTANTIATED);
        this.operators = new TreeMap<Integer, Pair<Object, Boolean>>();
        for (Map.Entry<Integer, Object> entry : operators.entrySet()) {
            this.operators.put(entry.getKey(), new Pair<Object, Boolean>(entry.getValue(), false));
        }
    }

    public String getDataFlowName() {
        return dataflowDesc.getDataflowName();
    }

    public String getDataFlowDeploymentId() {
        return dataflowDesc.getStatementContext().getDeploymentId();
    }

    public EPDataFlowState getState() {
        return state;
    }

    public synchronized void run() throws IllegalStateException, EPDataFlowExecutionException, EPDataFlowCancellationException {
        checkExecCompleteState();
        checkExecCancelledState();
        checkExecRunningState();
        String dataFlowName = dataflowDesc.getDataflowName();

        if (sourceRunnables.size() != 1) {
            throw new UnsupportedOperationException("The data flow '" + dataFlowName + "' has zero or multiple sources and requires the use of the start method instead");
        }

        callOperatorOpen();

        GraphSourceRunnable sourceRunnable = sourceRunnables.get(0);
        setState(EPDataFlowState.RUNNING);
        runCurrentThread = Thread.currentThread();
        try {
            sourceRunnable.runSync();
        } catch (InterruptedException ex) {
            callOperatorClose();
            setState(EPDataFlowState.CANCELLED);
            throw new EPDataFlowCancellationException("Data flow '" + dataFlowName + "' execution was cancelled", dataFlowName);
        } catch (Throwable t) {
            callOperatorClose();
            setState(EPDataFlowState.COMPLETE);
            throw new EPDataFlowExecutionException("Exception encountered running data flow '" + dataFlowName + "': " + t.getMessage(), t, dataFlowName);
        }
        callOperatorClose();
        if (state != EPDataFlowState.CANCELLED) {
            setState(EPDataFlowState.COMPLETE);
        }
    }

    public void start() throws IllegalStateException {
        checkExecCompleteState();
        checkExecCancelledState();
        checkExecRunningState();

        callOperatorOpen();

        final AtomicInteger countdown = new AtomicInteger(sourceRunnables.size());
        threads = new ArrayList<Thread>();
        for (int i = 0; i < sourceRunnables.size(); i++) {
            GraphSourceRunnable runnable = sourceRunnables.get(i);
            String threadName = "esper." + dataflowDesc.getDataflowName() + "-" + i;
            Thread thread = new Thread(runnable, threadName);
            thread.setContextClassLoader(agentInstanceContext.getClasspathImportServiceRuntime().getClassLoader());
            thread.setDaemon(true);
            runnable.addCompletionListener(new CompletionListener() {
                public void completed() {
                    int remaining = countdown.decrementAndGet();
                    if (remaining == 0) {
                        EPDataFlowInstanceImpl.this.completed();
                    }
                }
            });
            threads.add(thread);
            thread.start();
        }
        setState(EPDataFlowState.RUNNING);
    }

    public synchronized EPDataFlowInstanceCaptive startCaptive() {
        checkExecCompleteState();
        checkExecCancelledState();
        checkExecRunningState();
        setState(EPDataFlowState.RUNNING);

        callOperatorOpen();

        Map<String, EPDataFlowEmitterOperator> emitters = new HashMap<String, EPDataFlowEmitterOperator>();
        for (Pair<Object, Boolean> operatorStatePair : operators.values()) {
            if (operatorStatePair.getFirst() instanceof EPDataFlowEmitterOperator) {
                EPDataFlowEmitterOperator emitterOp = (EPDataFlowEmitterOperator) operatorStatePair.getFirst();
                emitters.put(emitterOp.getName(), emitterOp);
            }
        }

        return new EPDataFlowInstanceCaptive(emitters, sourceRunnables);
    }

    public void join() throws IllegalStateException, InterruptedException {
        String dataFlowName = dataflowDesc.getDataflowName();
        if (state == EPDataFlowState.INSTANTIATED) {
            throw new IllegalStateException("Data flow '" + dataFlowName + "' instance has not been executed, please use join after start or run");
        }
        if (state == EPDataFlowState.CANCELLED) {
            throw new IllegalStateException("Data flow '" + dataFlowName + "' instance has been cancelled and cannot be joined");
        }

        // latch used for non-blocking start
        if (threads != null) {
            for (Thread thread : threads) {
                thread.join();
            }
        } else {
            CountDownLatch latch = new CountDownLatch(1);
            synchronized (this) {
                if (joinedThreadLatches == null) {
                    joinedThreadLatches = new ArrayList<CountDownLatch>();
                }
                joinedThreadLatches.add(latch);
            }
            if (state != EPDataFlowState.COMPLETE) {
                latch.await();
            }
        }
    }

    public void cancel() {
        if (state == EPDataFlowState.COMPLETE || state == EPDataFlowState.CANCELLED) {
            return;
        }
        if (state == EPDataFlowState.INSTANTIATED) {
            setState(EPDataFlowState.CANCELLED);
            sourceRunnables.clear();
            callOperatorClose();
            return;
        }

        // handle async start
        if (threads != null) {
            for (GraphSourceRunnable runnable : sourceRunnables) {
                runnable.shutdown();
            }
            for (Thread thread : threads) {
                if (thread.isAlive() && !thread.isInterrupted()) {
                    thread.interrupt();
                }
            }
        } else {
            // handle run
            if (runCurrentThread != null) {
                runCurrentThread.interrupt();
            }
            runCurrentThread = null;
        }

        callOperatorClose();

        setState(EPDataFlowState.CANCELLED);
        sourceRunnables.clear();
    }

    public EPDataFlowInstanceStatistics getStatistics() {
        return statisticsProvider;
    }

    public Object getUserObject() {
        return dataFlowInstanceUserObject;
    }

    public String getInstanceId() {
        return dataFlowInstanceId;
    }

    public Map<String, Object> getParameters() {
        return parametersURIs;
    }

    public synchronized void completed() {
        if (state != EPDataFlowState.CANCELLED) {
            setState(EPDataFlowState.COMPLETE);
        }

        callOperatorClose();

        if (joinedThreadLatches != null) {
            for (CountDownLatch joinedThread : joinedThreadLatches) {
                joinedThread.countDown();
            }
        }
    }

    private void callOperatorOpen() {
        for (Integer opNum : dataflowDesc.getOperatorBuildOrder()) {
            Pair<Object, Boolean> operatorStatePair = operators.get(opNum);
            if (operatorStatePair.getFirst() instanceof DataFlowOperatorLifecycle) {
                try {
                    DataFlowOperatorLifecycle lf = (DataFlowOperatorLifecycle) operatorStatePair.getFirst();
                    lf.open(new DataFlowOpOpenContext(opNum));
                } catch (RuntimeException ex) {
                    throw new EPDataFlowExecutionException("Exception encountered opening data flow 'FlowOne' in operator " + operatorStatePair.getFirst().getClass().getSimpleName() + ": " + ex.getMessage(), ex, dataflowDesc.getDataflowName());
                }
            }
        }
    }

    private synchronized void callOperatorClose() {
        for (Integer opNum : dataflowDesc.getOperatorBuildOrder()) {
            Pair<Object, Boolean> operatorStatePair = operators.get(opNum);
            if (operatorStatePair.getFirst() instanceof DataFlowOperatorLifecycle && !operatorStatePair.getSecond()) {
                try {
                    DataFlowOperatorLifecycle lf = (DataFlowOperatorLifecycle) operatorStatePair.getFirst();
                    lf.close(new DataFlowOpCloseContext(opNum));
                } catch (RuntimeException ex) {
                    log.error("Exception encountered closing data flow '" + dataflowDesc.getDataflowName() + "': " + ex.getMessage(), ex);
                }
                operatorStatePair.setSecond(true);
            }
        }
    }

    private void checkExecCompleteState() {
        if (state == EPDataFlowState.COMPLETE) {
            throw new IllegalStateException("Data flow '" + dataflowDesc.getDataflowName() + "' instance has already completed, please use instantiate to run the data flow again");
        }
    }

    private void checkExecRunningState() {
        if (state == EPDataFlowState.RUNNING) {
            throw new IllegalStateException("Data flow '" + dataflowDesc.getDataflowName() + "' instance is already running");
        }
    }

    private void checkExecCancelledState() {
        if (state == EPDataFlowState.CANCELLED) {
            throw new IllegalStateException("Data flow '" + dataflowDesc.getDataflowName() + "' instance has been cancelled and cannot be run or started");
        }
    }

    private void setState(EPDataFlowState newState) {
        agentInstanceContext.getAuditProvider().dataflowTransition(dataflowDesc.getDataflowName(), dataFlowInstanceId, state, newState, agentInstanceContext);
        this.state = newState;
    }
}
