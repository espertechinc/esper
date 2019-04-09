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
import com.espertech.esper.common.client.dataflow.core.EPDataFlowInstance;
import com.espertech.esper.common.client.dataflow.core.EPDataFlowInstanceCaptive;
import com.espertech.esper.common.client.dataflow.core.EPDataFlowInstantiationOptions;
import com.espertech.esper.common.client.scopetest.EPAssertionUtil;
import com.espertech.esper.common.internal.epl.dataflow.util.DefaultSupportCaptureOp;
import com.espertech.esper.common.internal.epl.dataflow.util.DefaultSupportGraphOpProviderByOpName;
import com.espertech.esper.common.internal.support.SupportBean;
import com.espertech.esper.common.internal.util.CollectionUtil;
import com.espertech.esper.regressionlib.framework.RegressionEnvironment;
import com.espertech.esper.regressionlib.framework.RegressionExecution;
import com.espertech.esper.regressionlib.support.bean.SupportEventWithLongArray;
import com.espertech.esper.regressionlib.support.bean.SupportObjectArrayOneDim;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Map;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;

public class ViewMultikeyWArray {
    public static Collection<RegressionExecution> executions() {
        ArrayList<RegressionExecution> execs = new ArrayList<>();
        execs.add(new ViewMultiKeyLastUniqueTwoKey());
        execs.add(new ViewMultiKeyFirstUnique());
        execs.add(new ViewMultiKeyGroupWin());
        execs.add(new ViewMultiKeyRank());
        execs.add(new ViewMultiKeyLastUniqueThreeKey());
        execs.add(new ViewMultiKeyLastUniqueOneKeyArrayOfLongPrimitive());
        execs.add(new ViewMultiKeyLastUniqueOneKeyArrayOfObjectArray());
        execs.add(new ViewMultiKeyLastUniqueOneKey2DimArray());
        execs.add(new ViewMultiKeyLastUniqueTwoKeyAllArrayOfPrimitive());
        execs.add(new ViewMultiKeyLastUniqueTwoKeyAllArrayOfObject());
        execs.add(new ViewMultiKeyLastUniqueArrayKeyIntersection());
        execs.add(new ViewMultiKeyLastUniqueArrayKeyUnion());
        execs.add(new ViewMultiKeyLastUniqueArrayKeySubquery());
        execs.add(new ViewMultiKeyLastUniqueArrayKeyNamedWindow());
        execs.add(new ViewMultiKeyLastUniqueArrayKeySubqueryInFilter());
        execs.add(new ViewMultiKeyLastUniqueArrayKeyDataflow());
        return execs;
    }

    private static class ViewMultiKeyRank implements RegressionExecution {
        public void run(RegressionEnvironment env) {
            String epl = "@name('s0') select window(id) as ids from SupportEventWithLongArray#rank(coll, 10, id)";
            env.compileDeploy(epl).addListener("s0");

            sendAssertLongArrayIdWindow(env, "E1", new long[] {1, 2}, "E1");
            sendAssertLongArrayIdWindow(env, "E2", new long[] {1}, "E1,E2");
            sendAssertLongArrayIdWindow(env, "E3", new long[] {}, "E1,E2,E3");
            sendAssertLongArrayIdWindow(env, "E4", null, "E1,E2,E3,E4");

            env.milestone(0);

            sendAssertLongArrayIdWindow(env, "E10", new long[] {1}, "E1,E3,E4,E10");
            sendAssertLongArrayIdWindow(env, "E11", new long[] {}, "E1,E4,E10,E11");
            sendAssertLongArrayIdWindow(env, "E12", new long[] {1, 2}, "E4,E10,E11,E12");
            sendAssertLongArrayIdWindow(env, "E13", null, "E10,E11,E12,E13");

            env.undeployAll();
        }
    }

