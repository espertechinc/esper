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
package com.espertech.esper.core.service;

import com.espertech.esper.client.*;
import com.espertech.esper.client.hook.*;
import com.espertech.esper.codegen.compile.CodegenCompiler;
import com.espertech.esper.codegen.compile.CodegenCompilerJanino;
import com.espertech.esper.collection.Pair;
import com.espertech.esper.core.context.activator.ViewableActivatorFactoryDefault;
import com.espertech.esper.core.context.mgr.ContextControllerFactoryFactorySvcImpl;
import com.espertech.esper.core.context.mgr.ContextManagementService;
import com.espertech.esper.core.context.mgr.ContextManagementServiceImpl;
import com.espertech.esper.core.context.mgr.ContextManagerFactoryServiceImpl;
import com.espertech.esper.core.deploy.DeploymentStateService;
import com.espertech.esper.core.deploy.DeploymentStateServiceImpl;
import com.espertech.esper.core.service.multimatch.MultiMatchHandlerFactoryImpl;
import com.espertech.esper.core.start.EPStatementStartMethod;
import com.espertech.esper.core.thread.ThreadingService;
import com.espertech.esper.core.thread.ThreadingServiceImpl;
import com.espertech.esper.dataflow.core.DataFlowConfigurationStateServiceImpl;
import com.espertech.esper.dataflow.core.DataFlowServiceImpl;
import com.espertech.esper.epl.agg.factory.AggregationFactoryFactory;
import com.espertech.esper.epl.agg.factory.AggregationFactoryFactoryDefault;
import com.espertech.esper.epl.core.engineimport.EngineImportException;
import com.espertech.esper.epl.core.engineimport.EngineImportService;
import com.espertech.esper.epl.core.engineimport.EngineImportServiceImpl;
import com.espertech.esper.epl.core.engineimport.EngineSettingsService;
import com.espertech.esper.epl.core.resultset.core.ResultSetProcessorHelperFactoryImpl;
import com.espertech.esper.epl.db.DataCacheFactory;
import com.espertech.esper.epl.db.DatabaseConfigService;
import com.espertech.esper.epl.db.DatabaseConfigServiceImpl;
import com.espertech.esper.epl.declexpr.ExprDeclaredServiceImpl;
import com.espertech.esper.epl.expression.time.TimeAbacus;
import com.espertech.esper.epl.expression.time.TimeAbacusMicroseconds;
import com.espertech.esper.epl.expression.time.TimeAbacusMilliseconds;
import com.espertech.esper.epl.lookup.EventTableIndexServiceImpl;
import com.espertech.esper.epl.metric.MetricReportingServiceImpl;
import com.espertech.esper.epl.named.*;
import com.espertech.esper.epl.spec.PluggableObjectCollection;
import com.espertech.esper.epl.table.mgmt.TableService;
import com.espertech.esper.epl.table.mgmt.TableServiceImpl;
import com.espertech.esper.epl.variable.VariableExistsException;
import com.espertech.esper.epl.variable.VariableService;
import com.espertech.esper.epl.variable.VariableServiceImpl;
import com.espertech.esper.epl.variable.VariableTypeException;
import com.espertech.esper.event.*;
import com.espertech.esper.event.avro.EventAdapterAvroHandler;
import com.espertech.esper.event.avro.EventAdapterAvroHandlerUnsupported;
import com.espertech.esper.event.vaevent.ValueAddEventService;
import com.espertech.esper.event.vaevent.ValueAddEventServiceImpl;
import com.espertech.esper.event.xml.SchemaModel;
import com.espertech.esper.event.xml.XSDSchemaMapper;
import com.espertech.esper.filter.FilterBooleanExpressionFactoryImpl;
import com.espertech.esper.filter.FilterNonPropertyRegisteryServiceImpl;
import com.espertech.esper.filter.FilterServiceProvider;
import com.espertech.esper.filter.FilterServiceSPI;
import com.espertech.esper.pattern.PatternNodeFactoryImpl;
import com.espertech.esper.pattern.pool.PatternSubexpressionPoolEngineSvc;
import com.espertech.esper.plugin.PlugInEventRepresentation;
import com.espertech.esper.plugin.PlugInEventRepresentationContext;
import com.espertech.esper.rowregex.MatchRecognizeStatePoolEngineSvc;
import com.espertech.esper.rowregex.RegexHandlerFactoryDefault;
import com.espertech.esper.schedule.*;
import com.espertech.esper.timer.TimeSourceService;
import com.espertech.esper.timer.TimeSourceServiceImpl;
import com.espertech.esper.timer.TimerService;
import com.espertech.esper.timer.TimerServiceImpl;
import com.espertech.esper.util.GraphCircularDependencyException;
import com.espertech.esper.util.GraphUtil;
import com.espertech.esper.util.JavaClassHelper;
import com.espertech.esper.util.ManagedReadWriteLock;
import com.espertech.esper.view.ViewServicePreviousFactoryImpl;
import com.espertech.esper.view.stream.StreamFactoryService;
import com.espertech.esper.view.stream.StreamFactoryServiceProvider;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.Serializable;
import java.lang.reflect.Array;
import java.net.URI;
import java.util.*;
import java.util.concurrent.TimeUnit;

