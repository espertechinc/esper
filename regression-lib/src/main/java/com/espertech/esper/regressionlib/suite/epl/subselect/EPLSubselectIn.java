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

import com.espertech.esper.common.client.scopetest.EPAssertionUtil;
import com.espertech.esper.common.client.soda.*;
import com.espertech.esper.common.internal.support.SupportBean;
import com.espertech.esper.common.internal.support.SupportBean_S0;
import com.espertech.esper.common.internal.support.SupportBean_S1;
import com.espertech.esper.common.internal.support.SupportBean_S2;
import com.espertech.esper.common.internal.util.SerializableObjectCopier;
import com.espertech.esper.regressionlib.framework.RegressionEnvironment;
import com.espertech.esper.regressionlib.framework.RegressionExecution;
import com.espertech.esper.regressionlib.support.bean.*;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import static com.espertech.esper.regressionlib.framework.SupportMessageAssertUtil.tryInvalidCompile;
import static com.espertech.esper.regressionlib.support.util.SupportAdminUtil.assertStatelessStmt;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;

public class EPLSubselectIn {
    public static List<RegressionExecution> executions() {
        List<RegressionExecution> execs = new ArrayList<>();
        execs.add(new EPLSubselectInSelect());
        execs.add(new EPLSubselectInSelectOM());
        execs.add(new EPLSubselectInSelectCompile());
        execs.add(new EPLSubselectInSelectWhere());
        execs.add(new EPLSubselectInSelectWhereExpressions());
        execs.add(new EPLSubselectInFilterCriteria());
        execs.add(new EPLSubselectInWildcard());
        execs.add(new EPLSubselectInNullable());
        execs.add(new EPLSubselectInNullableCoercion());
        execs.add(new EPLSubselectInNullRow());
        execs.add(new EPLSubselectInSingleIndex());
        execs.add(new EPLSubselectInMultiIndex());
        execs.add(new EPLSubselectNotInNullRow());
        execs.add(new EPLSubselectNotInSelect());
        execs.add(new EPLSubselectNotInNullableCoercion());
        execs.add(new EPLSubselectInvalid());
        return execs;
    }

    private static class EPLSubselectInSelect implements RegressionExecution {
        public void run(RegressionEnvironment env) {
            String stmtText = "@name('s0') select id in (select id from SupportBean_S1#length(1000)) as value from SupportBean_S0";
            env.compileDeployAddListenerMileZero(stmtText, "s0");
            assertStatelessStmt(env, "s0", false);

            runTestInSelect(env);

            env.undeployAll();
        }
    }

    private static class EPLSubselectInSelectOM implements RegressionExecution {
        public void run(RegressionEnvironment env) {
            EPStatementObjectModel subquery = new EPStatementObjectModel();
            subquery.setSelectClause(SelectClause.create("id"));
            subquery.setFromClause(FromClause.create(FilterStream.create("SupportBean_S1").addView(View.create("length", Expressions.constant(1000)))));

            EPStatementObjectModel model = new EPStatementObjectModel();
            model.setFromClause(FromClause.create(FilterStream.create("SupportBean_S0")));
            model.setSelectClause(SelectClause.create().add(Expressions.subqueryIn("id", subquery), "value"));
            model = SerializableObjectCopier.copyMayFail(model);

            String stmtText = "select id in (select id from SupportBean_S1#length(1000)) as value from SupportBean_S0";
            assertEquals(stmtText, model.toEPL());

            model.setAnnotations(Collections.singletonList(AnnotationPart.nameAnnotation("s0")));
            env.compileDeploy(model).addListener("s0").milestone(0);

            runTestInSelect(env);

            env.undeployAll();
        }
    }

    private static class EPLSubselectInSelectCompile implements RegressionExecution {
        public void run(RegressionEnvironment env) {
            String stmtText = "@name('s0') select id in (select id from SupportBean_S1#length(1000)) as value from SupportBean_S0";
            env.eplToModelCompileDeploy(stmtText).addListener("s0");

            runTestInSelect(env);

            env.undeployAll();
        }
    }

