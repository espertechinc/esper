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
package com.espertech.esper.compiler.internal.util;

import com.espertech.esper.common.client.EPCompiled;
import com.espertech.esper.common.client.EPCompilerPathable;
import com.espertech.esper.common.client.EPException;
import com.espertech.esper.common.client.EventType;
import com.espertech.esper.common.client.configuration.Configuration;
import com.espertech.esper.common.client.configuration.ConfigurationException;
import com.espertech.esper.common.client.configuration.compiler.*;
import com.espertech.esper.common.client.serde.SerdeProvider;
import com.espertech.esper.common.client.serde.SerdeProviderFactory;
import com.espertech.esper.common.client.serde.SerdeProviderFactoryContext;
import com.espertech.esper.common.internal.collection.PathException;
import com.espertech.esper.common.internal.collection.PathRegistry;
import com.espertech.esper.common.internal.collection.PathRegistryObjectType;
import com.espertech.esper.common.internal.compile.stage1.CompilerServices;
import com.espertech.esper.common.internal.compile.stage1.spec.ExpressionDeclItem;
import com.espertech.esper.common.internal.compile.stage1.spec.ExpressionScriptProvided;
import com.espertech.esper.common.internal.compile.stage1.spec.PluggableObjectCollection;
import com.espertech.esper.common.internal.compile.stage1.spec.PluggableObjectRegistryImpl;
import com.espertech.esper.common.internal.compile.stage3.ModuleAccessModifierService;
import com.espertech.esper.common.internal.compile.stage3.ModuleCompileTimeServices;
import com.espertech.esper.common.internal.context.compile.*;
import com.espertech.esper.common.internal.context.module.*;
import com.espertech.esper.common.internal.context.util.ParentClassLoader;
import com.espertech.esper.common.internal.epl.expression.declared.compiletime.ExprDeclaredCollectorCompileTime;
import com.espertech.esper.common.internal.epl.expression.declared.compiletime.ExprDeclaredCompileTimeRegistry;
import com.espertech.esper.common.internal.epl.expression.declared.compiletime.ExprDeclaredCompileTimeResolver;
import com.espertech.esper.common.internal.epl.expression.declared.compiletime.ExprDeclaredCompileTimeResolverImpl;
import com.espertech.esper.common.internal.epl.expression.declared.core.ExprDeclaredCollector;
import com.espertech.esper.common.internal.epl.expression.time.abacus.TimeAbacus;
import com.espertech.esper.common.internal.epl.expression.time.abacus.TimeAbacusFactory;
import com.espertech.esper.common.internal.epl.historical.database.connection.DatabaseConfigServiceCompileTime;
import com.espertech.esper.common.internal.epl.historical.database.connection.DatabaseConfigServiceImpl;
import com.espertech.esper.common.internal.epl.index.compile.IndexCollectorCompileTime;
import com.espertech.esper.common.internal.epl.index.compile.IndexCompileTimeRegistry;
import com.espertech.esper.common.internal.epl.namedwindow.compile.NamedWindowCompileTimeRegistry;
import com.espertech.esper.common.internal.epl.namedwindow.path.*;
import com.espertech.esper.common.internal.epl.pattern.core.PatternObjectHelper;
import com.espertech.esper.common.internal.epl.pattern.core.PatternObjectResolutionService;
import com.espertech.esper.common.internal.epl.pattern.core.PatternObjectResolutionServiceImpl;
import com.espertech.esper.common.internal.epl.script.compiletime.ScriptCollectorCompileTime;
import com.espertech.esper.common.internal.epl.script.compiletime.ScriptCompileTimeRegistry;
import com.espertech.esper.common.internal.epl.script.compiletime.ScriptCompileTimeResolver;
import com.espertech.esper.common.internal.epl.script.compiletime.ScriptCompileTimeResolverImpl;
import com.espertech.esper.common.internal.epl.script.core.NameAndParamNum;
import com.espertech.esper.common.internal.epl.table.compiletime.TableCompileTimeRegistry;
import com.espertech.esper.common.internal.epl.table.compiletime.TableCompileTimeResolver;
import com.espertech.esper.common.internal.epl.table.compiletime.TableCompileTimeResolverImpl;
import com.espertech.esper.common.internal.epl.table.compiletime.TableMetaData;
import com.espertech.esper.common.internal.epl.table.core.TableCollector;
import com.espertech.esper.common.internal.epl.table.core.TableCollectorImpl;
import com.espertech.esper.common.internal.epl.util.EPCompilerPathableImpl;
import com.espertech.esper.common.internal.epl.variable.compiletime.VariableCompileTimeRegistry;
import com.espertech.esper.common.internal.epl.variable.compiletime.VariableCompileTimeResolver;
import com.espertech.esper.common.internal.epl.variable.compiletime.VariableCompileTimeResolverImpl;
import com.espertech.esper.common.internal.epl.variable.compiletime.VariableMetaData;
import com.espertech.esper.common.internal.epl.variable.core.VariableCollectorImpl;
import com.espertech.esper.common.internal.epl.variable.core.VariableRepositoryPreconfigured;
import com.espertech.esper.common.internal.epl.variable.core.VariableUtil;
import com.espertech.esper.common.internal.event.avro.EventTypeAvroHandler;
import com.espertech.esper.common.internal.event.avro.EventTypeAvroHandlerFactory;
import com.espertech.esper.common.internal.event.bean.core.BeanEventTypeRepoUtil;
import com.espertech.esper.common.internal.event.bean.core.BeanEventTypeStemService;
import com.espertech.esper.common.internal.event.bean.service.BeanEventTypeFactoryPrivate;
import com.espertech.esper.common.internal.event.bean.service.EventTypeRepositoryBeanTypeUtil;
import com.espertech.esper.common.internal.event.core.EventBeanTypedEventFactoryCompileTime;
import com.espertech.esper.common.internal.event.core.EventTypeCompileTimeResolver;
import com.espertech.esper.common.internal.event.eventtypefactory.EventTypeFactoryImpl;
import com.espertech.esper.common.internal.event.eventtyperepo.*;
import com.espertech.esper.common.internal.event.json.compiletime.JsonEventTypeUtility;
import com.espertech.esper.common.internal.event.path.EventTypeCollectorImpl;
import com.espertech.esper.common.internal.event.path.EventTypeResolverImpl;
import com.espertech.esper.common.internal.event.xml.XMLFragmentEventTypeFactory;
import com.espertech.esper.common.internal.serde.compiletime.eventtype.SerdeEventTypeCompileTimeRegistry;
import com.espertech.esper.common.internal.serde.compiletime.eventtype.SerdeEventTypeCompileTimeRegistryImpl;
import com.espertech.esper.common.internal.serde.compiletime.resolve.SerdeCompileTimeResolver;
import com.espertech.esper.common.internal.serde.compiletime.resolve.SerdeCompileTimeResolverImpl;
import com.espertech.esper.common.internal.serde.compiletime.resolve.SerdeCompileTimeResolverNonHA;
import com.espertech.esper.common.internal.serde.runtime.event.EventSerdeFactoryDefault;
import com.espertech.esper.common.internal.settings.ClasspathImportException;
import com.espertech.esper.common.internal.settings.ClasspathImportServiceCompileTime;
import com.espertech.esper.common.internal.util.JavaClassHelper;
import com.espertech.esper.common.internal.util.TransientConfigurationResolver;
import com.espertech.esper.common.internal.view.core.ViewEnumHelper;
import com.espertech.esper.common.internal.view.core.ViewResolutionService;
import com.espertech.esper.common.internal.view.core.ViewResolutionServiceImpl;
import com.espertech.esper.compiler.client.CompilerArguments;
import com.espertech.esper.compiler.client.CompilerOptions;
import com.espertech.esper.compiler.client.CompilerPath;
import com.espertech.esper.compiler.client.EPCompileException;

