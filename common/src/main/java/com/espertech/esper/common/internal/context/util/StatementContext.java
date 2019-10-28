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
import com.espertech.esper.common.client.EventType;
import com.espertech.esper.common.client.hook.expr.EventBeanService;
import com.espertech.esper.common.client.render.EPRenderEventService;
import com.espertech.esper.common.client.util.StatementType;
import com.espertech.esper.common.internal.collection.PathRegistry;
import com.espertech.esper.common.internal.context.airegistry.StatementAIResourceRegistry;
import com.espertech.esper.common.internal.context.compile.ContextMetaData;
import com.espertech.esper.common.internal.context.mgr.ContextManagementService;
import com.espertech.esper.common.internal.context.mgr.ContextServiceFactory;
import com.espertech.esper.common.internal.context.module.RuntimeExtensionServices;
import com.espertech.esper.common.internal.context.module.StatementAIFactoryProvider;
import com.espertech.esper.common.internal.context.module.StatementInformationalsRuntime;
import com.espertech.esper.common.internal.epl.dataflow.filtersvcadapter.DataFlowFilterServiceAdapter;
import com.espertech.esper.common.internal.epl.enummethod.cache.ExpressionResultCacheService;
import com.espertech.esper.common.internal.epl.expression.core.ExprEvaluatorContext;
import com.espertech.esper.common.internal.epl.historical.database.connection.DatabaseConfigServiceRuntime;
import com.espertech.esper.common.internal.epl.historical.datacache.HistoricalDataCacheFactory;
import com.espertech.esper.common.internal.epl.index.base.EventTableFactoryFactoryContext;
import com.espertech.esper.common.internal.epl.index.base.EventTableIndexService;
import com.espertech.esper.common.internal.epl.namedwindow.consume.NamedWindowConsumerManagementService;
import com.espertech.esper.common.internal.epl.namedwindow.core.NamedWindowManagementService;
import com.espertech.esper.common.internal.epl.pattern.pool.PatternSubexpressionPoolStmtSvc;
import com.espertech.esper.common.internal.epl.resultset.core.ResultSetProcessorHelperFactory;
import com.espertech.esper.common.internal.epl.rowrecog.state.RowRecogStatePoolStmtSvc;
import com.espertech.esper.common.internal.epl.rowrecog.state.RowRecogStateRepoFactory;
import com.espertech.esper.common.internal.epl.script.core.AgentInstanceScriptContext;
import com.espertech.esper.common.internal.epl.subselect.SubSelectStrategyFactoryContext;
import com.espertech.esper.common.internal.epl.table.core.TableExprEvaluatorContext;
import com.espertech.esper.common.internal.epl.table.core.TableManagementService;
import com.espertech.esper.common.internal.epl.variable.core.VariableManagementService;
import com.espertech.esper.common.internal.event.avro.EventTypeAvroHandler;
import com.espertech.esper.common.internal.event.core.EventBeanTypedEventFactory;
import com.espertech.esper.common.internal.event.core.EventServiceSendEventCommon;
import com.espertech.esper.common.internal.event.core.EventTypeResolvingBeanFactory;
import com.espertech.esper.common.internal.event.eventtyperepo.EventTypeRepositoryImpl;
import com.espertech.esper.common.internal.event.util.EPRuntimeEventProcessWrapped;
import com.espertech.esper.common.internal.filterspec.FilterBooleanExpressionFactory;
import com.espertech.esper.common.internal.filterspec.FilterSharedBoolExprRepository;
import com.espertech.esper.common.internal.filterspec.FilterSharedLookupableRepository;
import com.espertech.esper.common.internal.filterspec.FilterSpecActivatable;
import com.espertech.esper.common.internal.filtersvc.FilterService;
import com.espertech.esper.common.internal.metrics.audit.AuditProvider;
import com.espertech.esper.common.internal.metrics.audit.AuditProviderDefault;
import com.espertech.esper.common.internal.metrics.instrumentation.InstrumentationCommon;
import com.espertech.esper.common.internal.metrics.instrumentation.InstrumentationCommonDefault;
import com.espertech.esper.common.internal.metrics.stmtmetrics.MetricReportingService;
import com.espertech.esper.common.internal.schedule.ScheduleBucket;
import com.espertech.esper.common.internal.schedule.SchedulingService;
import com.espertech.esper.common.internal.schedule.TimeProvider;
import com.espertech.esper.common.internal.settings.ClasspathImportServiceRuntime;
import com.espertech.esper.common.internal.settings.ExceptionHandlingService;
import com.espertech.esper.common.internal.settings.RuntimeSettingsService;
import com.espertech.esper.common.internal.statement.dispatch.UpdateDispatchView;
import com.espertech.esper.common.internal.statement.resource.StatementResourceService;
import com.espertech.esper.common.internal.view.core.ViewFactoryService;
import com.espertech.esper.common.internal.view.previous.ViewServicePreviousFactory;

