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
package com.espertech.esper.regressionlib.suite.expr.filter;

import com.espertech.esper.common.client.scopetest.EPAssertionUtil;
import com.espertech.esper.common.internal.filterspec.FilterOperator;
import com.espertech.esper.common.internal.filterspec.FilterSpecParamForge;
import com.espertech.esper.common.internal.support.SupportBean;
import com.espertech.esper.common.internal.support.SupportBean_S0;
import com.espertech.esper.common.internal.support.SupportBean_S1;
import com.espertech.esper.common.internal.support.SupportBean_S2;
import com.espertech.esper.regressionlib.framework.RegressionEnvironment;
import com.espertech.esper.regressionlib.framework.RegressionExecution;
import com.espertech.esper.regressionlib.framework.RegressionPath;
import com.espertech.esper.regressionlib.support.filter.SupportFilterPlanHook;
import com.espertech.esper.runtime.internal.filtersvcimpl.FilterItem;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;

import static com.espertech.esper.common.internal.filterspec.FilterOperator.*;
import static com.espertech.esper.regressionlib.support.filter.SupportFilterOptimizableHelper.hasFilterIndexPlanAdvanced;
import static com.espertech.esper.regressionlib.support.filter.SupportFilterServiceHelper.*;
import static org.junit.Assert.assertEquals;

public class ExprFilterOptimizableLookupableLimitedExpr {
    public static Collection<RegressionExecution> executions() {
        ArrayList<RegressionExecution> executions = new ArrayList<>();
        executions.add(new ExprFilterOptLkupEqualsOneStmt());
        executions.add(new ExprFilterOptLkupEqualsOneStmtWPatternSharingIndex());
        executions.add(new ExprFilterOptLkupEqualsMultiStmtSharingIndex());
        executions.add(new ExprFilterOptLkupEqualsCoercion());
        executions.add(new ExprFilterOptLkupInSetOfValue());
        executions.add(new ExprFilterOptLkupInRangeWCoercion());
        executions.add(new ExprFilterOptLkupDisqualify());
        executions.add(new ExprFilterOptLkupCurrentTimestamp());
        return executions;
    }
    
    private static class ExprFilterOptLkupCurrentTimestamp implements RegressionExecution {
        public void run(RegressionEnvironment env) {
            String epl = "@name('s0') select * from pattern[a=SupportBean -> SupportBean(a.longPrimitive = current_timestamp() + longPrimitive)];\n";
            env.compileDeploy(epl).addListener("s0");

            env.advanceTime(1000);
            env.sendEventBean(makeSBLong(1123));
            if (hasFilterIndexPlanAdvanced(env)) {
                assertFilterSvcSingle(env.statement("s0"), "current_timestamp()+longPrimitive", EQUAL);
            }

            env.milestone(0);

            env.sendEventBean(makeSBLong(123));
            env.listener("s0").assertOneGetNewAndReset();

            env.undeployAll();
        }
    }

    private static class ExprFilterOptLkupDisqualify implements RegressionExecution {
        public void run(RegressionEnvironment env) {
            RegressionPath path = new RegressionPath();
            String objects = "@public create variable string MYVARIABLE_NONCONSTANT = 'abc';\n" +
                "@public create table MyTable(tablecol string);\n" +
                "@public create window MyWindow#keepall as SupportBean;\n" +
                "@public create inlined_class \"\"\"\n" +
                "  public class Helper {\n" +
                "    public static String doit(Object param) { return null;}\n" +
                "    public static String doit(Object one, Object two) { return null;}\n" +
                "  }\n" +
                "\"\"\";\n" +
                "@public create expression MyDeclaredExpr { (select theString from MyWindow) };\n" +
                "@public create expression MyHandThrough {v => v};\n" +
                "@public create expression string js:MyJavaScript(param) [\"a\"];\n";
            env.compile(objects, path);

            String hook = "@Hook(type=HookType.INTERNAL_FILTERSPEC, hook='" + SupportFilterPlanHook.class.getName() + "')";

            assertDisqualified(env, path, "SupportBean",
                hook + "select * from SupportBean(theString||MYVARIABLE_NONCONSTANT='ax')");
            assertDisqualified(env, path, "SupportBean",
                hook + "select * from SupportBean(theString||MyTable.tablecol='ax')");
            assertDisqualified(env, path, "SupportBean",
                hook + "select * from SupportBean(theString||(select theString from MyWindow)='ax')");
            assertDisqualified(env, path, "SupportBeanArrayCollMap",
                hook + "select * from SupportBeanArrayCollMap(id || setOfString.where(v => v=id).firstOf() = 'ax')");
            assertDisqualified(env, path, "SupportBean",
                hook + "select * from pattern[s0=SupportBean_S0 -> SupportBean(MyJavaScript(theString)='x')]");

            // local inlined class
            String eplWithLocalHelper = hook + "inlined_class \"\"\"\n" +
                "  public class LocalHelper {\n" +
                "    public static String doit(Object param) {\n" +
                "      return null;\n" +
                "    }\n" +
                "  }\n" +
                "\"\"\"\n" +
                "select * from SupportBean(LocalHelper.doit(theString) = 'abc')";
            assertDisqualified(env, path, "SupportBean", eplWithLocalHelper);
        }
    }

