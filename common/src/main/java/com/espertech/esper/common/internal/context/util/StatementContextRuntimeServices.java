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

import com.espertech.esper.common.client.EventType;
import com.espertech.esper.common.client.hook.expr.EventBeanService;
import com.espertech.esper.common.client.render.EPRenderEventService;
import com.espertech.esper.common.internal.collection.PathRegistry;
import com.espertech.esper.common.internal.context.compile.ContextMetaData;
import com.espertech.esper.common.internal.context.mgr.ContextManagementService;
import com.espertech.esper.common.internal.context.mgr.ContextServiceFactory;
import com.espertech.esper.common.internal.context.module.RuntimeExtensionServices;
import com.espertech.esper.common.internal.epl.dataflow.core.EPDataFlowServiceImpl;
import com.espertech.esper.common.internal.epl.dataflow.filtersvcadapter.DataFlowFilterServiceAdapter;
import com.espertech.esper.common.internal.epl.enummethod.cache.ExpressionResultCacheService;
import com.espertech.esper.common.internal.epl.historical.database.connection.DatabaseConfigServiceRuntime;
import com.espertech.esper.common.internal.epl.historical.datacache.HistoricalDataCacheFactory;
import com.espertech.esper.common.internal.epl.index.base.EventTableIndexService;
import com.espertech.esper.common.internal.epl.namedwindow.consume.NamedWindowConsumerManagementService;
import com.espertech.esper.common.internal.epl.namedwindow.core.NamedWindowManagementService;
import com.espertech.esper.common.internal.epl.namedwindow.path.NamedWindowMetaData;
import com.espertech.esper.common.internal.epl.resultset.core.ResultSetProcessorHelperFactory;
import com.espertech.esper.common.internal.epl.rowrecog.state.RowRecogStateRepoFactory;
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
import com.espertech.esper.common.internal.filtersvc.FilterService;
import com.espertech.esper.common.internal.metrics.stmtmetrics.MetricReportingService;
import com.espertech.esper.common.internal.schedule.SchedulingService;
import com.espertech.esper.common.internal.schedule.TimeProvider;
import com.espertech.esper.common.internal.settings.ClasspathImportServiceRuntime;
import com.espertech.esper.common.internal.settings.ExceptionHandlingService;
import com.espertech.esper.common.internal.settings.RuntimeSettingsService;
import com.espertech.esper.common.internal.statement.resource.StatementResourceHolderBuilder;
import com.espertech.esper.common.internal.view.core.ViewFactoryService;
import com.espertech.esper.common.internal.view.previous.ViewServicePreviousFactory;

import javax.naming.Context;

public class StatementContextRuntimeServices {
    private final ContextManagementService contextManagementService;
    private final ContextServiceFactory contextServiceFactory;
    private final DatabaseConfigServiceRuntime databaseConfigService;
    private final DataFlowFilterServiceAdapter dataFlowFilterServiceAdapter;
    private final EPDataFlowServiceImpl dataflowService;
    private final String runtimeURI;
    private final Context runtimeEnvContext;
    private final ClasspathImportServiceRuntime classpathImportServiceRuntime;
    private final RuntimeSettingsService runtimeSettingsService;
    private final RuntimeExtensionServices runtimeExtensionServices;
    private final Object epRuntime;
    private final EPRenderEventService epRuntimeRenderEvent;
    private final EventServiceSendEventCommon eventServiceSendEventInternal;
    private final EPRuntimeEventProcessWrapped epRuntimeEventProcessWrapped;
    private final EventBeanService eventBeanService;
    private final EventBeanTypedEventFactory eventBeanTypedEventFactory;
    private final EventTableIndexService eventTableIndexService;
    private final EventTypeAvroHandler eventTypeAvroHandler;
    private final PathRegistry<String, EventType> eventTypePathRegistry;
    private final EventTypeRepositoryImpl eventTypeRepositoryPreconfigured;
    private final EventTypeResolvingBeanFactory eventTypeResolvingBeanFactory;
    private final ExceptionHandlingService exceptionHandlingService;
    private final ExpressionResultCacheService expressionResultCacheService;
    private final FilterService filterService;
    private final FilterBooleanExpressionFactory filterBooleanExpressionFactory;
    private final FilterSharedBoolExprRepository filterSharedBoolExprRepository;
    private final FilterSharedLookupableRepository filterSharedLookupableRepository;
    private final HistoricalDataCacheFactory historicalDataCacheFactory;
    private final InternalEventRouter internalEventRouter;
    private final InternalEventRouteDest internalEventRouteDest;
    private final MetricReportingService metricReportingService;
    private final NamedWindowConsumerManagementService namedWindowConsumerManagementService;
    private final NamedWindowManagementService namedWindowManagementService;
    private final PathRegistry<String, ContextMetaData> pathContextRegistry;
    private final PathRegistry<String, NamedWindowMetaData> pathNamedWindowRegistry;
    private final RowRecogStateRepoFactory rowRecogStateRepoFactory;
    private final ResultSetProcessorHelperFactory resultSetProcessorHelperFactory;
    private final SchedulingService schedulingService;
    private final StatementAgentInstanceLockFactory statementAgentInstanceLockFactory;
    private final StatementResourceHolderBuilder statementResourceHolderBuilder;
    private final TableExprEvaluatorContext tableExprEvaluatorContext;
    private final TableManagementService tableManagementService;
    private final VariableManagementService variableManagementService;
    private final ViewFactoryService viewFactoryService;
    private final ViewServicePreviousFactory viewServicePreviousFactory;

