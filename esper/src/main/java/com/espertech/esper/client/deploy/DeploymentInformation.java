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
package com.espertech.esper.client.deploy;

import java.io.Serializable;
import java.util.Calendar;

/**
 * Available information about deployment made.
 */
public class DeploymentInformation implements Serializable {
    private static final long serialVersionUID = -7517475026231263141L;

    private String deploymentId;
    private Module module;
    private Calendar addedDate;
    private Calendar lastUpdateDate;
    private DeploymentInformationItem[] items;
    private DeploymentState state;

    /**
     * Ctor.
     *
     * @param deploymentId   deployment id
     * @param addedDate      date the deployment was added
     * @param lastUpdateDate date of last update to state
     * @param items          module statement-level details
     * @param state          current state
     * @param module         the module
     */
    public DeploymentInformation(String deploymentId, Module module, Calendar addedDate, Calendar lastUpdateDate, DeploymentInformationItem[] items, DeploymentState state) {
        this.deploymentId = deploymentId;
        this.module = module;
        this.lastUpdateDate = lastUpdateDate;
        this.addedDate = addedDate;
        this.items = items;
        this.state = state;
    }

    /**
     * Returns the deployment id.
     *
     * @return deployment id
     */
    public String getDeploymentId() {
        return deploymentId;
    }


    /**
     * Returns the last update date, i.e. date the information was last updated with new state.
     *
     * @return last update date
     */
    public Calendar getLastUpdateDate() {
        return lastUpdateDate;
    }

    /**
     * Returns deployment statement-level details: Note that for an newly-added undeployed modules
     * not all statement-level information is available and therefore returns an empty array.
     *
     * @return statement details or empty array for newly added deployments
     */
    public DeploymentInformationItem[] getItems() {
        return items;
    }

    /**
     * Returns current deployment state.
     *
     * @return state
     */
    public DeploymentState getState() {
        return state;
    }

    /**
     * Returns date the deployment was added.
     *
     * @return added-date
     */
    public Calendar getAddedDate() {
        return addedDate;
    }

    /**
     * Returns the module.
     *
     * @return module
     */
    public Module getModule() {
        return module;
    }

    public String toString() {
        return "id '" + deploymentId + "' " +
                " added on " + addedDate.getTime().toString();
    }
}
