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

import com.espertech.esper.common.client.EPCompiled;
import com.espertech.esper.common.client.EPException;
import com.espertech.esper.common.client.EventType;
import com.espertech.esper.common.client.configuration.runtime.ConfigurationRuntimeThreading;
import com.espertech.esper.common.client.meta.EventTypeApplicationType;
import com.espertech.esper.common.client.meta.EventTypeMetadata;
import com.espertech.esper.common.client.util.EventTypeBusModifier;
import com.espertech.esper.common.client.util.Locking;
import com.espertech.esper.common.client.util.NameAccessModifier;
import com.espertech.esper.common.client.util.StatementProperty;
import com.espertech.esper.common.internal.collection.PathException;
import com.espertech.esper.common.internal.collection.PathExceptionAlreadyRegistered;
import com.espertech.esper.common.internal.collection.PathRegistryObjectType;
import com.espertech.esper.common.internal.compile.stage1.spec.ExpressionDeclItem;
import com.espertech.esper.common.internal.compile.stage1.spec.ExpressionScriptProvided;
import com.espertech.esper.common.internal.context.aifactory.core.ModuleIncidentals;
import com.espertech.esper.common.internal.context.airegistry.AIRegistryRequirements;
import com.espertech.esper.common.internal.context.airegistry.StatementAIResourceRegistry;
import com.espertech.esper.common.internal.context.compile.ContextCollector;
import com.espertech.esper.common.internal.context.compile.ContextCollectorImpl;
import com.espertech.esper.common.internal.context.compile.ContextMetaData;
import com.espertech.esper.common.internal.context.mgr.ContextDeployTimeResolver;
import com.espertech.esper.common.internal.context.mgr.ContextManager;
import com.espertech.esper.common.internal.context.module.*;
import com.espertech.esper.common.internal.context.util.ContextRuntimeDescriptor;
import com.espertech.esper.common.internal.context.util.EPStatementHandle;
import com.espertech.esper.common.internal.context.util.StatementCPCacheService;
import com.espertech.esper.common.internal.context.util.StatementContext;
import com.espertech.esper.common.internal.epl.expression.core.ExprFilterSpecLookupable;
import com.espertech.esper.common.internal.epl.expression.declared.runtime.ExprDeclaredCollectorRuntime;
import com.espertech.esper.common.internal.epl.index.base.IndexCollectorRuntime;
import com.espertech.esper.common.internal.epl.lookupplansubord.EventTableIndexMetadata;
import com.espertech.esper.common.internal.epl.namedwindow.path.NamedWindowCollector;
import com.espertech.esper.common.internal.epl.namedwindow.path.NamedWindowCollectorImpl;
import com.espertech.esper.common.internal.epl.namedwindow.path.NamedWindowMetaData;
import com.espertech.esper.common.internal.epl.pattern.pool.PatternSubexpressionPoolStmtHandler;
import com.espertech.esper.common.internal.epl.pattern.pool.PatternSubexpressionPoolStmtSvc;
import com.espertech.esper.common.internal.epl.rowrecog.state.RowRecogStatePoolStmtHandler;
import com.espertech.esper.common.internal.epl.rowrecog.state.RowRecogStatePoolStmtSvc;
import com.espertech.esper.common.internal.epl.script.core.NameAndParamNum;
import com.espertech.esper.common.internal.epl.script.core.NameParamNumAndModule;
import com.espertech.esper.common.internal.epl.script.core.ScriptCollectorRuntime;
import com.espertech.esper.common.internal.epl.table.compiletime.TableMetaData;
import com.espertech.esper.common.internal.epl.table.core.TableCollectorImpl;
import com.espertech.esper.common.internal.epl.variable.compiletime.VariableMetaData;
import com.espertech.esper.common.internal.epl.variable.core.VariableCollector;
import com.espertech.esper.common.internal.epl.variable.core.VariableCollectorImpl;
import com.espertech.esper.common.internal.event.bean.service.BeanEventTypeFactoryPrivate;
import com.espertech.esper.common.internal.event.core.EventBeanTypedEventFactoryRuntime;
import com.espertech.esper.common.internal.event.core.EventTypeSPI;
import com.espertech.esper.common.internal.event.eventtypefactory.EventTypeFactoryImpl;
import com.espertech.esper.common.internal.event.path.EventTypeCollectorImpl;
import com.espertech.esper.common.internal.event.path.EventTypeResolver;
import com.espertech.esper.common.internal.event.path.EventTypeResolverImpl;
import com.espertech.esper.common.internal.filterspec.*;
import com.espertech.esper.common.internal.metrics.stmtmetrics.StatementMetricHandle;
import com.espertech.esper.common.internal.schedule.ScheduleBucket;
import com.espertech.esper.common.internal.statement.insertintolatch.InsertIntoLatchFactory;
import com.espertech.esper.common.internal.statement.multimatch.MultiMatchHandler;
import com.espertech.esper.common.internal.statement.resource.StatementResourceService;
import com.espertech.esper.common.internal.type.NameAndModule;
import com.espertech.esper.common.internal.util.CRC32Util;
import com.espertech.esper.common.internal.util.CollectionUtil;
import com.espertech.esper.common.internal.util.DeploymentIdNamePair;
import com.espertech.esper.runtime.client.EPDeployException;
import com.espertech.esper.runtime.client.EPDeployPreconditionException;
import com.espertech.esper.runtime.client.EPDeploySubstitutionParameterException;
import com.espertech.esper.runtime.client.EPStatement;
import com.espertech.esper.runtime.client.option.*;
import com.espertech.esper.runtime.internal.kernel.statement.EPStatementInitServicesImpl;
import com.espertech.esper.runtime.internal.kernel.statement.EPStatementSPI;
import com.espertech.esper.runtime.internal.kernel.updatedispatch.UpdateDispatchViewBase;
import com.espertech.esper.runtime.internal.kernel.updatedispatch.UpdateDispatchViewBlockingSpin;
import com.espertech.esper.runtime.internal.kernel.updatedispatch.UpdateDispatchViewBlockingWait;
import com.espertech.esper.runtime.internal.kernel.updatedispatch.UpdateDispatchViewNonBlocking;
import com.espertech.esper.runtime.internal.metrics.instrumentation.InstrumentationDefault;
import com.espertech.esper.runtime.internal.metrics.instrumentation.InstrumentationHelper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;

