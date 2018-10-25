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

import com.espertech.esper.common.client.EventBean;
import com.espertech.esper.common.client.scopetest.EPAssertionUtil;
import com.espertech.esper.regressionlib.framework.RegressionEnvironment;
import com.espertech.esper.regressionlib.framework.RegressionExecution;
import com.espertech.esper.regressionlib.framework.RegressionPath;
import com.espertech.esper.regressionlib.framework.SupportMessageAssertUtil;
import com.espertech.esper.common.internal.support.SupportBean;
import com.espertech.esper.common.internal.support.SupportBean_S0;

import static org.junit.Assert.assertEquals;

/**
 * NOTE: More table-related tests in "nwtable"
 */
public class InfraTableFilters implements RegressionExecution {

    public void run(RegressionEnvironment env) {
        RegressionPath path = new RegressionPath();
        env.compileDeploy("create table MyTable(pkey string primary key, col0 int)", path);
        env.compileDeploy("insert into MyTable select theString as pkey, intPrimitive as col0 from SupportBean", path);

        for (int i = 0; i < 5; i++) {
            env.sendEventBean(new SupportBean("E" + i, i));
        }
        String[] fields = "col0".split(",");

        // test FAF filter
        EventBean[] events = env.compileExecuteFAF("select col0 from MyTable(pkey='E1')", path).getArray();
        EPAssertionUtil.assertPropsPerRow(events, fields, new Object[][]{{1}});

        // test iterate
        env.compileDeploy("@name('iterate') select col0 from MyTable(pkey='E2')", path);
        EPAssertionUtil.assertPropsPerRow(env.iterator("iterate"), fields, new Object[][]{{2}});
        env.undeployModuleContaining("iterate");

        // test subquery
        env.compileDeploy("@name('subq') select (select col0 from MyTable(pkey='E3')) as col0 from SupportBean_S0", path).addListener("subq");
        env.sendEventBean(new SupportBean_S0(0));
        assertEquals(3, env.listener("subq").assertOneGetNewAndReset().get("col0"));
        env.undeployModuleContaining("subq");

        // test join
        SupportMessageAssertUtil.tryInvalidCompile(env, path, "select col0 from SupportBean_S0, MyTable(pkey='E4')",
            "Joins with tables do not allow table filter expressions, please add table filters to the where-clause instead [");

        env.undeployAll();
    }
}