/**
 * Factory for services context.
 */
public class EPServicesContextFactoryDefault implements EPServicesContextFactory {
    private static final Logger log = LoggerFactory.getLogger(EPServicesContextFactoryDefault.class);

    public EPServicesContext createServicesContext(EPServiceProvider epServiceProvider, ConfigurationInformation configSnapshot) {
        // JNDI context for binding resources
        EngineEnvContext jndiContext = new EngineEnvContext();

        // Engine import service
        EngineImportService engineImportService = makeEngineImportService(configSnapshot, AggregationFactoryFactoryDefault.INSTANCE, epServiceProvider.getURI());

        // Event Type Id Generation
        EventTypeIdGenerator eventTypeIdGenerator;
        if (configSnapshot.getEngineDefaults().getAlternativeContext() == null || configSnapshot.getEngineDefaults().getAlternativeContext().getEventTypeIdGeneratorFactory() == null) {
            eventTypeIdGenerator = new EventTypeIdGeneratorImpl();
        } else {
            EventTypeIdGeneratorFactory eventTypeIdGeneratorFactory = (EventTypeIdGeneratorFactory) JavaClassHelper.instantiate(EventTypeIdGeneratorFactory.class, configSnapshot.getEngineDefaults().getAlternativeContext().getEventTypeIdGeneratorFactory(), engineImportService.getClassForNameProvider());
            eventTypeIdGenerator = eventTypeIdGeneratorFactory.create(new EventTypeIdGeneratorContext(epServiceProvider.getURI()));
        }

        // Make services that depend on snapshot config entries
        EventAdapterAvroHandler avroHandler = EventAdapterAvroHandlerUnsupported.INSTANCE;
        if (configSnapshot.getEngineDefaults().getEventMeta().getAvroSettings().isEnableAvro()) {
            try {
                avroHandler = (EventAdapterAvroHandler) JavaClassHelper.instantiate(EventAdapterAvroHandler.class, EventAdapterAvroHandler.HANDLER_IMPL, engineImportService.getClassForNameProvider());
            } catch (Throwable t) {
                log.debug("Avro provider {} not instantiated, not enabling Avro support: {}", EventAdapterAvroHandler.HANDLER_IMPL, t.getMessage());
            }
            try {
                avroHandler.init(configSnapshot.getEngineDefaults().getEventMeta().getAvroSettings(), engineImportService);
            } catch (Throwable t) {
                throw new ConfigurationException("Failed to initialize Esper-Avro: " + t.getMessage(), t);
            }
        }
        EventAdapterServiceImpl eventAdapterService = new EventAdapterServiceImpl(eventTypeIdGenerator, configSnapshot.getEngineDefaults().getEventMeta().getAnonymousCacheSize(), avroHandler, engineImportService);
        init(eventAdapterService, configSnapshot, engineImportService);

        // New read-write lock for concurrent event processing
        ManagedReadWriteLock eventProcessingRWLock = new ManagedReadWriteLock("EventProcLock", false);

        TimeSourceService timeSourceService = makeTimeSource(configSnapshot);
        SchedulingServiceSPI schedulingService = SchedulingServiceProvider.newService(timeSourceService);
        SchedulingMgmtService schedulingMgmtService = new SchedulingMgmtServiceImpl();
        EngineSettingsService engineSettingsService = new EngineSettingsService(configSnapshot.getEngineDefaults(), configSnapshot.getPlugInEventTypeResolutionURIs());
        DatabaseConfigService databaseConfigService = makeDatabaseRefService(configSnapshot, schedulingService, schedulingMgmtService, engineImportService);

        PluggableObjectCollection plugInViews = new PluggableObjectCollection();
        plugInViews.addViews(configSnapshot.getPlugInViews(), configSnapshot.getPlugInVirtualDataWindows(), engineImportService);
        PluggableObjectCollection plugInPatternObj = new PluggableObjectCollection();
        plugInPatternObj.addPatternObjects(configSnapshot.getPlugInPatternObjects(), engineImportService);

        // exception handling
        ExceptionHandlingService exceptionHandlingService = initExceptionHandling(epServiceProvider.getURI(), configSnapshot.getEngineDefaults().getExceptionHandling(), configSnapshot.getEngineDefaults().getConditionHandling(), engineImportService);

        // Statement context factory
        Class systemVirtualDWViewFactory = null;
        if (configSnapshot.getEngineDefaults().getAlternativeContext().getVirtualDataWindowViewFactory() != null) {
            try {
                systemVirtualDWViewFactory = engineImportService.getClassForNameProvider().classForName(configSnapshot.getEngineDefaults().getAlternativeContext().getVirtualDataWindowViewFactory());
                if (!JavaClassHelper.isImplementsInterface(systemVirtualDWViewFactory, VirtualDataWindowFactory.class)) {
                    throw new ConfigurationException("Class " + systemVirtualDWViewFactory.getName() + " does not implement the interface " + VirtualDataWindowFactory.class.getName());
                }
            } catch (ClassNotFoundException e) {
                throw new ConfigurationException("Failed to look up class " + systemVirtualDWViewFactory);
            }
        }
        StatementContextFactory statementContextFactory = new StatementContextFactoryDefault(plugInViews, plugInPatternObj, systemVirtualDWViewFactory);

        long msecTimerResolution = configSnapshot.getEngineDefaults().getThreading().getInternalTimerMsecResolution();
        if (msecTimerResolution <= 0) {
            throw new ConfigurationException("Timer resolution configuration not set to a valid value, expecting a non-zero value");
        }
        TimerService timerService = new TimerServiceImpl(epServiceProvider.getURI(), msecTimerResolution);

        VariableService variableService = new VariableServiceImpl(configSnapshot.getEngineDefaults().getVariables().getMsecVersionRelease(), schedulingService, eventAdapterService, null);
        initVariables(variableService, configSnapshot.getVariables(), engineImportService);

        TableService tableService = new TableServiceImpl();

        StatementLockFactory statementLockFactory = new StatementLockFactoryImpl(configSnapshot.getEngineDefaults().getExecution().isFairlock(), configSnapshot.getEngineDefaults().getExecution().isDisableLocking());
        StreamFactoryService streamFactoryService = StreamFactoryServiceProvider.newService(epServiceProvider.getURI(), configSnapshot.getEngineDefaults().getViewResources().isShareViews());
        FilterServiceSPI filterService = FilterServiceProvider.newService(configSnapshot.getEngineDefaults().getExecution().getFilterServiceProfile(), configSnapshot.getEngineDefaults().getExecution().isAllowIsolatedService());
        MetricReportingServiceImpl metricsReporting = new MetricReportingServiceImpl(configSnapshot.getEngineDefaults().getMetricsReporting(), epServiceProvider.getURI());
        NamedWindowMgmtService namedWindowMgmtService = new NamedWindowMgmtServiceImpl(configSnapshot.getEngineDefaults().getLogging().isEnableQueryPlan(), metricsReporting);
        NamedWindowDispatchService namedWindowDispatchService = new NamedWindowDispatchServiceImpl(schedulingService, variableService, tableService, engineSettingsService.getEngineSettings().getExecution().isPrioritized(), eventProcessingRWLock, exceptionHandlingService, metricsReporting);

        ValueAddEventService valueAddEventService = new ValueAddEventServiceImpl();
        valueAddEventService.init(configSnapshot.getRevisionEventTypes(), configSnapshot.getVariantStreams(), eventAdapterService, eventTypeIdGenerator);

        StatementEventTypeRef statementEventTypeRef = new StatementEventTypeRefImpl();
        StatementVariableRef statementVariableRef = new StatementVariableRefImpl(variableService, tableService, namedWindowMgmtService);

        ThreadingService threadingService = new ThreadingServiceImpl(configSnapshot.getEngineDefaults().getThreading());

        InternalEventRouterImpl internalEventRouterImpl = new InternalEventRouterImpl(epServiceProvider.getURI(), engineImportService);

        StatementIsolationServiceImpl statementIsolationService = new StatementIsolationServiceImpl();

        DeploymentStateService deploymentStateService = new DeploymentStateServiceImpl();

        StatementMetadataFactory stmtMetadataFactory;
        if (configSnapshot.getEngineDefaults().getAlternativeContext().getStatementMetadataFactory() == null) {
            stmtMetadataFactory = new StatementMetadataFactoryDefault();
        } else {
            stmtMetadataFactory = (StatementMetadataFactory) JavaClassHelper.instantiate(StatementMetadataFactory.class, configSnapshot.getEngineDefaults().getAlternativeContext().getStatementMetadataFactory(), engineImportService.getClassForNameProvider());
        }

        ContextManagementService contextManagementService = new ContextManagementServiceImpl(engineImportService.getEngineURI());

        PatternSubexpressionPoolEngineSvc patternSubexpressionPoolSvc = null;
        if (configSnapshot.getEngineDefaults().getPatterns().getMaxSubexpressions() != null) {
            patternSubexpressionPoolSvc = new PatternSubexpressionPoolEngineSvc(configSnapshot.getEngineDefaults().getPatterns().getMaxSubexpressions(),
                    configSnapshot.getEngineDefaults().getPatterns().isMaxSubexpressionPreventStart());
        }

        MatchRecognizeStatePoolEngineSvc matchRecognizeStatePoolEngineSvc = null;
        if (configSnapshot.getEngineDefaults().getMatchRecognize().getMaxStates() != null) {
            matchRecognizeStatePoolEngineSvc = new MatchRecognizeStatePoolEngineSvc(configSnapshot.getEngineDefaults().getMatchRecognize().getMaxStates(),
                    configSnapshot.getEngineDefaults().getMatchRecognize().isMaxStatesPreventStart());
        }

        // New services context
        EPServicesContext services = new EPServicesContext(epServiceProvider.getURI(), schedulingService,
                eventAdapterService, engineImportService, engineSettingsService, databaseConfigService, plugInViews,
                statementLockFactory, eventProcessingRWLock, null, jndiContext, statementContextFactory,
                plugInPatternObj, timerService, filterService, streamFactoryService,
                namedWindowMgmtService, namedWindowDispatchService, variableService, tableService, timeSourceService, valueAddEventService, metricsReporting, statementEventTypeRef,
                statementVariableRef, configSnapshot, threadingService, internalEventRouterImpl, statementIsolationService, schedulingMgmtService,
                deploymentStateService, exceptionHandlingService, new PatternNodeFactoryImpl(), eventTypeIdGenerator, stmtMetadataFactory,
                contextManagementService, patternSubexpressionPoolSvc, matchRecognizeStatePoolEngineSvc,
                new DataFlowServiceImpl(epServiceProvider, new DataFlowConfigurationStateServiceImpl()),
                new ExprDeclaredServiceImpl(),
                new ContextControllerFactoryFactorySvcImpl(), new ContextManagerFactoryServiceImpl(),
                new EPStatementFactoryDefault(), new RegexHandlerFactoryDefault(), new ViewableActivatorFactoryDefault(),
                new FilterNonPropertyRegisteryServiceImpl(), new ResultSetProcessorHelperFactoryImpl(),
                new ViewServicePreviousFactoryImpl(), new EventTableIndexServiceImpl(), new EPRuntimeIsolatedFactoryImpl(),
                new FilterBooleanExpressionFactoryImpl(), new DataCacheFactory(), new MultiMatchHandlerFactoryImpl(), NamedWindowConsumerMgmtServiceImpl.INSTANCE,
                AggregationFactoryFactoryDefault.INSTANCE);

        // Engine services subset available to statements
        statementContextFactory.setStmtEngineServices(services);

        // Circular dependency
        StatementLifecycleSvc statementLifecycleSvc = new StatementLifecycleSvcImpl(epServiceProvider, services);
        services.setStatementLifecycleSvc(statementLifecycleSvc);

        // Observers to statement events
        statementLifecycleSvc.addObserver(metricsReporting);

        // Circular dependency
        statementIsolationService.setEpServicesContext(services);

        return services;
    }