public class Deployer {

    private final static Logger log = LoggerFactory.getLogger(Deployer.class);

    public static DeploymentInternal deployFresh(String deploymentId, int statementIdFirstStatement, EPCompiled compiled, StatementNameRuntimeOption statementNameResolverRuntime, StatementUserObjectRuntimeOption userObjectResolverRuntime, StatementSubstitutionParameterOption substitutionParameterResolver, EPRuntimeSPI epRuntime) throws EPDeployException {
        return deploy(false, deploymentId, statementIdFirstStatement, compiled, statementNameResolverRuntime, userObjectResolverRuntime, substitutionParameterResolver, epRuntime);
    }

    public static DeploymentInternal deployRecover(String deploymentId, int statementIdFirstStatement, EPCompiled compiled, StatementNameRuntimeOption statementNameResolverRuntime, StatementUserObjectRuntimeOption userObjectResolverRuntime, StatementSubstitutionParameterOption substitutionParameterResolver, EPRuntimeSPI epRuntime) throws EPDeployException {
        return deploy(true, deploymentId, statementIdFirstStatement, compiled, statementNameResolverRuntime, userObjectResolverRuntime, substitutionParameterResolver, epRuntime);
    }

    private static DeploymentInternal deploy(boolean recovery, String deploymentId, int statementIdFirstStatement, EPCompiled compiled, StatementNameRuntimeOption statementNameResolverRuntime, StatementUserObjectRuntimeOption userObjectResolverRuntime, StatementSubstitutionParameterOption substitutionParameterResolver, EPRuntimeSPI epRuntime) throws EPDeployException {
        // set variable local version
        epRuntime.getServicesContext().getVariableManagementService().setLocalVersion();

        try {
            return deploySafe(recovery, deploymentId, statementIdFirstStatement, compiled, statementNameResolverRuntime, userObjectResolverRuntime, substitutionParameterResolver, epRuntime);
        } catch (EPDeployException ex) {
            throw ex;
        } catch (Throwable t) {
            throw new EPDeployException(t.getMessage(), t);
        }
    }