    private static class ViewMultiKeyGroupWin implements RegressionExecution {
        public void run(RegressionEnvironment env) {
            String epl = "@name('s0') select window(id) as ids from SupportEventWithLongArray#groupwin(coll)#lastevent";
            env.compileDeploy(epl).addListener("s0");

            sendAssertLongArrayIdWindow(env, "E1", new long[] {1, 2}, "E1");
            sendAssertLongArrayIdWindow(env, "E2", new long[] {1}, "E1,E2");
            sendAssertLongArrayIdWindow(env, "E3", new long[] {}, "E1,E2,E3");
            sendAssertLongArrayIdWindow(env, "E4", null, "E1,E2,E3,E4");

            env.milestone(0);

            sendAssertLongArrayIdWindow(env, "E10", new long[] {1}, "E1,E3,E4,E10");
            sendAssertLongArrayIdWindow(env, "E11", new long[] {}, "E1,E4,E10,E11");
            sendAssertLongArrayIdWindow(env, "E12", new long[] {1, 2}, "E4,E10,E11,E12");
            sendAssertLongArrayIdWindow(env, "E13", null, "E10,E11,E12,E13");

            env.undeployAll();
        }
    }

    private static class ViewMultiKeyFirstUnique implements RegressionExecution {
        public void run(RegressionEnvironment env) {
            String epl = "@name('s0') select irstream * from SupportEventWithLongArray#firstunique(coll)";
            env.compileDeploy(epl).addListener("s0");

            sendAssertLongArrayIStream(env, true, "E1", false, 1, 2);
            sendAssertLongArrayIStream(env, true, "E2", false, 1);

            env.milestone(0);

            sendAssertLongArrayIStream(env, false, "E10", false, 1, 2);
            sendAssertLongArrayIStream(env, false, "E11", false, 1);
            sendAssertLongArrayIStream(env, true, "E12", false, 2, 2);
            sendAssertLongArrayIStream(env, true, "E13", true);

            env.milestone(1);

            sendAssertLongArrayIStream(env, false, "E20", true);

            env.undeployAll();
        }
    }

    private static class ViewMultiKeyLastUniqueArrayKeyDataflow implements RegressionExecution {
        public void run(RegressionEnvironment env) {
            if (env.isHA()) {
                return;
            }

            String graph = "@name('flow') create dataflow MySelect\n" +
                "Emitter -> instream_s0<SupportEventWithLongArray>{name: 'emitterS0'}\n" +
                "Select(instream_s0) -> outstream {\n" +
                "  select: (select window(id) as ids from instream_s0#unique(coll))\n" +
                "}\n" +
                "DefaultSupportCaptureOp(outstream) {}\n";
            env.compileDeploy(graph);

            DefaultSupportCaptureOp<Object> capture = new DefaultSupportCaptureOp<>();
            Map<String, Object> operators = CollectionUtil.populateNameValueMap("DefaultSupportCaptureOp", capture);

            EPDataFlowInstantiationOptions options = new EPDataFlowInstantiationOptions().operatorProvider(new DefaultSupportGraphOpProviderByOpName(operators));
            EPDataFlowInstance instance = env.runtime().getDataFlowService().instantiate(env.deploymentId("flow"), "MySelect", options);
            EPDataFlowInstanceCaptive captive = instance.startCaptive();

            assertDataflowIds(captive, "E1", new long[] {1, 2}, capture, "E1");
            assertDataflowIds(captive, "E2", new long[] {1, 2}, capture, "E2");
            assertDataflowIds(captive, "E3", new long[] {1}, capture, "E2,E3");
            assertDataflowIds(captive, "E4", new long[] {1}, capture, "E2,E4");
            assertDataflowIds(captive, "E5", new long[] {1, 2}, capture, "E4,E5");

            instance.cancel();
            env.undeployAll();
        }
    }

    public static class ViewMultiKeyLastUniqueArrayKeySubqueryInFilter implements RegressionExecution {
        public void run(RegressionEnvironment env) {
            String epl = "@name('s0') select * from SupportBean(2 = (select count(*) from SupportEventWithLongArray#unique(coll)))";
            env.compileDeploy(epl).addListener("s0");

            sendSBAssertFilter(env, false);

            env.sendEventBean(new SupportEventWithLongArray("E0", new long[] {1, 2}));
            env.sendEventBean(new SupportEventWithLongArray("E1", new long[] {1}));

            env.milestone(0);

            sendSBAssertFilter(env, true);

            env.sendEventBean(new SupportEventWithLongArray("E2", new long[] {1, 2}));
            env.sendEventBean(new SupportEventWithLongArray("E3", new long[] {1}));

            sendSBAssertFilter(env, true);

            env.sendEventBean(new SupportEventWithLongArray("E4", new long[] {3}));

            sendSBAssertFilter(env, false);

            env.undeployAll();
        }
    }

