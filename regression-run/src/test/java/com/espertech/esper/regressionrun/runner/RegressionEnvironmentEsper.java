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
import com.espertech.esper.regressionlib.framework.RegressionEnvironment;
import com.espertech.esper.regressionlib.framework.RegressionEnvironmentBase;
import com.espertech.esper.runtime.client.EPRuntime;
import com.espertech.esper.runtime.client.EPStatement;
import com.espertech.esper.runtime.client.scopetest.SupportListener;
import com.espertech.esper.runtime.client.scopetest.SupportUpdateListener;

import java.util.concurrent.atomic.AtomicInteger;

public class RegressionEnvironmentEsper extends RegressionEnvironmentBase {

    public RegressionEnvironmentEsper(Configuration configuration, EPRuntime runtime) {
        super(configuration, runtime);
    }

    public RegressionEnvironment milestone(int num) {
        return this;
    }

    public RegressionEnvironment milestoneInc(AtomicInteger counter) {
        milestone(counter.getAndIncrement());
        return this;
    }

    public RegressionEnvironment addListener(String statementName) {
        EPStatement stmt = getAssertStatement(statementName);
        stmt.addListener(new SupportUpdateListener());
        return this;
    }

    public boolean isHA() {
        return false;
    }

    public boolean isHA_Releasing() {
        return false;
    }

    public SupportListener listenerNew() {
        return new SupportUpdateListener();
    }
}
