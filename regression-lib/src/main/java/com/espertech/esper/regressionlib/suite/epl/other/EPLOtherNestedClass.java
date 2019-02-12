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
package com.espertech.esper.regressionlib.suite.epl.other;

import com.espertech.esper.regressionlib.framework.RegressionEnvironment;
import com.espertech.esper.regressionlib.framework.RegressionExecution;

import java.util.ArrayList;
import java.util.List;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;

public class EPLOtherNestedClass {
    public static List<RegressionExecution> executions() {
        List<RegressionExecution> execs = new ArrayList<>();
        execs.add(new EPLOtherNestedClassEnum());
        return execs;
    }

    private static class EPLOtherNestedClassEnum implements RegressionExecution {
        public void run(RegressionEnvironment env) {
            String epl = "@public @buseventtype create schema MyEventWithColorEnum as " + MyEventWithColorEnum.class.getName() + ";\n" +
                "@name('s0') select " + MyEventWithColorEnum.class.getName() + "$Color.RED as c0 from MyEventWithColorEnum(enumProp=" + MyEventWithColorEnum.class.getName() + "$Color.GREEN)#firstevent";
            env.compileDeploy(epl).addListener("s0");

            env.sendEventBean(new MyEventWithColorEnum(MyEventWithColorEnum.Color.BLUE));
            assertFalse(env.listener("s0").isInvoked());

            env.sendEventBean(new MyEventWithColorEnum(MyEventWithColorEnum.Color.GREEN));
            assertEquals(MyEventWithColorEnum.Color.RED, env.listener("s0").assertOneGetNewAndReset().get("c0"));

            env.undeployAll();
        }
    }

    public static class MyEventWithColorEnum {

        public enum Color {
            GREEN,
            BLUE,
            RED
        }

        private final Color enumProp;

        public MyEventWithColorEnum(Color enumProp) {
            this.enumProp = enumProp;
        }

        public Color getEnumProp() {
            return enumProp;
        }
    }
}