import java.util.*;

public class CompilerHelperServices {
    protected static ModuleCompileTimeServices getCompileTimeServices(CompilerArguments arguments, String moduleName, Set<String> moduleUses, boolean isFireAndForget) throws EPCompileException {
        try {
            return getServices(arguments, moduleName, moduleUses, isFireAndForget);
        } catch (EPCompileException ex) {
            throw ex;
        } catch (Throwable t) {
            throw new EPCompileException("Failed compiler startup: " + t.getMessage(), t, Collections.emptyList());
        }
    }

    private static ModuleCompileTimeServices getServices(CompilerArguments arguments, String moduleName, Set<String> moduleUses, boolean isFireAndForget) throws EPCompileException {
        Configuration configuration = arguments.getConfiguration();
        CompilerPath path = arguments.getPath();
        CompilerOptions options = arguments.getOptions();

        // imports
        ClasspathImportServiceCompileTime classpathImportServiceCompileTime = makeClasspathImportService(configuration);
        ParentClassLoader classLoaderParent = new ParentClassLoader(classpathImportServiceCompileTime.getClassLoader());

        // resolve pre-configured bean event types, make bean-stem service
        Map<String, Class> resolvedBeanEventTypes = BeanEventTypeRepoUtil.resolveBeanEventTypes(configuration.getCommon().getEventTypeNames(), classpathImportServiceCompileTime);
        BeanEventTypeStemService beanEventTypeStemService = BeanEventTypeRepoUtil.makeBeanEventTypeStemService(configuration, resolvedBeanEventTypes, EventBeanTypedEventFactoryCompileTime.INSTANCE);

        // allocate repositories
        EventTypeRepositoryImpl eventTypeRepositoryPreconfigured = new EventTypeRepositoryImpl(true);
        EventTypeCompileTimeRegistry eventTypeCompileRegistry = new EventTypeCompileTimeRegistry(eventTypeRepositoryPreconfigured);
        BeanEventTypeFactoryPrivate beanEventTypeFactoryPrivate = new BeanEventTypeFactoryPrivate(EventBeanTypedEventFactoryCompileTime.INSTANCE, EventTypeFactoryImpl.INSTANCE, beanEventTypeStemService);
        VariableRepositoryPreconfigured variableRepositoryPreconfigured = new VariableRepositoryPreconfigured();

        // allocate path registries
        PathRegistry<String, EventType> pathEventTypes = new PathRegistry<>(PathRegistryObjectType.EVENTTYPE);
        PathRegistry<String, NamedWindowMetaData> pathNamedWindows = new PathRegistry<>(PathRegistryObjectType.NAMEDWINDOW);
        PathRegistry<String, TableMetaData> pathTables = new PathRegistry<>(PathRegistryObjectType.TABLE);
        PathRegistry<String, ContextMetaData> pathContexts = new PathRegistry<>(PathRegistryObjectType.CONTEXT);
        PathRegistry<String, VariableMetaData> pathVariables = new PathRegistry<>(PathRegistryObjectType.VARIABLE);
        PathRegistry<String, ExpressionDeclItem> pathExprDeclared = new PathRegistry<>(PathRegistryObjectType.EXPRDECL);
        PathRegistry<NameAndParamNum, ExpressionScriptProvided> pathScript = new PathRegistry<>(PathRegistryObjectType.SCRIPT);

        // add runtime-path which is the information an existing runtime may have
        if (path.getCompilerPathables() != null) {
            for (EPCompilerPathable pathable : path.getCompilerPathables()) {
                EPCompilerPathableImpl impl = (EPCompilerPathableImpl) pathable;
                pathVariables.mergeFrom(impl.getVariablePathRegistry());
                pathEventTypes.mergeFrom(impl.getEventTypePathRegistry());
                pathExprDeclared.mergeFrom(impl.getExprDeclaredPathRegistry());
                pathNamedWindows.mergeFrom(impl.getNamedWindowPathRegistry());
                pathTables.mergeFrom(impl.getTablePathRegistry());
                pathContexts.mergeFrom(impl.getContextPathRegistry());
                pathScript.mergeFrom(impl.getScriptPathRegistry());
                eventTypeRepositoryPreconfigured.mergeFrom(impl.getEventTypePreconfigured());
                variableRepositoryPreconfigured.mergeFrom(impl.getVariablePreconfigured());

                JsonEventTypeUtility.addJsonUnderlyingClass(pathEventTypes, classLoaderParent);
            }
        }

        // build preconfigured type system
        EventTypeRepositoryBeanTypeUtil.buildBeanTypes(beanEventTypeStemService, eventTypeRepositoryPreconfigured, resolvedBeanEventTypes, beanEventTypeFactoryPrivate, configuration.getCommon().getEventTypesBean());
        EventTypeRepositoryMapTypeUtil.buildMapTypes(eventTypeRepositoryPreconfigured, configuration.getCommon().getMapTypeConfigurations(), configuration.getCommon().getEventTypesMapEvents(), configuration.getCommon().getEventTypesNestableMapEvents(), beanEventTypeFactoryPrivate, classpathImportServiceCompileTime);
        EventTypeRepositoryOATypeUtil.buildOATypes(eventTypeRepositoryPreconfigured, configuration.getCommon().getObjectArrayTypeConfigurations(), configuration.getCommon().getEventTypesNestableObjectArrayEvents(), beanEventTypeFactoryPrivate, classpathImportServiceCompileTime);
        XMLFragmentEventTypeFactory xmlFragmentEventTypeFactory = new XMLFragmentEventTypeFactory(beanEventTypeFactoryPrivate, eventTypeCompileRegistry, eventTypeRepositoryPreconfigured);
        EventTypeRepositoryXMLTypeUtil.buildXMLTypes(eventTypeRepositoryPreconfigured, configuration.getCommon().getEventTypesXMLDOM(), beanEventTypeFactoryPrivate, xmlFragmentEventTypeFactory, classpathImportServiceCompileTime);
        EventTypeAvroHandler eventTypeAvroHandler = EventTypeAvroHandlerFactory.resolve(classpathImportServiceCompileTime, configuration.getCommon().getEventMeta().getAvroSettings(), EventTypeAvroHandler.COMPILE_TIME_HANDLER_IMPL);
        EventTypeRepositoryAvroTypeUtil.buildAvroTypes(eventTypeRepositoryPreconfigured, configuration.getCommon().getEventTypesAvro(), eventTypeAvroHandler, beanEventTypeFactoryPrivate.getEventBeanTypedEventFactory());
        EventTypeRepositoryVariantStreamUtil.buildVariantStreams(eventTypeRepositoryPreconfigured, configuration.getCommon().getVariantStreams(), EventTypeFactoryImpl.INSTANCE);

        // build preconfigured variables
        VariableUtil.configureVariables(variableRepositoryPreconfigured, configuration.getCommon().getVariables(), classpathImportServiceCompileTime, EventBeanTypedEventFactoryCompileTime.INSTANCE, eventTypeRepositoryPreconfigured, beanEventTypeFactoryPrivate);

        int deploymentNumber = -1;

        for (EPCompiled unit : path.getCompileds()) {
            deploymentNumber++;
            ModuleProviderResult provider = ModuleProviderUtil.analyze(unit, classLoaderParent);
            String unitModuleName = provider.getModuleProvider().getModuleName();

            // initialize event types
            Map<String, EventType> moduleTypes = new LinkedHashMap<>();
            EventTypeResolverImpl eventTypeResolver = new EventTypeResolverImpl(moduleTypes, pathEventTypes, eventTypeRepositoryPreconfigured, beanEventTypeFactoryPrivate, EventSerdeFactoryDefault.INSTANCE);
            EventTypeCollectorImpl eventTypeCollector = new EventTypeCollectorImpl(moduleTypes, beanEventTypeFactoryPrivate, provider.getClassLoader(), EventTypeFactoryImpl.INSTANCE, beanEventTypeStemService, eventTypeResolver, xmlFragmentEventTypeFactory, eventTypeAvroHandler, EventBeanTypedEventFactoryCompileTime.INSTANCE, classpathImportServiceCompileTime);
            try {
                provider.getModuleProvider().initializeEventTypes(new EPModuleEventTypeInitServicesImpl(eventTypeCollector, eventTypeResolver));
            } catch (Throwable e) {
                throw new EPException(e);
            }
            JsonEventTypeUtility.addJsonUnderlyingClass(moduleTypes, classLoaderParent, null);

            // initialize named windows
            Map<String, NamedWindowMetaData> moduleNamedWindows = new HashMap<>();
            NamedWindowCollector namedWindowCollector = new NamedWindowCollectorImpl(moduleNamedWindows);
            try {
                provider.getModuleProvider().initializeNamedWindows(new EPModuleNamedWindowInitServicesImpl(namedWindowCollector, eventTypeResolver));
            } catch (Throwable e) {
                throw new EPException(e);
            }

            // initialize tables
            Map<String, TableMetaData> moduleTables = new HashMap<>();
            TableCollector tableCollector = new TableCollectorImpl(moduleTables);
            try {
                provider.getModuleProvider().initializeTables(new EPModuleTableInitServicesImpl(tableCollector, eventTypeResolver));
            } catch (Throwable e) {
                throw new EPException(e);
            }

            // initialize create-index indexes
            IndexCollectorCompileTime indexCollector = new IndexCollectorCompileTime(moduleNamedWindows, moduleTables, pathNamedWindows, pathTables);
            try {
                provider.getModuleProvider().initializeIndexes(new EPModuleIndexInitServicesImpl(indexCollector));
            } catch (Throwable e) {
                throw new EPException(e);
            }

            // initialize create-contexts
            Map<String, ContextMetaData> moduleContexts = new HashMap<>();
            ContextCollectorImpl contextCollector = new ContextCollectorImpl(moduleContexts);
            try {
                provider.getModuleProvider().initializeContexts(new EPModuleContextInitServicesImpl(contextCollector, eventTypeResolver));
            } catch (Throwable e) {
                throw new EPException(e);
            }

            // initialize variables
            Map<String, VariableMetaData> moduleVariables = new HashMap<>();
            VariableCollectorImpl variableCollector = new VariableCollectorImpl(moduleVariables);
            try {
                provider.getModuleProvider().initializeVariables(new EPModuleVariableInitServicesImpl(variableCollector, eventTypeResolver));
            } catch (Throwable e) {
                throw new EPException(e);
            }

            // initialize module expressions
            Map<String, ExpressionDeclItem> moduleExprDeclareds = new HashMap<>();
            ExprDeclaredCollector exprDeclaredCollector = new ExprDeclaredCollectorCompileTime(moduleExprDeclareds);
            try {
                provider.getModuleProvider().initializeExprDeclareds(new EPModuleExprDeclaredInitServicesImpl(exprDeclaredCollector));
            } catch (Throwable e) {
                throw new EPException(e);
            }

            // initialize module scripts
            Map<NameAndParamNum, ExpressionScriptProvided> moduleScripts = new HashMap<>();
            ScriptCollectorCompileTime scriptCollector = new ScriptCollectorCompileTime(moduleScripts);
            try {
                provider.getModuleProvider().initializeScripts(new EPModuleScriptInitServicesImpl(scriptCollector));
            } catch (Throwable e) {
                throw new EPException(e);
            }

            // save path-visibility event types and named windows to the path
            String deploymentId = "D" + deploymentNumber;
            try {
                for (Map.Entry<String, EventType> type : moduleTypes.entrySet()) {
                    if (type.getValue().getMetadata().getAccessModifier().isNonPrivateNonTransient()) {
                        pathEventTypes.add(type.getKey(), unitModuleName, type.getValue(), deploymentId);
                    }
                }
                for (Map.Entry<String, NamedWindowMetaData> entry : moduleNamedWindows.entrySet()) {
                    if (entry.getValue().getEventType().getMetadata().getAccessModifier().isNonPrivateNonTransient()) {
                        pathNamedWindows.add(entry.getKey(), unitModuleName, entry.getValue(), deploymentId);
                    }
                }
                for (Map.Entry<String, TableMetaData> entry : moduleTables.entrySet()) {
                    if (entry.getValue().getTableVisibility().isNonPrivateNonTransient()) {
                        pathTables.add(entry.getKey(), unitModuleName, entry.getValue(), deploymentId);
                    }
                }
                for (Map.Entry<String, ContextMetaData> entry : moduleContexts.entrySet()) {
                    if (entry.getValue().getContextVisibility().isNonPrivateNonTransient()) {
                        pathContexts.add(entry.getKey(), unitModuleName, entry.getValue(), deploymentId);
                    }
                }
                for (Map.Entry<String, VariableMetaData> entry : moduleVariables.entrySet()) {
                    if (entry.getValue().getVariableVisibility().isNonPrivateNonTransient()) {
                        pathVariables.add(entry.getKey(), unitModuleName, entry.getValue(), deploymentId);
                    }
                }
                for (Map.Entry<String, ExpressionDeclItem> entry : moduleExprDeclareds.entrySet()) {
                    if (entry.getValue().getVisibility().isNonPrivateNonTransient()) {
                        pathExprDeclared.add(entry.getKey(), unitModuleName, entry.getValue(), deploymentId);
                    }
                }
                for (Map.Entry<NameAndParamNum, ExpressionScriptProvided> entry : moduleScripts.entrySet()) {
                    if (entry.getValue().getVisibility().isNonPrivateNonTransient()) {
                        pathScript.add(entry.getKey(), unitModuleName, entry.getValue(), deploymentId);
                    }
                }
            } catch (PathException ex) {
                throw new EPCompileException("Invalid path: " + ex.getMessage(), ex, Collections.emptyList());
            }
        }

        ModuleDependenciesCompileTime moduleDependencies = new ModuleDependenciesCompileTime();

        // build bean space of public and protected
        EventTypeCompileTimeResolver eventTypeCompileTimeResolver = new EventTypeCompileTimeResolver(moduleName, moduleUses, eventTypeCompileRegistry, eventTypeRepositoryPreconfigured, pathEventTypes, moduleDependencies, isFireAndForget);

        // build named window registry
        NamedWindowCompileTimeRegistry namedWindowCompileTimeRegistry = new NamedWindowCompileTimeRegistry();
        NamedWindowCompileTimeResolver namedWindowCompileTimeResolver = new NamedWindowCompileTimeResolverImpl(moduleName, moduleUses, namedWindowCompileTimeRegistry, pathNamedWindows, moduleDependencies, isFireAndForget);

        // build context registry
        ContextCompileTimeRegistry contextCompileTimeRegistry = new ContextCompileTimeRegistry();
        ContextCompileTimeResolver contextCompileTimeResolver = new ContextCompileTimeResolverImpl(moduleName, moduleUses, contextCompileTimeRegistry, pathContexts, moduleDependencies, isFireAndForget);

        // build variable registry
        VariableCompileTimeRegistry variableCompileTimeRegistry = new VariableCompileTimeRegistry();
        VariableCompileTimeResolver variableCompileTimeResolver = new VariableCompileTimeResolverImpl(moduleName, moduleUses, variableRepositoryPreconfigured, variableCompileTimeRegistry, pathVariables, moduleDependencies, isFireAndForget);

        // build declared-expression registry
        ExprDeclaredCompileTimeRegistry exprDeclaredCompileTimeRegistry = new ExprDeclaredCompileTimeRegistry();
        ExprDeclaredCompileTimeResolver exprDeclaredCompileTimeResolver = new ExprDeclaredCompileTimeResolverImpl(moduleName, moduleUses, exprDeclaredCompileTimeRegistry, pathExprDeclared, moduleDependencies, isFireAndForget);

        // build table-registry
        Map<String, TableMetaData> localTables = new HashMap<>();
        TableCompileTimeRegistry tableCompileTimeRegistry = new TableCompileTimeRegistry(localTables);
        TableCompileTimeResolver tableCompileTimeResolver = new TableCompileTimeResolverImpl(moduleName, moduleUses, tableCompileTimeRegistry, pathTables, moduleDependencies, isFireAndForget);

        // build script registry
        ScriptCompileTimeRegistry scriptCompileTimeRegistry = new ScriptCompileTimeRegistry();
        ScriptCompileTimeResolver scriptCompileTimeResolver = new ScriptCompileTimeResolverImpl(moduleName, moduleUses, scriptCompileTimeRegistry, pathScript, moduleDependencies, isFireAndForget);

        // view resolution
        PluggableObjectCollection plugInViews = new PluggableObjectCollection();
        plugInViews.addViews(configuration.getCompiler().getPlugInViews(), configuration.getCompiler().getPlugInVirtualDataWindows(), classpathImportServiceCompileTime);
        PluggableObjectRegistryImpl viewRegistry = new PluggableObjectRegistryImpl(new PluggableObjectCollection[]{ViewEnumHelper.getBuiltinViews(), plugInViews});
        ViewResolutionService viewResolutionService = new ViewResolutionServiceImpl(viewRegistry);

        PluggableObjectCollection plugInPatternObj = new PluggableObjectCollection();
        plugInPatternObj.addPatternObjects(configuration.getCompiler().getPlugInPatternObjects(), classpathImportServiceCompileTime);
        plugInPatternObj.addObjects(PatternObjectHelper.getBuiltinPatternObjects());
        PatternObjectResolutionService patternResolutionService = new PatternObjectResolutionServiceImpl(plugInPatternObj);

        IndexCompileTimeRegistry indexCompileTimeRegistry = new IndexCompileTimeRegistry(new HashMap<>());

        ModuleAccessModifierService moduleVisibilityRules = new ModuleAccessModifierServiceImpl(options, configuration.getCompiler().getByteCode());

        DatabaseConfigServiceCompileTime databaseConfigServiceCompileTime = new DatabaseConfigServiceImpl(configuration.getCommon().getDatabaseReferences(), classpathImportServiceCompileTime);

        CompilerServices compilerServices = new CompilerServicesImpl();

        boolean targetHA = configuration.getClass().getName().endsWith("ConfigurationHA");
        SerdeEventTypeCompileTimeRegistry serdeEventTypeRegistry = new SerdeEventTypeCompileTimeRegistryImpl(targetHA);
        SerdeCompileTimeResolver serdeResolver = targetHA ? makeSerdeResolver(configuration.getCompiler().getSerde(), configuration.getCommon().getTransientConfiguration()) : SerdeCompileTimeResolverNonHA.INSTANCE;

        return new ModuleCompileTimeServices(compilerServices, configuration, contextCompileTimeRegistry, contextCompileTimeResolver, beanEventTypeStemService, beanEventTypeFactoryPrivate, databaseConfigServiceCompileTime, classpathImportServiceCompileTime, exprDeclaredCompileTimeRegistry, exprDeclaredCompileTimeResolver, eventTypeAvroHandler, eventTypeCompileRegistry, eventTypeCompileTimeResolver, eventTypeRepositoryPreconfigured, isFireAndForget, indexCompileTimeRegistry, moduleDependencies, moduleVisibilityRules, namedWindowCompileTimeResolver, namedWindowCompileTimeRegistry, classLoaderParent,
            patternResolutionService, scriptCompileTimeRegistry, scriptCompileTimeResolver, serdeEventTypeRegistry, serdeResolver,
            tableCompileTimeRegistry, tableCompileTimeResolver, variableCompileTimeRegistry, variableCompileTimeResolver, viewResolutionService, xmlFragmentEventTypeFactory);
    }

