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
package com.espertech.esper.regressionlib.suite.event.bean;

import com.espertech.esper.common.client.EventBean;
import com.espertech.esper.regressionlib.framework.RegressionEnvironment;
import com.espertech.esper.regressionlib.framework.RegressionExecution;
import com.espertech.esper.regressionlib.support.bean.*;
import com.espertech.esper.runtime.client.EPStatement;
import com.espertech.esper.runtime.client.scopetest.SupportUpdateListener;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import static org.junit.Assert.*;

public class EventBeanInheritAndInterface {
    public static Collection<RegressionExecution> executions() {
        List<RegressionExecution> execs = new ArrayList<>();
        execs.add(new EventBeanInheritAndInterfaceOverridingSubclass());
        execs.add(new EventBeanInheritAndInterfaceImplementationClass());
        return execs;
    }

    private static class EventBeanInheritAndInterfaceOverridingSubclass implements RegressionExecution {
        public void run(RegressionEnvironment env) {
            String epl = "@name('s0') select val as value from SupportOverrideOne#length(10)";
            env.compileDeployAddListenerMileZero(epl, "s0");

            env.sendEventBean(new SupportOverrideOneA("valA", "valOne", "valBase"));
            EventBean theEvent = env.listener("s0").getAndResetLastNewData()[0];
            assertEquals("valA", theEvent.get("value"));

            env.sendEventBean(new SupportOverrideBase("x"));
            assertFalse(env.listener("s0").isInvoked());

            env.sendEventBean(new SupportOverrideOneB("valB", "valTwo", "valBase2"));
            theEvent = env.listener("s0").getAndResetLastNewData()[0];
            assertEquals("valB", theEvent.get("value"));

            env.sendEventBean(new SupportOverrideOne("valThree", "valBase3"));
            theEvent = env.listener("s0").getAndResetLastNewData()[0];
            assertEquals("valThree", theEvent.get("value"));

            env.undeployAll();
        }
    }

    private static class EventBeanInheritAndInterfaceImplementationClass implements RegressionExecution {
        public void run(RegressionEnvironment env) {
            String[] epls = {
                "select baseAB from ISupportBaseAB#length(10)",
                "select baseAB, a from ISupportA#length(10)",
                "select baseAB, b from ISupportB#length(10)",
                "select c from ISupportC#length(10)",
                "select baseAB, a, g from ISupportAImplSuperG#length(10)",
                "select baseAB, a, b, g, c from ISupportAImplSuperGImplPlus#length(10)",
            };

            String[][] expected = {
                {"baseAB"},
                {"baseAB", "a"},
                {"baseAB", "b"},
                {"c"},
                {"baseAB", "a", "g"},
                {"baseAB", "a", "b", "g", "c"}
            };

            EPStatement[] stmts = new EPStatement[epls.length];
            SupportUpdateListener[] listeners = new SupportUpdateListener[epls.length];
            for (int i = 0; i < epls.length; i++) {
                String name = String.format("@name('%s')", "stmt_" + i);
                env.compileDeploy(name + epls[i]);
                stmts[i] = env.statement("stmt_" + i);
                listeners[i] = new SupportUpdateListener();
                stmts[i].addListener(listeners[i]);
            }

            env.sendEventBean(new ISupportAImplSuperGImplPlus("g", "a", "baseAB", "b", "c"));
            for (int i = 0; i < listeners.length; i++) {
                assertTrue(listeners[i].isInvoked());
                EventBean theEvent = listeners[i].getAndResetLastNewData()[0];

                for (int j = 0; j < expected[i].length; j++) {
                    assertTrue("failed property valid check for stmt=" + epls[i], theEvent.getEventType().isProperty(expected[i][j]));
                    assertEquals("failed property check for stmt=" + epls[i], expected[i][j], theEvent.get(expected[i][j]));
                }
            }

            env.undeployAll();
        }
    }
}
