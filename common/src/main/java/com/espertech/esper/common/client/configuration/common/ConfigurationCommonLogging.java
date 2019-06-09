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
package com.espertech.esper.common.client.configuration.common;

import java.io.Serializable;

/**
 * Holds view logging settings other then the Apache commons or Log4J settings.
 */
public class ConfigurationCommonLogging implements Serializable {
    private static final long serialVersionUID = -6972204511237827822L;
    private boolean enableQueryPlan;
    private boolean enableJDBC;

    /**
     * Ctor - sets up defaults.
     */
    protected ConfigurationCommonLogging() {
        enableQueryPlan = false;
        enableJDBC = false;
    }

    /**
     * Returns indicator whether query plan logging is enabled or not.
     *
     * @return indicator
     */
    public boolean isEnableQueryPlan() {
        return enableQueryPlan;
    }

    /**
     * Set indicator whether query plan logging is enabled, by default it is disabled.
     *
     * @param enableQueryPlan indicator
     */
    public void setEnableQueryPlan(boolean enableQueryPlan) {
        this.enableQueryPlan = enableQueryPlan;
    }

    /**
     * Returns an indicator whether JDBC query reporting is enabled.
     *
     * @return indicator
     */
    public boolean isEnableJDBC() {
        return enableJDBC;
    }

    /**
     * Set the indicator whether JDBC query reporting is enabled.
     *
     * @param enableJDBC set to true for JDBC query reorting enabled
     */
    public void setEnableJDBC(boolean enableJDBC) {
        this.enableJDBC = enableJDBC;
    }
}
