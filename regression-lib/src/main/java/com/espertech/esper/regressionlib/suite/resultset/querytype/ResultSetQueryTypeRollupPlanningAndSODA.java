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
package com.espertech.esper.regressionlib.suite.resultset.querytype;

import com.espertech.esper.common.client.annotation.HookType;
import com.espertech.esper.common.client.soda.AnnotationPart;
import com.espertech.esper.common.client.soda.EPStatementObjectModel;
import com.espertech.esper.common.internal.epl.agg.core.AggregationGroupByRollupLevelForge;
import com.espertech.esper.common.internal.epl.agg.rollup.GroupByRollupPlanDesc;
import com.espertech.esper.common.internal.epl.expression.core.ExprNodeUtilityPrint;
import com.espertech.esper.regressionlib.framework.RegressionEnvironment;
import com.espertech.esper.regressionlib.framework.RegressionExecution;
import com.espertech.esper.regressionlib.support.util.SupportGroupRollupPlanHook;

import java.io.StringWriter;

import static org.junit.Assert.assertEquals;

public class ResultSetQueryTypeRollupPlanningAndSODA implements RegressionExecution {
    public final static String PLAN_CALLBACK_HOOK = "@Hook(type=" + HookType.class.getName() + ".INTERNAL_GROUPROLLUP_PLAN,hook='" + SupportGroupRollupPlanHook.class.getName() + "')";

    public void run(RegressionEnvironment env) {
        // plain rollup
        validate(env, "a", "rollup(a)", new String[]{"a", ""});
        validate(env, "a, b", "rollup(a, b)", new String[]{"a,b", "a", ""});
        validate(env, "a, b, c", "rollup(a, b, c)", new String[]{"a,b,c", "a,b", "a", ""});
        validate(env, "a, b, c, d", "rollup(a, b, c, d)", new String[]{"a,b,c,d", "a,b,c", "a,b", "a", ""});

        // rollup with unenclosed
        validate(env, "a, b", "a, rollup(b)", new String[]{"a,b", "a"});
        validate(env, "a, b, c", "a, b, rollup(c)", new String[]{"a,b,c", "a,b"});
        validate(env, "a, b, c", "a, rollup(b, c)", new String[]{"a,b,c", "a,b", "a"});
        validate(env, "a, b, c, d", "a, b, rollup(c, d)", new String[]{"a,b,c,d", "a,b,c", "a,b"});
        validate(env, "a, b, c, d, e", "a, b, rollup(c, d, e)", new String[]{"a,b,c,d,e", "a,b,c,d", "a,b,c", "a,b"});

        // plain cube
        validate(env, "a", "cube(a)", new String[]{"a", ""});
        validate(env, "a, b", "cube(a, b)", new String[]{"a,b", "a", "b", ""});
        validate(env, "a, b, c", "cube(a, b, c)", new String[]{"a,b,c", "a,b", "a,c", "a", "b,c", "b", "c", ""});
        validate(env, "a, b, c, d", "cube(a, b, c, d)", new String[]{"a,b,c,d", "a,b,c", "a,b,d",
            "a,b", "a,c,d", "a,c", "a,d", "a",
            "b,c,d", "b,c", "b,d", "b",
            "c,d", "c", "d", ""});

        // cube with unenclosed
        validate(env, "a, b", "a, cube(b)", new String[]{"a,b", "a"});
        validate(env, "a, b, c", "a, cube(b, c)", new String[]{"a,b,c", "a,b", "a,c", "a"});
        validate(env, "a, b, c, d", "a, cube(b, c, d)", new String[]{"a,b,c,d", "a,b,c", "a,b,d", "a,b", "a,c,d", "a,c", "a,d", "a"});
        validate(env, "a, b, c, d", "a, b, cube(c, d)", new String[]{"a,b,c,d", "a,b,c", "a,b,d", "a,b"});

        // plain grouping set
        validate(env, "a", "grouping sets(a)", new String[]{"a"});
        validate(env, "a", "grouping sets(a)", new String[]{"a"});
        validate(env, "a, b", "grouping sets(a, b)", new String[]{"a", "b"});
        validate(env, "a, b", "grouping sets(a, b, (a, b), ())", new String[]{"a", "b", "a,b", ""});
        validate(env, "a, b", "grouping sets(a, (a, b), (), b)", new String[]{"a", "a,b", "", "b"});
        validate(env, "a, b, c", "grouping sets((a, b), (a, c), (), (b, c))", new String[]{"a,b", "a,c", "", "b,c"});
        validate(env, "a, b", "grouping sets((a, b))", new String[]{"a,b"});
        validate(env, "a, b, c", "grouping sets((a, b, c), ())", new String[]{"a,b,c", ""});
        validate(env, "a, b, c", "grouping sets((), (a, b, c), (b, c))", new String[]{"", "a,b,c", "b,c"});

        // grouping sets with unenclosed
        validate(env, "a, b", "a, grouping sets(b)", new String[]{"a,b"});
        validate(env, "a, b, c", "a, grouping sets(b, c)", new String[]{"a,b", "a,c"});
        validate(env, "a, b, c", "a, grouping sets((b, c))", new String[]{"a,b,c"});
        validate(env, "a, b, c, d", "a, b, grouping sets((), c, d, (c, d))", new String[]{"a,b", "a,b,c", "a,b,d", "a,b,c,d"});

        // multiple grouping sets
        validate(env, "a, b", "grouping sets(a), grouping sets(b)", new String[]{"a,b"});
        validate(env, "a, b, c", "grouping sets(a), grouping sets(b, c)", new String[]{"a,b", "a,c"});
        validate(env, "a, b, c, d", "grouping sets(a, b), grouping sets(c, d)", new String[]{"a,c", "a,d", "b,c", "b,d"});
        validate(env, "a, b, c", "grouping sets((), a), grouping sets(b, c)", new String[]{"b", "c", "a,b", "a,c"});
        validate(env, "a, b, c, d", "grouping sets(a, b, c), grouping sets(d)", new String[]{"a,d", "b,d", "c,d"});
        validate(env, "a, b, c, d, e", "grouping sets(a, b, c), grouping sets(d, e)", new String[]{"a,d", "a,e", "b,d", "b,e", "c,d", "c,e"});

        // multiple rollups
        validate(env, "a, b, c", "rollup(a, b), rollup(c)", new String[]{"a,b,c", "a,b", "a,c", "a", "c", ""});
        validate(env, "a, b, c, d", "rollup(a, b), rollup(c, d)", new String[]{"a,b,c,d", "a,b,c", "a,b", "a,c,d", "a,c", "a", "c,d", "c", ""});

        // grouping sets with rollup or cube inside
        validate(env, "a, b, c", "grouping sets(a, rollup(b, c))", new String[]{"a", "b,c", "b", ""});
        validate(env, "a, b, c", "grouping sets(a, cube(b, c))", new String[]{"a", "b,c", "b", "c", ""});
        validate(env, "a, b", "grouping sets(rollup(a, b))", new String[]{"a,b", "a", ""});
        validate(env, "a, b", "grouping sets(cube(a, b))", new String[]{"a,b", "a", "b", ""});
        validate(env, "a, b, c, d", "grouping sets((a, b), rollup(c, d))", new String[]{"a,b", "c,d", "c", ""});
        validate(env, "a, b, c, d", "grouping sets(a, b, rollup(c, d))", new String[]{"a", "b", "c,d", "c", ""});

        // cube and rollup with combined expression
        validate(env, "a, b, c", "cube((a, b), c)", new String[]{"a,b,c", "a,b", "c", ""});
        validate(env, "a, b, c", "rollup((a, b), c)", new String[]{"a,b,c", "a,b", ""});
        validate(env, "a, b, c, d", "cube((a, b), (c, d))", new String[]{"a,b,c,d", "a,b", "c,d", ""});
        validate(env, "a, b, c, d", "rollup((a, b), (c, d))", new String[]{"a,b,c,d", "a,b", ""});
        validate(env, "a, b, c", "cube(a, (b, c))", new String[]{"a,b,c", "a", "b,c", ""});
        validate(env, "a, b, c", "rollup(a, (b, c))", new String[]{"a,b,c", "a", ""});
        validate(env, "a, b, c", "grouping sets(rollup((a, b), c))", new String[]{"a,b,c", "a,b", ""});

        // multiple cubes and rollups
        validate(env, "a, b, c, d", "rollup(a, b), rollup(c, d)", new String[]{"a,b,c,d", "a,b,c", "a,b",
            "a,c,d", "a,c", "a", "c,d", "c", ""});
        validate(env, "a, b", "cube(a), cube(b)", new String[]{"a,b", "a", "b", ""});
        validate(env, "a, b, c", "cube(a, b), cube(c)", new String[]{"a,b,c", "a,b", "a,c", "a", "b,c", "b", "c", ""});
    }

