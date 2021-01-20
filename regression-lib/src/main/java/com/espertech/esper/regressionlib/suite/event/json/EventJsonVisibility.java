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
package com.espertech.esper.regressionlib.suite.event.json;

import com.espertech.esper.common.client.EPCompiled;
import com.espertech.esper.common.client.EventBean;
import com.espertech.esper.common.client.scopetest.EPAssertionUtil;
import com.espertech.esper.common.internal.support.SupportBean;
import com.espertech.esper.regressionlib.framework.RegressionEnvironment;
import com.espertech.esper.regressionlib.framework.RegressionExecution;
import com.espertech.esper.regressionlib.framework.RegressionPath;

import java.util.ArrayList;
import java.util.List;

public class EventJsonVisibility {

    public static List<RegressionExecution> executions() {
        List<RegressionExecution> execs = new ArrayList<>();
        execs.add(new EventJsonVisibilityPublicSameModule());
        execs.add(new EventJsonVisibilityPublicTwoModulesBinaryPath());
        execs.add(new EventJsonVisibilityPublicTwoModulesRuntimePath());
        execs.add(new EventJsonVisibilityProtected());
        return execs;
    }

    private static class EventJsonVisibilityProtected implements RegressionExecution {
        public void run(RegressionEnvironment env) {
            String moduleA = "module A; @protected create json schema JsonSchema(fruit string, size string);\n";
            String moduleB = "module B; @protected create json schema JsonSchema(carId string);\n";

            RegressionPath pathA = new RegressionPath();
            env.compileDeploy(moduleA, pathA);
            RegressionPath pathB = new RegressionPath();
            env.compileDeploy(moduleB, pathB);

            env.compileDeploy("module A; insert into JsonSchema select theString as fruit, 'large' as size from SupportBean;\n" +
                "@name('a') select fruit, size from JsonSchema#keepall", pathA).addListener("a");
            env.compileDeploy("module B; insert into JsonSchema select theString as carId from SupportBean;\n" +
                "@name('b') select carId from JsonSchema#keepall", pathB).addListener("b");

            env.sendEventBean(new SupportBean("E1", 0));
            env.assertEventNew("a", this::assertFruit);
            env.assertEventNew("b", this::assertCar);

            env.milestone(0);

            env.assertIterator("a", it -> assertFruit(it.next()));
            env.assertIterator("b", it -> assertCar(it.next()));

            env.undeployAll();
        }

        private void assertCar(EventBean event) {
            EPAssertionUtil.assertProps(event, "carId".split(","), new Object[]{"E1"});
        }

        private void assertFruit(EventBean event) {
            EPAssertionUtil.assertProps(event, "fruit,size".split(","), new Object[]{"E1", "large"});
        }
    }

    private static class EventJsonVisibilityPublicSameModule implements RegressionExecution {
        public void run(RegressionEnvironment env) {
            String epl = "@public @buseventtype create json schema SimpleJson(fruit string, size string, color string);\n" +
                "@name('s0') select fruit, size, color from SimpleJson#keepall;\n";
            env.compileDeploy(epl).addListener("s0");

            runAssertionSimple(env);

            env.undeployAll();
        }
    }

    private static class EventJsonVisibilityPublicTwoModulesBinaryPath implements RegressionExecution {
        public void run(RegressionEnvironment env) {
            RegressionPath path = new RegressionPath();
            env.compileDeploy("@public @buseventtype create json schema SimpleJson(fruit string, size string, color string)", path);
            env.compileDeploy("@name('s0') select fruit, size, color from SimpleJson#keepall", path).addListener("s0");

            runAssertionSimple(env);

            env.undeployAll();
        }
    }

    private static class EventJsonVisibilityPublicTwoModulesRuntimePath implements RegressionExecution {
        public void run(RegressionEnvironment env) {
            env.compileDeploy("@public @buseventtype create json schema SimpleJson(fruit string, size string, color string)");
            String epl = "@name('s0') select fruit, size, color from SimpleJson#keepall";
            EPCompiled compiled = env.compileWRuntimePath(epl);
            env.deploy(compiled).addListener("s0");

            runAssertionSimple(env);

            env.undeployAll();
        }
    }

    private static void runAssertionSimple(RegressionEnvironment env) {
        String json = "{ \"fruit\": \"Apple\", \"size\": \"Large\", \"color\": \"Red\"}";
        env.sendEventJson(json, "SimpleJson");
        env.assertEventNew("s0", EventJsonVisibility::assertFruitApple);

        json = "{ \"fruit\": \"Peach\", \"size\": \"Small\", \"color\": \"Yellow\"}";
        env.sendEventJson(json, "SimpleJson");
        env.assertEventNew("s0", EventJsonVisibility::assertFruitPeach);

        env.milestone(0);

        env.assertIterator("s0", it -> {
            assertFruitApple(it.next());
            assertFruitPeach(it.next());
        });
    }

    private static void assertFruitPeach(EventBean event) {
        EPAssertionUtil.assertProps(event, "fruit,size,color".split(","), new Object[]{"Peach", "Small", "Yellow"});
    }

    private static void assertFruitApple(EventBean event) {
        EPAssertionUtil.assertProps(event, "fruit,size,color".split(","), new Object[]{"Apple", "Large", "Red"});
    }
}