    public static class ViewMultiKeyLastUniqueArrayKeyNamedWindow implements RegressionExecution {
        public void run(RegressionEnvironment env) {
            String epl = "create window MyWindow#unique(coll) as SupportEventWithLongArray;\n" +
                "insert into MyWindow select * from SupportEventWithLongArray;\n" +
                "@name('s0') select irstream * from MyWindow;\n";
            env.compileDeploy(epl).addListener("s0");

            runAssertionLongArray(env);

            env.undeployAll();
        }
    }

    public static class ViewMultiKeyLastUniqueArrayKeySubquery implements RegressionExecution {
        public void run(RegressionEnvironment env) {
            String epl = "@name('s0') select irstream (select window(id) from SupportEventWithLongArray#unique(coll)) as c0 from SupportBean";
            env.compileDeploy(epl).addListener("s0");

            env.sendEventBean(new SupportEventWithLongArray("E0", new long[] {1, 2}));
            env.sendEventBean(new SupportEventWithLongArray("E1", null));
            env.sendEventBean(new SupportEventWithLongArray("E2", new long[] {1}));
            env.sendEventBean(new SupportEventWithLongArray("E3", new long[] {}));
            env.sendEventBean(new SupportEventWithLongArray("E4", new long[] {1}));

            env.milestone(0);

            env.sendEventBean(new SupportBean());
            EPAssertionUtil.assertEqualsAnyOrder((String[]) env.listener("s0").assertOneGetNewAndReset().get("c0"), "E0,E1,E3,E4".split(","));

            env.sendEventBean(new SupportEventWithLongArray("E10", new long[] {1, 2}));
            env.sendEventBean(new SupportEventWithLongArray("E13", new long[] {}));
            env.sendEventBean(new SupportEventWithLongArray("E14", new long[] {1}));

            env.sendEventBean(new SupportBean());
            EPAssertionUtil.assertEqualsAnyOrder((String[]) env.listener("s0").assertOneGetNewAndReset().get("c0"), "E10,E1,E13,E14".split(","));

            env.undeployAll();
        }
    }

    public static class ViewMultiKeyLastUniqueArrayKeyUnion implements RegressionExecution {
        public void run(RegressionEnvironment env) {
            String epl = "@public @buseventtype create schema EventTwoArrayOfPrimitive as " + EventTwoArrayOfPrimitive.class.getName() + ";\n" +
                "@name('s0') select irstream * from EventTwoArrayOfPrimitive#unique(one)#unique(two) retain-union";
            env.compileDeploy(epl).addListener("s0");

            sendAssertTwoArrayIterate(env, "E0", new int[]{1, 2}, new int[]{3, 4}, "E0");

            env.milestone(0);

            sendAssertTwoArrayIterate(env, "E1", new int[]{1, 2}, new int[]{3, 4}, "E1");
            sendAssertTwoArrayIterate(env, "E2", new int[]{10, 20}, new int[]{30}, "E1,E2");

            env.milestone(1);

            sendAssertTwoArrayIterate(env, "E3", new int[]{1, 2}, new int[]{40}, "E1,E2,E3");
            sendAssertTwoArrayIterate(env, "E4", new int[]{30}, new int[]{30}, "E1,E2,E3,E4");
            sendAssertTwoArrayIterate(env, "E5", new int[]{1, 2}, new int[]{3, 4}, "E2,E3,E4,E5");

            env.undeployAll();
        }
    }

