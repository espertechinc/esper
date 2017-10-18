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
package com.espertech.esper.regression.resultset.aggregate;

import com.espertech.esper.client.*;
import com.espertech.esper.client.scopetest.EPAssertionUtil;
import com.espertech.esper.client.scopetest.SupportUpdateListener;
import com.espertech.esper.client.soda.EPStatementObjectModel;
import com.espertech.esper.supportregression.bean.*;
import com.espertech.esper.supportregression.epl.SupportStaticMethodLib;
import com.espertech.esper.supportregression.execution.RegressionExecution;

import static com.espertech.esper.supportregression.util.SupportMessageAssertUtil.tryInvalid;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;

public class ExecAggregateFirstLastWindow implements RegressionExecution {

    public void configure(Configuration configuration) throws Exception {
        configuration.addEventType("SupportBean", SupportBean.class);
        configuration.addEventType("SupportBean_A", SupportBean_A.class);
        configuration.addEventType("SupportBean_B", SupportBean_B.class);
    }

    public void run(EPServiceProvider epService) throws Exception {
        runAssertionNoParamChainedAndProperty(epService);
        runAssertionLastMaxMixedOnSelect(epService);
        runAssertionPrevNthIndexedFirstLast(epService);
        runAssertionFirstLastIndexed(epService);
        runAssertionInvalid(epService);
        runAssertionSubquery(epService);
        runAssertionMethodAndAccessTogether(epService);
        runAssertionOutputRateLimiting(epService);
        runAssertionTypeAndColNameAndEquivalency(epService);
        runAssertionJoin2Access(epService);
        runAssertionOuterJoin1Access(epService);
        runAssertionBatchWindow(epService);
        runAssertionBatchWindowGrouped(epService);
        runAssertionLateInitialize(epService);
        runAssertionOnDelete(epService);
        runAssertionOnDemandQuery(epService);
        runAssertionStar(epService);
        runAssertionUnboundedStream(epService);
        runAssertionWindowedUnGrouped(epService);
        runAssertionWindowedGrouped(epService);
    }

    private void runAssertionNoParamChainedAndProperty(EPServiceProvider epService) {
        epService.getEPAdministrator().getConfiguration().addEventType("ChainEvent", ChainEvent.class);
        EPStatement stmt = epService.getEPAdministrator().createEPL("select first().property as val0, first().myMethod() as val1, window() as val2 from ChainEvent#lastevent");
        SupportUpdateListener listener = new SupportUpdateListener();
        stmt.addListener(listener);

        epService.getEPRuntime().sendEvent(new ChainEvent("p1"));
        EPAssertionUtil.assertProps(listener.assertOneGetNewAndReset(), "val0,val1".split(","), new Object[]{"p1", "abc"});

        stmt.destroy();
    }

    private void runAssertionLastMaxMixedOnSelect(EPServiceProvider epService) {
        epService.getEPAdministrator().createEPL("create window MyWindowOne#keepall as SupportBean");
        epService.getEPAdministrator().createEPL("insert into MyWindowOne select * from SupportBean(theString like 'A%')");

        String epl = "on SupportBean(theString like 'B%') select last(mw.intPrimitive) as li, max(mw.intPrimitive) as mi from MyWindowOne mw";
        EPStatement stmt = epService.getEPAdministrator().createEPL(epl);
        SupportUpdateListener listener = new SupportUpdateListener();
        stmt.addListener(listener);
        String[] fields = "li,mi".split(",");

        epService.getEPRuntime().sendEvent(new SupportBean("A1", 10));
        epService.getEPRuntime().sendEvent(new SupportBean("B1", -1));
        EPAssertionUtil.assertProps(listener.assertOneGetNewAndReset(), fields, new Object[]{10, 10});

        for (int i = 11; i < 20; i++) {
            epService.getEPRuntime().sendEvent(new SupportBean("A1", i));
            epService.getEPRuntime().sendEvent(new SupportBean("Bx", -1));
            EPAssertionUtil.assertProps(listener.assertOneGetNewAndReset(), fields, new Object[]{i, i});
        }

        epService.getEPRuntime().sendEvent(new SupportBean("A1", 1));
        epService.getEPRuntime().sendEvent(new SupportBean("B1", -1));
        EPAssertionUtil.assertProps(listener.assertOneGetNewAndReset(), fields, new Object[]{1, 19});

        epService.getEPRuntime().sendEvent(new SupportBean("A1", 2));
        epService.getEPRuntime().sendEvent(new SupportBean("B1", -1));
        EPAssertionUtil.assertProps(listener.assertOneGetNewAndReset(), fields, new Object[]{2, 19});

        epService.getEPAdministrator().destroyAllStatements();
    }

    private void runAssertionPrevNthIndexedFirstLast(EPServiceProvider epService) {
        String epl = "select " +
                "prev(intPrimitive, 0) as p0, " +
                "prev(intPrimitive, 1) as p1, " +
                "prev(intPrimitive, 2) as p2, " +
                "nth(intPrimitive, 0) as n0, " +
                "nth(intPrimitive, 1) as n1, " +
                "nth(intPrimitive, 2) as n2, " +
                "last(intPrimitive, 0) as l1, " +
                "last(intPrimitive, 1) as l2, " +
                "last(intPrimitive, 2) as l3 " +
                "from SupportBean#length(3)";
        EPStatement stmt = epService.getEPAdministrator().createEPL(epl);
        SupportUpdateListener listener = new SupportUpdateListener();
        stmt.addListener(listener);
        String[] fields = "p0,p1,p2,n0,n1,n2,l1,l2,l3".split(",");

        epService.getEPRuntime().sendEvent(new SupportBean("E1", 10));
        EPAssertionUtil.assertProps(listener.assertOneGetNewAndReset(), fields, new Object[]{10, null, null, 10, null, null, 10, null, null});

        epService.getEPRuntime().sendEvent(new SupportBean("E2", 11));
        EPAssertionUtil.assertProps(listener.assertOneGetNewAndReset(), fields, new Object[]{11, 10, null, 11, 10, null, 11, 10, null});

        epService.getEPRuntime().sendEvent(new SupportBean("E3", 12));
        EPAssertionUtil.assertProps(listener.assertOneGetNewAndReset(), fields, new Object[]{12, 11, 10, 12, 11, 10, 12, 11, 10});

        epService.getEPRuntime().sendEvent(new SupportBean("E4", 13));
        EPAssertionUtil.assertProps(listener.assertOneGetNewAndReset(), fields, new Object[]{13, 12, 11, 13, 12, 11, 13, 12, 11});

        stmt.destroy();
    }

