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
package com.espertech.esper.regressionlib.suite.view;

import com.espertech.esper.common.client.EventBean;
import com.espertech.esper.common.client.scopetest.EPAssertionUtil;
import com.espertech.esper.regressionlib.framework.RegressionEnvironment;
import com.espertech.esper.regressionlib.framework.RegressionExecution;
import com.espertech.esper.regressionlib.support.bean.SupportMarketDataBean;

import java.util.ArrayList;
import java.util.Collection;

import static org.junit.Assert.*;

public class ViewTimeLengthBatch {

    public static Collection<RegressionExecution> executions() {
        ArrayList<RegressionExecution> execs = new ArrayList<>();
        execs.add(new ViewTimeLengthBatchSceneOne());
        execs.add(new ViewTimeLengthBatchSceneTwo());
        execs.add(new ViewTimeLengthBatchForceOutputOne());
        execs.add(new ViewTimeLengthBatchForceOutputTwo());
        execs.add(new ViewTimeLengthBatchForceOutputSum());
        execs.add(new ViewTimeLengthBatchStartEager());
        execs.add(new ViewTimeLengthBatchForceOutputStartEagerSum());
        execs.add(new ViewTimeLengthBatchForceOutputStartNoEagerSum());
        execs.add(new ViewTimeLengthBatchPreviousAndPrior());
        execs.add(new ViewTimeLengthBatchGroupBySumStartEager());
        return execs;
    }

    public static class ViewTimeLengthBatchSceneOne implements RegressionExecution {
        public void run(RegressionEnvironment env) {
            sendTimer(env, 1000);

            String text = "@name('s0') select irstream * from SupportMarketDataBean#time_length_batch(10 sec, 3)";
            env.compileDeployAddListenerMileZero(text, "s0");

            sendTimer(env, 1000);
            sendEvent(env, "E1");

            env.milestone(1);

            sendTimer(env, 5000);
            sendEvent(env, "E2");

            env.milestone(2);

            sendTimer(env, 10999);
            assertFalse(env.listener("s0").isInvoked());

            sendTimer(env, 11000);
            EventBean[] newData = env.listener("s0").getLastNewData();
            EPAssertionUtil.assertPropsPerRow(newData, new String[]{"symbol"}, new Object[][]{{"E1"}, {"E2"}});
            env.listener("s0").reset();

            env.milestone(3);

            sendTimer(env, 12000);
            sendEvent(env, "E3");
            sendEvent(env, "E4");

            env.milestone(4);

            sendTimer(env, 15000);
            sendEvent(env, "E5");
            newData = env.listener("s0").getLastNewData();
            EventBean[] oldData = env.listener("s0").getLastOldData();
            EPAssertionUtil.assertPropsPerRow(newData, new String[]{"symbol"}, new Object[][]{{"E3"}, {"E4"}, {"E5"}});
            EPAssertionUtil.assertPropsPerRow(oldData, new String[]{"symbol"}, new Object[][]{{"E1"}, {"E2"}});
            env.listener("s0").reset();

            env.milestone(5);

            sendTimer(env, 24999);
            assertFalse(env.listener("s0").isInvoked());

            // wait 10 second, check call
            sendTimer(env, 25000);
            newData = env.listener("s0").getLastNewData();
            oldData = env.listener("s0").getLastOldData();
            assertNull(newData);
            EPAssertionUtil.assertPropsPerRow(oldData, new String[]{"symbol"}, new Object[][]{{"E3"}, {"E4"}, {"E5"}});
            env.listener("s0").reset();

            // wait 10 second, check no call received, no events
            sendTimer(env, 35000);
            assertFalse(env.listener("s0").isInvoked());

            env.undeployAll();
        }
    }

