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
package com.espertech.esper.regression.expr.expressiondef;

import com.espertech.esper.client.Configuration;
import com.espertech.esper.client.EPServiceProvider;
import com.espertech.esper.client.EPStatement;
import com.espertech.esper.client.EventPropertyGetter;
import com.espertech.esper.client.scopetest.EPAssertionUtil;
import com.espertech.esper.client.scopetest.SupportUpdateListener;
import com.espertech.esper.client.soda.EPStatementFormatter;
import com.espertech.esper.client.soda.EPStatementObjectModel;
import com.espertech.esper.supportregression.bean.*;
import com.espertech.esper.supportregression.bean.lambda.LambdaAssertionUtil;
import com.espertech.esper.supportregression.execution.RegressionExecution;
import com.espertech.esper.support.EventRepresentationChoice;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;

import static com.espertech.esper.supportregression.util.SupportMessageAssertUtil.tryInvalid;
import static org.junit.Assert.*;

public class ExecExpressionDef implements RegressionExecution {

    private final static String NEWLINE = System.getProperty("line.separator");

    public void configure(Configuration configuration) throws Exception {
        configuration.addEventType("SupportBean", SupportBean.class);
        configuration.addEventType("SupportBean_ST0", SupportBean_ST0.class);
        configuration.addEventType("SupportBean_ST1", SupportBean_ST1.class);
        configuration.addEventType("SupportBean_ST0_Container", SupportBean_ST0_Container.class);
        configuration.addEventType("SupportCollection", SupportCollection.class);
        configuration.getEngineDefaults().getLogging().setEnableCode(true);
    }

    public void run(EPServiceProvider epService) throws Exception {
        runAssertionNestedExpressionMultiSubquery(epService);
        runAssertionWildcardAndPattern(epService);
        runAssertionSequenceAndNested(epService);
        runAssertionCaseNewMultiReturnNoElse(epService);
        runAssertionAnnotationOrder(epService);
        runAssertionSubqueryMultiresult(epService);
        runAssertionSubqueryCross(epService);
        runAssertionSubqueryJoinSameField(epService);
        runAssertionSubqueryCorrelated(epService);
        runAssertionSubqueryUncorrelated(epService);
        runAssertionSubqueryNamedWindowUncorrelated(epService);
        runAssertionSubqueryNamedWindowCorrelated(epService);
        runAssertionAggregationNoAccess(epService);
        runAssertionSplitStream(epService);
        runAssertionAggregationAccess(epService);
        runAssertionAggregatedResult(epService);
        runAssertionScalarReturn(epService);
        runAssertionEventTypeAndSODA(epService);
        runAssertionOneParameterLambdaReturn(epService);
        runAssertionNoParameterArithmetic(epService);
        runAssertionNoParameterVariable(epService);
        runAssertionWhereClauseExpression(epService);
        runAssertionInvalid(epService);
    }

    private void runAssertionNestedExpressionMultiSubquery(EPServiceProvider epService) {
        String[] fields = "c0".split(",");
        SupportUpdateListener listener = new SupportUpdateListener();

        epService.getEPAdministrator().createEPL("create expression F1 { (select intPrimitive from SupportBean#lastevent)}");
        epService.getEPAdministrator().createEPL("create expression F2 { param => (select a.intPrimitive from SupportBean#unique(theString) as a where a.theString = param.theString) }");
        epService.getEPAdministrator().createEPL("create expression F3 { s => F1()+F2(s) }");
        epService.getEPAdministrator().createEPL("select F3(myevent) as c0 from SupportBean as myevent").addListener(listener);

        epService.getEPRuntime().sendEvent(new SupportBean("E1", 10));
        EPAssertionUtil.assertProps(listener.assertOneGetNewAndReset(), fields, new Object[]{20});

        epService.getEPRuntime().sendEvent(new SupportBean("E1", 11));
        EPAssertionUtil.assertProps(listener.assertOneGetNewAndReset(), fields, new Object[]{22});

        epService.getEPAdministrator().destroyAllStatements();
    }

    private void runAssertionWildcardAndPattern(EPServiceProvider epService) {
        String eplNonJoin =
                "expression abc { x => intPrimitive } " +
                        "expression def { (x, y) => x.intPrimitive * y.intPrimitive }" +
                        "select abc(*) as c0, def(*, *) as c1 from SupportBean";
        SupportUpdateListener listener = new SupportUpdateListener();
        epService.getEPAdministrator().createEPL(eplNonJoin).addListener(listener);

        epService.getEPRuntime().sendEvent(new SupportBean("E1", 2));
        EPAssertionUtil.assertProps(listener.assertOneGetNewAndReset(), "c0, c1".split(","), new Object[]{2, 4});
        epService.getEPAdministrator().destroyAllStatements();

        String eplPattern = "expression abc { x => intPrimitive * 2} " +
                "select * from pattern [a=SupportBean -> b=SupportBean(intPrimitive = abc(a))]";
        epService.getEPAdministrator().createEPL(eplPattern).addListener(listener);

        epService.getEPRuntime().sendEvent(new SupportBean("E1", 2));
        epService.getEPRuntime().sendEvent(new SupportBean("E2", 4));
        EPAssertionUtil.assertProps(listener.assertOneGetNewAndReset(), "a.theString, b.theString".split(","), new Object[]{"E1", "E2"});

        epService.getEPAdministrator().destroyAllStatements();
    }

    private void runAssertionSequenceAndNested(EPServiceProvider epService) {
        epService.getEPAdministrator().getConfiguration().addEventType("SupportBean_S0", SupportBean_S0.class);
        epService.getEPAdministrator().createEPL("create window WindowOne#keepall as (col1 string, col2 string)");
        epService.getEPAdministrator().createEPL("insert into WindowOne select p00 as col1, p01 as col2 from SupportBean_S0");

        epService.getEPAdministrator().getConfiguration().addEventType("SupportBean_S1", SupportBean_S1.class);
        epService.getEPAdministrator().createEPL("create window WindowTwo#keepall as (col1 string, col2 string)");
        epService.getEPAdministrator().createEPL("insert into WindowTwo select p10 as col1, p11 as col2 from SupportBean_S1");

        epService.getEPRuntime().sendEvent(new SupportBean_S0(1, "A", "B1"));
        epService.getEPRuntime().sendEvent(new SupportBean_S0(2, "A", "B2"));

        epService.getEPRuntime().sendEvent(new SupportBean_S1(11, "A", "B1"));
        epService.getEPRuntime().sendEvent(new SupportBean_S1(12, "A", "B2"));

        String epl =
                "@Audit('exprdef') " +
                        "expression last2X {\n" +
                        "  p => WindowOne(WindowOne.col1 = p.theString).takeLast(2)\n" +
                        "} " +
                        "expression last2Y {\n" +
                        "  p => WindowTwo(WindowTwo.col1 = p.theString).takeLast(2).selectFrom(q => q.col2)\n" +
                        "} " +
                        "select last2X(sb).selectFrom(a => a.col2).sequenceEqual(last2Y(sb)) as val from SupportBean as sb";
        EPStatement stmt = epService.getEPAdministrator().createEPL(epl);
        SupportUpdateListener listener = new SupportUpdateListener();
        stmt.addListener(listener);

        epService.getEPRuntime().sendEvent(new SupportBean("A", 1));
        assertEquals(true, listener.assertOneGetNewAndReset().get("val"));

        epService.getEPAdministrator().destroyAllStatements();
    }