    private void runAssertionFirstLastIndexed(EPServiceProvider epService) {
        String epl = "select " +
                "first(intPrimitive, 0) as f0, " +
                "first(intPrimitive, 1) as f1, " +
                "first(intPrimitive, 2) as f2, " +
                "first(intPrimitive, 3) as f3, " +
                "last(intPrimitive, 0) as l0, " +
                "last(intPrimitive, 1) as l1, " +
                "last(intPrimitive, 2) as l2, " +
                "last(intPrimitive, 3) as l3 " +
                "from SupportBean#length(3)";

        EPStatement stmt = epService.getEPAdministrator().createEPL(epl);
        SupportUpdateListener listener = new SupportUpdateListener();
        stmt.addListener(listener);

        tryAssertionFirstLastIndexed(epService, listener);

        // test join
        stmt.destroy();
        epl += ", SupportBean_A#lastevent";
        stmt = epService.getEPAdministrator().createEPL(epl);
        stmt.addListener(listener);
        epService.getEPRuntime().sendEvent(new SupportBean_A("A1"));

        tryAssertionFirstLastIndexed(epService, listener);

        // test variable
        stmt.destroy();
        epService.getEPAdministrator().createEPL("create variable int indexvar = 2");
        epl = "select " +
                "first(intPrimitive, indexvar) as f0 " +
                "from SupportBean#keepall";

        stmt = epService.getEPAdministrator().createEPL(epl);
        stmt.addListener(listener);

        String[] fields = "f0".split(",");
        epService.getEPRuntime().sendEvent(new SupportBean("E1", 10));
        epService.getEPRuntime().sendEvent(new SupportBean("E1", 11));
        listener.reset();

        epService.getEPRuntime().sendEvent(new SupportBean("E1", 12));
        EPAssertionUtil.assertProps(listener.assertOneGetNewAndReset(), fields, new Object[]{12});

        epService.getEPRuntime().setVariableValue("indexvar", 0);
        epService.getEPRuntime().sendEvent(new SupportBean("E1", 13));
        EPAssertionUtil.assertProps(listener.assertOneGetNewAndReset(), fields, new Object[]{10});
        stmt.destroy();

        // test as part of function
        epService.getEPAdministrator().createEPL("select Math.abs(last(intPrimitive)) from SupportBean").destroy();
    }

    private void tryAssertionFirstLastIndexed(EPServiceProvider epService, SupportUpdateListener listener) {
        String[] fields = "f0,f1,f2,f3,l0,l1,l2,l3".split(",");
        epService.getEPRuntime().sendEvent(new SupportBean("E1", 10));
        EPAssertionUtil.assertProps(listener.assertOneGetNewAndReset(), fields, new Object[]{10, null, null, null, 10, null, null, null});

        epService.getEPRuntime().sendEvent(new SupportBean("E2", 11));
        EPAssertionUtil.assertProps(listener.assertOneGetNewAndReset(), fields, new Object[]{10, 11, null, null, 11, 10, null, null});

        epService.getEPRuntime().sendEvent(new SupportBean("E3", 12));
        EPAssertionUtil.assertProps(listener.assertOneGetNewAndReset(), fields, new Object[]{10, 11, 12, null, 12, 11, 10, null});

        epService.getEPRuntime().sendEvent(new SupportBean("E4", 13));
        EPAssertionUtil.assertProps(listener.assertOneGetNewAndReset(), fields, new Object[]{11, 12, 13, null, 13, 12, 11, null});
    }

    private void runAssertionInvalid(EPServiceProvider epService) {
        tryInvalid(epService, "select window(distinct intPrimitive) from SupportBean",
                "Incorrect syntax near '(' ('distinct' is a reserved keyword) at line 1 column 13 near reserved keyword 'distinct' [");

        tryInvalid(epService, "select window(sa.intPrimitive + sb.intPrimitive) from SupportBean#lastevent sa, SupportBean#lastevent sb",
                "Error starting statement: Failed to validate select-clause expression 'window(sa.intPrimitive+sb.intPrimitive)': The 'window' aggregation function requires that any child expressions evaluate properties of the same stream; Use 'firstever' or 'lastever' or 'nth' instead [select window(sa.intPrimitive + sb.intPrimitive) from SupportBean#lastevent sa, SupportBean#lastevent sb]");

        tryInvalid(epService, "select last(*) from SupportBean#lastevent sa, SupportBean#lastevent sb",
                "Error starting statement: Failed to validate select-clause expression 'last(*)': The 'last' aggregation function requires that in joins or subqueries the stream-wildcard (stream-alias.*) syntax is used instead [select last(*) from SupportBean#lastevent sa, SupportBean#lastevent sb]");

        tryInvalid(epService, "select theString, (select first(*) from SupportBean#lastevent sa) from SupportBean#lastevent sb",
                "Error starting statement: Failed to plan subquery number 1 querying SupportBean: Failed to validate select-clause expression 'first(*)': The 'first' aggregation function requires that in joins or subqueries the stream-wildcard (stream-alias.*) syntax is used instead [select theString, (select first(*) from SupportBean#lastevent sa) from SupportBean#lastevent sb]");

        tryInvalid(epService, "select window(x.*) from SupportBean#lastevent",
                "Error starting statement: Failed to validate select-clause expression 'window(x.*)': Stream by name 'x' could not be found among all streams [select window(x.*) from SupportBean#lastevent]");

        tryInvalid(epService, "select window(*) from SupportBean x",
                "Error starting statement: Failed to validate select-clause expression 'window(*)': The 'window' aggregation function requires that the aggregated events provide a remove stream; Please define a data window onto the stream or use 'firstever', 'lastever' or 'nth' instead [select window(*) from SupportBean x]");
        tryInvalid(epService, "select window(x.*) from SupportBean x",
                "Error starting statement: Failed to validate select-clause expression 'window(x.*)': The 'window' aggregation function requires that the aggregated events provide a remove stream; Please define a data window onto the stream or use 'firstever', 'lastever' or 'nth' instead [select window(x.*) from SupportBean x]");
        tryInvalid(epService, "select window(x.intPrimitive) from SupportBean x",
                "Error starting statement: Failed to validate select-clause expression 'window(x.intPrimitive)': The 'window' aggregation function requires that the aggregated events provide a remove stream; Please define a data window onto the stream or use 'firstever', 'lastever' or 'nth' instead [select window(x.intPrimitive) from SupportBean x]");

        tryInvalid(epService, "select window(x.intPrimitive, 10) from SupportBean#keepall x",
                "Error starting statement: Failed to validate select-clause expression 'window(x.intPrimitive,10)': The 'window' aggregation function does not accept an index expression; Use 'first' or 'last' instead [");

        tryInvalid(epService, "select first(x.*, 10d) from SupportBean#lastevent as x",
                "Error starting statement: Failed to validate select-clause expression 'first(x.*,10.0)': The 'first' aggregation function requires an index expression that returns an integer value [select first(x.*, 10d) from SupportBean#lastevent as x]");
    }

