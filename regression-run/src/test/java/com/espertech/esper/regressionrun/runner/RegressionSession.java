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
package com.espertech.esper.regressionrun.runner;

import com.espertech.esper.common.client.configuration.Configuration;
import com.espertech.esper.runtime.client.EPRuntime;

public class RegressionSession {
    private final Configuration configuration;
    private EPRuntime runtime;

    public RegressionSession(Configuration configuration) {
        this.configuration = configuration;
    }

    public Configuration getConfiguration() {
        return configuration;
    }

    public EPRuntime getRuntime() {
        return runtime;
    }

    public void setRuntime(EPRuntime runtime) {
        this.runtime = runtime;
    }

    public void destroy() {
        if (runtime != null) {
            runtime.destroy();
        }
    }
}
