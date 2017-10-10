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
package com.espertech.esper.supportregression.execution;

import com.espertech.esper.client.Configuration;
import com.espertech.esper.client.EPServiceProvider;
import com.espertech.esper.client.EPServiceProviderManager;
import com.espertech.esper.metrics.instrumentation.InstrumentationHelper;
import com.espertech.esper.supportregression.client.SupportConfigFactory;

public class RegressionRunner {
    public static void run(RegressionExecution execution) {
        runInternal(execution, true);
        runInternal(execution, false);
    }

    private static void runInternal(RegressionExecution execution, boolean codegen) {
        Configuration configuration = SupportConfigFactory.getConfiguration();

        if (!codegen) {
            configuration.getEngineDefaults().getByteCodeGeneration().disableAll();
        }

        try {
            execution.configure(configuration);
        } catch (Exception ex) {
            throw new RuntimeException("Configuration-time exception thrown: " + ex.getMessage(), ex);
        }

        EPServiceProvider epService = EPServiceProviderManager.getDefaultProvider(configuration);
        epService.initialize();

        if (!execution.excludeWhenInstrumented()) {
            if (InstrumentationHelper.ENABLED) {
                InstrumentationHelper.startTest(epService, execution.getClass(), execution.getClass().getName());
            }
        }

        try {
            execution.run(epService);
        } catch (Exception ex) {
            throw new RuntimeException("Exception thrown: " + ex.getMessage(), ex);
        }

        if (!execution.excludeWhenInstrumented()) {
            if (InstrumentationHelper.ENABLED) {
                InstrumentationHelper.endTest();
            }
        }
    }
}
