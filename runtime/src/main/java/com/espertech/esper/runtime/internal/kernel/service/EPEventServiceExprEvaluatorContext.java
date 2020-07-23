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
package com.espertech.esper.runtime.internal.kernel.service;

import com.espertech.esper.common.client.EventBean;
import com.espertech.esper.common.client.hook.expr.EventBeanService;
import com.espertech.esper.common.internal.context.util.StatementAgentInstanceLock;
import com.espertech.esper.common.internal.epl.enummethod.cache.ExpressionResultCacheService;
import com.espertech.esper.common.internal.epl.expression.core.ExprEvaluatorContext;
import com.espertech.esper.common.internal.epl.expression.time.abacus.TimeAbacus;
import com.espertech.esper.common.internal.epl.script.core.AgentInstanceScriptContext;
import com.espertech.esper.common.internal.epl.table.core.TableExprEvaluatorContext;
import com.espertech.esper.common.internal.epl.variable.core.VariableManagementService;
import com.espertech.esper.common.internal.event.core.EventBeanTypedEventFactory;
import com.espertech.esper.common.internal.metrics.audit.AuditProvider;
import com.espertech.esper.common.internal.metrics.audit.AuditProviderDefault;
import com.espertech.esper.common.internal.metrics.instrumentation.InstrumentationCommon;
import com.espertech.esper.common.internal.metrics.instrumentation.InstrumentationCommonDefault;
import com.espertech.esper.common.internal.schedule.SchedulingService;
import com.espertech.esper.common.internal.schedule.TimeProvider;
import com.espertech.esper.common.internal.settings.ExceptionHandlingService;

import java.lang.annotation.Annotation;
import java.util.TimeZone;

public class EPEventServiceExprEvaluatorContext implements ExprEvaluatorContext {
    private final String runtimeURI;
    private final EventBeanService eventBeanService;
    private final ExceptionHandlingService exceptionHandlingService;
    private final SchedulingService schedulingService;
    private final TimeZone timeZone;
    private final TimeAbacus timeAbacus;
    private final VariableManagementService variableManagementService;
    private Object filterReboolConstant;

    public EPEventServiceExprEvaluatorContext(String runtimeURI, EventBeanService eventBeanService, ExceptionHandlingService exceptionHandlingService, SchedulingService schedulingService, TimeZone timeZone, TimeAbacus timeAbacus, VariableManagementService variableManagementService) {
        this.runtimeURI = runtimeURI;
        this.eventBeanService = eventBeanService;
        this.exceptionHandlingService = exceptionHandlingService;
        this.schedulingService = schedulingService;
        this.timeZone = timeZone;
        this.timeAbacus = timeAbacus;
        this.variableManagementService = variableManagementService;
    }

    public TimeProvider getTimeProvider() {
        return schedulingService;
    }

    public int getAgentInstanceId() {
        return -1;
    }

    public EventBean getContextProperties() {
        return null;
    }

    public String getStatementName() {
        return "(statement name not available)";
    }

    public String getRuntimeURI() {
        return runtimeURI;
    }

    public int getStatementId() {
        return -1;
    }

    public String getDeploymentId() {
        return "(deployment id not available)";
    }

    public Object getUserObjectCompileTime() {
        return null;
    }

    public EventBeanService getEventBeanService() {
        return eventBeanService;
    }

    public StatementAgentInstanceLock getAgentInstanceLock() {
        return null;
    }

    public ExpressionResultCacheService getExpressionResultCacheService() {
        return null;
    }

    public TableExprEvaluatorContext getTableExprEvaluatorContext() {
        throw new UnsupportedOperationException("Table-access evaluation is not supported in this expression");
    }

    public AgentInstanceScriptContext getAllocateAgentInstanceScriptContext() {
        return null;
    }

    public AuditProvider getAuditProvider() {
        return AuditProviderDefault.INSTANCE;
    }

    public InstrumentationCommon getInstrumentationProvider() {
        return InstrumentationCommonDefault.INSTANCE;
    }

    public ExceptionHandlingService getExceptionHandlingService() {
        return exceptionHandlingService;
    }

    @Override
    public Object getFilterReboolConstant() {
        return filterReboolConstant;
    }

    @Override
    public void setFilterReboolConstant(Object value) {
        this.filterReboolConstant = value;
    }

    public String getContextName() {
        return null;
    }

    public String getEPLWhenAvailable() {
        return null;
    }

    public TimeZone getTimeZone() {
        return timeZone;
    }

    public TimeAbacus getTimeAbacus() {
        return timeAbacus;
    }

    public VariableManagementService getVariableManagementService() {
        return variableManagementService;
    }

    public EventBeanTypedEventFactory getEventBeanTypedEventFactory() {
        return eventBeanService;
    }

    public String getModuleName() {
        return null;
    }

    public boolean isWritesToTables() {
        return false;
    }

    public Annotation[] getAnnotations() {
        return null;
    }
}