    private static DeploymentInternal deploySafe(boolean recovery,
                                                 String deploymentId,
                                                 int statementIdFirstStatement,
                                                 EPCompiled compiled,
                                                 StatementNameRuntimeOption statementNameResolverRuntime,
                                                 StatementUserObjectRuntimeOption userObjectResolverRuntime,
                                                 StatementSubstitutionParameterOption substitutionParameterResolver,
                                                 EPRuntimeSPI epRuntime) throws PathException, EPDeployException {
        ModuleProviderResult provider = ModuleProviderUtil.analyze(compiled, epRuntime.getServicesContext().getClasspathImportServiceRuntime());
        String moduleName = provider.getModuleProvider().getModuleName();
        EPServicesContext services = epRuntime.getServicesContext();

        // resolve external dependencies
        ModuleDependenciesRuntime moduleDependencies = provider.getModuleProvider().getModuleDependencies();
        Set<String> deploymentIdDependencies = resolveDependencies(moduleDependencies, services);

        // keep protected types
        BeanEventTypeFactoryPrivate beanEventTypeFactory = new BeanEventTypeFactoryPrivate(new EventBeanTypedEventFactoryRuntime(services.getEventTypeAvroHandler()), EventTypeFactoryImpl.INSTANCE, services.getBeanEventTypeStemService());

        // initialize module event types
        Map<String, EventType> moduleEventTypes = new HashMap<>();
        EventTypeResolverImpl eventTypeResolver = new EventTypeResolverImpl(moduleEventTypes, services.getEventTypePathRegistry(), services.getEventTypeRepositoryBus(), services.getBeanEventTypeFactoryPrivate());
        EventTypeCollectorImpl eventTypeCollector = new EventTypeCollectorImpl(moduleEventTypes, beanEventTypeFactory, services.getEventTypeFactory(), services.getBeanEventTypeStemService(), eventTypeResolver, services.getXmlFragmentEventTypeFactory(), services.getEventTypeAvroHandler(), services.getEventBeanTypedEventFactory());
        try {
            provider.getModuleProvider().initializeEventTypes(new EPModuleEventTypeInitServicesImpl(eventTypeCollector, eventTypeResolver));
        } catch (Throwable e) {
            throw new EPException(e);
        }

        // initialize module named windows
        Map<String, NamedWindowMetaData> moduleNamedWindows = new HashMap<>();
        NamedWindowCollector namedWindowCollector = new NamedWindowCollectorImpl(moduleNamedWindows);
        try {
            provider.getModuleProvider().initializeNamedWindows(new EPModuleNamedWindowInitServicesImpl(namedWindowCollector, eventTypeResolver));
        } catch (Throwable e) {
            throw new EPException(e);
        }

        // initialize module tables
        Map<String, TableMetaData> moduleTables = new HashMap<>();
        TableCollectorImpl tableCollector = new TableCollectorImpl(moduleTables);
        try {
            provider.getModuleProvider().initializeTables(new EPModuleTableInitServicesImpl(tableCollector, eventTypeResolver));
        } catch (Throwable e) {
            throw new EPException(e);
        }

        // initialize create-index indexes
        Set<ModuleIndexMeta> moduleIndexes = new HashSet<>();
        IndexCollectorRuntime indexCollector = new IndexCollectorRuntime(moduleIndexes);
        try {
            provider.getModuleProvider().initializeIndexes(new EPModuleIndexInitServicesImpl(indexCollector));
        } catch (Throwable e) {
            throw new EPException(e);
        }

        // initialize module contexts
        Map<String, ContextMetaData> moduleContexts = new HashMap<>();
        ContextCollector contextCollector = new ContextCollectorImpl(moduleContexts);
        try {
            provider.getModuleProvider().initializeContexts(new EPModuleContextInitServicesImpl(contextCollector, eventTypeResolver));
        } catch (Throwable e) {
            throw new EPException(e);
        }

        // initialize module variables
        Map<String, VariableMetaData> moduleVariables = new HashMap<>();
        VariableCollector variableCollector = new VariableCollectorImpl(moduleVariables);
        try {
            provider.getModuleProvider().initializeVariables(new EPModuleVariableInitServicesImpl(variableCollector, eventTypeResolver));
        } catch (Throwable e) {
            throw new EPException(e);
        }

        // initialize module expressions
        Map<String, ExpressionDeclItem> moduleExpressions = new HashMap<>();
        ExprDeclaredCollectorRuntime exprDeclaredCollector = new ExprDeclaredCollectorRuntime(moduleExpressions);
        try {
            provider.getModuleProvider().initializeExprDeclareds(new EPModuleExprDeclaredInitServicesImpl(exprDeclaredCollector));
        } catch (Throwable e) {
            throw new EPException(e);
        }

        // initialize module scripts
        Map<NameAndParamNum, ExpressionScriptProvided> moduleScripts = new HashMap<>();
        ScriptCollectorRuntime scriptCollectorRuntime = new ScriptCollectorRuntime(moduleScripts);
        try {
            provider.getModuleProvider().initializeScripts(new EPModuleScriptInitServicesImpl(scriptCollectorRuntime));
        } catch (Throwable e) {
            throw new EPException(e);
        }

        // save path-visibility event types and named windows to the path
        long deploymentIdCrc32 = CRC32Util.computeCRC32(deploymentId);
        Map<Long, EventType> deploymentTypes = Collections.emptyMap();
        List<String> pathEventTypes = new ArrayList<>(2);
        List<String> pathNamedWindows = new ArrayList<>(2);
        List<String> pathTables = new ArrayList<>(2);
        List<String> pathContexts = new ArrayList<>(2);
        List<String> pathVariables = new ArrayList<>(2);
        List<String> pathExprDecl = new ArrayList<>(2);
        List<NameAndParamNum> pathScripts = new ArrayList<>(2);

        try {
            for (Map.Entry<String, NamedWindowMetaData> entry : moduleNamedWindows.entrySet()) {
                if (entry.getValue().getEventType().getMetadata().getAccessModifier().isNonPrivateNonTransient()) {
                    try {
                        services.getNamedWindowPathRegistry().add(entry.getKey(), moduleName, entry.getValue(), deploymentId);
                    } catch (PathExceptionAlreadyRegistered ex) {
                        throw new EPDeployPreconditionException(ex.getMessage(), ex);
                    }
                    pathNamedWindows.add(entry.getKey());
                }
            }
            for (Map.Entry<String, TableMetaData> entry : moduleTables.entrySet()) {
                if (entry.getValue().getTableVisibility().isNonPrivateNonTransient()) {
                    try {
                        services.getTablePathRegistry().add(entry.getKey(), moduleName, entry.getValue(), deploymentId);
                    } catch (PathExceptionAlreadyRegistered ex) {
                        throw new EPDeployPreconditionException(ex.getMessage(), ex);
                    }
                    pathTables.add(entry.getKey());
                }
            }
            for (Map.Entry<String, EventType> entry : moduleEventTypes.entrySet()) {
                EventTypeSPI eventTypeSPI = (EventTypeSPI) entry.getValue();
                long nameTypeId = CRC32Util.computeCRC32(eventTypeSPI.getName());
                EventTypeMetadata eventTypeMetadata = entry.getValue().getMetadata();
                if (eventTypeMetadata.getAccessModifier() == NameAccessModifier.PRECONFIGURED) {
                    // For XML all fragment event types are public
                    if (eventTypeMetadata.getApplicationType() != EventTypeApplicationType.XML) {
                        throw new IllegalStateException("Unrecognized public visibility type in deployment");
                    }
                } else if (eventTypeMetadata.getAccessModifier().isNonPrivateNonTransient()) {
                    if (eventTypeMetadata.getBusModifier() == EventTypeBusModifier.BUS) {
                        eventTypeSPI.setMetadataId(nameTypeId, -1);
                        services.getEventTypeRepositoryBus().addType(eventTypeSPI);
                    } else {
                        eventTypeSPI.setMetadataId(deploymentIdCrc32, nameTypeId);
                    }
                    try {
                        services.getEventTypePathRegistry().add(entry.getKey(), moduleName, entry.getValue(), deploymentId);
                    } catch (PathExceptionAlreadyRegistered ex) {
                        throw new EPDeployPreconditionException(ex.getMessage(), ex);
                    }
                } else {
                    eventTypeSPI.setMetadataId(deploymentIdCrc32, nameTypeId);
                }
                if (eventTypeMetadata.getAccessModifier().isNonPrivateNonTransient()) {
                    pathEventTypes.add(entry.getKey());
                }

                // we retain all types to enable variant-streams
                if (deploymentTypes.isEmpty()) {
                    deploymentTypes = new HashMap<>(4);
                }
                deploymentTypes.put(nameTypeId, eventTypeSPI);
            }
            for (Map.Entry<String, ContextMetaData> entry : moduleContexts.entrySet()) {
                if (entry.getValue().getContextVisibility().isNonPrivateNonTransient()) {
                    try {
                        services.getContextPathRegistry().add(entry.getKey(), moduleName, entry.getValue(), deploymentId);
                    } catch (PathExceptionAlreadyRegistered ex) {
                        throw new EPDeployPreconditionException(ex.getMessage(), ex);
                    }
                    pathContexts.add(entry.getKey());
                }
            }
            for (Map.Entry<String, VariableMetaData> entry : moduleVariables.entrySet()) {
                if (entry.getValue().getVariableVisibility().isNonPrivateNonTransient()) {
                    try {
                        services.getVariablePathRegistry().add(entry.getKey(), moduleName, entry.getValue(), deploymentId);
                    } catch (PathExceptionAlreadyRegistered ex) {
                        throw new EPDeployPreconditionException(ex.getMessage(), ex);
                    }
                    pathVariables.add(entry.getKey());
                }
            }
            for (Map.Entry<String, ExpressionDeclItem> entry : moduleExpressions.entrySet()) {
                if (entry.getValue().getVisibility().isNonPrivateNonTransient()) {
                    try {
                        services.getExprDeclaredPathRegistry().add(entry.getKey(), moduleName, entry.getValue(), deploymentId);
                    } catch (PathExceptionAlreadyRegistered ex) {
                        throw new EPDeployPreconditionException(ex.getMessage(), ex);
                    }
                    pathExprDecl.add(entry.getKey());
                }
            }
            for (Map.Entry<NameAndParamNum, ExpressionScriptProvided> entry : moduleScripts.entrySet()) {
                if (entry.getValue().getVisibility().isNonPrivateNonTransient()) {
                    try {
                        services.getScriptPathRegistry().add(entry.getKey(), moduleName, entry.getValue(), deploymentId);
                    } catch (PathExceptionAlreadyRegistered ex) {
                        throw new EPDeployPreconditionException(ex.getMessage(), ex);
                    }
                    pathScripts.add(entry.getKey());
                }
            }
            for (ModuleIndexMeta index : moduleIndexes) {
                if (index.isNamedWindow()) {
                    NamedWindowMetaData namedWindow = services.getNamedWindowPathRegistry().getWithModule(index.getInfraName(), index.getInfraModuleName());
                    if (namedWindow == null) {
                        throw new IllegalStateException("Failed to find named window '" + index.getInfraName() + "'");
                    }
                    validateIndexPrecondition(namedWindow.getIndexMetadata(), index);
                } else {
                    TableMetaData table = services.getTablePathRegistry().getWithModule(index.getInfraName(), index.getInfraModuleName());
                    if (table == null) {
                        throw new IllegalStateException("Failed to find table '" + index.getInfraName() + "'");
                    }
                    validateIndexPrecondition(table.getIndexMetadata(), index);
                }
            }
        } catch (Throwable t) {
            Undeployer.deleteFromEventTypeBus(services, deploymentTypes);
            Undeployer.deleteFromPathRegistries(services, deploymentId);
            throw t;
        }

        // done validated block
        ModuleIncidentals moduleIncidentals = new ModuleIncidentals(moduleNamedWindows, moduleContexts, moduleVariables, moduleExpressions, moduleTables);

        // get module statements
        List<StatementProvider> statementResources;
        try {
            statementResources = provider.getModuleProvider().statements();
        } catch (Throwable e) {
            throw new EPException(e);
        }

        // initialize all statements
        List<StatementLightweight> lightweights = new ArrayList<>();
        Map<Integer, Map<Integer, Object>> substitutionParameters;
        Set<String> statementNames = new HashSet<>();
        try {
            int statementId = statementIdFirstStatement;
            for (StatementProvider statement : statementResources) {
                StatementLightweight lightweight = initStatement(recovery, moduleName, statement, deploymentId, statementId, eventTypeResolver, moduleIncidentals, statementNameResolverRuntime, userObjectResolverRuntime, services);
                lightweights.add(lightweight);
                statementId++;

                String statementName = lightweight.getStatementContext().getStatementName();
                if (statementNames.contains(statementName)) {
                    throw new EPDeployException("Duplicate statement name provide by statement name resolver for statement name '" + statementName + "'");
                }
                statementNames.add(statementName);
            }

            // set parameters
            substitutionParameters = setSubstitutionParameterValues(deploymentId, lightweights, substitutionParameterResolver);
        } catch (Throwable t) {
            reverseDeployment(deploymentId, deploymentTypes, lightweights, new EPStatement[0], provider, services);
            throw t;
        }

        // start statements depending on context association
        EPStatement[] statements = new EPStatement[lightweights.size()];
        int count = 0;
        for (StatementLightweight lightweight : lightweights) {

            EPStatementSPI stmt;
            try {
                stmt = DeployerStatement.deployStatement(recovery, lightweight, services, epRuntime);
            } catch (Throwable t) {
                try {
                    reverseDeployment(deploymentId, deploymentTypes, lightweights, statements, provider, services);
                } catch (Throwable udex) {
                    log.warn(udex.getMessage(), udex);
                }
                throw new EPDeployException("Failed to deploy: " + t.getMessage(), t);
            }

            statements[count++] = stmt;

            if (InstrumentationHelper.ENABLED) {
                InstrumentationHelper.get().qaRuntimeManagementStmtStarted(epRuntime.getURI(), deploymentId, lightweight.getStatementContext().getStatementId(), stmt.getName(),
                    (String) stmt.getProperty(StatementProperty.EPL), epRuntime.getEventService().getCurrentTime());
            }
        }

        // add dependencies
        addDependencies(deploymentId, moduleDependencies, services);

        // keep statement and deployment
        String[] deploymentIdDependenciesArray = deploymentIdDependencies.toArray(new String[deploymentIdDependencies.size()]);
        DeploymentInternal deployed = new DeploymentInternal(deploymentId, statements, deploymentIdDependenciesArray,
            CollectionUtil.toArray(pathNamedWindows), CollectionUtil.toArray(pathTables), CollectionUtil.toArray(pathVariables),
            CollectionUtil.toArray(pathContexts), CollectionUtil.toArray(pathEventTypes), CollectionUtil.toArray(pathExprDecl),
            NameAndParamNum.toArray(pathScripts), ModuleIndexMeta.toArray(moduleIndexes), provider.getModuleProvider(),
            provider.getModuleProvider().getModuleProperties(), deploymentTypes, System.currentTimeMillis());
        services.getDeploymentLifecycleService().addDeployment(deploymentId, deployed);

        // register for recovery
        if (!recovery) {
            RecoveryInformation recoveryInformation = getRecoveryInformation(deployed);
            services.getDeploymentRecoveryService().add(deploymentId, statementIdFirstStatement, compiled, recoveryInformation.statementUserObjectsRuntime, recoveryInformation.statementNamesWhenProvidedByAPI, substitutionParameters);
        }

        return deployed;
    }