    private static class ViewTimeLengthBatchSceneTwo implements RegressionExecution {
        public void run(RegressionEnvironment env) {
            final long startTime = 1000;
            SupportMarketDataBean[] events = get100Events();

            sendTimer(env, startTime);
            String epl = "@name('s0') select irstream * from SupportMarketDataBean#time_length_batch(10 sec, 3)";
            env.compileDeployAddListenerMileZero(epl, "s0");

            // Send 3 events in batch
            env.sendEventBean(events[0]);
            assertFalse(env.listener("s0").isInvoked());

            env.sendEventBean(events[1]);
            assertFalse(env.listener("s0").isInvoked());

            env.sendEventBean(events[2]);
            assertEquals(1, env.listener("s0").getNewDataList().size());
            EPAssertionUtil.assertEqualsExactOrderUnderlying(new Object[]{events[0], events[1], events[2]}, env.listener("s0").getNewDataListFlattened());
            env.listener("s0").reset();

            // Send another 3 events in batch
            env.sendEventBean(events[3]);
            env.sendEventBean(events[4]);
            assertFalse(env.listener("s0").isInvoked());

            env.sendEventBean(events[5]);
            assertEquals(1, env.listener("s0").getNewDataList().size());
            assertEquals(1, env.listener("s0").getOldDataList().size());
            EPAssertionUtil.assertEqualsExactOrderUnderlying(new Object[]{events[0], events[1], events[2]}, env.listener("s0").getOldDataListFlattened());
            EPAssertionUtil.assertEqualsExactOrderUnderlying(new Object[]{events[3], events[4], events[5]}, env.listener("s0").getNewDataListFlattened());
            env.listener("s0").reset();

            // Expire the last 3 events by moving time
            sendTimer(env, startTime + 9999);
            assertFalse(env.listener("s0").isInvoked());

            sendTimer(env, startTime + 10000);
            assertEquals(1, env.listener("s0").getNewDataList().size());
            assertEquals(1, env.listener("s0").getOldDataList().size());
            EPAssertionUtil.assertEqualsExactOrderUnderlying(new Object[]{events[3], events[4], events[5]}, env.listener("s0").getOldDataListFlattened());
            EPAssertionUtil.assertEqualsExactOrderUnderlying(new Object[]{}, env.listener("s0").getNewDataListFlattened());
            env.listener("s0").reset();

            sendTimer(env, startTime + 10001);
            assertFalse(env.listener("s0").isInvoked());

            // Send an event, let the timer send the batch
            sendTimer(env, startTime + 10100);
            env.sendEventBean(events[6]);
            assertFalse(env.listener("s0").isInvoked());

            sendTimer(env, startTime + 19999);
            assertFalse(env.listener("s0").isInvoked());

            sendTimer(env, startTime + 20000);
            assertEquals(1, env.listener("s0").getNewDataList().size());
            assertEquals(1, env.listener("s0").getOldDataList().size());
            EPAssertionUtil.assertEqualsExactOrderUnderlying(new Object[]{}, env.listener("s0").getOldDataListFlattened());
            EPAssertionUtil.assertEqualsExactOrderUnderlying(new Object[]{events[6]}, env.listener("s0").getNewDataListFlattened());
            env.listener("s0").reset();

            sendTimer(env, startTime + 20001);
            assertFalse(env.listener("s0").isInvoked());

            // Send two events, let the timer send the batch
            sendTimer(env, startTime + 29998);
            env.sendEventBean(events[7]);
            env.sendEventBean(events[8]);
            assertFalse(env.listener("s0").isInvoked());

            sendTimer(env, startTime + 29999);
            assertFalse(env.listener("s0").isInvoked());

            sendTimer(env, startTime + 30000);
            assertEquals(1, env.listener("s0").getNewDataList().size());
            assertEquals(1, env.listener("s0").getOldDataList().size());
            EPAssertionUtil.assertEqualsExactOrderUnderlying(new Object[]{events[6]}, env.listener("s0").getOldDataListFlattened());
            EPAssertionUtil.assertEqualsExactOrderUnderlying(new Object[]{events[7], events[8]}, env.listener("s0").getNewDataListFlattened());
            env.listener("s0").reset();

            // Send three events, the the 3 events batch
            sendTimer(env, startTime + 30001);
            assertFalse(env.listener("s0").isInvoked());

            env.sendEventBean(events[9]);
            env.sendEventBean(events[10]);
            assertFalse(env.listener("s0").isInvoked());

            sendTimer(env, startTime + 39000);
            assertFalse(env.listener("s0").isInvoked());

            env.sendEventBean(events[11]);
            assertEquals(1, env.listener("s0").getNewDataList().size());
            assertEquals(1, env.listener("s0").getOldDataList().size());
            EPAssertionUtil.assertEqualsExactOrderUnderlying(new Object[]{events[7], events[8]}, env.listener("s0").getOldDataListFlattened());
            EPAssertionUtil.assertEqualsExactOrderUnderlying(new Object[]{events[9], events[10], events[11]}, env.listener("s0").getNewDataListFlattened());
            env.listener("s0").reset();

            // Send 1 event, let the timer to do the batch
            sendTimer(env, startTime + 39000 + 9999);
            assertFalse(env.listener("s0").isInvoked());

            env.sendEventBean(events[12]);
            assertFalse(env.listener("s0").isInvoked());

            sendTimer(env, startTime + 39000 + 10000);
            assertEquals(1, env.listener("s0").getNewDataList().size());
            assertEquals(1, env.listener("s0").getOldDataList().size());
            EPAssertionUtil.assertEqualsExactOrderUnderlying(new Object[]{events[9], events[10], events[11]}, env.listener("s0").getOldDataListFlattened());
            EPAssertionUtil.assertEqualsExactOrderUnderlying(new Object[]{events[12]}, env.listener("s0").getNewDataListFlattened());
            env.listener("s0").reset();

            sendTimer(env, startTime + 39000 + 10001);
            assertFalse(env.listener("s0").isInvoked());

            // Send no events, let the timer to do the batch
            sendTimer(env, startTime + 39000 + 19999);
            assertFalse(env.listener("s0").isInvoked());

            sendTimer(env, startTime + 39000 + 20000);
            assertEquals(1, env.listener("s0").getNewDataList().size());
            assertEquals(1, env.listener("s0").getOldDataList().size());
            EPAssertionUtil.assertEqualsExactOrderUnderlying(new Object[]{events[12]}, env.listener("s0").getOldDataListFlattened());
            EPAssertionUtil.assertEqualsExactOrderUnderlying(new Object[]{}, env.listener("s0").getNewDataListFlattened());
            env.listener("s0").reset();

            sendTimer(env, startTime + 39000 + 20001);
            assertFalse(env.listener("s0").isInvoked());

            // Send no events, let the timer to do NO batch
            sendTimer(env, startTime + 39000 + 29999);
            assertFalse(env.listener("s0").isInvoked());

            sendTimer(env, startTime + 39000 + 30000);
            assertFalse(env.listener("s0").isInvoked());

            sendTimer(env, startTime + 39000 + 30001);
            assertFalse(env.listener("s0").isInvoked());

            // Send 1 more event
            sendTimer(env, startTime + 90000);
            assertFalse(env.listener("s0").isInvoked());

            env.sendEventBean(events[13]);
            assertFalse(env.listener("s0").isInvoked());

            sendTimer(env, startTime + 99999);
            assertFalse(env.listener("s0").isInvoked());

            sendTimer(env, startTime + 100000);
            assertEquals(1, env.listener("s0").getNewDataList().size());
            assertEquals(1, env.listener("s0").getOldDataList().size());
            EPAssertionUtil.assertEqualsExactOrderUnderlying(new Object[]{}, env.listener("s0").getOldDataListFlattened());
            EPAssertionUtil.assertEqualsExactOrderUnderlying(new Object[]{events[13]}, env.listener("s0").getNewDataListFlattened());
            env.listener("s0").reset();

            env.undeployAll();
        }
    }

