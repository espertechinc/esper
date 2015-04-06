/*
 * *************************************************************************************
 *  Copyright (C) 2006-2015 EsperTech, Inc. All rights reserved.                       *
 *  http://www.espertech.com/esper                                                     *
 *  http://www.espertech.com                                                           *
 *  ---------------------------------------------------------------------------------- *
 *  The software in this package is published under the terms of the GPL license       *
 *  a copy of which has been included with this distribution in the license.txt file.  *
 * *************************************************************************************
 */

package com.espertech.esper.regression.context;

import com.espertech.esper.client.*;
import com.espertech.esper.client.context.*;
import com.espertech.esper.client.scopetest.EPAssertionUtil;
import com.espertech.esper.client.scopetest.SupportUpdateListener;
import com.espertech.esper.client.soda.EPStatementObjectModel;
import com.espertech.esper.core.service.EPServiceProviderSPI;
import com.espertech.esper.core.service.EPStatementSPI;
import com.espertech.esper.filter.FilterServiceSPI;
import com.espertech.esper.metrics.instrumentation.InstrumentationHelper;
import com.espertech.esper.support.bean.SupportBean;
import com.espertech.esper.support.bean.SupportBean_S0;
import com.espertech.esper.support.client.SupportConfigFactory;
import com.espertech.esper.support.util.AgentInstanceAssertionUtil;
import junit.framework.TestCase;

import java.util.*;

public class TestContextCategory extends TestCase {

    private EPServiceProvider epService;
    private SupportUpdateListener listener;
    private EPServiceProviderSPI spi;

    public void setUp()
    {
        Configuration configuration = SupportConfigFactory.getConfiguration();
        configuration.addEventType("SupportBean", SupportBean.class);
        configuration.addEventType("SupportBean_S0", SupportBean_S0.class);
        configuration.getEngineDefaults().getLogging().setEnableExecutionDebug(true);
        epService = EPServiceProviderManager.getDefaultProvider(configuration);
        epService.initialize();
        if (InstrumentationHelper.ENABLED) { InstrumentationHelper.startTest(epService, this.getClass(), getName());}
        spi = (EPServiceProviderSPI) epService;

        listener = new SupportUpdateListener();
    }

    public void tearDown() {
        if (InstrumentationHelper.ENABLED) { InstrumentationHelper.endTest();}
        listener = null;
    }

    public void testContextPartitionSelection() {
        String[] fields = "c0,c1,c2,c3".split(",");
        epService.getEPAdministrator().createEPL("create context MyCtx as group by intPrimitive < -5 as grp1, group by intPrimitive between -5 and +5 as grp2, group by intPrimitive > 5 as grp3 from SupportBean");
        EPStatement stmt = epService.getEPAdministrator().createEPL("context MyCtx select context.id as c0, context.label as c1, theString as c2, sum(intPrimitive) as c3 from SupportBean.win:keepall() group by theString");

        epService.getEPRuntime().sendEvent(new SupportBean("E1", 1));
        epService.getEPRuntime().sendEvent(new SupportBean("E2", -5));
        epService.getEPRuntime().sendEvent(new SupportBean("E1", 2));
        epService.getEPRuntime().sendEvent(new SupportBean("E3", -100));
        epService.getEPRuntime().sendEvent(new SupportBean("E3", -8));
        epService.getEPRuntime().sendEvent(new SupportBean("E1", 60));
        EPAssertionUtil.assertPropsPerRowAnyOrder(stmt.iterator(), stmt.safeIterator(), fields, new Object[][]{{0, "grp1", "E3", -108}, {1, "grp2", "E1", 3}, {1, "grp2", "E2", -5}, {2, "grp3", "E1", 60}});

        // test iterator targeted by context partition id
        SupportSelectorById selectorById = new SupportSelectorById(Collections.singleton(1));
        EPAssertionUtil.assertPropsPerRowAnyOrder(stmt.iterator(selectorById), stmt.safeIterator(selectorById), fields, new Object[][]{{1, "grp2", "E1", 3}, {1, "grp2", "E2", -5}});

        // test iterator targeted for a given category
        SupportSelectorCategory selector = new SupportSelectorCategory(new HashSet<String>(Arrays.asList("grp1", "grp3")));
        EPAssertionUtil.assertPropsPerRowAnyOrder(stmt.iterator(selector), stmt.safeIterator(selector), fields, new Object[][]{{0, "grp1", "E3", -108}, {2, "grp3", "E1", 60}});

        // test iterator targeted for a given filtered category
        MySelectorFilteredCategory filtered = new MySelectorFilteredCategory("grp1");
        EPAssertionUtil.assertPropsPerRowAnyOrder(stmt.iterator(filtered), stmt.safeIterator(filtered), fields, new Object[][]{{0, "grp1", "E3", -108}});
        assertFalse(stmt.iterator(new SupportSelectorCategory((Set<String>)null)).hasNext());
        assertFalse(stmt.iterator(new SupportSelectorCategory(Collections.<String>emptySet())).hasNext());

        // test always-false filter - compare context partition info
        filtered = new MySelectorFilteredCategory(null);
        assertFalse(stmt.iterator(filtered).hasNext());
        EPAssertionUtil.assertEqualsAnyOrder(new Object[]{"grp1", "grp2", "grp3"}, filtered.getCategories());

        try {
            stmt.iterator(new ContextPartitionSelectorSegmented() {
                public List<Object[]> getPartitionKeys() {
                    return null;
                }
            });
            fail();
        }
        catch (InvalidContextPartitionSelector ex) {
            assertTrue("message: " + ex.getMessage(), ex.getMessage().startsWith("Invalid context partition selector, expected an implementation class of any of [ContextPartitionSelectorAll, ContextPartitionSelectorFiltered, ContextPartitionSelectorById, ContextPartitionSelectorCategory] interfaces but received com."));
        }
    }