    protected static ExceptionHandlingService initExceptionHandling(String engineURI, ConfigurationEngineDefaults.ExceptionHandling exceptionHandling,
                                                                    ConfigurationEngineDefaults.ConditionHandling conditionHandling,
                                                                    EngineImportService engineImportService)
            throws ConfigurationException {
        List<ExceptionHandler> exceptionHandlers;
        if (exceptionHandling.getHandlerFactories() == null || exceptionHandling.getHandlerFactories().isEmpty()) {
            exceptionHandlers = Collections.emptyList();
        } else {
            exceptionHandlers = new ArrayList<ExceptionHandler>();
            ExceptionHandlerFactoryContext context = new ExceptionHandlerFactoryContext(engineURI);
            for (String className : exceptionHandling.getHandlerFactories()) {
                try {
                    ExceptionHandlerFactory factory = (ExceptionHandlerFactory) JavaClassHelper.instantiate(ExceptionHandlerFactory.class, className, engineImportService.getClassForNameProvider());
                    ExceptionHandler handler = factory.getHandler(context);
                    if (handler == null) {
                        log.warn("Exception handler factory '" + className + "' returned a null handler, skipping factory");
                        continue;
                    }
                    exceptionHandlers.add(handler);
                } catch (RuntimeException ex) {
                    throw new ConfigurationException("Exception initializing exception handler from exception handler factory '" + className + "': " + ex.getMessage(), ex);
                }
            }
        }

        List<ConditionHandler> conditionHandlers;
        if (conditionHandling.getHandlerFactories() == null || conditionHandling.getHandlerFactories().isEmpty()) {
            conditionHandlers = Collections.emptyList();
        } else {
            conditionHandlers = new ArrayList<ConditionHandler>();
            ConditionHandlerFactoryContext context = new ConditionHandlerFactoryContext(engineURI);
            for (String className : conditionHandling.getHandlerFactories()) {
                try {
                    ConditionHandlerFactory factory = (ConditionHandlerFactory) JavaClassHelper.instantiate(ConditionHandlerFactory.class, className, engineImportService.getClassForNameProvider());
                    ConditionHandler handler = factory.getHandler(context);
                    if (handler == null) {
                        log.warn("Condition handler factory '" + className + "' returned a null handler, skipping factory");
                        continue;
                    }
                    conditionHandlers.add(handler);
                } catch (RuntimeException ex) {
                    throw new ConfigurationException("Exception initializing exception handler from exception handler factory '" + className + "': " + ex.getMessage(), ex);
                }
            }
        }
        return new ExceptionHandlingService(engineURI, exceptionHandlers, conditionHandlers);
    }

