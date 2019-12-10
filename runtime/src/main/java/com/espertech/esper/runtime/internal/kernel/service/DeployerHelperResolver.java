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
import com.espertech.esper.common.internal.collection.PathRegistryObjectType;
import com.espertech.esper.common.internal.context.module.ModuleDependenciesRuntime;
import com.espertech.esper.common.internal.context.module.ModuleIndexMeta;
import com.espertech.esper.common.internal.context.module.ModuleProviderCLPair;
import com.espertech.esper.common.internal.context.module.StatementLightweight;
import com.espertech.esper.common.internal.context.util.StatementContext;
import com.espertech.esper.common.internal.epl.namedwindow.path.NamedWindowMetaData;
import com.espertech.esper.common.internal.epl.script.core.NameAndParamNum;
import com.espertech.esper.common.internal.epl.script.core.NameParamNumAndModule;
import com.espertech.esper.common.internal.epl.table.compiletime.TableMetaData;
import com.espertech.esper.common.internal.type.NameAndModule;
import com.espertech.esper.runtime.client.*;
import com.espertech.esper.runtime.client.option.DeploymentClassLoaderContext;
import com.espertech.esper.runtime.client.option.DeploymentClassLoaderOption;
import com.espertech.esper.runtime.internal.deploymentlifesvc.DeploymentLifecycleService;

import java.util.*;

public class DeployerHelperResolver {
    static ClassLoader getClassLoader(int rolloutItemNumber, DeploymentClassLoaderOption deploymentClassLoaderOption, EPServicesContext servicesContext) throws EPDeployException {
        ClassLoader deploymentClassLoader = servicesContext.getClassLoaderParent();
        if (deploymentClassLoaderOption != null) {
            deploymentClassLoader = deploymentClassLoaderOption.getClassLoader(new DeploymentClassLoaderContext(servicesContext.getClassLoaderParent(), servicesContext.getConfigSnapshot()));
            if (deploymentClassLoader == null) {
                throw new EPDeployException("Deployment classloader option returned a null value for the classloader", rolloutItemNumber);
            }
        }
        return deploymentClassLoader;
    }

    static String determineDeploymentIdCheckExists(int rolloutItemNumber, DeploymentOptions optionsMayNull, DeploymentLifecycleService deploymentLifecycleService) throws EPDeployDeploymentExistsException {
        String deploymentId;
        if (optionsMayNull == null || optionsMayNull.getDeploymentId() == null) {
            deploymentId = UUID.randomUUID().toString();
        } else {
            deploymentId = optionsMayNull.getDeploymentId();
        }

        if (deploymentLifecycleService.getDeploymentById(deploymentId) != null) {
            throw new EPDeployDeploymentExistsException("Deployment by id '" + deploymentId + "' already exists", rolloutItemNumber);
        }
        return deploymentId;
    }