    public static class ViewMultiKeyLastUniqueArrayKeyIntersection implements RegressionExecution {
        public void run(RegressionEnvironment env) {
            String epl = "@public @buseventtype create schema EventTwoArrayOfPrimitive as " + EventTwoArrayOfPrimitive.class.getName() + ";\n" +
                "@name('s0') select irstream * from EventTwoArrayOfPrimitive#unique(one)#unique(two)";
            env.compileDeploy(epl).addListener("s0");

            sendAssertTwoArrayIterate(env, "E0", new int[]{1, 2}, new int[]{3, 4}, "E0");

            env.milestone(0);

            sendAssertTwoArrayIterate(env, "E1", new int[]{1, 2}, new int[]{3, 4}, "E1");
            sendAssertTwoArrayIterate(env, "E2", new int[]{10, 20}, new int[]{30}, "E1,E2");

            env.milestone(1);

            sendAssertTwoArrayIterate(env, "E3", new int[]{1, 2}, new int[]{40}, "E3,E2");
            sendAssertTwoArrayIterate(env, "E4", new int[]{30}, new int[]{30}, "E3,E4");
            sendAssertTwoArrayIterate(env, "E5", new int[]{1, 3}, new int[]{50}, "E3,E4,E5");

            env.undeployAll();
        }
    }

    public static class ViewMultiKeyLastUniqueTwoKeyAllArrayOfPrimitive implements RegressionExecution {

        public void run(RegressionEnvironment env) {
            String epl = "@public @buseventtype create schema EventTwoArrayOfPrimitive as " + EventTwoArrayOfPrimitive.class.getName() + ";\n" +
                "@name('s0') select irstream * from EventTwoArrayOfPrimitive#unique(one, two)";
            env.compileDeploy(epl).addListener("s0");

            EventTwoArrayOfPrimitive b0 = sendAssertTwoArray(env, null, "E0", new int[]{1, 2}, new int[]{3, 4});

            env.milestone(0);

            EventTwoArrayOfPrimitive b1 = sendAssertTwoArray(env, b0, "E1", new int[]{1, 2}, new int[]{3, 4});
            EventTwoArrayOfPrimitive b2 = sendAssertTwoArray(env, null, "E2", new int[]{}, new int[]{3, 4});
            EventTwoArrayOfPrimitive b3 = sendAssertTwoArray(env, null, "E3", new int[]{1, 2}, new int[]{});
            EventTwoArrayOfPrimitive b4 = sendAssertTwoArray(env, null, "E4", new int[]{1}, new int[]{3, 4});

            env.milestone(1);

            sendAssertTwoArray(env, b1, "E20", new int[]{1, 2}, new int[]{3, 4});
            sendAssertTwoArray(env, b3, "E21", new int[]{1, 2}, new int[]{});
            sendAssertTwoArray(env, b2, "E22", new int[]{}, new int[]{3, 4});
            sendAssertTwoArray(env, b4, "E23", new int[]{1}, new int[]{3, 4});

            env.undeployAll();
        }
    }

    public static class ViewMultiKeyLastUniqueTwoKeyAllArrayOfObject implements RegressionExecution {

        public void run(RegressionEnvironment env) {
            String epl = "@public @buseventtype create schema EventTwoArrayOfObject as " + EventTwoArrayOfObject.class.getName() + ";\n" +
                "@name('s0') select irstream * from EventTwoArrayOfObject#unique(one, two)";
            env.compileDeploy(epl).addListener("s0");

            EventTwoArrayOfObject b0 = sendAssertTwoArray(env, null, "E0", new Object[]{1, 2}, new Object[]{new String[]{"a", "b"}});

            env.milestone(0);

            EventTwoArrayOfObject b1 = sendAssertTwoArray(env, b0, "E1", new Object[]{1, 2}, new Object[]{new String[]{"a", "b"}});
            EventTwoArrayOfObject b2 = sendAssertTwoArray(env, null, "E2", new Object[]{0}, new Object[]{new String[]{"a", "b"}});
            EventTwoArrayOfObject b3 = sendAssertTwoArray(env, null, "E3", new Object[]{1, 2}, new Object[]{new String[]{"a"}});
            EventTwoArrayOfObject b4 = sendAssertTwoArray(env, null, "E4", new Object[]{}, new Object[]{new String[]{}});

            env.milestone(1);

            sendAssertTwoArray(env, b1, "E20", new Object[]{1, 2}, new Object[]{new String[]{"a", "b"}});
            sendAssertTwoArray(env, b3, "E21", new Object[]{1, 2}, new Object[]{new String[]{"a"}});
            sendAssertTwoArray(env, b2, "E22", new Object[]{0}, new Object[]{new String[]{"a", "b"}});
            sendAssertTwoArray(env, b4, "E23", new Object[]{}, new Object[]{new String[]{}});

            env.undeployAll();
        }
    }