    private void runAssertionCaseNewMultiReturnNoElse(EPServiceProvider epService) {

        String[] fieldsInner = "col1,col2".split(",");
        String epl = "expression gettotal {" +
                " x => case " +
                "  when theString = 'A' then new { col1 = 'X', col2 = 10 } " +
                "  when theString = 'B' then new { col1 = 'Y', col2 = 20 } " +
                "end" +
                "} " +
                "insert into OtherStream select gettotal(sb) as val0 from SupportBean sb";
        EPStatement stmt = epService.getEPAdministrator().createEPL(epl);
        SupportUpdateListener listener = new SupportUpdateListener();
        stmt.addListener(listener);
        assertEquals(Map.class, stmt.getEventType().getPropertyType("val0"));

        SupportUpdateListener listenerTwo = new SupportUpdateListener();
        epService.getEPAdministrator().createEPL("select val0.col1 as c1, val0.col2 as c2 from OtherStream").addListener(listenerTwo);
        String[] fieldsConsume = "c1,c2".split(",");

        epService.getEPRuntime().sendEvent(new SupportBean("E1", 1));
        EPAssertionUtil.assertPropsMap((Map) listener.assertOneGetNewAndReset().get("val0"), fieldsInner, new Object[]{null, null});
        EPAssertionUtil.assertProps(listenerTwo.assertOneGetNewAndReset(), fieldsConsume, new Object[]{null, null});

        epService.getEPRuntime().sendEvent(new SupportBean("A", 2));
        EPAssertionUtil.assertPropsMap((Map) listener.assertOneGetNewAndReset().get("val0"), fieldsInner, new Object[]{"X", 10});
        EPAssertionUtil.assertProps(listenerTwo.assertOneGetNewAndReset(), fieldsConsume, new Object[]{"X", 10});

        epService.getEPRuntime().sendEvent(new SupportBean("B", 3));
        EPAssertionUtil.assertPropsMap((Map) listener.assertOneGetNewAndReset().get("val0"), fieldsInner, new Object[]{"Y", 20});
        EPAssertionUtil.assertProps(listenerTwo.assertOneGetNewAndReset(), fieldsConsume, new Object[]{"Y", 20});

        stmt.destroy();
    }

    private void runAssertionAnnotationOrder(EPServiceProvider epService) {
        String epl = "expression scalar {1} @Name('test') select scalar() from SupportBean_ST0";
        tryAssertionAnnotation(epService, epl);

        epl = "@Name('test') expression scalar {1} select scalar() from SupportBean_ST0";
        tryAssertionAnnotation(epService, epl);
    }

    private void tryAssertionAnnotation(EPServiceProvider epService, String epl) {
        EPStatement stmt = epService.getEPAdministrator().createEPL(epl);
        SupportUpdateListener listener = new SupportUpdateListener();
        stmt.addListener(listener);

        assertEquals(Integer.class, stmt.getEventType().getPropertyType("scalar()"));
        assertEquals("test", stmt.getName());

        epService.getEPRuntime().sendEvent(new SupportBean_ST0("E1", 1));
        EPAssertionUtil.assertProps(listener.assertOneGetNewAndReset(), "scalar()".split(","), new Object[]{1});

        stmt.destroy();
    }

    private void runAssertionSubqueryMultiresult(EPServiceProvider epService) {
        String eplOne = "" +
                "expression maxi {" +
                " (select max(intPrimitive) from SupportBean#keepall)" +
                "} " +
                "expression mini {" +
                " (select min(intPrimitive) from SupportBean#keepall)" +
                "} " +
                "select p00/maxi() as val0, p00/mini() as val1 " +
                "from SupportBean_ST0#lastevent";
        tryAssertionMultiResult(epService, eplOne);

        String eplTwo = "" +
                "expression subq {" +
                " (select max(intPrimitive) as maxi, min(intPrimitive) as mini from SupportBean#keepall)" +
                "} " +
                "select p00/subq().maxi as val0, p00/subq().mini as val1 " +
                "from SupportBean_ST0#lastevent";
        tryAssertionMultiResult(epService, eplTwo);

        String eplTwoAlias = "" +
                "expression subq alias for " +
                " { (select max(intPrimitive) as maxi, min(intPrimitive) as mini from SupportBean#keepall) }" +
                " " +
                "select p00/subq().maxi as val0, p00/subq().mini as val1 " +
                "from SupportBean_ST0#lastevent";
        tryAssertionMultiResult(epService, eplTwoAlias);
    }

    private void tryAssertionMultiResult(EPServiceProvider epService, String epl) {
        String[] fields = new String[]{"val0", "val1"};

        EPStatement stmt = epService.getEPAdministrator().createEPL(epl);
        SupportUpdateListener listener = new SupportUpdateListener();
        stmt.addListener(listener);

        epService.getEPRuntime().sendEvent(new SupportBean("E1", 10));
        epService.getEPRuntime().sendEvent(new SupportBean("E2", 5));
        epService.getEPRuntime().sendEvent(new SupportBean_ST0("ST0", 2));
        EPAssertionUtil.assertProps(listener.assertOneGetNewAndReset(), fields, new Object[]{2 / 10d, 2 / 5d});

        epService.getEPRuntime().sendEvent(new SupportBean("E3", 20));
        epService.getEPRuntime().sendEvent(new SupportBean("E4", 2));
        epService.getEPRuntime().sendEvent(new SupportBean_ST0("ST0", 4));
        EPAssertionUtil.assertProps(listener.assertOneGetNewAndReset(), fields, new Object[]{4 / 20d, 4 / 2d});

        stmt.destroy();
    }

