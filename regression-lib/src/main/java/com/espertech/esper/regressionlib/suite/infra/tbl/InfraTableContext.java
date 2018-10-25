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

import com.espertech.esper.common.client.scopetest.EPAssertionUtil;
import com.espertech.esper.regressionlib.framework.RegressionEnvironment;
import com.espertech.esper.regressionlib.framework.RegressionExecution;
import com.espertech.esper.regressionlib.framework.RegressionPath;
import com.espertech.esper.regressionlib.framework.SupportMessageAssertUtil;
import com.espertech.esper.common.internal.support.SupportBean;
import com.espertech.esper.common.internal.support.SupportBean_S0;

import java.util.ArrayList;
import java.util.Collection;

import static org.junit.Assert.assertEquals;

/**
 * NOTE: More table-related tests in "nwtable"
 */
public class InfraTableContext {

    public static Collection<RegressionExecution> executions() {
        ArrayList<RegressionExecution> execs = new ArrayList<>();
        execs.add(new InfraPartitioned());
        execs.add(new InfraNonOverlapping());
        execs.add(new InfraTableContextInvalid());
        return execs;
    }

    private static class InfraTableContextInvalid implements RegressionExecution {
        public void run(RegressionEnvironment env) {
            RegressionPath path = new RegressionPath();
            env.compileDeploy("create context SimpleCtx start after 1 sec end after 1 sec", path);
            env.compileDeploy("context SimpleCtx create table MyTable(pkey string primary key, thesum sum(int), col0 string)", path);

            SupportMessageAssertUtil.tryInvalidCompile(env, path, "select * from MyTable",
                "Table by name 'MyTable' has been declared for context 'SimpleCtx' and can only be used within the same context [");
            SupportMessageAssertUtil.tryInvalidCompile(env, path, "select (select * from MyTable) from SupportBean",
                "Failed to plan subquery number 1 querying MyTable: Mismatch in context specification, the context for the table 'MyTable' is 'SimpleCtx' and the query specifies no context  [select (select * from MyTable) from SupportBean]");
            SupportMessageAssertUtil.tryInvalidCompile(env, path, "insert into MyTable select theString as pkey from SupportBean",
                "Table by name 'MyTable' has been declared for context 'SimpleCtx' and can only be used within the same context [");

            env.undeployAll();
        }
    }

    private static class InfraNonOverlapping implements RegressionExecution {
        public void run(RegressionEnvironment env) {
            RegressionPath path = new RegressionPath();
            env.compileDeploy("create context CtxNowTillS0 start @now end SupportBean_S0", path);
            env.compileDeploy("context CtxNowTillS0 create table MyTable(pkey string primary key, thesum sum(int), col0 string)", path);
            env.compileDeploy("context CtxNowTillS0 into table MyTable select sum(intPrimitive) as thesum from SupportBean group by theString", path);
            env.compileDeploy("@name('s0') context CtxNowTillS0 select pkey as c0, thesum as c1 from MyTable output snapshot when terminated", path).addListener("s0");

            env.sendEventBean(new SupportBean("E1", 50));
            env.sendEventBean(new SupportBean("E2", 20));

            env.milestone(0);

            env.sendEventBean(new SupportBean("E1", 60));
            env.sendEventBean(new SupportBean_S0(-1)); // terminated
            EPAssertionUtil.assertPropsPerRowAnyOrder(env.listener("s0").getAndResetLastNewData(), "c0,c1".split(","),
                new Object[][]{{"E1", 110}, {"E2", 20}});

            env.compileDeploy("context CtxNowTillS0 create index MyIdx on MyTable(col0)", path);
            env.compileDeploy("context CtxNowTillS0 select * from MyTable, SupportBean_S1 where col0 = p11", path);

            env.sendEventBean(new SupportBean("E3", 90));
            env.sendEventBean(new SupportBean("E1", 30));
            env.sendEventBean(new SupportBean("E3", 10));

            env.milestone(1);

            env.sendEventBean(new SupportBean_S0(-1)); // terminated
            EPAssertionUtil.assertPropsPerRowAnyOrder(env.listener("s0").getAndResetLastNewData(), "c0,c1".split(","),
                new Object[][]{{"E1", 30}, {"E3", 100}});

            env.undeployAll();
        }
    }

    private static class InfraPartitioned implements RegressionExecution {
        public void run(RegressionEnvironment env) {
            RegressionPath path = new RegressionPath();
            env.compileDeploy("create context CtxPerString " +
                "partition by theString from SupportBean, p00 from SupportBean_S0", path);
            env.compileDeploy("context CtxPerString create table MyTable(thesum sum(int))", path);
            env.compileDeploy("context CtxPerString into table MyTable select sum(intPrimitive) as thesum from SupportBean", path);
            env.compileDeploy("@name('s0') context CtxPerString select MyTable.thesum as c0 from SupportBean_S0", path).addListener("s0");

            env.sendEventBean(new SupportBean("E1", 50));
            env.sendEventBean(new SupportBean("E2", 20));
            env.sendEventBean(new SupportBean("E1", 60));
            env.sendEventBean(new SupportBean_S0(0, "E1"));
            assertEquals(110, env.listener("s0").assertOneGetNewAndReset().get("c0"));

            env.milestone(0);

            env.sendEventBean(new SupportBean_S0(0, "E2"));
            assertEquals(20, env.listener("s0").assertOneGetNewAndReset().get("c0"));

            env.undeployAll();
        }
    }
}
