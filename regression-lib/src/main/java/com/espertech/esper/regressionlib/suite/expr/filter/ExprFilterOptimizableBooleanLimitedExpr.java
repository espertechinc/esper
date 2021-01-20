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

import com.espertech.esper.common.client.EPCompiled;
import com.espertech.esper.common.internal.filterspec.FilterOperator;
import com.espertech.esper.common.internal.filterspec.FilterSpecParamForge;
import com.espertech.esper.common.internal.support.SupportBean;
import com.espertech.esper.common.internal.support.SupportBean_S0;
import com.espertech.esper.common.internal.support.SupportBean_S1;
import com.espertech.esper.regressionlib.framework.RegressionEnvironment;
import com.espertech.esper.regressionlib.framework.RegressionExecution;
import com.espertech.esper.regressionlib.framework.RegressionFlag;
import com.espertech.esper.regressionlib.framework.RegressionPath;
import com.espertech.esper.regressionlib.support.filter.SupportFilterPlanHook;
import com.espertech.esper.regressionlib.support.filter.SupportFilterServiceHelper;
import com.espertech.esper.runtime.client.EPDeployException;
import com.espertech.esper.runtime.client.EPDeployment;
import com.espertech.esper.runtime.client.EPDeploymentService;
import com.espertech.esper.runtime.client.EPStatement;
import com.espertech.esper.runtime.client.scopetest.SupportListener;
import com.espertech.esper.runtime.internal.filtersvcimpl.FilterItem;

import java.util.*;

import static com.espertech.esper.common.internal.filterspec.FilterOperator.EQUAL;
import static com.espertech.esper.common.internal.filterspec.FilterOperator.REBOOL;
import static com.espertech.esper.regressionlib.support.filter.SupportFilterOptimizableHelper.hasFilterIndexPlanAdvanced;
import static com.espertech.esper.regressionlib.support.filter.SupportFilterServiceHelper.*;
import static junit.framework.TestCase.assertFalse;
import static junit.framework.TestCase.assertTrue;
import static org.junit.Assert.*;

public class ExprFilterOptimizableBooleanLimitedExpr {
    public static Collection<RegressionExecution> executions() {
        ArrayList<RegressionExecution> executions = new ArrayList<>();
        executions.add(new ExprFilterOptReboolConstValueRegexpRHS());
        executions.add(new ExprFilterOptReboolMixedValueRegexpRHS());
        executions.add(new ExprFilterOptReboolConstValueRegexpRHSPerformance());
        executions.add(new ExprFilterOptReboolConstValueRegexpLHS());
        executions.add(new ExprFilterOptReboolNoValueExprRegexpSelf());
        executions.add(new ExprFilterOptReboolNoValueConcat());
        executions.add(new ExprFilterOptReboolContextValueDeep());
        executions.add(new ExprFilterOptReboolContextValueWithConst());
        executions.add(new ExprFilterOptReboolPatternValueWithConst());
        executions.add(new ExprFilterOptReboolWithEquals());
        executions.add(new ExprFilterOptReboolMultiple());
        executions.add(new ExprFilterOptReboolDisqualify());
        return executions;
    }

    private static class ExprFilterOptReboolWithEquals implements RegressionExecution {
        public void run(RegressionEnvironment env) {
            String epl = "@name('s0') select * from pattern[s0=SupportBean_S0 -> SupportBean(intPrimitive+5=s0.id and theString='a')]";
            env.compileDeploy(epl).addListener("s0");
            env.sendEventBean(new SupportBean_S0(10));

            if (hasFilterIndexPlanAdvanced(env)) {
                env.assertStatement("s0", statement -> {
                    FilterItem[] params = getFilterSvcMultiAssertNonEmpty(statement);
                    assertEquals(EQUAL, params[0].getOp());
                    assertEquals(EQUAL, params[1].getOp());
                });
            }

            env.milestone(0);

            sendSBAssert(env, "a", 10, false);
            sendSBAssert(env, "b", 5, false);
            sendSBAssert(env, "a", 5, true);

            env.undeployAll();
        }
    }

