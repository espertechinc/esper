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
package com.espertech.esper.regressionlib.suite.expr.define;

import com.espertech.esper.common.client.EventPropertyGetter;
import com.espertech.esper.common.client.scopetest.EPAssertionUtil;
import com.espertech.esper.common.client.soda.EPStatementFormatter;
import com.espertech.esper.common.client.soda.EPStatementObjectModel;
import com.espertech.esper.common.client.util.StatementProperty;
import com.espertech.esper.common.client.util.StatementType;
import com.espertech.esper.common.internal.support.EventRepresentationChoice;
import com.espertech.esper.common.internal.support.SupportBean;
import com.espertech.esper.common.internal.support.SupportBean_S0;
import com.espertech.esper.common.internal.support.SupportBean_S1;
import com.espertech.esper.regressionlib.framework.RegressionEnvironment;
import com.espertech.esper.regressionlib.framework.RegressionExecution;
import com.espertech.esper.regressionlib.framework.RegressionPath;
import com.espertech.esper.regressionlib.support.bean.*;
import com.espertech.esper.regressionlib.support.util.LambdaAssertionUtil;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;

import static com.espertech.esper.regressionlib.framework.SupportMessageAssertUtil.tryInvalidCompile;
import static org.junit.Assert.*;

public class ExprDefineBasic {

    private final static String NEWLINE = System.getProperty("line.separator");

    public static Collection<RegressionExecution> executions() {
        ArrayList<RegressionExecution> execs = new ArrayList<>();
        execs.add(new ExprDefineExpressionSimpleSameStmt());
        execs.add(new ExprDefineExpressionSimpleSameModule());
        execs.add(new ExprDefineExpressionSimpleTwoModule());
        execs.add(new ExprDefineAggregationNoAccess());
        execs.add(new ExprDefineAggregatedResult());
        execs.add(new ExprDefineAggregationAccess());
        execs.add(new ExprDefineWildcardAndPattern());
        execs.add(new ExprDefineScalarReturn());
        execs.add(new ExprDefineNoParameterArithmetic());
        execs.add(new ExprDefineOneParameterLambdaReturn());
        execs.add(new ExprDefineNoParameterVariable());
        execs.add(new ExprDefineAnnotationOrder());
        execs.add(new ExprDefineWhereClauseExpression());
        execs.add(new ExprDefineSequenceAndNested());
        execs.add(new ExprDefineCaseNewMultiReturnNoElse());
        execs.add(new ExprDefineSubqueryMultiresult());
        execs.add(new ExprDefineSubqueryCross());
        execs.add(new ExprDefineSubqueryJoinSameField());
        execs.add(new ExprDefineSubqueryCorrelated());
        execs.add(new ExprDefineSubqueryUncorrelated());
        execs.add(new ExprDefineSubqueryNamedWindowUncorrelated());
        execs.add(new ExprDefineSubqueryNamedWindowCorrelated());
        execs.add(new ExprDefineNestedExpressionMultiSubquery());
        execs.add(new ExprDefineEventTypeAndSODA());
        execs.add(new ExprDefineInvalid());
        execs.add(new ExprDefineSplitStream());
        return execs;
    }

    private static class ExprDefineExpressionSimpleSameStmt implements RegressionExecution {
        public void run(RegressionEnvironment env) {
            env.compileDeploy("@name('s0') expression returnsOne {1} select returnsOne as c0 from SupportBean").addListener("s0");
            assertEquals(StatementType.SELECT, env.statement("s0").getProperty(StatementProperty.STATEMENTTYPE));
            env.sendEventBean(new SupportBean());
            assertEquals(1, env.listener("s0").assertOneGetNewAndReset().get("c0"));
            env.undeployAll();
        }
    }

    private static class ExprDefineExpressionSimpleSameModule implements RegressionExecution {
        public void run(RegressionEnvironment env) {
            env.compileDeploy("create expression returnsOne {1};\n" +
                "@name('s0') select returnsOne as c0 from SupportBean;\n").addListener("s0");
            env.sendEventBean(new SupportBean());
            assertEquals(1, env.listener("s0").assertOneGetNewAndReset().get("c0"));
            env.undeployAll();
        }
    }

    private static class ExprDefineExpressionSimpleTwoModule implements RegressionExecution {
        public void run(RegressionEnvironment env) {
            RegressionPath path = new RegressionPath();
            env.compileDeploy("create expression returnsOne {1}", path);
            env.compileDeploy("@name('s0') select returnsOne as c0 from SupportBean", path).addListener("s0");
            env.sendEventBean(new SupportBean());
            assertEquals(1, env.listener("s0").assertOneGetNewAndReset().get("c0"));
            env.undeployAll();
        }
    }

    private static class ExprDefineNestedExpressionMultiSubquery implements RegressionExecution {
        public void run(RegressionEnvironment env) {
            String[] fields = "c0".split(",");

            RegressionPath path = new RegressionPath();
            env.compileDeploy("create expression F1 { (select intPrimitive from SupportBean#lastevent)}", path);
            env.compileDeploy("create expression F2 { param => (select a.intPrimitive from SupportBean#unique(theString) as a where a.theString = param.theString) }", path);
            env.compileDeploy("create expression F3 { s => F1()+F2(s) }", path);
            env.compileDeploy("@name('s0') select F3(myevent) as c0 from SupportBean as myevent", path).addListener("s0");

            env.sendEventBean(new SupportBean("E1", 10));
            EPAssertionUtil.assertProps(env.listener("s0").assertOneGetNewAndReset(), fields, new Object[]{20});

            env.sendEventBean(new SupportBean("E1", 11));
            EPAssertionUtil.assertProps(env.listener("s0").assertOneGetNewAndReset(), fields, new Object[]{22});

            env.undeployAll();
        }
    }

    private static class ExprDefineWildcardAndPattern implements RegressionExecution {
        public void run(RegressionEnvironment env) {
            String eplNonJoin =
                "@name('s0') expression abc { x => intPrimitive } " +
                    "expression def { (x, y) => x.intPrimitive * y.intPrimitive }" +
                    "select abc(*) as c0, def(*, *) as c1 from SupportBean";
            env.compileDeploy(eplNonJoin).addListener("s0");

            env.sendEventBean(new SupportBean("E1", 2));
            EPAssertionUtil.assertProps(env.listener("s0").assertOneGetNewAndReset(), "c0, c1".split(","), new Object[]{2, 4});
            env.undeployAll();

            String eplPattern = "@name('s0') expression abc { x => intPrimitive * 2} " +
                "select * from pattern [a=SupportBean -> b=SupportBean(intPrimitive = abc(a))]";
            env.compileDeploy(eplPattern).addListener("s0");

            env.sendEventBean(new SupportBean("E1", 2));
            env.sendEventBean(new SupportBean("E2", 4));
            EPAssertionUtil.assertProps(env.listener("s0").assertOneGetNewAndReset(), "a.theString, b.theString".split(","), new Object[]{"E1", "E2"});

            env.undeployAll();
        }
    }

