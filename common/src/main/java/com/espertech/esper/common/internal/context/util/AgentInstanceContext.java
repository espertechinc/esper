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
package com.espertech.esper.common.internal.context.util;

import com.espertech.esper.common.client.EventBean;
import com.espertech.esper.common.client.hook.expr.EventBeanService;
import com.espertech.esper.common.client.render.EPRenderEventService;
import com.espertech.esper.common.internal.context.mgr.ContextManagementService;
import com.espertech.esper.common.internal.context.mgr.ContextServiceFactory;
import com.espertech.esper.common.internal.context.module.RuntimeExtensionServices;
import com.espertech.esper.common.internal.epl.dataflow.filtersvcadapter.DataFlowFilterServiceAdapter;
import com.espertech.esper.common.internal.epl.enummethod.cache.ExpressionResultCacheService;
import com.espertech.esper.common.internal.epl.expression.core.ExprEvaluatorContext;
import com.espertech.esper.common.internal.epl.historical.database.connection.DatabaseConfigServiceRuntime;
import com.espertech.esper.common.internal.epl.historical.datacache.HistoricalDataCacheFactory;
import com.espertech.esper.common.internal.epl.index.base.EventTableIndexService;
import com.espertech.esper.common.internal.epl.namedwindow.core.NamedWindowManagementService;
import com.espertech.esper.common.internal.epl.resultset.core.ResultSetProcessorHelperFactory;
import com.espertech.esper.common.internal.epl.rowrecog.state.RowRecogStateRepoFactory;
import com.espertech.esper.common.internal.epl.script.core.AgentInstanceScriptContext;
import com.espertech.esper.common.internal.epl.table.core.TableExprEvaluatorContext;
import com.espertech.esper.common.internal.epl.table.core.TableManagementService;
import com.espertech.esper.common.internal.epl.variable.core.VariableManagementService;
import com.espertech.esper.common.internal.event.avro.EventTypeAvroHandler;
import com.espertech.esper.common.internal.event.core.EventBeanTypedEventFactory;
import com.espertech.esper.common.internal.event.core.EventServiceSendEventCommon;
import com.espertech.esper.common.internal.event.core.EventTypeResolvingBeanFactory;
import com.espertech.esper.common.internal.event.core.MappedEventBean;
import com.espertech.esper.common.internal.event.util.EPRuntimeEventProcessWrapped;
import com.espertech.esper.common.internal.filtersvc.FilterService;
import com.espertech.esper.common.internal.metrics.audit.AuditProvider;
import com.espertech.esper.common.internal.metrics.instrumentation.InstrumentationCommon;
import com.espertech.esper.common.internal.metrics.stmtmetrics.MetricReportingService;
import com.espertech.esper.common.internal.schedule.ScheduleBucket;
import com.espertech.esper.common.internal.schedule.SchedulingService;
import com.espertech.esper.common.internal.schedule.TimeProvider;
import com.espertech.esper.common.internal.settings.ClasspathImportServiceRuntime;
import com.espertech.esper.common.internal.settings.ExceptionHandlingService;
import com.espertech.esper.common.internal.settings.RuntimeSettingsService;
import com.espertech.esper.common.internal.statement.resource.StatementResourceService;
import com.espertech.esper.common.internal.view.core.ViewFactoryService;

import javax.naming.Context;
import java.lang.annotation.Annotation;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;

public class AgentInstanceContext implements ExprEvaluatorContext {
    private final long filterVersionAfterAllocation;
    private final StatementContext statementContext;
    private final EPStatementAgentInstanceHandle epStatementAgentInstanceHandle;
    private final AgentInstanceFilterProxy agentInstanceFilterProxy;
    private final MappedEventBean contextProperties;
    private final AuditProvider auditProvider;
    private final InstrumentationCommon instrumentationProvider;
    private StatementContextCPPair statementContextCPPair;
    private AgentInstanceScriptContext agentInstanceScriptContext;
    private Object terminationCallbacks;