    private static class ExprFilterOptReboolMultiple implements RegressionExecution {
        public void run(RegressionEnvironment env) {
            env.compileDeploy("@name('s0') select * from SupportBean_S0(p00 regexp '.*X' and p01 regexp '.*Y')").addListener("s0");
            env.compileDeploy("@name('s1') select * from SupportBean_S0(p01 regexp '.*Y' and p00 regexp '.*X')").addListener("s1");

            env.milestone(0);

            if (hasFilterIndexPlanAdvanced(env)) {
                env.assertThat(() -> {
                    Map<String, FilterItem[]> filters = getFilterSvcAllStmtForTypeMulti(env.runtime(), "SupportBean_S0");
                    FilterItem[] s0 = filters.get("s0");
                    FilterItem[] s1 = filters.get("s1");
                    assertEquals(REBOOL, s0[0].getOp());
                    assertEquals(".p00 regexp ?", s0[0].getName());
                    assertEquals(REBOOL, s0[1].getOp());
                    assertEquals(".p01 regexp ?", s0[1].getName());
                    assertEquals(s0[0], s1[0]);
                    assertEquals(s0[1], s1[1]);
                });
            }

            sendS0Assert(env, "AX", "AZ", false);
            sendS0Assert(env, "AY", "AX", false);
            sendS0Assert(env, "AX", "BY", true);

            env.undeployAll();
        }
    }

    private static class ExprFilterOptReboolPatternValueWithConst implements RegressionExecution {
        public void run(RegressionEnvironment env) {
            String epl = "@name('s0') select * from pattern[s0=SupportBean_S0 -> SupportBean_S1(p10 || 'abc' regexp s0.p00)];\n";
            env.compileDeploy(epl).addListener("s0");
            env.sendEventBean(new SupportBean_S0(1, "x.*abc"));
            if (hasFilterIndexPlanAdvanced(env)) {
                assertFilterSvcSingle(env, "s0", ".p10||\"abc\" regexp ?", REBOOL);
            }

            env.milestone(0);

            sendS1Assert(env, "ydotabc", false);
            sendS1Assert(env, "xdotabc", true);

            env.undeployAll();
        }
    }

    private static class ExprFilterOptReboolContextValueWithConst implements RegressionExecution {
        public void run(RegressionEnvironment env) {
            String epl = "create context MyContext start SupportBean_S0 as s0;\n" +
                "@name('s0') context MyContext select * from SupportBean_S1(p10 || 'abc' regexp context.s0.p00);\n";
            env.compileDeploy(epl).addListener("s0");
            env.sendEventBean(new SupportBean_S0(1, "x.*abc"));
            if (hasFilterIndexPlanAdvanced(env)) {
                assertFilterSvcSingle(env, "s0", ".p10||\"abc\" regexp ?", REBOOL);
            }

            env.milestone(0);

            sendS1Assert(env, "ydotabc", false);
            sendS1Assert(env, "xdotabc", true);

            env.undeployAll();
        }
    }

    private static class ExprFilterOptReboolContextValueDeep implements RegressionExecution {
        public void run(RegressionEnvironment env) {
            String epl = "create context MyContext start SupportBean_S0 as s0;\n" +
                "@name('s0') context MyContext select * from SupportBean_S1(p10 regexp p11 || context.s0.p00);\n";
            env.compileDeploy(epl).addListener("s0");
            env.sendEventBean(new SupportBean_S0(1, ".*X"));
            if (hasFilterIndexPlanAdvanced(env)) {
                assertFilterSvcSingle(env, "s0", ".p10 regexp p11||?", REBOOL);
            }

            env.milestone(0);

            sendS1Assert(env, "gardenX", "abc", false);
            sendS1Assert(env, "garden", "gard", false);
            sendS1Assert(env, "gardenX", "gard", true);

            env.undeployAll();
        }
    }