import javax.naming.Context;
import java.lang.annotation.Annotation;
import java.util.*;

public class StatementContext implements ExprEvaluatorContext, SubSelectStrategyFactoryContext, EventTableFactoryFactoryContext {
    private final ContextRuntimeDescriptor contextRuntimeDescriptor;
    private final String deploymentId;
    private final int statementId;
    private final String statementName;
    private final String moduleName;
    private final StatementInformationalsRuntime statementInformationals;
    private final Object userObjectRuntime;
    private final StatementContextRuntimeServices statementContextRuntimeServices;
    private final EPStatementHandle epStatementHandle;
    private final Map<Integer, FilterSpecActivatable> filterSpecActivatables;
    private final PatternSubexpressionPoolStmtSvc patternSubexpressionPoolSvc;
    private final RowRecogStatePoolStmtSvc rowRecogStatePoolStmtSvc;
    private final ScheduleBucket scheduleBucket;
    private final StatementAIResourceRegistry statementAIResourceRegistry;
    private final StatementCPCacheService statementCPCacheService;
    private final StatementAIFactoryProvider statementAIFactoryProvider;
    private final StatementResultService statementResultService;
    private final UpdateDispatchView updateDispatchView;
    private final StatementContextFilterEvalEnv statementContextFilterEvalEnv;

    private List<StatementFinalizeCallback> finalizeCallbacks;
    private StatementDestroyCallback destroyCallback;
    private AgentInstanceScriptContext defaultAgentInstanceScriptContext;

    public StatementContext(ContextRuntimeDescriptor contextRuntimeDescriptor, String deploymentId, int statementId, String statementName, String moduleName, StatementInformationalsRuntime statementInformationals, Object userObjectRuntime, StatementContextRuntimeServices statementContextRuntimeServices, EPStatementHandle epStatementHandle, Map<Integer, FilterSpecActivatable> filterSpecActivatables, PatternSubexpressionPoolStmtSvc patternSubexpressionPoolSvc, RowRecogStatePoolStmtSvc rowRecogStatePoolStmtSvc, ScheduleBucket scheduleBucket, StatementAIResourceRegistry statementAIResourceRegistry, StatementCPCacheService statementCPCacheService, StatementAIFactoryProvider statementAIFactoryProvider, StatementResultService statementResultService, UpdateDispatchView updateDispatchView) {
        this.contextRuntimeDescriptor = contextRuntimeDescriptor;
        this.deploymentId = deploymentId;
        this.statementId = statementId;
        this.statementName = statementName;
        this.moduleName = moduleName;
        this.statementInformationals = statementInformationals;
        this.userObjectRuntime = userObjectRuntime;
        this.statementContextRuntimeServices = statementContextRuntimeServices;
        this.epStatementHandle = epStatementHandle;
        this.filterSpecActivatables = filterSpecActivatables;
        this.patternSubexpressionPoolSvc = patternSubexpressionPoolSvc;
        this.rowRecogStatePoolStmtSvc = rowRecogStatePoolStmtSvc;
        this.scheduleBucket = scheduleBucket;
        this.statementAIResourceRegistry = statementAIResourceRegistry;
        this.statementCPCacheService = statementCPCacheService;
        this.statementAIFactoryProvider = statementAIFactoryProvider;
        this.statementResultService = statementResultService;
        this.updateDispatchView = updateDispatchView;
        this.statementContextFilterEvalEnv = new StatementContextFilterEvalEnv(statementContextRuntimeServices.getClasspathImportServiceRuntime(), statementInformationals.getAnnotations(), statementContextRuntimeServices.getVariableManagementService(), statementContextRuntimeServices.getTableExprEvaluatorContext());
    }