    /**
     * Makes the time source provider.
     *
     * @param configSnapshot the configuration
     * @return time source provider
     */
    protected static TimeSourceService makeTimeSource(ConfigurationInformation configSnapshot) {
        if (configSnapshot.getEngineDefaults().getTimeSource().getTimeSourceType() == ConfigurationEngineDefaults.TimeSourceType.NANO) {
            // this is a static variable to keep overhead down for getting a current time
            TimeSourceServiceImpl.isSystemCurrentTime = false;
        }
        return new TimeSourceServiceImpl();
    }

    /**
     * Adds configured variables to the variable service.
     *
     * @param variableService     service to add to
     * @param variables           configured variables
     * @param engineImportService engine imports
     */
    protected static void initVariables(VariableService variableService, Map<String, ConfigurationVariable> variables, EngineImportService engineImportService) {
        for (Map.Entry<String, ConfigurationVariable> entry : variables.entrySet()) {
            try {
                Pair<String, Boolean> arrayType = JavaClassHelper.isGetArrayType(entry.getValue().getType());
                variableService.createNewVariable(null, entry.getKey(), arrayType.getFirst(), entry.getValue().isConstant(), arrayType.getSecond(), false, entry.getValue().getInitializationValue(), engineImportService);
                variableService.allocateVariableState(entry.getKey(), EPStatementStartMethod.DEFAULT_AGENT_INSTANCE_ID, null, false);
            } catch (VariableExistsException e) {
                throw new ConfigurationException("Error configuring variables: " + e.getMessage(), e);
            } catch (VariableTypeException e) {
                throw new ConfigurationException("Error configuring variables: " + e.getMessage(), e);
            }
        }
    }