    private void runAssertionSubqueryCross(EPServiceProvider epService) {
        String eplDeclare = "expression subq {" +
                " (x, y) => (select theString from SupportBean#keepall where theString = x.id and intPrimitive = y.p10)" +
                "} " +
                "select subq(one, two) as val1 " +
                "from SupportBean_ST0#lastevent as one, SupportBean_ST1#lastevent as two";
        tryAssertionSubqueryCross(epService, eplDeclare);

        String eplAlias = "expression subq alias for { (select theString from SupportBean#keepall where theString = one.id and intPrimitive = two.p10) }" +
                "select subq as val1 " +
                "from SupportBean_ST0#lastevent as one, SupportBean_ST1#lastevent as two";
        tryAssertionSubqueryCross(epService, eplAlias);
    }

    private void tryAssertionSubqueryCross(EPServiceProvider epService, String epl) {
        String[] fields = new String[]{"val1"};
        EPStatement stmt = epService.getEPAdministrator().createEPL(epl);
        SupportUpdateListener listener = new SupportUpdateListener();
        stmt.addListener(listener);
        LambdaAssertionUtil.assertTypes(stmt.getEventType(), fields, new Class[]{String.class});

        epService.getEPRuntime().sendEvent(new SupportBean_ST0("ST0", 0));
        epService.getEPRuntime().sendEvent(new SupportBean_ST1("ST1", 20));
        EPAssertionUtil.assertProps(listener.assertOneGetNewAndReset(), fields, new Object[]{null});

        epService.getEPRuntime().sendEvent(new SupportBean("ST0", 20));

        epService.getEPRuntime().sendEvent(new SupportBean_ST1("x", 20));
        EPAssertionUtil.assertProps(listener.assertOneGetNewAndReset(), fields, new Object[]{"ST0"});

        epService.getEPAdministrator().destroyAllStatements();
    }

    private void runAssertionSubqueryJoinSameField(EPServiceProvider epService) {
        String eplDeclare = "" +
                "expression subq {" +
                " x => (select intPrimitive from SupportBean#keepall where theString = x.pcommon)" +   // a common field
                "} " +
                "select subq(one) as val1, subq(two) as val2 " +
                "from SupportBean_ST0#lastevent as one, SupportBean_ST1#lastevent as two";
        tryAssertionSubqueryJoinSameField(epService, eplDeclare);

        String eplAlias = "" +
                "expression subq alias for {(select intPrimitive from SupportBean#keepall where theString = pcommon) }" +
                "select subq as val1, subq as val2 " +
                "from SupportBean_ST0#lastevent as one, SupportBean_ST1#lastevent as two";
        tryInvalid(epService, eplAlias,
                "Error starting statement: Failed to plan subquery number 1 querying SupportBean: Failed to validate filter expression 'theString=pcommon': Property named 'pcommon' is ambiguous as is valid for more then one stream [expression subq alias for {(select intPrimitive from SupportBean#keepall where theString = pcommon) }select subq as val1, subq as val2 from SupportBean_ST0#lastevent as one, SupportBean_ST1#lastevent as two]");
    }

    private void tryAssertionSubqueryJoinSameField(EPServiceProvider epService, String epl) {
        String[] fields = new String[]{"val1", "val2"};
        EPStatement stmt = epService.getEPAdministrator().createEPL(epl);
        SupportUpdateListener listener = new SupportUpdateListener();
        stmt.addListener(listener);
        LambdaAssertionUtil.assertTypes(stmt.getEventType(), fields, new Class[]{Integer.class, Integer.class});

        epService.getEPRuntime().sendEvent(new SupportBean_ST0("ST0", 0));
        epService.getEPRuntime().sendEvent(new SupportBean_ST1("ST1", 0));
        EPAssertionUtil.assertProps(listener.assertOneGetNewAndReset(), fields, new Object[]{null, null});

        epService.getEPRuntime().sendEvent(new SupportBean("E0", 10));
        epService.getEPRuntime().sendEvent(new SupportBean_ST1("ST1", 0, "E0"));
        EPAssertionUtil.assertProps(listener.assertOneGetNewAndReset(), fields, new Object[]{null, 10});

        epService.getEPRuntime().sendEvent(new SupportBean_ST0("ST0", 0, "E0"));
        EPAssertionUtil.assertProps(listener.assertOneGetNewAndReset(), fields, new Object[]{10, 10});

        epService.getEPAdministrator().destroyAllStatements();
    }

    private void runAssertionSubqueryCorrelated(EPServiceProvider epService) {
        String eplDeclare = "expression subqOne {" +
                " x => (select id from SupportBean_ST0#keepall where p00 = x.intPrimitive)" +
                "} " +
                "select theString as val0, subqOne(t) as val1 from SupportBean as t";
        tryAssertionSubqueryCorrelated(epService, eplDeclare);

        String eplAlias = "expression subqOne alias for {(select id from SupportBean_ST0#keepall where p00 = t.intPrimitive)} " +
                "select theString as val0, subqOne(t) as val1 from SupportBean as t";
        tryAssertionSubqueryCorrelated(epService, eplAlias);
    }

    private void tryAssertionSubqueryCorrelated(EPServiceProvider epService, String epl) {
        String[] fields = new String[]{"val0", "val1"};
        EPStatement stmt = epService.getEPAdministrator().createEPL(epl);
        SupportUpdateListener listener = new SupportUpdateListener();
        stmt.addListener(listener);
        LambdaAssertionUtil.assertTypes(stmt.getEventType(), fields, new Class[]{String.class, String.class});

        epService.getEPRuntime().sendEvent(new SupportBean("E0", 0));
        EPAssertionUtil.assertProps(listener.assertOneGetNewAndReset(), fields, new Object[]{"E0", null});

        epService.getEPRuntime().sendEvent(new SupportBean_ST0("ST0", 100));
        epService.getEPRuntime().sendEvent(new SupportBean("E1", 99));
        EPAssertionUtil.assertProps(listener.assertOneGetNewAndReset(), fields, new Object[]{"E1", null});

        epService.getEPRuntime().sendEvent(new SupportBean("E2", 100));
        EPAssertionUtil.assertProps(listener.assertOneGetNewAndReset(), fields, new Object[]{"E2", "ST0"});

        epService.getEPRuntime().sendEvent(new SupportBean_ST0("ST1", 100));
        epService.getEPRuntime().sendEvent(new SupportBean("E3", 100));
        EPAssertionUtil.assertProps(listener.assertOneGetNewAndReset(), fields, new Object[]{"E3", null});

        epService.getEPAdministrator().destroyAllStatements();
    }