    public Annotation[] getAnnotations() {
        return statementInformationals.getAnnotations();
    }

    public String getContextName() {
        return statementInformationals.getOptionalContextName();
    }

    public ContextRuntimeDescriptor getContextRuntimeDescriptor() {
        return contextRuntimeDescriptor;
    }

    public ContextServiceFactory getContextServiceFactory() {
        return statementContextRuntimeServices.getContextServiceFactory();
    }

    public RuntimeSettingsService getRuntimeSettingsService() {
        return statementContextRuntimeServices.getRuntimeSettingsService();
    }

    public String getDeploymentId() {
        return deploymentId;
    }

    public EPStatementHandle getEpStatementHandle() {
        return epStatementHandle;
    }

    public RuntimeExtensionServices getRuntimeExtensionServices() {
        return statementContextRuntimeServices.getRuntimeExtensionServices();
    }

    public EventBeanTypedEventFactory getEventBeanTypedEventFactory() {
        return statementContextRuntimeServices.getEventBeanTypedEventFactory();
    }

    public EventBeanService getEventBeanService() {
        return statementContextRuntimeServices.getEventBeanService();
    }

    public String getRuntimeURI() {
        return statementContextRuntimeServices.getRuntimeURI();
    }

    public ExpressionResultCacheService getExpressionResultCacheServiceSharable() {
        return statementContextRuntimeServices.getExpressionResultCacheService();
    }

    public ClasspathImportServiceRuntime getClasspathImportServiceRuntime() {
        return statementContextRuntimeServices.getClasspathImportServiceRuntime();
    }

    public EventTableIndexService getEventTableIndexService() {
        return statementContextRuntimeServices.getEventTableIndexService();
    }

    public EventTypeRepositoryImpl getEventTypeRepositoryPreconfigured() {
        return statementContextRuntimeServices.getEventTypeRepositoryPreconfigured();
    }

    public FilterService getFilterService() {
        return statementContextRuntimeServices.getFilterService();
    }

    public FilterBooleanExpressionFactory getFilterBooleanExpressionFactory() {
        return statementContextRuntimeServices.getFilterBooleanExpressionFactory();
    }

    public FilterSharedLookupableRepository getFilterSharedLookupableRepository() {
        return statementContextRuntimeServices.getFilterSharedLookupableRepository();
    }

    public FilterSharedBoolExprRepository getFilterSharedBoolExprRepository() {
        return statementContextRuntimeServices.getFilterSharedBoolExprRepository();
    }

    public Map<Integer, FilterSpecActivatable> getFilterSpecActivatables() {
        return filterSpecActivatables;
    }

    public InternalEventRouter getInternalEventRouter() {
        return statementContextRuntimeServices.getInternalEventRouter();
    }

    public InternalEventRouteDest getInternalEventRouteDest() {
        return statementContextRuntimeServices.getInternalEventRouteDest();
    }

    public NamedWindowConsumerManagementService getNamedWindowConsumerManagementService() {
        return statementContextRuntimeServices.getNamedWindowConsumerManagementService();
    }

    public NamedWindowManagementService getNamedWindowManagementService() {
        return statementContextRuntimeServices.getNamedWindowManagementService();
    }

    public int getPriority() {
        return epStatementHandle.getPriority();
    }

    public ResultSetProcessorHelperFactory getResultSetProcessorHelperFactory() {
        return statementContextRuntimeServices.getResultSetProcessorHelperFactory();
    }

