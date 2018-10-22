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
package com.espertech.esper.regressionlib.support.dataflow;

import com.espertech.esper.common.client.dataflow.core.EPDataFlowExecutionException;
import com.espertech.esper.common.client.dataflow.core.EPDataFlowInstance;
import com.espertech.esper.common.client.dataflow.core.EPDataFlowInstantiationException;
import com.espertech.esper.regressionlib.framework.RegressionEnvironment;
import org.junit.Assert;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class SupportDataFlowAssertionUtil {
    private static final Logger log = LoggerFactory.getLogger(SupportDataFlowAssertionUtil.class);

    public static void tryInvalidRun(RegressionEnvironment env, String epl, String name, String message) {
        env.compileDeploy("@name('flow') " + epl);
        EPDataFlowInstance df = env.runtime().getDataFlowService().instantiate(env.deploymentId("flow"), name);

        try {
            df.run();
            Assert.fail();
        } catch (EPDataFlowExecutionException ex) {
            assertException(message, ex.getMessage());
        }

        env.undeployModuleContaining("flow");
    }

    public static void tryInvalidInstantiate(RegressionEnvironment env, String name, String epl, String message) {
        env.compileDeploy("@name('flow') " + epl);

        try {
            env.runtime().getDataFlowService().instantiate(env.deploymentId("flow"), name);
            Assert.fail();
        } catch (EPDataFlowInstantiationException ex) {
            log.info("Expected exception: " + ex.getMessage(), ex);
            assertException(message, ex.getMessage());
        } finally {
            env.undeployModuleContaining("flow");
        }
    }

    private static void assertException(String expected, String message) {
        String received;
        if (message.lastIndexOf("[") != -1) {
            received = message.substring(0, message.lastIndexOf("[") + 1);
        } else {
            received = message;
        }
        if (message.startsWith(expected)) {
            Assert.assertFalse("empty expected message, received:\n" + message, expected.trim().isEmpty());
            return;
        }
        Assert.fail("Expected:\n" + expected + "\nbut received:\n" + received + "\n");
    }
}
