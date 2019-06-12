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
package com.espertech.esper.common.internal.collection;

import com.espertech.esper.common.internal.util.CollectionUtil;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.function.Consumer;

public class PathModuleEntry<E> {
    private final Map<String, PathDeploymentEntry<E>> modules;

    PathModuleEntry() {
        modules = new HashMap<>(CollectionUtil.capacityHashMap(1));
    }

    private PathModuleEntry(Map<String, PathDeploymentEntry<E>> modules) {
        this.modules = modules;
    }

    public void add(String moduleName, E entity, String deploymentId) {
        modules.put(moduleName, new PathDeploymentEntry<>(deploymentId, entity));
    }

    public Pair<E, String> getAnyModuleExpectSingle(String entityName, PathRegistryObjectType objectType, Set<String> moduleNames) throws PathException {
        if (modules.isEmpty()) {
            return null;
        }
        if (moduleNames == null || moduleNames.isEmpty()) {
            if (modules.size() > 1) {
                throw new PathExceptionAmbiguous(entityName, objectType);
            }
            String moduleName = modules.entrySet().iterator().next().getKey();
            PathDeploymentEntry<E> entry = modules.get(moduleName);
            if (entry == null) {
                return null;
            }
            return new Pair<>(entry.getEntity(), moduleName);
        } else {
            PathDeploymentEntry<E> found = null;
            String moduleNameFound = null;
            for (String moduleName : moduleNames) {
                PathDeploymentEntry<E> entry = modules.get(moduleName);
                if (entry != null) {
                    if (found != null) {
                        throw new PathExceptionAmbiguous(entityName, objectType);
                    }
                    found = entry;
                    moduleNameFound = moduleName;
                }
            }
            return found == null ? null : new Pair<>(found.getEntity(), moduleNameFound);
        }
    }

    public String getDeploymentId(String moduleName) {
        PathDeploymentEntry<E> existing = modules.get(moduleName);
        return existing == null ? null : existing.getDeploymentId();
    }

    public E getWithModule(String moduleName) {
        PathDeploymentEntry<E> entry = modules.get(moduleName);
        return entry == null ? null : entry.getEntity();
    }

    public boolean deleteDeployment(String deploymentId) {
        for (Map.Entry<String, PathDeploymentEntry<E>> entry : modules.entrySet()) {
            if (entry.getValue().getDeploymentId().equals(deploymentId)) {
                modules.remove(entry.getKey());
                return modules.isEmpty();
            }
        }
        return modules.isEmpty();
    }

    public void addDependency(String entityName, String moduleName, String deploymentIdDep, PathRegistryObjectType objectType) {
        PathDeploymentEntry<E> existing = modules.get(moduleName);
        if (existing == null) {
            throw new IllegalArgumentException("Failed to find " + objectType.getName() + " '" + entityName + "' under module '" + moduleName + "'");
        }
        existing.addDependency(deploymentIdDep);
    }

    public Set<String> getDependencies(String entityName, String moduleName, PathRegistryObjectType objectType) {
        PathDeploymentEntry<E> existing = modules.get(moduleName);
        if (existing == null) {
            throw new IllegalArgumentException("Failed to find " + objectType.getName() + " '" + entityName + "' under module '" + moduleName + "'");
        }
        return existing.getDependencies();
    }

    public void removeDependency(String moduleName, String deploymentId) {
        PathDeploymentEntry<E> existing = modules.get(moduleName);
        if (existing == null) {
            return;
        }
        existing.removeDependency(deploymentId);
    }

    public void traverse(Consumer<E> consumer) {
        for (Map.Entry<String, PathDeploymentEntry<E>> entry : modules.entrySet()) {
            consumer.accept(entry.getValue().getEntity());
        }
    }

    public PathModuleEntry<E> copy() {
        Map<String, PathDeploymentEntry<E>> copy = new HashMap<>(4);
        for (Map.Entry<String, PathDeploymentEntry<E>> entry : modules.entrySet()) {
            PathDeploymentEntry<E> copyEntry = entry.getValue().copy();
            copy.put(entry.getKey(), copyEntry);
        }
        return new PathModuleEntry<>(copy);
    }
}
