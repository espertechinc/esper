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
package com.espertech.esper.common.internal.context.module;

import com.espertech.esper.common.client.serde.DataInputOutputSerde;
import com.espertech.esper.common.client.util.StatementProperty;
import com.espertech.esper.common.internal.bytecodemodel.model.expression.CodegenExpressionRef;
import com.espertech.esper.common.internal.collection.PathRegistry;
import com.espertech.esper.common.internal.compile.stage1.spec.ExpressionDeclItem;
import com.espertech.esper.common.internal.context.activator.ViewableActivatorFactory;
import com.espertech.esper.common.internal.context.controller.core.ContextDefinition;
import com.espertech.esper.common.internal.context.mgr.ContextManagementService;
import com.espertech.esper.common.internal.context.mgr.ContextServiceFactory;
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
import com.espertech.esper.common.internal.settings.ClasspathImportServiceRuntime;
import com.espertech.esper.common.internal.settings.ExceptionHandlingService;
import com.espertech.esper.common.internal.settings.RuntimeSettingsService;
import com.espertech.esper.common.internal.statement.resource.StatementResourceService;
import com.espertech.esper.common.internal.view.core.ViewFactoryService;

import java.lang.annotation.Annotation;
import java.util.Map;

import static com.espertech.esper.common.internal.bytecodemodel.model.expression.CodegenExpressionBuilder.ref;

public interface EPStatementInitServices {
    CodegenExpressionRef REF = ref("stmtInitSvc");
    String GETAGGREGATIONSERVICEFACTORYSERVICE = "getAggregationServiceFactoryService";
    String GETCONTEXTSERVICEFACTORY = "getContextServiceFactory";
    String GETCLASSPATHIMPORTSERVICERUNTIME = "getClasspathImportServiceRuntime";
    String GETRUNTIMESETTINGSSERVICE = "getRuntimeSettingsService";
    String GETEVENTBEANTYPEDEVENTFACTORY = "getEventBeanTypedEventFactory";
    String GETEVENTTABLEINDEXSERVICE = "getEventTableIndexService";
    String GETEVENTTYPERESOLVER = "getEventTypeResolver";
    String GETFILTERSHAREDBOOLEXPRREGISTERY = "getFilterSharedBoolExprRegistery";
    String GETFILTERSHAREDLOOKUPABLEREGISTERY = "getFilterSharedLookupableRegistery";
    String GETFILTERSPECACTIVATABLEREGISTRY = "getFilterSpecActivatableRegistry";
    String GETFILTERBOOLEANEXPRESSIONFACTORY = "getFilterBooleanExpressionFactory";
    String GETINTERNALEVENTROUTEDEST = "getInternalEventRouteDest";
    String GETPATTERNFACTORYSERVICE = "getPatternFactoryService";
    String GETRESULTSETPROCESSORHELPERFACTORY = "getResultSetProcessorHelperFactory";
    String GETSTATEMENTRESULTSERVICE = "getStatementResultService";
    String GETTIMEPROVIDER = "getTimeProvider";
    String GETVIEWFACTORYSERVICE = "getViewFactoryService";
    String GETVIEWABLEACTIVATORFACTORY = "getViewableActivatorFactory";

    Annotation[] getAnnotations();

    String getDeploymentId();

    String getRuntimeURI();

    String getStatementName();

    Map<StatementProperty, Object> getStatementProperties();

    AggregationServiceFactoryService getAggregationServiceFactoryService();

    ContextManagementService getContextManagementService();

    ContextServiceFactory getContextServiceFactory();

    ClasspathImportServiceRuntime getClasspathImportServiceRuntime();

    RuntimeSettingsService getRuntimeSettingsService();

    RuntimeExtensionServices getRuntimeExtensionServices();

    EventBeanTypedEventFactory getEventBeanTypedEventFactory();

    EventTableIndexService getEventTableIndexService();

    EventTypeAvroHandler getEventTypeAvroHandler();

    EventTypeResolver getEventTypeResolver();

    ExceptionHandlingService getExceptionHandlingService();

    PathRegistry<String, ExpressionDeclItem> getExprDeclaredPathRegistry();

    FilterSharedBoolExprRegistery getFilterSharedBoolExprRegistery();

    FilterSharedLookupableRegistery getFilterSharedLookupableRegistery();

    FilterSpecActivatableRegistry getFilterSpecActivatableRegistry();

    FilterBooleanExpressionFactory getFilterBooleanExpressionFactory();

    InternalEventRouteDest getInternalEventRouteDest();

    NamedWindowFactoryService getNamedWindowFactoryService();

    NamedWindowManagementService getNamedWindowManagementService();

    NamedWindowDispatchService getNamedWindowDispatchService();

    PatternFactoryService getPatternFactoryService();

    PathRegistry<String, NamedWindowMetaData> getNamedWindowPathRegistry();

    ResultSetProcessorHelperFactory getResultSetProcessorHelperFactory();

    StatementResourceService getStatementResourceService();

    StatementResultService getStatementResultService();

    TableManagementService getTableManagementService();

    PathRegistry<String, TableMetaData> getTablePathRegistry();

    TimeAbacus getTimeAbacus();

    TimeProvider getTimeProvider();

    TimeSourceService getTimeSourceService();

    VariableManagementService getVariableManagementService();

    PathRegistry<String, VariableMetaData> getVariablePathRegistry();

    ViewableActivatorFactory getViewableActivatorFactory();

    ViewFactoryService getViewFactoryService();

    void activateNamedWindow(String name);

    void activateVariable(String name, DataInputOutputSerde<Object> serde);

    void activateContext(String name, ContextDefinition definition);

    void activateExpression(String name);

    void activateTable(String name);

    void addReadyCallback(StatementReadyCallback readyCallback);
}