    public AgentInstanceContext(StatementContext statementContext, EPStatementAgentInstanceHandle epStatementAgentInstanceHandle, AgentInstanceFilterProxy agentInstanceFilterProxy, MappedEventBean contextProperties, AuditProvider auditProvider, InstrumentationCommon instrumentationProvider) {
        this.statementContext = statementContext;
        this.filterVersionAfterAllocation = statementContext.getFilterService().getFiltersVersion();
        this.epStatementAgentInstanceHandle = epStatementAgentInstanceHandle;
        this.agentInstanceFilterProxy = agentInstanceFilterProxy;
        this.contextProperties = contextProperties;
        this.auditProvider = auditProvider;
        this.instrumentationProvider = instrumentationProvider;
    }

    public AgentInstanceFilterProxy getAgentInstanceFilterProxy() {
        return agentInstanceFilterProxy;
    }

    public Annotation[] getAnnotations() {
        return statementContext.getAnnotations();
    }

    public ContextManagementService getContextManagementService() {
        return statementContext.getContextManagementService();
    }

    public ContextServiceFactory getContextServiceFactory() {
        return statementContext.getContextServiceFactory();
    }

    public EventBean getContextProperties() {
        return contextProperties;
    }

    public String getRuntimeURI() {
        return statementContext.getRuntimeURI();
    }

    public int getAgentInstanceId() {
        return epStatementAgentInstanceHandle.getAgentInstanceId();
    }

    public StatementAgentInstanceLock getAgentInstanceLock() {
        return epStatementAgentInstanceHandle.getStatementAgentInstanceLock();
    }

    public EPStatementAgentInstanceHandle getEpStatementAgentInstanceHandle() {
        return epStatementAgentInstanceHandle;
    }

    public RuntimeExtensionServices getRuntimeExtensionServicesContext() {
        return statementContext.getRuntimeExtensionServices();
    }

    public EventBeanTypedEventFactory getEventBeanTypedEventFactory() {
        return statementContext.getEventBeanTypedEventFactory();
    }

    public RuntimeSettingsService getRuntimeSettingsService() {
        return statementContext.getRuntimeSettingsService();
    }

    public ClasspathImportServiceRuntime getClasspathImportServiceRuntime() {
        return statementContext.getClasspathImportServiceRuntime();
    }

    public EventBeanService getEventBeanService() {
        return statementContext.getEventBeanService();
    }

    public FilterService getFilterService() {
        return statementContext.getFilterService();
    }

    public InternalEventRouter getInternalEventRouter() {
        return statementContext.getInternalEventRouter();
    }

    public InternalEventRouteDest getInternalEventRouteDest() {
        return statementContext.getInternalEventRouteDest();
    }

    public SchedulingService getSchedulingService() {
        return statementContext.getSchedulingService();
    }

    public ScheduleBucket getScheduleBucket() {
        return statementContext.getScheduleBucket();
    }

    public String getStatementName() {
        return statementContext.getStatementName();
    }

    public Object getUserObjectCompileTime() {
        return statementContext.getUserObjectCompileTime();
    }

    public int getStatementId() {
        return statementContext.getStatementId();
    }

    public StatementContextCPPair getStatementContextCPPair() {
        if (statementContextCPPair == null) {
            statementContextCPPair = new StatementContextCPPair(statementContext.getStatementId(), epStatementAgentInstanceHandle.getAgentInstanceId(), statementContext);
        }
        return statementContextCPPair;
    }

    public StatementContext getStatementContext() {
        return statementContext;
    }

    public StatementResultService getStatementResultService() {
        return statementContext.getStatementResultService();
    }

    public TimeProvider getTimeProvider() {
        return statementContext.getTimeProvider();
    }

    public ViewFactoryService getViewFactoryService() {
        return statementContext.getViewFactoryService();
    }

    public ResultSetProcessorHelperFactory getResultSetProcessorHelperFactory() {
        return statementContext.getResultSetProcessorHelperFactory();
    }

    public String getDeploymentId() {
        return statementContext.getDeploymentId();
    }

    public NamedWindowManagementService getNamedWindowManagementService() {
        return statementContext.getNamedWindowManagementService();
    }

    public StatementResourceService getStatementResourceService() {
        return statementContext.getStatementResourceService();
    }

    public ExpressionResultCacheService getExpressionResultCacheService() {
        return statementContext.getExpressionResultCacheServiceSharable();
    }

    public ExceptionHandlingService getExceptionHandlingService() {
        return statementContext.getExceptionHandlingService();
    }

    public VariableManagementService getVariableManagementService() {
        return statementContext.getVariableManagementService();
    }

