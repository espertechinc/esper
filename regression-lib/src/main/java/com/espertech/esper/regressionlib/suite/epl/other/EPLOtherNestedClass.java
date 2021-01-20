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

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

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
            env.assertListenerNotInvoked("s0");

            env.sendEventBean(new MyEventWithColorEnum(MyEventWithColorEnum.Color.GREEN));
            env.assertEqualsNew("s0", "c0", MyEventWithColorEnum.Color.RED);

            env.undeployAll();
        }
    }

    /**
     * Test event; only serializable because it *may* go over the wire  when running remote tests and serialization is just convenient. Serialization generally not used for HA and HA testing.
     */
    public static class MyEventWithColorEnum implements Serializable {

        private static final long serialVersionUID = -6156524967872658236L;

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