    private void runAssertionSubquery(EPServiceProvider epService) {
        String epl = "select id, (select window(sb.*) from SupportBean#length(2) as sb) as w from SupportBean_A";
        EPStatement stmt = epService.getEPAdministrator().createEPL(epl);
        SupportUpdateListener listener = new SupportUpdateListener();
        stmt.addListener(listener);
        String[] fields = "id,w".split(",");

        epService.getEPRuntime().sendEvent(new SupportBean_A("A1"));
        EPAssertionUtil.assertProps(listener.assertOneGetNewAndReset(), fields, new Object[]{"A1", null});

        SupportBean beanOne = sendEvent(epService, "E1", 0, 1);
        epService.getEPRuntime().sendEvent(new SupportBean_A("A2"));
        EPAssertionUtil.assertProps(listener.assertOneGetNewAndReset(), fields, new Object[]{"A2", new Object[]{beanOne}});

        SupportBean beanTwo = sendEvent(epService, "E2", 0, 1);
        epService.getEPRuntime().sendEvent(new SupportBean_A("A3"));
        EPAssertionUtil.assertProps(listener.assertOneGetNewAndReset(), fields, new Object[]{"A3", new Object[]{beanOne, beanTwo}});

        SupportBean beanThree = sendEvent(epService, "E2", 0, 1);
        epService.getEPRuntime().sendEvent(new SupportBean_A("A4"));
        EPAssertionUtil.assertProps(listener.assertOneGetNewAndReset(), fields, new Object[]{"A4", new Object[]{beanTwo, beanThree}});

        stmt.destroy();
    }

    private void runAssertionMethodAndAccessTogether(EPServiceProvider epService) {
        String epl = "select sum(intPrimitive) as si, window(sa.intPrimitive) as wi from SupportBean#length(2) as sa";
        EPStatement stmt = epService.getEPAdministrator().createEPL(epl);
        SupportUpdateListener listener = new SupportUpdateListener();
        stmt.addListener(listener);
        String[] fields = "si,wi".split(",");

        epService.getEPRuntime().sendEvent(new SupportBean("E1", 1));
        EPAssertionUtil.assertProps(listener.assertOneGetNewAndReset(), fields, new Object[]{1, intArray(1)});

        epService.getEPRuntime().sendEvent(new SupportBean("E2", 2));
        EPAssertionUtil.assertProps(listener.assertOneGetNewAndReset(), fields, new Object[]{3, intArray(1, 2)});

        epService.getEPRuntime().sendEvent(new SupportBean("E3", 3));
        EPAssertionUtil.assertProps(listener.assertOneGetNewAndReset(), fields, new Object[]{5, intArray(2, 3)});

        stmt.destroy();
        epl = "select sum(intPrimitive) as si, window(sa.intPrimitive) as wi from SupportBean#keepall as sa group by theString";
        stmt = epService.getEPAdministrator().createEPL(epl);
        stmt.addListener(listener);

        epService.getEPRuntime().sendEvent(new SupportBean("E1", 1));
        EPAssertionUtil.assertProps(listener.assertOneGetNewAndReset(), fields, new Object[]{1, intArray(1)});

        epService.getEPRuntime().sendEvent(new SupportBean("E2", 2));
        EPAssertionUtil.assertProps(listener.assertOneGetNewAndReset(), fields, new Object[]{2, intArray(2)});

        epService.getEPRuntime().sendEvent(new SupportBean("E2", 3));
        EPAssertionUtil.assertProps(listener.assertOneGetNewAndReset(), fields, new Object[]{5, intArray(2, 3)});

        epService.getEPRuntime().sendEvent(new SupportBean("E1", 4));
        EPAssertionUtil.assertProps(listener.assertOneGetNewAndReset(), fields, new Object[]{5, intArray(1, 4)});

        stmt.destroy();
    }

    private void runAssertionOutputRateLimiting(EPServiceProvider epService) {
        String epl = "select sum(intPrimitive) as si, window(sa.intPrimitive) as wi from SupportBean#keepall as sa output every 2 events";
        EPStatement stmt = epService.getEPAdministrator().createEPL(epl);
        SupportUpdateListener listener = new SupportUpdateListener();
        stmt.addListener(listener);
        String[] fields = "si,wi".split(",");

        epService.getEPRuntime().sendEvent(new SupportBean("E1", 1));
        epService.getEPRuntime().sendEvent(new SupportBean("E2", 2));
        EPAssertionUtil.assertPropsPerRow(listener.getAndResetLastNewData(), fields, new Object[][]{
                {1, intArray(1)},
                {3, intArray(1, 2)},
        });

        epService.getEPRuntime().sendEvent(new SupportBean("E3", 3));
        epService.getEPRuntime().sendEvent(new SupportBean("E4", 4));
        EPAssertionUtil.assertPropsPerRow(listener.getAndResetLastNewData(), fields, new Object[][]{
                {6, intArray(1, 2, 3)},
                {10, intArray(1, 2, 3, 4)},
        });

        stmt.destroy();
    }

