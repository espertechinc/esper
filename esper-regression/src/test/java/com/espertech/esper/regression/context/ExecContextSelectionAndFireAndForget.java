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
package com.espertech.esper.regression.context;

import com.espertech.esper.client.*;
import com.espertech.esper.client.context.ContextPartitionSelector;
import com.espertech.esper.client.context.ContextPartitionSelectorAll;
import com.espertech.esper.client.context.ContextPartitionSelectorCategory;
import com.espertech.esper.client.scopetest.EPAssertionUtil;
import com.espertech.esper.client.soda.EPStatementObjectModel;
import com.espertech.esper.core.service.EPContextPartitionAdminSPI;
import com.espertech.esper.supportregression.bean.SupportBean;
import com.espertech.esper.supportregression.bean.SupportBean_S0;
import com.espertech.esper.supportregression.bean.SupportBean_S1;
import com.espertech.esper.supportregression.context.SupportSelectorById;
import com.espertech.esper.supportregression.context.SupportSelectorPartitioned;
import com.espertech.esper.supportregression.execution.RegressionExecution;

import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

import static junit.framework.TestCase.*;
import static org.junit.Assert.assertEquals;

public class ExecContextSelectionAndFireAndForget implements RegressionExecution {

    public void configure(Configuration configuration) throws Exception {
        configuration.addEventType("SupportBean", SupportBean.class);
        configuration.addEventType("SupportBean_S0", SupportBean_S0.class);
        configuration.addEventType("SupportBean_S1", SupportBean_S1.class);
        configuration.getEngineDefaults().getLogging().setEnableExecutionDebug(true);
    }

    public void run(EPServiceProvider epService) throws Exception {
        runAssertionInvalid(epService);
        runAssertionContextNamedWindowQuery(epService);
        runAssertionNestedContextNamedWindowQuery(epService);
        runAssertionIterateStatement(epService);
    }

    private void runAssertionInvalid(EPServiceProvider epService) {

        epService.getEPAdministrator().createEPL("create context SegmentedSB as partition by theString from SupportBean");
        epService.getEPAdministrator().createEPL("create context SegmentedS0 as partition by p00 from SupportBean_S0");
        epService.getEPAdministrator().createEPL("context SegmentedSB create window WinSB#keepall as SupportBean");
        epService.getEPAdministrator().createEPL("context SegmentedS0 create window WinS0#keepall as SupportBean_S0");
        epService.getEPAdministrator().createEPL("create window WinS1#keepall as SupportBean_S1");

        // when a context is declared, it must be the same context that applies to all named windows
        tryInvalidRuntimeQuery(epService, "context SegmentedSB select * from WinSB, WinS0",
                "Error executing statement: Joins in runtime queries for context partitions are not supported [context SegmentedSB select * from WinSB, WinS0]");

        tryInvalidRuntimeQuery(epService, null, "select * from WinSB, WinS1",
                "No context partition selectors provided");

        tryInvalidRuntimeQuery(epService, new ContextPartitionSelector[1], "select * from WinSB, WinS1",
                "Error executing statement: Number of context partition selectors does not match the number of named windows in the from-clause [select * from WinSB, WinS1]");

        // test join
        epService.getEPAdministrator().createEPL("create context PartitionedByString partition by theString from SupportBean");
        epService.getEPAdministrator().createEPL("context PartitionedByString create window MyWindowOne#keepall as SupportBean");

        epService.getEPAdministrator().createEPL("create context PartitionedByP00 partition by p00 from SupportBean_S0");
        epService.getEPAdministrator().createEPL("context PartitionedByP00 create window MyWindowTwo#keepall as SupportBean_S0");

        epService.getEPRuntime().sendEvent(new SupportBean("G1", 10));
        epService.getEPRuntime().sendEvent(new SupportBean("G2", 11));
        epService.getEPRuntime().sendEvent(new SupportBean_S0(1, "G2"));
        epService.getEPRuntime().sendEvent(new SupportBean_S0(2, "G1"));

        try {
            runQueryAll(epService, "select mw1.intPrimitive as c1, mw2.id as c2 from MyWindowOne mw1, MyWindowTwo mw2 where mw1.theString = mw2.p00", "c1,c2",
                    new Object[][]{{10, 2}, {11, 1}}, 2);
        } catch (EPStatementException ex) {
            assertEquals(ex.getMessage(), "Error executing statement: Joins against named windows that are under context are not supported [select mw1.intPrimitive as c1, mw2.id as c2 from MyWindowOne mw1, MyWindowTwo mw2 where mw1.theString = mw2.p00]");
        }

        epService.getEPAdministrator().destroyAllStatements();
    }

