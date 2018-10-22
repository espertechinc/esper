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
package com.espertech.esper.common.internal.util;

public class DeploymentIdNamePair {
    private final String deploymentId;
    private final String name;

    public DeploymentIdNamePair(String deploymentId, String name) {
        if (name == null) {
            throw new IllegalArgumentException("Name is null");
        }
        this.deploymentId = deploymentId;
        this.name = name;
    }

    public String getDeploymentId() {
        return deploymentId;
    }

    public String getName() {
        return name;
    }

    public String toString() {
        return "DeploymentIdNamePair{" +
                "deploymentId='" + deploymentId + '\'' +
                ", name='" + name + '\'' +
                '}';
    }

    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        DeploymentIdNamePair that = (DeploymentIdNamePair) o;

        if (deploymentId != null ? !deploymentId.equals(that.deploymentId) : that.deploymentId != null) return false;
        return name.equals(that.name);
    }

    public int hashCode() {
        int result = deploymentId != null ? deploymentId.hashCode() : 0;
        result = 31 * result + name.hashCode();
        return result;
    }
}