    public static class ViewTimeLengthBatchForceOutputOne implements RegressionExecution {
        public void run(RegressionEnvironment env) {
            sendTimer(env, 1000);

            String text = "@name('s0') select irstream * from SupportMarketDataBean#time_length_batch(10 sec, 3, 'force_update')";
            env.compileDeployAddListenerMileZero(text, "s0");

            sendTimer(env, 1000);
            sendEvent(env, "E1");

            env.milestone(1);

            sendTimer(env, 5000);
            sendEvent(env, "E2");

            env.milestone(2);

            sendTimer(env, 10999);
            assertFalse(env.listener("s0").isInvoked());

            sendTimer(env, 11000);
            EventBean[] newData = env.listener("s0").getLastNewData();
            EPAssertionUtil.assertPropsPerRow(newData, new String[]{"symbol"}, new Object[][]{{"E1"}, {"E2"}});
            env.listener("s0").reset();

            env.milestone(3);

            sendTimer(env, 12000);
            sendEvent(env, "E3");
            sendEvent(env, "E4");

            env.milestone(4);

            sendTimer(env, 15000);
            sendEvent(env, "E5");
            newData = env.listener("s0").getLastNewData();
            EventBean[] oldData = env.listener("s0").getLastOldData();
            EPAssertionUtil.assertPropsPerRow(newData, new String[]{"symbol"}, new Object[][]{{"E3"}, {"E4"}, {"E5"}});
            EPAssertionUtil.assertPropsPerRow(oldData, new String[]{"symbol"}, new Object[][]{{"E1"}, {"E2"}});
            env.listener("s0").reset();

            env.milestone(5);

            sendTimer(env, 24999);
            assertFalse(env.listener("s0").isInvoked());

            // wait 10 second, check call
            sendTimer(env, 25000);
            newData = env.listener("s0").getLastNewData();
            oldData = env.listener("s0").getLastOldData();
            assertNull(newData);
            EPAssertionUtil.assertPropsPerRow(oldData, new String[]{"symbol"}, new Object[][]{{"E3"}, {"E4"}, {"E5"}});
            env.listener("s0").reset();

            env.milestone(6);

            // wait 10 second, check call, should receive event
            sendTimer(env, 35000);
            assertTrue(env.listener("s0").isInvoked());
            assertNull(env.listener("s0").getLastNewData());
            assertNull(env.listener("s0").getLastOldData());

            env.undeployAll();
        }
    }