    private void runAssertionContextNamedWindowQuery(EPServiceProvider epService) {

        epService.getEPAdministrator().createEPL("create context PartitionedByString partition by theString from SupportBean");
        epService.getEPAdministrator().createEPL("context PartitionedByString create window MyWindow#keepall as SupportBean");
        epService.getEPAdministrator().createEPL("insert into MyWindow select * from SupportBean");

        epService.getEPRuntime().sendEvent(new SupportBean("E1", 10));
        epService.getEPRuntime().sendEvent(new SupportBean("E2", 20));
        epService.getEPRuntime().sendEvent(new SupportBean("E2", 21));

        // test no context
        runQueryAll(epService, "select sum(intPrimitive) as c1 from MyWindow", "c1", new Object[][]{{51}}, 1);
        runQueryAll(epService, "select sum(intPrimitive) as c1 from MyWindow where intPrimitive > 15", "c1", new Object[][]{{41}}, 1);
        runQuery(epService, "select sum(intPrimitive) as c1 from MyWindow", "c1", new Object[][]{{41}}, new ContextPartitionSelector[]{new SupportSelectorPartitioned(Collections.singletonList(new Object[]{"E2"}))});
        runQuery(epService, "select sum(intPrimitive) as c1 from MyWindow", "c1", new Object[][]{{41}}, new ContextPartitionSelector[]{new SupportSelectorById(Collections.<Integer>singleton(1))});

        // test with context props
        runQueryAll(epService, "context PartitionedByString select context.key1 as c0, intPrimitive as c1 from MyWindow",
                "c0,c1", new Object[][]{{"E1", 10}, {"E2", 20}, {"E2", 21}}, 1);
        runQueryAll(epService, "context PartitionedByString select context.key1 as c0, intPrimitive as c1 from MyWindow where intPrimitive > 15",
                "c0,c1", new Object[][]{{"E2", 20}, {"E2", 21}}, 1);

        // test targeted context partition
        runQuery(epService, "context PartitionedByString select context.key1 as c0, intPrimitive as c1 from MyWindow where intPrimitive > 15",
                "c0,c1", new Object[][]{{"E2", 20}, {"E2", 21}}, new SupportSelectorPartitioned[]{new SupportSelectorPartitioned(Collections.singletonList(new Object[]{"E2"}))});

        try {
            epService.getEPRuntime().executeQuery("context PartitionedByString select * from MyWindow", new ContextPartitionSelector[] {new ContextPartitionSelectorCategory() {
                public Set<String> getLabels() {
                    return null;
                }
            } });
            fail();
        } catch (EPStatementException ex) {
            assertTrue("message: " + ex.getMessage(), ex.getMessage().startsWith("Error executing statement: Invalid context partition selector, expected an implementation class of any of [ContextPartitionSelectorAll, ContextPartitionSelectorFiltered, ContextPartitionSelectorById, ContextPartitionSelectorSegmented] interfaces but received com"));
        }

        epService.getEPAdministrator().destroyAllStatements();
    }

