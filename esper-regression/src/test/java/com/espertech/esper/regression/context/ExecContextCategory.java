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

import com.espertech.esper.client.Configuration;
import com.espertech.esper.client.EPServiceProvider;
import com.espertech.esper.client.EPStatement;
import com.espertech.esper.client.context.*;
import com.espertech.esper.client.scopetest.EPAssertionUtil;
import com.espertech.esper.client.scopetest.SupportUpdateListener;
import com.espertech.esper.client.soda.EPStatementObjectModel;
import com.espertech.esper.core.service.EPServiceProviderSPI;
import com.espertech.esper.core.service.EPStatementSPI;
import com.espertech.esper.filter.FilterServiceSPI;
import com.espertech.esper.supportregression.bean.SupportBean;
import com.espertech.esper.supportregression.bean.SupportBean_S0;
import com.espertech.esper.supportregression.context.SupportContextPropUtil;
import com.espertech.esper.supportregression.context.SupportSelectorById;
import com.espertech.esper.supportregression.context.SupportSelectorCategory;
import com.espertech.esper.supportregression.execution.RegressionExecution;
import com.espertech.esper.supportregression.util.AgentInstanceAssertionUtil;

import java.util.*;

import static com.espertech.esper.supportregression.util.SupportMessageAssertUtil.tryInvalid;
import static junit.framework.TestCase.*;
import static org.junit.Assert.assertEquals;

public class ExecContextCategory implements RegressionExecution {

    public void configure(Configuration configuration) throws Exception {
        configuration.addEventType("SupportBean", SupportBean.class);
        configuration.addEventType("SupportBean_S0", SupportBean_S0.class);
        configuration.getEngineDefaults().getLogging().setEnableExecutionDebug(true);
    }

    public void run(EPServiceProvider epService) throws Exception {
        runAssertionBooleanExprFilter(epService);
        runAssertionContextPartitionSelection(epService);
        runAssertionCategory(epService);
        runAssertionSingleCategorySODAPrior(epService);
        runAssertionInvalid(epService);
    }

    private void runAssertionBooleanExprFilter(EPServiceProvider epService) {
        String eplCtx = "create context Ctx600a group by theString like 'A%' as agroup, group by theString like 'B%' as bgroup, group by theString like 'C%' as cgroup from SupportBean";
        epService.getEPAdministrator().createEPL(eplCtx);
        String eplSum = "context Ctx600a select context.label as c0, count(*) as c1 from SupportBean";
        EPStatement stmt = epService.getEPAdministrator().createEPL(eplSum);
        SupportUpdateListener listener = new SupportUpdateListener();
        stmt.addListener(listener);

        sendAssertBooleanExprFilter(epService, listener, "B1", "bgroup", 1);
        sendAssertBooleanExprFilter(epService, listener, "A1", "agroup", 1);
        sendAssertBooleanExprFilter(epService, listener, "B171771", "bgroup", 2);
        sendAssertBooleanExprFilter(epService, listener, "A  x", "agroup", 2);

        epService.getEPAdministrator().destroyAllStatements();
    }