    private static class ViewTimeLengthBatchForceOutputTwo implements RegressionExecution {
        public void run(RegressionEnvironment env) {
            final long startTime = 1000;
            SupportMarketDataBean[] events = get100Events();
            sendTimer(env, startTime);

            String epl = "@name('s0') select irstream * from SupportMarketDataBean#time_length_batch(10 sec, 3, 'FORCE_UPDATE')";
            env.compileDeployAddListenerMileZero(epl, "s0");

            // Send 3 events in batch
            env.sendEventBean(events[0]);
            assertFalse(env.listener("s0").isInvoked());

            env.sendEventBean(events[1]);
            assertFalse(env.listener("s0").isInvoked());

            env.sendEventBean(events[2]);
            assertEquals(1, env.listener("s0").getNewDataList().size());
            EPAssertionUtil.assertEqualsExactOrderUnderlying(new Object[]{events[0], events[1], events[2]}, env.listener("s0").getNewDataListFlattened());
            env.listener("s0").reset();

            // Send another 3 events in batch
            env.sendEventBean(events[3]);
            env.sendEventBean(events[4]);
            assertFalse(env.listener("s0").isInvoked());

            env.sendEventBean(events[5]);
            assertEquals(1, env.listener("s0").getNewDataList().size());
            assertEquals(1, env.listener("s0").getOldDataList().size());
            EPAssertionUtil.assertEqualsExactOrderUnderlying(new Object[]{events[0], events[1], events[2]}, env.listener("s0").getOldDataListFlattened());
            EPAssertionUtil.assertEqualsExactOrderUnderlying(new Object[]{events[3], events[4], events[5]}, env.listener("s0").getNewDataListFlattened());
            env.listener("s0").reset();

            // Expire the last 3 events by moving time
            sendTimer(env, startTime + 9999);
            assertFalse(env.listener("s0").isInvoked());

            sendTimer(env, startTime + 10000);
            assertEquals(1, env.listener("s0").getNewDataList().size());
            assertEquals(1, env.listener("s0").getOldDataList().size());
            EPAssertionUtil.assertEqualsExactOrderUnderlying(new Object[]{events[3], events[4], events[5]}, env.listener("s0").getOldDataListFlattened());
            EPAssertionUtil.assertEqualsExactOrderUnderlying(new Object[]{}, env.listener("s0").getNewDataListFlattened());
            env.listener("s0").reset();

            sendTimer(env, startTime + 10001);
            assertFalse(env.listener("s0").isInvoked());

            // Send an event, let the timer send the batch
            sendTimer(env, startTime + 10100);
            env.sendEventBean(events[6]);
            assertFalse(env.listener("s0").isInvoked());

            sendTimer(env, startTime + 19999);
            assertFalse(env.listener("s0").isInvoked());

            sendTimer(env, startTime + 20000);
            assertEquals(1, env.listener("s0").getNewDataList().size());
            assertEquals(1, env.listener("s0").getOldDataList().size());
            EPAssertionUtil.assertEqualsExactOrderUnderlying(new Object[]{}, env.listener("s0").getOldDataListFlattened());
            EPAssertionUtil.assertEqualsExactOrderUnderlying(new Object[]{events[6]}, env.listener("s0").getNewDataListFlattened());
            env.listener("s0").reset();

            sendTimer(env, startTime + 20001);
            assertFalse(env.listener("s0").isInvoked());

            // Send two events, let the timer send the batch
            sendTimer(env, startTime + 29998);
            env.sendEventBean(events[7]);
            env.sendEventBean(events[8]);
            assertFalse(env.listener("s0").isInvoked());

            sendTimer(env, startTime + 29999);
            assertFalse(env.listener("s0").isInvoked());

            sendTimer(env, startTime + 30000);
            assertEquals(1, env.listener("s0").getNewDataList().size());
            assertEquals(1, env.listener("s0").getOldDataList().size());
            EPAssertionUtil.assertEqualsExactOrderUnderlying(new Object[]{events[6]}, env.listener("s0").getOldDataListFlattened());
            EPAssertionUtil.assertEqualsExactOrderUnderlying(new Object[]{events[7], events[8]}, env.listener("s0").getNewDataListFlattened());
            env.listener("s0").reset();

            // Send three events, the the 3 events batch
            sendTimer(env, startTime + 30001);
            assertFalse(env.listener("s0").isInvoked());

            env.sendEventBean(events[9]);
            env.sendEventBean(events[10]);
            assertFalse(env.listener("s0").isInvoked());

            sendTimer(env, startTime + 39000);
            assertFalse(env.listener("s0").isInvoked());

            env.sendEventBean(events[11]);
            assertEquals(1, env.listener("s0").getNewDataList().size());
            assertEquals(1, env.listener("s0").getOldDataList().size());
            EPAssertionUtil.assertEqualsExactOrderUnderlying(new Object[]{events[7], events[8]}, env.listener("s0").getOldDataListFlattened());
            EPAssertionUtil.assertEqualsExactOrderUnderlying(new Object[]{events[9], events[10], events[11]}, env.listener("s0").getNewDataListFlattened());
            env.listener("s0").reset();

            // Send 1 event, let the timer to do the batch
            sendTimer(env, startTime + 39000 + 9999);
            assertFalse(env.listener("s0").isInvoked());

            env.sendEventBean(events[12]);
            assertFalse(env.listener("s0").isInvoked());

            sendTimer(env, startTime + 39000 + 10000);
            assertEquals(1, env.listener("s0").getNewDataList().size());
            assertEquals(1, env.listener("s0").getOldDataList().size());
            EPAssertionUtil.assertEqualsExactOrderUnderlying(new Object[]{events[9], events[10], events[11]}, env.listener("s0").getOldDataListFlattened());
            EPAssertionUtil.assertEqualsExactOrderUnderlying(new Object[]{events[12]}, env.listener("s0").getNewDataListFlattened());
            env.listener("s0").reset();

            sendTimer(env, startTime + 39000 + 10001);
            assertFalse(env.listener("s0").isInvoked());

            // Send no events, let the timer to do the batch
            sendTimer(env, startTime + 39000 + 19999);
            assertFalse(env.listener("s0").isInvoked());

            sendTimer(env, startTime + 39000 + 20000);
            assertEquals(1, env.listener("s0").getNewDataList().size());
            assertEquals(1, env.listener("s0").getOldDataList().size());
            EPAssertionUtil.assertEqualsExactOrderUnderlying(new Object[]{events[12]}, env.listener("s0").getOldDataListFlattened());
            EPAssertionUtil.assertEqualsExactOrderUnderlying(new Object[]{}, env.listener("s0").getNewDataListFlattened());
            env.listener("s0").reset();

            sendTimer(env, startTime + 39000 + 20001);
            assertFalse(env.listener("s0").isInvoked());

            // Send no events, let the timer do a batch
            sendTimer(env, startTime + 39000 + 29999);
            assertFalse(env.listener("s0").isInvoked());

            sendTimer(env, startTime + 39000 + 30000);
            assertEquals(1, env.listener("s0").getNewDataList().size());
            assertEquals(1, env.listener("s0").getOldDataList().size());
            EPAssertionUtil.assertEqualsExactOrderUnderlying(new Object[]{}, env.listener("s0").getOldDataListFlattened());
            EPAssertionUtil.assertEqualsExactOrderUnderlying(new Object[]{}, env.listener("s0").getNewDataListFlattened());
            env.listener("s0").reset();

            sendTimer(env, startTime + 39000 + 30001);
            assertFalse(env.listener("s0").isInvoked());

            // Send no events, let the timer do a batch
            sendTimer(env, startTime + 39000 + 39999);
            assertFalse(env.listener("s0").isInvoked());

            sendTimer(env, startTime + 39000 + 40000);
            EPAssertionUtil.assertEqualsExactOrderUnderlying(new Object[]{}, env.listener("s0").getOldDataListFlattened());
            EPAssertionUtil.assertEqualsExactOrderUnderlying(new Object[]{}, env.listener("s0").getNewDataListFlattened());
            env.listener("s0").reset();

            sendTimer(env, startTime + 39000 + 40001);
            assertFalse(env.listener("s0").isInvoked());

            // Send 1 more event
            sendTimer(env, startTime + 80000);
            assertFalse(env.listener("s0").isInvoked());

            env.sendEventBean(events[13]);
            assertFalse(env.listener("s0").isInvoked());

            sendTimer(env, startTime + 88999);   // 10 sec from last batch
            assertFalse(env.listener("s0").isInvoked());

            sendTimer(env, startTime + 89000);
            EPAssertionUtil.assertEqualsExactOrderUnderlying(new Object[]{}, env.listener("s0").getOldDataListFlattened());
            EPAssertionUtil.assertEqualsExactOrderUnderlying(new Object[]{events[13]}, env.listener("s0").getNewDataListFlattened());
            env.listener("s0").reset();

            // Send 3 more events
            sendTimer(env, startTime + 90000);
            env.sendEventBean(events[14]);
            env.sendEventBean(events[15]);
            assertFalse(env.listener("s0").isInvoked());

            sendTimer(env, startTime + 92000);
            env.sendEventBean(events[16]);
            EPAssertionUtil.assertEqualsExactOrderUnderlying(new Object[]{events[13]}, env.listener("s0").getOldDataListFlattened());
            EPAssertionUtil.assertEqualsExactOrderUnderlying(new Object[]{events[14], events[15], events[16]}, env.listener("s0").getNewDataListFlattened());
            env.listener("s0").reset();

            // Send no events, let the timer do a batch
            sendTimer(env, startTime + 101999);
            assertFalse(env.listener("s0").isInvoked());

            sendTimer(env, startTime + 102000);
            EPAssertionUtil.assertEqualsExactOrderUnderlying(new Object[]{events[14], events[15], events[16]}, env.listener("s0").getOldDataListFlattened());
            EPAssertionUtil.assertEqualsExactOrderUnderlying(new Object[]{}, env.listener("s0").getNewDataListFlattened());
            env.listener("s0").reset();

            env.undeployAll();
        }
    }

