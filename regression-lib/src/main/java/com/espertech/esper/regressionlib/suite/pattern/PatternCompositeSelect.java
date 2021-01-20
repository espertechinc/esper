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
package com.espertech.esper.regressionlib.suite.pattern;

import com.espertech.esper.common.client.EventBean;
import com.espertech.esper.common.client.FragmentEventType;
import com.espertech.esper.common.internal.support.SupportEventPropDesc;
import com.espertech.esper.common.internal.support.SupportEventPropUtil;
import com.espertech.esper.regressionlib.framework.RegressionEnvironment;
import com.espertech.esper.regressionlib.framework.RegressionExecution;
import com.espertech.esper.regressionlib.support.bean.SupportBean_A;
import com.espertech.esper.regressionlib.support.bean.SupportBean_B;
import org.junit.Assert;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Map;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public class PatternCompositeSelect {

    public static Collection<RegressionExecution> executions() {
        ArrayList<RegressionExecution> execs = new ArrayList<>();
        execs.add(new PatternFollowedByFilter());
        execs.add(new PatternFragment());
        return execs;
    }

    private static class PatternFollowedByFilter implements RegressionExecution {
        public void run(RegressionEnvironment env) {
            String epl = "@name('insert') insert into StreamOne select * from pattern [a=SupportBean_A -> b=SupportBean_B];\n" +
                "@name('s0') select *, 1 as code from StreamOne;\n";
            env.compileDeploy(epl).addListener("s0");

            env.sendEventBean(new SupportBean_A("A1"));
            env.sendEventBean(new SupportBean_B("B1"));
            env.assertEventNew("s0", theEvent -> {
                Object[] values = new Object[theEvent.getEventType().getPropertyNames().length];
                int count = 0;
                for (String name : theEvent.getEventType().getPropertyNames()) {
                    values[count++] = theEvent.get(name);
                }

                SupportEventPropUtil.assertPropsEquals(env.statement("insert").getEventType().getPropertyDescriptors(),
                    new SupportEventPropDesc("a", SupportBean_A.class).fragment(),
                    new SupportEventPropDesc("b", SupportBean_B.class).fragment());
            });

            env.undeployAll();
        }
    }

    private static class PatternFragment implements RegressionExecution {
        public void run(RegressionEnvironment env) {
            String stmtTxtOne = "@name('s0') select * from pattern [[2] a=SupportBean_A -> b=SupportBean_B]";
            env.compileDeploy(stmtTxtOne).addListener("s0");

            env.assertStatement("s0", statement ->
                SupportEventPropUtil.assertPropsEquals(statement.getEventType().getPropertyDescriptors(),
                new SupportEventPropDesc("a", SupportBean_A[].class).indexed().fragment(),
                new SupportEventPropDesc("b", SupportBean_B.class).fragment()));

            env.sendEventBean(new SupportBean_A("A1"));
            env.sendEventBean(new SupportBean_A("A2"));

            env.milestone(0);

            env.sendEventBean(new SupportBean_B("B1"));

            env.assertEventNew("s0", theEvent -> {
                assertTrue(theEvent.getUnderlying() instanceof Map);

                // test fragment B type and event
                FragmentEventType typeFragB = theEvent.getEventType().getFragmentType("b");
                assertFalse(typeFragB.isIndexed());
                Assert.assertEquals("SupportBean_B", typeFragB.getFragmentType().getName());
                Assert.assertEquals(String.class, typeFragB.getFragmentType().getPropertyType("id"));

                EventBean eventFragB = (EventBean) theEvent.getFragment("b");
                Assert.assertEquals("SupportBean_B", eventFragB.getEventType().getName());

                // test fragment A type and event
                FragmentEventType typeFragA = theEvent.getEventType().getFragmentType("a");
                assertTrue(typeFragA.isIndexed());
                Assert.assertEquals("SupportBean_A", typeFragA.getFragmentType().getName());
                Assert.assertEquals(String.class, typeFragA.getFragmentType().getPropertyType("id"));

                assertTrue(theEvent.getFragment("a") instanceof EventBean[]);
                EventBean eventFragA1 = (EventBean) theEvent.getFragment("a[0]");
                Assert.assertEquals("SupportBean_A", eventFragA1.getEventType().getName());
                Assert.assertEquals("A1", eventFragA1.get("id"));
                EventBean eventFragA2 = (EventBean) theEvent.getFragment("a[1]");
                Assert.assertEquals("SupportBean_A", eventFragA2.getEventType().getName());
                Assert.assertEquals("A2", eventFragA2.get("id"));
            });

            env.undeployAll();
        }
    }
}
