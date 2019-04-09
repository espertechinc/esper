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

import com.espertech.esper.common.client.EventBean;
import com.espertech.esper.common.client.scopetest.EPAssertionUtil;
import com.espertech.esper.common.client.soda.EPStatementObjectModel;
import com.espertech.esper.common.internal.support.SupportBean;
import com.espertech.esper.common.internal.support.SupportEnum;
import com.espertech.esper.regressionlib.framework.RegressionEnvironment;
import com.espertech.esper.regressionlib.framework.RegressionExecution;
import com.espertech.esper.regressionlib.framework.SupportMessageAssertUtil;
import com.espertech.esper.regressionlib.support.bean.SupportEventWithManyArray;
import com.espertech.esper.runtime.client.scopetest.SupportSubscriberMRD;

import java.util.ArrayList;
import java.util.List;

import static org.junit.Assert.*;

public class EPLOtherForGroupDelivery {
    public static List<RegressionExecution> executions() {
        List<RegressionExecution> execs = new ArrayList<>();
        execs.add(new EPLOtherInvalid());
        execs.add(new EPLOtherSubscriberOnly());
        execs.add(new EPLOtherDiscreteDelivery());
        execs.add(new EPLOtherGroupDelivery());
        execs.add(new EPLOtherGroupDeliveryMultikeyWArraySingleArray());
        execs.add(new EPLOtherGroupDeliveryMultikeyWArrayTwoField());
        return execs;
    }

    private static class EPLOtherGroupDeliveryMultikeyWArrayTwoField implements RegressionExecution {
        public void run(RegressionEnvironment env) {
            env.advanceTime(0);
            String[] fields = new String[]{"theString", "intPrimitive", "longPrimitive"};
            String epl = "create context MyContext start @now end after 1 second;\n" +
                "@name('s0') context MyContext select * from SupportBean#keepall output snapshot when terminated for grouped_delivery (intPrimitive, longPrimitive)";
            env.compileDeploy(epl).addListener("s0");

            sendSB(env, "E1", 1, 10);
            sendSB(env, "E2", 2, 10);
            sendSB(env, "E3", 1, 11);
            sendSB(env, "E4", 2, 10);
            sendSB(env, "E5", 1, 10);

            env.advanceTime(1000);

            List<EventBean[]> received = env.listener("s0").getNewDataList();
            assertEquals(3, received.size());
            EPAssertionUtil.assertPropsPerRow(received.get(0), fields, new Object[][]{{"E1", 1, 10L}, {"E5", 1, 10L}});
            EPAssertionUtil.assertPropsPerRow(received.get(1), fields, new Object[][]{{"E2", 2, 10L}, {"E4", 2, 10L}});
            EPAssertionUtil.assertPropsPerRow(received.get(2), fields, new Object[][]{{"E3", 1, 11L}});

            env.undeployAll();
        }
    }

    private static class EPLOtherGroupDeliveryMultikeyWArraySingleArray implements RegressionExecution {
        public void run(RegressionEnvironment env) {
            env.advanceTime(0);
            String[] fields = new String[]{"id", "intOne"};
            String epl = "create context MyContext start @now end after 1 second;\n" +
                "@name('s0') context MyContext select * from SupportEventWithManyArray#keepall output snapshot when terminated for grouped_delivery (intOne)";
            env.compileDeploy(epl).addListener("s0");

            sendManyArray(env, "E1", new int[]{1, 2});
            sendManyArray(env, "E2", new int[]{1, 3});
            sendManyArray(env, "E3", new int[]{1, 2});
            sendManyArray(env, "E4", new int[]{1, 4});
            sendManyArray(env, "E5", new int[]{1, 4});

            env.advanceTime(1000);

            List<EventBean[]> received = env.listener("s0").getNewDataList();
            assertEquals(3, received.size());
            EPAssertionUtil.assertPropsPerRow(received.get(0), fields, new Object[][]{{"E1", new int[]{1, 2}}, {"E3", new int[]{1, 2}}});
            EPAssertionUtil.assertPropsPerRow(received.get(1), fields, new Object[][]{{"E2", new int[]{1, 3}}});
            EPAssertionUtil.assertPropsPerRow(received.get(2), fields, new Object[][]{{"E4", new int[]{1, 4}}, {"E5", new int[]{1, 4}}});

            env.undeployAll();
        }
    }