    private void runAssertionTypeAndColNameAndEquivalency(EPServiceProvider epService) {
        epService.getEPAdministrator().getConfiguration().addImport(SupportStaticMethodLib.class.getName());

        String epl = "select " +
                "first(sa.doublePrimitive + sa.intPrimitive), " +
                "first(sa.intPrimitive), " +
                "window(sa.*), " +
                "last(*) from SupportBean#length(2) as sa";
        EPStatement stmt = epService.getEPAdministrator().createEPL(epl);
        SupportUpdateListener listener = new SupportUpdateListener();
        stmt.addListener(listener);

        Object[][] rows = new Object[][]{
                {"first(sa.doublePrimitive+sa.intPrimitive)", Double.class},
                {"first(sa.intPrimitive)", Integer.class},
                {"window(sa.*)", SupportBean[].class},
                {"last(*)", SupportBean.class},
        };
        for (int i = 0; i < rows.length; i++) {
            EventPropertyDescriptor prop = stmt.getEventType().getPropertyDescriptors()[i];
            assertEquals(rows[i][0], prop.getPropertyName());
            assertEquals(rows[i][1], prop.getPropertyType());
        }

        stmt.destroy();
        epl = "select " +
                "first(sa.doublePrimitive + sa.intPrimitive) as f1, " +
                "first(sa.intPrimitive) as f2, " +
                "window(sa.*) as w1, " +
                "last(*) as l1 " +
                "from SupportBean#length(2) as sa";
        stmt = epService.getEPAdministrator().createEPL(epl);
        stmt.addListener(listener);

        tryAssertionType(epService, listener, false);

        stmt.destroy();

        epl = "select " +
                "first(sa.doublePrimitive + sa.intPrimitive) as f1, " +
                "first(sa.intPrimitive) as f2, " +
                "window(sa.*) as w1, " +
                "last(*) as l1 " +
                "from SupportBean#length(2) as sa " +
                "having SupportStaticMethodLib.alwaysTrue({first(sa.doublePrimitive + sa.intPrimitive), " +
                "first(sa.intPrimitive), window(sa.*), last(*)})";
        stmt = epService.getEPAdministrator().createEPL(epl);
        stmt.addListener(listener);

        tryAssertionType(epService, listener, true);

        stmt.destroy();
    }

    private void tryAssertionType(EPServiceProvider epService, SupportUpdateListener listener, boolean isCheckStatic) {
        String[] fields = "f1,f2,w1,l1".split(",");
        SupportStaticMethodLib.getInvocations().clear();

        SupportBean beanOne = sendEvent(epService, "E1", 10d, 100);
        Object[] expected = new Object[]{110d, 100, new Object[]{beanOne}, beanOne};
        EPAssertionUtil.assertProps(listener.assertOneGetNewAndReset(), fields, expected);
        if (isCheckStatic) {
            Object[] parameters = SupportStaticMethodLib.getInvocations().get(0);
            SupportStaticMethodLib.getInvocations().clear();
            EPAssertionUtil.assertEqualsExactOrder(expected, parameters);
        }
    }

    private void runAssertionJoin2Access(EPServiceProvider epService) {
        String epl = "select " +
                "sa.id as ast, " +
                "sb.id as bst, " +
                "first(sa.id) as fas, " +
                "window(sa.id) as was, " +
                "last(sa.id) as las, " +
                "first(sb.id) as fbs, " +
                "window(sb.id) as wbs, " +
                "last(sb.id) as lbs " +
                "from SupportBean_A#length(2) as sa, SupportBean_B#length(2) as sb " +
                "order by ast, bst";
        EPStatement stmt = epService.getEPAdministrator().createEPL(epl);
        SupportUpdateListener listener = new SupportUpdateListener();
        stmt.addListener(listener);

        String[] fields = "ast,bst,fas,was,las,fbs,wbs,lbs".split(",");

        epService.getEPRuntime().sendEvent(new SupportBean_A("A1"));
        epService.getEPRuntime().sendEvent(new SupportBean_B("B1"));
        EPAssertionUtil.assertProps(listener.assertOneGetNewAndReset(), fields, new Object[]{"A1", "B1", "A1", split("A1"), "A1", "B1", split("B1"), "B1"});

        epService.getEPRuntime().sendEvent(new SupportBean_A("A2"));
        EPAssertionUtil.assertPropsPerRow(listener.getLastNewData(), fields,
                new Object[][]{
                        {"A2", "B1", "A1", split("A1,A2"), "A2", "B1", split("B1"), "B1"}
                });

        epService.getEPRuntime().sendEvent(new SupportBean_A("A3"));
        EPAssertionUtil.assertPropsPerRow(listener.getLastNewData(), fields,
                new Object[][]{
                        {"A3", "B1", "A2", split("A2,A3"), "A3", "B1", split("B1"), "B1"}
                });

        epService.getEPRuntime().sendEvent(new SupportBean_B("B2"));
        EPAssertionUtil.assertPropsPerRow(listener.getLastNewData(), fields,
                new Object[][]{
                        {"A2", "B2", "A2", split("A2,A3"), "A3", "B1", split("B1,B2"), "B2"},
                        {"A3", "B2", "A2", split("A2,A3"), "A3", "B1", split("B1,B2"), "B2"}
                });

        epService.getEPRuntime().sendEvent(new SupportBean_B("B3"));
        EPAssertionUtil.assertPropsPerRow(listener.getLastNewData(), fields,
                new Object[][]{
                        {"A2", "B3", "A2", split("A2,A3"), "A3", "B2", split("B2,B3"), "B3"},
                        {"A3", "B3", "A2", split("A2,A3"), "A3", "B2", split("B2,B3"), "B3"}
                });

        epService.getEPRuntime().sendEvent(new SupportBean_A("A4"));
        EPAssertionUtil.assertPropsPerRow(listener.getLastNewData(), fields,
                new Object[][]{
                        {"A4", "B2", "A3", split("A3,A4"), "A4", "B2", split("B2,B3"), "B3"},
                        {"A4", "B3", "A3", split("A3,A4"), "A4", "B2", split("B2,B3"), "B3"}
                });

        stmt.destroy();
    }