    private static class ExprFilterOptReboolNoValueConcat implements RegressionExecution {
        public void run(RegressionEnvironment env) {
            String epl = "select * from SupportBean_S0(p00 || p01 = p02 || p03)";
            runTwoStmt(env, epl, epl,
                ".p00||p01=p02||p03",
                "SupportBean_S0",
                new SupportBean_S0(1, "a", "b", "a", "b"),
                new SupportBean_S0(1, "a", "b", "a", "c"));
        }
    }

    private static class ExprFilterOptReboolNoValueExprRegexpSelf implements RegressionExecution {
        public void run(RegressionEnvironment env) {
            runTwoStmt(env,
                "select * from SupportBean_S0(p00 regexp p01) as a",
                "select * from SupportBean_S0(s0.p00 regexp s0.p01) as s0",
                ".p00 regexp p01",
                "SupportBean_S0",
                new SupportBean_S0(1, "abc", ".*c"),
                new SupportBean_S0(2, "abc", ".*d"));
        }
    }

    private static class ExprFilterOptReboolConstValueRegexpLHS implements RegressionExecution {
        public void run(RegressionEnvironment env) {
            runTwoStmt(env,
                "select * from SupportBean('abc' regexp a.theString) as a",
                "select * from SupportBean('abc' regexp theString)",
                ".? regexp theString",
                "SupportBean",
                new SupportBean(".*bc", 0),
                new SupportBean(".*d", 0));
        }
    }

    private static class ExprFilterOptReboolMixedValueRegexpRHS implements RegressionExecution {
        public void run(RegressionEnvironment env) {
            String epl = "@name('var') create constant variable string MYVAR = '.*abc.*';\n" +
                "@name('s0') select * from SupportBean(theString regexp MYVAR);\n" +
                "" +
                "@name('ctx') create context MyContext start SupportBean_S0 as s0;\n" +
                "@name('s1') context MyContext select * from SupportBean(theString regexp context.s0.p00);\n" +
                "" +
                "@name('s2') select * from pattern[s0=SupportBean_S0 -> every SupportBean(theString regexp s0.p00)];\n" +
                "" +
                "@name('s3') select * from SupportBean(theString regexp '.*' || 'abc' || '.*');\n";
            env.compileDeploy(epl);
            EPDeployment deployment = env.deployment().getDeployment(env.deploymentId("s0"));
            Set<String> statementNames = new LinkedHashSet<>();
            for (EPStatement stmt : deployment.getStatements()) {
                if (stmt.getName().startsWith("s")) {
                    stmt.addListener(env.listenerNew());
                    statementNames.add(stmt.getName());
                }
            }

            env.milestone(0);

            env.sendEventBean(new SupportBean_S0(1, ".*abc.*"));

            sendSBAssert(env, "xabsx", statementNames, false);
            sendSBAssert(env, "xabcx", statementNames, true);

            if (hasFilterIndexPlanAdvanced(env)) {
                Map<String, FilterItem> filters = SupportFilterServiceHelper.getFilterSvcAllStmtForTypeSingleFilter(env.runtime(), "SupportBean");
                FilterItem s0 = filters.get("s0");
                for (String name : statementNames) {
                    FilterItem sn = filters.get(name);
                    assertEquals(FilterOperator.REBOOL, sn.getOp());
                    assertNotNull(s0.getOptionalValue());
                    assertNotNull(s0.getIndex());
                    assertSame(s0.getIndex(), sn.getIndex());
                    assertSame(s0.getOptionalValue(), sn.getOptionalValue());
                }
            }

            env.undeployAll();
        }

        public EnumSet<RegressionFlag> flags() {
            return EnumSet.of(RegressionFlag.OBSERVEROPS);
        }
    }