    public static class EPLSubselectInFilterCriteria implements RegressionExecution {
        public void run(RegressionEnvironment env) {
            String[] fields = new String[]{"id"};
            String text = "@name('s0') select id from SupportBean_S0(id in (select id from SupportBean_S1#length(2)))";
            env.compileDeployAddListenerMileZero(text, "s0");

            env.sendEventBean(new SupportBean_S0(1));
            assertFalse(env.listener("s0").isInvoked());

            env.sendEventBean(new SupportBean_S1(10));

            env.sendEventBean(new SupportBean_S0(10));
            EPAssertionUtil.assertProps(env.listener("s0").assertOneGetNewAndReset(), fields, new Object[]{10});
            env.sendEventBean(new SupportBean_S0(11));
            assertFalse(env.listener("s0").isInvoked());

            env.milestone(1);

            env.sendEventBean(new SupportBean_S0(10));
            EPAssertionUtil.assertProps(env.listener("s0").assertOneGetNewAndReset(), fields, new Object[]{10});
            env.sendEventBean(new SupportBean_S0(11));
            assertFalse(env.listener("s0").isInvoked());

            env.sendEventBean(new SupportBean_S1(11));
            env.sendEventBean(new SupportBean_S0(11));
            EPAssertionUtil.assertProps(env.listener("s0").assertOneGetNewAndReset(), fields, new Object[]{11});

            env.milestone(2);

            env.sendEventBean(new SupportBean_S0(11));
            EPAssertionUtil.assertProps(env.listener("s0").assertOneGetNewAndReset(), fields, new Object[]{11});

            env.sendEventBean(new SupportBean_S1(12));   //pushing 10 out

            env.milestone(3);

            env.sendEventBean(new SupportBean_S0(10));
            assertFalse(env.listener("s0").isInvoked());
            env.sendEventBean(new SupportBean_S0(11));
            EPAssertionUtil.assertProps(env.listener("s0").assertOneGetNewAndReset(), fields, new Object[]{11});
            env.sendEventBean(new SupportBean_S0(12));
            EPAssertionUtil.assertProps(env.listener("s0").assertOneGetNewAndReset(), fields, new Object[]{12});

            env.undeployAll();
        }
    }

    private static class EPLSubselectInSelectWhere implements RegressionExecution {
        public void run(RegressionEnvironment env) {
            String stmtText = "@name('s0') select id in (select id from SupportBean_S1#length(1000) where id > 0) as value from SupportBean_S0";

            env.compileDeployAddListenerMileZero(stmtText, "s0");

            env.sendEventBean(new SupportBean_S0(2));
            assertEquals(false, env.listener("s0").assertOneGetNewAndReset().get("value"));

            env.sendEventBean(new SupportBean_S1(-1));
            env.sendEventBean(new SupportBean_S0(2));
            assertEquals(false, env.listener("s0").assertOneGetNewAndReset().get("value"));

            env.sendEventBean(new SupportBean_S0(-1));
            assertEquals(false, env.listener("s0").assertOneGetNewAndReset().get("value"));

            env.sendEventBean(new SupportBean_S1(5));
            env.sendEventBean(new SupportBean_S0(4));
            assertEquals(false, env.listener("s0").assertOneGetNewAndReset().get("value"));

            env.sendEventBean(new SupportBean_S0(5));
            assertEquals(true, env.listener("s0").assertOneGetNewAndReset().get("value"));

            env.undeployAll();
        }
    }