    private static void validate(RegressionEnvironment env, String selectClause, String groupByClause, String[] expectedCSV) {
        String epl = PLAN_CALLBACK_HOOK + " select " + selectClause + ", count(*) from SupportEventABCProp group by " + groupByClause;
        SupportGroupRollupPlanHook.reset();

        env.compile(epl);
        comparePlan(expectedCSV);
        env.undeployAll();

        EPStatementObjectModel model = env.eplToModel(epl);
        assertEquals(epl, model.toEPL());
        SupportGroupRollupPlanHook.reset();

        model.getAnnotations().add(AnnotationPart.nameAnnotation("s0"));
        env.compileDeploy(model).addListener("s0");
        comparePlan(expectedCSV);

        env.undeployAll();
    }

    private static void comparePlan(String[] expectedCSV) {
        GroupByRollupPlanDesc plan = SupportGroupRollupPlanHook.getPlan();
        AggregationGroupByRollupLevelForge[] levels = plan.getRollupDesc().getLevels();
        String[][] received = new String[levels.length][];
        for (int i = 0; i < levels.length; i++) {
            AggregationGroupByRollupLevelForge level = levels[i];
            if (level.isAggregationTop()) {
                received[i] = new String[0];
            } else {
                received[i] = new String[level.getRollupKeys().length];
                for (int j = 0; j < received[i].length; j++) {
                    int key = level.getRollupKeys()[j];
                    received[i][j] = ExprNodeUtilityPrint.toExpressionStringMinPrecedenceSafe(plan.getExpressions()[key]);
                }
            }
        }

        assertEquals("Received: " + toCSV(received), expectedCSV.length, received.length);
        for (int i = 0; i < expectedCSV.length; i++) {
            String receivedCSV = toCSV(received[i]);
            assertEquals("Failed at row " + i, expectedCSV[i], receivedCSV);
        }
    }

    private static String toCSV(String[][] received) {
        StringWriter writer = new StringWriter();
        String delimiter = "";
        for (String[] item : received) {
            writer.append(delimiter);
            writer.append(toCSV(item));
            delimiter = "  ";
        }
        return writer.toString();
    }

    private static String toCSV(String[] received) {
        StringWriter writer = new StringWriter();
        String delimiter = "";
        for (String item : received) {
            writer.append(delimiter);
            writer.append(item);
            delimiter = ",";
        }
        return writer.toString();
    }
}
