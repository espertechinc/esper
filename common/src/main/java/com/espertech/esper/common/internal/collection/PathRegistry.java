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
import java.util.Iterator;
import java.util.Map;
import java.util.Set;
import java.util.function.Consumer;

public class PathRegistry<K, E> {
    private final PathRegistryObjectType objectType;
    private final Map<K, PathModuleEntry<E>> entities;

    public PathRegistry(PathRegistryObjectType objectType) {
        this.objectType = objectType;
        this.entities = new HashMap<>();
    }

    private PathRegistry(PathRegistryObjectType objectType, Map<K, PathModuleEntry<E>> entities) {
        this.objectType = objectType;
        this.entities = entities;
    }

    public PathRegistryObjectType getObjectType() {
        return objectType;
    }

    public void add(K entityKey, String moduleName, E entity, String deploymentId)
        throws PathException {
        checkModuleNameParameter(moduleName);
        PathModuleEntry<E> existing = entities.get(entityKey);
        if (existing == null) {
            existing = new PathModuleEntry<>();
            entities.put(entityKey, existing);
        } else {
            String existingDeploymentId = existing.getDeploymentId(moduleName);
            if (existingDeploymentId != null) {
                throw new PathExceptionAlreadyRegistered(entityKey.toString(), objectType, moduleName);
            }
        }
        existing.add(moduleName, entity, deploymentId);
    }

    public Pair<E, String> getAnyModuleExpectSingle(K entityKey, Set<String> moduleUses) throws PathException {
        PathModuleEntry<E> existing = entities.get(entityKey);
        return existing == null ? null : existing.getAnyModuleExpectSingle(entityKey.toString(), objectType, moduleUses);
    }

    public E getWithModule(K entityKey, String moduleName) {
        checkModuleNameParameter(moduleName);
        PathModuleEntry<E> existing = entities.get(entityKey);
        return existing == null ? null : existing.getWithModule(moduleName);
    }

    public String getDeploymentId(K entityEntity, String moduleName) {
        checkModuleNameParameter(moduleName);
        PathModuleEntry<E> existing = entities.get(entityEntity);
        return existing == null ? null : existing.getDeploymentId(moduleName);
    }

    public void deleteDeployment(String deploymentId) {
        Iterator<Map.Entry<K, PathModuleEntry<E>>> it = entities.entrySet().iterator();
        while (it.hasNext()) {
            Map.Entry<K, PathModuleEntry<E>> entry = it.next();
            boolean empty = entry.getValue().deleteDeployment(deploymentId);
            if (empty) {
                it.remove();
            }
        }
    }

    public int getCount() {
        return entities.size();
    }

    public void addDependency(K entityKey, String moduleName, String deploymentIdDep) {
        checkModuleNameParameter(moduleName);
        PathModuleEntry<E> existing = entities.get(entityKey);
        if (existing == null) {
            throw new IllegalArgumentException("Failed to find " + objectType.getName() + " '" + entityKey + "'");
        }
        existing.addDependency(entityKey.toString(), moduleName, deploymentIdDep, objectType);
    }

    public Set<String> getDependencies(K entityKey, String moduleName) {
        checkModuleNameParameter(moduleName);
        PathModuleEntry<E> existing = entities.get(entityKey);
        if (existing == null) {
            return null;
        }
        return existing.getDependencies(entityKey.toString(), moduleName, objectType);
    }

    public void removeDependency(K entityKey, String moduleName, String deploymentId) {
        checkModuleNameParameter(moduleName);
        PathModuleEntry<E> existing = entities.get(entityKey);
        if (existing == null) {
            return;
        }
        existing.removeDependency(moduleName, deploymentId);
    }

    public void traverse(Consumer<E> consumer) {
        for (Map.Entry<K, PathModuleEntry<E>> entry : entities.entrySet()) {
            entry.getValue().traverse(consumer);
        }
    }

    private void checkModuleNameParameter(String moduleName) {
        if (moduleName != null && moduleName.length() == 0) {
            throw new IllegalArgumentException("Invalid empty module name, use null or a non-empty value");
        }
    }

    public void mergeFrom(PathRegistry<K, E> other) {
        if (other.objectType != this.objectType) {
            throw new IllegalArgumentException("Invalid object type " + other.objectType + " expected " + this.objectType);
        }
        for (Map.Entry<K, PathModuleEntry<E>> entry : other.entities.entrySet()) {
            if (entities.containsKey(entry.getKey())) {
                continue;
            }
            entities.put(entry.getKey(), entry.getValue());
        }
    }

    public PathRegistry<K, E> copy() {
        Map<K, PathModuleEntry<E>> copy = new HashMap<>(CollectionUtil.capacityHashMap(entities.size()));
        for (Map.Entry<K, PathModuleEntry<E>> entry : entities.entrySet()) {
            PathModuleEntry<E> entryCopy = entry.getValue().copy();
            copy.put(entry.getKey(), entryCopy);
        }
        return new PathRegistry<>(objectType, copy);
    }
}