    public static class ViewMultiKeyLastUniqueOneKey2DimArray implements RegressionExecution {

        public void run(RegressionEnvironment env) {
            String epl = "@public @buseventtype create schema EventTwoDimArray as " + EventTwoDimArray.class.getName() + ";\n" +
                "@name('s0') select irstream * from EventTwoDimArray#unique(array)";
            env.compileDeploy(epl).addListener("s0");

            EventTwoDimArray b0 = sendAssertInt2DimArray(env, null, "E0", new int[][]{{1}, {2}});

            env.milestone(0);

            EventTwoDimArray b1 = sendAssertInt2DimArray(env, b0, "E1", new int[][]{{1}, {2}});
            EventTwoDimArray b2 = sendAssertInt2DimArray(env, null, "E2", new int[][]{{2}, {1}});
            EventTwoDimArray b3 = sendAssertInt2DimArray(env, null, "E3", new int[][]{{1, 2}});
            EventTwoDimArray b4 = sendAssertInt2DimArray(env, null, "E4", new int[][]{{}, {1, 2}});

            env.milestone(1);

            sendAssertInt2DimArray(env, b1, "E20", new int[][]{{1}, {2}});
            sendAssertInt2DimArray(env, b3, "E21", new int[][]{{1, 2}});
            sendAssertInt2DimArray(env, b2, "E22", new int[][]{{2}, {1}});
            sendAssertInt2DimArray(env, b4, "E23", new int[][]{{}, {1, 2}});

            env.undeployAll();
        }
    }

    public static class ViewMultiKeyLastUniqueOneKeyArrayOfObjectArray implements RegressionExecution {

        public void run(RegressionEnvironment env) {
            String epl = "@name('s0') select irstream * from SupportObjectArrayOneDim#unique(arr)";
            env.compileDeploy(epl).addListener("s0");

            SupportObjectArrayOneDim b0 = sendAssertObjectArray(env, null, "E0", false, 1, arrayOf("A", "B"));

            env.milestone(0);

            SupportObjectArrayOneDim b1 = sendAssertObjectArray(env, b0, "E1", false, 1, arrayOf("A", "B"));
            SupportObjectArrayOneDim b2 = sendAssertObjectArray(env, null, "E2", false, 2, arrayOf("A", "B"));
            SupportObjectArrayOneDim b3 = sendAssertObjectArray(env, null, "E3", false, 1, arrayOf("A", "A"));
            SupportObjectArrayOneDim b4 = sendAssertObjectArray(env, null, "E4", false, 1, arrayOf("B", "B"));
            SupportObjectArrayOneDim b5 = sendAssertObjectArray(env, null, "E5", false);
            SupportObjectArrayOneDim b6 = sendAssertObjectArray(env, null, "E6", false, 1);
            SupportObjectArrayOneDim b7 = sendAssertObjectArray(env, null, "E7", false, 1, 2);
            SupportObjectArrayOneDim b8 = sendAssertObjectArray(env, null, "E8", true);

            env.milestone(1);

            sendAssertObjectArray(env, b1, "E20", false, 1, arrayOf("A", "B"));
            sendAssertObjectArray(env, b3, "E21", false, 1, arrayOf("A", "A"));
            sendAssertObjectArray(env, b2, "E22", false, 2, arrayOf("A", "B"));
            sendAssertObjectArray(env, b4, "E23", false, 1, arrayOf("B", "B"));
            sendAssertObjectArray(env, b5, "E24", false);
            sendAssertObjectArray(env, b6, "E25", false, 1);
            sendAssertObjectArray(env, b7, "E26", false, 1, 2);
            sendAssertObjectArray(env, b8, "E27", true);

            env.undeployAll();
        }
    }

    public static class ViewMultiKeyLastUniqueOneKeyArrayOfLongPrimitive implements RegressionExecution {

        public void run(RegressionEnvironment env) {
            String epl = "@name('s0') select irstream * from SupportEventWithLongArray#unique(coll)";
            env.compileDeploy(epl).addListener("s0");

            runAssertionLongArray(env);

            env.undeployAll();
        }
    }

