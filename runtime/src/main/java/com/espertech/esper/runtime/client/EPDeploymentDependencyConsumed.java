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

/**
 * Provides information about EPL objects that a deployment consumes (requires, depends on, refers to) from other deployments.
 */
public class EPDeploymentDependencyConsumed {
    private final Collection<Item> dependencies;

    /**
     * Ctor.
     * @param dependencies consumptions
     */
    public EPDeploymentDependencyConsumed(Collection<Item> dependencies) {
        this.dependencies = dependencies;
    }

    /**
     * Returns the consumption dependencies
     * @return items
     */
    public Collection<Item> getDependencies() {
        return dependencies;
    }

    /**
     * Information about EPL objects consumed by another deployment.
     */
    public static class Item {
        private final String deploymentId;
        private final EPObjectType objectType;
        private final String objectName;

        /**
         * Ctor.
         * @param deploymentId deployment id of the provider
         * @param objectType EPL object type
         * @param objectName EPL object name
         */
        public Item(String deploymentId, EPObjectType objectType, String objectName) {
            this.deploymentId = deploymentId;
            this.objectType = objectType;
            this.objectName = objectName;
        }

        /**
         * Returns the deployment id of the provider
         * @return deployment id
         */
        public String getDeploymentId() {
            return deploymentId;
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

        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;

            Item that = (Item) o;

            if (!deploymentId.equals(that.deploymentId)) return false;
            if (objectType != that.objectType) return false;
            return objectName.equals(that.objectName);

        }

        public int hashCode() {
            int result = deploymentId.hashCode();
            result = 31 * result + objectType.hashCode();
            result = 31 * result + objectName.hashCode();
            return result;
        }

        public String toString() {
            return "Item{" +
                "deploymentId='" + deploymentId + '\'' +
                ", objectType=" + objectType +
                ", objectName='" + objectName + '\'' +
                '}';
        }
    }
}
