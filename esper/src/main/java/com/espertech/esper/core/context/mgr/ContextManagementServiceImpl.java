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
import com.espertech.esper.client.context.ContextStateEventContextCreated;
import com.espertech.esper.client.context.ContextStateEventContextDestroyed;
import com.espertech.esper.client.context.ContextStateListener;
import com.espertech.esper.client.hook.ExceptionHandlerExceptionType;
import com.espertech.esper.core.context.util.AgentInstanceContext;
import com.espertech.esper.core.context.util.ContextDescriptor;
import com.espertech.esper.core.service.EPServicesContext;
import com.espertech.esper.core.service.ExceptionHandlingService;
import com.espertech.esper.epl.expression.core.ExprValidationException;
import com.espertech.esper.epl.spec.CreateContextDesc;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.CopyOnWriteArrayList;

public class ContextManagementServiceImpl implements ContextManagementService {
    private static final Logger log = LoggerFactory.getLogger(ContextManagementServiceImpl.class);

    private final String engineURI;
    private final Map<String, ContextManagerEntry> contexts;
    private final Set<String> destroyedContexts = new HashSet<String>();
    private final CopyOnWriteArrayList<ContextStateListener> listeners = new CopyOnWriteArrayList<>();

    public ContextManagementServiceImpl(String engineURI) {
        this.engineURI = engineURI;
        contexts = new HashMap<String, ContextManagerEntry>();
    }

    public void addContextSpec(EPServicesContext servicesContext, AgentInstanceContext agentInstanceContext, CreateContextDesc contextDesc, boolean isRecoveringResilient, EventType statementResultEventType) throws ExprValidationException {

        ContextManagerEntry mgr = contexts.get(contextDesc.getContextName());
        if (mgr != null) {
            if (destroyedContexts.contains(contextDesc.getContextName())) {
                throw new ExprValidationException("Context by name '" + contextDesc.getContextName() + "' is still referenced by statements and may not be changed");
            }
            throw new ExprValidationException("Context by name '" + contextDesc.getContextName() + "' already exists");
        }

        ContextControllerFactoryServiceContext factoryServiceContext = new ContextControllerFactoryServiceContext(contextDesc.getContextName(), servicesContext, contextDesc.getContextDetail(), agentInstanceContext, isRecoveringResilient, statementResultEventType);
        ContextManager contextManager = servicesContext.getContextManagerFactoryService().make(contextDesc.getContextDetail(), factoryServiceContext);
        factoryServiceContext.getAgentInstanceContextCreate().getEpStatementAgentInstanceHandle().setFilterFaultHandler(contextManager);

        contexts.put(contextDesc.getContextName(), new ContextManagerEntry(contextManager));
        ContextStateEventUtil.dispatchContext(listeners, () -> new ContextStateEventContextCreated(servicesContext.getEngineURI(), contextDesc.getContextName()), ContextStateListener::onContextCreated);
    }

    public int getContextCount() {
        return contexts.size();
    }

    public ContextDescriptor getContextDescriptor(String contextName) {
        ContextManagerEntry entry = contexts.get(contextName);
        if (entry == null) {
            return null;
        }
        return entry.getContextManager().getContextDescriptor();
    }

    public ContextManager getContextManager(String contextName) {
        ContextManagerEntry entry = contexts.get(contextName);
        if (entry == null) {
            return null;
        }
        return entry.getContextManager();
    }

    public void addStatement(String contextName, ContextControllerStatementBase statement, boolean isRecoveringResilient) throws ExprValidationException {
        ContextManagerEntry entry = contexts.get(contextName);
        if (entry == null) {
            throw new ExprValidationException(getNotDecaredText(contextName));
        }
        entry.addStatement(statement.getStatementContext().getStatementId());
        entry.getContextManager().addStatement(statement, isRecoveringResilient);
    }

    public void destroyedStatement(String contextName, String statementName, int statementId) {
        ContextManagerEntry entry = contexts.get(contextName);
        if (entry == null) {
            log.warn("Destroy statement for statement '" + statementName + "' failed to locate corresponding context manager '" + contextName + "'");
            return;
        }
        entry.removeStatement(statementId);
        entry.getContextManager().destroyStatement(statementName, statementId);

        if (entry.getStatementCount() == 0 && destroyedContexts.contains(contextName)) {
            destroyContext(contextName, entry);
        }
    }

    public void stoppedStatement(String contextName, String statementName, int statementId, String epl, ExceptionHandlingService exceptionHandlingService) {
        ContextManagerEntry entry = contexts.get(contextName);
        if (entry == null) {
            log.warn("Stop statement for statement '" + statementName + "' failed to locate corresponding context manager '" + contextName + "'");
            return;
        }
        try {
            entry.getContextManager().stopStatement(statementName, statementId);
        } catch (RuntimeException ex) {
            exceptionHandlingService.handleException(ex, statementName, epl, ExceptionHandlerExceptionType.STOP, null);
        }
    }

    public void destroyedContext(String contextName) {
        ContextManagerEntry entry = contexts.get(contextName);
        if (entry == null) {
            log.warn("Destroy for context '" + contextName + "' failed to locate corresponding context manager '" + contextName + "'");
            return;
        }
        if (entry.getStatementCount() == 0) {
            destroyContext(contextName, entry);
        } else {
            // some remaining statements have references
            destroyedContexts.add(contextName);
        }
    }

    public Map<String, ContextManagerEntry> getContexts() {
        return contexts;
    }

    public CopyOnWriteArrayList<ContextStateListener> getListeners() {
        return listeners;
    }

    private void destroyContext(String contextName, ContextManagerEntry entry) {
        entry.getContextManager().safeDestroy();
        contexts.remove(contextName);
        destroyedContexts.remove(contextName);
        ContextStateEventUtil.dispatchContext(listeners, () -> new ContextStateEventContextDestroyed(engineURI, contextName), ContextStateListener::onContextDestroyed);
    }

    private String getNotDecaredText(String contextName) {
        return "Context by name '" + contextName + "' has not been declared";
    }
}