    public int getStatementId() {
        return statementId;
    }

    public StatementCPCacheService getStatementCPCacheService() {
        return statementCPCacheService;
    }

    public StatementContextRuntimeServices getStatementContextRuntimeServices() {
        return statementContextRuntimeServices;
    }

    public SchedulingService getSchedulingService() {
        return statementContextRuntimeServices.getSchedulingService();
    }

    public String getStatementName() {
        return statementName;
    }

    public StatementAIResourceRegistry getStatementAIResourceRegistry() {
        return statementAIResourceRegistry;
    }

    public StatementAIFactoryProvider getStatementAIFactoryProvider() {
        return statementAIFactoryProvider;
    }

    public ScheduleBucket getScheduleBucket() {
        return scheduleBucket;
    }

    public boolean isStatelessSelect() {
        return statementInformationals.isStateless();
    }

    public StatementAgentInstanceLockFactory getStatementAgentInstanceLockFactory() {
        return statementContextRuntimeServices.getStatementAgentInstanceLockFactory();
    }

    public StatementResultService getStatementResultService() {
        return statementResultService;
    }

    public TableManagementService getTableManagementService() {
        return statementContextRuntimeServices.getTableManagementService();
    }

    public TimeProvider getTimeProvider() {
        return statementContextRuntimeServices.getTimeProvider();
    }

    public Object getUserObjectCompileTime() {
        return statementInformationals.getUserObjectCompileTime();
    }

    public UpdateDispatchView getUpdateDispatchView() {
        return updateDispatchView;
    }

    public ViewServicePreviousFactory getViewServicePreviousFactory() {
        return statementContextRuntimeServices.getViewServicePreviousFactory();
    }

    public ViewFactoryService getViewFactoryService() {
        return statementContextRuntimeServices.getViewFactoryService();
    }

    public StatementResourceService getStatementResourceService() {
        return statementCPCacheService.getStatementResourceService();
    }

    public PathRegistry<String, ContextMetaData> getPathContextRegistry() {
        return statementContextRuntimeServices.getPathContextRegistry();
    }

    public PatternSubexpressionPoolStmtSvc getPatternSubexpressionPoolSvc() {
        return patternSubexpressionPoolSvc;
    }

    public StatementInformationalsRuntime getStatementInformationals() {
        return statementInformationals;
    }

    public void addFinalizeCallback(StatementFinalizeCallback callback) {
        if (finalizeCallbacks == null) {
            finalizeCallbacks = Collections.singletonList(callback);
            return;
        }
        if (finalizeCallbacks.size() == 1) {
            List<StatementFinalizeCallback> list = new ArrayList<>(2);
            list.addAll(finalizeCallbacks);
            finalizeCallbacks = list;
        }
        finalizeCallbacks.add(callback);
    }

    public Iterator<StatementFinalizeCallback> getFinalizeCallbacks() {
        return finalizeCallbacks == null ? Collections.emptyIterator() : finalizeCallbacks.iterator();
    }

    public ExceptionHandlingService getExceptionHandlingService() {
        return statementContextRuntimeServices.getExceptionHandlingService();
    }

    public AgentInstanceContext makeAgentInstanceContextUnpartitioned() {
        StatementAgentInstanceLock lock = statementAIFactoryProvider.getFactory().obtainAgentInstanceLock(this, -1);
        EPStatementAgentInstanceHandle epStatementAgentInstanceHandle = new EPStatementAgentInstanceHandle(epStatementHandle, -1, lock);
        AuditProvider auditProvider = getStatementInformationals().getAuditProvider();
        InstrumentationCommon instrumentationProvider = getStatementInformationals().getInstrumentationProvider();
        return new AgentInstanceContext(this, epStatementAgentInstanceHandle, null, null, auditProvider, instrumentationProvider);
    }

    public ContextManagementService getContextManagementService() {
        return statementContextRuntimeServices.getContextManagementService();
    }

    public VariableManagementService getVariableManagementService() {
        return statementContextRuntimeServices.getVariableManagementService();
    }