    private static class EPLOtherInvalid implements RegressionExecution {
        public void run(RegressionEnvironment env) {
            SupportMessageAssertUtil.tryInvalidCompile(env, "select * from SupportBean for ",
                "Incorrect syntax near end-of-input ('for' is a reserved keyword) expecting an identifier but found end-of-input at line 1 column 29");

            SupportMessageAssertUtil.tryInvalidCompile(env, "select * from SupportBean for other_keyword",
                "Expected any of the [grouped_delivery, discrete_delivery] for-clause keywords after reserved keyword 'for'");

            SupportMessageAssertUtil.tryInvalidCompile(env, "select * from SupportBean for grouped_delivery",
                "The for-clause with the grouped_delivery keyword requires one or more grouping expressions");

            SupportMessageAssertUtil.tryInvalidCompile(env, "select * from SupportBean for grouped_delivery()",
                "The for-clause with the grouped_delivery keyword requires one or more grouping expressions");

            SupportMessageAssertUtil.tryInvalidCompile(env, "select * from SupportBean for grouped_delivery(dummy)",
                "Failed to validate for-clause expression 'dummy': Property named 'dummy' is not valid in any stream");

            SupportMessageAssertUtil.tryInvalidCompile(env, "select * from SupportBean for discrete_delivery(dummy)",
                "The for-clause with the discrete_delivery keyword does not allow grouping expressions");

            SupportMessageAssertUtil.tryInvalidCompile(env, "select * from SupportBean for discrete_delivery for grouped_delivery(intPrimitive)",
                "Incorrect syntax near 'for' (a reserved keyword) at line 1 column 48 ");
        }
    }

    private static class EPLOtherSubscriberOnly implements RegressionExecution {
        public void run(RegressionEnvironment env) {
            SupportSubscriberMRD subscriber = new SupportSubscriberMRD();
            sendTimer(env, 0);
            env.compileDeploy("@name('s0') select irstream theString,intPrimitive from SupportBean#time_batch(1) for discrete_delivery");
            env.statement("s0").setSubscriber(subscriber);

            env.sendEventBean(new SupportBean("E1", 1));
            env.sendEventBean(new SupportBean("E2", 2));
            env.sendEventBean(new SupportBean("E3", 1));
            sendTimer(env, 1000);
            assertEquals(3, subscriber.getInsertStreamList().size());
            EPAssertionUtil.assertEqualsExactOrder(new Object[]{"E1", 1}, subscriber.getInsertStreamList().get(0)[0]);
            EPAssertionUtil.assertEqualsExactOrder(new Object[]{"E2", 2}, subscriber.getInsertStreamList().get(1)[0]);
            EPAssertionUtil.assertEqualsExactOrder(new Object[]{"E3", 1}, subscriber.getInsertStreamList().get(2)[0]);

            env.undeployAll();
            subscriber.reset();
            env.compileDeploy("@name('s0') select irstream theString,intPrimitive from SupportBean#time_batch(1) for grouped_delivery(intPrimitive)");
            env.statement("s0").setSubscriber(subscriber);

            env.sendEventBean(new SupportBean("E1", 1));
            env.sendEventBean(new SupportBean("E2", 2));
            env.sendEventBean(new SupportBean("E3", 1));
            sendTimer(env, 2000);
            assertEquals(2, subscriber.getInsertStreamList().size());
            assertEquals(2, subscriber.getRemoveStreamList().size());
            EPAssertionUtil.assertEqualsExactOrder(new Object[]{"E1", 1}, subscriber.getInsertStreamList().get(0)[0]);
            EPAssertionUtil.assertEqualsExactOrder(new Object[]{"E3", 1}, subscriber.getInsertStreamList().get(0)[1]);
            EPAssertionUtil.assertEqualsExactOrder(new Object[]{"E2", 2}, subscriber.getInsertStreamList().get(1)[0]);
            EPAssertionUtil.assertEqualsExactOrder(new Object[]{"E1", 1}, subscriber.getRemoveStreamList().get(0)[0]);
            EPAssertionUtil.assertEqualsExactOrder(new Object[]{"E3", 1}, subscriber.getRemoveStreamList().get(0)[1]);
            EPAssertionUtil.assertEqualsExactOrder(new Object[]{"E2", 2}, subscriber.getRemoveStreamList().get(1)[0]);

            env.undeployAll();
        }
    }