    private void runAssertionContextPartitionSelection(EPServiceProvider epService) {
        String[] fields = "c0,c1,c2,c3".split(",");
        epService.getEPAdministrator().createEPL("create context MyCtx as group by intPrimitive < -5 as grp1, group by intPrimitive between -5 and +5 as grp2, group by intPrimitive > 5 as grp3 from SupportBean");
        EPStatement stmt = epService.getEPAdministrator().createEPL("context MyCtx select context.id as c0, context.label as c1, theString as c2, sum(intPrimitive) as c3 from SupportBean#keepall group by theString");

        epService.getEPRuntime().sendEvent(new SupportBean("E1", 1));
        epService.getEPRuntime().sendEvent(new SupportBean("E2", -5));
        epService.getEPRuntime().sendEvent(new SupportBean("E1", 2));
        epService.getEPRuntime().sendEvent(new SupportBean("E3", -100));
        epService.getEPRuntime().sendEvent(new SupportBean("E3", -8));
        epService.getEPRuntime().sendEvent(new SupportBean("E1", 60));
        EPAssertionUtil.assertPropsPerRowAnyOrder(stmt.iterator(), stmt.safeIterator(), fields, new Object[][]{{0, "grp1", "E3", -108}, {1, "grp2", "E1", 3}, {1, "grp2", "E2", -5}, {2, "grp3", "E1", 60}});
        SupportContextPropUtil.assertContextProps(epService, "MyCtx", new int[] {0, 1, 2}, "label", new Object[][] {{"grp1"}, {"grp2"}, {"grp3"}});

        // test iterator targeted by context partition id
        SupportSelectorById selectorById = new SupportSelectorById(Collections.singleton(1));
        EPAssertionUtil.assertPropsPerRowAnyOrder(stmt.iterator(selectorById), stmt.safeIterator(selectorById), fields, new Object[][]{{1, "grp2", "E1", 3}, {1, "grp2", "E2", -5}});

        // test iterator targeted for a given category
        SupportSelectorCategory selector = new SupportSelectorCategory(new HashSet<>(Arrays.asList("grp1", "grp3")));
        EPAssertionUtil.assertPropsPerRowAnyOrder(stmt.iterator(selector), stmt.safeIterator(selector), fields, new Object[][]{{0, "grp1", "E3", -108}, {2, "grp3", "E1", 60}});

        // test iterator targeted for a given filtered category
        MySelectorFilteredCategory filtered = new MySelectorFilteredCategory("grp1");
        EPAssertionUtil.assertPropsPerRowAnyOrder(stmt.iterator(filtered), stmt.safeIterator(filtered), fields, new Object[][]{{0, "grp1", "E3", -108}});
        assertFalse(stmt.iterator(new SupportSelectorCategory((Set<String>) null)).hasNext());
        assertFalse(stmt.iterator(new SupportSelectorCategory(Collections.emptySet())).hasNext());

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
        } catch (InvalidContextPartitionSelector ex) {
            assertTrue("message: " + ex.getMessage(), ex.getMessage().startsWith("Invalid context partition selector, expected an implementation class of any of [ContextPartitionSelectorAll, ContextPartitionSelectorFiltered, ContextPartitionSelectorById, ContextPartitionSelectorCategory] interfaces but received com."));
        }