    /**
     * Initialize event adapter service for config snapshot.
     *
     * @param eventAdapterService is events adapter
     * @param configSnapshot      is the config snapshot
     * @param engineImportService engine import service
     */
    protected static void init(EventAdapterService eventAdapterService, ConfigurationInformation configSnapshot, EngineImportService engineImportService) {
        // Extract legacy event type definitions for each event type name, if supplied.
        //
        // We supply this information as setup information to the event adapter service
        // to allow discovery of superclasses and interfaces during event type construction for bean events,
        // such that superclasses and interfaces can use the legacy type definitions.
        Map<String, ConfigurationEventTypeLegacy> classLegacyInfo = new HashMap<String, ConfigurationEventTypeLegacy>();
        for (Map.Entry<String, String> entry : configSnapshot.getEventTypeNames().entrySet()) {
            String typeName = entry.getKey();
            String className = entry.getValue();
            ConfigurationEventTypeLegacy legacyDef = configSnapshot.getEventTypesLegacy().get(typeName);
            if (legacyDef != null) {
                classLegacyInfo.put(className, legacyDef);
            }
        }
        eventAdapterService.setClassLegacyConfigs(classLegacyInfo);
        eventAdapterService.setDefaultPropertyResolutionStyle(configSnapshot.getEngineDefaults().getEventMeta().getClassPropertyResolutionStyle());
        eventAdapterService.setDefaultAccessorStyle(configSnapshot.getEngineDefaults().getEventMeta().getDefaultAccessorStyle());

        for (String javaPackage : configSnapshot.getEventTypeAutoNamePackages()) {
            eventAdapterService.addAutoNamePackage(javaPackage);
        }

        // Add from the configuration the Java event class names
        Map<String, String> javaClassNames = configSnapshot.getEventTypeNames();
        for (Map.Entry<String, String> entry : javaClassNames.entrySet()) {
            // Add Java class
            try {
                String typeName = entry.getKey();
                eventAdapterService.addBeanType(typeName, entry.getValue(), false, true, true, true);
            } catch (EventAdapterException ex) {
                throw new ConfigurationException("Error configuring engine: " + ex.getMessage(), ex);
            }
        }

        // Add from the configuration the Java event class names
        Map<String, ConfigurationEventTypeAvro> avroNames = configSnapshot.getEventTypesAvro();
        for (Map.Entry<String, ConfigurationEventTypeAvro> entry : avroNames.entrySet()) {
            try {
                eventAdapterService.addAvroType(entry.getKey(), entry.getValue(), true, true, true, false, false);
            } catch (EventAdapterException ex) {
                throw new ConfigurationException("Error configuring engine: " + ex.getMessage(), ex);
            }
        }

        // Add from the configuration the XML DOM names and type def
        Map<String, ConfigurationEventTypeXMLDOM> xmlDOMNames = configSnapshot.getEventTypesXMLDOM();
        for (Map.Entry<String, ConfigurationEventTypeXMLDOM> entry : xmlDOMNames.entrySet()) {
            SchemaModel schemaModel = null;
            if ((entry.getValue().getSchemaResource() != null) || (entry.getValue().getSchemaText() != null)) {
                try {
                    schemaModel = XSDSchemaMapper.loadAndMap(entry.getValue().getSchemaResource(), entry.getValue().getSchemaText(), engineImportService);
                } catch (Exception ex) {
                    throw new ConfigurationException(ex.getMessage(), ex);
                }
            }

            // Add XML DOM type
            try {
                eventAdapterService.addXMLDOMType(entry.getKey(), entry.getValue(), schemaModel, true);
            } catch (EventAdapterException ex) {
                throw new ConfigurationException("Error configuring engine: " + ex.getMessage(), ex);
            }
        }

        // Add maps in dependency order such that supertypes are added before subtypes
        Set<String> dependentMapOrder;
        try {
            Map<String, Set<String>> typesReferences = toTypesReferences(configSnapshot.getMapTypeConfigurations());
            dependentMapOrder = GraphUtil.getTopDownOrder(typesReferences);
        } catch (GraphCircularDependencyException e) {
            throw new ConfigurationException("Error configuring engine, dependency graph between map type names is circular: " + e.getMessage(), e);
        }

        Map<String, Properties> mapNames = configSnapshot.getEventTypesMapEvents();
        Map<String, Map<String, Object>> nestableMapNames = configSnapshot.getEventTypesNestableMapEvents();
        dependentMapOrder.addAll(mapNames.keySet());
        dependentMapOrder.addAll(nestableMapNames.keySet());
        try {
            for (String mapName : dependentMapOrder) {
                ConfigurationEventTypeMap mapConfig = configSnapshot.getMapTypeConfigurations().get(mapName);
                Properties propertiesUnnested = mapNames.get(mapName);
                if (propertiesUnnested != null) {
                    Map<String, Object> propertyTypes = createPropertyTypes(propertiesUnnested, engineImportService);
                    Map<String, Object> propertyTypesCompiled = EventTypeUtility.compileMapTypeProperties(propertyTypes, eventAdapterService);
                    eventAdapterService.addNestableMapType(mapName, propertyTypesCompiled, mapConfig, true, true, true, false, false);
                }

                Map<String, Object> propertiesNestable = nestableMapNames.get(mapName);
                if (propertiesNestable != null) {
                    Map<String, Object> propertiesNestableCompiled = EventTypeUtility.compileMapTypeProperties(propertiesNestable, eventAdapterService);
                    eventAdapterService.addNestableMapType(mapName, propertiesNestableCompiled, mapConfig, true, true, true, false, false);
                }
            }
        } catch (EventAdapterException ex) {
            throw new ConfigurationException("Error configuring engine: " + ex.getMessage(), ex);
        }

        // Add object-array in dependency order such that supertypes are added before subtypes
        Set<String> dependentObjectArrayOrder;
        try {
            Map<String, Set<String>> typesReferences = toTypesReferences(configSnapshot.getObjectArrayTypeConfigurations());
            dependentObjectArrayOrder = GraphUtil.getTopDownOrder(typesReferences);
        } catch (GraphCircularDependencyException e) {
            throw new ConfigurationException("Error configuring engine, dependency graph between object array type names is circular: " + e.getMessage(), e);
        }
        Map<String, Map<String, Object>> nestableObjectArrayNames = configSnapshot.getEventTypesNestableObjectArrayEvents();
        dependentObjectArrayOrder.addAll(nestableObjectArrayNames.keySet());
        try {
            for (String objectArrayName : dependentObjectArrayOrder) {
                ConfigurationEventTypeObjectArray objectArrayConfig = configSnapshot.getObjectArrayTypeConfigurations().get(objectArrayName);
                Map<String, Object> propertyTypes = nestableObjectArrayNames.get(objectArrayName);
                propertyTypes = resolveClassesForStringPropertyTypes(propertyTypes, engineImportService);
                Map<String, Object> propertyTypesCompiled = EventTypeUtility.compileMapTypeProperties(propertyTypes, eventAdapterService);
                eventAdapterService.addNestableObjectArrayType(objectArrayName, propertyTypesCompiled, objectArrayConfig, true, true, true, false, false, false, null);
            }
        } catch (EventAdapterException ex) {
            throw new ConfigurationException("Error configuring engine: " + ex.getMessage(), ex);
        }

        // Add plug-in event representations
        Map<URI, ConfigurationPlugInEventRepresentation> plugInReps = configSnapshot.getPlugInEventRepresentation();
        for (Map.Entry<URI, ConfigurationPlugInEventRepresentation> entry : plugInReps.entrySet()) {
            String className = entry.getValue().getEventRepresentationClassName();
            Class eventRepClass;
            try {
                eventRepClass = engineImportService.getClassForNameProvider().classForName(className);
            } catch (ClassNotFoundException ex) {
                throw new ConfigurationException("Failed to load plug-in event representation class '" + className + "'", ex);
            }

            Object pluginEventRepObj;
            try {
                pluginEventRepObj = eventRepClass.newInstance();
            } catch (InstantiationException ex) {
                throw new ConfigurationException("Failed to instantiate plug-in event representation class '" + className + "' via default constructor", ex);
            } catch (IllegalAccessException ex) {
                throw new ConfigurationException("Illegal access to instantiate plug-in event representation class '" + className + "' via default constructor", ex);
            }

            if (!(pluginEventRepObj instanceof PlugInEventRepresentation)) {
                throw new ConfigurationException("Plug-in event representation class '" + className + "' does not implement the required interface " + PlugInEventRepresentation.class.getName());
            }

            URI eventRepURI = entry.getKey();
            PlugInEventRepresentation pluginEventRep = (PlugInEventRepresentation) pluginEventRepObj;
            Serializable initializer = entry.getValue().getInitializer();
            PlugInEventRepresentationContext context = new PlugInEventRepresentationContext(eventAdapterService, eventRepURI, initializer);

            try {
                pluginEventRep.init(context);
                eventAdapterService.addEventRepresentation(eventRepURI, pluginEventRep);
            } catch (Throwable t) {
                throw new ConfigurationException("Plug-in event representation class '" + className + "' and URI '" + eventRepURI + "' did not initialize correctly : " + t.getMessage(), t);
            }
        }

        // Add plug-in event type names
        Map<String, ConfigurationPlugInEventType> plugInNames = configSnapshot.getPlugInEventTypes();
        for (Map.Entry<String, ConfigurationPlugInEventType> entry : plugInNames.entrySet()) {
            String name = entry.getKey();
            ConfigurationPlugInEventType config = entry.getValue();
            eventAdapterService.addPlugInEventType(name, config.getEventRepresentationResolutionURIs(), config.getInitializer());
        }
    }