    private static class EPLOtherDiscreteDelivery implements RegressionExecution {
        public void run(RegressionEnvironment env) {
            sendTimer(env, 0);
            env.compileDeploy("@name('s0') select * from SupportBean#time_batch(1) for discrete_delivery").addListener("s0");

            env.sendEventBean(new SupportBean("E1", 1));
            env.sendEventBean(new SupportBean("E2", 2));
            env.sendEventBean(new SupportBean("E3", 1));
            sendTimer(env, 1000);
            assertEquals(3, env.listener("s0").getNewDataList().size());
            EPAssertionUtil.assertPropsPerRow(env.listener("s0").getNewDataList().get(0), "theString,intPrimitive".split(","), new Object[][]{{"E1", 1}});
            EPAssertionUtil.assertPropsPerRow(env.listener("s0").getNewDataList().get(1), "theString,intPrimitive".split(","), new Object[][]{{"E2", 2}});
            EPAssertionUtil.assertPropsPerRow(env.listener("s0").getNewDataList().get(2), "theString,intPrimitive".split(","), new Object[][]{{"E3", 1}});
            env.undeployAll();

            // test no-event delivery
            String epl = "@name('s0') SELECT *  FROM ObjectEvent OUTPUT ALL EVERY 1 seconds for discrete_delivery";
            env.compileDeploy(epl).addListener("s0");
            env.sendEventBean(new Object(), "ObjectEvent");
            sendTimer(env, 2000);
            assertTrue(env.listener("s0").getAndClearIsInvoked());
            sendTimer(env, 3000);
            assertFalse(env.listener("s0").getAndClearIsInvoked());

            env.undeployAll();
        }
    }