    private static class EPLSubselectInSelectWhereExpressions implements RegressionExecution {
        public void run(RegressionEnvironment env) {
            String stmtText = "@name('s0') select 3*id in (select 2*id from SupportBean_S1#length(1000)) as value from SupportBean_S0";

            env.compileDeployAddListenerMileZero(stmtText, "s0");

            env.sendEventBean(new SupportBean_S0(2));
            assertEquals(false, env.listener("s0").assertOneGetNewAndReset().get("value"));

            env.sendEventBean(new SupportBean_S1(-1));
            env.sendEventBean(new SupportBean_S0(2));
            assertEquals(false, env.listener("s0").assertOneGetNewAndReset().get("value"));

            env.sendEventBean(new SupportBean_S0(-1));
            assertEquals(false, env.listener("s0").assertOneGetNewAndReset().get("value"));

            env.sendEventBean(new SupportBean_S1(6));
            env.sendEventBean(new SupportBean_S0(4));
            assertEquals(true, env.listener("s0").assertOneGetNewAndReset().get("value"));

            env.undeployAll();
        }
    }

    private static class EPLSubselectInWildcard implements RegressionExecution {
        public void run(RegressionEnvironment env) {
            String stmtText = "@name('s0') select s0.anyObject in (select * from SupportBean_S1#length(1000)) as value from SupportBeanArrayCollMap s0";
            env.compileDeployAddListenerMileZero(stmtText, "s0");

            SupportBean_S1 s1 = new SupportBean_S1(100);
            SupportBeanArrayCollMap arrayBean = new SupportBeanArrayCollMap(s1);
            env.sendEventBean(s1);
            env.sendEventBean(arrayBean);
            assertEquals(true, env.listener("s0").assertOneGetNewAndReset().get("value"));

            SupportBean_S2 s2 = new SupportBean_S2(100);
            arrayBean.setAnyObject(s2);
            env.sendEventBean(s2);
            env.sendEventBean(arrayBean);
            assertEquals(false, env.listener("s0").assertOneGetNewAndReset().get("value"));

            env.undeployAll();
        }
    }

    private static class EPLSubselectInNullable implements RegressionExecution {
        public void run(RegressionEnvironment env) {
            String stmtText = "@name('s0') select id from SupportBean_S0 as s0 where p00 in (select p10 from SupportBean_S1#length(1000))";

            env.compileDeployAddListenerMileZero(stmtText, "s0");

            env.sendEventBean(new SupportBean_S0(1, "a"));
            assertFalse(env.listener("s0").isInvoked());

            env.sendEventBean(new SupportBean_S0(2, null));
            assertFalse(env.listener("s0").isInvoked());

            env.sendEventBean(new SupportBean_S1(-1, "A"));
            env.sendEventBean(new SupportBean_S0(3, null));
            assertFalse(env.listener("s0").isInvoked());

            env.sendEventBean(new SupportBean_S0(4, "A"));
            assertEquals(4, env.listener("s0").assertOneGetNewAndReset().get("id"));

            env.sendEventBean(new SupportBean_S1(-2, null));
            env.sendEventBean(new SupportBean_S0(5, null));
            assertFalse(env.listener("s0").isInvoked());

            env.undeployAll();
        }
    }

    private static class EPLSubselectInNullableCoercion implements RegressionExecution {
        public void run(RegressionEnvironment env) {
            String stmtText = "@name('s0') select longBoxed from SupportBean(theString='A') as s0 " +
                "where longBoxed in " +
                "(select intBoxed from SupportBean(theString='B')#length(1000))";

            env.compileDeployAddListenerMileZero(stmtText, "s0");

            sendBean(env, "A", 0, 0L);
            sendBean(env, "A", null, null);
            assertFalse(env.listener("s0").isInvoked());

            sendBean(env, "B", null, null);

            sendBean(env, "A", 0, 0L);
            assertFalse(env.listener("s0").isInvoked());
            sendBean(env, "A", null, null);
            assertFalse(env.listener("s0").isInvoked());

            sendBean(env, "B", 99, null);

            sendBean(env, "A", null, null);
            assertFalse(env.listener("s0").isInvoked());
            sendBean(env, "A", null, 99L);
            assertEquals(99L, env.listener("s0").assertOneGetNewAndReset().get("longBoxed"));

            sendBean(env, "B", 98, null);

            sendBean(env, "A", null, 98L);
            assertEquals(98L, env.listener("s0").assertOneGetNewAndReset().get("longBoxed"));

            env.undeployAll();
        }
    }

