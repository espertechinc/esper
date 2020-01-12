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
package com.espertech.esper.regressionlib.suite.client.stage;

import com.espertech.esper.regressionlib.framework.RegressionEnvironment;
import com.espertech.esper.regressionlib.framework.RegressionExecution;
import com.espertech.esper.runtime.client.stage.EPStage;

import java.util.ArrayList;
import java.util.List;

import static com.espertech.esper.common.client.scopetest.EPAssertionUtil.assertEqualsAnyOrder;
import static org.junit.Assert.*;

public class ClientStageEPStageService {

    public static List<RegressionExecution> executions() {
        List<RegressionExecution> execs = new ArrayList<>();
        execs.add(new ClientStageEPStageServiceGetStage());
        return execs;
    }

    private static class ClientStageEPStageServiceGetStage implements RegressionExecution {
        public void run(RegressionEnvironment env) {
            EPStage stageAOne = env.stageService().getStage("A");
            EPStage stageBTwo = env.stageService().getStage("A");

            assertSame(stageAOne, stageBTwo);
            assertEquals("A", stageAOne.getURI());
            assertEqualsAnyOrder("A".split(","), env.stageService().getStageURIs());
            assertNull(env.stageService().getExistingStage("B"));
            assertEquals("A", env.stageService().getStage("A").getURI());

            env.milestone(0);

            EPStage stageB = env.stageService().getStage("B");
            assertNotSame(stageB, stageAOne);
            assertEqualsAnyOrder("A,B".split(","), env.stageService().getStageURIs());
            assertNull(env.stageService().getExistingStage("C"));
            assertEquals("A", env.stageService().getExistingStage("A").getURI());
            assertEquals("B", env.stageService().getExistingStage("B").getURI());

            env.milestone(1);

            EPStage stageC = env.stageService().getStage("C");
            assertNotSame(stageB, stageC);
            assertEqualsAnyOrder("A,B,C".split(","), env.stageService().getStageURIs());
            assertEquals("A", env.stageService().getExistingStage("A").getURI());
            assertEquals("B", env.stageService().getExistingStage("B").getURI());
            assertEquals("C", env.stageService().getExistingStage("C").getURI());

            env.milestone(2);

            assertEqualsAnyOrder("A,B,C".split(","), env.stageService().getStageURIs());
            env.stageService().getStage("A").destroy();
            assertEqualsAnyOrder("B,C".split(","), env.stageService().getStageURIs());
            assertNull(env.stageService().getExistingStage("A"));
            assertEquals("B", env.stageService().getExistingStage("B").getURI());
            assertEquals("C", env.stageService().getExistingStage("C").getURI());

            env.milestone(3);

            assertNull(env.stageService().getExistingStage("A"));
            assertEquals("B", env.stageService().getExistingStage("B").getURI());
            assertEquals("C", env.stageService().getExistingStage("C").getURI());
            env.stageService().getStage("B").destroy();
            assertEqualsAnyOrder("C".split(","), env.stageService().getStageURIs());
            assertNull(env.stageService().getExistingStage("A"));
            assertNull(env.stageService().getExistingStage("B"));
            assertEquals("C", env.stageService().getExistingStage("C").getURI());

            env.milestone(4);

            assertEquals("C", stageC.getURI());
            stageC = env.stageService().getStage("C");
            stageC.destroy();
            stageC.destroy();
            assertEquals(0, env.stageService().getStageURIs().length);
            assertNull(env.stageService().getExistingStage("A"));
            assertNull(env.stageService().getExistingStage("B"));
            assertNull(env.stageService().getExistingStage("C"));

            try {
                env.stageService().getStage(null);
                fail();
            } catch (IllegalArgumentException ex) {
                // expected
            }

            try {
                env.stageService().getExistingStage(null);
                fail();
            } catch (IllegalArgumentException ex) {
                // expected
            }

            env.undeployAll();
        }
    }
}
