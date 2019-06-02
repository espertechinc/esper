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
package com.espertech.esper.common.internal.view.core;

import com.espertech.esper.common.client.EventBean;
import com.espertech.esper.common.client.hook.expr.EventBeanService;
import com.espertech.esper.common.internal.collection.ViewUpdatedCollection;
import com.espertech.esper.common.internal.context.module.RuntimeExtensionServices;
import com.espertech.esper.common.internal.context.util.AgentInstanceContext;
import com.espertech.esper.common.internal.context.util.EPStatementAgentInstanceHandle;
import com.espertech.esper.common.internal.context.util.StatementAgentInstanceLock;
import com.espertech.esper.common.internal.context.util.StatementContext;
import com.espertech.esper.common.internal.epl.enummethod.cache.ExpressionResultCacheService;
import com.espertech.esper.common.internal.epl.expression.core.ExprEvaluatorContext;
import com.espertech.esper.common.internal.epl.prior.PriorHelper;
import com.espertech.esper.common.internal.epl.script.core.AgentInstanceScriptContext;
import com.espertech.esper.common.internal.epl.table.core.TableExprEvaluatorContext;
import com.espertech.esper.common.internal.event.core.EventBeanTypedEventFactory;
import com.espertech.esper.common.internal.metrics.audit.AuditProvider;
import com.espertech.esper.common.internal.metrics.instrumentation.InstrumentationCommon;
import com.espertech.esper.common.internal.schedule.SchedulingService;
import com.espertech.esper.common.internal.schedule.TimeProvider;
import com.espertech.esper.common.internal.settings.ClasspathImportServiceRuntime;
import com.espertech.esper.common.internal.settings.ExceptionHandlingService;
import com.espertech.esper.common.internal.settings.RuntimeSettingsService;
import com.espertech.esper.common.internal.statement.helper.EPStatementStartMethodHelperPrevious;
import com.espertech.esper.common.internal.view.access.ViewResourceDelegateDesc;
import com.espertech.esper.common.internal.view.previous.PreviousGetterStrategy;
import com.espertech.esper.common.internal.view.prior.PriorEventViewFactory;

import java.lang.annotation.Annotation;

public class AgentInstanceViewFactoryChainContext implements ExprEvaluatorContext {
    private final AgentInstanceContext agentInstanceContext;
    private boolean isRemoveStream;
    private final PreviousGetterStrategy previousNodeGetter;
    private final ViewUpdatedCollection priorViewUpdatedCollection;

    public AgentInstanceViewFactoryChainContext(AgentInstanceContext agentInstanceContext, boolean isRemoveStream, PreviousGetterStrategy previousNodeGetter, ViewUpdatedCollection priorViewUpdatedCollection) {
        this.agentInstanceContext = agentInstanceContext;
        this.isRemoveStream = isRemoveStream;
        this.previousNodeGetter = previousNodeGetter;
        this.priorViewUpdatedCollection = priorViewUpdatedCollection;
    }

    public AgentInstanceContext getAgentInstanceContext() {
        return agentInstanceContext;
    }

    public PreviousGetterStrategy getPreviousNodeGetter() {
        return previousNodeGetter;
    }

    public ViewUpdatedCollection getPriorViewUpdatedCollection() {
        return priorViewUpdatedCollection;
    }

    public StatementContext getStatementContext() {
        return agentInstanceContext.getStatementContext();
    }

    public String getStatementName() {
        return agentInstanceContext.getStatementName();
    }

    public Object getUserObjectCompileTime() {
        return agentInstanceContext.getUserObjectCompileTime();
    }

    public int getStatementId() {
        return agentInstanceContext.getStatementId();
    }

    public String getDeploymentId() {
        return agentInstanceContext.getDeploymentId();
    }

    public int getAgentInstanceId() {
        return agentInstanceContext.getAgentInstanceId();
    }

    public String getRuntimeURI() {
        return agentInstanceContext.getRuntimeURI();
    }