    private static void reverseDeployment(String deploymentId, Map<Long, EventType> deploymentTypes, List<StatementLightweight> lightweights, EPStatement[] statements, ModuleProviderResult provider, EPServicesContext services) {
        List<StatementContext> revert = new ArrayList<>();
        for (StatementLightweight stmtToRemove : lightweights) {
            revert.add(stmtToRemove.getStatementContext());
        }
        Collections.reverse(revert);
        StatementContext[] reverted = revert.toArray(new StatementContext[revert.size()]);
        Undeployer.disassociate(statements);
        Undeployer.undeploy(deploymentId, deploymentTypes, reverted, provider.getModuleProvider(), services);
    }

    private static Map<Integer, Map<Integer, Object>> setSubstitutionParameterValues(String deploymentId, List<StatementLightweight> lightweights, StatementSubstitutionParameterOption substitutionParameterResolver) throws EPDeploySubstitutionParameterException {
        if (substitutionParameterResolver == null) {
            for (StatementLightweight lightweight : lightweights) {
                Class[] required = lightweight.getStatementInformationals().getSubstitutionParamTypes();
                if (required != null && required.length > 0) {
                    throw new EPDeploySubstitutionParameterException("Statement '" + lightweight.getStatementContext().getStatementName() + "' has " + required.length + " substitution parameters");
                }
            }
            return Collections.emptyMap();
        }

        Map<Integer, Map<Integer, Object>> providedAllStmt = new HashMap<>();
        for (StatementLightweight lightweight : lightweights) {
            Class[] substitutionTypes = lightweight.getStatementInformationals().getSubstitutionParamTypes();
            Map<String, Integer> paramNames = lightweight.getStatementInformationals().getSubstitutionParamNames();
            DeployerSubstitutionParameterHandler handler = new DeployerSubstitutionParameterHandler(deploymentId, lightweight, providedAllStmt, substitutionTypes, paramNames);

            try {
                substitutionParameterResolver.setStatementParameters(handler);
            } catch (Throwable t) {
                throw new EPDeploySubstitutionParameterException("Failed to set substitution parameter value for statement '" + lightweight.getStatementContext().getStatementName() + "': " + t.getMessage(), t);
            }

            if (substitutionTypes == null || substitutionTypes.length == 0) {
                continue;
            }

            // check that all values are provided
            Map<Integer, Object> provided = providedAllStmt.get(lightweight.getStatementContext().getStatementId());
            int providedSize = provided == null ? 0 : provided.size();
            if (providedSize != substitutionTypes.length) {
                for (int i = 0; i < substitutionTypes.length; i++) {
                    if (provided == null || !provided.containsKey(i + 1)) {
                        String name = Integer.toString(i + 1);
                        if (paramNames != null && !paramNames.isEmpty()) {
                            for (Map.Entry<String, Integer> entry : paramNames.entrySet()) {
                                if (entry.getValue() == i + 1) {
                                    name = "'" + entry.getKey() + "'";
                                }
                            }
                        }
                        throw new EPDeploySubstitutionParameterException("Missing value for substitution parameter " + name + " for statement '" + lightweight.getStatementContext().getStatementName() + "'");
                    }
                }
            }
        }

        return providedAllStmt;
    }