    public static class ViewMultiKeyLastUniqueTwoKey implements RegressionExecution {

        public void run(RegressionEnvironment env) {
            String epl = "@name('s0') select irstream * from SupportBean#unique(intBoxed, longBoxed)";
            env.compileDeploy(epl).addListener("s0");

            SupportBean b0 = sendAssertSB(env, "E0", 1, 10L, null);

            env.milestone(0);

            SupportBean b1 = sendAssertSB(env, "E1", 1, 10L, b0);
            SupportBean b2 = sendAssertSB(env, "E2", 1, 20L, null);
            SupportBean b3 = sendAssertSB(env, "E3", 2, 10L, null);
            SupportBean b4 = sendAssertSB(env, "E4", null, null, null);
            SupportBean b5 = sendAssertSB(env, "E5", 3, null, null);
            SupportBean b6 = sendAssertSB(env, "E6", null, 3L, null);

            env.milestone(1);

            sendAssertSB(env, "E10", 1, 10L, b1);
            sendAssertSB(env, "E11", 2, 10L, b3);
            sendAssertSB(env, "E12", 1, 20L, b2);
            sendAssertSB(env, "E13", null, null, b4);
            sendAssertSB(env, "E14", 3, null, b5);
            sendAssertSB(env, "E15", null, 3L, b6);

            env.undeployAll();
        }
    }

    public static class ViewMultiKeyLastUniqueThreeKey implements RegressionExecution {

        public void run(RegressionEnvironment env) {
            String epl = "@name('s0') select irstream * from SupportBean#unique(intBoxed, longBoxed, doubleBoxed)";
            env.compileDeploy(epl).addListener("s0");

            SupportBean b0 = sendAssertSB(env, "E0", 1, 10L, 100d, null);

            env.milestone(0);

            SupportBean b1 = sendAssertSB(env, "E1", 1, 10L, 100d, b0);
            SupportBean b2 = sendAssertSB(env, "E2", 1, 20L, 30d, null);
            SupportBean b3 = sendAssertSB(env, "E3", 2, 10L, 20d, null);
            SupportBean b4 = sendAssertSB(env, "E4", null, null, null, null);
            SupportBean b5 = sendAssertSB(env, "E5", 3, null, null, null);
            SupportBean b6 = sendAssertSB(env, "E6", null, 3L, null, null);
            SupportBean b7 = sendAssertSB(env, "E6", null, null, 3d, null);

            env.milestone(1);

            sendAssertSB(env, "E10", 1, 10L, 100d, b1);
            sendAssertSB(env, "E11", 2, 10L, 20d, b3);
            sendAssertSB(env, "E12", 1, 20L, 30d, b2);
            sendAssertSB(env, "E13", null, null, null, b4);
            sendAssertSB(env, "E14", 3, null, null, b5);
            sendAssertSB(env, "E15", null, 3L, null, b6);
            sendAssertSB(env, "E15", null, null, 3d, b7);

            env.undeployAll();
        }
    }

    private static SupportEventWithLongArray sendAssertLongArray(RegressionEnvironment env, SupportEventWithLongArray expectedRemove, String id, boolean isNull, long... array) {
        SupportEventWithLongArray event = new SupportEventWithLongArray(id, isNull ? null : array);
        env.sendEventBean(event);
        assertExpectedRemove(env, expectedRemove);
        return event;
    }

    private static SupportEventWithLongArray sendAssertLongArrayIStream(RegressionEnvironment env, boolean expected, String id, boolean isNull, long... array) {
        SupportEventWithLongArray event = new SupportEventWithLongArray(id, isNull ? null : array);
        env.sendEventBean(event);
        assertEquals(expected, env.listener("s0").getAndClearIsInvoked());
        return event;
    }

    private static void sendAssertLongArrayIdWindow(RegressionEnvironment env, String id, long[] array, String expectedCSV) {
        SupportEventWithLongArray event = new SupportEventWithLongArray(id, array);
        env.sendEventBean(event);
        String[] ids = (String[]) env.listener("s0").assertOneGetNewAndReset().get("ids");
        EPAssertionUtil.assertEqualsAnyOrder(expectedCSV.split(","), ids);
    }

