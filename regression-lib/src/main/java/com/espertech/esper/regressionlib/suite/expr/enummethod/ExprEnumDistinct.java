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
package com.espertech.esper.regressionlib.suite.expr.enummethod;

import com.espertech.esper.common.client.EventBean;
import com.espertech.esper.common.client.scopetest.EPAssertionUtil;
import com.espertech.esper.common.client.type.EPTypeClassParameterized;
import com.espertech.esper.common.internal.support.SupportBean;
import com.espertech.esper.common.internal.support.SupportEventPropUtil;
import com.espertech.esper.regressionlib.framework.RegressionEnvironment;
import com.espertech.esper.regressionlib.framework.RegressionExecution;
import com.espertech.esper.regressionlib.support.bean.SupportBean_ST0;
import com.espertech.esper.regressionlib.support.bean.SupportBean_ST0_Container;
import com.espertech.esper.regressionlib.support.bean.SupportCollection;
import com.espertech.esper.regressionlib.support.bean.SupportEventWithManyArray;
import com.espertech.esper.regressionlib.support.expreval.SupportEvalBuilder;
import com.espertech.esper.regressionlib.support.util.LambdaAssertionUtil;

import java.util.ArrayList;
import java.util.Collection;

import static com.espertech.esper.regressionlib.support.util.LambdaAssertionUtil.assertST0Id;
import static com.espertech.esper.regressionlib.support.util.LambdaAssertionUtil.assertValuesArrayScalar;
import static org.junit.Assert.assertEquals;

public class ExprEnumDistinct {

    public static Collection<RegressionExecution> executions() {
        ArrayList<RegressionExecution> execs = new ArrayList<>();
        execs.add(new ExprEnumDistinctEvents());
        execs.add(new ExprEnumDistinctScalar());
        execs.add(new ExprEnumDistinctEventsMultikeyWArray());
        execs.add(new ExprEnumDistinctScalarMultikeyWArray());
        return execs;
    }

    private static class ExprEnumDistinctScalarMultikeyWArray implements RegressionExecution {
        public void run(RegressionEnvironment env) {
            String epl = "@name('s0') select intArrayCollection.distinctOf() as c0, intArrayCollection.distinctOf(v => v) as c1 from SupportEventWithManyArray";
            env.compileDeploy(epl).addListener("s0");

            Collection<int[]> coll = new ArrayList<>();
            coll.add(new int[]{1, 2});
            coll.add(new int[]{2});
            coll.add(new int[]{1, 2});
            coll.add(new int[]{2});
            SupportEventWithManyArray event = new SupportEventWithManyArray().withIntArrayCollection(coll);
            env.sendEventBean(event);
            EventBean received = env.listener("s0").assertOneGetNewAndReset();
            assertField(received, "c0");
            assertField(received, "c1");

            env.undeployAll();
        }

        private void assertField(EventBean received, String field) {
            EPAssertionUtil.assertEqualsExactOrder(new Object[]{new int[]{1, 2}, new int[]{2}}, ((Collection) received.get(field)).toArray());
        }
    }

    private static class ExprEnumDistinctEventsMultikeyWArray implements RegressionExecution {
        public void run(RegressionEnvironment env) {
            String eplFragment = "@name('s0') select (select * from SupportEventWithManyArray#keepall).distinctOf(r => r.intOne) as c0 " +
                " from SupportBean";
            env.compileDeploy(eplFragment).addListener("s0");

            sendManyArray(env, "E1", new int[]{1, 2});
            sendManyArray(env, "E2", new int[]{2});
            sendManyArray(env, "E3", new int[]{1, 2});
            sendManyArray(env, "E4", new int[]{2});

            env.sendEventBean(new SupportBean("SB1", 0));
            Collection<SupportEventWithManyArray> collection = (Collection<SupportEventWithManyArray>) env.listener("s0").assertOneGetNewAndReset().get("c0");
            assertEquals(2, collection.size());

            env.undeployAll();
        }

        private void sendManyArray(RegressionEnvironment env, String id, int[] ints) {
            env.sendEventBean(new SupportEventWithManyArray(id).withIntOne(ints));
        }
    }

