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

import com.espertech.esper.regressionlib.framework.RegressionExecution;
import com.espertech.esper.regressionlib.framework.RegressionExecutionWithConfigure;
import com.espertech.esper.regressionlib.framework.RegressionFilter;
import com.espertech.esper.runtime.client.EPRuntime;
import com.espertech.esper.runtime.client.EPRuntimeProvider;
import com.espertech.esper.runtime.internal.metrics.instrumentation.InstrumentationHelper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Collection;

public class RegressionRunner {
    private static final Logger log = LoggerFactory.getLogger(RegressionRunner.class);

    public static void runConfigurable(RegressionExecutionWithConfigure configurable) {
        RegressionSession session = RegressionRunner.session();
        configurable.configure(session.getConfiguration());
        RegressionRunner.run(session, configurable);
        session.destroy();
    }

    public static RegressionSession session() {
        return new RegressionSession(SupportConfigFactory.getConfiguration());
    }

    public static void run(RegressionSession session, Collection<? extends RegressionExecution> executions) {
        Collection<? extends RegressionExecution> filtered = RegressionFilter.filterBySystemProperty(executions);
        for (RegressionExecution execution : filtered) {
            run(session, execution);
        }
    }

    public static void run(RegressionSession session, RegressionExecution execution) {
        if (session.getConfiguration().getClass().getSimpleName().contains("HA")) {
            throw new IllegalStateException("Does not handle HA tests");
        }

        if (session.getRuntime() == null) {
            boolean exists = EPRuntimeProvider.hasRuntime(EPRuntimeProvider.DEFAULT_RUNTIME_URI);
            EPRuntime runtime = EPRuntimeProvider.getDefaultRuntime(session.getConfiguration());
            if (exists) {
                runtime.initialize();
            }
            session.setRuntime(runtime);
        }

        if (InstrumentationHelper.ENABLED && !execution.excludeWhenInstrumented()) {
            InstrumentationHelper.startTest(session.getRuntime(), execution.getClass(), execution.getClass().getName());
        }

        log.info("Running test " + execution.name());
        execution.run(new RegressionEnvironmentEsper(session.getConfiguration(), session.getRuntime()));

        if (InstrumentationHelper.ENABLED && !execution.excludeWhenInstrumented()) {
            InstrumentationHelper.endTest();
        }
    }
}
