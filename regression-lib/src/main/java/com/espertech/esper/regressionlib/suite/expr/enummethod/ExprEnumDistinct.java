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
import com.espertech.esper.common.internal.support.SupportBean;
import com.espertech.esper.regressionlib.framework.RegressionEnvironment;
import com.espertech.esper.regressionlib.framework.RegressionExecution;
import com.espertech.esper.regressionlib.support.bean.SupportBean_ST0_Container;
import com.espertech.esper.regressionlib.support.bean.SupportCollection;
import com.espertech.esper.regressionlib.support.bean.SupportEventWithManyArray;
import com.espertech.esper.regressionlib.support.util.LambdaAssertionUtil;

import java.util.ArrayList;
import java.util.Collection;

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
            String eplFragment = "@name('s0') select intArrayCollection.distinctOf() as c0, intArrayCollection.distinctOf(v => v) as c1 from SupportEventWithManyArray";
            env.compileDeploy(eplFragment).addListener("s0");

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

            String[] fields = "val0".split(",");
            String eplFragment = "@name('s0') select " +
                "contained.distinctOf(x => p00) as val0 " +
                " from SupportBean_ST0_Container";
            env.compileDeploy(eplFragment).addListener("s0");

            LambdaAssertionUtil.assertTypes(env.statement("s0").getEventType(), fields, new Class[]{Collection.class});

            env.sendEventBean(SupportBean_ST0_Container.make2Value("E1,1", "E2,2", "E3,1"));
            LambdaAssertionUtil.assertST0Id(env.listener("s0"), "val0", "E1,E2");
            env.listener("s0").reset();

            env.sendEventBean(SupportBean_ST0_Container.make2Value("E3,1", "E2,2", "E4,1", "E1,2"));
            LambdaAssertionUtil.assertST0Id(env.listener("s0"), "val0", "E3,E2");
            env.listener("s0").reset();

            env.sendEventBean(SupportBean_ST0_Container.make2Value(null));
            for (String field : fields) {
                LambdaAssertionUtil.assertST0Id(env.listener("s0"), field, null);
            }
            env.listener("s0").reset();

            env.sendEventBean(SupportBean_ST0_Container.make2Value());
            for (String field : fields) {
                LambdaAssertionUtil.assertST0Id(env.listener("s0"), field, "");
            }
            env.listener("s0").reset();

            env.undeployAll();
        }
    }

    private static class ExprEnumDistinctScalar implements RegressionExecution {
        public void run(RegressionEnvironment env) {

            String[] fields = "val0,val1".split(",");
            String eplFragment = "@name('s0') select " +
                "strvals.distinctOf() as val0, " +
                "strvals.distinctOf(v => extractNum(v)) as val1 " +
                "from SupportCollection";
            env.compileDeploy(eplFragment).addListener("s0");

            LambdaAssertionUtil.assertTypes(env.statement("s0").getEventType(), fields, new Class[]{Collection.class, Collection.class});

            env.sendEventBean(SupportCollection.makeString("E2,E1,E2,E2"));
            LambdaAssertionUtil.assertValuesArrayScalar(env.listener("s0"), "val0", "E2", "E1");
            LambdaAssertionUtil.assertValuesArrayScalar(env.listener("s0"), "val1", "E2", "E1");
            env.listener("s0").reset();

            LambdaAssertionUtil.assertSingleAndEmptySupportColl(env, fields);
            env.undeployAll();
        }
    }
}
