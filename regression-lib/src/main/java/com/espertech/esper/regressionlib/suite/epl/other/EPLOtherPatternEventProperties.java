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

import com.espertech.esper.common.internal.support.SupportBean;
import com.espertech.esper.regressionlib.framework.RegressionEnvironment;
import com.espertech.esper.regressionlib.framework.RegressionExecution;
import com.espertech.esper.regressionlib.support.bean.SupportBeanComplexProps;

import java.util.ArrayList;
import java.util.List;

import static org.junit.Assert.*;

public class EPLOtherPatternEventProperties {
    public static List<RegressionExecution> executions() {
        List<RegressionExecution> execs = new ArrayList<>();
        execs.add(new EPLOtherWildcardSimplePattern());
        execs.add(new EPLOtherWildcardOrPattern());
        execs.add(new EPLOtherPropertiesSimplePattern());
        execs.add(new EPLOtherPropertiesOrPattern());
        return execs;
    }

    private static class EPLOtherWildcardSimplePattern implements RegressionExecution {
        public void run(RegressionEnvironment env) {
            setupSimplePattern(env, "*");

            Object theEvent = new SupportBean();
            env.sendEventBean(theEvent);

            env.assertEventNew("s0", event -> assertSame(theEvent, event.get("a")));

            env.undeployAll();
        }
    }

    private static class EPLOtherWildcardOrPattern implements RegressionExecution {
        public void run(RegressionEnvironment env) {
            setupOrPattern(env, "*");

            Object eventOne = new SupportBean();
            env.sendEventBean(eventOne);
            env.assertEventNew("s0", eventBean -> {
                assertSame(eventOne, eventBean.get("a"));
                assertNull(eventBean.get("b"));
            });

            Object eventTwo = SupportBeanComplexProps.makeDefaultBean();
            env.sendEventBean(eventTwo);
            env.assertEventNew("s0", eventBean -> {
                assertSame(eventTwo, eventBean.get("b"));
                assertNull(eventBean.get("a"));
            });

            env.undeployAll();
        }
    }

    private static class EPLOtherPropertiesSimplePattern implements RegressionExecution {
        public void run(RegressionEnvironment env) {
            setupSimplePattern(env, "a, a as myEvent, a.intPrimitive as myInt, a.theString");

            SupportBean theEvent = new SupportBean();
            theEvent.setIntPrimitive(1);
            theEvent.setTheString("test");
            env.sendEventBean(theEvent);

            env.assertEventNew("s0", eventBean -> {
                assertSame(theEvent, eventBean.get("a"));
                assertSame(theEvent, eventBean.get("myEvent"));
                assertEquals(1, eventBean.get("myInt"));
                assertEquals("test", eventBean.get("a.theString"));
            });

            env.undeployAll();
        }
    }

    private static class EPLOtherPropertiesOrPattern implements RegressionExecution {
        public void run(RegressionEnvironment env) {
            setupOrPattern(env, "a, a as myAEvent, b, b as myBEvent, a.intPrimitive as myInt, " +
                "a.theString, b.simpleProperty as simple, b.indexed[0] as indexed, b.nested.nestedValue as nestedVal");

            Object theEvent = SupportBeanComplexProps.makeDefaultBean();
            env.sendEventBean(theEvent);
            env.assertEventNew("s0", eventBean -> {
                assertSame(theEvent, eventBean.get("b"));
                assertEquals("simple", eventBean.get("simple"));
                assertEquals(1, eventBean.get("indexed"));
                assertEquals("nestedValue", eventBean.get("nestedVal"));
                assertNull(eventBean.get("a"));
                assertNull(eventBean.get("myAEvent"));
                assertNull(eventBean.get("myInt"));
                assertNull(eventBean.get("a.theString"));
            });

            SupportBean eventTwo = new SupportBean();
            eventTwo.setIntPrimitive(2);
            eventTwo.setTheString("test2");
            env.sendEventBean(eventTwo);
            env.assertEventNew("s0", eventBean -> {
                assertEquals(2, eventBean.get("myInt"));
                assertEquals("test2", eventBean.get("a.theString"));
                assertNull(eventBean.get("b"));
                assertNull(eventBean.get("myBEvent"));
                assertNull(eventBean.get("simple"));
                assertNull(eventBean.get("indexed"));
                assertNull(eventBean.get("nestedVal"));
            });

            env.undeployAll();
        }
    }

    private static void setupSimplePattern(RegressionEnvironment env, String selectCriteria) {
        String stmtText = "@name('s0') select " + selectCriteria + " from pattern [a=SupportBean]";
        env.compileDeploy(stmtText).addListener("s0");
    }

    private static void setupOrPattern(RegressionEnvironment env, String selectCriteria) {
        String stmtText = "@name('s0') select " + selectCriteria + " from pattern [every(a=SupportBean" +
            " or b=SupportBeanComplexProps)]";
        env.compileDeploy(stmtText).addListener("s0");
    }
}