    private static class ExprEnumDistinctEvents implements RegressionExecution {
        public void run(RegressionEnvironment env) {
            String[] fields = "c0,c1,c2,c3".split(",");
            SupportEvalBuilder builder = new SupportEvalBuilder("SupportBean_ST0_Container");
            builder.expression(fields[0], "contained.distinctOf(x => p00)");
            builder.expression(fields[1], "contained.distinctOf( (x, i) => case when i<2 then p00 else -1*p00 end)");
            builder.expression(fields[2], "contained.distinctOf( (x, i, s) => case when s<=2 then p00 else 0 end)");
            builder.expression(fields[3], "contained.distinctOf(x => null)");

            builder.statementConsumer(stmt ->
                SupportEventPropUtil.assertTypesAllSame(stmt.getEventType(), fields, EPTypeClassParameterized.from(Collection.class, SupportBean_ST0.class)));

            builder.assertion(SupportBean_ST0_Container.make2Value("E1,1", "E2,2", "E3,1"))
                .verify("c0", val -> assertST0Id(val, "E1,E2"))
                .verify("c1", val -> assertST0Id(val, "E1,E2,E3"))
                .verify("c2", val -> assertST0Id(val, "E1"))
                .verify("c3", val -> assertST0Id(val, "E1"));

            builder.assertion(SupportBean_ST0_Container.make2Value("E3,1", "E2,2", "E4,1", "E1,2"))
                .verify("c0", val -> assertST0Id(val, "E3,E2"))
                .verify("c1", val -> assertST0Id(val, "E3,E2,E4,E1"))
                .verify("c2", val -> assertST0Id(val, "E3"))
                .verify("c3", val -> assertST0Id(val, "E3"));

            builder.assertion(SupportBean_ST0_Container.make2Value("E3,1", "E2,2"))
                .verify("c0", val -> assertST0Id(val, "E3,E2"))
                .verify("c1", val -> assertST0Id(val, "E3,E2"))
                .verify("c2", val -> assertST0Id(val, "E3,E2"))
                .verify("c3", val -> assertST0Id(val, "E3"));

            builder.assertion(SupportBean_ST0_Container.make2ValueNull())
                .verify("c0", val -> assertST0Id(val, null))
                .verify("c1", val -> assertST0Id(val, null))
                .verify("c2", val -> assertST0Id(val, null))
                .verify("c3", val -> assertST0Id(val, null));

            builder.assertion(SupportBean_ST0_Container.make2Value())
                .verify("c0", val -> assertST0Id(val, ""))
                .verify("c1", val -> assertST0Id(val, ""))
                .verify("c2", val -> assertST0Id(val, ""))
                .verify("c3", val -> assertST0Id(val, ""));

            builder.run(env);
        }
    }

    private static class ExprEnumDistinctScalar implements RegressionExecution {
        public void run(RegressionEnvironment env) {
            String[] fields = "c0,c1,c2,c3,c4".split(",");
            SupportEvalBuilder builder = new SupportEvalBuilder("SupportCollection");
            builder.expression(fields[0], "strvals.distinctOf()");
            builder.expression(fields[1], "strvals.distinctOf(v => extractNum(v))");
            builder.expression(fields[2], "strvals.distinctOf((v, i) => case when i<2 then extractNum(v) else 0 end)");
            builder.expression(fields[3], "strvals.distinctOf((v, i, s) => case when s<=2 then extractNum(v) else 0 end)");
            builder.expression(fields[4], "strvals.distinctOf(v => null)");

            builder.statementConsumer(stmt -> SupportEventPropUtil.assertTypesAllSame(stmt.getEventType(), fields, EPTypeClassParameterized.from(Collection.class, String.class)));

            builder.assertion(SupportCollection.makeString("E2,E1,E2,E2"))
                .verify("c0", val -> assertValuesArrayScalar(val, "E2", "E1"))
                .verify("c1", val -> assertValuesArrayScalar(val, "E2", "E1"))
                .verify("c2", val -> assertValuesArrayScalar(val, "E2", "E1", "E2"))
                .verify("c3", val -> assertValuesArrayScalar(val, "E2"))
                .verify("c4", val -> assertValuesArrayScalar(val, "E2"));

            LambdaAssertionUtil.assertSingleAndEmptySupportColl(builder, fields);

            builder.run(env);
        }
    }
}