    private void runAssertionSubqueryUncorrelated(EPServiceProvider epService) {
        String eplDeclare = "expression subqOne {(select id from SupportBean_ST0#lastevent)} " +
                "select theString as val0, subqOne() as val1 from SupportBean as t";
        tryAssertionSubqueryUncorrelated(epService, eplDeclare);

        String eplAlias = "expression subqOne alias for {(select id from SupportBean_ST0#lastevent)} " +
                "select theString as val0, subqOne as val1 from SupportBean as t";
        tryAssertionSubqueryUncorrelated(epService, eplAlias);
    }

    private void tryAssertionSubqueryUncorrelated(EPServiceProvider epService, String epl) {

        String[] fields = new String[]{"val0", "val1"};
        EPStatement stmt = epService.getEPAdministrator().createEPL(epl);
        SupportUpdateListener listener = new SupportUpdateListener();
        stmt.addListener(listener);
        LambdaAssertionUtil.assertTypes(stmt.getEventType(), fields, new Class[]{String.class, String.class});

        epService.getEPRuntime().sendEvent(new SupportBean("E0", 0));
        EPAssertionUtil.assertProps(listener.assertOneGetNewAndReset(), fields, new Object[]{"E0", null});

        epService.getEPRuntime().sendEvent(new SupportBean_ST0("ST0", 0));
        epService.getEPRuntime().sendEvent(new SupportBean("E1", 99));
        EPAssertionUtil.assertProps(listener.assertOneGetNewAndReset(), fields, new Object[]{"E1", "ST0"});

        epService.getEPRuntime().sendEvent(new SupportBean_ST0("ST1", 0));
        epService.getEPRuntime().sendEvent(new SupportBean("E2", 100));
        EPAssertionUtil.assertProps(listener.assertOneGetNewAndReset(), fields, new Object[]{"E2", "ST1"});

        epService.getEPAdministrator().destroyAllStatements();
    }

    private void runAssertionSubqueryNamedWindowUncorrelated(EPServiceProvider epService) {
        String eplDeclare = "expression subqnamedwin { MyWindow.where(x => x.val1 > 10).orderBy(x => x.val0) } " +
                "select subqnamedwin() as c0, subqnamedwin().where(x => x.val1 < 100) as c1 from SupportBean_ST0 as t";
        tryAssertionSubqueryNamedWindowUncorrelated(epService, eplDeclare);

        String eplAlias = "expression subqnamedwin alias for {MyWindow.where(x => x.val1 > 10).orderBy(x => x.val0)}" +
                "select subqnamedwin as c0, subqnamedwin.where(x => x.val1 < 100) as c1 from SupportBean_ST0";
        tryAssertionSubqueryNamedWindowUncorrelated(epService, eplAlias);
    }

    private void tryAssertionSubqueryNamedWindowUncorrelated(EPServiceProvider epService, String epl) {

        String[] fieldsSelected = "c0,c1".split(",");
        String[] fieldsInside = "val0".split(",");

        epService.getEPAdministrator().createEPL(EventRepresentationChoice.MAP.getAnnotationText() + " create window MyWindow#keepall as (val0 string, val1 int)");
        epService.getEPAdministrator().createEPL("insert into MyWindow (val0, val1) select theString, intPrimitive from SupportBean");

        EPStatement stmt = epService.getEPAdministrator().createEPL(epl);
        SupportUpdateListener listener = new SupportUpdateListener();
        stmt.addListener(listener);
        LambdaAssertionUtil.assertTypes(stmt.getEventType(), fieldsSelected, new Class[]{Collection.class, Collection.class});

        epService.getEPRuntime().sendEvent(new SupportBean("E0", 0));
        epService.getEPRuntime().sendEvent(new SupportBean_ST0("ID0", 0));
        EPAssertionUtil.assertPropsPerRow(toArrayMap((Collection) listener.assertOneGetNew().get("c0")), fieldsInside, null);
        EPAssertionUtil.assertPropsPerRow(toArrayMap((Collection) listener.assertOneGetNew().get("c1")), fieldsInside, null);
        listener.reset();

        epService.getEPRuntime().sendEvent(new SupportBean("E1", 11));
        epService.getEPRuntime().sendEvent(new SupportBean_ST0("ID1", 0));
        EPAssertionUtil.assertPropsPerRow(toArrayMap((Collection) listener.assertOneGetNew().get("c0")), fieldsInside, new Object[][]{{"E1"}});
        EPAssertionUtil.assertPropsPerRow(toArrayMap((Collection) listener.assertOneGetNew().get("c1")), fieldsInside, new Object[][]{{"E1"}});
        listener.reset();

        epService.getEPRuntime().sendEvent(new SupportBean("E2", 500));
        epService.getEPRuntime().sendEvent(new SupportBean_ST0("ID2", 0));
        EPAssertionUtil.assertPropsPerRow(toArrayMap((Collection) listener.assertOneGetNew().get("c0")), fieldsInside, new Object[][]{{"E1"}, {"E2"}});
        EPAssertionUtil.assertPropsPerRow(toArrayMap((Collection) listener.assertOneGetNew().get("c1")), fieldsInside, new Object[][]{{"E1"}});
        listener.reset();

        epService.getEPAdministrator().destroyAllStatements();
    }

    private void runAssertionSubqueryNamedWindowCorrelated(EPServiceProvider epService) {

        String epl = "expression subqnamedwin {" +
                "  x => MyWindow(val0 = x.key0).where(y => val1 > 10)" +
                "} " +
                "select subqnamedwin(t) as c0 from SupportBean_ST0 as t";
        tryAssertionSubqNWCorrelated(epService, epl);

        // more or less prefixes
        epl = "expression subqnamedwin {" +
                "  x => MyWindow(val0 = x.key0).where(y => y.val1 > 10)" +
                "} " +
                "select subqnamedwin(t) as c0 from SupportBean_ST0 as t";
        tryAssertionSubqNWCorrelated(epService, epl);

        // with property-explicit stream name
        epl = "expression subqnamedwin {" +
                "  x => MyWindow(MyWindow.val0 = x.key0).where(y => y.val1 > 10)" +
                "} " +
                "select subqnamedwin(t) as c0 from SupportBean_ST0 as t";
        tryAssertionSubqNWCorrelated(epService, epl);

        // with alias
        epl = "expression subqnamedwin alias for {MyWindow(MyWindow.val0 = t.key0).where(y => y.val1 > 10)}" +
                "select subqnamedwin as c0 from SupportBean_ST0 as t";
        tryAssertionSubqNWCorrelated(epService, epl);

        // test ambiguous property names
        epService.getEPAdministrator().createEPL(EventRepresentationChoice.MAP.getAnnotationText() + " create window MyWindowTwo#keepall as (id string, p00 int)");
        epService.getEPAdministrator().createEPL("insert into MyWindowTwo (id, p00) select theString, intPrimitive from SupportBean");
        epl = "expression subqnamedwin {" +
                "  x => MyWindowTwo(MyWindowTwo.id = x.id).where(y => y.p00 > 10)" +
                "} " +
                "select subqnamedwin(t) as c0 from SupportBean_ST0 as t";
        epService.getEPAdministrator().createEPL(epl);
    }