    private static RecoveryInformation getRecoveryInformation(DeploymentInternal deployerResult) {
        Map<Integer, Object> userObjects = Collections.emptyMap();
        Map<Integer, String> statementNamesWhenOverridden = Collections.emptyMap();
        for (EPStatement stmt : deployerResult.getStatements()) {
            EPStatementSPI spi = (EPStatementSPI) stmt;
            if (stmt.getUserObjectRuntime() != null) {
                if (userObjects.isEmpty()) {
                    userObjects = new HashMap<>();
                }
                userObjects.put(spi.getStatementId(), spi.getStatementContext().getUserObjectRuntime());
            }
            if (!spi.getStatementContext().getStatementInformationals().getStatementNameCompileTime().equals(spi.getName())) {
                if (statementNamesWhenOverridden.isEmpty()) {
                    statementNamesWhenOverridden = new HashMap<>();
                }
                statementNamesWhenOverridden.put(spi.getStatementId(), spi.getName());
            }
        }
        return new RecoveryInformation(userObjects, statementNamesWhenOverridden);
    }

    private static void addDependencies(String deploymentId, ModuleDependenciesRuntime moduleDependencies, EPServicesContext services) {
        for (NameAndModule eventType : moduleDependencies.getPathEventTypes()) {
            services.getEventTypePathRegistry().addDependency(eventType.getName(), eventType.getModuleName(), deploymentId);
        }

        for (NameAndModule namedWindow : moduleDependencies.getPathNamedWindows()) {
            services.getNamedWindowPathRegistry().addDependency(namedWindow.getName(), namedWindow.getModuleName(), deploymentId);
        }

        for (NameAndModule table : moduleDependencies.getPathTables()) {
            services.getTablePathRegistry().addDependency(table.getName(), table.getModuleName(), deploymentId);
        }

        for (NameAndModule variable : moduleDependencies.getPathVariables()) {
            services.getVariablePathRegistry().addDependency(variable.getName(), variable.getModuleName(), deploymentId);
        }

        for (NameAndModule context : moduleDependencies.getPathContexts()) {
            services.getContextPathRegistry().addDependency(context.getName(), context.getModuleName(), deploymentId);
        }

        for (NameAndModule exprDecl : moduleDependencies.getPathExpressions()) {
            services.getExprDeclaredPathRegistry().addDependency(exprDecl.getName(), exprDecl.getModuleName(), deploymentId);
        }

        for (NameParamNumAndModule script : moduleDependencies.getPathScripts()) {
            services.getScriptPathRegistry().addDependency(new NameAndParamNum(script.getName(), script.getParamNum()), script.getModuleName(), deploymentId);
        }

        for (ModuleIndexMeta index : moduleDependencies.getPathIndexes()) {
            EventTableIndexMetadata indexMetadata;
            if (index.isNamedWindow()) {
                NameAndModule namedWindowName = NameAndModule.findName(index.getInfraName(), moduleDependencies.getPathNamedWindows());
                NamedWindowMetaData namedWindow = services.getNamedWindowPathRegistry().getWithModule(namedWindowName.getName(), namedWindowName.getModuleName());
                indexMetadata = namedWindow.getIndexMetadata();
            } else {
                NameAndModule tableName = NameAndModule.findName(index.getInfraName(), moduleDependencies.getPathTables());
                TableMetaData table = services.getTablePathRegistry().getWithModule(tableName.getName(), tableName.getModuleName());
                indexMetadata = table.getIndexMetadata();
            }
            indexMetadata.addIndexReference(index.getIndexName(), deploymentId);
        }
    }

