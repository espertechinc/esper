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
package com.espertech.esper.regressionlib.suite.infra.namedwindow;

import com.espertech.esper.common.client.EventBean;
import com.espertech.esper.common.client.scopetest.EPAssertionUtil;
import com.espertech.esper.common.internal.support.SupportBean;
import com.espertech.esper.common.internal.support.SupportBean_S0;
import com.espertech.esper.regressionlib.framework.RegressionEnvironment;
import com.espertech.esper.regressionlib.framework.RegressionExecution;
import com.espertech.esper.regressionlib.support.bean.*;

import java.util.ArrayList;
import java.util.Collection;

import static org.junit.Assert.assertEquals;

/**
 * NOTE: More namedwindow-related tests in "nwtable"
 */
public class InfraNamedWindowOnUpdate {

    public static Collection<RegressionExecution> executions() {
        ArrayList<RegressionExecution> execs = new ArrayList<>();
        execs.add(new InfraUpdateNonPropertySet());
        execs.add(new InfraMultipleDataWindowIntersect());
        execs.add(new InfraMultipleDataWindowUnion());
        execs.add(new InfraSubclass());
        execs.add(new InfraUpdateCopyMethodBean());
        execs.add(new InfraUpdateWrapper());
        execs.add(new InfraUpdateMultikeyWArrayPrimitiveArray());
        execs.add(new InfraUpdateMultikeyWArrayTwoFields());
        return execs;
    }

    private static class InfraUpdateMultikeyWArrayTwoFields implements RegressionExecution {

        public void run(RegressionEnvironment env) {
            String epl = "@name('create') create window MyWindow#keepall as SupportEventWithManyArray;\n" +
                "insert into MyWindow select * from SupportEventWithManyArray;\n" +
                "on SupportEventWithIntArray as sewia " +
                "update MyWindow as mw set value = sewia.value " +
                "where mw.id = sewia.id and mw.intOne = sewia.array;\n";
            env.compileDeploy(epl);

            env.sendEventBean(new SupportEventWithManyArray("ID1").withIntOne(new int[] {1, 2}));
            env.sendEventBean(new SupportEventWithManyArray("ID2").withIntOne(new int[] {3, 4}));
            env.sendEventBean(new SupportEventWithManyArray("ID3").withIntOne(new int[] {1}));

            env.milestone(0);

            env.sendEventBean(new SupportEventWithIntArray("ID2", new int[] {3, 4}, 10));
            env.sendEventBean(new SupportEventWithIntArray("ID3", new int[] {1}, 11));
            env.sendEventBean(new SupportEventWithIntArray("ID1", new int[] {1, 2}, 12));
            env.sendEventBean(new SupportEventWithIntArray("IDX", new int[] {1}, 14));
            env.sendEventBean(new SupportEventWithIntArray("ID1", new int[] {1, 2, 3}, 15));

            EPAssertionUtil.assertPropsPerRowAnyOrder(env.iterator("create"), "id,value".split(","),
                new Object[][] {{"ID1", 12}, {"ID2", 10}, {"ID3", 11}});

            env.undeployAll();
        }
    }

    private static class InfraUpdateMultikeyWArrayPrimitiveArray implements RegressionExecution {

        public void run(RegressionEnvironment env) {
            String epl = "@name('create') create window MyWindow#keepall as SupportEventWithManyArray;\n" +
                         "insert into MyWindow select * from SupportEventWithManyArray;\n" +
                         "on SupportEventWithIntArray as sewia " +
                             "update MyWindow as mw set value = sewia.value " +
                             "where mw.intOne = sewia.array;\n";
            env.compileDeploy(epl);

            env.sendEventBean(new SupportEventWithManyArray("E1").withIntOne(new int[] {1, 2}));
            env.sendEventBean(new SupportEventWithManyArray("E2").withIntOne(new int[] {3, 4}));
            env.sendEventBean(new SupportEventWithManyArray("E3").withIntOne(new int[] {1}));
            env.sendEventBean(new SupportEventWithManyArray("E4").withIntOne(new int[] {}));

            env.milestone(0);

            env.sendEventBean(new SupportEventWithIntArray("U1", new int[] {3, 4}, 10));
            env.sendEventBean(new SupportEventWithIntArray("U2", new int[] {1}, 11));
            env.sendEventBean(new SupportEventWithIntArray("U3", new int[] {}, 12));
            env.sendEventBean(new SupportEventWithIntArray("U4", new int[] {1, 2}, 13));

            EPAssertionUtil.assertPropsPerRowAnyOrder(env.iterator("create"), "id,value".split(","),
                new Object[][] {{"E1", 13}, {"E2", 10}, {"E3", 11}, {"E4", 12}});

            env.undeployAll();
        }
    }

    private static class InfraUpdateWrapper implements RegressionExecution {
        public void run(RegressionEnvironment env) {
            String epl = "@name('window') create window MyWindow#keepall as select *, 1 as p0 from SupportBean;\n" +
                "insert into MyWindow select *, 2 as p0 from SupportBean;\n" +
                "on SupportBean_S0 update MyWindow set theString = 'x', p0 = 2;\n";
            env.compileDeploy(epl);
            env.sendEventBean(new SupportBean("E1", 100));
            env.sendEventBean(new SupportBean_S0(-1));
            EPAssertionUtil.assertProps(env.iterator("window").next(), new String[]{"theString", "p0"}, new Object[]{"x", 2});

            env.undeployAll();
        }
    }