    public void testInvalid() {
        String epl;

        // invalid filter spec
        epl = "create context ACtx group theString is not null as cat1 from SupportBean(dummy = 1)";
        tryInvalid(epl, "Error starting statement: Failed to validate filter expression 'dummy=1': Property named 'dummy' is not valid in any stream [");

        // not a boolean expression
        epl = "create context ACtx group intPrimitive as grp1 from SupportBean";
        tryInvalid(epl, "Error starting statement: Filter expression not returning a boolean value: 'intPrimitive' [");

        // validate statement not applicable filters
        epService.getEPAdministrator().createEPL("create context ACtx group intPrimitive < 10 as cat1 from SupportBean");
        epl = "context ACtx select * from SupportBean_S0";
        tryInvalid(epl, "Error starting statement: Category context 'ACtx' requires that any of the events types that are listed in the category context also appear in any of the filter expressions of the statement [");
    }

    private void tryInvalid(String epl, String expected) {
        try {
            epService.getEPAdministrator().createEPL(epl);
            fail();
        }
        catch (EPStatementException ex) {
            if (!ex.getMessage().startsWith(expected)) {
                throw new RuntimeException("Expected/Received:\n" + expected + "\n" + ex.getMessage() + "\n");
            }
            assertTrue(expected.trim().length() != 0);
        }
    }

    public void testCategory() {
        FilterServiceSPI filterSPI = (FilterServiceSPI) spi.getFilterService();
        String ctx = "CategorizedContext";
        epService.getEPAdministrator().createEPL("@Name('context') create context " + ctx + " " +
                "group intPrimitive < 10 as cat1, " +
                "group intPrimitive between 10 and 20 as cat2, " +
                "group intPrimitive > 20 as cat3 " +
                "from SupportBean");

        String[] fields = "c0,c1,c2".split(",");
        EPStatementSPI statement = (EPStatementSPI) epService.getEPAdministrator().createEPL("context CategorizedContext " +
                "select context.name as c0, context.label as c1, sum(intPrimitive) as c2 from SupportBean");
        statement.addListener(listener);
        assertEquals(3, filterSPI.getFilterCountApprox());
        AgentInstanceAssertionUtil.assertInstanceCounts(statement.getStatementContext(), 3, 0, 0, 0);

        epService.getEPRuntime().sendEvent(new SupportBean("E1", 5));
        EPAssertionUtil.assertProps(listener.assertOneGetNewAndReset(), fields, new Object[]{ctx, "cat1", 5});
        EPAssertionUtil.assertPropsPerRow(statement.iterator(), statement.safeIterator(), fields, new Object[][]{{ctx, "cat1", 5}, {ctx, "cat2", null}, {ctx, "cat3", null}});

        epService.getEPRuntime().sendEvent(new SupportBean("E2", 4));
        EPAssertionUtil.assertProps(listener.assertOneGetNewAndReset(), fields, new Object[]{ctx, "cat1", 9});

        epService.getEPRuntime().sendEvent(new SupportBean("E3", 11));
        EPAssertionUtil.assertProps(listener.assertOneGetNewAndReset(), fields, new Object[]{ctx, "cat2", 11});

        epService.getEPRuntime().sendEvent(new SupportBean("E4", 25));
        EPAssertionUtil.assertProps(listener.assertOneGetNewAndReset(), fields, new Object[]{ctx, "cat3", 25});

        epService.getEPRuntime().sendEvent(new SupportBean("E5", 25));
        EPAssertionUtil.assertProps(listener.assertOneGetNewAndReset(), fields, new Object[]{ctx, "cat3", 50});

        epService.getEPRuntime().sendEvent(new SupportBean("E6", 3));
        EPAssertionUtil.assertProps(listener.assertOneGetNewAndReset(), fields, new Object[]{ctx, "cat1", 12});

        EPAssertionUtil.assertPropsPerRow(statement.iterator(), statement.safeIterator(), fields, new Object[][]{{ctx, "cat1", 12}, {ctx, "cat2", 11}, {ctx, "cat3", 50}});

        statement.stop();
        assertEquals(0, filterSPI.getFilterCountApprox());
        AgentInstanceAssertionUtil.assertInstanceCounts(statement.getStatementContext(), 0, 0, 0, 0);

        assertEquals(1, spi.getContextManagementService().getContextCount());
        epService.getEPAdministrator().getStatement("context").destroy();
        assertEquals(1, spi.getContextManagementService().getContextCount());

        statement.destroy();
        assertEquals(0, spi.getContextManagementService().getContextCount());
    }

