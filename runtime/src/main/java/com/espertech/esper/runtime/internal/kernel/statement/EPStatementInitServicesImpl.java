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
package com.espertech.esper.runtime.internal.kernel.statement;

import com.espertech.esper.common.client.util.StatementProperty;
import com.espertech.esper.common.internal.collection.PathRegistry;
import com.espertech.esper.common.internal.compile.stage1.spec.ExpressionDeclItem;
import com.espertech.esper.common.internal.context.activator.ViewableActivatorFactory;
import com.espertech.esper.common.internal.context.aifactory.core.ModuleIncidentals;
import com.espertech.esper.common.internal.context.compile.ContextMetaData;
import com.espertech.esper.common.internal.context.controller.core.ContextDefinition;
import com.espertech.esper.common.internal.context.mgr.ContextDeployTimeResolver;
import com.espertech.esper.common.internal.context.mgr.ContextManagementService;
import com.espertech.esper.common.internal.context.mgr.ContextServiceFactory;
import com.espertech.esper.common.internal.context.module.EPStatementInitServices;
import com.espertech.esper.common.internal.context.module.RuntimeExtensionServices;
import com.espertech.esper.common.internal.context.module.StatementReadyCallback;
import com.espertech.esper.common.internal.context.util.InternalEventRouteDest;
import com.espertech.esper.common.internal.context.util.StatementResultService;
import com.espertech.esper.common.internal.epl.agg.core.AggregationServiceFactoryService;
import com.espertech.esper.common.internal.epl.expression.time.abacus.TimeAbacus;
import com.espertech.esper.common.internal.epl.index.base.EventTableIndexService;
import com.espertech.esper.common.internal.epl.namedwindow.consume.NamedWindowDispatchService;
import com.espertech.esper.common.internal.epl.namedwindow.consume.NamedWindowFactoryService;
import com.espertech.esper.common.internal.epl.namedwindow.core.NamedWindowManagementService;
import com.espertech.esper.common.internal.epl.namedwindow.path.NamedWindowMetaData;
import com.espertech.esper.common.internal.epl.pattern.core.PatternFactoryService;
import com.espertech.esper.common.internal.epl.resultset.core.ResultSetProcessorHelperFactory;
import com.espertech.esper.common.internal.epl.table.compiletime.TableMetaData;
import com.espertech.esper.common.internal.epl.table.core.TableManagementService;
import com.espertech.esper.common.internal.epl.variable.compiletime.VariableMetaData;
import com.espertech.esper.common.internal.epl.variable.core.VariableManagementService;
import com.espertech.esper.common.internal.event.avro.EventTypeAvroHandler;
import com.espertech.esper.common.internal.event.core.EventBeanTypedEventFactory;
import com.espertech.esper.common.internal.event.path.EventTypeResolver;
import com.espertech.esper.common.internal.filterspec.FilterBooleanExpressionFactory;
import com.espertech.esper.common.internal.filterspec.FilterSharedBoolExprRegistery;
import com.espertech.esper.common.internal.filterspec.FilterSharedLookupableRegistery;
import com.espertech.esper.common.internal.filterspec.FilterSpecActivatableRegistry;
import com.espertech.esper.common.internal.schedule.TimeProvider;
import com.espertech.esper.common.internal.schedule.TimeSourceService;
import com.espertech.esper.common.internal.serde.DataInputOutputSerdeProvider;
import com.espertech.esper.common.internal.settings.ClasspathImportServiceRuntime;
import com.espertech.esper.common.internal.settings.ExceptionHandlingService;
import com.espertech.esper.common.internal.settings.RuntimeSettingsService;
import com.espertech.esper.common.internal.statement.resource.StatementResourceService;
import com.espertech.esper.common.internal.view.core.ViewFactoryService;
import com.espertech.esper.runtime.internal.kernel.service.EPServicesContext;

import java.lang.annotation.Annotation;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import static com.espertech.esper.common.internal.context.util.StatementCPCacheService.DEFAULT_AGENT_INSTANCE_ID;

public class EPStatementInitServicesImpl implements EPStatementInitServices {
    private final String statementName;
    private final Map<StatementProperty, Object> statementProperties;
    private final Annotation[] annotations;
    private final String deploymentId;
    private final EventTypeResolver eventTypeResolver;
    private final FilterSpecActivatableRegistry filterSpecActivatableRegistry;
    private final FilterSharedBoolExprRegistery filterSharedBoolExprRegistery;
    private final FilterSharedLookupableRegistery filterSharedLookupableRegistery;
    private final ModuleIncidentals moduleIncidentals;
    private final boolean recovery;
    private final StatementResourceService statementResourceService;
    private final StatementResultService statementResultService;
    private final EPServicesContext servicesContext;
    private final List<StatementReadyCallback> readyCallbacks = new ArrayList<>();

