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

import com.espertech.esper.common.client.fireandforget.EPFireAndForgetQueryResult;
import com.espertech.esper.common.client.scopetest.EPAssertionUtil;
import com.espertech.esper.regressionlib.framework.RegressionEnvironment;
import com.espertech.esper.regressionlib.framework.RegressionExecution;
import com.espertech.esper.regressionlib.framework.RegressionPath;
import com.espertech.esper.common.internal.support.SupportBean;
import com.espertech.esper.regressionlib.support.util.IndexBackingTableInfo;
import com.espertech.esper.runtime.client.EPStatement;

import java.util.ArrayList;
import java.util.Collection;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertSame;

/**
 * NOTE: More table-related tests in "nwtable"
 */
public class InfraTableFAFExecuteQuery implements IndexBackingTableInfo {

    public static Collection<RegressionExecution> executions() {
        ArrayList<RegressionExecution> execs = new ArrayList<>();
        execs.add(new InfraFAFInsert());
        execs.add(new InfraFAFDelete());
        execs.add(new InfraFAFUpdate());
        execs.add(new InfraFAFSelect());
        return execs;
    }

    private static class InfraFAFInsert implements RegressionExecution {
        public void run(RegressionEnvironment env) {
            RegressionPath path = new RegressionPath();
            String[] propertyNames = "p0,p1".split(",");
            env.compileDeploy("@name('create') create table MyTableINS as (p0 string, p1 int)", path);

            String eplInsertInto = "insert into MyTableINS (p0, p1) select 'a', 1";
            EPFireAndForgetQueryResult resultOne = env.compileExecuteFAF(eplInsertInto, path);
            assertFAFInsertResult(resultOne, propertyNames, env.statement("create"));
            EPAssertionUtil.assertPropsPerRow(env.iterator("create"), propertyNames, new Object[][]{{"a", 1}});

            env.undeployAll();
        }
    }

    private static class InfraFAFDelete implements RegressionExecution {
        public void run(RegressionEnvironment env) {
            RegressionPath path = new RegressionPath();
            env.compileDeploy("@name('create') create table MyTableDEL as (p0 string primary key, thesum sum(int))", path);
            env.compileDeploy("into table MyTableDEL select theString, sum(intPrimitive) as thesum from SupportBean group by theString", path);
            for (int i = 0; i < 10; i++) {
                env.sendEventBean(new SupportBean("G" + i, i));
            }
            assertEquals(10L, getTableCount(env.statement("create")));
            env.compileExecuteFAF("delete from MyTableDEL", path);
            assertEquals(0L, getTableCount(env.statement("create")));

            env.undeployAll();
        }
    }

    private static class InfraFAFUpdate implements RegressionExecution {
        public void run(RegressionEnvironment env) {
            RegressionPath path = new RegressionPath();
            String[] fields = "p0,p1".split(",");
            env.compileDeploy("@Name('TheTable') create table MyTableUPD as (p0 string primary key, p1 string, thesum sum(int))", path);
            env.compileDeploy("into table MyTableUPD select theString, sum(intPrimitive) as thesum from SupportBean group by theString", path);
            env.sendEventBean(new SupportBean("E1", 1));
            env.sendEventBean(new SupportBean("E2", 2));
            env.compileExecuteFAF("update MyTableUPD set p1 = 'ABC'", path);
            EPAssertionUtil.assertPropsPerRowAnyOrder(env.statement("TheTable").iterator(), fields, new Object[][]{{"E1", "ABC"}, {"E2", "ABC"}});
            env.undeployAll();
        }
    }

    private static class InfraFAFSelect implements RegressionExecution {
        public void run(RegressionEnvironment env) {
            RegressionPath path = new RegressionPath();
            String[] fields = "p0".split(",");
            env.compileDeploy("@Name('TheTable') create table MyTableSEL as (p0 string primary key, thesum sum(int))", path);
            env.compileDeploy("into table MyTableSEL select theString, sum(intPrimitive) as thesum from SupportBean group by theString", path);
            env.sendEventBean(new SupportBean("E1", 1));
            env.sendEventBean(new SupportBean("E2", 2));
            EPFireAndForgetQueryResult result = env.compileExecuteFAF("select * from MyTableSEL", path);
            EPAssertionUtil.assertPropsPerRowAnyOrder(result.getArray(), fields, new Object[][]{{"E1"}, {"E2"}});
            env.undeployAll();
        }
    }

    private static long getTableCount(EPStatement stmt) {
        return EPAssertionUtil.iteratorCount(stmt.iterator());
    }

    private static void assertFAFInsertResult(EPFireAndForgetQueryResult resultOne, String[] propertyNames, EPStatement stmt) {
        assertEquals(0, resultOne.getArray().length);
        assertSame(resultOne.getEventType(), stmt.getEventType());
    }
}