    private static class EPLSubselectInNullRow implements RegressionExecution {
        public void run(RegressionEnvironment env) {
            String stmtText = "@name('s0') select intBoxed from SupportBean(theString='A') as s0 " +
                "where intBoxed in " +
                "(select longBoxed from SupportBean(theString='B')#length(1000))";

            env.compileDeployAddListenerMileZero(stmtText, "s0");

            sendBean(env, "B", 1, 1L);

            sendBean(env, "A", null, null);
            assertFalse(env.listener("s0").isInvoked());

            sendBean(env, "A", 1, 1L);
            assertEquals(1, env.listener("s0").assertOneGetNewAndReset().get("intBoxed"));

            sendBean(env, "B", null, null);

            sendBean(env, "A", null, null);
            assertFalse(env.listener("s0").isInvoked());

            sendBean(env, "A", 1, 1L);
            assertEquals(1, env.listener("s0").assertOneGetNewAndReset().get("intBoxed"));

            env.undeployAll();
        }
    }

    public static class EPLSubselectInSingleIndex implements RegressionExecution {
        public void run(RegressionEnvironment env) {
            String epl = "@Name('s0') select (select p00 from SupportBean_S0#keepall() as s0 where s0.p01 in (s1.p10, s1.p11)) as c0 from SupportBean_S1 as s1";
            env.compileDeploy(epl).addListener("s0");

            for (int i = 0; i < 10; i++) {
                env.sendEventBean(new SupportBean_S0(i, "v" + i, "p00_" + i));
            }

            env.milestone(0);

            for (int i = 0; i < 5; i++) {
                int index = i + 4;
                env.sendEventBean(new SupportBean_S1(index, "x", "p00_" + index));
                assertEquals("v" + index, env.listener("s0").assertOneGetNewAndReset().get("c0"));
            }

            env.undeployAll();
        }
    }

    public static class EPLSubselectInMultiIndex implements RegressionExecution {
        public void run(RegressionEnvironment env) {
            String epl = "@name('s0') select (select p00 from SupportBean_S0#keepall() as s0 where s1.p11 in (s0.p00, s0.p01)) as c0 from SupportBean_S1 as s1";
            env.compileDeploy(epl).addListener("s0");

            for (int i = 0; i < 10; i++) {
                env.sendEventBean(new SupportBean_S0(i, "v" + i, "p00_" + i));
            }

            env.milestone(0);

            for (int i = 0; i < 5; i++) {
                int index = i + 4;
                env.sendEventBean(new SupportBean_S1(index, "x", "p00_" + index));
                assertEquals("v" + index, env.listener("s0").assertOneGetNewAndReset().get("c0"));
            }

            env.undeployAll();
        }
    }

    private static class EPLSubselectNotInNullRow implements RegressionExecution {
        public void run(RegressionEnvironment env) {
            String stmtText = "@name('s0') select intBoxed from SupportBean(theString='A') as s0 " +
                "where intBoxed not in " +
                "(select longBoxed from SupportBean(theString='B')#length(1000))";

            env.compileDeployAddListenerMileZero(stmtText, "s0");

            sendBean(env, "B", 1, 1L);

            sendBean(env, "A", null, null);
            assertFalse(env.listener("s0").isInvoked());

            sendBean(env, "A", 1, 1L);
            assertFalse(env.listener("s0").isInvoked());

            sendBean(env, "B", null, null);

            sendBean(env, "A", null, null);
            assertFalse(env.listener("s0").isInvoked());

            sendBean(env, "A", 1, 1L);
            assertFalse(env.listener("s0").isInvoked());

            env.undeployAll();
        }
    }

