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

import com.espertech.esper.common.client.soda.*;
import com.espertech.esper.common.internal.util.SerializableObjectCopier;
import com.espertech.esper.regressionlib.framework.RegressionEnvironment;
import com.espertech.esper.regressionlib.framework.RegressionExecution;
import com.espertech.esper.common.internal.support.SupportBean_S0;
import com.espertech.esper.common.internal.support.SupportBean_S1;
import com.espertech.esper.common.internal.support.SupportBean_S2;
import org.junit.Assert;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;

public class EPLSubselectExists {

    public static List<RegressionExecution> executions() {
        List<RegressionExecution> execs = new ArrayList<>();
        execs.add(new EPLSubselectExistsInSelect());
        execs.add(new EPLSubselectExistsInSelectOM());
        execs.add(new EPLSubselectExistsInSelectCompile());
        execs.add(new EPLSubselectExistsSceneOne());
        execs.add(new EPLSubselectExistsFiltered());
        execs.add(new EPLSubselectTwoExistsFiltered());
        execs.add(new EPLSubselectNotExistsOM());
        execs.add(new EPLSubselectNotExistsCompile());
        execs.add(new EPLSubselectNotExists());
        return execs;
    }

    private static class EPLSubselectExistsInSelect implements RegressionExecution {
        public void run(RegressionEnvironment env) {
            String stmtText = "@name('s0') select exists (select * from SupportBean_S1#length(1000)) as value from SupportBean_S0";
            env.compileDeployAddListenerMileZero(stmtText, "s0");

            runTestExistsInSelect(env);

            env.undeployAll();
        }
    }

    private static class EPLSubselectExistsInSelectOM implements RegressionExecution {
        public void run(RegressionEnvironment env) {
            EPStatementObjectModel subquery = new EPStatementObjectModel();
            subquery.setSelectClause(SelectClause.createWildcard());
            subquery.setFromClause(FromClause.create(FilterStream.create("SupportBean_S1").addView(View.create("length", Expressions.constant(1000)))));

            EPStatementObjectModel model = new EPStatementObjectModel();
            model.setFromClause(FromClause.create(FilterStream.create("SupportBean_S0")));
            model.setSelectClause(SelectClause.create().add(Expressions.subqueryExists(subquery), "value"));
            model = SerializableObjectCopier.copyMayFail(model);

            String stmtText = "select exists (select * from SupportBean_S1#length(1000)) as value from SupportBean_S0";
            Assert.assertEquals(stmtText, model.toEPL());

            model.setAnnotations(Collections.singletonList(AnnotationPart.nameAnnotation("s0")));
            env.compileDeploy(model).addListener("s0");

            runTestExistsInSelect(env);

            env.undeployAll();
        }
    }

    private static class EPLSubselectExistsInSelectCompile implements RegressionExecution {
        public void run(RegressionEnvironment env) {
            String stmtText = "@name('s0') select exists (select * from SupportBean_S1#length(1000)) as value from SupportBean_S0";
            env.eplToModelCompileDeploy(stmtText).addListener("s0").milestone(1);

            runTestExistsInSelect(env);

            env.undeployAll();
        }
    }

    private static class EPLSubselectExistsSceneOne implements RegressionExecution {
        public void run(RegressionEnvironment env) {
            String stmtText = "@name('s0') select id from SupportBean_S0 where exists (select * from SupportBean_S1#length(1000))";
            env.compileDeploy(stmtText).addListener("s0");

            env.sendEventBean(new SupportBean_S0(2));
            assertFalse(env.listener("s0").isInvoked());

            env.sendEventBean(new SupportBean_S1(-1));
            env.sendEventBean(new SupportBean_S0(2));
            Assert.assertEquals(2, env.listener("s0").assertOneGetNewAndReset().get("id"));

            env.sendEventBean(new SupportBean_S1(-2));
            env.sendEventBean(new SupportBean_S0(3));
            Assert.assertEquals(3, env.listener("s0").assertOneGetNewAndReset().get("id"));

            env.undeployAll();
        }
    }

    private static class EPLSubselectExistsFiltered implements RegressionExecution {
        public void run(RegressionEnvironment env) {
            String stmtText = "@name('s0') select id from SupportBean_S0 as s0 where exists (select * from SupportBean_S1#length(1000) as s1 where s1.id=s0.id)";
            env.compileDeploy(stmtText).addListener("s0");

            env.sendEventBean(new SupportBean_S0(2));
            assertFalse(env.listener("s0").isInvoked());

            env.sendEventBean(new SupportBean_S1(-1));
            env.sendEventBean(new SupportBean_S0(2));
            assertFalse(env.listener("s0").isInvoked());

            env.sendEventBean(new SupportBean_S1(-2));
            env.sendEventBean(new SupportBean_S0(-2));
            Assert.assertEquals(-2, env.listener("s0").assertOneGetNewAndReset().get("id"));

            env.sendEventBean(new SupportBean_S1(1));
            env.sendEventBean(new SupportBean_S1(2));
            env.sendEventBean(new SupportBean_S1(3));
            env.sendEventBean(new SupportBean_S0(3));
            Assert.assertEquals(3, env.listener("s0").assertOneGetNewAndReset().get("id"));

            env.undeployAll();
        }
    }

