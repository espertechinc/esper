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
package com.espertech.esper.regressionlib.suite.infra.tbl;

import com.espertech.esper.common.client.EPCompiled;
import com.espertech.esper.common.client.EPException;
import com.espertech.esper.common.client.fireandforget.EPFireAndForgetQueryResult;
import com.espertech.esper.common.client.scopetest.EPAssertionUtil;
import com.espertech.esper.compiler.client.EPCompileException;
import com.espertech.esper.regressionlib.framework.RegressionEnvironment;
import com.espertech.esper.regressionlib.framework.RegressionExecution;
import com.espertech.esper.regressionlib.framework.RegressionPath;
import com.espertech.esper.regressionlib.framework.SupportMessageAssertUtil;
import com.espertech.esper.common.internal.support.SupportBean;
import com.espertech.esper.common.internal.support.SupportBean_S0;
import com.espertech.esper.common.internal.support.SupportBean_S1;
import com.espertech.esper.runtime.client.EPDeployException;

import java.util.ArrayList;
import java.util.Collection;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.fail;

/**
 * NOTE: More table-related tests in "nwtable"
 */
public class InfraTableUpdateAndIndex {

    public static Collection<RegressionExecution> executions() {
        ArrayList<RegressionExecution> execs = new ArrayList<>();
        execs.add(new InfraEarlyUniqueIndexViolation());
        execs.add(new InfraLateUniqueIndexViolation());
        execs.add(new InfraFAFUpdate());
        execs.add(new InfraTableKeyUpdateSingleKey());
        execs.add(new InfraTableKeyUpdateMultiKey());
        return execs;
    }

    private static class InfraEarlyUniqueIndexViolation implements RegressionExecution {
        public void run(RegressionEnvironment env) {
            RegressionPath path = new RegressionPath();
            env.compileDeploy("@name('create') create table MyTableEUIV as (pkey0 string primary key, pkey1 int primary key, thecnt count(*))", path);
            env.compileDeploy("into table MyTableEUIV select count(*) as thecnt from SupportBean group by theString, intPrimitive", path);
            env.sendEventBean(new SupportBean("E1", 10));
            env.sendEventBean(new SupportBean("E1", 20));

            // invalid index being created
            try {
                EPCompiled compiled = env.compile("create unique index SecIndex on MyTableEUIV(pkey0)", path);
                env.runtime().getDeploymentService().deploy(compiled);
                fail();
            } catch (EPDeployException ex) {
                SupportMessageAssertUtil.assertMessage(ex.getMessage(), "Failed to deploy: Unique index violation, index 'SecIndex' is a unique index and key 'E1' already exists");
            }

            // try fire-and-forget update of primary key to non-unique value
            try {
                env.compileExecuteFAF("update MyTableEUIV set pkey1 = 0", path);
                fail();
            } catch (EPException ex) {
                SupportMessageAssertUtil.assertMessage(ex, "Unique index violation, index 'MyTableEUIV' is a unique index and key 'MultiKey[E1,0]' already exists");
                // assert events are unchanged - no update actually performed
                EPAssertionUtil.assertPropsPerRowAnyOrder(env.iterator("create"), "pkey0,pkey1".split(","), new Object[][]{{"E1", 10}, {"E1", 20}});
            }

            // try on-update unique index violation
            env.compileDeploy("@name('on-update') on SupportBean_S1 update MyTableEUIV set pkey1 = 0", path);
            try {
                env.sendEventBean(new SupportBean_S1(0));
                fail();
            } catch (EPException ex) {
                SupportMessageAssertUtil.assertMessage(ex.getCause(), "Unexpected exception in statement 'on-update': Unique index violation, index 'MyTableEUIV' is a unique index and key 'MultiKey[E1,0]' already exists");
                // assert events are unchanged - no update actually performed
                EPAssertionUtil.assertPropsPerRowAnyOrder(env.statement("create").iterator(), "pkey0,pkey1".split(","), new Object[][]{{"E1", 10}, {"E1", 20}});
            }

            // disallow on-merge unique key updates
            try {
                env.compileWCheckedEx("@name('on-merge') on SupportBean_S1 merge MyTableEUIV when matched then update set pkey1 = 0", path);
                fail();
            } catch (EPCompileException ex) {
                SupportMessageAssertUtil.assertMessage(ex.getCause(), "Validation failed in when-matched (clause 1): On-merge statements may not update unique keys of tables");
            }

            env.undeployAll();
        }
    }