    private static Map<String, Set<String>> toTypesReferences(Map<String, ? extends ConfigurationEventTypeWithSupertype> mapTypeConfigurations) {
        Map<String, Set<String>> result = new LinkedHashMap<String, Set<String>>();
        for (Map.Entry<String, ? extends ConfigurationEventTypeWithSupertype> entry : mapTypeConfigurations.entrySet()) {
            result.put(entry.getKey(), entry.getValue().getSuperTypes());
        }
        return result;
    }


    /**
     * Constructs the auto import service.
     *
     * @param configSnapshot            config info
     * @param aggregationFactoryFactory factory of aggregation service provider
     * @param engineURI engine URI
     * @return service
     */
    protected static EngineImportService makeEngineImportService(ConfigurationInformation configSnapshot, AggregationFactoryFactory aggregationFactoryFactory, String engineURI) {
        TimeUnit timeUnit = configSnapshot.getEngineDefaults().getTimeSource().getTimeUnit();
        TimeAbacus timeAbacus;
        if (timeUnit == TimeUnit.MILLISECONDS) {
            timeAbacus = TimeAbacusMilliseconds.INSTANCE;
        } else if (timeUnit == TimeUnit.MICROSECONDS) {
            timeAbacus = TimeAbacusMicroseconds.INSTANCE;
        } else {
            throw new ConfigurationException("Invalid time-source time unit of " + timeUnit + ", expected millis or micros");
        }

        CodegenCompiler codegenCompiler = null;
        if (configSnapshot.getEngineDefaults().getByteCodeGeneration().isEnabledAny()) {
            codegenCompiler = new CodegenCompilerJanino(engineURI, configSnapshot.getEngineDefaults().getLogging().isEnableCode(), configSnapshot.getEngineDefaults().getByteCodeGeneration().isIncludeDebugSymbols());
        }

        ConfigurationEngineDefaults.Expression expression = configSnapshot.getEngineDefaults().getExpression();
        EngineImportServiceImpl engineImportService = new EngineImportServiceImpl(expression.isExtendedAggregation(),
                expression.isUdfCache(), expression.isDuckTyping(),
                configSnapshot.getEngineDefaults().getLanguage().isSortUsingCollator(),
                configSnapshot.getEngineDefaults().getExpression().getMathContext(),
                configSnapshot.getEngineDefaults().getExpression().getTimeZone(), timeAbacus,
                configSnapshot.getEngineDefaults().getExecution().getThreadingProfile(),
                configSnapshot.getTransientConfiguration(),
                aggregationFactoryFactory, configSnapshot.getEngineDefaults().getByteCodeGeneration(), engineURI, codegenCompiler);
        engineImportService.addMethodRefs(configSnapshot.getMethodInvocationReferences());

        // Add auto-imports
        try {
            for (String importName : configSnapshot.getImports()) {
                engineImportService.addImport(importName);
            }

            for (String importName : configSnapshot.getAnnotationImports()) {
                engineImportService.addAnnotationImport(importName);
            }

            for (ConfigurationPlugInAggregationFunction config : configSnapshot.getPlugInAggregationFunctions()) {
                engineImportService.addAggregation(config.getName(), config);
            }

            for (ConfigurationPlugInAggregationMultiFunction config : configSnapshot.getPlugInAggregationMultiFunctions()) {
                engineImportService.addAggregationMultiFunction(config);
            }

            for (ConfigurationPlugInSingleRowFunction config : configSnapshot.getPlugInSingleRowFunctions()) {
                engineImportService.addSingleRow(config.getName(), config.getFunctionClassName(), config.getFunctionMethodName(), config.getValueCache(), config.getFilterOptimizable(), config.isRethrowExceptions(), config.getEventTypeName());
            }
        } catch (EngineImportException ex) {
            throw new ConfigurationException("Error configuring engine: " + ex.getMessage(), ex);
        }

        return engineImportService;
    }

