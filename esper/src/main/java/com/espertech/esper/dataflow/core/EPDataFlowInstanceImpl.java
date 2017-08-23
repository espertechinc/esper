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
package com.espertech.esper.dataflow.core;

import com.espertech.esper.client.annotation.AuditEnum;
import com.espertech.esper.client.dataflow.*;
import com.espertech.esper.collection.Pair;
import com.espertech.esper.dataflow.interfaces.DataFlowOpCloseContext;
import com.espertech.esper.dataflow.interfaces.DataFlowOpLifecycle;
import com.espertech.esper.dataflow.interfaces.DataFlowOpOpenContext;
import com.espertech.esper.dataflow.ops.Emitter;
import com.espertech.esper.dataflow.runnables.CompletionListener;
import com.espertech.esper.dataflow.runnables.GraphSourceRunnable;
import com.espertech.esper.epl.core.engineimport.EngineImportService;
import com.espertech.esper.util.AuditPath;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.atomic.AtomicInteger;

public class EPDataFlowInstanceImpl implements EPDataFlowInstance, CompletionListener {
    private static final Logger log = LoggerFactory.getLogger(EPDataFlowInstanceImpl.class);

    private final String engineURI;
    private final String statementName;
    private final boolean audit;
    private final String dataFlowName;
    private final Object userObject;
    private final String instanceId;
    private volatile EPDataFlowState state;
    private final List<GraphSourceRunnable> sourceRunnables;
    private final Map<Integer, Pair<Object, Boolean>> operators;
    private final Set<Integer> operatorBuildOrder;
    private final EPDataFlowInstanceStatistics statisticsProvider;
    private final Map<String, Object> parameters;
    private final EngineImportService engineImportService;

    private List<CountDownLatch> joinedThreadLatches;
    private List<Thread> threads;
    private Thread runCurrentThread;

    public EPDataFlowInstanceImpl(String engineURI, String statementName, boolean audit, String dataFlowName, Object userObject, String instanceId, EPDataFlowState state, List<GraphSourceRunnable> sourceRunnables, Map<Integer, Object> operators, Set<Integer> operatorBuildOrder, EPDataFlowInstanceStatistics statisticsProvider, Map<String, Object> parameters, EngineImportService engineImportService) {
        this.engineURI = engineURI;
        this.statementName = statementName;
        this.audit = audit;
        this.dataFlowName = dataFlowName;
        this.userObject = userObject;
        this.instanceId = instanceId;
        this.sourceRunnables = sourceRunnables;
        this.operators = new TreeMap<Integer, Pair<Object, Boolean>>();
        for (Map.Entry<Integer, Object> entry : operators.entrySet()) {
            this.operators.put(entry.getKey(), new Pair<Object, Boolean>(entry.getValue(), false));
        }
        this.operatorBuildOrder = operatorBuildOrder;
        this.statisticsProvider = statisticsProvider;
        setState(state);
        this.parameters = parameters;
        this.engineImportService = engineImportService;
    }

    public String getDataFlowName() {
        return dataFlowName;
    }

    public EPDataFlowState getState() {
        return state;
    }

    public Object getUserObject() {
        return userObject;
    }

    public String getInstanceId() {
        return instanceId;
    }

    public Map<String, Object> getParameters() {
        return parameters;
    }

    public synchronized EPDataFlowInstanceCaptive startCaptive() {
        checkExecCompleteState();
        checkExecCancelledState();
        checkExecRunningState();
        setState(EPDataFlowState.RUNNING);

        callOperatorOpen();

        Map<String, Emitter> emitters = new HashMap<String, Emitter>();
        for (Pair<Object, Boolean> operatorStatePair : operators.values()) {
            if (operatorStatePair.getFirst() instanceof Emitter) {
                Emitter emitter = (Emitter) operatorStatePair.getFirst();
                emitters.put(emitter.getName(), emitter);
            }
        }

        return new EPDataFlowInstanceCaptive(emitters, sourceRunnables);
    }

    public synchronized void run() {
        checkExecCompleteState();
        checkExecCancelledState();
        checkExecRunningState();

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

    public synchronized void start() {
        checkExecCompleteState();
        checkExecCancelledState();
        checkExecRunningState();

        callOperatorOpen();

        final AtomicInteger countdown = new AtomicInteger(sourceRunnables.size());
        threads = new ArrayList<Thread>();
        for (int i = 0; i < sourceRunnables.size(); i++) {
            GraphSourceRunnable runnable = sourceRunnables.get(i);
            String threadName = "esper." + dataFlowName + "-" + i;
            Thread thread = new Thread(runnable, threadName);
            thread.setContextClassLoader(engineImportService.getClassLoader());
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

    public void join() throws InterruptedException {
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

    public EPDataFlowInstanceStatistics getStatistics() {
        return statisticsProvider;
    }

    private void checkExecCompleteState() {
        if (state == EPDataFlowState.COMPLETE) {
            throw new IllegalStateException("Data flow '" + dataFlowName + "' instance has already completed, please use instantiate to run the data flow again");
        }
    }

    private void checkExecRunningState() {
        if (state == EPDataFlowState.RUNNING) {
            throw new IllegalStateException("Data flow '" + dataFlowName + "' instance is already running");
        }
    }

    private void checkExecCancelledState() {
        if (state == EPDataFlowState.CANCELLED) {
            throw new IllegalStateException("Data flow '" + dataFlowName + "' instance has been cancelled and cannot be run or started");
        }
    }

    private synchronized void callOperatorClose() {
        for (Integer opNum : operatorBuildOrder) {
            Pair<Object, Boolean> operatorStatePair = operators.get(opNum);
            if (operatorStatePair.getFirst() instanceof DataFlowOpLifecycle && !operatorStatePair.getSecond()) {
                try {
                    DataFlowOpLifecycle lf = (DataFlowOpLifecycle) operatorStatePair.getFirst();
                    lf.close(new DataFlowOpCloseContext());
                } catch (RuntimeException ex) {
                    log.error("Exception encountered closing data flow '" + dataFlowName + "': " + ex.getMessage(), ex);
                }
                operatorStatePair.setSecond(true);
            }
        }
    }

    private void callOperatorOpen() {
        for (Integer opNum : operatorBuildOrder) {
            Pair<Object, Boolean> operatorStatePair = operators.get(opNum);
            if (operatorStatePair.getFirst() instanceof DataFlowOpLifecycle) {
                try {
                    DataFlowOpLifecycle lf = (DataFlowOpLifecycle) operatorStatePair.getFirst();
                    lf.open(new DataFlowOpOpenContext());
                } catch (RuntimeException ex) {
                    throw new EPDataFlowExecutionException("Exception encountered opening data flow 'FlowOne' in operator " + operatorStatePair.getFirst().getClass().getSimpleName() + ": " + ex.getMessage(), ex, dataFlowName);
                }
            }
        }
    }

    private void setState(EPDataFlowState newState) {
        if (audit) {
            AuditPath.auditLog(engineURI, statementName, AuditEnum.DATAFLOW_TRANSITION, "dataflow " + dataFlowName + " instance " + instanceId + " from state " + state + " to state " + newState);
        }
        this.state = newState;
    }
}