    private static class ExprDefineSequenceAndNested implements RegressionExecution {
        public void run(RegressionEnvironment env) {
            RegressionPath path = new RegressionPath();
            env.compileDeploy("create window WindowOne#keepall as (col1 string, col2 string)", path);
            env.compileDeploy("insert into WindowOne select p00 as col1, p01 as col2 from SupportBean_S0", path);

            env.compileDeploy("create window WindowTwo#keepall as (col1 string, col2 string)", path);
            env.compileDeploy("insert into WindowTwo select p10 as col1, p11 as col2 from SupportBean_S1", path);

            env.sendEventBean(new SupportBean_S0(1, "A", "B1"));
            env.sendEventBean(new SupportBean_S0(2, "A", "B2"));

            env.sendEventBean(new SupportBean_S1(11, "A", "B1"));
            env.sendEventBean(new SupportBean_S1(12, "A", "B2"));

            String epl = "@name('s0') @Audit('exprdef') " +
                "expression last2X {\n" +
                "  p => WindowOne(WindowOne.col1 = p.theString).takeLast(2)\n" +
                "} " +
                "expression last2Y {\n" +
                "  p => WindowTwo(WindowTwo.col1 = p.theString).takeLast(2).selectFrom(q => q.col2)\n" +
                "} " +
                "select last2X(sb).selectFrom(a => a.col2).sequenceEqual(last2Y(sb)) as val from SupportBean as sb";
            env.compileDeploy(epl, path).addListener("s0");

            env.sendEventBean(new SupportBean("A", 1));
            assertEquals(true, env.listener("s0").assertOneGetNewAndReset().get("val"));

            env.undeployAll();
        }
    }

    private static class ExprDefineCaseNewMultiReturnNoElse implements RegressionExecution {
        public void run(RegressionEnvironment env) {
            RegressionPath path = new RegressionPath();
            String[] fieldsInner = "col1,col2".split(",");
            String epl = "@name('s0') expression gettotal {" +
                " x => case " +
                "  when theString = 'A' then new { col1 = 'X', col2 = 10 } " +
                "  when theString = 'B' then new { col1 = 'Y', col2 = 20 } " +
                "end" +
                "} " +
                "insert into OtherStream select gettotal(sb) as val0 from SupportBean sb";
            env.compileDeploy(epl, path).addListener("s0");

            assertEquals(Map.class, env.statement("s0").getEventType().getPropertyType("val0"));

            env.compileDeploy("@name('s1') select val0.col1 as c1, val0.col2 as c2 from OtherStream", path).addListener("s1");
            String[] fieldsConsume = "c1,c2".split(",");

            env.sendEventBean(new SupportBean("E1", 1));
            EPAssertionUtil.assertPropsMap((Map) env.listener("s0").assertOneGetNewAndReset().get("val0"), fieldsInner, new Object[]{null, null});
            EPAssertionUtil.assertProps(env.listener("s1").assertOneGetNewAndReset(), fieldsConsume, new Object[]{null, null});

            env.sendEventBean(new SupportBean("A", 2));
            EPAssertionUtil.assertPropsMap((Map) env.listener("s0").assertOneGetNewAndReset().get("val0"), fieldsInner, new Object[]{"X", 10});
            EPAssertionUtil.assertProps(env.listener("s1").assertOneGetNewAndReset(), fieldsConsume, new Object[]{"X", 10});

            env.sendEventBean(new SupportBean("B", 3));
            EPAssertionUtil.assertPropsMap((Map) env.listener("s0").assertOneGetNewAndReset().get("val0"), fieldsInner, new Object[]{"Y", 20});
            EPAssertionUtil.assertProps(env.listener("s1").assertOneGetNewAndReset(), fieldsConsume, new Object[]{"Y", 20});

            env.undeployAll();
        }
    }

    private static class ExprDefineAnnotationOrder implements RegressionExecution {
        public void run(RegressionEnvironment env) {
            String epl = "expression scalar {1} @Name('s0') select scalar() from SupportBean_ST0";
            tryAssertionAnnotation(env, epl);

            epl = "@Name('s0') expression scalar {1} select scalar() from SupportBean_ST0";
            tryAssertionAnnotation(env, epl);
        }

        private void tryAssertionAnnotation(RegressionEnvironment env, String epl) {
            env.compileDeploy(epl).addListener("s0");

            assertEquals(Integer.class, env.statement("s0").getEventType().getPropertyType("scalar()"));
            assertEquals("s0", env.statement("s0").getName());

            env.sendEventBean(new SupportBean_ST0("E1", 1));
            EPAssertionUtil.assertProps(env.listener("s0").assertOneGetNewAndReset(), "scalar()".split(","), new Object[]{1});

            env.undeployAll();
        }
    }

    private static class ExprDefineSubqueryMultiresult implements RegressionExecution {
        public void run(RegressionEnvironment env) {
            String eplOne = "@name('s0') " +
                "expression maxi {" +
                " (select max(intPrimitive) from SupportBean#keepall)" +
                "} " +
                "expression mini {" +
                " (select min(intPrimitive) from SupportBean#keepall)" +
                "} " +
                "select p00/maxi() as val0, p00/mini() as val1 " +
                "from SupportBean_ST0#lastevent";
            tryAssertionMultiResult(env, eplOne);

            String eplTwo = "@name('s0') " +
                "expression subq {" +
                " (select max(intPrimitive) as maxi, min(intPrimitive) as mini from SupportBean#keepall)" +
                "} " +
                "select p00/subq().maxi as val0, p00/subq().mini as val1 " +
                "from SupportBean_ST0#lastevent";
            tryAssertionMultiResult(env, eplTwo);

            String eplTwoAlias = "@name('s0') " +
                "expression subq alias for " +
                " { (select max(intPrimitive) as maxi, min(intPrimitive) as mini from SupportBean#keepall) }" +
                " " +
                "select p00/subq().maxi as val0, p00/subq().mini as val1 " +
                "from SupportBean_ST0#lastevent";
            tryAssertionMultiResult(env, eplTwoAlias);
        }

