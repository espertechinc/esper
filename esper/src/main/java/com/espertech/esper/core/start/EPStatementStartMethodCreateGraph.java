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
package com.espertech.esper.core.start;

import com.espertech.esper.client.EventType;
import com.espertech.esper.core.context.factory.StatementAgentInstanceFactoryNoAgentInstance;
import com.espertech.esper.core.context.util.AgentInstanceContext;
import com.espertech.esper.core.service.EPServicesContext;
import com.espertech.esper.core.service.StatementContext;
import com.espertech.esper.epl.expression.core.ExprValidationException;
import com.espertech.esper.epl.spec.CreateDataFlowDesc;
import com.espertech.esper.core.service.speccompiled.StatementSpecCompiled;
import com.espertech.esper.view.ViewProcessingException;
import com.espertech.esper.view.ZeroDepthStreamNoIterate;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Collections;

/**
 * Starts and provides the stop method for EPL statements.
 */
public class EPStatementStartMethodCreateGraph extends EPStatementStartMethodBase {
    private static final Logger log = LoggerFactory.getLogger(EPStatementStartMethodCreateGraph.class);

    public EPStatementStartMethodCreateGraph(StatementSpecCompiled statementSpec) {
        super(statementSpec);
    }

    public EPStatementStartResult startInternal(final EPServicesContext services, StatementContext statementContext, boolean isNewStatement, boolean isRecoveringStatement, boolean isRecoveringResilient) throws ExprValidationException, ViewProcessingException {
        final CreateDataFlowDesc createGraphDesc = statementSpec.getCreateGraphDesc();
        final AgentInstanceContext agentInstanceContext = getDefaultAgentInstanceContext(statementContext);

        // define output event type
        String typeName = "EventType_Graph_" + createGraphDesc.getGraphName();
        EventType resultType = services.getEventAdapterService().createAnonymousMapType(typeName, Collections.<String, Object>emptyMap(), true);

        services.getDataFlowService().addStartGraph(createGraphDesc, statementContext, services, agentInstanceContext, isNewStatement);

        EPStatementStopMethod stopMethod = new EPStatementStopMethod() {
            public void stop() {
                services.getDataFlowService().stopGraph(createGraphDesc.getGraphName());
            }
        };

        EPStatementDestroyMethod destroyMethod = new EPStatementDestroyMethod() {
            public void destroy() {
                services.getDataFlowService().removeGraph(createGraphDesc.getGraphName());
            }
        };

        ZeroDepthStreamNoIterate resultView = new ZeroDepthStreamNoIterate(resultType);
        statementContext.setStatementAgentInstanceFactory(new StatementAgentInstanceFactoryNoAgentInstance(resultView));
        return new EPStatementStartResult(resultView, stopMethod, destroyMethod);
    }
}
