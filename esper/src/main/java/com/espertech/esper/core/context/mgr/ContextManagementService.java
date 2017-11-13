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
package com.espertech.esper.core.context.mgr;

import com.espertech.esper.client.EventType;
import com.espertech.esper.client.context.ContextStateListener;
import com.espertech.esper.core.context.util.AgentInstanceContext;
import com.espertech.esper.core.context.util.ContextDescriptor;
import com.espertech.esper.core.service.EPServicesContext;
import com.espertech.esper.core.service.ExceptionHandlingService;
import com.espertech.esper.epl.expression.core.ExprValidationException;
import com.espertech.esper.epl.spec.CreateContextDesc;

import java.util.Map;
import java.util.concurrent.CopyOnWriteArrayList;

public interface ContextManagementService {
    public void addContextSpec(EPServicesContext servicesContext, AgentInstanceContext agentInstanceContext, CreateContextDesc contextDesc, boolean isRecoveringResilient, EventType statementResultEventType) throws ExprValidationException;

    public int getContextCount();

    public ContextDescriptor getContextDescriptor(String contextName);

    public void addStatement(String contextName, ContextControllerStatementBase statement, boolean isRecoveringResilient) throws ExprValidationException;

    public void stoppedStatement(String contextName, String statementName, int statementId, String epl, ExceptionHandlingService exceptionHandlingService);

    public void destroyedStatement(String contextName, String statementName, int statementId);

    public void destroyedContext(String contextName);

    public Map<String, ContextManagerEntry> getContexts();

    public ContextManager getContextManager(String contextName);

    public CopyOnWriteArrayList<ContextStateListener> getListeners();
}