    public EPStatementInitServicesImpl(String statementName, Map<StatementProperty, Object> statementProperties, Annotation[] annotations, String deploymentId, EventTypeResolver eventTypeResolver, FilterSpecActivatableRegistry filterSpecActivatableRegistry, FilterSharedBoolExprRegistery filterSharedBoolExprRegistery, FilterSharedLookupableRegistery filterSharedLookupableRegistery, ModuleIncidentals moduleIncidentals, boolean recovery, StatementResourceService statementResourceService, StatementResultService statementResultService, EPServicesContext servicesContext) {
        this.statementName = statementName;
        this.statementProperties = statementProperties;
        this.annotations = annotations;
        this.deploymentId = deploymentId;
        this.eventTypeResolver = eventTypeResolver;
        this.filterSpecActivatableRegistry = filterSpecActivatableRegistry;
        this.filterSharedBoolExprRegistery = filterSharedBoolExprRegistery;
        this.filterSharedLookupableRegistery = filterSharedLookupableRegistery;
        this.moduleIncidentals = moduleIncidentals;
        this.recovery = recovery;
        this.statementResourceService = statementResourceService;
        this.statementResultService = statementResultService;
        this.servicesContext = servicesContext;
    }

    public AggregationServiceFactoryService getAggregationServiceFactoryService() {
        return servicesContext.getAggregationServiceFactoryService();
    }

    public Annotation[] getAnnotations() {
        return annotations;
    }

    public ContextServiceFactory getContextServiceFactory() {
        return servicesContext.getContextServiceFactory();
    }

    public String getDeploymentId() {
        return deploymentId;
    }

    public EventBeanTypedEventFactory getEventBeanTypedEventFactory() {
        return servicesContext.getEventBeanTypedEventFactory();
    }

    public ClasspathImportServiceRuntime getClasspathImportServiceRuntime() {
        return servicesContext.getClasspathImportServiceRuntime();
    }

    public String getRuntimeURI() {
        return servicesContext.getRuntimeURI();
    }

    public RuntimeSettingsService getRuntimeSettingsService() {
        return servicesContext.getRuntimeSettingsService();
    }

    public RuntimeExtensionServices getRuntimeExtensionServices() {
        return servicesContext.getRuntimeExtensionServices();
    }

    public EventTableIndexService getEventTableIndexService() {
        return servicesContext.getEventTableIndexService();
    }

    public EventTypeAvroHandler getEventTypeAvroHandler() {
        return servicesContext.getEventTypeAvroHandler();
    }

    public ExceptionHandlingService getExceptionHandlingService() {
        return servicesContext.getExceptionHandlingService();
    }

    public FilterSharedLookupableRegistery getFilterSharedLookupableRegistery() {
        return filterSharedLookupableRegistery;
    }

    public FilterSharedBoolExprRegistery getFilterSharedBoolExprRegistery() {
        return filterSharedBoolExprRegistery;
    }

    public EPServicesContext getServicesContext() {
        return servicesContext;
    }

    public NamedWindowDispatchService getNamedWindowDispatchService() {
        return servicesContext.getNamedWindowDispatchService();
    }

    public NamedWindowFactoryService getNamedWindowFactoryService() {
        return servicesContext.getNamedWindowFactoryService();
    }

    public NamedWindowManagementService getNamedWindowManagementService() {
        return servicesContext.getNamedWindowManagementService();
    }

    public PatternFactoryService getPatternFactoryService() {
        return servicesContext.getPatternFactoryService();
    }

    public StatementResultService getStatementResultService() {
        return statementResultService;
    }

    public ContextManagementService getContextManagementService() {
        return servicesContext.getContextManagementService();
    }

    public DataInputOutputSerdeProvider getDataInputOutputSerdeProvider() {
        return servicesContext.getDataInputOutputSerdeProvider();
    }

    public PathRegistry<String, ExpressionDeclItem> getExprDeclaredPathRegistry() {
        return servicesContext.getExprDeclaredPathRegistry();
    }

    public EventTypeResolver getEventTypeResolver() {
        return eventTypeResolver;
    }

