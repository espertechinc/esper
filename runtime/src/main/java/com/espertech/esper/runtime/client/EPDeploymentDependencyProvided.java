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
package com.espertech.esper.runtime.client;

import com.espertech.esper.runtime.client.util.EPObjectType;

import java.util.Collection;
import java.util.Set;

/**
 * Provides information about EPL objects that a deployment provides to other deployments.
 */
public class EPDeploymentDependencyProvided {
    private final Collection<Item> dependencies;

    /**
     * Ctor.
     * @param dependencies provision dependencies
     */
    public EPDeploymentDependencyProvided(Collection<Item> dependencies) {
        this.dependencies = dependencies;
    }

    /**
     * Returns the provision dependencies
     * @return items
     */
    public Collection<Item> getDependencies() {
        return dependencies;
    }

    /**
     * Information about EPL Objects provided by the deployment
     */
    public static class Item {
        private final EPObjectType objectType;
        private final String objectName;
        private final Set<String> deploymentIds;

        /**
         * Ctor.
         * @param objectType EPL object type
         * @param objectName EPL object name
         * @param deploymentIds deployment ids of consumers
         */
        public Item(EPObjectType objectType, String objectName, Set<String> deploymentIds) {
            this.objectType = objectType;
            this.objectName = objectName;
            this.deploymentIds = deploymentIds;
        }

        /**
         * Returns the EPL object type
         * @return object type
         */
        public EPObjectType getObjectType() {
            return objectType;
        }

        /**
         * Returns the EPL object name.
         * For scripts the object name is formatted as the script name followed by hash(#) and followed by the number of parameters.
         * For indexes the object name is formatted as "IndexName on named-window WindowName" or "IndexName on table TableName".
         * @return object name
         */
        public String getObjectName() {
            return objectName;
        }

        /**
         * Returns the deployment id of consuming deployments
         * @return deployment ids
         */
        public Set<String> getDeploymentIds() {
            return deploymentIds;
        }

        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;

            Item that = (Item) o;

            if (objectType != that.objectType) return false;
            if (!objectName.equals(that.objectName)) return false;
            return deploymentIds.equals(that.deploymentIds);

        }

        public int hashCode() {
            int result = objectType.hashCode();
            result = 31 * result + objectName.hashCode();
            return result;
        }

        public String toString() {
            return "Item{" +
                "objectType=" + objectType +
                ", objectName='" + objectName + '\'' +
                ", deploymentIds=" + deploymentIds +
                '}';
        }
    }
}
