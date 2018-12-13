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
 * Holder for script settings.
 */
public class ConfigurationCompilerScripts implements Serializable {
    private static final long serialVersionUID = -3111856398932434323L;
    private boolean enabled = true;
    private String defaultDialect = "js";

    /**
     * Returns the default script dialect.
     *
     * @return dialect
     */
    public String getDefaultDialect() {
        return defaultDialect;
    }

    /**
     * Sets the default script dialect.
     *
     * @param defaultDialect dialect
     */
    public void setDefaultDialect(String defaultDialect) {
        this.defaultDialect = defaultDialect;
    }

    /**
     * Returns indicator whether scripting is allowed or not is not allowed.
     * @return indicator
     */
    public boolean isEnabled() {
        return enabled;
    }

    /**
     * Sets indicator whether scripting is allowed or not is not allowed.
     * @param enabled indicator
     */
    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }
}
