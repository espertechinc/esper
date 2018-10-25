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
package com.espertech.esper.regressionlib.suite.epl.subselect;

import com.espertech.esper.common.client.EventBean;
import com.espertech.esper.common.client.scopetest.EPAssertionUtil;
import com.espertech.esper.regressionlib.framework.RegressionEnvironment;
import com.espertech.esper.regressionlib.framework.RegressionExecution;
import com.espertech.esper.common.internal.support.SupportBean;

import java.util.ArrayList;
import java.util.List;

import static com.espertech.esper.regressionlib.framework.SupportMessageAssertUtil.tryInvalidCompile;

public class EPLSubselectAllAnySomeExpr {
    public static List<RegressionExecution> executions() {
        List<RegressionExecution> execs = new ArrayList<>();
        execs.add(new EPLSubselectRelationalOpAll());
        execs.add(new EPLSubselectRelationalOpNullOrNoRows());
        execs.add(new EPLSubselectRelationalOpSome());
        execs.add(new EPLSubselectEqualsNotEqualsAll());
        execs.add(new EPLSubselectEqualsAnyOrSome());
        execs.add(new EPLSubselectEqualsInNullOrNoRows());
        execs.add(new EPLSubselectInvalid());
        return execs;
    }

    private static class EPLSubselectRelationalOpAll implements RegressionExecution {
        public void run(RegressionEnvironment env) {
            String[] fields = "g,ge,l,le".split(",");
            String stmtText = "@name('s0') select " +
                "intPrimitive > all (select intPrimitive from SupportBean(theString like \"S%\")#keepall) as g, " +
                "intPrimitive >= all (select intPrimitive from SupportBean(theString like \"S%\")#keepall) as ge, " +
                "intPrimitive < all (select intPrimitive from SupportBean(theString like \"S%\")#keepall) as l, " +
                "intPrimitive <= all (select intPrimitive from SupportBean(theString like \"S%\")#keepall) as le " +
                "from SupportBean(theString like \"E%\")";
            env.compileDeployAddListenerMileZero(stmtText, "s0");

            env.sendEventBean(new SupportBean("E1", 1));
            EPAssertionUtil.assertProps(env.listener("s0").assertOneGetNewAndReset(), fields, new Object[]{true, true, true, true});

            env.sendEventBean(new SupportBean("S1", 1));

            env.sendEventBean(new SupportBean("E2", 1));
            EPAssertionUtil.assertProps(env.listener("s0").assertOneGetNewAndReset(), fields, new Object[]{false, true, false, true});

            env.sendEventBean(new SupportBean("E2", 2));
            EPAssertionUtil.assertProps(env.listener("s0").assertOneGetNewAndReset(), fields, new Object[]{true, true, false, false});

            env.sendEventBean(new SupportBean("S2", 2));

            env.sendEventBean(new SupportBean("E3", 3));
            EPAssertionUtil.assertProps(env.listener("s0").assertOneGetNewAndReset(), fields, new Object[]{true, true, false, false});

            env.sendEventBean(new SupportBean("E4", 2));
            EPAssertionUtil.assertProps(env.listener("s0").assertOneGetNewAndReset(), fields, new Object[]{false, true, false, false});

            env.sendEventBean(new SupportBean("E5", 1));
            EPAssertionUtil.assertProps(env.listener("s0").assertOneGetNewAndReset(), fields, new Object[]{false, false, false, true});

            env.sendEventBean(new SupportBean("E6", 0));
            EPAssertionUtil.assertProps(env.listener("s0").assertOneGetNewAndReset(), fields, new Object[]{false, false, true, true});

            env.undeployAll();

            tryInvalidCompile(env, "select intArr > all (select intPrimitive from SupportBean#keepall) from SupportBeanArrayCollMap",
                "Failed to validate select-clause expression subquery number 1 querying SupportBean: Collection or array comparison is not allowed for the IN, ANY, SOME or ALL keywords [select intArr > all (select intPrimitive from SupportBean#keepall) from SupportBeanArrayCollMap]");

            // test OM
            env.eplToModelCompileDeploy(stmtText).addListener("s0");
            env.sendEventBean(new SupportBean("E1", 1));
            EPAssertionUtil.assertProps(env.listener("s0").assertOneGetNewAndReset(), fields, new Object[]{true, true, true, true});
            env.undeployAll();
        }
    }