    private void tryAssertionSubqNWCorrelated(EPServiceProvider epService, String epl) {
        String[] fieldSelected = "c0".split(",");
        String[] fieldInside = "val0".split(",");

        epService.getEPAdministrator().createEPL(EventRepresentationChoice.MAP.getAnnotationText() + " create window MyWindow#keepall as (val0 string, val1 int)");
        epService.getEPAdministrator().createEPL("insert into MyWindow (val0, val1) select theString, intPrimitive from SupportBean");
        EPStatement stmt = epService.getEPAdministrator().createEPL(epl);
        SupportUpdateListener listener = new SupportUpdateListener();
        stmt.addListener(listener);
        LambdaAssertionUtil.assertTypes(stmt.getEventType(), fieldSelected, new Class[]{Collection.class});

        epService.getEPRuntime().sendEvent(new SupportBean("E0", 0));
        epService.getEPRuntime().sendEvent(new SupportBean_ST0("ID0", "x", 0));
        EPAssertionUtil.assertPropsPerRow(toArrayMap((Collection) listener.assertOneGetNew().get("c0")), fieldInside, null);
        listener.reset();

        epService.getEPRuntime().sendEvent(new SupportBean("E1", 11));
        epService.getEPRuntime().sendEvent(new SupportBean_ST0("ID1", "x", 0));
        EPAssertionUtil.assertPropsPerRow(toArrayMap((Collection) listener.assertOneGetNew().get("c0")), fieldInside, null);
        listener.reset();

        epService.getEPRuntime().sendEvent(new SupportBean("E2", 12));
        epService.getEPRuntime().sendEvent(new SupportBean_ST0("ID2", "E2", 0));
        EPAssertionUtil.assertPropsPerRow(toArrayMap((Collection) listener.assertOneGetNew().get("c0")), fieldInside, new Object[][]{{"E2"}});
        listener.reset();

        epService.getEPRuntime().sendEvent(new SupportBean("E3", 13));
        epService.getEPRuntime().sendEvent(new SupportBean_ST0("E3", "E3", 0));
        EPAssertionUtil.assertPropsPerRow(toArrayMap((Collection) listener.assertOneGetNew().get("c0")), fieldInside, new Object[][]{{"E3"}});
        listener.reset();

        epService.getEPAdministrator().destroyAllStatements();
    }

    private void runAssertionAggregationNoAccess(EPServiceProvider epService) {
        String[] fields = new String[]{"val1", "val2", "val3", "val4"};
        String epl = "" +
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

        EPStatement stmt = epService.getEPAdministrator().createEPL(epl);
        SupportUpdateListener listener = new SupportUpdateListener();
        stmt.addListener(listener);
        LambdaAssertionUtil.assertTypes(stmt.getEventType(), fields, new Class[]{Integer.class, Integer.class, Double.class, Long.class});

        epService.getEPRuntime().sendEvent(getSupportBean(5, 6));
        EPAssertionUtil.assertProps(listener.assertOneGetNewAndReset(), fields, new Object[]{5, 6, 5 / 6d, 1L});

        epService.getEPRuntime().sendEvent(getSupportBean(8, 10));
        EPAssertionUtil.assertProps(listener.assertOneGetNewAndReset(), fields, new Object[]{5 + 8, 6 + 10, (5 + 8) / (6d + 10d), 2L});

        stmt.destroy();
    }

    private void runAssertionSplitStream(EPServiceProvider epService) {
        String epl = "expression myLittleExpression { event => false }" +
                "on SupportBean as myEvent " +
                " insert into ABC select * where myLittleExpression(myEvent)" +
                " insert into DEF select * where not myLittleExpression(myEvent)";
        epService.getEPAdministrator().createEPL(epl);
        SupportUpdateListener listener = new SupportUpdateListener();

        epService.getEPAdministrator().createEPL("select * from DEF").addListener(listener);
        epService.getEPRuntime().sendEvent(new SupportBean());
        assertTrue(listener.isInvoked());
    }

    private void runAssertionAggregationAccess(EPServiceProvider epService) {
        String eplDeclare = "expression wb {s => window(*).where(y => y.intPrimitive > 2) }" +
                "select wb(t) as val1 from SupportBean#keepall as t";
        tryAssertionAggregationAccess(epService, eplDeclare);

        String eplAlias = "expression wb alias for {window(*).where(y => y.intPrimitive > 2)}" +
                "select wb as val1 from SupportBean#keepall as t";
        tryAssertionAggregationAccess(epService, eplAlias);
    }

    private void runAssertionAggregatedResult(EPServiceProvider epService) {
        String[] fields = "c0,c1".split(",");
        String epl =
                "expression lambda1 { o => 1 * o.intPrimitive }\n" +
                        "expression lambda2 { o => 3 * o.intPrimitive }\n" +
                        "select sum(lambda1(e)) as c0, sum(lambda2(e)) as c1 from SupportBean as e";
        SupportUpdateListener listener = new SupportUpdateListener();
        epService.getEPAdministrator().createEPL(epl).addListener(listener);

        epService.getEPRuntime().sendEvent(new SupportBean("E1", 10));
        EPAssertionUtil.assertProps(listener.assertOneGetNewAndReset(), fields, new Object[]{10, 30});

        epService.getEPRuntime().sendEvent(new SupportBean("E2", 5));
        EPAssertionUtil.assertProps(listener.assertOneGetNewAndReset(), fields, new Object[]{15, 45});

        epService.getEPAdministrator().destroyAllStatements();
    }