    public void testSingleCategorySODAPrior() {
        String ctx = "CategorizedContext";
        String eplCtx = "@Name('context') create context " + ctx + " as " +
                "group intPrimitive<10 as cat1 " +
                "from SupportBean";
        epService.getEPAdministrator().createEPL(eplCtx);

        String eplStmt = "context CategorizedContext select context.name as c0, context.label as c1, prior(1,intPrimitive) as c2 from SupportBean";
        EPStatementSPI statementOne = (EPStatementSPI) epService.getEPAdministrator().createEPL(eplStmt);

        runAssertion(ctx, statementOne);

        // test SODA
        EPStatementObjectModel modelContext = epService.getEPAdministrator().compileEPL(eplCtx);
        assertEquals(eplCtx, modelContext.toEPL());
        EPStatement stmt = epService.getEPAdministrator().create(modelContext);
        assertEquals(eplCtx, stmt.getText());

        EPStatementObjectModel modelStmt = epService.getEPAdministrator().compileEPL(eplStmt);
        assertEquals(eplStmt, modelStmt.toEPL());
        EPStatementSPI statementTwo = (EPStatementSPI) epService.getEPAdministrator().create(modelStmt);
        assertEquals(eplStmt, statementTwo.getText());

        runAssertion(ctx, statementTwo);
    }

    private void runAssertion(String ctx, EPStatementSPI statement) {
        statement.addListener(listener);

        String[] fields = "c0,c1,c2".split(",");
        epService.getEPRuntime().sendEvent(new SupportBean("E1", 5));
        EPAssertionUtil.assertProps(listener.assertOneGetNewAndReset(), fields, new Object[]{ctx, "cat1", null});

        epService.getEPRuntime().sendEvent(new SupportBean("E2", 20));
        assertFalse(listener.isInvoked());

        epService.getEPRuntime().sendEvent(new SupportBean("E1", 4));
        EPAssertionUtil.assertProps(listener.assertOneGetNewAndReset(), fields, new Object[]{ctx, "cat1", 5});

        epService.getEPAdministrator().getStatement("context").destroy();
        assertEquals(1, spi.getContextManagementService().getContextCount());

        epService.getEPAdministrator().destroyAllStatements();
        AgentInstanceAssertionUtil.assertInstanceCounts(statement.getStatementContext(), 0, 0, 0, 0);
        assertEquals(0, spi.getContextManagementService().getContextCount());
    }

    private static class MySelectorFilteredCategory implements ContextPartitionSelectorFiltered {

        private final String matchCategory;

        private List<Object> categories = new ArrayList<Object>();
        private LinkedHashSet<Integer> cpids = new LinkedHashSet<Integer>();

        private MySelectorFilteredCategory(String matchCategory) {
            this.matchCategory = matchCategory;
        }

        public boolean filter(ContextPartitionIdentifier contextPartitionIdentifier) {
            ContextPartitionIdentifierCategory id = (ContextPartitionIdentifierCategory) contextPartitionIdentifier;
            if (matchCategory == null && cpids.contains(id.getContextPartitionId())) {
                throw new RuntimeException("Already exists context id: " + id.getContextPartitionId());
            }
            cpids.add(id.getContextPartitionId());
            categories.add(id.getLabel());
            return matchCategory != null && matchCategory.equals(id.getLabel());
        }

        public Object[] getCategories() {
            return categories.toArray();
        }
    }
}
