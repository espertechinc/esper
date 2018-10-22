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

import java.util.Locale;

public class InstrumentationAssertionHelper {
    private final static String PROVIDER_PROPERTY = "instrumentation_provider";

    public final static boolean ASSERTIONENABLED = false;

    public static InstrumentationAssertionService assertionService;

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

    private static void resolveAssertionService(EPRuntime runtime) {
        String provider = System.getProperty(PROVIDER_PROPERTY);
        if (provider == null) {
            throw new RuntimeException("Failed to find '" + PROVIDER_PROPERTY + "' system property");
        }
        if (provider.toLowerCase(Locale.ENGLISH).trim().equals("default")) {
            assertionService = new DefaultInstrumentationAssertionService();
        } else {
            EPRuntimeSPI spi = (EPRuntimeSPI) runtime;
            assertionService = (InstrumentationAssertionService) JavaClassHelper.instantiate(InstrumentationAssertionService.class, provider, spi.getServicesContext().getClasspathImportServiceRuntime().getClassForNameProvider());
        }
    }

    private static class DefaultInstrumentationAssertionService implements InstrumentationAssertionService {
        public void startTest(EPRuntime runtime, Class testClass, String testName) {

        }

        public void endTest() {

        }
    }
}