    public StatementContextRuntimeServices(ContextManagementService contextManagementService, ContextServiceFactory contextServiceFactory, DatabaseConfigServiceRuntime databaseConfigService, DataFlowFilterServiceAdapter dataFlowFilterServiceAdapter, EPDataFlowServiceImpl dataflowService, String runtimeURI, Context runtimeEnvContext, ClasspathImportServiceRuntime classpathImportServiceRuntime, RuntimeSettingsService runtimeSettingsService, RuntimeExtensionServices runtimeExtensionServices, Object epRuntime, EPRenderEventService epRuntimeRenderEvent, EventServiceSendEventCommon eventServiceSendEventInternal, EPRuntimeEventProcessWrapped epRuntimeEventProcessWrapped, EventBeanService eventBeanService, EventBeanTypedEventFactory eventBeanTypedEventFactory, EventTableIndexService eventTableIndexService, EventTypeAvroHandler eventTypeAvroHandler, PathRegistry<String, EventType> eventTypePathRegistry, EventTypeRepositoryImpl eventTypeRepositoryPreconfigured, EventTypeResolvingBeanFactory eventTypeResolvingBeanFactory, ExceptionHandlingService exceptionHandlingService, ExpressionResultCacheService expressionResultCacheService, FilterService filterService, FilterBooleanExpressionFactory filterBooleanExpressionFactory, FilterSharedBoolExprRepository filterSharedBoolExprRepository, FilterSharedLookupableRepository filterSharedLookupableRepository, HistoricalDataCacheFactory historicalDataCacheFactory, InternalEventRouter internalEventRouter, InternalEventRouteDest internalEventRouteDest, MetricReportingService metricReportingService, NamedWindowConsumerManagementService namedWindowConsumerManagementService, NamedWindowManagementService namedWindowManagementService, PathRegistry<String, ContextMetaData> pathContextRegistry, PathRegistry<String, NamedWindowMetaData> pathNamedWindowRegistry, RowRecogStateRepoFactory rowRecogStateRepoFactory, ResultSetProcessorHelperFactory resultSetProcessorHelperFactory, SchedulingService schedulingService, StatementAgentInstanceLockFactory statementAgentInstanceLockFactory, StatementResourceHolderBuilder statementResourceHolderBuilder, TableExprEvaluatorContext tableExprEvaluatorContext, TableManagementService tableManagementService, VariableManagementService variableManagementService, ViewFactoryService viewFactoryService, ViewServicePreviousFactory viewServicePreviousFactory) {
        this.contextManagementService = contextManagementService;
        this.contextServiceFactory = contextServiceFactory;
        this.databaseConfigService = databaseConfigService;
        this.dataFlowFilterServiceAdapter = dataFlowFilterServiceAdapter;
        this.dataflowService = dataflowService;
        this.runtimeURI = runtimeURI;
        this.runtimeEnvContext = runtimeEnvContext;
        this.classpathImportServiceRuntime = classpathImportServiceRuntime;
        this.runtimeSettingsService = runtimeSettingsService;
        this.runtimeExtensionServices = runtimeExtensionServices;
        this.epRuntime = epRuntime;
        this.epRuntimeRenderEvent = epRuntimeRenderEvent;
        this.eventServiceSendEventInternal = eventServiceSendEventInternal;
        this.epRuntimeEventProcessWrapped = epRuntimeEventProcessWrapped;
        this.eventBeanService = eventBeanService;
        this.eventBeanTypedEventFactory = eventBeanTypedEventFactory;
        this.eventTableIndexService = eventTableIndexService;
        this.eventTypeAvroHandler = eventTypeAvroHandler;
        this.eventTypePathRegistry = eventTypePathRegistry;
        this.eventTypeRepositoryPreconfigured = eventTypeRepositoryPreconfigured;
        this.eventTypeResolvingBeanFactory = eventTypeResolvingBeanFactory;
        this.exceptionHandlingService = exceptionHandlingService;
        this.expressionResultCacheService = expressionResultCacheService;
        this.filterService = filterService;
        this.filterBooleanExpressionFactory = filterBooleanExpressionFactory;
        this.filterSharedBoolExprRepository = filterSharedBoolExprRepository;
        this.filterSharedLookupableRepository = filterSharedLookupableRepository;
        this.historicalDataCacheFactory = historicalDataCacheFactory;
        this.internalEventRouter = internalEventRouter;
        this.internalEventRouteDest = internalEventRouteDest;
        this.metricReportingService = metricReportingService;
        this.namedWindowConsumerManagementService = namedWindowConsumerManagementService;
        this.namedWindowManagementService = namedWindowManagementService;
        this.pathContextRegistry = pathContextRegistry;
        this.pathNamedWindowRegistry = pathNamedWindowRegistry;
        this.rowRecogStateRepoFactory = rowRecogStateRepoFactory;
        this.resultSetProcessorHelperFactory = resultSetProcessorHelperFactory;
        this.schedulingService = schedulingService;
        this.statementAgentInstanceLockFactory = statementAgentInstanceLockFactory;
        this.statementResourceHolderBuilder = statementResourceHolderBuilder;
        this.tableExprEvaluatorContext = tableExprEvaluatorContext;
        this.tableManagementService = tableManagementService;
        this.variableManagementService = variableManagementService;
        this.viewFactoryService = viewFactoryService;
        this.viewServicePreviousFactory = viewServicePreviousFactory;
    }