    private static class InfraLateUniqueIndexViolation implements RegressionExecution {
        public void run(RegressionEnvironment env) {
            RegressionPath path = new RegressionPath();
            env.compileDeploy("@name('create') create table MyTableLUIV as (" +
                "pkey0 string primary key, " +
                "pkey1 int primary key, " +
                "col0 int, " +
                "thecnt count(*))", path);

            env.compileDeploy("into table MyTableLUIV select count(*) as thecnt from SupportBean group by theString, intPrimitive", path);
            env.sendEventBean(new SupportBean("E1", 10));
            env.sendEventBean(new SupportBean("E2", 20));

            // On-merge exists before creating a unique index
            env.compileDeploy("@name('on-merge') on SupportBean_S1 merge MyTableLUIV " +
                "when matched then update set col0 = 0", path);
            try {
                EPCompiled compiled = env.compile("create unique index MyUniqueSecondary on MyTableLUIV (col0)", path);
                path.getCompileds().remove(compiled);
                env.runtime().getDeploymentService().deploy(compiled);
                fail();
            } catch (EPDeployException ex) {
                SupportMessageAssertUtil.assertMessage(ex, "Failed to deploy: Create-index adds a unique key on columns that are updated by one or more on-merge statements");
            }
            env.undeployModuleContaining("on-merge");

            // on-update exists before creating a unique index
            env.compileDeploy("@name('on-update') on SupportBean_S1 update MyTableLUIV set pkey1 = 0", path);
            env.compileDeploy("create unique index MyUniqueSecondary on MyTableLUIV (pkey1)", path);
            try {
                env.sendEventBean(new SupportBean_S1(0));
                fail();
            } catch (EPException ex) {
                SupportMessageAssertUtil.assertMessage(ex.getCause(), "Unexpected exception in statement 'on-update': Unique index violation, index 'MyUniqueSecondary' is a unique index and key '0' already exists");
                // assert events are unchanged - no update actually performed
                EPAssertionUtil.assertPropsPerRowAnyOrder(env.iterator("create"), "pkey0,pkey1".split(","), new Object[][]{{"E1", 10}, {"E2", 20}});
            }

            // unregister
            env.undeployModuleContaining("on-update");
            env.undeployAll();
        }
    }

    private static class InfraFAFUpdate implements RegressionExecution {
        public void run(RegressionEnvironment env) {
            RegressionPath path = new RegressionPath();
            env.compileDeploy("create table MyTableFAFU as (pkey0 string primary key, col0 int, col1 int, thecnt count(*))", path);
            env.compileDeploy("create index MyIndex on MyTableFAFU(col0)", path);

            env.compileDeploy("into table MyTableFAFU select count(*) as thecnt from SupportBean group by theString", path);
            env.sendEventBean(new SupportBean("E1", 0));
            env.sendEventBean(new SupportBean("E2", 0));

            env.compileExecuteFAF("update MyTableFAFU set col0 = 1 where pkey0='E1'", path);
            env.compileExecuteFAF("update MyTableFAFU set col0 = 2 where pkey0='E2'", path);
            assertFAFOneRowResult(env, path, "select pkey0 from MyTableFAFU where col0=1", "pkey0", new Object[]{"E1"});

            env.compileExecuteFAF("update MyTableFAFU set col1 = 100 where pkey0='E1'", path);
            assertFAFOneRowResult(env, path, "select pkey0 from MyTableFAFU where col1=100", "pkey0", new Object[]{"E1"});

            env.undeployAll();
        }
    }

