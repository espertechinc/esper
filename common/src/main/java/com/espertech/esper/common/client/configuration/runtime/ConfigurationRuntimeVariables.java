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

/**
 * Holds variables settings.
 */
public class ConfigurationRuntimeVariables implements Serializable {
    private long msecVersionRelease;
    private static final long serialVersionUID = 8276015152830052323L;

    /**
     * Ctor - sets up defaults.
     */
    protected ConfigurationRuntimeVariables() {
        msecVersionRelease = 15000;
    }

    /**
     * Returns the number of milliseconds that a version of a variables is held stable for
     * use by very long-running atomic statement execution.
     * <p>
     * A slow-executing statement such as an SQL join may use variables that, at the time
     * the statement starts to execute, have certain values. The runtime guarantees that during
     * statement execution the value of the variables stays the same as long as the statement
     * does not take longer then the given number of milliseconds to execute. If the statement does take longer
     * to execute then the variables release time, the current variables value applies instead.
     *
     * @return millisecond time interval that a variables version is guaranteed to be stable
     * in the context of an atomic statement execution
     */
    public long getMsecVersionRelease() {
        return msecVersionRelease;
    }

    /**
     * Sets the number of milliseconds that a version of a variables is held stable for
     * use by very long-running atomic statement execution.
     *
     * @param msecVersionRelease millisecond time interval that a variables version is guaranteed to be stable
     *                           in the context of an atomic statement execution
     */
    public void setMsecVersionRelease(long msecVersionRelease) {
        this.msecVersionRelease = msecVersionRelease;
    }
}