    private static Set<String> resolveDependencies(ModuleDependenciesRuntime moduleDependencies, EPServicesContext services) throws EPDeployPreconditionException {
        Set<String> dependencies = new HashSet<>();

        for (String publicEventType : moduleDependencies.getPublicEventTypes()) {
            if (services.getEventTypeRepositoryBus().getTypeByName(publicEventType) == null) {
                throw makePreconditionExceptionPreconfigured(PathRegistryObjectType.EVENTTYPE, publicEventType);
            }
        }

        for (String publicVariable : moduleDependencies.getPublicVariables()) {
            if (services.getConfigSnapshot().getCommon().getVariables().get(publicVariable) == null) {
                throw makePreconditionExceptionPreconfigured(PathRegistryObjectType.VARIABLE, publicVariable);
            }
        }

        for (NameAndModule pathNamedWindow : moduleDependencies.getPathNamedWindows()) {
            String depIdNamedWindow = services.getNamedWindowPathRegistry().getDeploymentId(pathNamedWindow.getName(), pathNamedWindow.getModuleName());
            if (depIdNamedWindow == null) {
                throw makePreconditionExceptionPath(PathRegistryObjectType.NAMEDWINDOW, pathNamedWindow);
            }
            dependencies.add(depIdNamedWindow);
        }

        for (NameAndModule pathTable : moduleDependencies.getPathTables()) {
            String depIdTable = services.getTablePathRegistry().getDeploymentId(pathTable.getName(), pathTable.getModuleName());
            if (depIdTable == null) {
                throw makePreconditionExceptionPath(PathRegistryObjectType.TABLE, pathTable);
            }
            dependencies.add(depIdTable);
        }

        for (NameAndModule pathEventType : moduleDependencies.getPathEventTypes()) {
            String depIdEventType = services.getEventTypePathRegistry().getDeploymentId(pathEventType.getName(), pathEventType.getModuleName());
            if (depIdEventType == null) {
                throw makePreconditionExceptionPath(PathRegistryObjectType.EVENTTYPE, pathEventType);
            }
            dependencies.add(depIdEventType);
        }

        for (NameAndModule pathVariable : moduleDependencies.getPathVariables()) {
            String depIdVariable = services.getVariablePathRegistry().getDeploymentId(pathVariable.getName(), pathVariable.getModuleName());
            if (depIdVariable == null) {
                throw makePreconditionExceptionPath(PathRegistryObjectType.VARIABLE, pathVariable);
            }
            dependencies.add(depIdVariable);
        }

        for (NameAndModule pathContext : moduleDependencies.getPathContexts()) {
            String depIdContext = services.getContextPathRegistry().getDeploymentId(pathContext.getName(), pathContext.getModuleName());
            if (depIdContext == null) {
                throw makePreconditionExceptionPath(PathRegistryObjectType.CONTEXT, pathContext);
            }
            dependencies.add(depIdContext);
        }

        for (NameAndModule pathExpression : moduleDependencies.getPathExpressions()) {
            String depIdExpression = services.getExprDeclaredPathRegistry().getDeploymentId(pathExpression.getName(), pathExpression.getModuleName());
            if (depIdExpression == null) {
                throw makePreconditionExceptionPath(PathRegistryObjectType.EXPRDECL, pathExpression);
            }
            dependencies.add(depIdExpression);
        }

        for (NameParamNumAndModule pathScript : moduleDependencies.getPathScripts()) {
            String depIdExpression = services.getScriptPathRegistry().getDeploymentId(new NameAndParamNum(pathScript.getName(), pathScript.getParamNum()), pathScript.getModuleName());
            if (depIdExpression == null) {
                throw makePreconditionExceptionPath(PathRegistryObjectType.SCRIPT, new NameAndModule(pathScript.getName(), pathScript.getModuleName()));
            }
            dependencies.add(depIdExpression);
        }

        for (ModuleIndexMeta index : moduleDependencies.getPathIndexes()) {
            String depIdIndex;
            if (index.isNamedWindow()) {
                NameAndModule namedWindowName = NameAndModule.findName(index.getInfraName(), moduleDependencies.getPathNamedWindows());
                NamedWindowMetaData namedWindow = services.getNamedWindowPathRegistry().getWithModule(namedWindowName.getName(), namedWindowName.getModuleName());
                depIdIndex = namedWindow.getIndexMetadata().getIndexDeploymentId(index.getIndexName());
            } else {
                NameAndModule tableName = NameAndModule.findName(index.getInfraName(), moduleDependencies.getPathTables());
                TableMetaData table = services.getTablePathRegistry().getWithModule(tableName.getName(), tableName.getModuleName());
                depIdIndex = table.getIndexMetadata().getIndexDeploymentId(index.getIndexName());
            }
            if (depIdIndex == null) {
                throw makePreconditionExceptionPath(PathRegistryObjectType.INDEX, new NameAndModule(index.getIndexName(), index.getIndexModuleName()));
            }
            dependencies.add(depIdIndex);
        }

        return dependencies;
    }