    private void runAssertionNestedContextNamedWindowQuery(EPServiceProvider epService) {

        epService.getEPAdministrator().createEPL("create context NestedContext " +
                "context ACtx initiated by SupportBean_S0 as s0 terminated by SupportBean_S1(id=s0.id), " +
                "context BCtx group by intPrimitive < 0 as grp1, group by intPrimitive = 0 as grp2, group by intPrimitive > 0 as grp3 from SupportBean");
        epService.getEPAdministrator().createEPL("context NestedContext create window MyWindow#keepall as SupportBean");
        epService.getEPAdministrator().createEPL("insert into MyWindow select * from SupportBean");

        epService.getEPRuntime().sendEvent(new SupportBean_S0(1, "S0_1"));
        epService.getEPRuntime().sendEvent(new SupportBean("E1", 1));
        epService.getEPRuntime().sendEvent(new SupportBean_S0(2, "S0_2"));
        epService.getEPRuntime().sendEvent(new SupportBean("E2", -1));
        epService.getEPRuntime().sendEvent(new SupportBean("E3", 5));
        epService.getEPRuntime().sendEvent(new SupportBean("E1", 2));

        runQueryAll(epService, "select theString as c1, sum(intPrimitive) as c2 from MyWindow group by theString", "c1,c2", new Object[][]{{"E1", 5}, {"E2", -2}, {"E3", 10}}, 1);
        runQuery(epService, "select theString as c1, sum(intPrimitive) as c2 from MyWindow group by theString", "c1,c2", new Object[][]{{"E1", 3}, {"E3", 5}},
                new ContextPartitionSelector[]{new SupportSelectorById(Collections.singleton(2))});

        runQuery(epService, "context NestedContext select context.ACtx.s0.p00 as c1, context.BCtx.label as c2, theString as c3, sum(intPrimitive) as c4 from MyWindow group by theString", "c1,c2,c3,c4", new Object[][]{{"S0_1", "grp3", "E1", 3}, {"S0_1", "grp3", "E3", 5}},
                new ContextPartitionSelector[]{new SupportSelectorById(Collections.singleton(2))});

        // extract path
        if (getSpi(epService).isSupportsExtract()) {
            getSpi(epService).extractPaths("NestedContext", new ContextPartitionSelectorAll());
        }

        epService.getEPAdministrator().destroyAllStatements();
    }

    private void runAssertionIterateStatement(EPServiceProvider epService) {
        epService.getEPAdministrator().createEPL("create context PartitionedByString partition by theString from SupportBean");
        String[] fields = "c0,c1".split(",");
        EPStatement stmt = epService.getEPAdministrator().createEPL("@Name('StmtOne') context PartitionedByString select context.key1 as c0, sum(intPrimitive) as c1 from SupportBean#length(5)");

        epService.getEPRuntime().sendEvent(new SupportBean("E1", 10));
        epService.getEPRuntime().sendEvent(new SupportBean("E2", 20));
        epService.getEPRuntime().sendEvent(new SupportBean("E2", 21));

        Object[][] expectedAll = new Object[][]{{"E1", 10}, {"E2", 41}};
        EPAssertionUtil.assertPropsPerRow(stmt.iterator(), stmt.safeIterator(), fields, expectedAll);

        // test iterator ALL
        ContextPartitionSelector selector = ContextPartitionSelectorAll.INSTANCE;
        EPAssertionUtil.assertPropsPerRow(stmt.iterator(selector), stmt.safeIterator(selector), fields, expectedAll);

        // test iterator by context partition id
        selector = new SupportSelectorById(new HashSet<Integer>(Arrays.asList(0, 1, 2)));
        EPAssertionUtil.assertPropsPerRow(stmt.iterator(selector), stmt.safeIterator(selector), fields, expectedAll);

        selector = new SupportSelectorById(new HashSet<Integer>(Arrays.asList(1)));
        EPAssertionUtil.assertPropsPerRow(stmt.iterator(selector), stmt.safeIterator(selector), fields, new Object[][]{{"E2", 41}});

        assertFalse(stmt.iterator(new SupportSelectorById(Collections.<Integer>emptySet())).hasNext());
        assertFalse(stmt.iterator(new SupportSelectorById(null)).hasNext());

        try {
            stmt.iterator(null);
            fail();
        } catch (IllegalArgumentException ex) {
            assertEquals(ex.getMessage(), "No selector provided");
        }

        try {
            stmt.safeIterator(null);
            fail();
        } catch (IllegalArgumentException ex) {
            assertEquals(ex.getMessage(), "No selector provided");
        }

        EPStatement stmtTwo = epService.getEPAdministrator().createEPL("select * from java.lang.Object");
        try {
            stmtTwo.iterator(null);
            fail();
        } catch (UnsupportedOperationException ex) {
            assertEquals(ex.getMessage(), "Iterator with context selector is only supported for statements under context");
        }

        try {
            stmtTwo.safeIterator(null);
            fail();
        } catch (UnsupportedOperationException ex) {
            assertEquals(ex.getMessage(), "Iterator with context selector is only supported for statements under context");
        }

        epService.getEPAdministrator().destroyAllStatements();
    }

