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
package com.espertech.esper.runtime.internal.metrics.instrumentation;

import com.espertech.esper.common.internal.util.JavaClassHelper;
import com.espertech.esper.runtime.client.EPRuntime;
import com.espertech.esper.runtime.internal.kernel.service.EPRuntimeSPI;

import static com.espertech.esper.runtime.internal.metrics.instrumentation.InstrumentationAssertionHelper.assertionService;

public class InstrumentationHelper {
    private final static String PROVIDER_PROPERTY = "esper_instrumentation_provider";

    public final static boolean ENABLED = false;
    public final static boolean ASSERTIONENABLED = false;

    public final static Instrumentation DEFAULT_INSTRUMENTATION = InstrumentationDefault.INSTANCE;
    public static Instrumentation instrumentation = DEFAULT_INSTRUMENTATION;

    public static Instrumentation get() {
        return instrumentation;
    }

    public static void startTest(EPRuntime runtime, Class testClass, String testName) {
        if (!ASSERTIONENABLED) {
            return;
        }
        if (assertionService == null) {
            resolveAssertionService(runtime);
        }
        assertionService.startTest(runtime, testClass, testName);
    }

    public static void endTest() {
        if (!ASSERTIONENABLED) {
            return;
        }
        assertionService.endTest();
    }

    private static void resolveAssertionService(EPRuntime epServiceProvider) {
        String provider = System.getProperty(PROVIDER_PROPERTY);
        if (provider == null) {
            throw new RuntimeException("Failed to find '" + PROVIDER_PROPERTY + "' system property");
        }
        EPRuntimeSPI spi = (EPRuntimeSPI) epServiceProvider;
        assertionService = (InstrumentationAssertionService) JavaClassHelper.instantiate(InstrumentationAssertionService.class, provider, spi.getServicesContext().getClassForNameProvider());
    }
}