    private static StatementLightweight initStatement(boolean recovery, String moduleName, StatementProvider statementProvider, String deploymentId, int statementId, EventTypeResolver eventTypeResolver, ModuleIncidentals moduleIncidentals, StatementNameRuntimeOption statementNameResolverRuntime, StatementUserObjectRuntimeOption userObjectResolverRuntime, EPServicesContext services) {
        StatementInformationalsRuntime informationals = statementProvider.getInformationals();

        // set instrumentation unless already provided
        if (informationals.getInstrumentationProvider() == null) {
            informationals.setInstrumentationProvider(InstrumentationDefault.INSTANCE);
        }

        StatementResultServiceImpl statementResultService = new StatementResultServiceImpl(informationals, services);
        FilterSharedLookupableRegistery filterSharedLookupableRegistery = new FilterSharedLookupableRegistery() {
            public void registerLookupable(EventType eventType, ExprFilterSpecLookupable lookupable) {
                services.getFilterSharedLookupableRepository().registerLookupable(statementId, eventType, lookupable);
            }
        };

        FilterSharedBoolExprRegistery filterSharedBoolExprRegistery = new FilterSharedBoolExprRegistery() {
            public void registerBoolExpr(FilterSpecParamExprNode node) {
                services.getFilterSharedBoolExprRepository().registerBoolExpr(statementId, node);
            }
        };

        Map<Integer, FilterSpecActivatable> filterSpecActivatables = new HashMap<>();
        FilterSpecActivatableRegistry filterSpecActivatableRegistry = new FilterSpecActivatableRegistry() {
            public void register(FilterSpecActivatable filterSpecActivatable) {
                filterSpecActivatables.put(filterSpecActivatable.getFilterCallbackId(), filterSpecActivatable);
            }
        };

        boolean contextPartitioned = informationals.getOptionalContextName() != null;
        StatementResourceService statementResourceService = new StatementResourceService(contextPartitioned);

        // determine statement name
        String statementName = informationals.getStatementNameCompileTime();
        if (statementNameResolverRuntime != null) {
            String statementNameAssigned = statementNameResolverRuntime.getStatementName(new StatementNameRuntimeContext(deploymentId, statementName, statementId, (String) informationals.getProperties().get(StatementProperty.EPL), informationals.getAnnotations()));
            if (statementNameAssigned != null) {
                statementName = statementNameAssigned;
            }
        }
        statementName = statementName.trim();

        EPStatementInitServicesImpl epInitServices = new EPStatementInitServicesImpl(statementName, informationals.getProperties(), informationals.getAnnotations(), deploymentId,
            eventTypeResolver, filterSpecActivatableRegistry, filterSharedBoolExprRegistery, filterSharedLookupableRegistery, moduleIncidentals,
            recovery, statementResourceService, statementResultService, services);

        statementProvider.initialize(epInitServices);

        MultiMatchHandler multiMatchHandler = services.getMultiMatchHandlerFactory().make(informationals.isHasSubquery(), informationals.isNeedDedup());

        StatementMetricHandle stmtMetric = services.getMetricReportingService().getStatementHandle(statementId, deploymentId, statementName);

        String optionalEPL = (String) informationals.getProperties().get(StatementProperty.EPL);
        InsertIntoLatchFactory insertIntoFrontLatchFactory = null;
        InsertIntoLatchFactory insertIntoBackLatchFactory = null;
        if (informationals.getInsertIntoLatchName() != null) {
            String latchFactoryNameBack = "insert_stream_B_" + informationals.getInsertIntoLatchName() + "_" + statementName;
            String latchFactoryNameFront = "insert_stream_F_" + informationals.getInsertIntoLatchName() + "_" + statementName;
            long msecTimeout = services.getRuntimeSettingsService().getConfigurationRuntime().getThreading().getInsertIntoDispatchTimeout();
            Locking locking = services.getRuntimeSettingsService().getConfigurationRuntime().getThreading().getInsertIntoDispatchLocking();
            InsertIntoLatchFactory latchFactoryFront = new InsertIntoLatchFactory(latchFactoryNameFront, informationals.isStateless(), msecTimeout, locking, services.getTimeSourceService());
            InsertIntoLatchFactory latchFactoryBack = new InsertIntoLatchFactory(latchFactoryNameBack, informationals.isStateless(), msecTimeout, locking, services.getTimeSourceService());
            insertIntoFrontLatchFactory = latchFactoryFront;
            insertIntoBackLatchFactory = latchFactoryBack;
        }

        EPStatementHandle statementHandle = new EPStatementHandle(statementName, deploymentId, statementId, optionalEPL, informationals.getPriority(), informationals.isPreemptive(), informationals.isCanSelfJoin(), multiMatchHandler, informationals.isHasVariables(), informationals.isHasTableAccess(), stmtMetric, insertIntoFrontLatchFactory, insertIntoBackLatchFactory);

        // determine context
        StatementAIResourceRegistry statementAgentInstanceRegistry = null;
        ContextRuntimeDescriptor contextRuntimeDescriptor = null;
        String optionalContextName = informationals.getOptionalContextName();
        if (optionalContextName != null) {
            String contextDeploymentId = ContextDeployTimeResolver.resolveContextDeploymentId(informationals.getOptionalContextModuleName(), informationals.getOptionalContextVisibility(), optionalContextName, deploymentId, services.getContextPathRegistry());
            ContextManager contextManager = services.getContextManagementService().getContextManager(contextDeploymentId, optionalContextName);
            contextRuntimeDescriptor = contextManager.getContextRuntimeDescriptor();
            AIRegistryRequirements registryRequirements = statementProvider.getStatementAIFactoryProvider().getFactory().getRegistryRequirements();
            statementAgentInstanceRegistry = contextManager.allocateAgentInstanceResourceRegistry(registryRequirements);
        }

        StatementCPCacheService statementCPCacheService = new StatementCPCacheService(contextPartitioned, statementResourceService, statementAgentInstanceRegistry);

        EventType eventType = statementProvider.getStatementAIFactoryProvider().getFactory().getStatementEventType();

        ConfigurationRuntimeThreading configurationThreading = services.getRuntimeSettingsService().getConfigurationRuntime().getThreading();
        boolean preserveDispatchOrder = configurationThreading.isListenerDispatchPreserveOrder() && !informationals.isStateless();
        boolean isSpinLocks = configurationThreading.getListenerDispatchLocking() == Locking.SPIN;
        long msecBlockingTimeout = configurationThreading.getListenerDispatchTimeout();
        UpdateDispatchViewBase dispatchChildView;
        if (preserveDispatchOrder) {
            if (isSpinLocks) {
                dispatchChildView = new UpdateDispatchViewBlockingSpin(eventType, statementResultService, services.getDispatchService(), msecBlockingTimeout, services.getTimeSourceService());
            } else {
                dispatchChildView = new UpdateDispatchViewBlockingWait(eventType, statementResultService, services.getDispatchService(), msecBlockingTimeout);
            }
        } else {
            dispatchChildView = new UpdateDispatchViewNonBlocking(eventType, statementResultService, services.getDispatchService());
        }

        boolean countSubexpressions = services.getConfigSnapshot().getRuntime().getPatterns().getMaxSubexpressions() != null;
        PatternSubexpressionPoolStmtSvc patternSubexpressionPoolStmtSvc = null;
        if (countSubexpressions) {
            PatternSubexpressionPoolStmtHandler stmtCounter = new PatternSubexpressionPoolStmtHandler();
            patternSubexpressionPoolStmtSvc = new PatternSubexpressionPoolStmtSvc(services.getPatternSubexpressionPoolRuntimeSvc(), stmtCounter);
            services.getPatternSubexpressionPoolRuntimeSvc().addPatternContext(statementId, statementName, stmtCounter);
        }

        boolean countMatchRecogStates = services.getConfigSnapshot().getRuntime().getMatchRecognize().getMaxStates() != null;
        RowRecogStatePoolStmtSvc rowRecogStatePoolStmtSvc = null;
        if (countMatchRecogStates && informationals.isHasMatchRecognize()) {
            RowRecogStatePoolStmtHandler stmtCounter = new RowRecogStatePoolStmtHandler();
            rowRecogStatePoolStmtSvc = new RowRecogStatePoolStmtSvc(services.getRowRecogStatePoolEngineSvc(), stmtCounter);
            services.getRowRecogStatePoolEngineSvc().addPatternContext(new DeploymentIdNamePair(deploymentId, statementName), stmtCounter);
        }

        // get user object for runtime
        Object userObjectRuntime = null;
        if (userObjectResolverRuntime != null) {
            userObjectRuntime = userObjectResolverRuntime.getUserObject(new StatementUserObjectRuntimeContext(deploymentId, statementName, statementId, (String) informationals.getProperties().get(StatementProperty.EPL), informationals.getAnnotations()));
        }

        StatementContext statementContext = new StatementContext(contextRuntimeDescriptor, deploymentId,
            statementId,
            statementName,
            moduleName,
            informationals,
            userObjectRuntime,
            services.getStatementContextRuntimeServices(),
            statementHandle,
            filterSpecActivatables,
            patternSubexpressionPoolStmtSvc,
            rowRecogStatePoolStmtSvc,
            new ScheduleBucket(statementId),
            statementAgentInstanceRegistry,
            statementCPCacheService,
            statementProvider.getStatementAIFactoryProvider(),
            statementResultService,
            dispatchChildView
        );

        for (StatementReadyCallback readyCallback : epInitServices.getReadyCallbacks()) {
            readyCallback.ready(statementContext, moduleIncidentals, recovery);
        }

        return new StatementLightweight(statementProvider, informationals, statementResultService, statementContext);
    }