        epService.getEPAdministrator().destroyAllStatements();
    }

    private void runAssertionInvalid(EPServiceProvider epService) {
        String epl;

        // invalid filter spec
        epl = "create context ACtx group theString is not null as cat1 from SupportBean(dummy = 1)";
        tryInvalid(epService, epl, "Error starting statement: Failed to validate filter expression 'dummy=1': Property named 'dummy' is not valid in any stream [");

        // not a boolean expression
        epl = "create context ACtx group intPrimitive as grp1 from SupportBean";
        tryInvalid(epService, epl, "Error starting statement: Filter expression not returning a boolean value: 'intPrimitive' [");

        // validate statement not applicable filters
        epService.getEPAdministrator().createEPL("create context ACtx group intPrimitive < 10 as cat1 from SupportBean");
        epl = "context ACtx select * from SupportBean_S0";
        tryInvalid(epService, epl, "Error starting statement: Category context 'ACtx' requires that any of the events types that are listed in the category context also appear in any of the filter expressions of the statement [");

        epService.getEPAdministrator().destroyAllStatements();
    }

    private void runAssertionCategory(EPServiceProvider epService) {
        FilterServiceSPI filterSPI = (FilterServiceSPI) ((EPServiceProviderSPI) epService).getFilterService();
        String ctx = "CategorizedContext";
        epService.getEPAdministrator().createEPL("@Name('context') create context " + ctx + " " +
                "group intPrimitive < 10 as cat1, " +
                "group intPrimitive between 10 and 20 as cat2, " +
                "group intPrimitive > 20 as cat3 " +
                "from SupportBean");

        String[] fields = "c0,c1,c2".split(",");
        EPStatementSPI statement = (EPStatementSPI) epService.getEPAdministrator().createEPL("context CategorizedContext " +
                "select context.name as c0, context.label as c1, sum(intPrimitive) as c2 from SupportBean");
        SupportUpdateListener listener = new SupportUpdateListener();
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

        EPServiceProviderSPI spi = (EPServiceProviderSPI) epService;
        assertEquals(1, spi.getContextManagementService().getContextCount());
        epService.getEPAdministrator().getStatement("context").destroy();
        assertEquals(1, spi.getContextManagementService().getContextCount());

        statement.destroy();
        assertEquals(0, spi.getContextManagementService().getContextCount());
    }

    private void runAssertionSingleCategorySODAPrior(EPServiceProvider epService) {
        String ctx = "CategorizedContext";
        String eplCtx = "@Name('context') create context " + ctx + " as " +
                "group intPrimitive<10 as cat1 " +
                "from SupportBean";
        epService.getEPAdministrator().createEPL(eplCtx);

        String eplStmt = "context CategorizedContext select context.name as c0, context.label as c1, prior(1,intPrimitive) as c2 from SupportBean";
        EPStatementSPI statementOne = (EPStatementSPI) epService.getEPAdministrator().createEPL(eplStmt);
        SupportUpdateListener listener = new SupportUpdateListener();

        runAssertion(epService, listener, ctx, statementOne);

        // test SODA
        EPStatementObjectModel modelContext = epService.getEPAdministrator().compileEPL(eplCtx);
        assertEquals(eplCtx, modelContext.toEPL());
        EPStatement stmt = epService.getEPAdministrator().create(modelContext);
        assertEquals(eplCtx, stmt.getText());

        EPStatementObjectModel modelStmt = epService.getEPAdministrator().compileEPL(eplStmt);
        assertEquals(eplStmt, modelStmt.toEPL());
        EPStatementSPI statementTwo = (EPStatementSPI) epService.getEPAdministrator().create(modelStmt);
        assertEquals(eplStmt, statementTwo.getText());

        runAssertion(epService, listener, ctx, statementTwo);
    }

    private void runAssertion(EPServiceProvider epService, SupportUpdateListener listener, String ctx, EPStatementSPI statement) {
        statement.addListener(listener);

        String[] fields = "c0,c1,c2".split(",");
        epService.getEPRuntime().sendEvent(new SupportBean("E1", 5));
        EPAssertionUtil.assertProps(listener.assertOneGetNewAndReset(), fields, new Object[]{ctx, "cat1", null});

        epService.getEPRuntime().sendEvent(new SupportBean("E2", 20));
        assertFalse(listener.isInvoked());

        epService.getEPRuntime().sendEvent(new SupportBean("E1", 4));
        EPAssertionUtil.assertProps(listener.assertOneGetNewAndReset(), fields, new Object[]{ctx, "cat1", 5});

        epService.getEPAdministrator().getStatement("context").destroy();
        EPServiceProviderSPI spi = (EPServiceProviderSPI) epService;
        assertEquals(1, spi.getContextManagementService().getContextCount());

        epService.getEPAdministrator().destroyAllStatements();
        AgentInstanceAssertionUtil.assertInstanceCounts(statement.getStatementContext(), 0, 0, 0, 0);
        assertEquals(0, spi.getContextManagementService().getContextCount());
    }

    private void sendAssertBooleanExprFilter(EPServiceProvider epService, SupportUpdateListener listener, String theString, String groupExpected, long countExpected) {
        String[] fields = "c0,c1".split(",");
        epService.getEPRuntime().sendEvent(new SupportBean(theString, 1));
        EPAssertionUtil.assertProps(listener.assertOneGetNewAndReset(), fields, new Object[]{groupExpected, countExpected});
    }

    private static class MySelectorFilteredCategory implements ContextPartitionSelectorFiltered {

        private final String matchCategory;

        private List<Object> categories = new ArrayList<>();
        private LinkedHashSet<Integer> cpids = new LinkedHashSet<>();

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

        Object[] getCategories() {
            return categories.toArray();
        }
    }
}