    private static class ExprFilterOptLkupInRangeWCoercion implements RegressionExecution {
        public void run(RegressionEnvironment env) {
            String epl = "@name('s0') select * from pattern [" +
                "a=SupportBean_S0 -> b=SupportBean_S1 -> every SupportBean(longPrimitive+longBoxed in [a.id - 2 : b.id + 2])];\n";
            runAssertionInRange(env, epl, false);

            epl = "@name('s0') select * from pattern [" +
                "a=SupportBean_S0 -> b=SupportBean_S1 -> every SupportBean(longPrimitive+longBoxed not in [a.id - 2 : b.id + 2])];\n";
            runAssertionInRange(env, epl, true);
        }

        private void runAssertionInRange(RegressionEnvironment env, String epl, boolean not) {
            env.compileDeploy(epl).addListener("s0");
            env.sendEventBean(new SupportBean_S0(10));
            env.sendEventBean(new SupportBean_S1(200));

            env.milestone(0);
            if (hasFilterIndexPlanAdvanced(env)) {
                assertFilterSvcSingle(env.statement("s0"), "longPrimitive+longBoxed", not ? NOT_RANGE_CLOSED : RANGE_CLOSED);
            }

            sendSBLongsAssert(env, 3, 4, not);
            sendSBLongsAssert(env, 5, 3, !not);
            sendSBLongsAssert(env, 1, 99, !not);
            sendSBLongsAssert(env, 101, 101, !not);
            sendSBLongsAssert(env, 200, 3, not);

            env.undeployAll();
        }
    }

    private static class ExprFilterOptLkupInSetOfValue implements RegressionExecution {
        public void run(RegressionEnvironment env) {
            String epl = "@name('s0') select * from pattern [" +
                "a=SupportBean_S0 -> b=SupportBean_S1 -> c=SupportBean_S2 -> every SupportBean(longPrimitive+longBoxed in (a.id, b.id, c.id))];\n";
            env.compileDeploy(epl).addListener("s0");
            env.sendEventBean(new SupportBean_S0(10));
            env.sendEventBean(new SupportBean_S1(200));
            env.sendEventBean(new SupportBean_S2(3000));

            env.milestone(0);

            if (hasFilterIndexPlanAdvanced(env)) {
                assertFilterSvcSingle(env.statement("s0"), "longPrimitive+longBoxed", IN_LIST_OF_VALUES);
            }

            sendSBLongsAssert(env, 0, 9, false);
            sendSBLongsAssert(env, 9, 1, true);
            sendSBLongsAssert(env, 199, 1, true);
            sendSBLongsAssert(env, 2090, 910, true);

            env.undeployAll();
        }
    }

    private static class ExprFilterOptLkupEqualsCoercion implements RegressionExecution {
        public void run(RegressionEnvironment env) {
            String epl = "@name('s0') select * from SupportBean(doublePrimitive + doubleBoxed = Integer.parseInt('10'))";
            env.compileDeploy(epl).addListener("s0");
            if (hasFilterIndexPlanAdvanced(env)) {
                assertFilterSvcSingle(env.statement("s0"), "doublePrimitive+doubleBoxed", EQUAL);
            }

            env.milestone(0);

            sendSBDoublesAssert(env, 5, 5, true);
            sendSBDoublesAssert(env, 10, 0, true);
            sendSBDoublesAssert(env, 0, 10, true);
            sendSBDoublesAssert(env, 0, 9, false);

            env.undeployAll();
        }
    }

    private static class ExprFilterOptLkupEqualsOneStmt implements RegressionExecution {
        public void run(RegressionEnvironment env) {
            String epl = "@Audit @name('s0') select * from pattern[s0=SupportBean_S0 -> every SupportBean_S1(p10 || p11 = 'ax')];\n";

            env.compileDeploy(epl).addListener("s0");
            env.sendEventBean(new SupportBean_S0(1));
            if (hasFilterIndexPlanAdvanced(env)) {
                assertFilterSvcSingle(env.statement("s0"), "p10||p11", EQUAL);
            }

            env.milestone(0);

            sendSB1Assert(env, "a", "x", true);
            sendSB1Assert(env, "a", "y", false);
            sendSB1Assert(env, "b", "x", false);
            sendSB1Assert(env, "a", "x", true);

            env.undeployAll();
        }
    }