    public StatementContextFilterEvalEnv getStatementContextFilterEvalEnv() {
        return statementContext.getStatementContextFilterEvalEnv();
    }

    public TableExprEvaluatorContext getTableExprEvaluatorContext() {
        return statementContext.getTableExprEvaluatorContext();
    }

    public TableManagementService getTableManagementService() {
        return statementContext.getTableManagementService();
    }

    public AgentInstanceScriptContext getAllocateAgentInstanceScriptContext() {
        if (agentInstanceScriptContext == null) {
            agentInstanceScriptContext = AgentInstanceScriptContext.from(statementContext);
        }
        return agentInstanceScriptContext;
    }

    public EventTypeResolvingBeanFactory getEventTypeResolvingBeanFactory() {
        return statementContext.getEventTypeResolvingBeanFactory();
    }

    public EventTypeAvroHandler getEventTypeAvroHandler() {
        return statementContext.getEventTypeAvroHandler();
    }

    public RowRecogStateRepoFactory getRowRecogStateRepoFactory() {
        return statementContext.getRowRecogStateRepoFactory();
    }

    public EventTableIndexService getEventTableIndexService() {
        return statementContext.getEventTableIndexService();
    }

    public HistoricalDataCacheFactory getHistoricalDataCacheFactory() {
        return statementContext.getHistoricalDataCacheFactory();
    }

    public DatabaseConfigServiceRuntime getDatabaseConfigService() {
        return statementContext.getDatabaseConfigService();
    }

    public EPRuntimeEventProcessWrapped getEPRuntimeEventProcessWrapped() {
        return statementContext.getEPRuntimeEventProcessWrapped();
    }

    public EventServiceSendEventCommon getEPRuntimeSendEvent() {
        return statementContext.getEPRuntimeSendEvent();
    }

    public EPRenderEventService getEPRuntimeRenderEvent() {
        return statementContext.getEPRuntimeRenderEvent();
    }

    public DataFlowFilterServiceAdapter getDataFlowFilterServiceAdapter() {
        return statementContext.getDataFlowFilterServiceAdapter();
    }

    public Object getRuntime() {
        return statementContext.getRuntime();
    }

    public MetricReportingService getMetricReportingService() {
        return statementContext.getMetricReportingService();
    }

    public AuditProvider getAuditProvider() {
        return auditProvider;
    }

    public InstrumentationCommon getInstrumentationProvider() {
        return instrumentationProvider;
    }

    public Collection<AgentInstanceStopCallback> getTerminationCallbackRO() {
        if (terminationCallbacks == null) {
            return Collections.emptyList();
        } else if (terminationCallbacks instanceof Collection) {
            return (Collection<AgentInstanceStopCallback>) terminationCallbacks;
        }
        return Collections.singletonList((AgentInstanceStopCallback) terminationCallbacks);
    }

    /**
     * Add a stop-callback.
     * Use to add a stop-callback other than already registered.
     * This is generally not required by views that implement AgentInstanceStopCallback as
     * they gets stopped as part of normal processing.
     *
     * @param callback to add
     */
    public void addTerminationCallback(AgentInstanceStopCallback callback) {
        if (terminationCallbacks == null) {
            terminationCallbacks = callback;
        } else if (terminationCallbacks instanceof Collection) {
            ((Collection<AgentInstanceStopCallback>) terminationCallbacks).add(callback);
        } else {
            AgentInstanceStopCallback cb = (AgentInstanceStopCallback) terminationCallbacks;
            HashSet<AgentInstanceStopCallback> q = new HashSet<AgentInstanceStopCallback>(2);
            q.add(cb);
            q.add(callback);
            terminationCallbacks = q;
        }
    }

    public void removeTerminationCallback(AgentInstanceStopCallback callback) {
        if (terminationCallbacks == null) {
            return;
        } else if (terminationCallbacks instanceof Collection) {
            ((Collection<AgentInstanceStopCallback>) terminationCallbacks).remove(callback);
        } else if (terminationCallbacks == callback) {
            terminationCallbacks = null;
        }
    }

    public Context getRuntimeEnvContext() {
        return statementContext.getRuntimeEnvContext();
    }

    public long getFilterVersionAfterAllocation() {
        return filterVersionAfterAllocation;
    }

    public String getModuleName() {
        return statementContext.getModuleName();
    }
}
