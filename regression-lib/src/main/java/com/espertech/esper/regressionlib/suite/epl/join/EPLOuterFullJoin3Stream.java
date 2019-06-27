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
package com.espertech.esper.regressionlib.suite.epl.join;

import com.espertech.esper.common.client.EventBean;
import com.espertech.esper.common.client.scopetest.EPAssertionUtil;
import com.espertech.esper.common.internal.support.EventRepresentationChoice;
import com.espertech.esper.common.internal.support.SupportBean_S0;
import com.espertech.esper.common.internal.support.SupportBean_S1;
import com.espertech.esper.common.internal.support.SupportBean_S2;
import com.espertech.esper.regressionlib.framework.RegressionEnvironment;
import com.espertech.esper.regressionlib.framework.RegressionExecution;
import com.espertech.esper.regressionlib.support.util.ArrayHandlingUtil;

import java.util.ArrayList;
import java.util.List;

public class EPLOuterFullJoin3Stream {
    private final static String[] FIELDS = new String[]{"s0.p00", "s0.p01", "s1.p10", "s1.p11", "s2.p20", "s2.p21"};

    public static List<RegressionExecution> executions() {
        List<RegressionExecution> execs = new ArrayList<>();
        execs.add(new EPLJoinFullJoin2SidesMulticolumn());
        execs.add(new EPLJoinFullJoin2Sides());
        return execs;
    }

    private static class EPLJoinFullJoin2SidesMulticolumn implements RegressionExecution {
        public void run(RegressionEnvironment env) {
            tryAssertionFullJoin_2sides_multicolumn(env, EventRepresentationChoice.OBJECTARRAY);
            tryAssertionFullJoin_2sides_multicolumn(env, EventRepresentationChoice.MAP);
            tryAssertionFullJoin_2sides_multicolumn(env, EventRepresentationChoice.DEFAULT);
        }