    private static class ExprFilterOptReboolConstValueRegexpRHS implements RegressionExecution {
        public void run(RegressionEnvironment env) {
            SupportFilterPlanHook.reset();
            String hook = "@Hook(type=HookType.INTERNAL_FILTERSPEC, hook='" + SupportFilterPlanHook.class.getName() + "')";
            String epl = hook + "@name('s0') select * from SupportBean(theString regexp '.*a.*')";
            env.compileDeploy(epl).addListener("s0");
            if (hasFilterIndexPlanAdvanced(env)) {
                FilterSpecParamForge forge = SupportFilterPlanHook.assertPlanSingleTripletAndReset("SupportBean");
                assertEquals(FilterOperator.REBOOL, forge.getFilterOperator());
                assertEquals(".theString regexp ?", forge.getLookupable().getExpression());
                assertEquals(String.class, forge.getLookupable().getReturnType().getType());
                assertFilterSvcSingle(env, "s0", ".theString regexp ?", REBOOL);
            }

            epl = "@name('s1') select * from SupportBean(theString regexp '.*a.*')";
            env.compileDeploy(epl).addListener("s1");

            epl = "@name('s2') select * from SupportBean(theString regexp '.*b.*')";
            env.compileDeploy(epl).addListener("s2");

            env.milestone(0);

            if (hasFilterIndexPlanAdvanced(env)) {
                Map<String, FilterItem> filters = SupportFilterServiceHelper.getFilterSvcAllStmtForTypeSingleFilter(env.runtime(), "SupportBean");
                FilterItem s0 = filters.get("s0");
                FilterItem s1 = filters.get("s1");
                FilterItem s2 = filters.get("s2");
                assertEquals(FilterOperator.REBOOL, s0.getOp());
                assertNotNull(s0.getOptionalValue());
                assertNotNull(s0.getIndex());
                assertSame(s0.getIndex(), s1.getIndex());
                assertSame(s0.getOptionalValue(), s1.getOptionalValue());
                assertSame(s0.getIndex(), s2.getIndex());
                assertNotSame(s0.getOptionalValue(), s2.getOptionalValue());
            }

            sendSBAssert(env, "garden", true, true, false);
            sendSBAssert(env, "house", false, false, false);
            sendSBAssert(env, "grub", false, false, true);

            env.undeployAll();
        }

        public EnumSet<RegressionFlag> flags() {
            return EnumSet.of(RegressionFlag.STATICHOOK);
        }
    }

    private static class ExprFilterOptReboolConstValueRegexpRHSPerformance implements RegressionExecution {
        @Override
        public EnumSet<RegressionFlag> flags() {
            return EnumSet.of(RegressionFlag.EXCLUDEWHENINSTRUMENTED, RegressionFlag.OBSERVEROPS);
        }

        public void run(RegressionEnvironment env) {
            RegressionPath path = new RegressionPath();

            String epl = "select * from SupportBean(theString regexp '.*,.*,.*,.*,.*,13,.*,.*,.*,.*,.*,.*')";
            int count = 5;
            deployMultiple(count, path, epl, env);

            env.milestone(0);

            SupportListener[] listeners = new SupportListener[count];
            for (int i = 0; i < count; i++) {
                listeners[i] = env.listener("s" + i);
            }

            long startTime = System.currentTimeMillis();
            int loops = 1000;
            for (int i = 0; i < loops; i++) {
                boolean match = i % 100 == 0;
                String value = match ?
                    "42,12,13,12,32,13,14,43,56,31,78,10" : // match
                    "42,12,13,12,32,14,13,43,56,31,78,10";  // no-match

                env.sendEventBean(new SupportBean(value, 0));
                if (match) {
                    for (SupportListener listener : listeners) {
                        listener.assertOneGetNewAndReset();
                    }
                } else {
                    for (SupportListener listener : listeners) {
                        assertFalse(listener.isInvoked());
                    }
                }
            }
            long delta = System.currentTimeMillis() - startTime;

            assertTrue("Delta is " + delta, delta < 1000); // ~7 seconds without optimization
            env.undeployAll();
        }
    }

