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

import com.espertech.esper.common.client.EPException;
import com.espertech.esper.common.client.EventType;
import com.espertech.esper.common.client.configuration.runtime.ConfigurationRuntimeThreading;
import com.espertech.esper.common.client.util.Locking;
import com.espertech.esper.common.client.util.StatementProperty;
import com.espertech.esper.common.internal.context.aifactory.core.ModuleIncidentals;
import com.espertech.esper.common.internal.context.airegistry.AIRegistryRequirements;
import com.espertech.esper.common.internal.context.airegistry.StatementAIResourceRegistry;
import com.espertech.esper.common.internal.context.mgr.ContextDeployTimeResolver;
import com.espertech.esper.common.internal.context.mgr.ContextManager;
import com.espertech.esper.common.internal.context.module.*;
import com.espertech.esper.common.internal.context.util.ContextRuntimeDescriptor;
import com.espertech.esper.common.internal.context.util.EPStatementHandle;
import com.espertech.esper.common.internal.context.util.StatementCPCacheService;
import com.espertech.esper.common.internal.context.util.StatementContext;
import com.espertech.esper.common.internal.epl.expression.core.ExprFilterSpecLookupable;
import com.espertech.esper.common.internal.epl.pattern.pool.PatternSubexpressionPoolStmtHandler;
import com.espertech.esper.common.internal.epl.pattern.pool.PatternSubexpressionPoolStmtSvc;
import com.espertech.esper.common.internal.epl.rowrecog.state.RowRecogStatePoolStmtHandler;
import com.espertech.esper.common.internal.epl.rowrecog.state.RowRecogStatePoolStmtSvc;
import com.espertech.esper.common.internal.event.path.EventTypeResolver;
import com.espertech.esper.common.internal.filterspec.*;
import com.espertech.esper.common.internal.metrics.stmtmetrics.StatementMetricHandle;
import com.espertech.esper.common.internal.schedule.ScheduleBucket;
import com.espertech.esper.common.internal.statement.insertintolatch.InsertIntoLatchFactory;
import com.espertech.esper.common.internal.statement.multimatch.MultiMatchHandler;
import com.espertech.esper.common.internal.statement.resource.StatementResourceService;
import com.espertech.esper.common.internal.util.DeploymentIdNamePair;
import com.espertech.esper.runtime.client.EPDeployException;
import com.espertech.esper.runtime.client.EPDeploySubstitutionParameterException;
import com.espertech.esper.runtime.client.EPStatement;
import com.espertech.esper.runtime.client.option.*;
import com.espertech.esper.runtime.internal.kernel.statement.EPStatementInitServicesImpl;
import com.espertech.esper.runtime.internal.kernel.updatedispatch.UpdateDispatchViewBase;
import com.espertech.esper.runtime.internal.kernel.updatedispatch.UpdateDispatchViewBlockingSpin;
import com.espertech.esper.runtime.internal.kernel.updatedispatch.UpdateDispatchViewBlockingWait;
import com.espertech.esper.runtime.internal.kernel.updatedispatch.UpdateDispatchViewNonBlocking;
import com.espertech.esper.runtime.internal.metrics.instrumentation.InstrumentationDefault;

import java.util.*;

public class DeployerHelperInitStatement {
    public static DeployerModuleStatementLightweights initializeStatements(int rolloutItemNumber, boolean recovery, DeployerModuleEPLObjects moduleEPLObjects, DeployerModulePaths modulePaths, String moduleName, ModuleProviderCLPair moduleProvider, String deploymentId, int statementIdFirstStatement, StatementUserObjectRuntimeOption userObjectResolverRuntime, StatementNameRuntimeOption statementNameResolverRuntime, StatementSubstitutionParameterOption substitutionParameterResolver, EPServicesContext services) throws Throwable {
        // get module statements
        List<StatementProvider> statementResources;
        try {
            statementResources = moduleProvider.getModuleProvider().statements();
        } catch (Throwable e) {
            throw new EPException(e);
        }

        // initialize all statements
        List<StatementLightweight> lightweights = new ArrayList<>();
        Map<Integer, Map<Integer, Object>> substitutionParameters;
        Set<String> statementNames = new HashSet<>();
        ModuleIncidentals moduleIncidentals = moduleEPLObjects.getIncidentals();
        try {
            int statementId = statementIdFirstStatement;
            for (StatementProvider statement : statementResources) {
                StatementLightweight lightweight = initStatement(recovery, moduleName, statement, deploymentId, statementId, moduleEPLObjects.getEventTypeResolver(), moduleIncidentals, statementNameResolverRuntime, userObjectResolverRuntime, services);
                lightweights.add(lightweight);
                statementId++;

                String statementName = lightweight.getStatementContext().getStatementName();
                if (statementNames.contains(statementName)) {
                    throw new EPDeployException("Duplicate statement name provide by statement name resolver for statement name '" + statementName + "'", rolloutItemNumber);
                }
                statementNames.add(statementName);
            }

            // set parameters
            substitutionParameters = setSubstitutionParameterValues(rolloutItemNumber, deploymentId, lightweights, substitutionParameterResolver);
        } catch (Throwable t) {
            DeployerHelperResolver.reverseDeployment(deploymentId, modulePaths.getDeploymentTypes(), lightweights, new EPStatement[0], moduleProvider, services);
            throw t;
        }

        return new DeployerModuleStatementLightweights(statementIdFirstStatement, lightweights, substitutionParameters);
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
            dispatchChildView,
            services.getFilterService(),
            services.getSchedulingService(),
            services.getInternalEventRouteDest()
        );

        for (StatementReadyCallback readyCallback : epInitServices.getReadyCallbacks()) {
            readyCallback.ready(statementContext, moduleIncidentals, recovery);
        }

        return new StatementLightweight(statementProvider, informationals, statementResultService, statementContext);
    }

    private static Map<Integer, Map<Integer, Object>> setSubstitutionParameterValues(int rolloutItemNumber, String deploymentId, List<StatementLightweight> lightweights, StatementSubstitutionParameterOption substitutionParameterResolver) throws EPDeploySubstitutionParameterException {
        if (substitutionParameterResolver == null) {
            for (StatementLightweight lightweight : lightweights) {
                Class[] required = lightweight.getStatementInformationals().getSubstitutionParamTypes();
                if (required != null && required.length > 0) {
                    throw new EPDeploySubstitutionParameterException("Statement '" + lightweight.getStatementContext().getStatementName() + "' has " + required.length + " substitution parameters", rolloutItemNumber);
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
                throw new EPDeploySubstitutionParameterException("Failed to set substitution parameter value for statement '" + lightweight.getStatementContext().getStatementName() + "': " + t.getMessage(), t, rolloutItemNumber);
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
                        throw new EPDeploySubstitutionParameterException("Missing value for substitution parameter " + name + " for statement '" + lightweight.getStatementContext().getStatementName() + "'", rolloutItemNumber);
                    }
                }
            }
        }

        return providedAllStmt;
    }
}