        private static void tryAssertionFullJoin_2sides_multicolumn(RegressionEnvironment env, EventRepresentationChoice eventRepresentationEnum) {
            String[] fields = "s0.id, s0.p00, s0.p01, s1.id, s1.p10, s1.p11, s2.id, s2.p20, s2.p21".split(",");

            String epl = eventRepresentationEnum.getAnnotationTextWJsonProvided(MyLocalJsonProvided.class) + " @name('s0') select * from " +
                "SupportBean_S0#length(1000) as s0 " +
                " full outer join SupportBean_S1#length(1000) as s1 on s0.p00 = s1.p10 and s0.p01 = s1.p11" +
                " full outer join SupportBean_S2#length(1000) as s2 on s0.p00 = s2.p20 and s0.p01 = s2.p21";
            env.compileDeployAddListenerMileZero(epl, "s0");

            env.sendEventBean(new SupportBean_S1(10, "A_1", "B_1"));
            EPAssertionUtil.assertProps(env.listener("s0").assertOneGetNewAndReset(), fields, new Object[]{null, null, null, 10, "A_1", "B_1", null, null, null});

            env.sendEventBean(new SupportBean_S1(11, "A_2", "B_1"));
            EPAssertionUtil.assertProps(env.listener("s0").assertOneGetNewAndReset(), fields, new Object[]{null, null, null, 11, "A_2", "B_1", null, null, null});

            env.sendEventBean(new SupportBean_S1(12, "A_1", "B_2"));
            EPAssertionUtil.assertProps(env.listener("s0").assertOneGetNewAndReset(), fields, new Object[]{null, null, null, 12, "A_1", "B_2", null, null, null});

            env.sendEventBean(new SupportBean_S1(13, "A_2", "B_2"));
            EPAssertionUtil.assertProps(env.listener("s0").assertOneGetNewAndReset(), fields, new Object[]{null, null, null, 13, "A_2", "B_2", null, null, null});

            env.sendEventBean(new SupportBean_S2(20, "A_1", "B_1"));
            EPAssertionUtil.assertProps(env.listener("s0").assertOneGetNewAndReset(), fields, new Object[]{null, null, null, null, null, null, 20, "A_1", "B_1"});

            env.sendEventBean(new SupportBean_S2(21, "A_2", "B_1"));
            EPAssertionUtil.assertProps(env.listener("s0").assertOneGetNewAndReset(), fields, new Object[]{null, null, null, null, null, null, 21, "A_2", "B_1"});

            env.sendEventBean(new SupportBean_S2(22, "A_1", "B_2"));
            EPAssertionUtil.assertProps(env.listener("s0").assertOneGetNewAndReset(), fields, new Object[]{null, null, null, null, null, null, 22, "A_1", "B_2"});

            env.sendEventBean(new SupportBean_S2(23, "A_2", "B_2"));
            EPAssertionUtil.assertProps(env.listener("s0").assertOneGetNewAndReset(), fields, new Object[]{null, null, null, null, null, null, 23, "A_2", "B_2"});

            env.sendEventBean(new SupportBean_S0(1, "A_3", "B_3"));
            EPAssertionUtil.assertProps(env.listener("s0").assertOneGetNewAndReset(), fields, new Object[]{1, "A_3", "B_3", null, null, null, null, null, null});

            env.sendEventBean(new SupportBean_S0(2, "A_1", "B_3"));
            EPAssertionUtil.assertProps(env.listener("s0").assertOneGetNewAndReset(), fields, new Object[]{2, "A_1", "B_3", null, null, null, null, null, null});

            env.sendEventBean(new SupportBean_S0(3, "A_3", "B_1"));
            EPAssertionUtil.assertProps(env.listener("s0").assertOneGetNewAndReset(), fields, new Object[]{3, "A_3", "B_1", null, null, null, null, null, null});

            env.sendEventBean(new SupportBean_S0(4, "A_2", "B_2"));
            EPAssertionUtil.assertProps(env.listener("s0").assertOneGetNewAndReset(), fields, new Object[]{4, "A_2", "B_2", 13, "A_2", "B_2", 23, "A_2", "B_2"});

            env.sendEventBean(new SupportBean_S0(5, "A_2", "B_1"));
            EPAssertionUtil.assertProps(env.listener("s0").assertOneGetNewAndReset(), fields, new Object[]{5, "A_2", "B_1", 11, "A_2", "B_1", 21, "A_2", "B_1"});

            env.sendEventBean(new SupportBean_S1(14, "A_4", "B_3"));
            EPAssertionUtil.assertProps(env.listener("s0").assertOneGetNewAndReset(), fields, new Object[]{null, null, null, 14, "A_4", "B_3", null, null, null});

            env.sendEventBean(new SupportBean_S1(15, "A_1", "B_3"));
            EPAssertionUtil.assertProps(env.listener("s0").assertOneGetNewAndReset(), fields, new Object[]{2, "A_1", "B_3", 15, "A_1", "B_3", null, null, null});

            env.sendEventBean(new SupportBean_S2(24, "A_1", "B_3"));
            EPAssertionUtil.assertProps(env.listener("s0").assertOneGetNewAndReset(), fields, new Object[]{2, "A_1", "B_3", 15, "A_1", "B_3", 24, "A_1", "B_3"});

            env.sendEventBean(new SupportBean_S2(25, "A_2", "B_3"));
            EPAssertionUtil.assertProps(env.listener("s0").assertOneGetNewAndReset(), fields, new Object[]{null, null, null, null, null, null, 25, "A_2", "B_3"});

            env.undeployAll();
        }
    }

    private static class EPLJoinFullJoin2Sides implements RegressionExecution {
        public void run(RegressionEnvironment env) {
            /**
             * Query:
             *                  s0
             *           s1 <->      <-> s2
             */
            String joinStatement = "@name('s0') select * from " +
                "SupportBean_S0#length(1000) as s0 " +
                " full outer join SupportBean_S1#length(1000) as s1 on s0.p00 = s1.p10 " +
                " full outer join SupportBean_S2#length(1000) as s2 on s0.p00 = s2.p20 ";
            env.compileDeployAddListenerMileZero(joinStatement, "s0");

            tryAssertsFullJoin_2sides(env);

            env.undeployAll();
        }
    }