    private static void deployMultiple(int count, RegressionPath path, String epl, RegressionEnvironment env) {
        for (int i = 0; i < count; i++) {
            EPCompiled compiled = env.compile("@name('s" + i + "')" + epl, path);
            EPDeploymentService admin = env.runtime().getDeploymentService();
            try {
                admin.deploy(compiled);
            } catch (EPDeployException ex) {
                ex.printStackTrace();
                fail();
            }
        }
        for (int i = 0; i < count; i++) {
            env.addListener("s" + i);
        }
    }

    public static class ExprFilterOptReboolDisqualify implements RegressionExecution {
        public void run(RegressionEnvironment env) {
            RegressionPath path = new RegressionPath();
            String objects = "@public create variable string MYVARIABLE_NONCONSTANT = 'abc';\n" +
                "@public create table MyTable(tablecol string);\n" +
                "@public create window MyWindow#keepall as SupportBean;\n" +
                "@public create inlined_class \"\"\"\n" +
                "  import com.espertech.esper.common.client.hook.singlerowfunc.*;\n" +
                "  import com.espertech.esper.common.client.configuration.compiler.*;\n" +
                "  @ExtensionSingleRowFunction(name=\"doit\", methodName=\"doit\", filterOptimizable=ConfigurationCompilerPlugInSingleRowFunction.FilterOptimizable.DISABLED)\n" +
                "  public class Helper {\n" +
                "    public static String doit(Object param) {\n" +
                "      return null;\n" +
                "    }\n" +
                "  }\n" +
                "\"\"\";\n" +
                "@public create expression MyDeclaredExpr { (select theString from MyWindow) };\n" +
                "@public create expression MyHandThrough {v => v};" +
                "@public create expression string js:MyJavaScript() [\"a\"];\n";
            env.compile(objects, path);
            String hook = "@Hook(type=HookType.INTERNAL_FILTERSPEC, hook='" + SupportFilterPlanHook.class.getName() + "')";

            // Core disqualifing: non-constant variables, tables, subselects, lambda, plug-in UDF with filter-opt-disabled, scripts
            assertDisqualified(env, path, "SupportBean", hook + "select * from SupportBean(theString regexp MYVARIABLE_NONCONSTANT)");
            assertDisqualified(env, path, "SupportBean", hook + "select * from SupportBean(theString=MyTable.tablecol)");
            assertDisqualified(env, path, "SupportBean", hook + "select * from SupportBean(theString=(select theString from MyWindow))");
            assertDisqualified(env, path, "SupportBeanArrayCollMap", hook + "select * from SupportBeanArrayCollMap(id = setOfString.where(v => v=id).firstOf())");
            assertDisqualified(env, path, "SupportBean", hook + "select * from SupportBean(theString regexp doit('abc'))");
            assertDisqualified(env, path, "SupportBean", hook + "select * from SupportBean(theString regexp MyJavaScript())");

            // multiple value expressions
            assertDisqualified(env, path, "SupportBean_S1",
                hook + "select * from pattern[s0=SupportBean_S0 -> SupportBean_S1(s0.p00 || p10 = s0.p00 || p11)]");
            assertDisqualified(env, path, "SupportBean_S1",
                "create context MyContext start SupportBean_S0 as s0;\n" +
                    hook + "context MyContext select * from SupportBean_S1(context.s0.p00 || p10 = context.s0.p01)");

            String eplWithLocalHelper = hook + "inlined_class \"\"\"\n" +
                "  public class LocalHelper {\n" +
                "    public static String doit(Object param) {\n" +
                "      return null;\n" +
                "    }\n" +
                "  }\n" +
                "\"\"\"\n" +
                "select * from SupportBean(theString regexp LocalHelper.doit('abc'))";
            assertDisqualified(env, path, "SupportBean", eplWithLocalHelper);
        }

        public EnumSet<RegressionFlag> flags() {
            return EnumSet.of(RegressionFlag.STATICHOOK);
        }
    }