    private void tryAssertionAggregationAccess(EPServiceProvider epService, String epl) {

        EPStatement stmt = epService.getEPAdministrator().createEPL(epl);
        SupportUpdateListener listener = new SupportUpdateListener();
        stmt.addListener(listener);
        LambdaAssertionUtil.assertTypes(stmt.getEventType(), "val1".split(","), new Class[]{Collection.class, Collection.class});

        epService.getEPRuntime().sendEvent(new SupportBean("E1", 2));
        SupportBean[] outArray = toArray((Collection) listener.assertOneGetNewAndReset().get("val1"));
        assertEquals(0, outArray.length);

        epService.getEPRuntime().sendEvent(new SupportBean("E2", 3));
        outArray = toArray((Collection) listener.assertOneGetNewAndReset().get("val1"));
        assertEquals(1, outArray.length);
        assertEquals("E2", outArray[0].getTheString());

        epService.getEPAdministrator().destroyAllStatements();
    }

    private void runAssertionScalarReturn(EPServiceProvider epService) {
        epService.getEPAdministrator().getConfiguration().addEventType(MyEvent.class);
        epService.getEPAdministrator().getConfiguration().addEventType(SupportBean.class);

        String eplScalarDeclare = "expression scalarfilter {s => strvals.where(y => y != 'E1') } " +
                "select scalarfilter(t).where(x => x != 'E2') as val1 from SupportCollection as t";
        tryAssertionScalarReturn(epService, eplScalarDeclare);

        String eplScalarAlias = "expression scalarfilter alias for {strvals.where(y => y != 'E1')}" +
                "select scalarfilter.where(x => x != 'E2') as val1 from SupportCollection";
        tryAssertionScalarReturn(epService, eplScalarAlias);

        // test with cast and with on-select and where-clause use
        String inner = "case when myEvent.myObject = 'X' then 0 else cast(myEvent.myObject, long) end ";
        String eplCaseDeclare = "expression theExpression { myEvent => " + inner + "} " +
                "on MyEvent as myEvent select mw.* from MyWindowFirst as mw where mw.myObject = theExpression(myEvent)";
        tryAssertionNamedWindowCast(epService, eplCaseDeclare, "First");

        String eplCaseAlias = "expression theExpression alias for {" + inner + "}" +
                "on MyEvent as myEvent select mw.* from MyWindowSecond as mw where mw.myObject = theExpression";
        tryAssertionNamedWindowCast(epService, eplCaseAlias, "Second");
    }

    private void tryAssertionNamedWindowCast(EPServiceProvider epService, String epl, String windowPostfix) {

        epService.getEPAdministrator().createEPL("create window MyWindow" + windowPostfix + "#keepall as (myObject long)");
        epService.getEPAdministrator().createEPL("insert into MyWindow" + windowPostfix + "(myObject) select cast(intPrimitive, long) from SupportBean");
        EPStatement stmt = epService.getEPAdministrator().createEPL(epl);
        SupportUpdateListener listener = new SupportUpdateListener();
        stmt.addListener(listener);
        String[] props = new String[]{"myObject"};

        epService.getEPRuntime().sendEvent(new SupportBean("E1", 0));
        epService.getEPRuntime().sendEvent(new SupportBean("E2", 1));

        epService.getEPRuntime().sendEvent(new MyEvent(2));
        assertFalse(listener.isInvoked());

        epService.getEPRuntime().sendEvent(new MyEvent("X"));
        EPAssertionUtil.assertProps(listener.assertOneGetNewAndReset(), props, new Object[]{0L});

        epService.getEPRuntime().sendEvent(new MyEvent(1));
        EPAssertionUtil.assertProps(listener.assertOneGetNewAndReset(), props, new Object[]{1L});

        epService.getEPAdministrator().destroyAllStatements();
    }

    private void tryAssertionScalarReturn(EPServiceProvider epService, String epl) {
        EPStatement stmt = epService.getEPAdministrator().createEPL(epl);
        SupportUpdateListener listener = new SupportUpdateListener();
        stmt.addListener(listener);
        LambdaAssertionUtil.assertTypes(stmt.getEventType(), "val1".split(","), new Class[]{Collection.class});

        epService.getEPRuntime().sendEvent(SupportCollection.makeString("E1,E2,E3,E4"));
        LambdaAssertionUtil.assertValuesArrayScalar(listener, "val1", "E3", "E4");
        listener.reset();

        epService.getEPAdministrator().destroyAllStatements();
    }

    private void runAssertionEventTypeAndSODA(EPServiceProvider epService) {

        String[] fields = new String[]{"fZero()", "fOne(t)", "fTwo(t,t)", "fThree(t,t)"};
        String eplDeclared = "" +
                "expression fZero {10} " +
                "expression fOne {x => x.intPrimitive} " +
                "expression fTwo {(x,y) => x.intPrimitive+y.intPrimitive} " +
                "expression fThree {(x,y) => x.intPrimitive+100} " +
                "select fZero(), fOne(t), fTwo(t,t), fThree(t,t) from SupportBean as t";
        String eplFormatted = "" +
                "expression fZero {10}" + NEWLINE +
                "expression fOne {x => x.intPrimitive}" + NEWLINE +
                "expression fTwo {(x,y) => x.intPrimitive+y.intPrimitive}" + NEWLINE +
                "expression fThree {(x,y) => x.intPrimitive+100}" + NEWLINE +
                "select fZero(), fOne(t), fTwo(t,t), fThree(t,t)" + NEWLINE +
                "from SupportBean as t";
        EPStatement stmt = epService.getEPAdministrator().createEPL(eplDeclared);
        SupportUpdateListener listener = new SupportUpdateListener();
        stmt.addListener(listener);

        tryAssertionTwoParameterArithmetic(epService, listener, stmt, fields);

        stmt.destroy();
        EPStatementObjectModel model = epService.getEPAdministrator().compileEPL(eplDeclared);
        assertEquals(eplDeclared, model.toEPL());
        assertEquals(eplFormatted, model.toEPL(new EPStatementFormatter(true)));
        stmt = epService.getEPAdministrator().create(model);
        assertEquals(eplDeclared, stmt.getText());
        stmt.addListener(listener);

        tryAssertionTwoParameterArithmetic(epService, listener, stmt, fields);
        stmt.destroy();

        String eplAlias = "" +
                "expression fZero alias for {10} " +
                "expression fOne alias for {intPrimitive} " +
                "expression fTwo alias for {intPrimitive+intPrimitive} " +
                "expression fThree alias for {intPrimitive+100} " +
                "select fZero, fOne, fTwo, fThree from SupportBean";
        EPStatement stmtAlias = epService.getEPAdministrator().createEPL(eplAlias);
        stmtAlias.addListener(listener);
        tryAssertionTwoParameterArithmetic(epService, listener, stmtAlias, new String[]{"fZero", "fOne", "fTwo", "fThree"});
        stmtAlias.destroy();
    }

