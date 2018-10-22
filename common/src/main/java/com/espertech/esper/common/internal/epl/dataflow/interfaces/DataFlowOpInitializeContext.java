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
package com.espertech.esper.common.internal.epl.dataflow.interfaces;

import com.espertech.esper.common.client.dataflow.core.EPDataFlowOperatorParameterProvider;
import com.espertech.esper.common.internal.context.util.AgentInstanceContext;

import java.util.Map;

public class DataFlowOpInitializeContext {

    private final String dataFlowName;
    private final String operatorName;
    private final int operatorNumber;
    private final AgentInstanceContext agentInstanceContext;
    private final Map<String, Object> additionalParameters;
    private final String dataFlowInstanceId;
    private final EPDataFlowOperatorParameterProvider parameterProvider;
    private final DataFlowOperatorFactory dataFlowOperatorFactory;
    private final Object dataflowInstanceUserObject;

    public DataFlowOpInitializeContext(String dataFlowName, String operatorName, int operatorNumber, AgentInstanceContext agentInstanceContext, Map<String, Object> additionalParameters, String dataFlowInstanceId, EPDataFlowOperatorParameterProvider parameterProvider, DataFlowOperatorFactory dataFlowOperatorFactory, Object dataflowInstanceUserObject) {
        this.dataFlowName = dataFlowName;
        this.operatorName = operatorName;
        this.operatorNumber = operatorNumber;
        this.agentInstanceContext = agentInstanceContext;
        this.additionalParameters = additionalParameters;
        this.dataFlowInstanceId = dataFlowInstanceId;
        this.parameterProvider = parameterProvider;
        this.dataFlowOperatorFactory = dataFlowOperatorFactory;
        this.dataflowInstanceUserObject = dataflowInstanceUserObject;
    }

    public AgentInstanceContext getAgentInstanceContext() {
        return agentInstanceContext;
    }

    public Map<String, Object> getAdditionalParameters() {
        return additionalParameters;
    }

    public String getDataFlowInstanceId() {
        return dataFlowInstanceId;
    }

    public EPDataFlowOperatorParameterProvider getParameterProvider() {
        return parameterProvider;
    }

    public String getOperatorName() {
        return operatorName;
    }

    public int getOperatorNumber() {
        return operatorNumber;
    }

    public String getDataFlowName() {
        return dataFlowName;
    }

    public DataFlowOperatorFactory getDataFlowOperatorFactory() {
        return dataFlowOperatorFactory;
    }

    public Object getDataflowInstanceUserObject() {
        return dataflowInstanceUserObject;
    }
}