    private static class ViewTimeLengthBatchForceOutputSum implements RegressionExecution {
        public void run(RegressionEnvironment env) {
            final long startTime = 1000;
            sendTimer(env, startTime);
            SupportMarketDataBean[] events = get100Events();

            String epl = "@name('s0') select sum(price) from SupportMarketDataBean#time_length_batch(10 sec, 3, 'FORCE_UPDATE')";
            env.compileDeployAddListenerMileZero(epl, "s0");

            // Send 1 events in batch
            env.sendEventBean(events[10]);
            assertFalse(env.listener("s0").isInvoked());

            sendTimer(env, startTime + 10000);
            assertEquals(10.0, env.listener("s0").getLastNewData()[0].get("sum(price)"));
            env.listener("s0").reset();

            sendTimer(env, startTime + 20000);
            assertEquals(null, env.listener("s0").getLastNewData()[0].get("sum(price)"));
            env.listener("s0").reset();

            sendTimer(env, startTime + 30000);
            assertEquals(null, env.listener("s0").getLastNewData()[0].get("sum(price)"));
            env.listener("s0").reset();

            sendTimer(env, startTime + 40000);
            assertEquals(null, env.listener("s0").getLastNewData()[0].get("sum(price)"));
            env.listener("s0").reset();

            env.undeployAll();
        }
    }

