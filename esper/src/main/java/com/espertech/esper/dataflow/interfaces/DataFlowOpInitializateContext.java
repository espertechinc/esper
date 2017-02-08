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
package com.espertech.esper.dataflow.interfaces;

import com.espertech.esper.client.EPServiceProvider;
import com.espertech.esper.core.context.util.AgentInstanceContext;
import com.espertech.esper.core.service.EPRuntimeEventSender;
import com.espertech.esper.core.service.EPServicesContext;
import com.espertech.esper.core.service.StatementContext;

import java.lang.annotation.Annotation;
import java.util.Map;

public class DataFlowOpInitializateContext {

    private final String dataflowName;
    private final String dataflowInstanceId;
    private final Object dataflowInstanceUserObject;
    private final Map<Integer, DataFlowOpInputPort> inputPorts;
    private final Map<Integer, DataFlowOpOutputPort> outputPorts;
    private final StatementContext statementContext;
    private final EPServicesContext servicesContext;
    private final AgentInstanceContext agentInstanceContext;
    private final EPRuntimeEventSender runtimeEventSender;
    private final EPServiceProvider engine;
    private final Annotation[] operatorAnnotations;

    public DataFlowOpInitializateContext(String dataflowName, String dataflowInstanceId, Object dataflowInstanceUserObject, Map<Integer, DataFlowOpInputPort> inputPorts, Map<Integer, DataFlowOpOutputPort> outputPorts, StatementContext statementContext, EPServicesContext servicesContext, AgentInstanceContext agentInstanceContext, EPRuntimeEventSender runtimeEventSender, EPServiceProvider engine, Annotation[] operatorAnnotations) {
        this.dataflowName = dataflowName;
        this.dataflowInstanceId = dataflowInstanceId;
        this.dataflowInstanceUserObject = dataflowInstanceUserObject;
        this.inputPorts = inputPorts;
        this.outputPorts = outputPorts;
        this.statementContext = statementContext;
        this.servicesContext = servicesContext;
        this.agentInstanceContext = agentInstanceContext;
        this.runtimeEventSender = runtimeEventSender;
        this.engine = engine;
        this.operatorAnnotations = operatorAnnotations;
    }

    public String getDataflowName() {
        return dataflowName;
    }

    public String getDataflowInstanceId() {
        return dataflowInstanceId;
    }

    public Object getDataflowInstanceUserObject() {
        return dataflowInstanceUserObject;
    }

    public StatementContext getStatementContext() {
        return statementContext;
    }

    public EPServicesContext getServicesContext() {
        return servicesContext;
    }

    public AgentInstanceContext getAgentInstanceContext() {
        return agentInstanceContext;
    }

    public Map<Integer, DataFlowOpInputPort> getInputPorts() {
        return inputPorts;
    }

    public Map<Integer, DataFlowOpOutputPort> getOutputPorts() {
        return outputPorts;
    }

    public EPRuntimeEventSender getRuntimeEventSender() {
        return runtimeEventSender;
    }

    public EPServiceProvider getEngine() {
        return engine;
    }

    public Annotation[] getOperatorAnnotations() {
        return operatorAnnotations;
    }
}