    protected static ClasspathImportServiceCompileTime makeClasspathImportService(Configuration configuration) {
        TimeAbacus timeAbacus = TimeAbacusFactory.make(configuration.getCommon().getTimeSource().getTimeUnit());
        ConfigurationCompilerExpression expression = configuration.getCompiler().getExpression();
        ClasspathImportServiceCompileTime classpathImportService = new ClasspathImportServiceCompileTime(configuration.getCommon().getTransientConfiguration(), timeAbacus, configuration.getCommon().getEventTypeAutoNamePackages(), expression.getMathContext(), expression.isExtendedAggregation(), configuration.getCompiler().getLanguage().isSortUsingCollator()
        );

        // Add auto-imports
        try {
            for (String importName : configuration.getCommon().getImports()) {
                classpathImportService.addImport(importName);
            }

            for (String importName : configuration.getCommon().getAnnotationImports()) {
                classpathImportService.addAnnotationImport(importName);
            }

            for (ConfigurationCompilerPlugInAggregationFunction config : configuration.getCompiler().getPlugInAggregationFunctions()) {
                classpathImportService.addAggregation(config.getName(), config);
            }

            for (ConfigurationCompilerPlugInAggregationMultiFunction config : configuration.getCompiler().getPlugInAggregationMultiFunctions()) {
                classpathImportService.addAggregationMultiFunction(config);
            }

            for (ConfigurationCompilerPlugInSingleRowFunction config : configuration.getCompiler().getPlugInSingleRowFunctions()) {
                classpathImportService.addSingleRow(config.getName(), config.getFunctionClassName(), config.getFunctionMethodName(), config.getValueCache(), config.getFilterOptimizable(), config.isRethrowExceptions(), config.getEventTypeName());
            }

            for (ConfigurationCompilerPlugInDateTimeMethod config : configuration.getCompiler().getPlugInDateTimeMethods()) {
                classpathImportService.addPlugInDateTimeMethod(config.getName(), config);
            }

            for (ConfigurationCompilerPlugInEnumMethod config : configuration.getCompiler().getPlugInEnumMethods()) {
                classpathImportService.addPlugInEnumMethod(config.getName(), config);
            }
        } catch (ClasspathImportException ex) {
            throw new ConfigurationException("Error configuring compiler: " + ex.getMessage(), ex);
        }

        return classpathImportService;
    }

