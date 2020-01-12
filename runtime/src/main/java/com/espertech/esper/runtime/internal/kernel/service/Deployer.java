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
import com.espertech.esper.common.internal.context.module.ModuleDependenciesRuntime;
import com.espertech.esper.common.internal.context.module.ModuleIndexMeta;
import com.espertech.esper.common.internal.context.module.ModuleProviderCLPair;
import com.espertech.esper.common.internal.context.module.ModuleProviderUtil;
import com.espertech.esper.common.internal.epl.lookupplansubord.EventTableIndexMetadata;
import com.espertech.esper.common.internal.epl.namedwindow.path.NamedWindowMetaData;
import com.espertech.esper.common.internal.epl.script.core.NameAndParamNum;
import com.espertech.esper.common.internal.epl.script.core.NameParamNumAndModule;
import com.espertech.esper.common.internal.epl.table.compiletime.TableMetaData;
import com.espertech.esper.common.internal.type.NameAndModule;
import com.espertech.esper.runtime.client.EPDeployException;
import com.espertech.esper.runtime.client.EPStatement;
import com.espertech.esper.runtime.client.option.DeploymentClassLoaderOption;
import com.espertech.esper.runtime.client.option.StatementNameRuntimeOption;
import com.espertech.esper.runtime.client.option.StatementSubstitutionParameterOption;
import com.espertech.esper.runtime.client.option.StatementUserObjectRuntimeOption;
import com.espertech.esper.runtime.internal.kernel.statement.EPStatementSPI;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

import static com.espertech.esper.runtime.internal.kernel.service.DeployerHelperInitStatement.initializeStatements;
import static com.espertech.esper.runtime.internal.kernel.service.DeployerHelperInitializeEPLObjects.initializeEPLObjects;
import static com.espertech.esper.runtime.internal.kernel.service.DeployerHelperInitializeEPLObjects.validateStagedEPLObjects;
import static com.espertech.esper.runtime.internal.kernel.service.DeployerHelperResolver.resolveDependencies;
import static com.espertech.esper.runtime.internal.kernel.service.DeployerHelperStatement.deployStatements;
import static com.espertech.esper.runtime.internal.kernel.service.DeployerHelperUpdatePath.updatePath;

public class Deployer {

    public static DeploymentInternal deployFresh(String deploymentId, int statementIdFirstStatement, EPCompiled compiled, StatementNameRuntimeOption statementNameResolverRuntime, StatementUserObjectRuntimeOption userObjectResolverRuntime, StatementSubstitutionParameterOption substitutionParameterResolver, DeploymentClassLoaderOption deploymentClassLoaderOption, EPRuntimeSPI epRuntime) throws EPDeployException {
        return deploy(false, deploymentId, statementIdFirstStatement, compiled, statementNameResolverRuntime, userObjectResolverRuntime, substitutionParameterResolver, deploymentClassLoaderOption, epRuntime);
    }

    public static DeploymentInternal deployRecover(String deploymentId, int statementIdFirstStatement, EPCompiled compiled, StatementNameRuntimeOption statementNameResolverRuntime, StatementUserObjectRuntimeOption userObjectResolverRuntime, StatementSubstitutionParameterOption substitutionParameterResolver, DeploymentClassLoaderOption deploymentClassLoaderOption, EPRuntimeSPI epRuntime) throws EPDeployException {
        return deploy(true, deploymentId, statementIdFirstStatement, compiled, statementNameResolverRuntime, userObjectResolverRuntime, substitutionParameterResolver, deploymentClassLoaderOption, epRuntime);
    }