    private static class EPLSubselectTwoExistsFiltered implements RegressionExecution {
        public void run(RegressionEnvironment env) {
            String stmtText = "@name('s0') select id from SupportBean_S0 as s0 where " +
                "exists (select * from SupportBean_S1#length(1000) as s1 where s1.id=s0.id) " +
                "and " +
                "exists (select * from SupportBean_S2#length(1000) as s2 where s2.id=s0.id) ";
            env.compileDeploy(stmtText).addListener("s0");

            env.sendEventBean(new SupportBean_S0(2));
            assertFalse(env.listener("s0").isInvoked());

            env.sendEventBean(new SupportBean_S2(3));
            env.sendEventBean(new SupportBean_S0(3));
            assertFalse(env.listener("s0").isInvoked());

            env.sendEventBean(new SupportBean_S1(3));
            env.sendEventBean(new SupportBean_S0(3));
            Assert.assertEquals(3, env.listener("s0").assertOneGetNewAndReset().get("id"));

            env.sendEventBean(new SupportBean_S1(1));
            env.sendEventBean(new SupportBean_S1(2));
            env.sendEventBean(new SupportBean_S2(1));
            env.sendEventBean(new SupportBean_S0(1));
            Assert.assertEquals(1, env.listener("s0").assertOneGetNewAndReset().get("id"));

            env.sendEventBean(new SupportBean_S0(2));
            env.sendEventBean(new SupportBean_S0(0));
            assertFalse(env.listener("s0").isInvoked());

            env.undeployAll();
        }
    }

    private static class EPLSubselectNotExistsOM implements RegressionExecution {
        public void run(RegressionEnvironment env) {
            EPStatementObjectModel subquery = new EPStatementObjectModel();
            subquery.setSelectClause(SelectClause.createWildcard());
            subquery.setFromClause(FromClause.create(FilterStream.create("SupportBean_S1").addView("length", Expressions.constant(1000))));

            EPStatementObjectModel model = new EPStatementObjectModel();
            model.setSelectClause(SelectClause.create("id"));
            model.setFromClause(FromClause.create(FilterStream.create("SupportBean_S0")));
            model.setWhereClause(Expressions.not(Expressions.subqueryExists(subquery)));
            model = SerializableObjectCopier.copyMayFail(model);

            String stmtText = "select id from SupportBean_S0 where not exists (select * from SupportBean_S1#length(1000))";
            Assert.assertEquals(stmtText, model.toEPL());

            model.setAnnotations(Collections.singletonList(AnnotationPart.nameAnnotation("s0")));
            env.compileDeploy(model).addListener("s0");

            env.sendEventBean(new SupportBean_S0(2));
            Assert.assertEquals(2, env.listener("s0").assertOneGetNewAndReset().get("id"));

            env.sendEventBean(new SupportBean_S1(-1));
            env.sendEventBean(new SupportBean_S0(1));
            assertFalse(env.listener("s0").isInvoked());

            env.sendEventBean(new SupportBean_S1(-2));
            env.sendEventBean(new SupportBean_S0(3));
            assertFalse(env.listener("s0").isInvoked());

            env.undeployAll();
        }
    }

    private static class EPLSubselectNotExistsCompile implements RegressionExecution {
        public void run(RegressionEnvironment env) {
            String stmtText = "@name('s0') select id from SupportBean_S0 where not exists (select * from SupportBean_S1#length(1000))";
            env.eplToModelCompileDeploy(stmtText).addListener("s0");

            env.sendEventBean(new SupportBean_S0(2));
            Assert.assertEquals(2, env.listener("s0").assertOneGetNewAndReset().get("id"));

            env.sendEventBean(new SupportBean_S1(-1));
            env.sendEventBean(new SupportBean_S0(1));
            assertFalse(env.listener("s0").isInvoked());

            env.sendEventBean(new SupportBean_S1(-2));
            env.sendEventBean(new SupportBean_S0(3));
            assertFalse(env.listener("s0").isInvoked());

            env.undeployAll();
        }
    }

    private static class EPLSubselectNotExists implements RegressionExecution {
        public void run(RegressionEnvironment env) {
            String stmtText = "@name('s0') select id from SupportBean_S0 where not exists (select * from SupportBean_S1#length(1000))";
            env.compileDeployAddListenerMileZero(stmtText, "s0");

            env.sendEventBean(new SupportBean_S0(2));
            Assert.assertEquals(2, env.listener("s0").assertOneGetNewAndReset().get("id"));

            env.sendEventBean(new SupportBean_S1(-1));
            env.sendEventBean(new SupportBean_S0(1));
            assertFalse(env.listener("s0").isInvoked());

            env.sendEventBean(new SupportBean_S1(-2));
            env.sendEventBean(new SupportBean_S0(3));
            assertFalse(env.listener("s0").isInvoked());

            env.undeployAll();
        }
    }

    private static void runTestExistsInSelect(RegressionEnvironment env) {
        env.sendEventBean(new SupportBean_S0(2));
        assertEquals(false, env.listener("s0").assertOneGetNewAndReset().get("value"));

        env.sendEventBean(new SupportBean_S1(-1));
        env.sendEventBean(new SupportBean_S0(2));
        assertEquals(true, env.listener("s0").assertOneGetNewAndReset().get("value"));
    }
}