    private void tryAssertionTwoParameterArithmetic(EPServiceProvider epService, SupportUpdateListener listener, EPStatement stmt, String[] fields) {
        String[] props = stmt.getEventType().getPropertyNames();
        EPAssertionUtil.assertEqualsAnyOrder(props, fields);
        assertEquals(Integer.class, stmt.getEventType().getPropertyType(fields[0]));
        assertEquals(Integer.class, stmt.getEventType().getPropertyType(fields[1]));
        assertEquals(Integer.class, stmt.getEventType().getPropertyType(fields[2]));
        assertEquals(Integer.class, stmt.getEventType().getPropertyType(fields[3]));
        EventPropertyGetter getter = stmt.getEventType().getGetter(fields[3]);

        epService.getEPRuntime().sendEvent(new SupportBean("E1", 11));
        EPAssertionUtil.assertProps(listener.assertOneGetNew(), fields, new Object[]{10, 11, 22, 111});
        assertEquals(111, getter.get(listener.assertOneGetNewAndReset()));
    }

    private void runAssertionOneParameterLambdaReturn(EPServiceProvider epService) {

        String eplDeclare = "" +
                "expression one {x1 => x1.contained.where(y => y.p00 < 10) } " +
                "expression two {x2 => one(x2).where(y => y.p00 > 1)  } " +
                "select one(s0c) as val1, two(s0c) as val2 from SupportBean_ST0_Container as s0c";
        tryAssertionOneParameterLambdaReturn(epService, eplDeclare);

        String eplAliasWParen = "" +
                "expression one alias for {contained.where(y => y.p00 < 10)}" +
                "expression two alias for {one().where(y => y.p00 > 1)}" +
                "select one as val1, two as val2 from SupportBean_ST0_Container as s0c";
        tryAssertionOneParameterLambdaReturn(epService, eplAliasWParen);

        String eplAliasNoParen = "" +
                "expression one alias for {contained.where(y => y.p00 < 10)}" +
                "expression two alias for {one.where(y => y.p00 > 1)}" +
                "select one as val1, two as val2 from SupportBean_ST0_Container as s0c";
        tryAssertionOneParameterLambdaReturn(epService, eplAliasNoParen);
    }

    private void tryAssertionOneParameterLambdaReturn(EPServiceProvider epService, String epl) {

        EPStatement stmt = epService.getEPAdministrator().createEPL(epl);
        SupportUpdateListener listener = new SupportUpdateListener();
        stmt.addListener(listener);
        LambdaAssertionUtil.assertTypes(stmt.getEventType(), "val1,val2".split(","), new Class[]{Collection.class, Collection.class});

        SupportBean_ST0_Container theEvent = SupportBean_ST0_Container.make3Value("E1,K1,1", "E2,K2,2", "E20,K20,20");
        epService.getEPRuntime().sendEvent(theEvent);
        Object[] resultVal1 = ((Collection) listener.getLastNewData()[0].get("val1")).toArray();
        EPAssertionUtil.assertEqualsExactOrder(new Object[]{theEvent.getContained().get(0), theEvent.getContained().get(1)}, resultVal1
        );
        Object[] resultVal2 = ((Collection) listener.getLastNewData()[0].get("val2")).toArray();
        EPAssertionUtil.assertEqualsExactOrder(new Object[]{theEvent.getContained().get(1)}, resultVal2
        );

        epService.getEPAdministrator().destroyAllStatements();
    }

    private void runAssertionNoParameterArithmetic(EPServiceProvider epService) {

        String eplDeclared = "expression getEnumerationSource {1} " +
                "select getEnumerationSource() as val1, getEnumerationSource()*5 as val2 from SupportBean";
        tryAssertionNoParameterArithmetic(epService, eplDeclared);

        String eplDeclaredNoParen = "expression getEnumerationSource {1} " +
                "select getEnumerationSource as val1, getEnumerationSource*5 as val2 from SupportBean";
        tryAssertionNoParameterArithmetic(epService, eplDeclaredNoParen);

        String eplAlias = "expression getEnumerationSource alias for {1} " +
                "select getEnumerationSource as val1, getEnumerationSource*5 as val2 from SupportBean";
        tryAssertionNoParameterArithmetic(epService, eplAlias);
    }

    private void tryAssertionNoParameterArithmetic(EPServiceProvider epService, String epl) {

        String[] fields = "val1,val2".split(",");
        EPStatement stmt = epService.getEPAdministrator().createEPL(epl);
        SupportUpdateListener listener = new SupportUpdateListener();
        stmt.addListener(listener);
        LambdaAssertionUtil.assertTypes(stmt.getEventType(), fields, new Class[]{Integer.class, Integer.class});

        epService.getEPRuntime().sendEvent(new SupportBean());
        EPAssertionUtil.assertProps(listener.assertOneGetNewAndReset(), fields, new Object[]{1, 5});

        epService.getEPAdministrator().destroyAllStatements();
    }

    private void runAssertionNoParameterVariable(EPServiceProvider epService) {
        String eplDeclared = "expression one {myvar} " +
                "expression two {myvar * 10} " +
                "select one() as val1, two() as val2, one() * two() as val3 from SupportBean";
        tryAssertionNoParameterVariable(epService, eplDeclared);

        String eplAlias = "expression one alias for {myvar} " +
                "expression two alias for {myvar * 10} " +
                "select one() as val1, two() as val2, one * two as val3 from SupportBean";
        tryAssertionNoParameterVariable(epService, eplAlias);
    }

    private void tryAssertionNoParameterVariable(EPServiceProvider epService, String epl) {

        epService.getEPAdministrator().createEPL("create variable int myvar = 2");

        String[] fields = "val1,val2,val3".split(",");
        EPStatement stmt = epService.getEPAdministrator().createEPL(epl);
        SupportUpdateListener listener = new SupportUpdateListener();
        stmt.addListener(listener);
        LambdaAssertionUtil.assertTypes(stmt.getEventType(), fields, new Class[]{Integer.class, Integer.class, Integer.class});

        epService.getEPRuntime().sendEvent(new SupportBean());
        EPAssertionUtil.assertProps(listener.assertOneGetNewAndReset(), fields, new Object[]{2, 20, 40});

        epService.getEPRuntime().setVariableValue("myvar", 3);
        epService.getEPRuntime().sendEvent(new SupportBean());
        EPAssertionUtil.assertProps(listener.assertOneGetNewAndReset(), fields, new Object[]{3, 30, 90});

        epService.getEPAdministrator().destroyAllStatements();
    }

