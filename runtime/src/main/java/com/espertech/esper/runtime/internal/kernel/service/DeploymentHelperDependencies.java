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

import com.espertech.esper.common.internal.collection.PathRegistry;
import com.espertech.esper.common.internal.context.module.ModuleIndexMeta;
import com.espertech.esper.common.internal.epl.lookupplansubord.EventTableIndexMetadata;
import com.espertech.esper.common.internal.epl.lookupplansubord.EventTableIndexMetadataEntry;
import com.espertech.esper.common.internal.epl.namedwindow.path.NamedWindowMetaData;
import com.espertech.esper.common.internal.epl.script.core.NameAndParamNum;
import com.espertech.esper.common.internal.epl.table.compiletime.TableMetaData;
import com.espertech.esper.runtime.client.EPDeploymentDependencyConsumed;
import com.espertech.esper.runtime.client.EPDeploymentDependencyProvided;
import com.espertech.esper.runtime.client.util.EPObjectType;

import java.util.*;
import java.util.function.Function;

public class DeploymentHelperDependencies {

    private final static Function<NameAndParamNum, String> SCRIPT_OBJECTNAME = nameParam -> nameParam.getName() + "#" + Integer.toString(nameParam.getParamNum());
    private final static Function<ModuleIndexMeta, String> INDEX_OBJECTNAME = idx -> idx.getIndexName() + (idx.isNamedWindow() ? " on named-window " : " on table ") + idx.getInfraName();

    // what are the dependencies that the given deployment consumes from other modules?
    public static EPDeploymentDependencyConsumed getDependenciesConsumed(String selfDeploymentId, EPServicesContext services) {
        DeploymentInternal selfDeployment = services.getDeploymentLifecycleService().getDeploymentById(selfDeploymentId);
        if (selfDeployment == null) {
            return null;
        }
        String[] consumedDeploymentIds = selfDeployment.getDeploymentIdDependencies();
        List<EPDeploymentDependencyConsumed.Item> consumed = new ArrayList<>(4);
        for (String providerDeploymentId : consumedDeploymentIds) {
            DeploymentInternal providingDeployment = services.getDeploymentLifecycleService().getDeploymentById(providerDeploymentId);
            if (providingDeployment == null) {
                continue;
            }
            String moduleName = providingDeployment.getModuleProvider().getModuleName();
            handleConsumed(providerDeploymentId, providingDeployment.getPathNamedWindows(), EPObjectType.NAMEDWINDOW, services.getNamedWindowPathRegistry(), moduleName, selfDeploymentId, consumed, name -> name);
            handleConsumed(providerDeploymentId, providingDeployment.getPathTables(), EPObjectType.TABLE, services.getTablePathRegistry(), moduleName, selfDeploymentId, consumed, name -> name);
            handleConsumed(providerDeploymentId, providingDeployment.getPathVariables(), EPObjectType.VARIABLE, services.getVariablePathRegistry(), moduleName, selfDeploymentId, consumed, name -> name);
            handleConsumed(providerDeploymentId, providingDeployment.getPathContexts(), EPObjectType.CONTEXT, services.getContextPathRegistry(), moduleName, selfDeploymentId, consumed, name -> name);
            handleConsumed(providerDeploymentId, providingDeployment.getPathEventTypes(), EPObjectType.EVENTTYPE, services.getEventTypePathRegistry(), moduleName, selfDeploymentId, consumed, name -> name);
            handleConsumed(providerDeploymentId, providingDeployment.getPathExprDecls(), EPObjectType.EXPRESSION, services.getExprDeclaredPathRegistry(), moduleName, selfDeploymentId, consumed, name -> name);
            handleConsumed(providerDeploymentId, providingDeployment.getPathScripts(), EPObjectType.SCRIPT, services.getScriptPathRegistry(), moduleName, selfDeploymentId, consumed, SCRIPT_OBJECTNAME);

            for (ModuleIndexMeta objectName : providingDeployment.getPathIndexes()) {
                EventTableIndexMetadata indexMetadata = getIndexMetadata(objectName, moduleName, services);
                if (indexMetadata == null) {
                    continue;
                }
                EventTableIndexMetadataEntry meta = indexMetadata.getIndexEntryByName(objectName.getIndexName());
                if (meta != null && meta.getReferringDeployments() != null && meta.getReferringDeployments().length > 0) {
                    boolean found = false;
                    for (String dep : meta.getReferringDeployments()) {
                        if (dep.equals(selfDeploymentId)) {
                            found = true;
                            break;
                        }
                    }
                    if (found) {
                        consumed.add(new EPDeploymentDependencyConsumed.Item(providerDeploymentId, EPObjectType.INDEX, INDEX_OBJECTNAME.apply(objectName)));
                    }
                }
            }
        }
        return new EPDeploymentDependencyConsumed(consumed);
    }