    private static SupportObjectArrayOneDim sendAssertObjectArray(RegressionEnvironment env, SupportObjectArrayOneDim expectedRemove, String id, boolean isNull, Object... array) {
        SupportObjectArrayOneDim event = new SupportObjectArrayOneDim(id, isNull ? null : array);
        env.sendEventBean(event);
        assertExpectedRemove(env, expectedRemove);
        return event;
    }

    private static SupportBean sendAssertSB(RegressionEnvironment env, String theString, Integer intBoxed, Long longBoxed, SupportBean expectedRemove) {
        SupportBean sb = new SupportBean(theString, -1);
        sb.setIntBoxed(intBoxed);
        sb.setLongBoxed(longBoxed);
        env.sendEventBean(sb);
        assertExpectedRemove(env, expectedRemove);
        return sb;
    }

    private static SupportBean sendAssertSB(RegressionEnvironment env, String theString, Integer intBoxed, Long longBoxed, Double doubleBoxed, SupportBean expectedRemove) {
        SupportBean sb = new SupportBean(theString, -1);
        sb.setIntBoxed(intBoxed);
        sb.setLongBoxed(longBoxed);
        sb.setDoubleBoxed(doubleBoxed);
        env.sendEventBean(sb);
        assertExpectedRemove(env, expectedRemove);
        return sb;
    }

    private static EventTwoDimArray sendAssertInt2DimArray(RegressionEnvironment env, EventTwoDimArray expectedRemove, String id, int[][] ints) {
        EventTwoDimArray event = new EventTwoDimArray(id, ints);
        env.sendEventBean(event);
        assertExpectedRemove(env, expectedRemove);
        return event;
    }

    private static EventTwoArrayOfPrimitive sendAssertTwoArray(RegressionEnvironment env, EventTwoArrayOfPrimitive expectedRemove, String id, int[] one, int[] two) {
        EventTwoArrayOfPrimitive event = new EventTwoArrayOfPrimitive(id, one, two);
        env.sendEventBean(event);
        assertExpectedRemove(env, expectedRemove);
        return event;
    }

    private static EventTwoArrayOfObject sendAssertTwoArray(RegressionEnvironment env, EventTwoArrayOfObject expectedRemove, String id, Object[] one, Object[] two) {
        EventTwoArrayOfObject event = new EventTwoArrayOfObject(id, one, two);
        env.sendEventBean(event);
        assertExpectedRemove(env, expectedRemove);
        return event;
    }

    private static void sendAssertTwoArrayIterate(RegressionEnvironment env, String id, int[] one, int[] two, String iterateCSV) {
        EventTwoArrayOfPrimitive event = new EventTwoArrayOfPrimitive(id, one, two);
        env.sendEventBean(event);
        Object[] ids = EPAssertionUtil.iteratorToObjectArr(env.iterator("s0"), "id");
        EPAssertionUtil.assertEqualsAnyOrder(iterateCSV.split(","), ids);
    }

    private static void sendSBAssertFilter(RegressionEnvironment env, boolean received) {
        env.sendEventBean(new SupportBean());
        assertEquals(received, env.listener("s0").getAndClearIsInvoked());
    }

    private static void assertExpectedRemove(RegressionEnvironment env, Object expectedRemove) {
        EventBean[] old = env.listener("s0").getLastOldData();
        if (expectedRemove != null) {
            assertEquals(1, old.length);
            assertEquals(expectedRemove, old[0].getUnderlying());
        } else {
            assertNull(old);
        }
    }

    private static void runAssertionLongArray(RegressionEnvironment env) {
        SupportEventWithLongArray b0 = sendAssertLongArray(env, null, "E0", false, 1, 2);

        env.milestone(0);

        SupportEventWithLongArray b1 = sendAssertLongArray(env, b0, "E1", false, 1, 2);
        SupportEventWithLongArray b2 = sendAssertLongArray(env, null, "E2", false, 2, 1);
        SupportEventWithLongArray b3 = sendAssertLongArray(env, null, "E3", false, 2, 2);
        SupportEventWithLongArray b4 = sendAssertLongArray(env, null, "E4", false, 2, 2, 2);
        SupportEventWithLongArray b5 = sendAssertLongArray(env, null, "E5", false);
        SupportEventWithLongArray b6 = sendAssertLongArray(env, null, "E6", false, 1);
        SupportEventWithLongArray b7 = sendAssertLongArray(env, null, "E7", true);

        env.milestone(1);

        sendAssertLongArray(env, b1, "E10", false, 1, 2);
        sendAssertLongArray(env, b3, "E11", false, 2, 2);
        sendAssertLongArray(env, b2, "E12", false, 2, 1);
        sendAssertLongArray(env, b4, "E13", false, 2, 2, 2);
        sendAssertLongArray(env, b5, "E14", false);
        sendAssertLongArray(env, b6, "E15", false, 1);
        sendAssertLongArray(env, b7, "E16", true);
    }

