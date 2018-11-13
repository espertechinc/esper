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

import com.espertech.esper.common.internal.util.Copyable;

import java.util.HashSet;
import java.util.Set;

public class PathDeploymentEntry<E> {
    private final String deploymentId;
    private final E entity;
    private Set<String> dependencyDeploymentIds;

    public PathDeploymentEntry(String deploymentId, E entity) {
        this.deploymentId = deploymentId;
        this.entity = entity;
    }

    public String getDeploymentId() {
        return deploymentId;
    }

    public E getEntity() {
        return entity;
    }

    public void addDependency(String deploymentIdDep) {
        if (dependencyDeploymentIds == null) {
            dependencyDeploymentIds = new HashSet<>(4);
        }
        dependencyDeploymentIds.add(deploymentIdDep);
    }

    public Set<String> getDependencies() {
        return dependencyDeploymentIds;
    }

    public void removeDependency(String deploymentId) {
        if (dependencyDeploymentIds == null) {
            return;
        }
        dependencyDeploymentIds.remove(deploymentId);
        if (dependencyDeploymentIds.isEmpty()) {
            dependencyDeploymentIds = null;
        }
    }

    public PathDeploymentEntry<E> copy() {
        E reference;
        if (entity instanceof Copyable) {
            reference = (E) ((Copyable) entity).copy();
        } else {
            reference = entity;
        }
        return new PathDeploymentEntry<>(deploymentId, reference);
    }
}