    protected static void assertDisqualified(RegressionEnvironment env, RegressionPath path, String typeName, String epl) {
        SupportFilterPlanHook.reset();
        env.compile(epl, path);
        FilterSpecParamForge forge = SupportFilterPlanHook.assertPlanSingleForTypeAndReset(typeName);
        assertEquals(FilterOperator.BOOLEAN_EXPRESSION, forge.getFilterOperator());
    }

    private static void sendS0Assert(RegressionEnvironment env, String p00, String p01, boolean receivedS0) {
        env.sendEventBean(new SupportBean_S0(0, p00, p01));
        env.assertListenerInvokedFlag("s0", receivedS0);
    }

    private static void sendSBAssert(RegressionEnvironment env, String theString, boolean receivedS0, boolean receivedS1, boolean receivedS2) {
        env.sendEventBean(new SupportBean(theString, 0));
        env.assertListenerInvokedFlag("s0", receivedS0);
        env.assertListenerInvokedFlag("s1", receivedS1);
        env.assertListenerInvokedFlag("s2", receivedS2);
    }

    private static void sendSBAssert(RegressionEnvironment env, String theString, int intPrimitive, boolean receivedS0) {
        env.sendEventBean(new SupportBean(theString, intPrimitive));
        env.assertListenerInvokedFlag("s0", receivedS0);
    }

    private static void sendSBAssert(RegressionEnvironment env, String theString, Collection<String> names, boolean expected) {
        env.sendEventBean(new SupportBean(theString, 0));
        assertReceived(env, names, expected);
    }

    private static void assertReceived(RegressionEnvironment env, Collection<String> names, boolean expected) {
        for (String name : names) {
            env.assertListenerInvokedFlag(name, expected, "failed for '" + name + "'");
        }
    }

    private static void assertSameFilterEntry(RegressionEnvironment env, String eventTypeName) {
        env.assertThat(() -> {
            Map<String, FilterItem> filters = SupportFilterServiceHelper.getFilterSvcAllStmtForTypeSingleFilter(env.runtime(), eventTypeName);
            FilterItem s0 = filters.get("s0");
            FilterItem s1 = filters.get("s1");
            assertEquals(FilterOperator.REBOOL, s0.getOp());
            assertNotNull(s0.getIndex());
            assertSame(s0.getIndex(), s1.getIndex());
            assertSame(s0.getOptionalValue(), s1.getOptionalValue());
        });
    }

    private static void sendS1Assert(RegressionEnvironment env, String p10, String p11, boolean expected) {
        env.sendEventBean(new SupportBean_S1(1, p10, p11));
        env.assertListenerInvokedFlag("s0", expected);
    }

    private static void sendS1Assert(RegressionEnvironment env, String p10, boolean expected) {
        sendS1Assert(env, p10, null, expected);
    }

    private static void runTwoStmt(RegressionEnvironment env, String eplZero, String eplOne, String reboolExpressionText, String eventTypeName,
                                   Object eventReceived, Object eventNotReceived) {
        env.compileDeploy("@name('s0') " + eplZero).addListener("s0");
        boolean advanced = hasFilterIndexPlanAdvanced(env);
        if (advanced) {
            assertFilterSvcSingle(env, "s0", reboolExpressionText, REBOOL);
        }

        env.compileDeploy("@name('s1') " + eplOne).addListener("s1");
        if (advanced) {
            assertFilterSvcSingle(env, "s1", reboolExpressionText, REBOOL);
        }
        List<String> statementNames = Arrays.asList("s0", "s1");

        env.milestone(0);

        if (advanced) {
            assertSameFilterEntry(env, eventTypeName);
        }

        env.sendEventBean(eventReceived);
        assertReceived(env, statementNames, true);

        env.sendEventBean(eventNotReceived);
        assertReceived(env, statementNames, false);

        env.undeployAll();
    }

    public static String myStaticMethod(Object value) {
        SupportBean sb = (SupportBean) value;
        return sb.getTheString().startsWith("X") ? null : sb.getTheString();
    }
}