    private static class EPLSubselectRelationalOpNullOrNoRows implements RegressionExecution {
        public void run(RegressionEnvironment env) {
            String[] fields = "vall,vany".split(",");
            String stmtText = "@name('s0') select " +
                "intBoxed >= all (select doubleBoxed from SupportBean(theString like 'S%')#keepall) as vall, " +
                "intBoxed >= any (select doubleBoxed from SupportBean(theString like 'S%')#keepall) as vany " +
                " from SupportBean(theString like 'E%')";
            env.compileDeployAddListenerMileZero(stmtText, "s0");

            // subs is empty
            // select  null >= all (select val from subs), null >= any (select val from subs)
            sendEvent(env, "E1", null, null);
            EPAssertionUtil.assertProps(env.listener("s0").assertOneGetNewAndReset(), fields, new Object[]{true, false});

            // select  1 >= all (select val from subs), 1 >= any (select val from subs)
            sendEvent(env, "E2", 1, null);
            EPAssertionUtil.assertProps(env.listener("s0").assertOneGetNewAndReset(), fields, new Object[]{true, false});

            // subs is {null}
            sendEvent(env, "S1", null, null);

            sendEvent(env, "E3", null, null);
            EPAssertionUtil.assertProps(env.listener("s0").assertOneGetNewAndReset(), fields, new Object[]{null, null});
            sendEvent(env, "E4", 1, null);
            EPAssertionUtil.assertProps(env.listener("s0").assertOneGetNewAndReset(), fields, new Object[]{null, null});

            // subs is {null, 1}
            sendEvent(env, "S2", null, 1d);

            sendEvent(env, "E5", null, null);
            EPAssertionUtil.assertProps(env.listener("s0").assertOneGetNewAndReset(), fields, new Object[]{null, null});
            sendEvent(env, "E6", 1, null);
            EPAssertionUtil.assertProps(env.listener("s0").assertOneGetNewAndReset(), fields, new Object[]{null, true});

            sendEvent(env, "E7", 0, null);
            EventBean theEvent = env.listener("s0").assertOneGetNewAndReset();
            EPAssertionUtil.assertProps(theEvent, fields, new Object[]{false, false});

            env.undeployAll();
        }
    }

    private static class EPLSubselectRelationalOpSome implements RegressionExecution {
        public void run(RegressionEnvironment env) {
            String[] fields = "g,ge,l,le".split(",");
            String stmtText = "@name('s0') select " +
                "intPrimitive > any (select intPrimitive from SupportBean(theString like 'S%')#keepall) as g, " +
                "intPrimitive >= any (select intPrimitive from SupportBean(theString like 'S%')#keepall) as ge, " +
                "intPrimitive < any (select intPrimitive from SupportBean(theString like 'S%')#keepall) as l, " +
                "intPrimitive <= any (select intPrimitive from SupportBean(theString like 'S%')#keepall) as le " +
                " from SupportBean(theString like 'E%')";
            env.compileDeployAddListenerMileZero(stmtText, "s0");

            env.sendEventBean(new SupportBean("E1", 1));
            EPAssertionUtil.assertProps(env.listener("s0").assertOneGetNewAndReset(), fields, new Object[]{false, false, false, false});

            env.sendEventBean(new SupportBean("S1", 1));

            env.sendEventBean(new SupportBean("E2", 1));
            EPAssertionUtil.assertProps(env.listener("s0").assertOneGetNewAndReset(), fields, new Object[]{false, true, false, true});

            env.sendEventBean(new SupportBean("E2", 2));
            EPAssertionUtil.assertProps(env.listener("s0").assertOneGetNewAndReset(), fields, new Object[]{true, true, false, false});

            env.sendEventBean(new SupportBean("E2a", 0));
            EPAssertionUtil.assertProps(env.listener("s0").assertOneGetNewAndReset(), fields, new Object[]{false, false, true, true});

            env.sendEventBean(new SupportBean("S2", 2));

            env.sendEventBean(new SupportBean("E3", 3));
            EPAssertionUtil.assertProps(env.listener("s0").assertOneGetNewAndReset(), fields, new Object[]{true, true, false, false});

            env.sendEventBean(new SupportBean("E4", 2));
            EPAssertionUtil.assertProps(env.listener("s0").assertOneGetNewAndReset(), fields, new Object[]{true, true, false, true});

            env.sendEventBean(new SupportBean("E5", 1));
            EPAssertionUtil.assertProps(env.listener("s0").assertOneGetNewAndReset(), fields, new Object[]{false, true, true, true});

            env.sendEventBean(new SupportBean("E6", 0));
            EPAssertionUtil.assertProps(env.listener("s0").assertOneGetNewAndReset(), fields, new Object[]{false, false, true, true});

            env.undeployAll();
        }
    }