    public StatementContextRuntimeServices() {
        this.contextManagementService = null;
        this.contextServiceFactory = null;
        this.databaseConfigService = null;
        this.dataFlowFilterServiceAdapter = null;
        this.dataflowService = null;
        this.runtimeURI = null;
        this.runtimeEnvContext = null;
        this.classpathImportServiceRuntime = null;
        this.runtimeSettingsService = null;
        this.runtimeExtensionServices = null;
        this.epRuntime = null;
        this.epRuntimeRenderEvent = null;
        this.eventServiceSendEventInternal = null;
        this.epRuntimeEventProcessWrapped = null;
        this.eventBeanService = null;
        this.eventBeanTypedEventFactory = null;
        this.eventTableIndexService = null;
        this.eventTypeAvroHandler = null;
        this.eventTypePathRegistry = null;
        this.eventTypeRepositoryPreconfigured = null;
        this.eventTypeResolvingBeanFactory = null;
        this.exceptionHandlingService = null;
        this.expressionResultCacheService = null;
        this.filterService = null;
        this.filterBooleanExpressionFactory = null;
        this.filterSharedBoolExprRepository = null;
        this.filterSharedLookupableRepository = null;
        this.historicalDataCacheFactory = null;
        this.internalEventRouter = null;
        this.internalEventRouteDest = null;
        this.metricReportingService = null;
        this.namedWindowConsumerManagementService = null;
        this.namedWindowManagementService = null;
        this.pathContextRegistry = null;
        this.pathNamedWindowRegistry = null;
        this.rowRecogStateRepoFactory = null;
        this.resultSetProcessorHelperFactory = null;
        this.schedulingService = null;
        this.statementAgentInstanceLockFactory = null;
        this.statementResourceHolderBuilder = null;
        this.tableExprEvaluatorContext = null;
        this.tableManagementService = null;
        this.variableManagementService = null;
        this.viewFactoryService = null;
        this.viewServicePreviousFactory = null;
    }

    public ContextManagementService getContextManagementService() {
        return contextManagementService;
    }

    public ContextServiceFactory getContextServiceFactory() {
        return contextServiceFactory;
    }

    public DatabaseConfigServiceRuntime getDatabaseConfigService() {
        return databaseConfigService;
    }

    public String getRuntimeURI() {
        return runtimeURI;
    }

    public RuntimeExtensionServices getRuntimeExtensionServices() {
        return runtimeExtensionServices;
    }

    public ClasspathImportServiceRuntime getClasspathImportServiceRuntime() {
        return classpathImportServiceRuntime;
    }

    public RuntimeSettingsService getRuntimeSettingsService() {
        return runtimeSettingsService;
    }