    private static class ExprFilterOptLkupEqualsOneStmtWPatternSharingIndex implements RegressionExecution {
        public void run(RegressionEnvironment env) {
            String epl = "@name('s0') select * from pattern[every s0=SupportBean_S0 -> every SupportBean_S1('ax' = p10 || p11)] order by s0.id asc;\n";

            env.compileDeploy(epl).addListener("s0");
            env.sendEventBean(new SupportBean_S0(1));
            env.sendEventBean(new SupportBean_S0(2));

            env.milestone(0);

            if (hasFilterIndexPlanAdvanced(env)) {
                assertFilterSvcMultiSameIndexDepthOne(env.statement("s0"), "SupportBean_S1", 2, "p10||p11", EQUAL);
            }

            env.sendEventBean(new SupportBean_S1(10, "a", "x"));
            EPAssertionUtil.assertPropsPerRow(env.listener("s0").getAndResetLastNewData(), "s0.id".split(","), new Object[][]{{1}, {2}});

            env.undeployAll();
        }
    }

    private static class ExprFilterOptLkupEqualsMultiStmtSharingIndex implements RegressionExecution {
        public void run(RegressionEnvironment env) {
            String epl = "@name('s0') select * from SupportBean_S0(p00 || p01 = 'ax');\n" +
                "@name('s1') select * from SupportBean_S0(p00 || p01 = 'ax');\n" +
                "" +
                "create constant variable string VAR = 'ax';\n" +
                "@name('s2') select * from SupportBean_S0(p00 || p01 = VAR);\n" +
                "" +
                "create context MyContextOne start SupportBean_S1 as s1;\n" +
                "@name('s3') context MyContextOne select * from SupportBean_S0(p00 || p01 = context.s1.p10);\n" +
                "" +
                "create context MyContextTwo start SupportBean_S1 as s1;\n" +
                "@name('s4') context MyContextTwo select * from pattern[a=SupportBean_S1 -> SupportBean_S0(a.p10 = p00     ||     p01)];\n";
            env.compileDeploy(epl);
            String[] names = "s0,s1,s2,s3,s4".split(",");
            for (String name : names) {
                env.addListener(name);
            }
            env.sendEventBean(new SupportBean_S1(0, "ax"));

            env.milestone(0);

            Map<Integer, List<FilterItem[]>> filters = getFilterSvcAllStmtForType(env.runtime(), "SupportBean_S0");
            if (hasFilterIndexPlanAdvanced(env)) {
                assertFilterSvcMultiSameIndexDepthOne(filters, 5, "p00||p01", EQUAL);
            }

            env.sendEventBean(new SupportBean_S0(10, "a", "x"));
            for (String name : names) {
                env.listener(name).assertOneGetNewAndReset();
            }

            env.undeployAll();
        }
    }

    private static void sendSBDoublesAssert(RegressionEnvironment env, double doublePrimitive, double doubleBoxed, boolean received) {
        SupportBean sb = new SupportBean();
        sb.setDoublePrimitive(doublePrimitive);
        sb.setDoubleBoxed(doubleBoxed);
        env.sendEventBean(sb);
        assertEquals(received, env.listener("s0").getIsInvokedAndReset());
    }

    private static void sendSBLongsAssert(RegressionEnvironment env, long longPrimitive, long longBoxed, boolean received) {
        SupportBean sb = new SupportBean();
        sb.setLongPrimitive(longPrimitive);
        sb.setLongBoxed(longBoxed);
        env.sendEventBean(sb);
        assertEquals(received, env.listener("s0").getIsInvokedAndReset());
    }

    private static SupportBean makeSBLong(long longPrimitive) {
        SupportBean sb = new SupportBean();
        sb.setLongPrimitive(longPrimitive);
        return sb;
    }

    private static void sendSB1Assert(RegressionEnvironment env, String p10, String p11, boolean received) {
        env.sendEventBean(new SupportBean_S1(0, p10, p11));
        assertEquals(received, env.listener("s0").getIsInvokedAndReset());
    }

    protected static void assertDisqualified(RegressionEnvironment env, RegressionPath path, String typeName, String epl) {
        SupportFilterPlanHook.reset();
        env.compile(epl, path);
        FilterSpecParamForge forge = SupportFilterPlanHook.assertPlanSingleForTypeAndReset(typeName);
        assertEquals(FilterOperator.BOOLEAN_EXPRESSION, forge.getFilterOperator());
    }
}