    private static void assertDataflowIds(EPDataFlowInstanceCaptive captive, String id, long[] longs, DefaultSupportCaptureOp<Object> capture, String csv) {
        captive.getEmitters().get("emitterS0").submit(new SupportEventWithLongArray(id, longs));
        String[] received = (String[]) ((Object[]) capture.getCurrentAndReset()[0])[0];
        EPAssertionUtil.assertEqualsAnyOrder(csv.split(","), received);
    }

    private static String[] arrayOf(String... strings) {
        return strings;
    }

    public static class EventTwoDimArray implements Serializable {
        private final String id;
        private final int[][] array;

        public EventTwoDimArray(String id, int[][] array) {
            this.id = id;
            this.array = array;
        }

        public String getId() {
            return id;
        }

        public int[][] getArray() {
            return array;
        }

        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;

            EventTwoDimArray that = (EventTwoDimArray) o;

            if (!id.equals(that.id)) return false;
            return Arrays.deepEquals(array, that.array);
        }

        public int hashCode() {
            int result = id.hashCode();
            result = 31 * result + Arrays.deepHashCode(array);
            return result;
        }
    }

    public static class EventTwoArrayOfPrimitive implements Serializable {
        private final String id;
        private final int[] one;
        private final int[] two;

        public EventTwoArrayOfPrimitive(String id, int[] one, int[] two) {
            this.id = id;
            this.one = one;
            this.two = two;
        }

        public String getId() {
            return id;
        }

        public int[] getOne() {
            return one;
        }

        public int[] getTwo() {
            return two;
        }

        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;

            EventTwoArrayOfPrimitive that = (EventTwoArrayOfPrimitive) o;

            if (!id.equals(that.id)) return false;
            if (!Arrays.equals(one, that.one)) return false;
            return Arrays.equals(two, that.two);
        }

        public int hashCode() {
            int result = id.hashCode();
            result = 31 * result + Arrays.hashCode(one);
            result = 31 * result + Arrays.hashCode(two);
            return result;
        }

        public String toString() {
            return "EventTwoArrayOfPrimitive{" +
                "id='" + id + '\'' +
                ", one=" + Arrays.toString(one) +
                ", two=" + Arrays.toString(two) +
                '}';
        }
    }

    public static class EventTwoArrayOfObject implements Serializable {
        private final String id;
        private final Object[] one;
        private final Object[] two;

        public EventTwoArrayOfObject(String id, Object[] one, Object[] two) {
            this.id = id;
            this.one = one;
            this.two = two;
        }

        public String getId() {
            return id;
        }

        public Object[] getOne() {
            return one;
        }

        public Object[] getTwo() {
            return two;
        }

        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;

            EventTwoArrayOfObject that = (EventTwoArrayOfObject) o;

            if (!id.equals(that.id)) return false;
            // Probably incorrect - comparing Object[] arrays with Arrays.equals
            if (!Arrays.deepEquals(one, that.one)) return false;
            // Probably incorrect - comparing Object[] arrays with Arrays.equals
            return Arrays.deepEquals(two, that.two);
        }

        public int hashCode() {
            int result = id.hashCode();
            result = 31 * result + Arrays.deepHashCode(one);
            result = 31 * result + Arrays.deepHashCode(two);
            return result;
        }

        public String toString() {
            return "EventTwoArrayOfObject{" +
                "id='" + id + '\'' +
                ", one=" + Arrays.toString(one) +
                ", two=" + Arrays.toString(two) +
                '}';
        }
    }
}