    private static class EPLSubselectNotInSelect implements RegressionExecution {
        public void run(RegressionEnvironment env) {
            String stmtText = "@name('s0') select not id in (select id from SupportBean_S1#length(1000)) as value from SupportBean_S0";

            env.compileDeployAddListenerMileZero(stmtText, "s0");

            env.sendEventBean(new SupportBean_S0(2));
            assertEquals(true, env.listener("s0").assertOneGetNewAndReset().get("value"));

            env.sendEventBean(new SupportBean_S1(-1));
            env.sendEventBean(new SupportBean_S0(2));
            assertEquals(true, env.listener("s0").assertOneGetNewAndReset().get("value"));

            env.sendEventBean(new SupportBean_S0(-1));
            assertEquals(false, env.listener("s0").assertOneGetNewAndReset().get("value"));

            env.sendEventBean(new SupportBean_S1(5));
            env.sendEventBean(new SupportBean_S0(4));
            assertEquals(true, env.listener("s0").assertOneGetNewAndReset().get("value"));

            env.sendEventBean(new SupportBean_S0(5));
            assertEquals(false, env.listener("s0").assertOneGetNewAndReset().get("value"));

            env.undeployAll();
        }
    }

    private static class EPLSubselectNotInNullableCoercion implements RegressionExecution {
        public void run(RegressionEnvironment env) {
            String stmtText = "@name('s0') select longBoxed from SupportBean(theString='A') as s0 " +
                "where longBoxed not in " +
                "(select intBoxed from SupportBean(theString='B')#length(1000))";

            env.compileDeployAddListenerMileZero(stmtText, "s0");

            sendBean(env, "A", 0, 0L);
            assertEquals(0L, env.listener("s0").assertOneGetNewAndReset().get("longBoxed"));

            sendBean(env, "A", null, null);
            assertEquals(null, env.listener("s0").assertOneGetNewAndReset().get("longBoxed"));

            sendBean(env, "B", null, null);

            sendBean(env, "A", 1, 1L);
            assertFalse(env.listener("s0").isInvoked());
            sendBean(env, "A", null, null);
            assertFalse(env.listener("s0").isInvoked());

            sendBean(env, "B", 99, null);

            sendBean(env, "A", null, null);
            assertFalse(env.listener("s0").isInvoked());
            sendBean(env, "A", null, 99L);
            assertFalse(env.listener("s0").isInvoked());

            sendBean(env, "B", 98, null);

            sendBean(env, "A", null, 98L);
            assertFalse(env.listener("s0").isInvoked());

            sendBean(env, "A", null, 97L);
            assertFalse(env.listener("s0").isInvoked());

            env.undeployAll();
        }
    }

    private static void runTestInSelect(RegressionEnvironment env) {
        env.sendEventBean(new SupportBean_S0(2));
        assertEquals(false, env.listener("s0").assertOneGetNewAndReset().get("value"));

        env.sendEventBean(new SupportBean_S1(-1));
        env.sendEventBean(new SupportBean_S0(2));
        assertEquals(false, env.listener("s0").assertOneGetNewAndReset().get("value"));

        env.sendEventBean(new SupportBean_S0(-1));
        assertEquals(true, env.listener("s0").assertOneGetNewAndReset().get("value"));

        env.sendEventBean(new SupportBean_S1(5));
        env.sendEventBean(new SupportBean_S0(4));
        assertEquals(false, env.listener("s0").assertOneGetNewAndReset().get("value"));

        env.sendEventBean(new SupportBean_S0(5));
        assertEquals(true, env.listener("s0").assertOneGetNewAndReset().get("value"));
    }

    private static class EPLSubselectInvalid implements RegressionExecution {
        public void run(RegressionEnvironment env) {
            tryInvalidCompile(env, "@name('s0') select intArr in (select intPrimitive from SupportBean#keepall) as r1 from SupportBeanArrayCollMap",
                "Failed to validate select-clause expression subquery number 1 querying SupportBean: Collection or array comparison is not allowed for the IN, ANY, SOME or ALL keywords");
        }
    }

    private static void sendBean(RegressionEnvironment env, String theString, Integer intBoxed, Long longBoxed) {
        SupportBean bean = new SupportBean();
        bean.setTheString(theString);
        bean.setIntBoxed(intBoxed);
        bean.setLongBoxed(longBoxed);
        env.sendEventBean(bean);
    }
}