    private static class EPLSubselectEqualsNotEqualsAll implements RegressionExecution {
        public void run(RegressionEnvironment env) {
            String[] fields = "eq,neq,sqlneq,nneq".split(",");
            String stmtText = "@name('s0') select " +
                "intPrimitive=all(select intPrimitive from SupportBean(theString like 'S%')#keepall) as eq, " +
                "intPrimitive != all (select intPrimitive from SupportBean(theString like 'S%')#keepall) as neq, " +
                "intPrimitive <> all (select intPrimitive from SupportBean(theString like 'S%')#keepall) as sqlneq, " +
                "not intPrimitive = all (select intPrimitive from SupportBean(theString like 'S%')#keepall) as nneq " +
                " from SupportBean(theString like 'E%')";
            env.compileDeployAddListenerMileZero(stmtText, "s0");

            env.sendEventBean(new SupportBean("E1", 10));
            EPAssertionUtil.assertProps(env.listener("s0").assertOneGetNewAndReset(), fields, new Object[]{true, true, true, false});

            env.sendEventBean(new SupportBean("S1", 11));

            env.sendEventBean(new SupportBean("E2", 11));
            EPAssertionUtil.assertProps(env.listener("s0").assertOneGetNewAndReset(), fields, new Object[]{true, false, false, false});

            env.sendEventBean(new SupportBean("E3", 10));
            EPAssertionUtil.assertProps(env.listener("s0").assertOneGetNewAndReset(), fields, new Object[]{false, true, true, true});

            env.sendEventBean(new SupportBean("S1", 12));

            env.sendEventBean(new SupportBean("E4", 11));
            EPAssertionUtil.assertProps(env.listener("s0").assertOneGetNewAndReset(), fields, new Object[]{false, false, false, true});

            env.sendEventBean(new SupportBean("E5", 14));
            EPAssertionUtil.assertProps(env.listener("s0").assertOneGetNewAndReset(), fields, new Object[]{false, true, true, true});

            env.undeployAll();
        }

    }    // Test "value = SOME (subselect)" which is the same as "value IN (subselect)"

    private static class EPLSubselectEqualsAnyOrSome implements RegressionExecution {
        public void run(RegressionEnvironment env) {
            String[] fields = "r1,r2,r3,r4".split(",");
            String stmtText = "@name('s0') select " +
                "intPrimitive = SOME (select intPrimitive from SupportBean(theString like 'S%')#keepall) as r1, " +
                "intPrimitive = ANY (select intPrimitive from SupportBean(theString like 'S%')#keepall) as r2, " +
                "intPrimitive != SOME (select intPrimitive from SupportBean(theString like 'S%')#keepall) as r3, " +
                "intPrimitive <> ANY (select intPrimitive from SupportBean(theString like 'S%')#keepall) as r4 " +
                "from SupportBean(theString like 'E%')";
            env.compileDeployAddListenerMileZero(stmtText, "s0");

            env.sendEventBean(new SupportBean("E1", 10));
            EPAssertionUtil.assertProps(env.listener("s0").assertOneGetNewAndReset(), fields, new Object[]{false, false, false, false});

            env.sendEventBean(new SupportBean("S1", 11));
            env.sendEventBean(new SupportBean("E2", 11));
            EPAssertionUtil.assertProps(env.listener("s0").assertOneGetNewAndReset(), fields, new Object[]{true, true, false, false});

            env.sendEventBean(new SupportBean("E3", 12));
            EPAssertionUtil.assertProps(env.listener("s0").assertOneGetNewAndReset(), fields, new Object[]{false, false, true, true});

            env.sendEventBean(new SupportBean("S2", 12));
            env.sendEventBean(new SupportBean("E4", 12));
            EPAssertionUtil.assertProps(env.listener("s0").assertOneGetNewAndReset(), fields, new Object[]{true, true, true, true});

            env.sendEventBean(new SupportBean("E5", 13));
            EPAssertionUtil.assertProps(env.listener("s0").assertOneGetNewAndReset(), fields, new Object[]{false, false, true, true});

            env.undeployAll();
        }
    }