    public static class ViewTimeLengthBatchStartEager implements RegressionExecution {
        public void run(RegressionEnvironment env) {
            sendTimer(env, 1000);

            String text = "@name('s0') select irstream * from SupportMarketDataBean#time_length_batch(10 sec, 3, 'start_eager')";
            env.compileDeployAddListenerMileZero(text, "s0");

            sendTimer(env, 10999);
            assertFalse(env.listener("s0").isInvoked());

            sendTimer(env, 11000);
            assertTrue(env.listener("s0").isInvoked());
            assertNull(env.listener("s0").getLastNewData());
            assertNull(env.listener("s0").getLastOldData());
            env.listener("s0").reset();

            env.milestone(1);

            // Time period without events
            sendTimer(env, 20999);
            assertFalse(env.listener("s0").isInvoked());

            sendTimer(env, 21000);
            assertTrue(env.listener("s0").isInvoked());
            assertNull(env.listener("s0").getLastNewData());
            assertNull(env.listener("s0").getLastOldData());
            env.listener("s0").reset();

            // 3 events in batch
            sendTimer(env, 22000);
            sendEvent(env, "E1");
            sendEvent(env, "E2");

            env.milestone(2);

            sendTimer(env, 25000);
            sendEvent(env, "E3");
            EventBean[] newData = env.listener("s0").getLastNewData();
            assertNull(env.listener("s0").getLastOldData());
            EPAssertionUtil.assertPropsPerRow(newData, new String[]{"symbol"}, new Object[][]{{"E1"}, {"E2"}, {"E3"}});
            env.listener("s0").reset();

            env.milestone(3);

            // Time period without events
            sendTimer(env, 34999);
            assertFalse(env.listener("s0").isInvoked());

            sendTimer(env, 35000);
            EventBean[] oldData = env.listener("s0").getLastOldData();
            EPAssertionUtil.assertPropsPerRow(oldData, new String[]{"symbol"}, new Object[][]{{"E1"}, {"E2"}, {"E3"}});
            assertNull(env.listener("s0").getLastNewData());
            env.listener("s0").reset();

            env.milestone(4);

            // 1 events in time period
            sendTimer(env, 44999);
            sendEvent(env, "E4");

            env.milestone(5);

            sendTimer(env, 45000);
            newData = env.listener("s0").getLastNewData();
            assertNull(env.listener("s0").getLastOldData());
            EPAssertionUtil.assertPropsPerRow(newData, new String[]{"symbol"}, new Object[][]{{"E4"}});
            env.listener("s0").reset();

            env.undeployAll();
        }
    }

