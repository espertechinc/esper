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
import com.espertech.esper.regressionlib.support.bean.*;
import com.espertech.esper.regressionlib.support.sales.PersonSales;
import com.espertech.esper.regressionlib.support.sales.Sale;

import java.util.*;

import static org.junit.Assert.*;

public class ExprEnumNested {

    public static Collection<RegressionExecution> executions() {
        ArrayList<RegressionExecution> execs = new ArrayList<>();
        execs.add(new ExprEnumEquivalentToMinByUncorrelated());
        execs.add(new ExprEnumMinByWhere());
        execs.add(new ExprEnumCorrelated());
        execs.add(new ExprEnumAnyOf());
        return execs;
    }

    private static class ExprEnumEquivalentToMinByUncorrelated implements RegressionExecution {
        public void run(RegressionEnvironment env) {

            String eplFragment = "@name('s0') select contained.where(x => (x.p00 = contained.min(y => y.p00))) as val from SupportBean_ST0_Container";
            env.compileDeploy(eplFragment).addListener("s0");

            SupportBean_ST0_Container bean = SupportBean_ST0_Container.make2Value("E1,2", "E2,1", "E3,2");
            env.sendEventBean(bean);
            Collection<SupportBean_ST0> result = (Collection<SupportBean_ST0>) env.listener("s0").assertOneGetNewAndReset().get("val");
            EPAssertionUtil.assertEqualsExactOrder(new Object[]{bean.getContained().get(1)}, result.toArray());

            env.undeployAll();
        }
    }

    private static class ExprEnumMinByWhere implements RegressionExecution {
        public void run(RegressionEnvironment env) {

            String eplFragment = "@name('s0') select sales.where(x => x.buyer = persons.minBy(y => age)) as val from PersonSales";
            env.compileDeploy(eplFragment).addListener("s0");

            PersonSales bean = PersonSales.make();
            env.sendEventBean(bean);

            Collection<Sale> sales = (Collection<Sale>) env.listener("s0").assertOneGetNewAndReset().get("val");
            EPAssertionUtil.assertEqualsExactOrder(new Object[]{bean.getSales().get(0)}, sales.toArray());

            env.undeployAll();
        }
    }

    private static class ExprEnumCorrelated implements RegressionExecution {
        public void run(RegressionEnvironment env) {

            String eplFragment = "@name('s0') select contained.where(x => x = (contained.firstOf(y => y.p00 = x.p00 ))) as val from SupportBean_ST0_Container";
            env.compileDeploy(eplFragment).addListener("s0");

            SupportBean_ST0_Container bean = SupportBean_ST0_Container.make2Value("E1,2", "E2,1", "E3,3");
            env.sendEventBean(bean);
            Collection<SupportBean_ST0> result = (Collection<SupportBean_ST0>) env.listener("s0").assertOneGetNewAndReset().get("val");
            assertEquals(3, result.size());  // this would be 1 if the cache is invalid

            env.undeployAll();
        }
    }

    private static class ExprEnumAnyOf implements RegressionExecution {
        public void run(RegressionEnvironment env) {

            // try "in" with "Set<String> multivalues"
            env.compileDeploy("@name('s0') select * from SupportContainerLevelEvent(level1s.anyOf(x=>x.level2s.anyOf(y => 'A' in (y.multivalues))))").addListener("s0");
            tryAssertionAnyOf(env);
            env.undeployAll();

            // try "in" with "String singlevalue"
            env.compileDeploy("@name('s0') select * from SupportContainerLevelEvent(level1s.anyOf(x=>x.level2s.anyOf(y => y.singlevalue = 'A')))").addListener("s0");
            tryAssertionAnyOf(env);
            env.undeployAll();
        }
    }

    private static void tryAssertionAnyOf(RegressionEnvironment env) {
        env.sendEventBean(makeContainerEvent("A"));
        assertTrue(env.listener("s0").getAndClearIsInvoked());

        env.sendEventBean(makeContainerEvent("B"));
        assertFalse(env.listener("s0").getAndClearIsInvoked());
    }

    private static SupportContainerLevelEvent makeContainerEvent(String value) {
        Set<SupportContainerLevel1Event> level1s = new LinkedHashSet<SupportContainerLevel1Event>();
        level1s.add(new SupportContainerLevel1Event(Collections.singleton(new SupportContainerLevel2Event(Collections.singleton("X1"), "X1"))));
        level1s.add(new SupportContainerLevel1Event(Collections.singleton(new SupportContainerLevel2Event(Collections.singleton(value), value))));
        level1s.add(new SupportContainerLevel1Event(Collections.singleton(new SupportContainerLevel2Event(Collections.singleton("X2"), "X2"))));
        return new SupportContainerLevelEvent(level1s);
    }
}