    private static void tryAssertsFullJoin_2sides(RegressionEnvironment env) {
        // Test s0 outer join to 2 streams, 2 results for each (cartesian product)
        //
        Object[] s1Events = SupportBean_S1.makeS1("A", new String[]{"A-s1-1", "A-s1-2"});
        sendEvent(env, s1Events);
        EPAssertionUtil.assertSameAnyOrder(new Object[][]{{null, s1Events[1], null}}, getAndResetNewEvents(env));
        EPAssertionUtil.assertPropsPerRowAnyOrder(env.iterator("s0"), FIELDS,
            new Object[][]{{null, null, "A", "A-s1-1", null, null},
                {null, null, "A", "A-s1-2", null, null}});

        Object[] s2Events = SupportBean_S2.makeS2("A", new String[]{"A-s2-1", "A-s2-2"});
        sendEvent(env, s2Events);
        EPAssertionUtil.assertSameAnyOrder(new Object[][]{{null, null, s2Events[1]}}, getAndResetNewEvents(env));
        EPAssertionUtil.assertPropsPerRowAnyOrder(env.iterator("s0"), FIELDS,
            new Object[][]{{null, null, "A", "A-s1-1", null, null},
                {null, null, "A", "A-s1-2", null, null},
                {null, null, null, null, "A", "A-s2-1"},
                {null, null, null, null, "A", "A-s2-2"}});

        Object[] s0Events = SupportBean_S0.makeS0("A", new String[]{"A-s0-1"});
        sendEvent(env, s0Events);
        Object[][] expected = new Object[][]{
            {s0Events[0], s1Events[0], s2Events[0]},
            {s0Events[0], s1Events[1], s2Events[0]},
            {s0Events[0], s1Events[0], s2Events[1]},
            {s0Events[0], s1Events[1], s2Events[1]},
        };
        EPAssertionUtil.assertSameAnyOrder(expected, getAndResetNewEvents(env));
        EPAssertionUtil.assertPropsPerRowAnyOrder(env.iterator("s0"), FIELDS,
            new Object[][]{{"A", "A-s0-1", "A", "A-s1-1", "A", "A-s2-1"},
                {"A", "A-s0-1", "A", "A-s1-2", "A", "A-s2-1"},
                {"A", "A-s0-1", "A", "A-s1-1", "A", "A-s2-2"},
                {"A", "A-s0-1", "A", "A-s1-2", "A", "A-s2-2"}});

        // Test s0 outer join to s1 and s2, no results for each s1 and s2
        //
        s0Events = SupportBean_S0.makeS0("B", new String[]{"B-s0-1"});
        sendEvent(env, s0Events);
        EPAssertionUtil.assertSameAnyOrder(new Object[][]{{s0Events[0], null, null}}, getAndResetNewEvents(env));
        EPAssertionUtil.assertPropsPerRowAnyOrder(env.iterator("s0"), FIELDS,
            new Object[][]{{"A", "A-s0-1", "A", "A-s1-1", "A", "A-s2-1"},
                {"A", "A-s0-1", "A", "A-s1-2", "A", "A-s2-1"},
                {"A", "A-s0-1", "A", "A-s1-1", "A", "A-s2-2"},
                {"A", "A-s0-1", "A", "A-s1-2", "A", "A-s2-2"},
                {"B", "B-s0-1", null, null, null, null}});

        s0Events = SupportBean_S0.makeS0("B", new String[]{"B-s0-2"});
        sendEvent(env, s0Events);
        EPAssertionUtil.assertSameAnyOrder(new Object[][]{{s0Events[0], null, null}}, getAndResetNewEvents(env));
        EPAssertionUtil.assertPropsPerRowAnyOrder(env.iterator("s0"), FIELDS,
            new Object[][]{{"A", "A-s0-1", "A", "A-s1-1", "A", "A-s2-1"},
                {"A", "A-s0-1", "A", "A-s1-2", "A", "A-s2-1"},
                {"A", "A-s0-1", "A", "A-s1-1", "A", "A-s2-2"},
                {"A", "A-s0-1", "A", "A-s1-2", "A", "A-s2-2"},
                {"B", "B-s0-1", null, null, null, null},
                {"B", "B-s0-2", null, null, null, null}});

        // Test s0 outer join to s1 and s2, one row for s1 and no results for s2
        //
        s1Events = SupportBean_S1.makeS1("C", new String[]{"C-s1-1"});
        sendEventsAndReset(env, s1Events);
        EPAssertionUtil.assertPropsPerRowAnyOrder(env.iterator("s0"), FIELDS,
            new Object[][]{{"A", "A-s0-1", "A", "A-s1-1", "A", "A-s2-1"},
                {"A", "A-s0-1", "A", "A-s1-2", "A", "A-s2-1"},
                {"A", "A-s0-1", "A", "A-s1-1", "A", "A-s2-2"},
                {"A", "A-s0-1", "A", "A-s1-2", "A", "A-s2-2"},
                {"B", "B-s0-1", null, null, null, null},
                {"B", "B-s0-2", null, null, null, null},
                {null, null, "C", "C-s1-1", null, null}});

        s0Events = SupportBean_S0.makeS0("C", new String[]{"C-s0-1"});
        sendEvent(env, s0Events);
        EPAssertionUtil.assertSameAnyOrder(new Object[][]{{s0Events[0], s1Events[0], null}}, getAndResetNewEvents(env));
        EPAssertionUtil.assertPropsPerRowAnyOrder(env.iterator("s0"), FIELDS,
            new Object[][]{{"A", "A-s0-1", "A", "A-s1-1", "A", "A-s2-1"},
                {"A", "A-s0-1", "A", "A-s1-2", "A", "A-s2-1"},
                {"A", "A-s0-1", "A", "A-s1-1", "A", "A-s2-2"},
                {"A", "A-s0-1", "A", "A-s1-2", "A", "A-s2-2"},
                {"B", "B-s0-1", null, null, null, null},
                {"B", "B-s0-2", null, null, null, null},
                {"C", "C-s0-1", "C", "C-s1-1", null, null}});

        // Test s0 outer join to s1 and s2, two rows for s1 and no results for s2
        //
        s1Events = SupportBean_S1.makeS1("D", new String[]{"D-s1-1", "D-s1-2"});
        sendEventsAndReset(env, s1Events);

        s0Events = SupportBean_S0.makeS0("D", new String[]{"D-s0-1"});
        sendEvent(env, s0Events);
        EPAssertionUtil.assertSameAnyOrder(new Object[][]{
            {s0Events[0], s1Events[0], null},
            {s0Events[0], s1Events[1], null}}, getAndResetNewEvents(env));

        // Test s0 outer join to s1 and s2, one row for s2 and no results for s1
        //
        s2Events = SupportBean_S2.makeS2("E", new String[]{"E-s2-1"});
        sendEventsAndReset(env, s2Events);

        s0Events = SupportBean_S0.makeS0("E", new String[]{"E-s0-1"});
        sendEvent(env, s0Events);
        EPAssertionUtil.assertSameAnyOrder(new Object[][]{{s0Events[0], null, s2Events[0]}}, getAndResetNewEvents(env));

        // Test s0 outer join to s1 and s2, two rows for s2 and no results for s1
        //
        s2Events = SupportBean_S2.makeS2("F", new String[]{"F-s2-1", "F-s2-2"});
        sendEventsAndReset(env, s2Events);

        s0Events = SupportBean_S0.makeS0("F", new String[]{"F-s0-1"});
        sendEvent(env, s0Events);
        EPAssertionUtil.assertSameAnyOrder(new Object[][]{
            {s0Events[0], null, s2Events[0]},
            {s0Events[0], null, s2Events[1]}}, getAndResetNewEvents(env));

        // Test s0 outer join to s1 and s2, one row for s1 and two rows s2
        //
        s1Events = SupportBean_S1.makeS1("G", new String[]{"G-s1-1"});
        sendEventsAndReset(env, s1Events);

        s2Events = SupportBean_S2.makeS2("G", new String[]{"G-s2-1", "G-s2-2"});
        sendEventsAndReset(env, s2Events);

        s0Events = SupportBean_S0.makeS0("G", new String[]{"G-s0-2"});
        sendEvent(env, s0Events);
        expected = new Object[][]{
            {s0Events[0], s1Events[0], s2Events[0]},
            {s0Events[0], s1Events[0], s2Events[1]},
        };
        EPAssertionUtil.assertSameAnyOrder(expected, getAndResetNewEvents(env));

        // Test s0 outer join to s1 and s2, one row for s2 and two rows s1
        //
        s1Events = SupportBean_S1.makeS1("H", new String[]{"H-s1-1", "H-s1-2"});
        sendEventsAndReset(env, s1Events);

        s2Events = SupportBean_S2.makeS2("H", new String[]{"H-s2-1"});
        sendEventsAndReset(env, s2Events);

        s0Events = SupportBean_S0.makeS0("H", new String[]{"H-s0-2"});
        sendEvent(env, s0Events);
        expected = new Object[][]{
            {s0Events[0], s1Events[0], s2Events[0]},
            {s0Events[0], s1Events[1], s2Events[0]},
        };
        EPAssertionUtil.assertSameAnyOrder(expected, getAndResetNewEvents(env));

        // Test s0 outer join to s1 and s2, one row for each s1 and s2
        //
        s1Events = SupportBean_S1.makeS1("I", new String[]{"I-s1-1"});
        sendEventsAndReset(env, s1Events);

        s2Events = SupportBean_S2.makeS2("I", new String[]{"I-s2-1"});
        sendEventsAndReset(env, s2Events);

        s0Events = SupportBean_S0.makeS0("I", new String[]{"I-s0-2"});
        sendEvent(env, s0Events);
        expected = new Object[][]{
            {s0Events[0], s1Events[0], s2Events[0]},
        };
        EPAssertionUtil.assertSameAnyOrder(expected, getAndResetNewEvents(env));

        // Test s1 inner join to s0 and outer to s2:  s0 with 1 rows, s2 with 2 rows
        //
        s0Events = SupportBean_S0.makeS0("Q", new String[]{"Q-s0-1"});
        sendEvent(env, s0Events);
        EPAssertionUtil.assertSameAnyOrder(new Object[][]{{s0Events[0], null, null}}, getAndResetNewEvents(env));

        s2Events = SupportBean_S2.makeS2("Q", new String[]{"Q-s2-1", "Q-s2-2"});
        sendEvent(env, s2Events[0]);
        EPAssertionUtil.assertSameAnyOrder(new Object[][]{{s0Events[0], null, s2Events[0]}}, getAndResetNewEvents(env));
        sendEvent(env, s2Events[1]);
        EPAssertionUtil.assertSameAnyOrder(new Object[][]{{s0Events[0], null, s2Events[1]}}, getAndResetNewEvents(env));

        s1Events = SupportBean_S1.makeS1("Q", new String[]{"Q-s1-1"});
        sendEvent(env, s1Events);
        expected = new Object[][]{
            {s0Events[0], s1Events[0], s2Events[0]},
            {s0Events[0], s1Events[0], s2Events[1]},
        };
        EPAssertionUtil.assertSameAnyOrder(expected, getAndResetNewEvents(env));

        // Test s1 inner join to s0 and outer to s2:  s0 with 0 rows, s2 with 2 rows
        //
        s2Events = SupportBean_S2.makeS2("R", new String[]{"R-s2-1", "R-s2-2"});
        sendEvent(env, s2Events);
        EPAssertionUtil.assertSameAnyOrder(new Object[][]{{null, null, s2Events[1]}}, getAndResetNewEvents(env));

        s1Events = SupportBean_S1.makeS1("R", new String[]{"R-s1-1"});
        sendEvent(env, s1Events);
        EPAssertionUtil.assertSameAnyOrder(new Object[][]{{null, s1Events[0], null}}, getAndResetNewEvents(env));

        // Test s1 inner join to s0 and outer to s2:  s0 with 1 rows, s2 with 0 rows
        //
        s0Events = SupportBean_S0.makeS0("S", new String[]{"S-s0-1"});
        sendEvent(env, s0Events);
        EPAssertionUtil.assertSameAnyOrder(new Object[][]{{s0Events[0], null, null}}, getAndResetNewEvents(env));

        s1Events = SupportBean_S1.makeS1("S", new String[]{"S-s1-1"});
        sendEvent(env, s1Events);
        EPAssertionUtil.assertSameAnyOrder(new Object[][]{{s0Events[0], s1Events[0], null}}, getAndResetNewEvents(env));

        // Test s1 inner join to s0 and outer to s2:  s0 with 1 rows, s2 with 1 rows
        //
        s0Events = SupportBean_S0.makeS0("T", new String[]{"T-s0-1"});
        sendEvent(env, s0Events);
        EPAssertionUtil.assertSameAnyOrder(new Object[][]{{s0Events[0], null, null}}, getAndResetNewEvents(env));

        s2Events = SupportBean_S2.makeS2("T", new String[]{"T-s2-1"});
        sendEventsAndReset(env, s2Events);

        s1Events = SupportBean_S1.makeS1("T", new String[]{"T-s1-1"});
        sendEvent(env, s1Events);
        EPAssertionUtil.assertSameAnyOrder(new Object[][]{{s0Events[0], s1Events[0], s2Events[0]}}, getAndResetNewEvents(env));

        // Test s1 inner join to s0 and outer to s2:  s0 with 2 rows, s2 with 0 rows
        //
        s0Events = SupportBean_S0.makeS0("U", new String[]{"U-s0-1", "U-s0-1"});
        sendEventsAndReset(env, s0Events);

        s1Events = SupportBean_S1.makeS1("U", new String[]{"U-s1-1"});
        sendEvent(env, s1Events);
        expected = new Object[][]{
            {s0Events[0], s1Events[0], null},
            {s0Events[1], s1Events[0], null},
        };
        EPAssertionUtil.assertSameAnyOrder(expected, getAndResetNewEvents(env));

        // Test s1 inner join to s0 and outer to s2:  s0 with 2 rows, s2 with 1 rows
        //
        s0Events = SupportBean_S0.makeS0("V", new String[]{"V-s0-1", "V-s0-1"});
        sendEventsAndReset(env, s0Events);

        s2Events = SupportBean_S2.makeS2("V", new String[]{"V-s2-1"});
        sendEventsAndReset(env, s2Events);

        s1Events = SupportBean_S1.makeS1("V", new String[]{"V-s1-1"});
        sendEvent(env, s1Events);
        expected = new Object[][]{
            {s0Events[0], s1Events[0], s2Events[0]},
            {s0Events[1], s1Events[0], s2Events[0]},
        };
        EPAssertionUtil.assertSameAnyOrder(expected, getAndResetNewEvents(env));

        // Test s1 inner join to s0 and outer to s2:  s0 with 2 rows, s2 with 2 rows
        //
        s0Events = SupportBean_S0.makeS0("W", new String[]{"W-s0-1", "W-s0-2"});
        sendEventsAndReset(env, s0Events);

        s2Events = SupportBean_S2.makeS2("W", new String[]{"W-s2-1", "W-s2-2"});
        sendEventsAndReset(env, s2Events);

        s1Events = SupportBean_S1.makeS1("W", new String[]{"W-s1-1"});
        sendEvent(env, s1Events);
        expected = new Object[][]{
            {s0Events[0], s1Events[0], s2Events[0]},
            {s0Events[1], s1Events[0], s2Events[0]},
            {s0Events[0], s1Events[0], s2Events[1]},
            {s0Events[1], s1Events[0], s2Events[1]},
        };
        EPAssertionUtil.assertSameAnyOrder(expected, getAndResetNewEvents(env));

        // Test s2 inner join to s0 and outer to s1:  s0 with 1 rows, s1 with 2 rows
        //
        s0Events = SupportBean_S0.makeS0("J", new String[]{"J-s0-1"});
        sendEventsAndReset(env, s0Events);

        s1Events = SupportBean_S1.makeS1("J", new String[]{"J-s1-1", "J-s1-2"});
        sendEventsAndReset(env, s1Events);

        s2Events = SupportBean_S2.makeS2("J", new String[]{"J-s2-1"});
        sendEvent(env, s2Events);
        expected = new Object[][]{
            {s0Events[0], s1Events[0], s2Events[0]},
            {s0Events[0], s1Events[1], s2Events[0]},
        };
        EPAssertionUtil.assertSameAnyOrder(expected, getAndResetNewEvents(env));

        // Test s2 inner join to s0 and outer to s1:  s0 with 0 rows, s1 with 2 rows
        //
        s1Events = SupportBean_S1.makeS1("K", new String[]{"K-s1-1", "K-s1-2"});
        sendEventsAndReset(env, s2Events);

        s2Events = SupportBean_S2.makeS2("K", new String[]{"K-s2-1"});
        sendEventsAndReset(env, s2Events);

        // Test s2 inner join to s0 and outer to s1:  s0 with 1 rows, s1 with 0 rows
        //
        s0Events = SupportBean_S0.makeS0("L", new String[]{"L-s0-1"});
        sendEventsAndReset(env, s0Events);

        s2Events = SupportBean_S2.makeS2("L", new String[]{"L-s2-1"});
        sendEvent(env, s2Events);
        EPAssertionUtil.assertSameAnyOrder(new Object[][]{{s0Events[0], null, s2Events[0]}}, getAndResetNewEvents(env));

        // Test s2 inner join to s0 and outer to s1:  s0 with 1 rows, s1 with 1 rows
        //
        s0Events = SupportBean_S0.makeS0("M", new String[]{"M-s0-1"});
        sendEventsAndReset(env, s0Events);

        s1Events = SupportBean_S1.makeS1("M", new String[]{"M-s1-1"});
        sendEventsAndReset(env, s1Events);

        s2Events = SupportBean_S2.makeS2("M", new String[]{"M-s2-1"});
        sendEvent(env, s2Events);
        EPAssertionUtil.assertSameAnyOrder(new Object[][]{{s0Events[0], s1Events[0], s2Events[0]}}, getAndResetNewEvents(env));

        // Test s2 inner join to s0 and outer to s1:  s0 with 2 rows, s1 with 0 rows
        //
        s0Events = SupportBean_S0.makeS0("N", new String[]{"N-s0-1", "N-s0-1"});
        sendEventsAndReset(env, s0Events);

        s2Events = SupportBean_S2.makeS2("N", new String[]{"N-s2-1"});
        sendEvent(env, s2Events);
        expected = new Object[][]{
            {s0Events[0], null, s2Events[0]},
            {s0Events[1], null, s2Events[0]},
        };
        EPAssertionUtil.assertSameAnyOrder(expected, getAndResetNewEvents(env));

        // Test s2 inner join to s0 and outer to s1:  s0 with 2 rows, s1 with 1 rows
        //
        s0Events = SupportBean_S0.makeS0("O", new String[]{"O-s0-1", "O-s0-1"});
        sendEventsAndReset(env, s0Events);

        s1Events = SupportBean_S1.makeS1("O", new String[]{"O-s1-1"});
        sendEventsAndReset(env, s1Events);

        s2Events = SupportBean_S2.makeS2("O", new String[]{"O-s2-1"});
        sendEvent(env, s2Events);
        expected = new Object[][]{
            {s0Events[0], s1Events[0], s2Events[0]},
            {s0Events[1], s1Events[0], s2Events[0]},
        };
        EPAssertionUtil.assertSameAnyOrder(expected, getAndResetNewEvents(env));

        // Test s2 inner join to s0 and outer to s1:  s0 with 2 rows, s1 with 2 rows
        //
        s0Events = SupportBean_S0.makeS0("P", new String[]{"P-s0-1", "P-s0-2"});
        sendEventsAndReset(env, s0Events);

        s1Events = SupportBean_S1.makeS1("P", new String[]{"P-s1-1", "P-s1-2"});
        sendEventsAndReset(env, s1Events);

        s2Events = SupportBean_S2.makeS2("P", new String[]{"P-s2-1"});
        sendEvent(env, s2Events);
        expected = new Object[][]{
            {s0Events[0], s1Events[0], s2Events[0]},
            {s0Events[1], s1Events[0], s2Events[0]},
            {s0Events[0], s1Events[1], s2Events[0]},
            {s0Events[1], s1Events[1], s2Events[0]},
        };
        EPAssertionUtil.assertSameAnyOrder(expected, getAndResetNewEvents(env));
    }

    private static void sendEvent(RegressionEnvironment env, Object theEvent) {
        env.sendEventBean(theEvent);
    }

    private static void sendEventsAndReset(RegressionEnvironment env, Object[] events) {
        sendEvent(env, events);
        env.listener("s0").reset();
    }

    private static void sendEvent(RegressionEnvironment env, Object[] events) {
        for (int i = 0; i < events.length; i++) {
            env.sendEventBean(events[i]);
        }
    }

    private static Object[][] getAndResetNewEvents(RegressionEnvironment env) {
        EventBean[] newEvents = env.listener("s0").getLastNewData();
        env.listener("s0").reset();
        return ArrayHandlingUtil.getUnderlyingEvents(newEvents, new String[]{"s0", "s1", "s2"});
    }

    private static class MyLocalJsonProvided {
        public SupportBean_S0 s0;
        public SupportBean_S1 s1;
        public SupportBean_S2 s2;
    }
}