    private void runAssertionWhereClauseExpression(EPServiceProvider epService) {
        String eplNoAlias = "expression one {x=>x.boolPrimitive} select * from SupportBean as sb where one(sb)";
        tryAssertionWhereClauseExpression(epService, eplNoAlias);

        String eplAlias = "expression one alias for {boolPrimitive} select * from SupportBean as sb where one";
        tryAssertionWhereClauseExpression(epService, eplAlias);
    }

    private void tryAssertionWhereClauseExpression(EPServiceProvider epService, String epl) {
        EPStatement stmt = epService.getEPAdministrator().createEPL(epl);
        SupportUpdateListener listener = new SupportUpdateListener();
        stmt.addListener(listener);

        epService.getEPRuntime().sendEvent(new SupportBean());
        assertFalse(listener.getAndClearIsInvoked());

        SupportBean theEvent = new SupportBean();
        theEvent.setBoolPrimitive(true);
        epService.getEPRuntime().sendEvent(theEvent);
        assertTrue(listener.getAndClearIsInvoked());

        epService.getEPAdministrator().destroyAllStatements();
    }

    private void runAssertionInvalid(EPServiceProvider epService) {

        String epl = "expression abc {(select * from SupportBean_ST0#lastevent as st0 where p00=intPrimitive)} select abc() from SupportBean";
        tryInvalid(epService, epl, "Error starting statement: Failed to plan subquery number 1 querying SupportBean_ST0: Failed to validate filter expression 'p00=intPrimitive': Property named 'intPrimitive' is not valid in any stream [expression abc {(select * from SupportBean_ST0#lastevent as st0 where p00=intPrimitive)} select abc() from SupportBean]");

        epl = "expression abc {x=>strvals.where(x=> x != 'E1')} select abc(str) from SupportCollection str";
        tryInvalid(epService, epl, "Error starting statement: Failed to validate select-clause expression 'abc(str)': Error validating expression declaration 'abc': Failed to validate declared expression body expression 'strvals.where()': Error validating enumeration method 'where', the lambda-parameter name 'x' has already been declared in this context [expression abc {x=>strvals.where(x=> x != 'E1')} select abc(str) from SupportCollection str]");

        epl = "expression abc {avg(intPrimitive)} select abc() from SupportBean";
        tryInvalid(epService, epl, "Error starting statement: Failed to validate select-clause expression 'abc()': Error validating expression declaration 'abc': Failed to validate declared expression body expression 'avg(intPrimitive)': Property named 'intPrimitive' is not valid in any stream [expression abc {avg(intPrimitive)} select abc() from SupportBean]");

        epl = "expression abc {(select * from SupportBean_ST0#lastevent as st0 where p00=sb.intPrimitive)} select abc() from SupportBean sb";
        tryInvalid(epService, epl, "Error starting statement: Failed to plan subquery number 1 querying SupportBean_ST0: Failed to validate filter expression 'p00=sb.intPrimitive': Failed to find a stream named 'sb' (did you mean 'st0'?) [expression abc {(select * from SupportBean_ST0#lastevent as st0 where p00=sb.intPrimitive)} select abc() from SupportBean sb]");

        epl = "expression abc {window(*)} select abc() from SupportBean";
        tryInvalid(epService, epl, "Error starting statement: Failed to validate select-clause expression 'abc()': Error validating expression declaration 'abc': Failed to validate declared expression body expression 'window(*)': The 'window' aggregation function requires that at least one stream is provided [expression abc {window(*)} select abc() from SupportBean]");

        epl = "expression abc {x => intPrimitive} select abc() from SupportBean";
        tryInvalid(epService, epl, "Error starting statement: Failed to validate select-clause expression 'abc()': Parameter count mismatches for declared expression 'abc', expected 1 parameters but received 0 parameters [expression abc {x => intPrimitive} select abc() from SupportBean]");

        epl = "expression abc {intPrimitive} select abc(sb) from SupportBean sb";
        tryInvalid(epService, epl, "Error starting statement: Failed to validate select-clause expression 'abc(sb)': Parameter count mismatches for declared expression 'abc', expected 0 parameters but received 1 parameters [expression abc {intPrimitive} select abc(sb) from SupportBean sb]");

        epl = "expression abc {x=>} select abc(sb) from SupportBean sb";
        tryInvalid(epService, epl, "Incorrect syntax near '}' at line 1 column 19 near reserved keyword 'select' [expression abc {x=>} select abc(sb) from SupportBean sb]");

        epl = "expression abc {intPrimitive} select abc() from SupportBean sb";
        tryInvalid(epService, epl, "Error starting statement: Failed to validate select-clause expression 'abc()': Error validating expression declaration 'abc': Failed to validate declared expression body expression 'intPrimitive': Property named 'intPrimitive' is not valid in any stream [expression abc {intPrimitive} select abc() from SupportBean sb]");

        epl = "expression abc {x=>x} select abc(1) from SupportBean sb";
        tryInvalid(epService, epl, "Error starting statement: Failed to validate select-clause expression 'abc(1)': Expression 'abc' requires a stream name as a parameter [expression abc {x=>x} select abc(1) from SupportBean sb]");

        epl = "expression abc {x=>intPrimitive} select * from SupportBean sb where abc(sb)";
        tryInvalid(epService, epl, "Filter expression not returning a boolean value: 'abc(sb)' [expression abc {x=>intPrimitive} select * from SupportBean sb where abc(sb)]");

        epl = "expression abc {x=>x.intPrimitive = 0} select * from SupportBean#lastevent sb1, SupportBean#lastevent sb2 where abc(*)";
        tryInvalid(epService, epl, "Error validating expression: Failed to validate filter expression 'abc(*)': Expression 'abc' only allows a wildcard parameter if there is a single stream available, please use a stream or tag name instead [expression abc {x=>x.intPrimitive = 0} select * from SupportBean#lastevent sb1, SupportBean#lastevent sb2 where abc(*)]");
    }

    private SupportBean getSupportBean(int intPrimitive, Integer intBoxed) {
        SupportBean b = new SupportBean(null, intPrimitive);
        b.setIntBoxed(intBoxed);
        return b;
    }

    private SupportBean[] toArray(Collection it) {
        List<SupportBean> result = new ArrayList<>();
        for (Object item : it) {
            result.add((SupportBean) item);
        }
        return result.toArray(new SupportBean[result.size()]);
    }

    private Map[] toArrayMap(Collection it) {
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

    public static class MyEvent {
        private final Object myObject;

        public MyEvent(Object myObject) {
            this.myObject = myObject;
        }

        public Object getMyObject() {
            return myObject;
        }
    }

}