    private void runAssertionOuterJoin1Access(EPServiceProvider epService) {
        epService.getEPAdministrator().getConfiguration().addEventType("S0", SupportBean_S0.class);
        epService.getEPAdministrator().getConfiguration().addEventType("S1", SupportBean_S1.class);
        String epl = "select " +
                "sa.id as aid, " +
                "sb.id as bid, " +
                "first(sb.p10) as fb, " +
                "window(sb.p10) as wb, " +
                "last(sb.p10) as lb " +
                "from S0#keepall as sa " +
                "left outer join " +
                "S1#keepall as sb " +
                "on sa.id = sb.id";
        EPStatement stmt = epService.getEPAdministrator().createEPL(epl);
        SupportUpdateListener listener = new SupportUpdateListener();
        stmt.addListener(listener);

        String[] fields = "aid,bid,fb,wb,lb".split(",");

        epService.getEPRuntime().sendEvent(new SupportBean_S0(1));
        EPAssertionUtil.assertProps(listener.assertOneGetNewAndReset(), fields,
                new Object[]{1, null, null, null, null});

        epService.getEPRuntime().sendEvent(new SupportBean_S1(1, "A"));
        EPAssertionUtil.assertProps(listener.assertOneGetNewAndReset(), fields,
                new Object[]{1, 1, "A", split("A"), "A"});

        epService.getEPRuntime().sendEvent(new SupportBean_S1(2, "B"));
        assertFalse(listener.isInvoked());

        epService.getEPRuntime().sendEvent(new SupportBean_S0(2, "A"));
        EPAssertionUtil.assertProps(listener.assertOneGetNewAndReset(), fields,
                new Object[]{2, 2, "A", split("A,B"), "B"});

        epService.getEPRuntime().sendEvent(new SupportBean_S1(3, "C"));
        assertFalse(listener.isInvoked());

        epService.getEPRuntime().sendEvent(new SupportBean_S0(3, "C"));
        EPAssertionUtil.assertProps(listener.assertOneGetNewAndReset(), fields,
                new Object[]{3, 3, "A", split("A,B,C"), "C"});

        stmt.destroy();
    }

    private void runAssertionBatchWindow(EPServiceProvider epService) {
        String epl = "select irstream " +
                "first(theString) as fs, " +
                "window(theString) as ws, " +
                "last(theString) as ls " +
                "from SupportBean#length_batch(2) as sb";
        EPStatement stmt = epService.getEPAdministrator().createEPL(epl);
        SupportUpdateListener listener = new SupportUpdateListener();
        stmt.addListener(listener);

        String[] fields = "fs,ws,ls".split(",");

        epService.getEPRuntime().sendEvent(new SupportBean("E1", 0));
        epService.getEPRuntime().sendEvent(new SupportBean("E2", 0));
        EPAssertionUtil.assertProps(listener.assertOneGetOld(), fields, new Object[]{null, null, null});
        EPAssertionUtil.assertProps(listener.assertOneGetNew(), fields, new Object[]{"E1", split("E1,E2"), "E2"});
        listener.reset();

        epService.getEPRuntime().sendEvent(new SupportBean("E3", 0));
        epService.getEPRuntime().sendEvent(new SupportBean("E4", 0));
        EPAssertionUtil.assertProps(listener.assertOneGetOld(), fields, new Object[]{"E1", split("E1,E2"), "E2"});
        EPAssertionUtil.assertProps(listener.assertOneGetNew(), fields, new Object[]{"E3", split("E3,E4"), "E4"});
        listener.reset();

        epService.getEPRuntime().sendEvent(new SupportBean("E5", 0));
        epService.getEPRuntime().sendEvent(new SupportBean("E6", 0));
        EPAssertionUtil.assertProps(listener.assertOneGetOld(), fields, new Object[]{"E3", split("E3,E4"), "E4"});
        EPAssertionUtil.assertProps(listener.assertOneGetNew(), fields, new Object[]{"E5", split("E5,E6"), "E6"});
        listener.reset();

        stmt.destroy();
    }

    private void runAssertionBatchWindowGrouped(EPServiceProvider epService) {
        String epl = "select " +
                "theString, " +
                "first(intPrimitive) as fi, " +
                "window(intPrimitive) as wi, " +
                "last(intPrimitive) as li " +
                "from SupportBean#length_batch(6) as sb group by theString order by theString asc";
        EPStatement stmt = epService.getEPAdministrator().createEPL(epl);
        SupportUpdateListener listener = new SupportUpdateListener();
        stmt.addListener(listener);

        String[] fields = "theString,fi,wi,li".split(",");

        epService.getEPRuntime().sendEvent(new SupportBean("E1", 10));
        epService.getEPRuntime().sendEvent(new SupportBean("E2", 20));
        epService.getEPRuntime().sendEvent(new SupportBean("E1", 11));
        epService.getEPRuntime().sendEvent(new SupportBean("E3", 30));
        epService.getEPRuntime().sendEvent(new SupportBean("E3", 31));
        assertFalse(listener.isInvoked());
        epService.getEPRuntime().sendEvent(new SupportBean("E1", 12));
        EPAssertionUtil.assertPropsPerRow(listener.getAndResetLastNewData(), fields, new Object[][]{
                {"E1", 10, intArray(10, 11, 12), 12},
                {"E2", 20, intArray(20), 20},
                {"E3", 30, intArray(30, 31), 31}
        });

        epService.getEPRuntime().sendEvent(new SupportBean("E1", 13));
        epService.getEPRuntime().sendEvent(new SupportBean("E1", 14));
        epService.getEPRuntime().sendEvent(new SupportBean("E1", 15));
        epService.getEPRuntime().sendEvent(new SupportBean("E1", 16));
        epService.getEPRuntime().sendEvent(new SupportBean("E1", 17));
        epService.getEPRuntime().sendEvent(new SupportBean("E1", 18));
        EventBean[] result = listener.getAndResetLastNewData();
        EPAssertionUtil.assertPropsPerRow(result, fields, new Object[][]{
                {"E1", 13, intArray(13, 14, 15, 16, 17, 18), 18},
                {"E2", null, null, null},
                {"E3", null, null, null}
        });

        stmt.destroy();
    }