    private static class EPLOtherGroupDelivery implements RegressionExecution {
        public void run(RegressionEnvironment env) {
            sendTimer(env, 0);
            env.compileDeploy("@name('s0') select * from SupportBean#time_batch(1) for grouped_delivery (intPrimitive)").addListener("s0");

            env.sendEventBean(new SupportBean("E1", 1));

            env.milestone(0);

            env.sendEventBean(new SupportBean("E2", 2));
            env.sendEventBean(new SupportBean("E3", 1));
            sendTimer(env, 1000);
            assertEquals(2, env.listener("s0").getNewDataList().size());
            assertEquals(2, env.listener("s0").getNewDataList().get(0).length);
            EPAssertionUtil.assertPropsPerRow(env.listener("s0").getNewDataList().get(0), "theString,intPrimitive".split(","), new Object[][]{{"E1", 1}, {"E3", 1}});
            assertEquals(1, env.listener("s0").getNewDataList().get(1).length);
            EPAssertionUtil.assertPropsPerRow(env.listener("s0").getNewDataList().get(1), "theString,intPrimitive".split(","), new Object[][]{{"E2", 2}});

            // test sorted
            env.undeployAll();
            env.compileDeploy("@name('s0') select * from SupportBean#time_batch(1) order by intPrimitive desc for grouped_delivery (intPrimitive)");
            env.addListener("s0");

            env.sendEventBean(new SupportBean("E1", 1));
            env.sendEventBean(new SupportBean("E2", 2));
            env.sendEventBean(new SupportBean("E3", 1));
            sendTimer(env, 2000);
            assertEquals(2, env.listener("s0").getNewDataList().size());
            assertEquals(1, env.listener("s0").getNewDataList().get(0).length);
            EPAssertionUtil.assertPropsPerRow(env.listener("s0").getNewDataList().get(0), "theString,intPrimitive".split(","), new Object[][]{{"E2", 2}});
            assertEquals(2, env.listener("s0").getNewDataList().get(1).length);
            EPAssertionUtil.assertPropsPerRow(env.listener("s0").getNewDataList().get(1), "theString,intPrimitive".split(","), new Object[][]{{"E1", 1}, {"E3", 1}});

            // test multiple criteria
            env.undeployAll();
            String stmtText = "@name('s0') select theString, doubleBoxed, enumValue from SupportBean#time_batch(1) order by theString, doubleBoxed, enumValue for grouped_delivery(doubleBoxed, enumValue)";
            env.compileDeploy(stmtText).addListener("s0");

            sendEvent(env, "E1", 10d, SupportEnum.ENUM_VALUE_2); // A (1)
            sendEvent(env, "E2", 11d, SupportEnum.ENUM_VALUE_1); // B (2)
            sendEvent(env, "E3", 9d, SupportEnum.ENUM_VALUE_2);  // C (3)
            sendEvent(env, "E4", 10d, SupportEnum.ENUM_VALUE_2); // A
            sendEvent(env, "E5", 10d, SupportEnum.ENUM_VALUE_1); // D (4)
            sendEvent(env, "E6", 10d, SupportEnum.ENUM_VALUE_1); // D
            sendEvent(env, "E7", 11d, SupportEnum.ENUM_VALUE_1); // B
            sendEvent(env, "E8", 10d, SupportEnum.ENUM_VALUE_1); // D
            sendTimer(env, 3000);
            assertEquals(4, env.listener("s0").getNewDataList().size());
            String[] fields = "theString,doubleBoxed,enumValue".split(",");
            EPAssertionUtil.assertPropsPerRow(env.listener("s0").getNewDataList().get(0), fields,
                new Object[][]{{"E1", 10d, SupportEnum.ENUM_VALUE_2}, {"E4", 10d, SupportEnum.ENUM_VALUE_2}});
            EPAssertionUtil.assertPropsPerRow(env.listener("s0").getNewDataList().get(1), fields,
                new Object[][]{{"E2", 11d, SupportEnum.ENUM_VALUE_1}, {"E7", 11d, SupportEnum.ENUM_VALUE_1}});
            EPAssertionUtil.assertPropsPerRow(env.listener("s0").getNewDataList().get(2), fields,
                new Object[][]{{"E3", 9d, SupportEnum.ENUM_VALUE_2}});
            EPAssertionUtil.assertPropsPerRow(env.listener("s0").getNewDataList().get(3), fields,
                new Object[][]{{"E5", 10d, SupportEnum.ENUM_VALUE_1}, {"E6", 10d, SupportEnum.ENUM_VALUE_1}, {"E8", 10d, SupportEnum.ENUM_VALUE_1}});
            env.undeployAll();

            // test SODA
            EPStatementObjectModel model = env.eplToModel(stmtText);
            assertEquals(stmtText, model.toEPL());
            env.compileDeploy(model).addListener("s0");

            sendEvent(env, "E1", 10d, SupportEnum.ENUM_VALUE_2); // A (1)
            sendEvent(env, "E2", 11d, SupportEnum.ENUM_VALUE_1); // B (2)
            sendEvent(env, "E3", 11d, SupportEnum.ENUM_VALUE_1); // B (2)
            sendTimer(env, 4000);
            assertEquals(2, env.listener("s0").getNewDataList().size());
            EPAssertionUtil.assertPropsPerRow(env.listener("s0").getNewDataList().get(0), fields,
                new Object[][]{{"E1", 10d, SupportEnum.ENUM_VALUE_2}});
            EPAssertionUtil.assertPropsPerRow(env.listener("s0").getNewDataList().get(1), fields,
                new Object[][]{{"E2", 11d, SupportEnum.ENUM_VALUE_1}, {"E3", 11d, SupportEnum.ENUM_VALUE_1}});

            env.undeployAll();
        }
    }

    private static void sendTimer(RegressionEnvironment env, long timeInMSec) {
        env.advanceTime(timeInMSec);
    }

    private static void sendEvent(RegressionEnvironment env, String theString, Double doubleBoxed, SupportEnum enumVal) {
        SupportBean bean = new SupportBean();
        bean.setTheString(theString);
        bean.setDoubleBoxed(doubleBoxed);
        bean.setEnumValue(enumVal);
        env.sendEventBean(bean);
    }

    private static void sendSB(RegressionEnvironment env, String theString, int intPrimitive, long longPrimitive) {
        SupportBean sb = new SupportBean(theString, intPrimitive);
        sb.setLongPrimitive(longPrimitive);
        env.sendEventBean(sb);
    }

    private static void sendManyArray(RegressionEnvironment env, String id, int[] intOne) {
        env.sendEventBean(new SupportEventWithManyArray(id).withIntOne(intOne));
    }
}