    private static class EPLSubselectEqualsInNullOrNoRows implements RegressionExecution {
        public void run(RegressionEnvironment env) {
            String[] fields = "eall,eany,neall,neany,isin".split(",");
            String stmtText = "@name('s0') select " +
                "intBoxed = all (select doubleBoxed from SupportBean(theString like 'S%')#keepall) as eall, " +
                "intBoxed = any (select doubleBoxed from SupportBean(theString like 'S%')#keepall) as eany, " +
                "intBoxed != all (select doubleBoxed from SupportBean(theString like 'S%')#keepall) as neall, " +
                "intBoxed != any (select doubleBoxed from SupportBean(theString like 'S%')#keepall) as neany, " +
                "intBoxed in (select doubleBoxed from SupportBean(theString like 'S%')#keepall) as isin " +
                " from SupportBean(theString like 'E%')";
            env.compileDeployAddListenerMileZero(stmtText, "s0");

            // subs is empty
            // select  null = all (select val from subs), null = any (select val from subs), null != all (select val from subs), null != any (select val from subs), null in (select val from subs)
            sendEvent(env, "E1", null, null);
            EPAssertionUtil.assertProps(env.listener("s0").assertOneGetNewAndReset(), fields, new Object[]{true, false, true, false, false});

            // select  1 = all (select val from subs), 1 = any (select val from subs), 1 != all (select val from subs), 1 != any (select val from subs), 1 in (select val from subs)
            sendEvent(env, "E2", 1, null);
            EPAssertionUtil.assertProps(env.listener("s0").assertOneGetNewAndReset(), fields, new Object[]{true, false, true, false, false});

            // subs is {null}
            sendEvent(env, "S1", null, null);

            sendEvent(env, "E3", null, null);
            EPAssertionUtil.assertProps(env.listener("s0").assertOneGetNewAndReset(), fields, new Object[]{null, null, null, null, null});
            sendEvent(env, "E4", 1, null);
            EPAssertionUtil.assertProps(env.listener("s0").assertOneGetNewAndReset(), fields, new Object[]{null, null, null, null, null});

            // subs is {null, 1}
            sendEvent(env, "S2", null, 1d);

            sendEvent(env, "E5", null, null);
            EPAssertionUtil.assertProps(env.listener("s0").assertOneGetNewAndReset(), fields, new Object[]{null, null, null, null, null});
            sendEvent(env, "E6", 1, null);
            EPAssertionUtil.assertProps(env.listener("s0").assertOneGetNewAndReset(), fields, new Object[]{null, true, false, null, true});
            sendEvent(env, "E7", 0, null);
            EPAssertionUtil.assertProps(env.listener("s0").assertOneGetNewAndReset(), fields, new Object[]{false, null, null, true, null});

            env.undeployAll();
        }
    }

    private static class EPLSubselectInvalid implements RegressionExecution {
        public void run(RegressionEnvironment env) {
            tryInvalidCompile(env,
                "select intArr = all (select intPrimitive from SupportBean#keepall) as r1 from SupportBeanArrayCollMap",
                "Failed to validate select-clause expression subquery number 1 querying SupportBean: Collection or array comparison is not allowed for the IN, ANY, SOME or ALL keywords [select intArr = all (select intPrimitive from SupportBean#keepall) as r1 from SupportBeanArrayCollMap]");
        }
    }

    private static void sendEvent(RegressionEnvironment env, String theString, Integer intBoxed, Double doubleBoxed) {
        SupportBean bean = new SupportBean(theString, -1);
        bean.setIntBoxed(intBoxed);
        bean.setDoubleBoxed(doubleBoxed);
        env.sendEventBean(bean);
    }
}