    private void runAssertionLateInitialize(EPServiceProvider epService) {
        epService.getEPAdministrator().createEPL("create window MyWindowTwo#keepall as select * from SupportBean");
        epService.getEPAdministrator().createEPL("insert into MyWindowTwo select * from SupportBean");

        epService.getEPRuntime().sendEvent(new SupportBean("E1", 10));
        epService.getEPRuntime().sendEvent(new SupportBean("E2", 20));

        String[] fields = "firststring,windowstring,laststring".split(",");
        String epl = "select " +
                "first(theString) as firststring, " +
                "window(theString) as windowstring, " +
                "last(theString) as laststring " +
                "from MyWindowTwo";
        EPStatement stmt = epService.getEPAdministrator().createEPL(epl);
        SupportUpdateListener listener = new SupportUpdateListener();
        stmt.addListener(listener);

        epService.getEPRuntime().sendEvent(new SupportBean("E3", 30));
        EPAssertionUtil.assertProps(listener.assertOneGetNewAndReset(), fields, new Object[]{"E1", split("E1,E2,E3"), "E3"});

        epService.getEPAdministrator().destroyAllStatements();
    }

    private void runAssertionOnDelete(EPServiceProvider epService) {
        epService.getEPAdministrator().createEPL("create window MyWindowThree#keepall as select * from SupportBean");
        epService.getEPAdministrator().createEPL("insert into MyWindowThree select * from SupportBean");
        epService.getEPAdministrator().createEPL("on SupportBean_A delete from MyWindowThree where theString = id");

        String[] fields = "firststring,windowstring,laststring".split(",");
        String epl = "select " +
                "first(theString) as firststring, " +
                "window(theString) as windowstring, " +
                "last(theString) as laststring " +
                "from MyWindowThree";
        EPStatement stmt = epService.getEPAdministrator().createEPL(epl);
        SupportUpdateListener listener = new SupportUpdateListener();
        stmt.addListener(listener);

        epService.getEPRuntime().sendEvent(new SupportBean("E1", 10));
        EPAssertionUtil.assertProps(listener.assertOneGetNewAndReset(), fields, new Object[]{"E1", split("E1"), "E1"});

        epService.getEPRuntime().sendEvent(new SupportBean("E2", 20));
        EPAssertionUtil.assertProps(listener.assertOneGetNewAndReset(), fields, new Object[]{"E1", split("E1,E2"), "E2"});

        epService.getEPRuntime().sendEvent(new SupportBean("E3", 30));
        EPAssertionUtil.assertProps(listener.assertOneGetNewAndReset(), fields, new Object[]{"E1", split("E1,E2,E3"), "E3"});

        epService.getEPRuntime().sendEvent(new SupportBean_A("E2"));
        EPAssertionUtil.assertProps(listener.assertOneGetNewAndReset(), fields, new Object[]{"E1", split("E1,E3"), "E3"});

        epService.getEPRuntime().sendEvent(new SupportBean_A("E3"));
        EPAssertionUtil.assertProps(listener.assertOneGetNewAndReset(), fields, new Object[]{"E1", split("E1"), "E1"});

        epService.getEPRuntime().sendEvent(new SupportBean_A("E1"));
        EPAssertionUtil.assertProps(listener.assertOneGetNewAndReset(), fields, new Object[]{null, null, null});

        epService.getEPRuntime().sendEvent(new SupportBean("E4", 40));
        EPAssertionUtil.assertProps(listener.assertOneGetNewAndReset(), fields, new Object[]{"E4", split("E4"), "E4"});

        epService.getEPRuntime().sendEvent(new SupportBean("E5", 50));
        EPAssertionUtil.assertProps(listener.assertOneGetNewAndReset(), fields, new Object[]{"E4", split("E4,E5"), "E5"});

        epService.getEPRuntime().sendEvent(new SupportBean_A("E4"));
        EPAssertionUtil.assertProps(listener.assertOneGetNewAndReset(), fields, new Object[]{"E5", split("E5"), "E5"});

        epService.getEPRuntime().sendEvent(new SupportBean("E6", 60));
        EPAssertionUtil.assertProps(listener.assertOneGetNewAndReset(), fields, new Object[]{"E5", split("E5,E6"), "E6"});

        epService.getEPAdministrator().destroyAllStatements();
    }

    private void runAssertionOnDemandQuery(EPServiceProvider epService) {
        epService.getEPAdministrator().createEPL("create window MyWindowFour#keepall as select * from SupportBean");
        epService.getEPAdministrator().createEPL("insert into MyWindowFour select * from SupportBean");

        epService.getEPRuntime().sendEvent(new SupportBean("E1", 10));
        epService.getEPRuntime().sendEvent(new SupportBean("E2", 20));
        epService.getEPRuntime().sendEvent(new SupportBean("E3", 30));
        epService.getEPRuntime().sendEvent(new SupportBean("E3", 31));
        epService.getEPRuntime().sendEvent(new SupportBean("E1", 11));
        epService.getEPRuntime().sendEvent(new SupportBean("E1", 12));

        EPOnDemandPreparedQuery q = epService.getEPRuntime().prepareQuery("select first(intPrimitive) as f, window(intPrimitive) as w, last(intPrimitive) as l from MyWindowFour as s");
        EPAssertionUtil.assertPropsPerRow(q.execute().getArray(), "f,w,l".split(","),
                new Object[][]{{10, intArray(10, 20, 30, 31, 11, 12), 12}});

        epService.getEPRuntime().sendEvent(new SupportBean("E1", 13));
        EPAssertionUtil.assertPropsPerRow(q.execute().getArray(), "f,w,l".split(","),
                new Object[][]{{10, intArray(10, 20, 30, 31, 11, 12, 13), 13}});

        q = epService.getEPRuntime().prepareQuery("select theString as s, first(intPrimitive) as f, window(intPrimitive) as w, last(intPrimitive) as l from MyWindowFour as s group by theString order by theString asc");
        Object[][] expected = new Object[][]{
                {"E1", 10, intArray(10, 11, 12, 13), 13},
                {"E2", 20, intArray(20), 20},
                {"E3", 30, intArray(30, 31), 31}
        };
        EPAssertionUtil.assertPropsPerRow(q.execute().getArray(), "s,f,w,l".split(","), expected);
        EPAssertionUtil.assertPropsPerRow(q.execute().getArray(), "s,f,w,l".split(","), expected);

        epService.getEPAdministrator().destroyAllStatements();
    }

