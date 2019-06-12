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
 * Holds view logging settings other then the Apache commons or Log4J settings.
 */
public class ConfigurationCompilerLogging implements Serializable {
    private static final long serialVersionUID = 2629607342543286828L;
    private boolean enableCode;

    /**
     * Ctor - sets up defaults.
     */
    protected ConfigurationCompilerLogging() {
        enableCode = false;
    }

    /**
     * Returns indicator whether code generation logging is enabled or not.
     *
     * @return indicator
     */
    public boolean isEnableCode() {
        return enableCode;
    }

    /**
     * Set indicator whether code generation logging is enabled, by default it is disabled.
     *
     * @param enableCode indicator
     */
    public void setEnableCode(boolean enableCode) {
        this.enableCode = enableCode;
    }

}