    private static SerdeCompileTimeResolver makeSerdeResolver(ConfigurationCompilerSerde config, Map<String, Object> transientConfiguration) {
        SerdeProviderFactoryContext context = new SerdeProviderFactoryContext();

        List<SerdeProvider> providers = null;
        if (config.getSerdeProviderFactories() != null) {
            for (String factory : config.getSerdeProviderFactories()) {
                try {
                    SerdeProviderFactory instance = (SerdeProviderFactory) (JavaClassHelper.instantiate(SerdeProviderFactory.class, factory, TransientConfigurationResolver.resolveClassForNameProvider(transientConfiguration)));
                    SerdeProvider provider = instance.getProvider(context);
                    if (provider == null) {
                        throw new ConfigurationException("Binding provider factory '" + factory + "' returned a null value");
                    }
                    if (providers == null) {
                        providers = new ArrayList<>();
                    }
                    providers.add(provider);
                } catch (RuntimeException ex) {
                    throw new ConfigurationException("Binding provider factory '" + factory + "' failed to initialize: " + ex.getMessage(), ex);
                }
            }
        }
        if (providers == null) {
            providers = Collections.emptyList();
        }

        return new SerdeCompileTimeResolverImpl(providers, config.isEnableExtendedBuiltin(), config.isEnableSerializable(), config.isEnableExternalizable(), config.isEnableSerializationFallback());
    }
}
