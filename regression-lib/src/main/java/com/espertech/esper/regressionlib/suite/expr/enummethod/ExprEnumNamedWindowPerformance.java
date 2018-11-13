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
import com.espertech.esper.regressionlib.framework.RegressionEnvironment;
import com.espertech.esper.regressionlib.framework.RegressionExecution;
import com.espertech.esper.regressionlib.framework.RegressionPath;
import com.espertech.esper.common.internal.support.SupportBean;
import com.espertech.esper.regressionlib.support.bean.SupportBean_ST0;

import java.util.Collection;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

public class ExprEnumNamedWindowPerformance implements RegressionExecution {

    @Override
    public boolean excludeWhenInstrumented() {
        return true;
    }

    public void run(RegressionEnvironment env) {

        RegressionPath path = new RegressionPath();
        env.compileDeploy("create window Win#keepall as SupportBean", path);
        env.compileDeploy("insert into Win select * from SupportBean", path);

        // preload
        for (int i = 0; i < 10000; i++) {
            env.sendEventBean(new SupportBean("K" + i % 100, i));
        }

        runAssertionReuse(env, path);

        runAssertionSubquery(env, path);

        env.undeployAll();
    }

    private void runAssertionSubquery(RegressionEnvironment env, RegressionPath path) {

        // test expression reuse
        String epl = "@name('s0') expression q {" +
            "  x => (select * from Win where intPrimitive = x.p00)" +
            "}" +
            "select " +
            "q(st0).where(x => theString = key0) as val0, " +
            "q(st0).where(x => theString = key0) as val1, " +
            "q(st0).where(x => theString = key0) as val2, " +
            "q(st0).where(x => theString = key0) as val3, " +
            "q(st0).where(x => theString = key0) as val4, " +
            "q(st0).where(x => theString = key0) as val5, " +
            "q(st0).where(x => theString = key0) as val6, " +
            "q(st0).where(x => theString = key0) as val7, " +
            "q(st0).where(x => theString = key0) as val8, " +
            "q(st0).where(x => theString = key0) as val9 " +
            "from SupportBean_ST0 st0";
        env.compileDeploy(epl, path).addListener("s0");

        long start = System.currentTimeMillis();
        for (int i = 0; i < 5000; i++) {
            env.sendEventBean(new SupportBean_ST0("ID", "K50", 1050));
            EventBean theEvent = env.listener("s0").assertOneGetNewAndReset();
            for (int j = 0; j < 10; j++) {
                Collection coll = (Collection) theEvent.get("val" + j);
                assertEquals(1, coll.size());
                SupportBean bean = (SupportBean) coll.iterator().next();
                assertEquals("K50", bean.getTheString());
                assertEquals(1050, bean.getIntPrimitive());
            }
        }
        long delta = System.currentTimeMillis() - start;
        assertTrue("Delta = " + delta, delta < 1000);

        env.undeployModuleContaining("s0");
    }

    private void runAssertionReuse(RegressionEnvironment env, RegressionPath path) {

        // test expression reuse
        String epl = "@name('s0') expression q {" +
            "  x => Win(theString = x.key0).where(y => intPrimitive = x.p00)" +
            "}" +
            "select " +
            "q(st0) as val0, " +
            "q(st0) as val1, " +
            "q(st0) as val2, " +
            "q(st0) as val3, " +
            "q(st0) as val4, " +
            "q(st0) as val5, " +
            "q(st0) as val6, " +
            "q(st0) as val7, " +
            "q(st0) as val8, " +
            "q(st0) as val9 " +
            "from SupportBean_ST0 st0";
        env.compileDeploy(epl, path).addListener("s0");

        long start = System.currentTimeMillis();
        for (int i = 0; i < 5000; i++) {
            env.sendEventBean(new SupportBean_ST0("ID", "K50", 1050));
            EventBean theEvent = env.listener("s0").assertOneGetNewAndReset();
            for (int j = 0; j < 10; j++) {
                Collection coll = (Collection) theEvent.get("val" + j);
                assertEquals(1, coll.size());
                SupportBean bean = (SupportBean) coll.iterator().next();
                assertEquals("K50", bean.getTheString());
                assertEquals(1050, bean.getIntPrimitive());
            }
        }
        long delta = System.currentTimeMillis() - start;
        assertTrue("Delta = " + delta, delta < 1000);

        // This will create a single dispatch
        // env.sendEventBean(new SupportBean("E1", 1));
        env.undeployModuleContaining("s0");
    }
}