    /**
     * Creates the database config service.
     *
     * @param configSnapshot        is the config snapshot
     * @param schedulingService     is the timer stuff
     * @param schedulingMgmtService for statement schedule management
     * @param engineImportService   engine import service
     * @return database config svc
     */
    protected static DatabaseConfigService makeDatabaseRefService(ConfigurationInformation configSnapshot,
                                                                  SchedulingService schedulingService,
                                                                  SchedulingMgmtService schedulingMgmtService,
                                                                  EngineImportService engineImportService) {
        DatabaseConfigService databaseConfigService;

        // Add auto-imports
        try {
            ScheduleBucket allStatementsBucket = schedulingMgmtService.allocateBucket();
            databaseConfigService = new DatabaseConfigServiceImpl(configSnapshot.getDatabaseReferences(), schedulingService, allStatementsBucket, engineImportService);
        } catch (IllegalArgumentException ex) {
            throw new ConfigurationException("Error configuring engine: " + ex.getMessage(), ex);
        }

        return databaseConfigService;
    }

    private static Map<String, Object> createPropertyTypes(Properties properties, EngineImportService engineImportService) {
        Map<String, Object> propertyTypes = new LinkedHashMap<String, Object>();
        for (Map.Entry entry : properties.entrySet()) {
            String property = (String) entry.getKey();
            String className = (String) entry.getValue();
            Class clazz = resolveClassForTypeName(className, engineImportService);
            if (clazz != null) {
                propertyTypes.put(property, clazz);
            }
        }
        return propertyTypes;
    }

