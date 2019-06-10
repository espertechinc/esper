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
import java.util.concurrent.TimeUnit;

/**
 * Time source configuration, the default in MILLI (millisecond resolution from System.currentTimeMillis).
 */
public class ConfigurationCommonTimeSource implements Serializable {
    private static final long serialVersionUID = 4952034651495455128L;
    private TimeUnit timeUnit = TimeUnit.MILLISECONDS;

    /**
     * Returns the time unit time resolution level of time tracking
     *
     * @return time resolution
     */
    public TimeUnit getTimeUnit() {
        return timeUnit;
    }

    /**
     * Sets the time unit time resolution level of time tracking
     *
     * @param timeUnit time resolution
     */
    public void setTimeUnit(TimeUnit timeUnit) {
        this.timeUnit = timeUnit;
    }
}