        private void tryAssertionMultiResult(RegressionEnvironment env, String epl) {
            String[] fields = new String[]{"val0", "val1"};
            env.compileDeploy(epl).addListener("s0");

            env.sendEventBean(new SupportBean("E1", 10));
            env.sendEventBean(new SupportBean("E2", 5));
            env.sendEventBean(new SupportBean_ST0("ST0", 2));
            EPAssertionUtil.assertProps(env.listener("s0").assertOneGetNewAndReset(), fields, new Object[]{2 / 10d, 2 / 5d});

            env.sendEventBean(new SupportBean("E3", 20));
            env.sendEventBean(new SupportBean("E4", 2));
            env.sendEventBean(new SupportBean_ST0("ST0", 4));
            EPAssertionUtil.assertProps(env.listener("s0").assertOneGetNewAndReset(), fields, new Object[]{4 / 20d, 4 / 2d});

            env.undeployAll();
        }
    }

    private static class ExprDefineSubqueryCross implements RegressionExecution {
        public void run(RegressionEnvironment env) {
            String eplDeclare = "@name('s0') expression subq {" +
                " (x, y) => (select theString from SupportBean#keepall where theString = x.id and intPrimitive = y.p10)" +
                "} " +
                "select subq(one, two) as val1 " +
                "from SupportBean_ST0#lastevent as one, SupportBean_ST1#lastevent as two";
            tryAssertionSubqueryCross(env, eplDeclare);

            String eplAlias = "@name('s0') expression subq alias for { (select theString from SupportBean#keepall where theString = one.id and intPrimitive = two.p10) }" +
                "select subq as val1 " +
                "from SupportBean_ST0#lastevent as one, SupportBean_ST1#lastevent as two";
            tryAssertionSubqueryCross(env, eplAlias);
        }

        private void tryAssertionSubqueryCross(RegressionEnvironment env, String epl) {
            String[] fields = new String[]{"val1"};
            env.compileDeploy(epl).addListener("s0");

            LambdaAssertionUtil.assertTypes(env.statement("s0").getEventType(), fields, new Class[]{String.class});

            env.sendEventBean(new SupportBean_ST0("ST0", 0));
            env.sendEventBean(new SupportBean_ST1("ST1", 20));
            EPAssertionUtil.assertProps(env.listener("s0").assertOneGetNewAndReset(), fields, new Object[]{null});

            env.sendEventBean(new SupportBean("ST0", 20));

            env.sendEventBean(new SupportBean_ST1("x", 20));
            EPAssertionUtil.assertProps(env.listener("s0").assertOneGetNewAndReset(), fields, new Object[]{"ST0"});

            env.undeployAll();
        }
    }

    private static class ExprDefineSubqueryJoinSameField implements RegressionExecution {
        public void run(RegressionEnvironment env) {
            String eplDeclare = "@name('s0') " +
                "expression subq {" +
                " x => (select intPrimitive from SupportBean#keepall where theString = x.pcommon)" +   // a common field
                "} " +
                "select subq(one) as val1, subq(two) as val2 " +
                "from SupportBean_ST0#lastevent as one, SupportBean_ST1#lastevent as two";
            tryAssertionSubqueryJoinSameField(env, eplDeclare);

            String eplAlias = "@name('s0') " +
                "expression subq alias for {(select intPrimitive from SupportBean#keepall where theString = pcommon) }" +
                "select subq as val1, subq as val2 " +
                "from SupportBean_ST0#lastevent as one, SupportBean_ST1#lastevent as two";
            tryInvalidCompile(env, eplAlias,
                "Failed to plan subquery number 1 querying SupportBean: Failed to validate filter expression 'theString=pcommon': Property named 'pcommon' is ambiguous as is valid for more then one stream");
        }

        private void tryAssertionSubqueryJoinSameField(RegressionEnvironment env, String epl) {
            String[] fields = new String[]{"val1", "val2"};
            env.compileDeploy(epl).addListener("s0");

            LambdaAssertionUtil.assertTypes(env.statement("s0").getEventType(), fields, new Class[]{Integer.class, Integer.class});

            env.sendEventBean(new SupportBean_ST0("ST0", 0));
            env.sendEventBean(new SupportBean_ST1("ST1", 0));
            EPAssertionUtil.assertProps(env.listener("s0").assertOneGetNewAndReset(), fields, new Object[]{null, null});

            env.sendEventBean(new SupportBean("E0", 10));
            env.sendEventBean(new SupportBean_ST1("ST1", 0, "E0"));
            EPAssertionUtil.assertProps(env.listener("s0").assertOneGetNewAndReset(), fields, new Object[]{null, 10});

            env.sendEventBean(new SupportBean_ST0("ST0", 0, "E0"));
            EPAssertionUtil.assertProps(env.listener("s0").assertOneGetNewAndReset(), fields, new Object[]{10, 10});

            env.undeployAll();
        }
    }

    private static class ExprDefineSubqueryCorrelated implements RegressionExecution {
        public void run(RegressionEnvironment env) {
            String eplDeclare = "@name('s0') expression subqOne {" +
                " x => (select id from SupportBean_ST0#keepall where p00 = x.intPrimitive)" +
                "} " +
                "select theString as val0, subqOne(t) as val1 from SupportBean as t";
            tryAssertionSubqueryCorrelated(env, eplDeclare);

            String eplAlias = "@name('s0') expression subqOne alias for {(select id from SupportBean_ST0#keepall where p00 = t.intPrimitive)} " +
                "select theString as val0, subqOne() as val1 from SupportBean as t";
            tryAssertionSubqueryCorrelated(env, eplAlias);
        }

        private void tryAssertionSubqueryCorrelated(RegressionEnvironment env, String epl) {
            String[] fields = new String[]{"val0", "val1"};
            env.compileDeploy(epl).addListener("s0");

            LambdaAssertionUtil.assertTypes(env.statement("s0").getEventType(), fields, new Class[]{String.class, String.class});

            env.sendEventBean(new SupportBean("E0", 0));
            EPAssertionUtil.assertProps(env.listener("s0").assertOneGetNewAndReset(), fields, new Object[]{"E0", null});

            env.sendEventBean(new SupportBean_ST0("ST0", 100));
            env.sendEventBean(new SupportBean("E1", 99));
            EPAssertionUtil.assertProps(env.listener("s0").assertOneGetNewAndReset(), fields, new Object[]{"E1", null});

            env.sendEventBean(new SupportBean("E2", 100));
            EPAssertionUtil.assertProps(env.listener("s0").assertOneGetNewAndReset(), fields, new Object[]{"E2", "ST0"});

            env.sendEventBean(new SupportBean_ST0("ST1", 100));
            env.sendEventBean(new SupportBean("E3", 100));
            EPAssertionUtil.assertProps(env.listener("s0").assertOneGetNewAndReset(), fields, new Object[]{"E3", null});

            env.undeployAll();
        }
    }

