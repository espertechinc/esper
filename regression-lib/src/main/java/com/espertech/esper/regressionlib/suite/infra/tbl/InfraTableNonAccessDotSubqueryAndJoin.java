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
import com.espertech.esper.common.internal.support.SupportEventTypeAssertionEnum;
import com.espertech.esper.common.internal.support.SupportEventTypeAssertionUtil;
import com.espertech.esper.regressionlib.framework.RegressionEnvironment;
import com.espertech.esper.regressionlib.framework.RegressionExecution;
import com.espertech.esper.regressionlib.framework.RegressionPath;
import com.espertech.esper.regressionlib.framework.SupportMessageAssertUtil;
import com.espertech.esper.common.internal.support.SupportBean;

/**
 * NOTE: More table-related tests in "nwtable"
 */
public class InfraTableNonAccessDotSubqueryAndJoin implements RegressionExecution {

    public void run(RegressionEnvironment env) {
        runAssertionUse(env, false);
        runAssertionUse(env, true);
    }

    private static void runAssertionUse(RegressionEnvironment env, boolean soda) {
        RegressionPath path = new RegressionPath();
        String eplCreate = "create table MyTable (" +
            "col0 string, " +
            "col1 sum(int), " +
            "col2 sorted(intPrimitive) @type('SupportBean'), " +
            "col3 int[], " +
            "col4 window(*) @type('SupportBean')" +
            ")";
        env.compileDeploy(soda, eplCreate, path);

        String eplIntoTable = "@name('into') into table MyTable select sum(intPrimitive) as col1, sorted() as col2, " +
            "window(*) as col4 from SupportBean#length(3)";
        env.compileDeploy(soda, eplIntoTable, path);
        SupportBean[] sentSB = new SupportBean[2];
        sentSB[0] = makeSendSupportBean(env, "E1", 20);
        sentSB[1] = makeSendSupportBean(env, "E2", 21);
        env.undeployModuleContaining("into");

        String eplMerge = "@name('merge') on SupportBean merge MyTable when matched then update set col3={1,2,4,2}, col0=\"x\"";
        env.compileDeploy(soda, eplMerge, path);
        makeSendSupportBean(env, null, -1);
        env.undeployModuleContaining("merge");

        String eplSelect = "@name('s0') select " +
            "col0 as c0_1, mt.col0 as c0_2, " +
            "col1 as c1_1, mt.col1 as c1_2, " +
            "col2 as c2_1, mt.col2 as c2_2, " +
            "col2.minBy() as c2_3, mt.col2.maxBy() as c2_4, " +
            "col2.sorted().firstOf() as c2_5, mt.col2.sorted().firstOf() as c2_6, " +
            "col3.mostFrequent() as c3_1, mt.col3.mostFrequent() as c3_2, " +
            "col4 as c4_1 " +
            "from SupportBean unidirectional, MyTable as mt";
        env.compileDeploy(soda, eplSelect, path).addListener("s0");

        Object[][] expectedType = new Object[][]{
            {"c0_1", String.class}, {"c0_2", String.class},
            {"c1_1", Integer.class}, {"c1_2", Integer.class},
            {"c2_1", SupportBean[].class}, {"c2_2", SupportBean[].class},
            {"c2_3", SupportBean.class}, {"c2_4", SupportBean.class},
            {"c2_5", SupportBean.class}, {"c2_6", SupportBean.class},
            {"c3_1", Integer.class}, {"c3_2", Integer.class},
            {"c4_1", SupportBean[].class}
        };
        SupportEventTypeAssertionUtil.assertEventTypeProperties(expectedType, env.statement("s0").getEventType(), SupportEventTypeAssertionEnum.NAME, SupportEventTypeAssertionEnum.TYPE);

        makeSendSupportBean(env, null, -1);
        EventBean event = env.listener("s0").assertOneGetNewAndReset();
        EPAssertionUtil.assertProps(event, "c0_1,c0_2,c1_1,c1_2".split(","), new Object[]{"x", "x", 41, 41});
        EPAssertionUtil.assertProps(event, "c2_1,c2_2".split(","), new Object[]{sentSB, sentSB});
        EPAssertionUtil.assertProps(event, "c2_3,c2_4".split(","), new Object[]{sentSB[0], sentSB[1]});
        EPAssertionUtil.assertProps(event, "c2_5,c2_6".split(","), new Object[]{sentSB[0], sentSB[0]});
        EPAssertionUtil.assertProps(event, "c3_1,c3_2".split(","), new Object[]{2, 2});
        EPAssertionUtil.assertProps(event, "c4_1".split(","), new Object[]{sentSB});

        // unnamed column
        String eplSelectUnnamed = "@name('s1') select col2.sorted().firstOf(), mt.col2.sorted().firstOf()" +
            " from SupportBean unidirectional, MyTable mt";
        env.compileDeploy(eplSelectUnnamed, path);
        Object[][] expectedTypeUnnamed = new Object[][]{{"col2.sorted().firstOf()", SupportBean.class},
            {"mt.col2.sorted().firstOf()", SupportBean.class}};
        SupportEventTypeAssertionUtil.assertEventTypeProperties(expectedTypeUnnamed, env.statement("s1").getEventType(), SupportEventTypeAssertionEnum.NAME, SupportEventTypeAssertionEnum.TYPE);

        // invalid: ambiguous resolution
        SupportMessageAssertUtil.tryInvalidCompile(env, path, "" +
                "select col0 from SupportBean#lastevent, MyTable, MyTable",
            "Failed to validate select-clause expression 'col0': Ambiguous table column 'col0' should be prefixed by a stream name [");

        env.undeployAll();
    }

    private static SupportBean makeSendSupportBean(RegressionEnvironment env, String theString, int intPrimitive) {
        SupportBean b = new SupportBean(theString, intPrimitive);
        env.sendEventBean(b);
        return b;
    }
}