    private void runQueryAll(EPServiceProvider epService, String epl, String fields, Object[][] expected, int numStreams) {
        ContextPartitionSelector[] selectors = new ContextPartitionSelector[numStreams];
        for (int i = 0; i < numStreams; i++) {
            selectors[i] = ContextPartitionSelectorAll.INSTANCE;
        }

        runQuery(epService, epl, fields, expected, selectors);

        // run same query without selector
        EPOnDemandQueryResult result = epService.getEPRuntime().executeQuery(epl);
        EPAssertionUtil.assertPropsPerRowAnyOrder(result.getArray(), fields.split(","), expected);
    }

    private void runQuery(EPServiceProvider epService, String epl, String fields, Object[][] expected, ContextPartitionSelector[] selectors) {
        // try FAF without prepare
        EPOnDemandQueryResult result = epService.getEPRuntime().executeQuery(epl, selectors);
        EPAssertionUtil.assertPropsPerRowAnyOrder(result.getArray(), fields.split(","), expected);

        // test unparameterized prepare and execute
        EPOnDemandPreparedQuery preparedQuery = epService.getEPRuntime().prepareQuery(epl);
        EPOnDemandQueryResult resultPrepared = preparedQuery.execute(selectors);
        EPAssertionUtil.assertPropsPerRowAnyOrder(resultPrepared.getArray(), fields.split(","), expected);

        // test unparameterized prepare and execute
        EPOnDemandPreparedQueryParameterized preparedParameterizedQuery = epService.getEPRuntime().prepareQueryWithParameters(epl);
        EPOnDemandQueryResult resultPreparedParameterized = epService.getEPRuntime().executeQuery(preparedParameterizedQuery, selectors);
        EPAssertionUtil.assertPropsPerRowAnyOrder(resultPreparedParameterized.getArray(), fields.split(","), expected);

        // test SODA prepare and execute
        EPStatementObjectModel modelForPrepare = epService.getEPAdministrator().compileEPL(epl);
        EPOnDemandPreparedQuery preparedQueryModel = epService.getEPRuntime().prepareQuery(modelForPrepare);
        EPOnDemandQueryResult resultPreparedModel = preparedQueryModel.execute(selectors);
        EPAssertionUtil.assertPropsPerRowAnyOrder(resultPreparedModel.getArray(), fields.split(","), expected);

        // test model query
        EPStatementObjectModel model = epService.getEPAdministrator().compileEPL(epl);
        result = epService.getEPRuntime().executeQuery(model, selectors);
        EPAssertionUtil.assertPropsPerRowAnyOrder(result.getArray(), fields.split(","), expected);
    }

    private void tryInvalidRuntimeQuery(EPServiceProvider epService, ContextPartitionSelector[] selectors, String epl, String expected) {
        try {
            epService.getEPRuntime().executeQuery(epl, selectors);
            fail();
        } catch (Exception ex) {
            assertEquals(ex.getMessage(), expected);
        }
    }

    private void tryInvalidRuntimeQuery(EPServiceProvider epService, String epl, String expected) {
        try {
            epService.getEPRuntime().executeQuery(epl);
            fail();
        } catch (Exception ex) {
            assertEquals(expected, ex.getMessage());
        }
    }

    private static EPContextPartitionAdminSPI getSpi(EPServiceProvider epService) {
        return (EPContextPartitionAdminSPI) epService.getEPAdministrator().getContextPartitionAdmin();
    }
}