    private static class ExprDefineSubqueryUncorrelated implements RegressionExecution {
        public void run(RegressionEnvironment env) {
            String eplDeclare = "@name('s0') expression subqOne {(select id from SupportBean_ST0#lastevent)} " +
                "select theString as val0, subqOne() as val1 from SupportBean as t";
            tryAssertionSubqueryUncorrelated(env, eplDeclare);

            String eplAlias = "@name('s0') expression subqOne alias for {(select id from SupportBean_ST0#lastevent)} " +
                "select theString as val0, subqOne as val1 from SupportBean as t";
            tryAssertionSubqueryUncorrelated(env, eplAlias);
        }

        private void tryAssertionSubqueryUncorrelated(RegressionEnvironment env, String epl) {

            String[] fields = new String[]{"val0", "val1"};
            env.compileDeploy(epl).addListener("s0");

            LambdaAssertionUtil.assertTypes(env.statement("s0").getEventType(), fields, new Class[]{String.class, String.class});

            env.sendEventBean(new SupportBean("E0", 0));
            EPAssertionUtil.assertProps(env.listener("s0").assertOneGetNewAndReset(), fields, new Object[]{"E0", null});

            env.sendEventBean(new SupportBean_ST0("ST0", 0));
            env.sendEventBean(new SupportBean("E1", 99));
            EPAssertionUtil.assertProps(env.listener("s0").assertOneGetNewAndReset(), fields, new Object[]{"E1", "ST0"});

            env.sendEventBean(new SupportBean_ST0("ST1", 0));
            env.sendEventBean(new SupportBean("E2", 100));
            EPAssertionUtil.assertProps(env.listener("s0").assertOneGetNewAndReset(), fields, new Object[]{"E2", "ST1"});

            env.undeployAll();
        }
    }

    private static class ExprDefineSubqueryNamedWindowUncorrelated implements RegressionExecution {
        public void run(RegressionEnvironment env) {
            String eplDeclare = "@name('s0') expression subqnamedwin { MyWindow.where(x => x.val1 > 10).orderBy(x => x.val0) } " +
                "select subqnamedwin() as c0, subqnamedwin().where(x => x.val1 < 100) as c1 from SupportBean_ST0 as t";
            tryAssertionSubqueryNamedWindowUncorrelated(env, eplDeclare);

            String eplAlias = "@name('s0') expression subqnamedwin alias for {MyWindow.where(x => x.val1 > 10).orderBy(x => x.val0)}" +
                "select subqnamedwin as c0, subqnamedwin.where(x => x.val1 < 100) as c1 from SupportBean_ST0";
            tryAssertionSubqueryNamedWindowUncorrelated(env, eplAlias);
        }

        private void tryAssertionSubqueryNamedWindowUncorrelated(RegressionEnvironment env, String epl) {

            String[] fieldsSelected = "c0,c1".split(",");
            String[] fieldsInside = "val0".split(",");

            RegressionPath path = new RegressionPath();
            env.compileDeploy(EventRepresentationChoice.MAP.getAnnotationText() + " create window MyWindow#keepall as (val0 string, val1 int)", path);
            env.compileDeploy("insert into MyWindow (val0, val1) select theString, intPrimitive from SupportBean", path);
            env.compileDeploy(epl, path).addListener("s0");

            LambdaAssertionUtil.assertTypes(env.statement("s0").getEventType(), fieldsSelected, new Class[]{Collection.class, Collection.class});

            env.sendEventBean(new SupportBean("E0", 0));
            env.sendEventBean(new SupportBean_ST0("ID0", 0));
            EPAssertionUtil.assertPropsPerRow(toArrayMap((Collection) env.listener("s0").assertOneGetNew().get("c0")), fieldsInside, null);
            EPAssertionUtil.assertPropsPerRow(toArrayMap((Collection) env.listener("s0").assertOneGetNew().get("c1")), fieldsInside, null);
            env.listener("s0").reset();

            env.sendEventBean(new SupportBean("E1", 11));
            env.sendEventBean(new SupportBean_ST0("ID1", 0));
            EPAssertionUtil.assertPropsPerRow(toArrayMap((Collection) env.listener("s0").assertOneGetNew().get("c0")), fieldsInside, new Object[][]{{"E1"}});
            EPAssertionUtil.assertPropsPerRow(toArrayMap((Collection) env.listener("s0").assertOneGetNew().get("c1")), fieldsInside, new Object[][]{{"E1"}});
            env.listener("s0").reset();

            env.sendEventBean(new SupportBean("E2", 500));
            env.sendEventBean(new SupportBean_ST0("ID2", 0));
            EPAssertionUtil.assertPropsPerRow(toArrayMap((Collection) env.listener("s0").assertOneGetNew().get("c0")), fieldsInside, new Object[][]{{"E1"}, {"E2"}});
            EPAssertionUtil.assertPropsPerRow(toArrayMap((Collection) env.listener("s0").assertOneGetNew().get("c1")), fieldsInside, new Object[][]{{"E1"}});
            env.listener("s0").reset();

            env.undeployAll();
        }
    }

