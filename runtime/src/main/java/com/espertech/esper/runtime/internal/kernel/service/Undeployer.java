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

import com.espertech.esper.common.client.EventType;
import com.espertech.esper.common.client.util.EventTypeBusModifier;
import com.espertech.esper.common.client.util.StatementProperty;
import com.espertech.esper.common.internal.collection.PathRegistry;
import com.espertech.esper.common.internal.collection.PathRegistryObjectType;
import com.espertech.esper.common.internal.context.module.ModuleDependenciesRuntime;
import com.espertech.esper.common.internal.context.module.ModuleIndexMeta;
import com.espertech.esper.common.internal.context.module.ModuleProvider;
import com.espertech.esper.common.internal.context.util.StatementContext;
import com.espertech.esper.common.internal.context.util.StatementDestroyServices;
import com.espertech.esper.common.internal.context.util.StatementFinalizeCallback;
import com.espertech.esper.common.internal.epl.join.lookup.IndexMultiKey;
import com.espertech.esper.common.internal.epl.lookupplansubord.EventTableIndexMetadata;
import com.espertech.esper.common.internal.epl.lookupplansubord.EventTableIndexMetadataEntry;
import com.espertech.esper.common.internal.epl.namedwindow.path.NamedWindowMetaData;
import com.espertech.esper.common.internal.epl.script.core.NameAndParamNum;
import com.espertech.esper.common.internal.epl.script.core.NameParamNumAndModule;
import com.espertech.esper.common.internal.epl.table.compiletime.TableMetaData;
import com.espertech.esper.common.internal.type.NameAndModule;
import com.espertech.esper.common.internal.util.DeploymentIdNamePair;
import com.espertech.esper.runtime.client.EPStatement;
import com.espertech.esper.runtime.client.EPUndeployPreconditionException;
import com.espertech.esper.runtime.internal.kernel.statement.EPStatementSPI;
import com.espertech.esper.runtime.internal.metrics.instrumentation.Instrumentation;
import com.espertech.esper.runtime.internal.metrics.instrumentation.InstrumentationHelper;

import java.util.Iterator;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

public class Undeployer {

    public static void checkModulePreconditions(String deploymentId, String moduleName, DeploymentInternal deployment, EPServicesContext services) throws EPUndeployPreconditionException {
        for (String namedWindow : deployment.getPathNamedWindows()) {
            checkDependency(services.getNamedWindowPathRegistry(), namedWindow, moduleName);
        }

        for (String table : deployment.getPathTables()) {
            checkDependency(services.getTablePathRegistry(), table, moduleName);
        }

        for (String variable : deployment.getPathVariables()) {
            checkDependency(services.getVariablePathRegistry(), variable, moduleName);
        }

        for (String context : deployment.getPathContexts()) {
            checkDependency(services.getContextPathRegistry(), context, moduleName);
        }

        for (String eventType : deployment.getPathEventTypes()) {
            checkDependency(services.getEventTypePathRegistry(), eventType, moduleName);
        }

        for (String exprDecl : deployment.getPathExprDecls()) {
            checkDependency(services.getExprDeclaredPathRegistry(), exprDecl, moduleName);
        }

        for (NameAndParamNum script : deployment.getPathScripts()) {
            checkDependency(services.getScriptPathRegistry(), script, moduleName);
        }

        for (ModuleIndexMeta index : deployment.getPathIndexes()) {
            if (index.isNamedWindow()) {
                NamedWindowMetaData namedWindow = services.getNamedWindowPathRegistry().getWithModule(index.getInfraName(), index.getInfraModuleName());
                validateIndexPrecondition(namedWindow.getIndexMetadata(), index, deploymentId);
            } else {
                TableMetaData table = services.getTablePathRegistry().getWithModule(index.getInfraName(), index.getInfraModuleName());
                validateIndexPrecondition(table.getIndexMetadata(), index, deploymentId);
            }
        }
    }

    public static void disassociate(EPStatement[] statements) {
        for (EPStatement stmt : statements) {
            EPStatementSPI statement = (EPStatementSPI) stmt;
            if (statement != null) {
                statement.setParentView(null);
                statement.setDestroyed();
            }
        }
    }

