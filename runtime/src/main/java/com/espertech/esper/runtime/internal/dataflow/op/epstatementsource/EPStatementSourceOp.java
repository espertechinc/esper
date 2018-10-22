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
package com.espertech.esper.runtime.internal.dataflow.op.epstatementsource;

import com.espertech.esper.common.client.dataflow.annotations.DataFlowContext;
import com.espertech.esper.common.client.dataflow.core.EPDataFlowEPStatementFilter;
import com.espertech.esper.common.client.dataflow.core.EPDataFlowEPStatementFilterContext;
import com.espertech.esper.common.client.dataflow.core.EPDataFlowIRStreamCollector;
import com.espertech.esper.common.client.dataflow.util.EPDataFlowSignal;
import com.espertech.esper.common.internal.context.util.AgentInstanceContext;
import com.espertech.esper.common.internal.epl.dataflow.interfaces.*;
import com.espertech.esper.runtime.client.*;
import com.espertech.esper.runtime.internal.kernel.service.EPRuntimeSPI;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.LinkedBlockingQueue;

public class EPStatementSourceOp implements DataFlowSourceOperator, DataFlowOperatorLifecycle, DeploymentStateListener {
    private static final Logger log = LoggerFactory.getLogger(EPStatementSourceOp.class);

    private final EPStatementSourceFactory factory;
    private final AgentInstanceContext agentInstanceContext;
    private final String statementDeploymentId;
    private final String statementName;
    private final EPDataFlowEPStatementFilter statementFilter;
    private final EPDataFlowIRStreamCollector collector;
    private Map<EPStatement, UpdateListener> listeners = new HashMap<EPStatement, UpdateListener>();

    @DataFlowContext
    private EPDataFlowEmitter graphContext;

    private LinkedBlockingQueue<Object> emittables = new LinkedBlockingQueue<Object>();

    public EPStatementSourceOp(EPStatementSourceFactory factory, AgentInstanceContext agentInstanceContext, String statementDeploymentId, String statementName, EPDataFlowEPStatementFilter statementFilter, EPDataFlowIRStreamCollector collector) {
        this.factory = factory;
        this.agentInstanceContext = agentInstanceContext;
        this.statementDeploymentId = statementDeploymentId;
        this.statementName = statementName;
        this.statementFilter = statementFilter;
        this.collector = collector;
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
        EPRuntimeSPI spi = (EPRuntimeSPI) agentInstanceContext.getRuntime();
        spi.getDeploymentService().addDeploymentStateListener(this);

        if (statementDeploymentId != null && statementName != null) {
            EPStatement stmt = spi.getDeploymentService().getStatement(statementDeploymentId, statementName);
            if (stmt != null) {
                addStatement(stmt);
            }
        } else {
            String[] deployments = spi.getDeploymentService().getDeployments();
            for (String deploymentId : deployments) {
                EPDeployment info = spi.getDeploymentService().getDeployment(deploymentId);
                if (info == null) {
                    continue;
                }
                for (EPStatement stmt : info.getStatements()) {
                    if (statementFilter.pass(toContext(stmt))) {
                        addStatement(stmt);
                    }
                }
            }
        }
    }

    public void onDeployment(DeploymentStateEventDeployed event) {
        for (EPStatement stmt : event.getStatements()) {
            if (statementFilter == null) {
                if (stmt.getDeploymentId().equals(statementDeploymentId) && stmt.getName().equals(statementName)) {
                    addStatement(stmt);
                }
            } else {
                if (statementFilter.pass(toContext(stmt))) {
                    addStatement(stmt);
                }
            }
        }
    }

    public void onUndeployment(DeploymentStateEventUndeployed event) {
        for (EPStatement stmt : event.getStatements()) {
            UpdateListener listener = listeners.remove(stmt);
            if (listener != null) {
                stmt.removeListener(listener);
            }
        }
    }

    public void close(DataFlowOpCloseContext openContext) {
        for (Map.Entry<EPStatement, UpdateListener> entry : listeners.entrySet()) {
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
        UpdateListener listener;
        if (collector == null) {
            listener = new EmitterUpdateListener(emittables, factory.isSubmitEventBean());
        } else {
            LocalEmitter emitterForCollector = new LocalEmitter(emittables);
            listener = new EmitterCollectorUpdateListener(collector, emitterForCollector, factory.isSubmitEventBean());
        }
        stmt.addListener(listener);

        // save listener instance
        listeners.put(stmt, listener);
    }

    private EPDataFlowEPStatementFilterContext toContext(EPStatement stmt) {
        return new EPDataFlowEPStatementFilterContext(stmt.getDeploymentId(), stmt.getName(), stmt);
    }
}