    private static class ExprDefineSubqueryNamedWindowCorrelated implements RegressionExecution {
        public void run(RegressionEnvironment env) {

            String epl = "@name('s0') expression subqnamedwin {" +
                "  x => MyWindow(val0 = x.key0).where(y => val1 > 10)" +
                "} " +
                "select subqnamedwin(t) as c0 from SupportBean_ST0 as t";
            tryAssertionSubqNWCorrelated(env, epl);

            // more or less prefixes
            epl = "@name('s0') expression subqnamedwin {" +
                "  x => MyWindow(val0 = x.key0).where(y => y.val1 > 10)" +
                "} " +
                "select subqnamedwin(t) as c0 from SupportBean_ST0 as t";
            tryAssertionSubqNWCorrelated(env, epl);

            // with property-explicit stream name
            epl = "@name('s0') expression subqnamedwin {" +
                "  x => MyWindow(MyWindow.val0 = x.key0).where(y => y.val1 > 10)" +
                "} " +
                "select subqnamedwin(t) as c0 from SupportBean_ST0 as t";
            tryAssertionSubqNWCorrelated(env, epl);

            // with alias
            epl = "@name('s0') expression subqnamedwin alias for {MyWindow(MyWindow.val0 = t.key0).where(y => y.val1 > 10)}" +
                "select subqnamedwin as c0 from SupportBean_ST0 as t";
            tryAssertionSubqNWCorrelated(env, epl);

            // test ambiguous property names
            RegressionPath path = new RegressionPath();
            env.compileDeploy(EventRepresentationChoice.MAP.getAnnotationText() + " create window MyWindowTwo#keepall as (id string, p00 int)", path);
            env.compileDeploy("insert into MyWindowTwo (id, p00) select theString, intPrimitive from SupportBean", path);
            epl = "expression subqnamedwin {" +
                "  x => MyWindowTwo(MyWindowTwo.id = x.id).where(y => y.p00 > 10)" +
                "} " +
                "select subqnamedwin(t) as c0 from SupportBean_ST0 as t";
            env.compileDeploy(epl, path);
            env.undeployAll();
        }

        private void tryAssertionSubqNWCorrelated(RegressionEnvironment env, String epl) {
            String[] fieldSelected = "c0".split(",");
            String[] fieldInside = "val0".split(",");

            RegressionPath path = new RegressionPath();
            env.compileDeploy(EventRepresentationChoice.MAP.getAnnotationText() + " create window MyWindow#keepall as (val0 string, val1 int)", path);
            env.compileDeploy("insert into MyWindow (val0, val1) select theString, intPrimitive from SupportBean", path);
            env.compileDeploy(epl, path).addListener("s0");

            LambdaAssertionUtil.assertTypes(env.statement("s0").getEventType(), fieldSelected, new Class[]{Collection.class});

            env.sendEventBean(new SupportBean("E0", 0));
            env.sendEventBean(new SupportBean_ST0("ID0", "x", 0));
            EPAssertionUtil.assertPropsPerRow(toArrayMap((Collection) env.listener("s0").assertOneGetNew().get("c0")), fieldInside, null);
            env.listener("s0").reset();

            env.sendEventBean(new SupportBean("E1", 11));
            env.sendEventBean(new SupportBean_ST0("ID1", "x", 0));
            EPAssertionUtil.assertPropsPerRow(toArrayMap((Collection) env.listener("s0").assertOneGetNew().get("c0")), fieldInside, null);
            env.listener("s0").reset();

            env.sendEventBean(new SupportBean("E2", 12));
            env.sendEventBean(new SupportBean_ST0("ID2", "E2", 0));
            EPAssertionUtil.assertPropsPerRow(toArrayMap((Collection) env.listener("s0").assertOneGetNew().get("c0")), fieldInside, new Object[][]{{"E2"}});
            env.listener("s0").reset();

            env.sendEventBean(new SupportBean("E3", 13));
            env.sendEventBean(new SupportBean_ST0("E3", "E3", 0));
            EPAssertionUtil.assertPropsPerRow(toArrayMap((Collection) env.listener("s0").assertOneGetNew().get("c0")), fieldInside, new Object[][]{{"E3"}});
            env.listener("s0").reset();

            env.undeployAll();
        }
    }

    private static class ExprDefineAggregationNoAccess implements RegressionExecution {
        public void run(RegressionEnvironment env) {
            String[] fields = new String[]{"val1", "val2", "val3", "val4"};
            String epl = "@name('s0') " +
                "expression sumA {x => " +
                "   sum(x.intPrimitive) " +
                "} " +
                "expression sumB {x => " +
                "   sum(x.intBoxed) " +
                "} " +
                "expression countC {" +
                "   count(*) " +
                "} " +
                "select sumA(t) as val1, sumB(t) as val2, sumA(t)/sumB(t) as val3, countC() as val4 from SupportBean as t";

            env.compileDeploy(epl).addListener("s0");

            LambdaAssertionUtil.assertTypes(env.statement("s0").getEventType(), fields, new Class[]{Integer.class, Integer.class, Double.class, Long.class});

            env.sendEventBean(getSupportBean(5, 6));
            EPAssertionUtil.assertProps(env.listener("s0").assertOneGetNewAndReset(), fields, new Object[]{5, 6, 5 / 6d, 1L});

            env.sendEventBean(getSupportBean(8, 10));
            EPAssertionUtil.assertProps(env.listener("s0").assertOneGetNewAndReset(), fields, new Object[]{5 + 8, 6 + 10, (5 + 8) / (6d + 10d), 2L});

            env.undeployAll();
        }
    }

    private static class ExprDefineSplitStream implements RegressionExecution {
        public void run(RegressionEnvironment env) {
            RegressionPath path = new RegressionPath();
            String epl = "@name('split') expression myLittleExpression { event => false }" +
                "on SupportBean as myEvent " +
                " insert into ABC select * where myLittleExpression(myEvent)" +
                " insert into DEF select * where not myLittleExpression(myEvent)";
            env.compileDeploy(epl, path);

            env.compileDeploy("@name('s0') select * from DEF", path).addListener("s0");
            env.sendEventBean(new SupportBean());
            assertTrue(env.listener("s0").isInvoked());

            env.undeployAll();
        }
    }

    private static class ExprDefineAggregationAccess implements RegressionExecution {
        public void run(RegressionEnvironment env) {
            String eplDeclare = "@name('s0') expression wb {s => window(*).where(y => y.intPrimitive > 2) }" +
                "select wb(t) as val1 from SupportBean#keepall as t";
            tryAssertionAggregationAccess(env, eplDeclare);

            String eplAlias = "@name('s0') expression wb alias for {window(*).where(y => y.intPrimitive > 2)}" +
                "select wb as val1 from SupportBean#keepall as t";
            tryAssertionAggregationAccess(env, eplAlias);
        }
    }

    private static class ExprDefineAggregatedResult implements RegressionExecution {
        public void run(RegressionEnvironment env) {
            String[] fields = "c0,c1".split(",");
            String epl = "@name('s0') expression lambda1 { o => 1 * o.intPrimitive }\n" +
                "expression lambda2 { o => 3 * o.intPrimitive }\n" +
                "select sum(lambda1(e)) as c0, sum(lambda2(e)) as c1 from SupportBean as e";
            env.compileDeploy(epl).addListener("s0");

            env.sendEventBean(new SupportBean("E1", 10));
            EPAssertionUtil.assertProps(env.listener("s0").assertOneGetNewAndReset(), fields, new Object[]{10, 30});

            env.sendEventBean(new SupportBean("E2", 5));
            EPAssertionUtil.assertProps(env.listener("s0").assertOneGetNewAndReset(), fields, new Object[]{15, 45});

            env.undeployAll();
        }
    }

