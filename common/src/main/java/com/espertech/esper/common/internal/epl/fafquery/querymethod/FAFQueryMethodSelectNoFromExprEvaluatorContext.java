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
package com.espertech.esper.common.internal.epl.fafquery.querymethod;

import com.espertech.esper.common.client.EventBean;
import com.espertech.esper.common.client.hook.expr.EventBeanService;
import com.espertech.esper.common.internal.context.util.StatementAgentInstanceLock;
import com.espertech.esper.common.internal.context.util.StatementAgentInstanceLockRW;
import com.espertech.esper.common.internal.context.util.StatementContextRuntimeServices;
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
import com.espertech.esper.common.internal.schedule.TimeProvider;
import com.espertech.esper.common.internal.settings.ExceptionHandlingService;

import java.lang.annotation.Annotation;
import java.util.TimeZone;

class FAFQueryMethodSelectNoFromExprEvaluatorContext implements ExprEvaluatorContext {
    private final StatementContextRuntimeServices services;
    private final FAFQueryMethodSelect select;
    private final StatementAgentInstanceLock lock = new StatementAgentInstanceLockRW(false);
    private final TableExprEvaluatorContext tableExprEvaluatorContext;
    private EventBean contextProperties;

    public FAFQueryMethodSelectNoFromExprEvaluatorContext(StatementContextRuntimeServices services, FAFQueryMethodSelect select) {
        this.services = services;
        this.select = select;
        this.tableExprEvaluatorContext = select.isHasTableAccess() ? new TableExprEvaluatorContext() : null;
    }

    public TimeProvider getTimeProvider() {
        return services.getSchedulingService();
    }

    public int getAgentInstanceId() {
        return -1;
    }

    public EventBean getContextProperties() {
        return contextProperties;
    }

    public String getStatementName() {
        return "(statement name not available)";
    }

    public String getRuntimeURI() {
        return services.getRuntimeURI();
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
        return services.getEventBeanService();
    }

    public StatementAgentInstanceLock getAgentInstanceLock() {
        return lock;
    }

    public ExpressionResultCacheService getExpressionResultCacheService() {
        return null;
    }

    public TableExprEvaluatorContext getTableExprEvaluatorContext() {
        return tableExprEvaluatorContext;
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
        return services.getExceptionHandlingService();
    }

    @Override
    public Object getFilterReboolConstant() {
        return null;
    }

    @Override
    public void setFilterReboolConstant(Object value) {
        throw new UnsupportedOperationException("Operation not implemented");
    }

    public String getContextName() {
        return select.getContextName();
    }

    public String getEPLWhenAvailable() {
        return select.getContextName();
    }

    public TimeZone getTimeZone() {
        return services.getClasspathImportServiceRuntime().getTimeZone();
    }

    public TimeAbacus getTimeAbacus() {
        return services.getClasspathImportServiceRuntime().getTimeAbacus();
    }

    public VariableManagementService getVariableManagementService() {
        return services.getVariableManagementService();
    }

    public EventBeanTypedEventFactory getEventBeanTypedEventFactory() {
        return services.getEventBeanService();
    }

    public String getModuleName() {
        return null;
    }

    public boolean isWritesToTables() {
        return false;
    }

    public Annotation[] getAnnotations() {
        return select.getAnnotations();
    }

    public void setContextProperties(EventBean contextProperties) {
        this.contextProperties = contextProperties;
    }
}