    public StatementContextFilterEvalEnv getStatementContextFilterEvalEnv() {
        return statementContextFilterEvalEnv;
    }

    public StatementDestroyCallback getDestroyCallback() {
        return destroyCallback;
    }

    public void setDestroyCallback(StatementDestroyCallback destroyCallback) {
        this.destroyCallback = destroyCallback;
    }

    public TableExprEvaluatorContext getTableExprEvaluatorContext() {
        return statementContextRuntimeServices.getTableExprEvaluatorContext();
    }

    public EventBean getContextProperties() {
        throw new IllegalStateException("Context properties not available at statement-level");
    }

    public int getAgentInstanceId() {
        throw new IllegalStateException("Agent instance id not available at statement-level");
    }

    public StatementAgentInstanceLock getAgentInstanceLock() {
        throw new IllegalStateException("Agent instance lock not available at statement-level");
    }

    public ExpressionResultCacheService getExpressionResultCacheService() {
        return getExpressionResultCacheServiceSharable();
    }

    public AgentInstanceScriptContext getAllocateAgentInstanceScriptContext() {
        if (defaultAgentInstanceScriptContext == null) {
            defaultAgentInstanceScriptContext = AgentInstanceScriptContext.from(this);
        }
        return defaultAgentInstanceScriptContext;
    }

    public EventTypeResolvingBeanFactory getEventTypeResolvingBeanFactory() {
        return statementContextRuntimeServices.getEventTypeResolvingBeanFactory();
    }

    public PathRegistry<String, EventType> getEventTypePathRegistry() {
        return statementContextRuntimeServices.getEventTypePathRegistry();
    }

    public EventTypeAvroHandler getEventTypeAvroHandler() {
        return statementContextRuntimeServices.getEventTypeAvroHandler();
    }

    public Object getRuntime() {
        return statementContextRuntimeServices.getRuntime();
    }

    public RowRecogStatePoolStmtSvc getRowRecogStatePoolStmtSvc() {
        return rowRecogStatePoolStmtSvc;
    }

    public RowRecogStateRepoFactory getRowRecogStateRepoFactory() {
        return statementContextRuntimeServices.getRowRecogStateRepoFactory();
    }

    public HistoricalDataCacheFactory getHistoricalDataCacheFactory() {
        return statementContextRuntimeServices.getHistoricalDataCacheFactory();
    }

    public DatabaseConfigServiceRuntime getDatabaseConfigService() {
        return statementContextRuntimeServices.getDatabaseConfigService();
    }

    public EPRuntimeEventProcessWrapped getEPRuntimeEventProcessWrapped() {
        return statementContextRuntimeServices.getEPRuntimeEventProcessWrapped();
    }

    public EventServiceSendEventCommon getEPRuntimeSendEvent() {
        return statementContextRuntimeServices.getEPRuntimeSendEvent();
    }

    public EPRenderEventService getEPRuntimeRenderEvent() {
        return statementContextRuntimeServices.getEPRuntimeRenderEvent();
    }

    public DataFlowFilterServiceAdapter getDataFlowFilterServiceAdapter() {
        return statementContextRuntimeServices.getDataFlowFilterServiceAdapter();
    }

    public MetricReportingService getMetricReportingService() {
        return statementContextRuntimeServices.getMetricReportingService();
    }

    public AuditProvider getAuditProvider() {
        return AuditProviderDefault.INSTANCE;
    }

    public InstrumentationCommon getInstrumentationProvider() {
        return InstrumentationCommonDefault.INSTANCE;
    }

    public Context getRuntimeEnvContext() {
        return statementContextRuntimeServices.getRuntimeEnvContext();
    }

    public Object getUserObjectRuntime() {
        return userObjectRuntime;
    }

    public StatementType getStatementType() {
        return statementInformationals.getStatementType();
    }

    public String getModuleName() {
        return moduleName;
    }

    public EventTableFactoryFactoryContext getEventTableFactoryContext() {
        return this;
    }
}
