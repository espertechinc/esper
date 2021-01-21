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
package com.espertech.esper.regressionlib.suite.epl.other;

import com.espertech.esper.common.client.scopetest.EPAssertionUtil;
import com.espertech.esper.common.internal.support.SupportBean;
import com.espertech.esper.common.internal.support.SupportBean_S0;
import com.espertech.esper.common.internal.support.SupportBean_S1;
import com.espertech.esper.regressionlib.framework.RegressionEnvironment;
import com.espertech.esper.regressionlib.framework.RegressionExecution;
import com.espertech.esper.regressionlib.framework.RegressionFlag;
import com.espertech.esper.regressionlib.framework.RegressionPath;

import java.util.ArrayList;
import java.util.EnumSet;
import java.util.List;

import static org.junit.Assert.assertEquals;

public class EPLOtherAsKeywordBacktick {
    public static List<RegressionExecution> executions() {
        List<RegressionExecution> execs = new ArrayList<>();
        execs.add(new EPLOtherFAFUpdateDelete());
        execs.add(new EPLOtherFromClause());
        execs.add(new EPLOtherOnTrigger());
        execs.add(new EPLOtherUpdateIStream());
        execs.add(new EPLOthernMergeAndUpdateAndSelect());
        execs.add(new EPLOtherSubselect());
        execs.add(new EPLOtherOnSelectProperty());
        return execs;
    }

    private static class EPLOtherOnSelectProperty implements RegressionExecution {
        public void run(RegressionEnvironment env) {
            String stmtText = "on OrderBean insert into ABC select * " +
                "insert into DEF select `order`.reviewId from [books][reviews] `order`";
            env.compileDeploy(stmtText);
            env.undeployAll();
        }
    }

    private static class EPLOtherSubselect implements RegressionExecution {
        public void run(RegressionEnvironment env) {
            String epl = "@name('s0') select (select `order`.p00 from SupportBean_S0#lastevent as `order`) as c0 from SupportBean_S1";
            env.compileDeploy(epl).addListener("s0");

            env.sendEventBean(new SupportBean_S0(1, "A"));
            env.sendEventBean(new SupportBean_S1(2));
            env.assertEqualsNew("s0", "c0", "A");

            env.undeployAll();
        }
    }

    private static class EPLOthernMergeAndUpdateAndSelect implements RegressionExecution {
        public void run(RegressionEnvironment env) {
            RegressionPath path = new RegressionPath();
            env.compileDeploy("@public create window MyWindowMerge#keepall as (p0 string, p1 string)", path);
            env.compileExecuteFAFNoResult("insert into MyWindowMerge select 'a' as p0, 'b' as p1", path);
            env.compileDeploy("on SupportBean_S0 merge MyWindowMerge as `order` when matched then update set `order`.p1 = `order`.p0", path);
            env.compileDeploy("on SupportBean_S1 update MyWindowMerge as `order` set p0 = 'x'", path);

            assertFAF(env, path, "MyWindowMerge", "a", "b");

            env.sendEventBean(new SupportBean_S0(1));
            assertFAF(env, path, "MyWindowMerge", "a", "a");

            env.milestone(0);

            env.sendEventBean(new SupportBean_S1(1, "x"));
            assertFAF(env, path, "MyWindowMerge", "x", "a");

            env.compileDeploy("@name('s0') on SupportBean select `order`.p0 as c0 from MyWindowMerge as `order`", path).addListener("s0");

            env.sendEventBean(new SupportBean());
            env.assertEqualsNew("s0", "c0", "x");

            env.undeployAll();
        }

        public EnumSet<RegressionFlag> flags() {
            return EnumSet.of(RegressionFlag.FIREANDFORGET);
        }
    }

    private static class EPLOtherFAFUpdateDelete implements RegressionExecution {
        public void run(RegressionEnvironment env) {
            RegressionPath path = new RegressionPath();
            env.compileDeploy("@public create window MyWindowFAF#keepall as (p0 string, p1 string)", path);
            env.compileExecuteFAF("insert into MyWindowFAF select 'a' as p0, 'b' as p1", path);
            assertFAF(env, path, "MyWindowFAF", "a", "b");

            env.compileExecuteFAF("update MyWindowFAF as `order` set `order`.p0 = `order`.p1", path);
            assertFAF(env, path, "MyWindowFAF", "b", "b");

            env.milestone(0);

            env.compileExecuteFAF("delete from MyWindowFAF as `order` where `order`.p0 = 'b'", path);
            assertEquals(0, env.compileExecuteFAF("select * from MyWindowFAF", path).getArray().length);

            env.undeployAll();
        }

        public EnumSet<RegressionFlag> flags() {
            return EnumSet.of(RegressionFlag.FIREANDFORGET);
        }
    }

    private static class EPLOtherUpdateIStream implements RegressionExecution {
        public void run(RegressionEnvironment env) {
            env.compileDeploy("update istream SupportBean_S0 as `order` set p00=`order`.p01");
            String epl = "@name('s0') select * from SupportBean_S0";
            env.compileDeploy(epl).addListener("s0");

            env.sendEventBean(new SupportBean_S0(1, "a", "x"));
            env.assertEqualsNew("s0", "p00", "x");

            env.undeployAll();
        }
    }

    private static class EPLOtherOnTrigger implements RegressionExecution {
        public void run(RegressionEnvironment env) {
            RegressionPath path = new RegressionPath();
            env.compileDeploy("@public create table MyTable(k1 string primary key, v1 string)", path);
            env.compileExecuteFAFNoResult("insert into MyTable select 'x' as k1, 'y' as v1", path);
            env.compileExecuteFAFNoResult("insert into MyTable select 'a' as k1, 'b' as v1", path);

            String epl = "@name('s0') on SupportBean_S0 as `order` select v1 from MyTable where `order`.p00 = k1";
            env.compileDeploy(epl, path).addListener("s0");

            env.sendEventBean(new SupportBean_S0(1, "a"));
            env.assertEqualsNew("s0", "v1", "b");

            env.undeployAll();
        }
    }

    private static class EPLOtherFromClause implements RegressionExecution {
        public void run(RegressionEnvironment env) {
            String epl = "@name('s0') select * from SupportBean_S0#lastevent as `order`, SupportBean_S1#lastevent as `select`";
            env.compileDeploy(epl).addListener("s0");

            SupportBean_S0 s0 = new SupportBean_S0(1, "S0_1");
            env.sendEventBean(s0);

            env.milestone(0);

            SupportBean_S1 s1 = new SupportBean_S1(10, "S1_1");
            env.sendEventBean(s1);
            env.assertPropsNew("s0", "order,select,order.p00,select.p10".split(","), new Object[]{s0, s1, "S0_1", "S1_1"});

            env.undeployAll();
        }
    }

    private static void assertFAF(RegressionEnvironment env, RegressionPath path, String windowName, String p0, String p1) {
        EPAssertionUtil.assertProps(env.compileExecuteFAF("select * from " + windowName, path).getArray()[0], "p0,p1".split(","), new Object[]{p0, p1});
    }
}
