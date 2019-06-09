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
package com.espertech.esper.common.client.configuration.compiler;

import java.io.Serializable;

/**
 * Holds view resources settings.
 */
public class ConfigurationCompilerViewResources implements Serializable {
    private static final long serialVersionUID = 4401151763108581565L;

    private boolean iterableUnbound;
    private boolean outputLimitOpt;

    /**
     * Ctor - sets up defaults.
     */
    protected ConfigurationCompilerViewResources() {
        iterableUnbound = false;
        outputLimitOpt = true;
    }

    /**
     * Returns flag to indicate unbound statements are iterable and return the last event.
     *
     * @return indicator
     */
    public boolean isIterableUnbound() {
        return iterableUnbound;
    }

    /**
     * Sets flag to indicate unbound statements are iterable and return the last event.
     *
     * @param iterableUnbound to set
     */
    public void setIterableUnbound(boolean iterableUnbound) {
        this.iterableUnbound = iterableUnbound;
    }

    /**
     * Returns indicator whether for output limiting the options are enabled by default.
     * Has the same effect as adding "@hint("ENABLE_OUTPUTLIMIT_OPT") to all statements (true by default).
     *
     * @return flag
     */
    public boolean isOutputLimitOpt() {
        return outputLimitOpt;
    }

    /**
     * Sets indicator whether for output limiting the options are enabled by default.
     * Has the same effect as adding "@hint("ENABLE_OUTPUTLIMIT_OPT") to all statements (true by default).
     *
     * @param outputLimitOpt flag
     */
    public void setOutputLimitOpt(boolean outputLimitOpt) {
        this.outputLimitOpt = outputLimitOpt;
    }
}