    public EventServiceSendEventCommon getEventServiceSendEventInternal() {
        return eventServiceSendEventInternal;
    }

    public EPRuntimeEventProcessWrapped getEPRuntimeEventProcessWrapped() {
        return epRuntimeEventProcessWrapped;
    }

    public Object getRuntime() {
        return epRuntime;
    }

    public EventTableIndexService getEventTableIndexService() {
        return eventTableIndexService;
    }

    public PathRegistry<String, EventType> getEventTypePathRegistry() {
        return eventTypePathRegistry;
    }

    public EventTypeResolvingBeanFactory getEventTypeResolvingBeanFactory() {
        return eventTypeResolvingBeanFactory;
    }

    public EventBeanTypedEventFactory getEventBeanTypedEventFactory() {
        return eventBeanTypedEventFactory;
    }

    public EventBeanService getEventBeanService() {
        return eventBeanService;
    }

    public EventTypeAvroHandler getEventTypeAvroHandler() {
        return eventTypeAvroHandler;
    }

    public ExceptionHandlingService getExceptionHandlingService() {
        return exceptionHandlingService;
    }

    public ExpressionResultCacheService getExpressionResultCacheService() {
        return expressionResultCacheService;
    }

    public FilterService getFilterService() {
        return filterService;
    }

    public FilterBooleanExpressionFactory getFilterBooleanExpressionFactory() {
        return filterBooleanExpressionFactory;
    }

    public FilterSharedBoolExprRepository getFilterSharedBoolExprRepository() {
        return filterSharedBoolExprRepository;
    }

    public FilterSharedLookupableRepository getFilterSharedLookupableRepository() {
        return filterSharedLookupableRepository;
    }

    public HistoricalDataCacheFactory getHistoricalDataCacheFactory() {
        return historicalDataCacheFactory;
    }

    public InternalEventRouter getInternalEventRouter() {
        return internalEventRouter;
    }

    public InternalEventRouteDest getInternalEventRouteDest() {
        return internalEventRouteDest;
    }

    public NamedWindowConsumerManagementService getNamedWindowConsumerManagementService() {
        return namedWindowConsumerManagementService;
    }

    public NamedWindowManagementService getNamedWindowManagementService() {
        return namedWindowManagementService;
    }

    public PathRegistry<String, ContextMetaData> getPathContextRegistry() {
        return pathContextRegistry;
    }

    public PathRegistry<String, NamedWindowMetaData> getPathNamedWindowRegistry() {
        return pathNamedWindowRegistry;
    }

    public RowRecogStateRepoFactory getRowRecogStateRepoFactory() {
        return rowRecogStateRepoFactory;
    }

    public ResultSetProcessorHelperFactory getResultSetProcessorHelperFactory() {
        return resultSetProcessorHelperFactory;
    }

    public SchedulingService getSchedulingService() {
        return schedulingService;
    }

    public StatementAgentInstanceLockFactory getStatementAgentInstanceLockFactory() {
        return statementAgentInstanceLockFactory;
    }

    public StatementResourceHolderBuilder getStatementResourceHolderBuilder() {
        return statementResourceHolderBuilder;
    }

    public TimeProvider getTimeProvider() {
        return schedulingService;
    }

    public ViewServicePreviousFactory getViewServicePreviousFactory() {
        return viewServicePreviousFactory;
    }

    public ViewFactoryService getViewFactoryService() {
        return viewFactoryService;
    }

    public EventTypeRepositoryImpl getEventTypeRepositoryPreconfigured() {
        return eventTypeRepositoryPreconfigured;
    }

    public VariableManagementService getVariableManagementService() {
        return variableManagementService;
    }

    public TableExprEvaluatorContext getTableExprEvaluatorContext() {
        return tableExprEvaluatorContext;
    }

    public TableManagementService getTableManagementService() {
        return tableManagementService;
    }

    public EPDataFlowServiceImpl getDataflowService() {
        return dataflowService;
    }

    public EventServiceSendEventCommon getEPRuntimeSendEvent() {
        return eventServiceSendEventInternal;
    }

    public EPRenderEventService getEPRuntimeRenderEvent() {
        return epRuntimeRenderEvent;
    }

    public DataFlowFilterServiceAdapter getDataFlowFilterServiceAdapter() {
        return dataFlowFilterServiceAdapter;
    }

    public MetricReportingService getMetricReportingService() {
        return metricReportingService;
    }

    public Context getRuntimeEnvContext() {
        return runtimeEnvContext;
    }
}
