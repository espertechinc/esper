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

import com.espertech.esper.common.client.scopetest.EPAssertionUtil;
import com.espertech.esper.regressionlib.framework.RegressionEnvironment;
import com.espertech.esper.regressionlib.framework.RegressionExecution;
import com.espertech.esper.regressionlib.support.bean.SupportBean_ST0;
import com.espertech.esper.regressionlib.support.bean.SupportBean_ST0_Container;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import static org.junit.Assert.assertTrue;

public class ExprEnumNestedPerformance implements RegressionExecution {

    @Override
    public boolean excludeWhenInstrumented() {
        return true;
    }

    public void run(RegressionEnvironment env) {

        List<SupportBean_ST0> list = new ArrayList<SupportBean_ST0>();
        for (int i = 0; i < 10000; i++) {
            list.add(new SupportBean_ST0("E1", 1000));
        }
        SupportBean_ST0 minEvent = new SupportBean_ST0("E2", 5);
        list.add(minEvent);
        SupportBean_ST0_Container theEvent = new SupportBean_ST0_Container(list);

        // the "contained.min" inner lambda only depends on values within "contained" (a stream's value)
        // and not on the particular "x".
        String eplFragment = "@name('s0') select contained.where(x => x.p00 = contained.min(y => y.p00)) as val from SupportBean_ST0_Container";
        env.compileDeploy(eplFragment).addListener("s0");

        long start = System.currentTimeMillis();
        env.sendEventBean(theEvent);
        long delta = System.currentTimeMillis() - start;
        assertTrue("delta=" + delta, delta < 100);

        Collection<SupportBean_ST0> result = (Collection<SupportBean_ST0>) env.listener("s0").assertOneGetNewAndReset().get("val");
        EPAssertionUtil.assertEqualsExactOrder(new Object[]{minEvent}, result.toArray());

        env.undeployAll();
    }
}
