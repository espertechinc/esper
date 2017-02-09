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

import com.espertech.esper.client.dataflow.EPDataFlowRuntime;
import com.espertech.esper.core.context.util.AgentInstanceContext;
import com.espertech.esper.core.service.EPServicesContext;
import com.espertech.esper.core.service.StatementContext;
import com.espertech.esper.epl.expression.core.ExprValidationException;
import com.espertech.esper.epl.spec.CreateDataFlowDesc;

public interface DataFlowService extends EPDataFlowRuntime {

    public void addStartGraph(CreateDataFlowDesc desc, StatementContext statementContext, EPServicesContext servicesContext, AgentInstanceContext agentInstanceContext, boolean newStatement)
            throws ExprValidationException;

    public void removeGraph(String graphName);

    public void stopGraph(String graphName);

    public void destroy();
}