    private static Map<String, Object> resolveClassesForStringPropertyTypes(Map<String, Object> properties, EngineImportService engineImportService) {
        Map<String, Object> propertyTypes = new LinkedHashMap<String, Object>();
        for (Map.Entry entry : properties.entrySet()) {
            String property = (String) entry.getKey();
            propertyTypes.put(property, entry.getValue());
            if (!(entry.getValue() instanceof String)) {
                continue;
            }
            String className = (String) entry.getValue();
            Class clazz = resolveClassForTypeName(className, engineImportService);
            if (clazz != null) {
                propertyTypes.put(property, clazz);
            }
        }
        return propertyTypes;
    }

    private static Class resolveClassForTypeName(String type, EngineImportService engineImportService) {
        boolean isArray = false;
        if (type != null && EventTypeUtility.isPropertyArray(type)) {
            isArray = true;
            type = EventTypeUtility.getPropertyRemoveArray(type);
        }

        if (type == null) {
            throw new ConfigurationException("A null value has been provided for the type");
        }
        Class clazz = JavaClassHelper.getClassForSimpleName(type, engineImportService.getClassForNameProvider());
        if (clazz == null) {
            throw new ConfigurationException("The type '" + type + "' is not a recognized type");
        }

        if (isArray) {
            clazz = Array.newInstance(clazz, 0).getClass();
        }
        return clazz;
    }
}