    private static class ViewTimeLengthBatchForceOutputStartEagerSum implements RegressionExecution {
        public void run(RegressionEnvironment env) {
            final long startTime = 1000;
            sendTimer(env, startTime);
            SupportMarketDataBean[] events = get100Events();

            String epl = "@name('s0') select sum(price) from SupportMarketDataBean#time_length_batch(10 sec, 3, 'force_update, start_eager')";
            env.compileDeployAddListenerMileZero(epl, "s0");
            assertFalse(env.listener("s0").isInvoked());

            sendTimer(env, startTime + 9999);
            assertFalse(env.listener("s0").isInvoked());

            // Send batch off
            sendTimer(env, startTime + 10000);
            assertEquals(null, env.listener("s0").getLastNewData()[0].get("sum(price)"));
            env.listener("s0").reset();

            // Send batch off
            sendTimer(env, startTime + 20000);
            assertEquals(null, env.listener("s0").getLastNewData()[0].get("sum(price)"));
            env.listener("s0").reset();

            env.sendEventBean(events[11]);
            env.sendEventBean(events[12]);
            sendTimer(env, startTime + 30000);
            assertEquals(23.0, env.listener("s0").getLastNewData()[0].get("sum(price)"));
            env.listener("s0").reset();

            env.undeployAll();
        }
    }

