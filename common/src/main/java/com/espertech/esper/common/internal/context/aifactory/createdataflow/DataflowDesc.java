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
package com.espertech.esper.common.internal.context.aifactory.createdataflow;

import com.espertech.esper.common.client.EventType;
import com.espertech.esper.common.internal.context.util.StatementContext;
import com.espertech.esper.common.internal.epl.dataflow.interfaces.DataFlowOperatorFactory;
import com.espertech.esper.common.internal.epl.dataflow.realize.LogicalChannel;
import com.espertech.esper.common.internal.epl.dataflow.util.OperatorMetadataDescriptor;

import java.util.List;
import java.util.Map;
import java.util.Set;

public class DataflowDesc {
    private String dataflowName;
    private Map<String, EventType> declaredTypes;
    private Map<Integer, OperatorMetadataDescriptor> operatorMetadata;
    private Map<Integer, DataFlowOperatorFactory> operatorFactories;
    private Set<Integer> operatorBuildOrder;
    private List<LogicalChannel> logicalChannels;

    private StatementContext statementContext;

    public String getDataflowName() {
        return dataflowName;
    }

    public Map<Integer, DataFlowOperatorFactory> getOperatorFactories() {
        return operatorFactories;
    }

    public Map<Integer, OperatorMetadataDescriptor> getOperatorMetadata() {
        return operatorMetadata;
    }

    public Set<Integer> getOperatorBuildOrder() {
        return operatorBuildOrder;
    }

    public Map<String, EventType> getDeclaredTypes() {
        return declaredTypes;
    }

    public void setDataflowName(String dataflowName) {
        this.dataflowName = dataflowName;
    }

    public void setDeclaredTypes(Map<String, EventType> declaredTypes) {
        this.declaredTypes = declaredTypes;
    }

    public void setOperatorFactories(Map<Integer, DataFlowOperatorFactory> operatorFactories) {
        this.operatorFactories = operatorFactories;
    }

    public void setOperatorMetadata(Map<Integer, OperatorMetadataDescriptor> operatorMetadata) {
        this.operatorMetadata = operatorMetadata;
    }

    public void setOperatorBuildOrder(Set<Integer> operatorBuildOrder) {
        this.operatorBuildOrder = operatorBuildOrder;
    }

    public StatementContext getStatementContext() {
        return statementContext;
    }

    public void setStatementContext(StatementContext statementContext) {
        this.statementContext = statementContext;
    }

    public List<LogicalChannel> getLogicalChannels() {
        return logicalChannels;
    }

    public void setLogicalChannels(List<LogicalChannel> logicalChannels) {
        this.logicalChannels = logicalChannels;
    }
}