    private static class InfraTableKeyUpdateMultiKey implements RegressionExecution {
        public void run(RegressionEnvironment env) {
            String[] fields = "pkey0,pkey1,c0".split(",");
            RegressionPath path = new RegressionPath();
            env.compileDeploy("@name('s1') create table MyTableMultiKey(pkey0 string primary key, pkey1 int primary key, c0 long)", path);
            env.compileDeploy("insert into MyTableMultiKey select theString as pkey0, intPrimitive as pkey1, longPrimitive as c0 from SupportBean", path);
            env.compileDeploy("on SupportBean_S0 update MyTableMultiKey set pkey0 = p01 where pkey0 = p00", path);

            sendSupportBean(env, "E1", 10, 100);
            sendSupportBean(env, "E2", 20, 200);
            sendSupportBean(env, "E3", 30, 300);

            env.milestone(0);

            env.sendEventBean(new SupportBean_S0(0, "E2", "E20"));
            EPAssertionUtil.assertPropsPerRowAnyOrder(env.iterator("s1"), fields, new Object[][]{{"E1", 10, 100L}, {"E20", 20, 200L}, {"E3", 30, 300L}});

            env.milestone(1);

            env.sendEventBean(new SupportBean_S0(0, "E1", "E10"));

            env.milestone(2);

            EPAssertionUtil.assertPropsPerRowAnyOrder(env.iterator("s1"), fields, new Object[][]{{"E10", 10, 100L}, {"E20", 20, 200L}, {"E3", 30, 300L}});

            env.sendEventBean(new SupportBean_S0(0, "E3", "E30"));

            env.milestone(3);

            EPAssertionUtil.assertPropsPerRowAnyOrder(env.iterator("s1"), fields, new Object[][]{{"E10", 10, 100L}, {"E20", 20, 200L}, {"E30", 30, 300L}});

            env.undeployAll();
        }
    }

    private static class InfraTableKeyUpdateSingleKey implements RegressionExecution {
        public void run(RegressionEnvironment env) {

            String[] fields = "pkey0,c0".split(",");
            RegressionPath path = new RegressionPath();
            env.compileDeploy("@name('s0') create table MyTableSingleKey(pkey0 string primary key, c0 int)", path);
            env.compileDeploy("insert into MyTableSingleKey select theString as pkey0, intPrimitive as c0 from SupportBean", path);
            env.compileDeploy("on SupportBean_S0 update MyTableSingleKey set pkey0 = p01 where pkey0 = p00", path);

            sendSupportBean(env, "E1", 10);
            sendSupportBean(env, "E2", 20);
            sendSupportBean(env, "E3", 30);

            env.milestone(0);

            env.sendEventBean(new SupportBean_S0(0, "E2", "E20"));
            EPAssertionUtil.assertPropsPerRowAnyOrder(env.iterator("s0"), fields, new Object[][]{{"E1", 10}, {"E20", 20}, {"E3", 30}});

            env.milestone(1);

            env.sendEventBean(new SupportBean_S0(0, "E1", "E10"));

            env.milestone(2);

            EPAssertionUtil.assertPropsPerRowAnyOrder(env.iterator("s0"), fields, new Object[][]{{"E10", 10}, {"E20", 20}, {"E3", 30}});

            env.sendEventBean(new SupportBean_S0(0, "E3", "E30"));

            env.milestone(3);

            EPAssertionUtil.assertPropsPerRowAnyOrder(env.iterator("s0"), fields, new Object[][]{{"E10", 10}, {"E20", 20}, {"E30", 30}});

            env.undeployAll();
        }
    }

    private static void sendSupportBean(RegressionEnvironment env, String string, int intPrimitive, long longPrimitive) {
        SupportBean bean = new SupportBean(string, intPrimitive);
        bean.setLongPrimitive(longPrimitive);
        env.sendEventBean(bean);
    }

    private static void sendSupportBean(RegressionEnvironment env, String string, int intPrimitive) {
        SupportBean bean = new SupportBean(string, intPrimitive);
        env.sendEventBean(bean);
    }

    private static void assertFAFOneRowResult(RegressionEnvironment env, RegressionPath path, String epl, String fields, Object[] objects) {
        EPFireAndForgetQueryResult result = env.compileExecuteFAF(epl, path);
        assertEquals(1, result.getArray().length);
        EPAssertionUtil.assertProps(result.getArray()[0], fields.split(","), objects);
    }
}
