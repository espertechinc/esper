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
package com.espertech.esper.common.client.configuration.runtime;

import java.io.Serializable;
import java.util.TimeZone;

/**
 * Expression evaluation settings in the runtime are for results of expressions.
 */
public class ConfigurationRuntimeExpression implements Serializable {
    private static final long serialVersionUID = -5269442371030768939L;

    private boolean selfSubselectPreeval;
    private TimeZone timeZone;

    /**
     * Ctor.
     */
    public ConfigurationRuntimeExpression() {
        timeZone = TimeZone.getDefault();
        selfSubselectPreeval = true;
    }

    /**
     * Returns the time zone for calendar operations.
     *
     * @return time zone
     */
    public TimeZone getTimeZone() {
        return timeZone;
    }

    /**
     * Sets the time zone for calendar operations.
     *
     * @param timeZone time zone
     */
    public void setTimeZone(TimeZone timeZone) {
        this.timeZone = timeZone;
    }

    /**
     * Set to true (the default) to indicate that sub-selects within a statement are updated first when a new
     * event arrives. This is only relevant for statements in which both sub-selects
     * and the from-clause may react to the same exact event.
     *
     * @return indicator whether to evaluate sub-selects first or last on new event arrival
     */
    public boolean isSelfSubselectPreeval() {
        return selfSubselectPreeval;
    }

    /**
     * Set to true (the default) to indicate that sub-selects within a statement are updated first when a new
     * event arrives. This is only relevant for statements in which both sub-selects
     * and the from-clause may react to the same exact event.
     *
     * @param selfSubselectPreeval indicator whether to evaluate sub-selects first or last on new event arrival
     */
    public void setSelfSubselectPreeval(boolean selfSubselectPreeval) {
        this.selfSubselectPreeval = selfSubselectPreeval;
    }
}
