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
package com.espertech.esper.common.internal.epl.dataflow.runnables;

import com.espertech.esper.common.client.dataflow.core.EPDataFlowExceptionContext;
import com.espertech.esper.common.client.dataflow.core.EPDataFlowExceptionHandler;
import com.espertech.esper.common.client.dataflow.util.EPDataFlowSignal;
import com.espertech.esper.common.client.dataflow.util.EPDataFlowSignalFinalMarker;
import com.espertech.esper.common.internal.context.util.AgentInstanceContext;
import com.espertech.esper.common.internal.epl.dataflow.interfaces.DataFlowSourceOperator;
import com.espertech.esper.common.internal.epl.dataflow.util.DataFlowSignalListener;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.List;

public class GraphSourceRunnable implements BaseRunnable, DataFlowSignalListener {
    private static final Logger log = LoggerFactory.getLogger(GraphSourceRunnable.class);

    private final AgentInstanceContext agentInstanceContext;
    private final DataFlowSourceOperator graphSource;
    private final String dataFlowName;
    private final String instanceId;
    private final String operatorName;
    private final int operatorNumber;
    private final String operatorPrettyPrint;
    private final EPDataFlowExceptionHandler optionalExceptionHandler;
    private final boolean audit;

    private boolean shutdown;
    private List<CompletionListener> completionListeners;

    public GraphSourceRunnable(AgentInstanceContext agentInstanceContext, DataFlowSourceOperator graphSource, String dataFlowName, String instanceId, String operatorName, int operatorNumber, String operatorPrettyPrint, EPDataFlowExceptionHandler optionalExceptionHandler, boolean audit) {
        this.agentInstanceContext = agentInstanceContext;
        this.graphSource = graphSource;
        this.dataFlowName = dataFlowName;
        this.instanceId = instanceId;
        this.operatorName = operatorName;
        this.operatorNumber = operatorNumber;
        this.operatorPrettyPrint = operatorPrettyPrint;
        this.optionalExceptionHandler = optionalExceptionHandler;
        this.audit = audit;
    }

    public void processSignal(EPDataFlowSignal signal) {
        if (signal instanceof EPDataFlowSignalFinalMarker) {
            shutdown = true;
        }
    }

    public void run() {
        try {
            runLoop();
        } catch (InterruptedException ex) {
            log.debug("Interruped runnable: " + ex.getMessage(), ex);
        } catch (RuntimeException ex) {
            log.error("Exception encountered: " + ex.getMessage(), ex);
            handleException(ex);
        }

        invokeCompletionListeners();
    }

    public void runSync() throws InterruptedException {
        try {
            runLoop();
        } catch (InterruptedException ex) {
            log.debug("Interruped runnable: " + ex.getMessage(), ex);
            throw ex;
        } catch (RuntimeException ex) {
            log.error("Exception encountered: " + ex.getMessage(), ex);
            handleException(ex);
            throw ex;
        }
    }

    private void handleException(RuntimeException ex) {
        if (optionalExceptionHandler == null) {
            return;
        }

        optionalExceptionHandler.handle(new EPDataFlowExceptionContext(dataFlowName, operatorName, operatorNumber, operatorPrettyPrint, ex));
    }

    private void runLoop() throws InterruptedException {
        while (true) {
            agentInstanceContext.getAuditProvider().dataflowSource(dataFlowName, instanceId, operatorName, operatorNumber, agentInstanceContext);
            graphSource.next();

            if (shutdown) {
                break;
            }
        }
    }

    private void invokeCompletionListeners() {
        synchronized (this) {
            if (completionListeners != null) {
                for (CompletionListener listener : completionListeners) {
                    listener.completed();
                }
            }
        }
    }

    public synchronized void addCompletionListener(CompletionListener completionListener) {
        if (completionListeners == null) {
            completionListeners = new ArrayList<CompletionListener>();
        }
        completionListeners.add(completionListener);
    }

    public void next() throws InterruptedException {
        agentInstanceContext.getAuditProvider().dataflowSource(dataFlowName, instanceId, operatorName, operatorNumber, agentInstanceContext);
        graphSource.next();
    }

    public void shutdown() {
        shutdown = true;
    }

    public boolean isShutdown() {
        return shutdown;
    }
}