    private static class ExprDefineScalarReturn implements RegressionExecution {
        public void run(RegressionEnvironment env) {
            String eplScalarDeclare = "@name('s0') expression scalarfilter {s => strvals.where(y => y != 'E1') } " +
                "select scalarfilter(t).where(x => x != 'E2') as val1 from SupportCollection as t";
            tryAssertionScalarReturn(env, eplScalarDeclare);

            String eplScalarAlias = "@name('s0') expression scalarfilter alias for {strvals.where(y => y != 'E1')}" +
                "select scalarfilter.where(x => x != 'E2') as val1 from SupportCollection";
            tryAssertionScalarReturn(env, eplScalarAlias);

            // test with cast and with on-select and where-clause use
            String inner = "case when myEvent.one = 'X' then 0 else cast(myEvent.one, long) end ";
            String eplCaseDeclare = "@name('s0') expression theExpression { myEvent => " + inner + "} " +
                "on SupportBeanObject as myEvent select mw.* from MyWindowFirst as mw where mw.myObject = theExpression(myEvent)";
            tryAssertionNamedWindowCast(env, eplCaseDeclare, "First");

            String eplCaseAlias = "@name('s0') expression theExpression alias for {" + inner + "}" +
                "on SupportBeanObject as myEvent select mw.* from MyWindowSecond as mw where mw.myObject = theExpression";
            tryAssertionNamedWindowCast(env, eplCaseAlias, "Second");
        }

        private void tryAssertionNamedWindowCast(RegressionEnvironment env, String epl, String windowPostfix) {

            RegressionPath path = new RegressionPath();
            env.compileDeploy("create window MyWindow" + windowPostfix + "#keepall as (myObject long)", path);
            env.compileDeploy("insert into MyWindow" + windowPostfix + "(myObject) select cast(intPrimitive, long) from SupportBean", path);
            env.compileDeploy(epl, path).addListener("s0");

            String[] props = new String[]{"myObject"};

            env.sendEventBean(new SupportBean("E1", 0));
            env.sendEventBean(new SupportBean("E2", 1));

            env.sendEventBean(new SupportBeanObject(2));
            assertFalse(env.listener("s0").isInvoked());

            env.sendEventBean(new SupportBeanObject("X"));
            EPAssertionUtil.assertProps(env.listener("s0").assertOneGetNewAndReset(), props, new Object[]{0L});

            env.sendEventBean(new SupportBeanObject(1));
            EPAssertionUtil.assertProps(env.listener("s0").assertOneGetNewAndReset(), props, new Object[]{1L});

            env.undeployAll();
        }

        private void tryAssertionScalarReturn(RegressionEnvironment env, String epl) {
            env.compileDeploy(epl).addListener("s0");

            LambdaAssertionUtil.assertTypes(env.statement("s0").getEventType(), "val1".split(","), new Class[]{Collection.class});

            env.sendEventBean(SupportCollection.makeString("E1,E2,E3,E4"));
            LambdaAssertionUtil.assertValuesArrayScalar(env.listener("s0"), "val1", "E3", "E4");
            env.listener("s0").reset();

            env.undeployAll();
        }
    }

    private static class ExprDefineEventTypeAndSODA implements RegressionExecution {
        public void run(RegressionEnvironment env) {

            String[] fields = new String[]{"fZero()", "fOne(t)", "fTwo(t,t)", "fThree(t,t)"};
            String eplDeclared = "@Name('s0') " +
                "expression fZero {10} " +
                "expression fOne {x => x.intPrimitive} " +
                "expression fTwo {(x,y) => x.intPrimitive+y.intPrimitive} " +
                "expression fThree {(x,y) => x.intPrimitive+100} " +
                "select fZero(), fOne(t), fTwo(t,t), fThree(t,t) from SupportBean as t";
            String eplFormatted = "@Name('s0')" + NEWLINE +
                "expression fZero {10}" + NEWLINE +
                "expression fOne {x => x.intPrimitive}" + NEWLINE +
                "expression fTwo {(x,y) => x.intPrimitive+y.intPrimitive}" + NEWLINE +
                "expression fThree {(x,y) => x.intPrimitive+100}" + NEWLINE +
                "select fZero(), fOne(t), fTwo(t,t), fThree(t,t)" + NEWLINE +
                "from SupportBean as t";
            env.compileDeploy(eplDeclared).addListener("s0");
            tryAssertionTwoParameterArithmetic(env, fields);
            env.undeployAll();

            EPStatementObjectModel model = env.eplToModel(eplDeclared);
            assertEquals(eplDeclared, model.toEPL());
            assertEquals(eplFormatted, model.toEPL(new EPStatementFormatter(true)));
            env.compileDeploy(model).addListener("s0");

            tryAssertionTwoParameterArithmetic(env, fields);
            env.undeployAll();

            String eplAlias = "@name('s0') " +
                "expression fZero alias for {10} " +
                "expression fOne alias for {intPrimitive} " +
                "expression fTwo alias for {intPrimitive+intPrimitive} " +
                "expression fThree alias for {intPrimitive+100} " +
                "select fZero, fOne, fTwo, fThree from SupportBean";
            env.compileDeploy(eplAlias).addListener("s0");
            tryAssertionTwoParameterArithmetic(env, new String[]{"fZero", "fOne", "fTwo", "fThree"});
            env.undeployAll();
        }

        private void tryAssertionTwoParameterArithmetic(RegressionEnvironment env, String[] fields) {
            String[] props = env.statement("s0").getEventType().getPropertyNames();
            EPAssertionUtil.assertEqualsAnyOrder(props, fields);
            assertEquals(Integer.class, env.statement("s0").getEventType().getPropertyType(fields[0]));
            assertEquals(Integer.class, env.statement("s0").getEventType().getPropertyType(fields[1]));
            assertEquals(Integer.class, env.statement("s0").getEventType().getPropertyType(fields[2]));
            assertEquals(Integer.class, env.statement("s0").getEventType().getPropertyType(fields[3]));
            EventPropertyGetter getter = env.statement("s0").getEventType().getGetter(fields[3]);

            env.sendEventBean(new SupportBean("E1", 11));
            EPAssertionUtil.assertProps(env.listener("s0").assertOneGetNew(), fields, new Object[]{10, 11, 22, 111});
            assertEquals(111, getter.get(env.listener("s0").assertOneGetNewAndReset()));
        }
    }