    public EventBeanService getEventBeanService() {
        return agentInstanceContext.getEventBeanService();
    }

    public TimeProvider getTimeProvider() {
        return agentInstanceContext.getTimeProvider();
    }

    public StatementAgentInstanceLock getAgentInstanceLock() {
        return agentInstanceContext.getAgentInstanceLock();
    }

    public EventBean getContextProperties() {
        return agentInstanceContext.getContextProperties();
    }

    public TableExprEvaluatorContext getTableExprEvaluatorContext() {
        return agentInstanceContext.getTableExprEvaluatorContext();
    }

    public static AgentInstanceViewFactoryChainContext create(ViewFactory[] viewFactoryChain, AgentInstanceContext agentInstanceContext, ViewResourceDelegateDesc viewResourceDelegate) {

        PreviousGetterStrategy previousNodeGetter = null;
        if (viewResourceDelegate.isHasPrevious()) {
            DataWindowViewWithPrevious factoryFound = EPStatementStartMethodHelperPrevious.findPreviousViewFactory(viewFactoryChain);
            previousNodeGetter = factoryFound.makePreviousGetter();
        }

        ViewUpdatedCollection priorViewUpdatedCollection = null;
        if (viewResourceDelegate.getPriorRequests() != null && !viewResourceDelegate.getPriorRequests().isEmpty()) {
            PriorEventViewFactory priorEventViewFactory = PriorHelper.findPriorViewFactory(viewFactoryChain);
            priorViewUpdatedCollection = priorEventViewFactory.makeViewUpdatedCollection(viewResourceDelegate.getPriorRequests(), agentInstanceContext);
        }

        boolean removedStream = false;
        if (viewFactoryChain.length > 1) {
            int countDataWindow = 0;
            for (ViewFactory viewFactory : viewFactoryChain) {
                if (viewFactory instanceof DataWindowViewFactory) {
                    countDataWindow++;
                }
            }
            removedStream = countDataWindow > 1;
        }

        return new AgentInstanceViewFactoryChainContext(agentInstanceContext, removedStream, previousNodeGetter, priorViewUpdatedCollection);
    }

    public RuntimeSettingsService getRuntimeSettingsService() {
        return agentInstanceContext.getStatementContext().getRuntimeSettingsService();
    }

    public Annotation[] getAnnotations() {
        return agentInstanceContext.getStatementContext().getAnnotations();
    }

    public EPStatementAgentInstanceHandle getEpStatementAgentInstanceHandle() {
        return agentInstanceContext.getEpStatementAgentInstanceHandle();
    }

    public boolean isRemoveStream() {
        return isRemoveStream;
    }

    public SchedulingService getSchedulingService() {
        return agentInstanceContext.getSchedulingService();
    }

    public EventBeanTypedEventFactory getEventBeanTypedEventFactory() {
        return agentInstanceContext.getEventBeanTypedEventFactory();
    }

    public void setRemoveStream(boolean removeStream) {
        isRemoveStream = removeStream;
    }

    public RuntimeExtensionServices getRuntimeExtensionServices() {
        return agentInstanceContext.getRuntimeExtensionServicesContext();
    }

    public ExpressionResultCacheService getExpressionResultCacheService() {
        return agentInstanceContext.getExpressionResultCacheService();
    }

    public AgentInstanceScriptContext getAllocateAgentInstanceScriptContext() {
        return agentInstanceContext.getAllocateAgentInstanceScriptContext();
    }

    public AuditProvider getAuditProvider() {
        return agentInstanceContext.getAuditProvider();
    }

    public InstrumentationCommon getInstrumentationProvider() {
        return agentInstanceContext.getInstrumentationProvider();
    }

    public ClasspathImportServiceRuntime getClasspathImportService() {
        return agentInstanceContext.getClasspathImportServiceRuntime();
    }

    public ExceptionHandlingService getExceptionHandlingService() {
        return agentInstanceContext.getExceptionHandlingService();
    }
}
