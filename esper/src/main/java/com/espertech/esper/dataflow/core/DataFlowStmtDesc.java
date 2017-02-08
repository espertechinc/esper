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

import com.espertech.esper.core.context.util.AgentInstanceContext;
import com.espertech.esper.core.service.EPServicesContext;
import com.espertech.esper.core.service.StatementContext;
import com.espertech.esper.epl.spec.CreateDataFlowDesc;
import com.espertech.esper.epl.spec.GraphOperatorSpec;

import java.lang.annotation.Annotation;
import java.util.Map;

public class DataFlowStmtDesc {
    private final CreateDataFlowDesc graphDesc;
    private final StatementContext statementContext;
    private final EPServicesContext servicesContext;
    private final AgentInstanceContext agentInstanceContext;
    private final Map<GraphOperatorSpec, Annotation[]> operatorAnnotations;

    public DataFlowStmtDesc(CreateDataFlowDesc graphDesc, StatementContext statementContext, EPServicesContext servicesContext, AgentInstanceContext agentInstanceContext, Map<GraphOperatorSpec, Annotation[]> operatorAnnotations) {
        this.graphDesc = graphDesc;
        this.statementContext = statementContext;
        this.servicesContext = servicesContext;
        this.agentInstanceContext = agentInstanceContext;
        this.operatorAnnotations = operatorAnnotations;
    }

    public CreateDataFlowDesc getGraphDesc() {
        return graphDesc;
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

    public Map<GraphOperatorSpec, Annotation[]> getOperatorAnnotations() {
        return operatorAnnotations;
    }
}