    // what are the dependencies that the given deployment provides to other modules?
    public static EPDeploymentDependencyProvided getDependenciesProvided(String selfDeploymentId, EPServicesContext services) {
        DeploymentInternal selfDeployment = services.getDeploymentLifecycleService().getDeploymentById(selfDeploymentId);
        if (selfDeployment == null) {
            return null;
        }
        List<EPDeploymentDependencyProvided.Item> dependencies = new ArrayList<>(4);
        String moduleName = selfDeployment.getModuleProvider().getModuleName();
        handleProvided(selfDeployment.getPathNamedWindows(), EPObjectType.NAMEDWINDOW, services.getNamedWindowPathRegistry(), moduleName, dependencies, name -> name);
        handleProvided(selfDeployment.getPathTables(), EPObjectType.TABLE, services.getTablePathRegistry(), moduleName, dependencies, name -> name);
        handleProvided(selfDeployment.getPathVariables(), EPObjectType.VARIABLE, services.getVariablePathRegistry(), moduleName, dependencies, name -> name);
        handleProvided(selfDeployment.getPathContexts(), EPObjectType.CONTEXT, services.getContextPathRegistry(), moduleName, dependencies, name -> name);
        handleProvided(selfDeployment.getPathEventTypes(), EPObjectType.EVENTTYPE, services.getEventTypePathRegistry(), moduleName, dependencies, name -> name);
        handleProvided(selfDeployment.getPathExprDecls(), EPObjectType.EXPRESSION, services.getExprDeclaredPathRegistry(), moduleName, dependencies, name -> name);
        handleProvided(selfDeployment.getPathScripts(), EPObjectType.SCRIPT, services.getScriptPathRegistry(), moduleName, dependencies, SCRIPT_OBJECTNAME);

        for (ModuleIndexMeta objectName : selfDeployment.getPathIndexes()) {
            EventTableIndexMetadata indexMetadata = getIndexMetadata(objectName, moduleName, services);
            if (indexMetadata == null) {
                continue;
            }
            EventTableIndexMetadataEntry meta = indexMetadata.getIndexEntryByName(objectName.getIndexName());
            if (meta != null && meta.getReferringDeployments() != null && meta.getReferringDeployments().length > 0) {
                Set<String> referred = new HashSet<>(Arrays.asList(meta.getReferringDeployments()));
                referred.remove(selfDeploymentId);
                if (!referred.isEmpty()) {
                    dependencies.add(new EPDeploymentDependencyProvided.Item(EPObjectType.INDEX, INDEX_OBJECTNAME.apply(objectName), referred));
                }
            }
        }

        return new EPDeploymentDependencyProvided(dependencies);
    }

    private static EventTableIndexMetadata getIndexMetadata(ModuleIndexMeta objectName, String moduleName, EPServicesContext services) {
        if (objectName.isNamedWindow()) {
            NamedWindowMetaData metaData = services.getNamedWindowPathRegistry().getWithModule(objectName.getInfraName(), moduleName);
            return metaData == null ? null : metaData.getIndexMetadata();
        }
        TableMetaData metaData = services.getTablePathRegistry().getWithModule(objectName.getInfraName(), moduleName);
        return metaData == null ? null : metaData.getIndexMetadata();
    }

    private static <K, E> void handleConsumed(String providerDeploymentId, K[] objectNames, EPObjectType objectType, PathRegistry<K, E> registry, String moduleName, String selfDeploymentId, List<EPDeploymentDependencyConsumed.Item> consumed, Function<K, String> objectNameFunction) {
        for (K objectName : objectNames) {
            try {
                Set<String> ids = registry.getDependencies(objectName, moduleName);
                if (ids != null && ids.contains(selfDeploymentId)) {
                    consumed.add(new EPDeploymentDependencyConsumed.Item(providerDeploymentId, objectType, objectNameFunction.apply(objectName)));
                }
            } catch (IllegalArgumentException ex) {
                // not handled
            }
        }
    }

    private static <K, E> void handleProvided(K[] objectNames, EPObjectType objectType, PathRegistry<K, E> registry, String moduleName, List<EPDeploymentDependencyProvided.Item> dependencies, Function<K, String> objectNameFunction) {
        for (K objectName : objectNames) {
            try {
                Set<String> ids = registry.getDependencies(objectName, moduleName);
                if (ids != null) {
                    dependencies.add(new EPDeploymentDependencyProvided.Item(objectType, objectNameFunction.apply(objectName), new HashSet<>(ids)));
                }
            } catch (IllegalArgumentException ex) {
                // no need to handle
            }
        }
    }
}