    private void runAssertionStar(EPServiceProvider epService) {
        String epl = "select " +
                "first(*) as firststar, " +
                "first(sb.*) as firststarsb, " +
                "last(*) as laststar, " +
                "last(sb.*) as laststarsb, " +
                "window(*) as windowstar, " +
                "window(sb.*) as windowstarsb, " +
                "firstever(*) as firsteverstar, " +
                "lastever(*) as lasteverstar " +
                "from SupportBean#length(2) as sb";
        EPStatement stmt = epService.getEPAdministrator().createEPL(epl);
        SupportUpdateListener listener = new SupportUpdateListener();
        stmt.addListener(listener);

        EventPropertyDescriptor[] props = stmt.getEventType().getPropertyDescriptors();
        for (int i = 0; i < props.length; i++) {
            assertEquals(i == 4 || i == 5 ? SupportBean[].class : SupportBean.class, props[i].getPropertyType());
        }

        tryAssertionStar(epService, listener);
        stmt.destroy();

        EPStatementObjectModel model = epService.getEPAdministrator().compileEPL(epl);
        stmt = epService.getEPAdministrator().create(model);
        stmt.addListener(listener);
        assertEquals(epl, model.toEPL());

        tryAssertionStar(epService, listener);

        stmt.destroy();
    }

    private void tryAssertionStar(EPServiceProvider epService, SupportUpdateListener listener) {
        String[] fields = "firststar,firststarsb,laststar,laststarsb,windowstar,windowstarsb,firsteverstar,lasteverstar".split(",");

        Object beanE1 = new SupportBean("E1", 10);
        epService.getEPRuntime().sendEvent(beanE1);
        Object[] window = new Object[]{beanE1};
        EPAssertionUtil.assertProps(listener.assertOneGetNewAndReset(), fields, new Object[]{beanE1, beanE1, beanE1, beanE1, window, window, beanE1, beanE1});

        Object beanE2 = new SupportBean("E2", 20);
        epService.getEPRuntime().sendEvent(beanE2);
        window = new Object[]{beanE1, beanE2};
        EPAssertionUtil.assertProps(listener.assertOneGetNewAndReset(), fields, new Object[]{beanE1, beanE1, beanE2, beanE2, window, window, beanE1, beanE2});

        Object beanE3 = new SupportBean("E3", 30);
        epService.getEPRuntime().sendEvent(beanE3);
        window = new Object[]{beanE2, beanE3};
        EPAssertionUtil.assertProps(listener.assertOneGetNewAndReset(), fields, new Object[]{beanE2, beanE2, beanE3, beanE3, window, window, beanE1, beanE3});
    }

    private void runAssertionUnboundedStream(EPServiceProvider epService) {
        String epl = "select " +
                "first(theString) as f1, " +
                "first(sb.*) as f2, " +
                "first(*) as f3, " +
                "last(theString) as l1, " +
                "last(sb.*) as l2, " +
                "last(*) as l3 " +
                "from SupportBean as sb";
        EPStatement stmt = epService.getEPAdministrator().createEPL(epl);
        SupportUpdateListener listener = new SupportUpdateListener();
        stmt.addListener(listener);

        String[] fields = "f1,f2,f3,l1,l2,l3".split(",");

        SupportBean beanOne = sendEvent(epService, "E1", 1d, 1);
        EPAssertionUtil.assertProps(listener.assertOneGetNewAndReset(), fields, new Object[]{"E1", beanOne, beanOne, "E1", beanOne, beanOne});

        SupportBean beanTwo = sendEvent(epService, "E2", 2d, 2);
        EPAssertionUtil.assertProps(listener.assertOneGetNewAndReset(), fields, new Object[]{"E1", beanOne, beanOne, "E2", beanTwo, beanTwo});

        SupportBean beanThree = sendEvent(epService, "E3", 3d, 3);
        EPAssertionUtil.assertProps(listener.assertOneGetNewAndReset(), fields, new Object[]{"E1", beanOne, beanOne, "E3", beanThree, beanThree});

        stmt.destroy();
    }

    private void runAssertionWindowedUnGrouped(EPServiceProvider epService) {
        String epl = "select " +
                "first(theString) as firststring, " +
                "last(theString) as laststring, " +
                "first(intPrimitive) as firstint, " +
                "last(intPrimitive) as lastint, " +
                "window(intPrimitive) as allint " +
                "from SupportBean#length(2)";
        EPStatement stmt = epService.getEPAdministrator().createEPL(epl);
        SupportUpdateListener listener = new SupportUpdateListener();
        stmt.addListener(listener);

        tryAssertionUngrouped(epService, listener);

        stmt.destroy();

        EPStatementObjectModel model = epService.getEPAdministrator().compileEPL(epl);
        stmt = epService.getEPAdministrator().create(model);
        stmt.addListener(listener);
        assertEquals(epl, model.toEPL());

        tryAssertionUngrouped(epService, listener);

        stmt.destroy();

        // test null-value provided
        EPStatement stmtWNull = epService.getEPAdministrator().createEPL("select window(intBoxed).take(10) from SupportBean#length(2)");
        stmtWNull.addListener(listener);
        epService.getEPRuntime().sendEvent(new SupportBean("E1", 1));

        stmtWNull.destroy();
    }