    public FilterSpecActivatableRegistry getFilterSpecActivatableRegistry() {
        return filterSpecActivatableRegistry;
    }

    public FilterBooleanExpressionFactory getFilterBooleanExpressionFactory() {
        return servicesContext.getFilterBooleanExpressionFactory();
    }

    public InternalEventRouteDest getInternalEventRouteDest() {
        return servicesContext.getInternalEventRouteDest();
    }

    public ModuleIncidentals getModuleIncidentals() {
        return moduleIncidentals;
    }

    public PathRegistry<String, NamedWindowMetaData> getNamedWindowPathRegistry() {
        return servicesContext.getNamedWindowPathRegistry();
    }

    public void addReadyCallback(StatementReadyCallback readyCallback) {
        readyCallbacks.add(readyCallback);
    }

    public List<StatementReadyCallback> getReadyCallbacks() {
        return readyCallbacks;
    }

    public boolean isRecovery() {
        return recovery;
    }

    public ResultSetProcessorHelperFactory getResultSetProcessorHelperFactory() {
        return servicesContext.getResultSetProcessorHelperFactory();
    }

    public StatementResourceService getStatementResourceService() {
        return statementResourceService;
    }

    public TableManagementService getTableManagementService() {
        return servicesContext.getTableManagementService();
    }

    public PathRegistry<String, TableMetaData> getTablePathRegistry() {
        return servicesContext.getTablePathRegistry();
    }

    public TimeAbacus getTimeAbacus() {
        return servicesContext.getTimeAbacus();
    }

    public TimeProvider getTimeProvider() {
        return servicesContext.getSchedulingService();
    }

    public TimeSourceService getTimeSourceService() {
        return servicesContext.getTimeSourceService();
    }

    public VariableManagementService getVariableManagementService() {
        return servicesContext.getVariableManagementService();
    }

    public PathRegistry<String, VariableMetaData> getVariablePathRegistry() {
        return servicesContext.getVariablePathRegistry();
    }

    public ViewableActivatorFactory getViewableActivatorFactory() {
        return servicesContext.getViewableActivatorFactory();
    }

    public ViewFactoryService getViewFactoryService() {
        return servicesContext.getViewFactoryService();
    }

    public void activateNamedWindow(String name) {
        // we are checking that all is resolved
        NamedWindowMetaData detail = moduleIncidentals.getNamedWindows().get(name);
        if (detail == null) {
            throw new IllegalArgumentException("Failed to find named window information for '" + name + "'");
        }
        servicesContext.getNamedWindowManagementService().addNamedWindow(name, detail, this);
    }

    public void activateTable(String name) {
        // we are checking that all is resolved
        TableMetaData detail = moduleIncidentals.getTables().get(name);
        if (detail == null) {
            throw new IllegalArgumentException("Failed to find table information for '" + name + "'");
        }
        servicesContext.getTableManagementService().addTable(name, detail, this);
    }

    public void activateContext(String name, ContextDefinition definition) {
        // we are checking that all is resolved
        ContextMetaData detail = moduleIncidentals.getContexts().get(name);
        if (detail == null) {
            throw new IllegalArgumentException("Failed to find context information for '" + name + "'");
        }
        servicesContext.getContextManagementService().addContext(definition, this);
    }

    public void activateVariable(String name) {
        VariableMetaData variable = moduleIncidentals.getVariables().get(name);
        if (variable == null) {
            throw new IllegalArgumentException("Failed to find variable information for '" + name + "'");
        }

        String contextDeploymentId = null;
        if (variable.getOptionalContextName() != null) {
            contextDeploymentId = ContextDeployTimeResolver.resolveContextDeploymentId(variable.getOptionalContextModule(),
                    variable.getOptionalContextVisibility(), variable.getOptionalContextName(),
                    deploymentId, servicesContext.getContextPathRegistry());
        }

        servicesContext.getVariableManagementService().addVariable(deploymentId, variable, contextDeploymentId);

        // for non-context variables we allocate the state
        if (contextDeploymentId == null) {
            servicesContext.getVariableManagementService().allocateVariableState(deploymentId, name, DEFAULT_AGENT_INSTANCE_ID, recovery, null, servicesContext.getEventBeanTypedEventFactory());
        }
    }

    public void activateExpression(String name) {
    }

    public String getStatementName() {
        return statementName;
    }

    public Map<StatementProperty, Object> getStatementProperties() {
        return statementProperties;
    }
}