    static Set<String> resolveDependencies(int rolloutItemNumber, ModuleDependenciesRuntime moduleDependencies, EPServicesContext services) throws EPDeployPreconditionException {
        Set<String> dependencies = new HashSet<>();

        for (String publicEventType : moduleDependencies.getPublicEventTypes()) {
            if (services.getEventTypeRepositoryBus().getTypeByName(publicEventType) == null) {
                throw makePreconditionExceptionPreconfigured(rolloutItemNumber, PathRegistryObjectType.EVENTTYPE, publicEventType);
            }
        }

        for (String publicVariable : moduleDependencies.getPublicVariables()) {
            if (services.getConfigSnapshot().getCommon().getVariables().get(publicVariable) == null) {
                throw makePreconditionExceptionPreconfigured(rolloutItemNumber, PathRegistryObjectType.VARIABLE, publicVariable);
            }
        }

        for (NameAndModule pathNamedWindow : moduleDependencies.getPathNamedWindows()) {
            String depIdNamedWindow = services.getNamedWindowPathRegistry().getDeploymentId(pathNamedWindow.getName(), pathNamedWindow.getModuleName());
            if (depIdNamedWindow == null) {
                throw makePreconditionExceptionPath(rolloutItemNumber, PathRegistryObjectType.NAMEDWINDOW, pathNamedWindow);
            }
            dependencies.add(depIdNamedWindow);
        }

        for (NameAndModule pathTable : moduleDependencies.getPathTables()) {
            String depIdTable = services.getTablePathRegistry().getDeploymentId(pathTable.getName(), pathTable.getModuleName());
            if (depIdTable == null) {
                throw makePreconditionExceptionPath(rolloutItemNumber, PathRegistryObjectType.TABLE, pathTable);
            }
            dependencies.add(depIdTable);
        }

        for (NameAndModule pathEventType : moduleDependencies.getPathEventTypes()) {
            String depIdEventType = services.getEventTypePathRegistry().getDeploymentId(pathEventType.getName(), pathEventType.getModuleName());
            if (depIdEventType == null) {
                throw makePreconditionExceptionPath(rolloutItemNumber, PathRegistryObjectType.EVENTTYPE, pathEventType);
            }
            dependencies.add(depIdEventType);
        }

        for (NameAndModule pathVariable : moduleDependencies.getPathVariables()) {
            String depIdVariable = services.getVariablePathRegistry().getDeploymentId(pathVariable.getName(), pathVariable.getModuleName());
            if (depIdVariable == null) {
                throw makePreconditionExceptionPath(rolloutItemNumber, PathRegistryObjectType.VARIABLE, pathVariable);
            }
            dependencies.add(depIdVariable);
        }

        for (NameAndModule pathContext : moduleDependencies.getPathContexts()) {
            String depIdContext = services.getContextPathRegistry().getDeploymentId(pathContext.getName(), pathContext.getModuleName());
            if (depIdContext == null) {
                throw makePreconditionExceptionPath(rolloutItemNumber, PathRegistryObjectType.CONTEXT, pathContext);
            }
            dependencies.add(depIdContext);
        }

        for (NameAndModule pathExpression : moduleDependencies.getPathExpressions()) {
            String depIdExpression = services.getExprDeclaredPathRegistry().getDeploymentId(pathExpression.getName(), pathExpression.getModuleName());
            if (depIdExpression == null) {
                throw makePreconditionExceptionPath(rolloutItemNumber, PathRegistryObjectType.EXPRDECL, pathExpression);
            }
            dependencies.add(depIdExpression);
        }

        for (NameParamNumAndModule pathScript : moduleDependencies.getPathScripts()) {
            String depIdExpression = services.getScriptPathRegistry().getDeploymentId(new NameAndParamNum(pathScript.getName(), pathScript.getParamNum()), pathScript.getModuleName());
            if (depIdExpression == null) {
                throw makePreconditionExceptionPath(rolloutItemNumber, PathRegistryObjectType.SCRIPT, new NameAndModule(pathScript.getName(), pathScript.getModuleName()));
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
                throw makePreconditionExceptionPath(rolloutItemNumber, PathRegistryObjectType.INDEX, new NameAndModule(index.getIndexName(), index.getIndexModuleName()));
            }
            dependencies.add(depIdIndex);
        }

        return dependencies;
    }

    private static EPDeployPreconditionException makePreconditionExceptionPath(int rolloutItemNumber, PathRegistryObjectType objectType, NameAndModule nameAndModule) {
        String message = "Required dependency ";
        message += objectType.getName() + " '" + nameAndModule.getName() + "'";
        if (nameAndModule.getModuleName() != null && nameAndModule.getModuleName().length() != 0) {
            message += " module '" + nameAndModule.getModuleName() + "'";
        }
        message += " cannot be found";
        return new EPDeployPreconditionException(message, rolloutItemNumber);
    }

    private static EPDeployPreconditionException makePreconditionExceptionPreconfigured(int rolloutItemNumber, PathRegistryObjectType objectType, String name) {
        String message = "Required pre-configured ";
        message += objectType.getName() + " '" + name + "'";
        message += " cannot be found";
        return new EPDeployPreconditionException(message, rolloutItemNumber);
    }

    static void reverseDeployment(String deploymentId, Map<Long, EventType> deploymentTypes, List<StatementLightweight> lightweights, EPStatement[] statements, ModuleProviderCLPair provider, EPServicesContext services) {
        List<StatementContext> revert = new ArrayList<>();
        for (StatementLightweight stmtToRemove : lightweights) {
            revert.add(stmtToRemove.getStatementContext());
        }
        Collections.reverse(revert);
        StatementContext[] reverted = revert.toArray(new StatementContext[revert.size()]);
        Undeployer.disassociate(statements);
        Undeployer.undeploy(deploymentId, deploymentTypes, reverted, provider.getModuleProvider(), services);
    }
}
