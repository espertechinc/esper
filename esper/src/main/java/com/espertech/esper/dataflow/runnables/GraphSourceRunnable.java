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
package com.espertech.esper.dataflow.runnables;

import com.espertech.esper.client.annotation.AuditEnum;
import com.espertech.esper.client.dataflow.EPDataFlowExceptionContext;
import com.espertech.esper.client.dataflow.EPDataFlowExceptionHandler;
import com.espertech.esper.client.dataflow.EPDataFlowSignal;
import com.espertech.esper.client.dataflow.EPDataFlowSignalFinalMarker;
import com.espertech.esper.dataflow.interfaces.DataFlowSourceOperator;
import com.espertech.esper.dataflow.util.DataFlowSignalListener;
import com.espertech.esper.util.AuditPath;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.List;

public class GraphSourceRunnable implements BaseRunnable, DataFlowSignalListener {
    private static final Logger log = LoggerFactory.getLogger(GraphSourceRunnable.class);

    private final String engineURI;
    private final String statementName;
    private final DataFlowSourceOperator graphSource;
    private final String dataFlowName;
    private final String operatorName;
    private final int operatorNumber;
    private final String operatorPrettyPrint;
    private final EPDataFlowExceptionHandler optionalExceptionHandler;
    private final boolean audit;

    private boolean shutdown;
    private List<CompletionListener> completionListeners;

    public GraphSourceRunnable(String engineURI, String statementName, DataFlowSourceOperator graphSource, String dataFlowName, String operatorName, int operatorNumber, String operatorPrettyPrint, EPDataFlowExceptionHandler optionalExceptionHandler, boolean audit) {
        this.engineURI = engineURI;
        this.statementName = statementName;
        this.graphSource = graphSource;
        this.dataFlowName = dataFlowName;
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
            if (audit) {
                AuditPath.auditLog(engineURI, statementName, AuditEnum.DATAFLOW_SOURCE, "dataflow " + dataFlowName + " operator " + operatorName + "(" + operatorNumber + ") invoking source.next()");
            }
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
        graphSource.next();
    }

    public void shutdown() {
        shutdown = true;
    }

    public boolean isShutdown() {
        return shutdown;
    }
}