    private static DeploymentInternal deploy(boolean recovery, String deploymentId, int statementIdFirstStatement, EPCompiled compiled, StatementNameRuntimeOption statementNameResolverRuntime, StatementUserObjectRuntimeOption userObjectResolverRuntime, StatementSubstitutionParameterOption substitutionParameterResolver, DeploymentClassLoaderOption deploymentClassLoaderOption, EPRuntimeSPI epRuntime) throws EPDeployException {
        // set variable local version
        epRuntime.getServicesContext().getVariableManagementService().setLocalVersion();

        try {
            return deploySafe(recovery, deploymentId, statementIdFirstStatement, compiled, statementNameResolverRuntime, userObjectResolverRuntime, substitutionParameterResolver, deploymentClassLoaderOption, epRuntime);
        } catch (EPDeployException ex) {
            throw ex;
        } catch (Throwable t) {
            throw new EPDeployException(t.getMessage(), t, -1);
        }
    }

    private static DeploymentInternal deploySafe(boolean recovery,
                                                 String deploymentId,
                                                 int statementIdFirstStatement,
                                                 EPCompiled compiled,
                                                 StatementNameRuntimeOption statementNameResolverRuntime,
                                                 StatementUserObjectRuntimeOption userObjectResolverRuntime,
                                                 StatementSubstitutionParameterOption substitutionParameterResolver,
                                                 DeploymentClassLoaderOption deploymentClassLoaderOption,
                                                 EPRuntimeSPI epRuntime) throws Throwable {

        EPServicesContext services = epRuntime.getServicesContext();
        ClassLoader deploymentClassLoader = DeployerHelperResolver.getClassLoader(-1, deploymentClassLoaderOption, services);
        ModuleProviderCLPair moduleProvider = ModuleProviderUtil.analyze(compiled, deploymentClassLoader);
        String moduleName = moduleProvider.getModuleProvider().getModuleName();

        // resolve external dependencies
        ModuleDependenciesRuntime moduleDependencies = moduleProvider.getModuleProvider().getModuleDependencies();
        Set<String> deploymentIdDependencies = resolveDependencies(-1, moduleDependencies, services);

        // initialize EPL objects defined by module
        DeployerModuleEPLObjects moduleEPLObjects = initializeEPLObjects(moduleProvider, deploymentId, services);

        // determine staged EPL object overlap
        validateStagedEPLObjects(moduleEPLObjects, moduleProvider.getModuleProvider().getModuleName(), -1, epRuntime.getStageService());

        // add EPL objects defined by module to path
        DeployerModulePaths modulePaths = updatePath(-1, moduleEPLObjects, moduleName, deploymentId, services);

        // obtain statement lightweights
        DeployerModuleStatementLightweights stmtLightweights = initializeStatements(-1, recovery, moduleEPLObjects, modulePaths, moduleName, moduleProvider, deploymentId, statementIdFirstStatement, userObjectResolverRuntime, statementNameResolverRuntime, substitutionParameterResolver, services);

        // start statements depending on context association
        EPStatement[] statements = deployStatements(-1, stmtLightweights.getLightweights(), recovery, modulePaths, moduleProvider, deploymentId, epRuntime);

        // add dependencies
        addPathDependencies(deploymentId, moduleDependencies, services);

        // keep statement and deployment
        DeploymentInternal deployed = DeploymentInternal.from(deploymentId, statements, deploymentIdDependencies, modulePaths, moduleEPLObjects, moduleProvider);
        services.getDeploymentLifecycleService().addDeployment(deploymentId, deployed);

        // register for recovery
        if (!recovery) {
            DeploymentRecoveryInformation recoveryInformation = getRecoveryInformation(deployed);
            services.getDeploymentRecoveryService().add(deploymentId, statementIdFirstStatement, compiled, recoveryInformation.getStatementUserObjectsRuntime(), recoveryInformation.getStatementNamesWhenProvidedByAPI(), stmtLightweights.getSubstitutionParameters());
        }

        return deployed;
    }

    static DeploymentRecoveryInformation getRecoveryInformation(DeploymentInternal deployerResult) {
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
        return new DeploymentRecoveryInformation(userObjects, statementNamesWhenOverridden);
    }

    static void addPathDependencies(String deploymentId, ModuleDependenciesRuntime moduleDependencies, EPServicesContext services) {
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

}