    private static class ViewTimeLengthBatchForceOutputStartNoEagerSum implements RegressionExecution {
        public void run(RegressionEnvironment env) {
            final long startTime = 1000;
            sendTimer(env, startTime);

            String epl = "@name('s0') select sum(price) from SupportMarketDataBean#time_length_batch(10 sec, 3, 'force_update')";
            env.compileDeployAddListenerMileZero(epl, "s0");

            // No batch as we are not start eager
            sendTimer(env, startTime + 10000);
            assertFalse(env.listener("s0").isInvoked());

            // No batch as we are not start eager
            sendTimer(env, startTime + 20000);
            assertFalse(env.listener("s0").isInvoked());

            env.undeployAll();
        }
    }

    private static class ViewTimeLengthBatchPreviousAndPrior implements RegressionExecution {
        public void run(RegressionEnvironment env) {
            final long startTime = 1000;
            sendTimer(env, startTime);
            SupportMarketDataBean[] premades = get100Events();

            String epl = "@name('s0') select price, prev(1, price) as prevPrice, prior(1, price) as priorPrice from SupportMarketDataBean#time_length_batch(10 sec, 3)";
            env.compileDeployAddListenerMileZero(epl, "s0");

            // Send 3 events in batch
            env.sendEventBean(premades[0]);
            env.sendEventBean(premades[1]);
            assertFalse(env.listener("s0").isInvoked());

            env.sendEventBean(premades[2]);
            assertEquals(1, env.listener("s0").getNewDataList().size());
            EventBean[] events = env.listener("s0").getLastNewData();
            assertData(events[0], 0, null, null);
            assertData(events[1], 1.0, 0.0, 0.0);
            assertData(events[2], 2.0, 1.0, 1.0);
            env.listener("s0").reset();

            env.undeployAll();
        }
    }

    private static class ViewTimeLengthBatchGroupBySumStartEager implements RegressionExecution {
        public void run(RegressionEnvironment env) {
            final long startTime = 1000;
            sendTimer(env, startTime);

            String epl = "@name('s0') select symbol, sum(price) as s from SupportMarketDataBean#time_length_batch(5, 10, \"START_EAGER\") group by symbol order by symbol asc";
            env.compileDeployAddListenerMileZero(epl, "s0");

            sendTimer(env, startTime + 4000);
            assertFalse(env.listener("s0").isInvoked());

            sendTimer(env, startTime + 6000);
            assertEquals(1, env.listener("s0").getNewDataList().size());
            EventBean[] events = env.listener("s0").getLastNewData();
            assertNull(events);
            env.listener("s0").reset();

            sendTimer(env, startTime + 7000);
            env.sendEventBean(new SupportMarketDataBean("S1", "e1", 10d));

            sendTimer(env, startTime + 8000);
            env.sendEventBean(new SupportMarketDataBean("S2", "e2", 77d));

            sendTimer(env, startTime + 9000);
            env.sendEventBean(new SupportMarketDataBean("S1", "e3", 1d));

            sendTimer(env, startTime + 10000);
            assertFalse(env.listener("s0").isInvoked());

            sendTimer(env, startTime + 11000);
            assertEquals(1, env.listener("s0").getNewDataList().size());
            events = env.listener("s0").getLastNewData();
            assertEquals(2, events.length);
            assertEquals("S1", events[0].get("symbol"));
            assertEquals(11d, events[0].get("s"));
            assertEquals("S2", events[1].get("symbol"));
            assertEquals(77d, events[1].get("s"));
            env.listener("s0").reset();

            env.undeployAll();
        }
    }

    private static void sendTimer(RegressionEnvironment env, long timeInMSec) {
        env.advanceTime(timeInMSec);
    }

    private static void assertData(EventBean theEvent, double price, Double prevPrice, Double priorPrice) {
        assertEquals(price, theEvent.get("price"));
        assertEquals(prevPrice, theEvent.get("prevPrice"));
        assertEquals(priorPrice, theEvent.get("priorPrice"));
    }

    private static SupportMarketDataBean[] get100Events() {
        SupportMarketDataBean[] events = new SupportMarketDataBean[100];
        for (int i = 0; i < events.length; i++) {
            events[i] = new SupportMarketDataBean("S" + Integer.toString(i), "id_" + Integer.toString(i), i);
        }
        return events;
    }

    private static SupportMarketDataBean sendEvent(RegressionEnvironment env, String symbol) {
        SupportMarketDataBean bean = new SupportMarketDataBean(symbol, 0, 0L, null);
        env.sendEventBean(bean);
        return bean;
    }
}