    private static class ExprDefineOneParameterLambdaReturn implements RegressionExecution {
        public void run(RegressionEnvironment env) {

            String eplDeclare = "" +
                "@name('s0') expression one {x1 => x1.contained.where(y => y.p00 < 10) } " +
                "expression two {x2 => one(x2).where(y => y.p00 > 1)  } " +
                "select one(s0c) as val1, two(s0c) as val2 from SupportBean_ST0_Container as s0c";
            tryAssertionOneParameterLambdaReturn(env, eplDeclare);

            String eplAliasWParen = "" +
                "@name('s0') expression one alias for {contained.where(y => y.p00 < 10)}" +
                "expression two alias for {one().where(y => y.p00 > 1)}" +
                "select one as val1, two as val2 from SupportBean_ST0_Container as s0c";
            tryAssertionOneParameterLambdaReturn(env, eplAliasWParen);

            String eplAliasNoParen = "" +
                "@name('s0') expression one alias for {contained.where(y => y.p00 < 10)}" +
                "expression two alias for {one.where(y => y.p00 > 1)}" +
                "select one as val1, two as val2 from SupportBean_ST0_Container as s0c";
            tryAssertionOneParameterLambdaReturn(env, eplAliasNoParen);
        }

        private void tryAssertionOneParameterLambdaReturn(RegressionEnvironment env, String epl) {

            env.compileDeploy(epl).addListener("s0");

            LambdaAssertionUtil.assertTypes(env.statement("s0").getEventType(), "val1,val2".split(","), new Class[]{Collection.class, Collection.class});

            SupportBean_ST0_Container theEvent = SupportBean_ST0_Container.make3Value("E1,K1,1", "E2,K2,2", "E20,K20,20");
            env.sendEventBean(theEvent);
            Object[] resultVal1 = ((Collection) env.listener("s0").getLastNewData()[0].get("val1")).toArray();
            EPAssertionUtil.assertEqualsExactOrder(new Object[]{theEvent.getContained().get(0), theEvent.getContained().get(1)}, resultVal1);
            Object[] resultVal2 = ((Collection) env.listener("s0").getLastNewData()[0].get("val2")).toArray();
            EPAssertionUtil.assertEqualsExactOrder(new Object[]{theEvent.getContained().get(1)}, resultVal2);

            env.undeployAll();
        }
    }

    private static class ExprDefineNoParameterArithmetic implements RegressionExecution {
        public void run(RegressionEnvironment env) {

            String eplDeclared = "@name('s0') expression getEnumerationSource {1} " +
                "select getEnumerationSource() as val1, getEnumerationSource()*5 as val2 from SupportBean";
            tryAssertionNoParameterArithmetic(env, eplDeclared);

            String eplDeclaredNoParen = "@name('s0') expression getEnumerationSource {1} " +
                "select getEnumerationSource as val1, getEnumerationSource*5 as val2 from SupportBean";
            tryAssertionNoParameterArithmetic(env, eplDeclaredNoParen);

            String eplAlias = "@name('s0') expression getEnumerationSource alias for {1} " +
                "select getEnumerationSource as val1, getEnumerationSource*5 as val2 from SupportBean";
            tryAssertionNoParameterArithmetic(env, eplAlias);
        }

        private void tryAssertionNoParameterArithmetic(RegressionEnvironment env, String epl) {

            String[] fields = "val1,val2".split(",");
            env.compileDeploy(epl).addListener("s0");

            LambdaAssertionUtil.assertTypes(env.statement("s0").getEventType(), fields, new Class[]{Integer.class, Integer.class});

            env.sendEventBean(new SupportBean());
            EPAssertionUtil.assertProps(env.listener("s0").assertOneGetNewAndReset(), fields, new Object[]{1, 5});

            env.undeployAll();
        }
    }

    private static class ExprDefineNoParameterVariable implements RegressionExecution {
        public void run(RegressionEnvironment env) {
            String eplDeclared = "@name('s0') expression one {myvar} " +
                "expression two {myvar * 10} " +
                "select one() as val1, two() as val2, one() * two() as val3 from SupportBean";
            tryAssertionNoParameterVariable(env, eplDeclared);

            String eplAlias = "@name('s0') expression one alias for {myvar} " +
                "expression two alias for {myvar * 10} " +
                "select one() as val1, two() as val2, one * two as val3 from SupportBean";
            tryAssertionNoParameterVariable(env, eplAlias);
        }

        private void tryAssertionNoParameterVariable(RegressionEnvironment env, String epl) {

            RegressionPath path = new RegressionPath();
            env.compileDeploy("@name('var') create variable int myvar = 2", path);

            String[] fields = "val1,val2,val3".split(",");
            env.compileDeploy(epl, path).addListener("s0");

            LambdaAssertionUtil.assertTypes(env.statement("s0").getEventType(), fields, new Class[]{Integer.class, Integer.class, Integer.class});

            env.sendEventBean(new SupportBean());
            EPAssertionUtil.assertProps(env.listener("s0").assertOneGetNewAndReset(), fields, new Object[]{2, 20, 40});

            env.runtime().getVariableService().setVariableValue(env.deploymentId("var"), "myvar", 3);
            env.sendEventBean(new SupportBean());
            EPAssertionUtil.assertProps(env.listener("s0").assertOneGetNewAndReset(), fields, new Object[]{3, 30, 90});

            env.undeployAll();
        }
    }

    private static class ExprDefineWhereClauseExpression implements RegressionExecution {
        public void run(RegressionEnvironment env) {
            String eplNoAlias = "@name('s0') expression one {x=>x.boolPrimitive} select * from SupportBean as sb where one(sb)";
            tryAssertionWhereClauseExpression(env, eplNoAlias);

            String eplAlias = "@name('s0') expression one alias for {boolPrimitive} select * from SupportBean as sb where one";
            tryAssertionWhereClauseExpression(env, eplAlias);
        }

        private void tryAssertionWhereClauseExpression(RegressionEnvironment env, String epl) {
            env.compileDeploy(epl).addListener("s0");

            env.sendEventBean(new SupportBean());
            assertFalse(env.listener("s0").getAndClearIsInvoked());

            SupportBean theEvent = new SupportBean();
            theEvent.setBoolPrimitive(true);
            env.sendEventBean(theEvent);
            assertTrue(env.listener("s0").getAndClearIsInvoked());

            env.undeployAll();
        }
    }