    private static EPDeployPreconditionException makePreconditionExceptionPath(PathRegistryObjectType objectType, NameAndModule nameAndModule) {
        String message = "Required dependency ";
        message += objectType.getName() + " '" + nameAndModule.getName() + "'";
        if (nameAndModule.getModuleName() != null && nameAndModule.getModuleName().length() != 0) {
            message += " module '" + nameAndModule.getModuleName() + "'";
        }
        message += " cannot be found";
        return new EPDeployPreconditionException(message);
    }

    private static EPDeployPreconditionException makePreconditionExceptionPreconfigured(PathRegistryObjectType objectType, String name) {
        String message = "Required pre-configured ";
        message += objectType.getName() + " '" + name + "'";
        message += " cannot be found";
        return new EPDeployPreconditionException(message);
    }

    private static void validateIndexPrecondition(EventTableIndexMetadata indexMetadata, ModuleIndexMeta index) throws EPDeployPreconditionException {
        if (indexMetadata.getIndexByName(index.getIndexName()) != null) {
            PathExceptionAlreadyRegistered ex = new PathExceptionAlreadyRegistered(index.getIndexName(), PathRegistryObjectType.INDEX, index.getIndexModuleName());
            throw new EPDeployPreconditionException(ex.getMessage(), ex);
        }
    }

    private static class RecoveryInformation {
        private final Map<Integer, Object> statementUserObjectsRuntime;
        private final Map<Integer, String> statementNamesWhenProvidedByAPI;

        public RecoveryInformation(Map<Integer, Object> statementUserObjectsRuntime, Map<Integer, String> statementNamesWhenProvidedByAPI) {
            this.statementUserObjectsRuntime = statementUserObjectsRuntime;
            this.statementNamesWhenProvidedByAPI = statementNamesWhenProvidedByAPI;
        }

        public Map<Integer, Object> getStatementUserObjectsRuntime() {
            return statementUserObjectsRuntime;
        }

        public Map<Integer, String> getStatementNamesWhenProvidedByAPI() {
            return statementNamesWhenProvidedByAPI;
        }
    }
}