    private static class InfraUpdateCopyMethodBean implements RegressionExecution {
        public void run(RegressionEnvironment env) {
            String epl = "@name('window') create window MyWindowBeanCopyMethod#keepall as SupportBeanCopyMethod;\n" +
                "insert into MyWindowBeanCopyMethod select * from SupportBeanCopyMethod;\n" +
                "on SupportBean update MyWindowBeanCopyMethod set valOne = 'x';\n";
            env.compileDeploy(epl);
            env.sendEventBean(new SupportBeanCopyMethod("a", "b"));
            env.sendEventBean(new SupportBean());
            assertEquals("x", env.iterator("window").next().get("valOne"));

            env.undeployAll();
        }
    }

    private static class InfraUpdateNonPropertySet implements RegressionExecution {
        public void run(RegressionEnvironment env) {
            String epl = "create window MyWindowUNP#keepall as SupportBean;\n" +
                "insert into MyWindowUNP select * from SupportBean;\n" +
                "@name('update') on SupportBean_S0 as sb " +
                "update MyWindowUNP as mywin" +
                " set mywin.setIntPrimitive(10)," +
                "     setBeanLongPrimitive999(mywin);\n";
            env.compileDeploy(epl).addListener("update");

            String[] fields = "intPrimitive,longPrimitive".split(",");
            env.sendEventBean(new SupportBean("E1", 1));
            env.sendEventBean(new SupportBean_S0(1));
            EPAssertionUtil.assertProps(env.listener("update").getAndResetLastNewData()[0], fields, new Object[]{10, 999L});

            env.undeployAll();
        }
    }

    private static class InfraMultipleDataWindowIntersect implements RegressionExecution {
        public void run(RegressionEnvironment env) {
            String epl = "@name('create') create window MyWindowMDW#unique(theString)#length(2) as select * from SupportBean;\n" +
                "insert into MyWindowMDW select * from SupportBean;\n" +
                "on SupportBean_A update MyWindowMDW set intPrimitive=intPrimitive*100 where theString=id;\n";
            env.compileDeploy(epl).addListener("create");

            env.sendEventBean(new SupportBean("E1", 2));
            env.sendEventBean(new SupportBean("E2", 3));
            env.sendEventBean(new SupportBean_A("E2"));
            EventBean[] newevents = env.listener("create").getLastNewData();
            EventBean[] oldevents = env.listener("create").getLastOldData();

            assertEquals(1, newevents.length);
            EPAssertionUtil.assertProps(newevents[0], "intPrimitive".split(","), new Object[]{300});
            assertEquals(1, oldevents.length);
            oldevents = EPAssertionUtil.sort(oldevents, "theString");
            EPAssertionUtil.assertPropsPerRow(oldevents, "theString,intPrimitive".split(","), new Object[][]{{"E2", 3}});

            EPAssertionUtil.assertPropsPerRowAnyOrder(env.iterator("create"), "theString,intPrimitive".split(","), new Object[][]{{"E1", 2}, {"E2", 300}});

            env.undeployAll();
        }
    }

    private static class InfraMultipleDataWindowUnion implements RegressionExecution {
        public void run(RegressionEnvironment env) {
            String epl = "@name('create') create window MyWindowMU#unique(theString)#length(2) retain-union as select * from SupportBean;\n" +
                "insert into MyWindowMU select * from SupportBean;\n" +
                "on SupportBean_A update MyWindowMU mw set mw.intPrimitive=intPrimitive*100 where theString=id;\n";
            env.compileDeploy(epl).addListener("create");

            env.sendEventBean(new SupportBean("E1", 2));
            env.sendEventBean(new SupportBean("E2", 3));
            env.sendEventBean(new SupportBean_A("E2"));
            EventBean[] newevents = env.listener("create").getLastNewData();
            EventBean[] oldevents = env.listener("create").getLastOldData();

            assertEquals(1, newevents.length);
            EPAssertionUtil.assertProps(newevents[0], "intPrimitive".split(","), new Object[]{300});
            assertEquals(1, oldevents.length);
            EPAssertionUtil.assertPropsPerRow(oldevents, "theString,intPrimitive".split(","), new Object[][]{{"E2", 3}});

            EventBean[] events = EPAssertionUtil.sort(env.iterator("create"), "theString");
            EPAssertionUtil.assertPropsPerRow(events, "theString,intPrimitive".split(","), new Object[][]{{"E1", 2}, {"E2", 300}});

            env.undeployAll();
        }
    }

    private static class InfraSubclass implements RegressionExecution {
        public void run(RegressionEnvironment env) {
            String epl = "@name('create') create window MyWindowSC#keepall as select * from SupportBeanAbstractSub;\n" +
                "insert into MyWindowSC select * from SupportBeanAbstractSub;\n" +
                "on SupportBean update MyWindowSC set v1=theString, v2=theString;\n";
            env.compileDeploy(epl).addListener("create");

            env.sendEventBean(new SupportBeanAbstractSub("value2"));
            env.listener("create").reset();

            env.sendEventBean(new SupportBean("E1", 1));
            EPAssertionUtil.assertProps(env.listener("create").getLastNewData()[0], new String[]{"v1", "v2"}, new Object[]{"E1", "E1"});

            env.undeployAll();
        }
    }

    // Don't delete me, dynamically-invoked
    public static void setBeanLongPrimitive999(SupportBean event) {
        event.setLongPrimitive(999);
    }
}