    private static class ExprDefineInvalid implements RegressionExecution {
        public void run(RegressionEnvironment env) {

            String epl = "expression abc {(select * from SupportBean_ST0#lastevent as st0 where p00=intPrimitive)} select abc() from SupportBean";
            tryInvalidCompile(env, epl, "Failed to plan subquery number 1 querying SupportBean_ST0: Failed to validate filter expression 'p00=intPrimitive': Property named 'intPrimitive' is not valid in any stream [expression abc {(select * from SupportBean_ST0#lastevent as st0 where p00=intPrimitive)} select abc() from SupportBean]");

            epl = "expression abc {x=>strvals.where(x=> x != 'E1')} select abc(str) from SupportCollection str";
            tryInvalidCompile(env, epl, "Failed to validate select-clause expression 'abc(str)': Error validating expression declaration 'abc': Failed to validate declared expression body expression 'strvals.where()': Error validating enumeration method 'where', the lambda-parameter name 'x' has already been declared in this context [expression abc {x=>strvals.where(x=> x != 'E1')} select abc(str) from SupportCollection str]");

            epl = "expression abc {avg(intPrimitive)} select abc() from SupportBean";
            tryInvalidCompile(env, epl, "Failed to validate select-clause expression 'abc()': Error validating expression declaration 'abc': Failed to validate declared expression body expression 'avg(intPrimitive)': Property named 'intPrimitive' is not valid in any stream [expression abc {avg(intPrimitive)} select abc() from SupportBean]");

            epl = "expression abc {(select * from SupportBean_ST0#lastevent as st0 where p00=sb.intPrimitive)} select abc() from SupportBean sb";
            tryInvalidCompile(env, epl, "Failed to plan subquery number 1 querying SupportBean_ST0: Failed to validate filter expression 'p00=sb.intPrimitive': Failed to find a stream named 'sb' (did you mean 'st0'?) [expression abc {(select * from SupportBean_ST0#lastevent as st0 where p00=sb.intPrimitive)} select abc() from SupportBean sb]");

            epl = "expression abc {window(*)} select abc() from SupportBean";
            tryInvalidCompile(env, epl, "Failed to validate select-clause expression 'abc()': Error validating expression declaration 'abc': Failed to validate declared expression body expression 'window(*)': The 'window' aggregation function requires that at least one stream is provided [expression abc {window(*)} select abc() from SupportBean]");

            epl = "expression abc {x => intPrimitive} select abc() from SupportBean";
            tryInvalidCompile(env, epl, "Failed to validate select-clause expression 'abc()': Parameter count mismatches for declared expression 'abc', expected 1 parameters but received 0 parameters [expression abc {x => intPrimitive} select abc() from SupportBean]");

            epl = "expression abc {intPrimitive} select abc(sb) from SupportBean sb";
            tryInvalidCompile(env, epl, "Failed to validate select-clause expression 'abc(sb)': Parameter count mismatches for declared expression 'abc', expected 0 parameters but received 1 parameters [expression abc {intPrimitive} select abc(sb) from SupportBean sb]");

            epl = "expression abc {x=>} select abc(sb) from SupportBean sb";
            tryInvalidCompile(env, epl, "Incorrect syntax near '}' at line 1 column 19 near reserved keyword 'select' [expression abc {x=>} select abc(sb) from SupportBean sb]");

            epl = "expression abc {intPrimitive} select abc() from SupportBean sb";
            tryInvalidCompile(env, epl, "Failed to validate select-clause expression 'abc()': Error validating expression declaration 'abc': Failed to validate declared expression body expression 'intPrimitive': Property named 'intPrimitive' is not valid in any stream [expression abc {intPrimitive} select abc() from SupportBean sb]");

            epl = "expression abc {x=>intPrimitive} select * from SupportBean sb where abc(sb)";
            tryInvalidCompile(env, epl, "Filter expression not returning a boolean value: 'abc(sb)' [expression abc {x=>intPrimitive} select * from SupportBean sb where abc(sb)]");

            epl = "expression abc {x=>x.intPrimitive = 0} select * from SupportBean#lastevent sb1, SupportBean#lastevent sb2 where abc(*)";
            tryInvalidCompile(env, epl, "Error validating expression: Failed to validate filter expression 'abc(*)': Expression 'abc' only allows a wildcard parameter if there is a single stream available, please use a stream or tag name instead [expression abc {x=>x.intPrimitive = 0} select * from SupportBean#lastevent sb1, SupportBean#lastevent sb2 where abc(*)]");

            epl = "expression ABC alias for {1} select ABC(t) from SupportBean as t";
            tryInvalidCompile(env, epl, "Failed to validate select-clause expression 'ABC': Expression 'ABC is an expression-alias and does not allow parameters [expression ABC alias for {1} select ABC(t) from SupportBean as t]");
        }
    }

    private static void tryAssertionAggregationAccess(RegressionEnvironment env, String epl) {

        env.compileDeploy(epl).addListener("s0");
        LambdaAssertionUtil.assertTypes(env.statement("s0").getEventType(), "val1".split(","), new Class[]{Collection.class, Collection.class});

        env.sendEventBean(new SupportBean("E1", 2));
        SupportBean[] outArray = toArray((Collection) env.listener("s0").assertOneGetNewAndReset().get("val1"));
        assertEquals(0, outArray.length);

        env.sendEventBean(new SupportBean("E2", 3));
        outArray = toArray((Collection) env.listener("s0").assertOneGetNewAndReset().get("val1"));
        assertEquals(1, outArray.length);
        assertEquals("E2", outArray[0].getTheString());

        env.undeployAll();
    }

    private static SupportBean getSupportBean(int intPrimitive, Integer intBoxed) {
        SupportBean b = new SupportBean(null, intPrimitive);
        b.setIntBoxed(intBoxed);
        return b;
    }

    private static SupportBean[] toArray(Collection it) {
        List<SupportBean> result = new ArrayList<>();
        for (Object item : it) {
            result.add((SupportBean) item);
        }
        return result.toArray(new SupportBean[result.size()]);
    }

    private static Map[] toArrayMap(Collection it) {
        if (it == null) {
            return null;
        }
        List<Map> result = new ArrayList<Map>();
        for (Object item : it) {
            Map map = (Map) item;
            result.add(map);
        }
        return result.toArray(new Map[result.size()]);
    }
}