    private void runAssertionWindowedGrouped(EPServiceProvider epService) {
        String epl = "select " +
                "theString, " +
                "first(theString) as firststring, " +
                "last(theString) as laststring, " +
                "first(intPrimitive) as firstint, " +
                "last(intPrimitive) as lastint, " +
                "window(intPrimitive) as allint " +
                "from SupportBean#length(5) " +
                "group by theString order by theString";
        EPStatement stmt = epService.getEPAdministrator().createEPL(epl);
        SupportUpdateListener listener = new SupportUpdateListener();
        stmt.addListener(listener);

        tryAssertionGrouped(epService, listener);

        stmt.destroy();

        // SODA
        EPStatementObjectModel model = epService.getEPAdministrator().compileEPL(epl);
        stmt = epService.getEPAdministrator().create(model);
        stmt.addListener(listener);
        assertEquals(epl, model.toEPL());

        tryAssertionGrouped(epService, listener);

        // test hints
        stmt.destroy();
        String newEPL = "@Hint('disable_reclaim_group') " + epl;
        stmt = epService.getEPAdministrator().createEPL(newEPL);
        stmt.addListener(listener);
        tryAssertionGrouped(epService, listener);

        // test hints
        stmt.destroy();
        newEPL = "@Hint('reclaim_group_aged=10,reclaim_group_freq=5') " + epl;
        stmt = epService.getEPAdministrator().createEPL(newEPL);
        stmt.addListener(listener);
        tryAssertionGrouped(epService, listener);
        stmt.destroy();

        // test SODA indexes
        String eplFirstLast = "select " +
                "last(intPrimitive), " +
                "last(intPrimitive,1), " +
                "first(intPrimitive), " +
                "first(intPrimitive,1) " +
                "from SupportBean#length(3)";
        EPStatementObjectModel modelFirstLast = epService.getEPAdministrator().compileEPL(eplFirstLast);
        assertEquals(eplFirstLast, modelFirstLast.toEPL());
    }

    private void tryAssertionGrouped(EPServiceProvider epService, SupportUpdateListener listener) {
        String[] fields = "theString,firststring,firstint,laststring,lastint,allint".split(",");

        epService.getEPRuntime().sendEvent(new SupportBean("E1", 10));
        EPAssertionUtil.assertProps(listener.assertOneGetNewAndReset(), fields, new Object[]{"E1", "E1", 10, "E1", 10, new int[]{10}});

        epService.getEPRuntime().sendEvent(new SupportBean("E2", 11));
        EPAssertionUtil.assertProps(listener.assertOneGetNewAndReset(), fields, new Object[]{"E2", "E2", 11, "E2", 11, new int[]{11}});

        epService.getEPRuntime().sendEvent(new SupportBean("E1", 12));
        EPAssertionUtil.assertProps(listener.assertOneGetNewAndReset(), fields, new Object[]{"E1", "E1", 10, "E1", 12, new int[]{10, 12}});

        epService.getEPRuntime().sendEvent(new SupportBean("E2", 13));
        EPAssertionUtil.assertProps(listener.assertOneGetNewAndReset(), fields, new Object[]{"E2", "E2", 11, "E2", 13, new int[]{11, 13}});

        epService.getEPRuntime().sendEvent(new SupportBean("E2", 14));
        EPAssertionUtil.assertProps(listener.assertOneGetNewAndReset(), fields, new Object[]{"E2", "E2", 11, "E2", 14, new int[]{11, 13, 14}});

        epService.getEPRuntime().sendEvent(new SupportBean("E1", 15));  // push out E1/10
        EPAssertionUtil.assertProps(listener.assertOneGetNewAndReset(), fields, new Object[]{"E1", "E1", 12, "E1", 15, new int[]{12, 15}});

        epService.getEPRuntime().sendEvent(new SupportBean("E1", 16));  // push out E2/11 --> 2 events
        EventBean[] received = listener.getAndResetLastNewData();
        EPAssertionUtil.assertPropsPerRow(received, fields,
                new Object[][]{
                    new Object[]{"E1", "E1", 12, "E1", 16, new int[]{12, 15, 16}},
                    new Object[]{"E2", "E2", 13, "E2", 14, new int[]{13, 14}}
                });
    }

    private void tryAssertionUngrouped(EPServiceProvider epService, SupportUpdateListener listener) {
        String[] fields = "firststring,firstint,laststring,lastint,allint".split(",");

        epService.getEPRuntime().sendEvent(new SupportBean("E1", 10));
        EPAssertionUtil.assertProps(listener.assertOneGetNewAndReset(), fields, new Object[]{"E1", 10, "E1", 10, new int[]{10}});

        epService.getEPRuntime().sendEvent(new SupportBean("E2", 11));
        EPAssertionUtil.assertProps(listener.assertOneGetNewAndReset(), fields, new Object[]{"E1", 10, "E2", 11, new int[]{10, 11}});

        epService.getEPRuntime().sendEvent(new SupportBean("E3", 12));
        EPAssertionUtil.assertProps(listener.assertOneGetNewAndReset(), fields, new Object[]{"E2", 11, "E3", 12, new int[]{11, 12}});

        epService.getEPRuntime().sendEvent(new SupportBean("E4", 13));
        EPAssertionUtil.assertProps(listener.assertOneGetNewAndReset(), fields, new Object[]{"E3", 12, "E4", 13, new int[]{12, 13}});
    }

    private Object split(String s) {
        if (s == null) {
            return new Object[0];
        }
        return s.split(",");
    }

    private int[] intArray(int... value) {
        if (value == null) {
            return new int[0];
        }
        return value;
    }

    private SupportBean sendEvent(EPServiceProvider epService, String theString, double doublePrimitive, int intPrimitive) {
        SupportBean bean = new SupportBean(theString, intPrimitive);
        bean.setDoublePrimitive(doublePrimitive);
        epService.getEPRuntime().sendEvent(bean);
        return bean;
    }

    public static class ChainEvent {
        private String property;

        public ChainEvent() {
        }

        public ChainEvent(String property) {
            this.property = property;
        }

        public String getProperty() {
            return property;
        }

        public String myMethod() {
            return "abc";
        }
    }
}