    public static void undeploy(String deploymentId, Map<Long, EventType> deploymentTypes, StatementContext[] reverted, ModuleProvider moduleProvider, EPServicesContext services) {
        for (StatementContext statement : reverted) {
            // remove any match-recognize counts
            if (services.getRowRecogStatePoolEngineSvc() != null) {
                services.getRowRecogStatePoolEngineSvc().removeStatement(new DeploymentIdNamePair(statement.getDeploymentId(), statement.getStatementName()));
            }

            Iterator<StatementFinalizeCallback> it = statement.getFinalizeCallbacks();
            while (it.hasNext()) {
                it.next().statementDestroyed(statement);
            }

            if (statement.getDestroyCallback() != null) {
                statement.getDestroyCallback().destroy(new StatementDestroyServices(services.getFilterService()), statement);
            } else {
                statement.getStatementAIFactoryProvider().getFactory().statementDestroy(statement);
            }

            services.getEpServicesHA().getListenerRecoveryService().remove(statement.getStatementId());
            services.getStatementLifecycleService().removeStatement(statement.getStatementId());
            services.getPatternSubexpressionPoolRuntimeSvc().removeStatement(statement.getStatementId());
            services.getFilterSharedBoolExprRepository().removeStatement(statement.getStatementId());
            services.getFilterSharedLookupableRepository().removeReferencesStatement(statement.getStatementId());
        }

        ModuleDependenciesRuntime moduleDependencies = moduleProvider.getModuleDependencies();
        for (NameAndModule namedWindow : moduleDependencies.getPathNamedWindows()) {
            services.getNamedWindowPathRegistry().removeDependency(namedWindow.getName(), namedWindow.getModuleName(), deploymentId);
        }
        for (NameAndModule table : moduleDependencies.getPathTables()) {
            services.getTablePathRegistry().removeDependency(table.getName(), table.getModuleName(), deploymentId);
        }
        for (NameAndModule variable : moduleDependencies.getPathVariables()) {
            services.getVariablePathRegistry().removeDependency(variable.getName(), variable.getModuleName(), deploymentId);
        }
        for (NameAndModule context : moduleDependencies.getPathContexts()) {
            services.getContextPathRegistry().removeDependency(context.getName(), context.getModuleName(), deploymentId);
        }
        for (NameAndModule eventType : moduleDependencies.getPathEventTypes()) {
            services.getEventTypePathRegistry().removeDependency(eventType.getName(), eventType.getModuleName(), deploymentId);
        }
        for (NameAndModule exprDecl : moduleDependencies.getPathExpressions()) {
            services.getExprDeclaredPathRegistry().removeDependency(exprDecl.getName(), exprDecl.getModuleName(), deploymentId);
        }
        for (NameParamNumAndModule script : moduleDependencies.getPathScripts()) {
            services.getScriptPathRegistry().removeDependency(new NameAndParamNum(script.getName(), script.getParamNum()), script.getModuleName(), deploymentId);
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
            indexMetadata.removeIndexReference(index.getIndexName(), deploymentId);
        }

        deleteFromEventTypeBus(services, deploymentTypes);
        deleteFromPathRegistries(services, deploymentId);

        if (InstrumentationHelper.ENABLED) {
            Instrumentation instrumentation = InstrumentationHelper.get();
            for (StatementContext ctx : reverted) {
                instrumentation.qaRuntimeManagementStmtStop(services.getRuntimeURI(), deploymentId, ctx.getStatementId(), ctx.getStatementName(),
                        (String) ctx.getStatementInformationals().getProperties().get(StatementProperty.EPL), services.getSchedulingService().getTime());
            }
        }
    }

    static void deleteFromEventTypeBus(EPServicesContext services, Map<Long, EventType> eventTypes) {
        for (Map.Entry<Long, EventType> entry : eventTypes.entrySet()) {
            if (entry.getValue().getMetadata().getBusModifier() == EventTypeBusModifier.BUS) {
                services.getEventTypeRepositoryBus().removeType(entry.getValue());
            }
        }
    }

    static void deleteFromPathRegistries(EPServicesContext services, String deploymentId) {
        services.getEventTypePathRegistry().deleteDeployment(deploymentId);
        services.getNamedWindowPathRegistry().deleteDeployment(deploymentId);
        services.getTablePathRegistry().deleteDeployment(deploymentId);
        services.getContextPathRegistry().deleteDeployment(deploymentId);
        services.getVariablePathRegistry().deleteDeployment(deploymentId);
        services.getExprDeclaredPathRegistry().deleteDeployment(deploymentId);
        services.getScriptPathRegistry().deleteDeployment(deploymentId);
    }

    private static <K, E> void checkDependency(PathRegistry<K, E> registry, K entityKey, String moduleName) throws EPUndeployPreconditionException {
        Set<String> dependencies = registry.getDependencies(entityKey, moduleName);
        if (dependencies != null && !dependencies.isEmpty()) {
            throw makeException(registry.getObjectType(), entityKey.toString(), dependencies.iterator().next());
        }
    }

    private static EPUndeployPreconditionException makeException(PathRegistryObjectType objectType, String name, String otherDeploymentId) {
        String objectName = objectType.getName();
        String firstUppercase = objectName.substring(0, 1).toUpperCase(Locale.ENGLISH) + objectName.substring(1);
        return new EPUndeployPreconditionException(firstUppercase + " '" + name + "' cannot be un-deployed as it is referenced by deployment '" + otherDeploymentId + "'");
    }

    private static void validateIndexPrecondition(EventTableIndexMetadata indexMetadata, ModuleIndexMeta index, String deploymentId) throws EPUndeployPreconditionException {
        IndexMultiKey imk = indexMetadata.getIndexByName(index.getIndexName());
        EventTableIndexMetadataEntry entry = indexMetadata.getIndexes().get(imk);
        if (entry == null) {
            return;
        }
        String[] referring = indexMetadata.getIndexes().get(imk).getReferringDeployments();
        if (referring != null && referring.length > 0) {
            String first = null;
            for (String referringeploymentId : referring) {
                if (!referringeploymentId.equals(deploymentId)) {
                    first = referringeploymentId;
                }
            }
            if (first != null) {
                throw makeException(PathRegistryObjectType.INDEX, index.getIndexName(), first);
            }
        }
    }
}
