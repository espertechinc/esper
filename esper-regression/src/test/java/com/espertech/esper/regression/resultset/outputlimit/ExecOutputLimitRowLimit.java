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
package com.espertech.esper.regression.resultset.outputlimit;

import com.espertech.esper.client.Configuration;
import com.espertech.esper.client.EPRuntime;
import com.espertech.esper.client.EPServiceProvider;
import com.espertech.esper.client.EPStatement;
import com.espertech.esper.client.scopetest.EPAssertionUtil;
import com.espertech.esper.client.scopetest.SupportUpdateListener;
import com.espertech.esper.client.soda.*;
import com.espertech.esper.client.time.CurrentTimeEvent;
import com.espertech.esper.supportregression.bean.SupportBean;
import com.espertech.esper.supportregression.bean.SupportBeanNumeric;
import com.espertech.esper.supportregression.bean.SupportBean_S0;
import com.espertech.esper.supportregression.bean.SupportBean_S1;
import com.espertech.esper.supportregression.execution.RegressionExecution;

import static com.espertech.esper.supportregression.util.SupportMessageAssertUtil.tryInvalid;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;

public class ExecOutputLimitRowLimit implements RegressionExecution {

    public void configure(Configuration configuration) throws Exception {
        configuration.addEventType("SupportBean", SupportBean.class);
        configuration.addEventType("SupportBeanNumeric", SupportBeanNumeric.class);
    }

    public void run(EPServiceProvider epService) throws Exception {
        runAssertionLimitOneWithOrderOptimization(epService);
        runAssertionBatchNoOffsetNoOrder(epService);
        runAssertionLengthOffsetVariable(epService);
        runAssertionOrderBy(epService);
        runAssertionBatchOffsetNoOrderOM(epService);
        runAssertionFullyGroupedOrdered(epService);
        runAssertionEventPerRowUnGrouped(epService);
        runAssertionGroupedSnapshot(epService);
        runAssertionGroupedSnapshotNegativeRowcount(epService);
        runAssertionInvalid(epService);
    }

    private void runAssertionLimitOneWithOrderOptimization(EPServiceProvider epService) {
        epService.getEPAdministrator().getConfiguration().addEventType(SupportBean_S0.class);
        epService.getEPAdministrator().getConfiguration().addEventType(SupportBean_S1.class);

        // batch-window assertions
        String eplWithBatchSingleKey = "select theString from SupportBean#length_batch(10) order by theString limit 1";
        tryAssertionLimitOneSingleKeySortBatch(epService, eplWithBatchSingleKey);

        String eplWithBatchMultiKey = "select theString, intPrimitive from SupportBean#length_batch(5) order by theString asc, intPrimitive desc limit 1";
        tryAssertionLimitOneMultiKeySortBatch(epService, eplWithBatchMultiKey);

        // context output-when-terminated assertions
        epService.getEPAdministrator().createEPL("create context StartS0EndS1 as start SupportBean_S0 end SupportBean_S1");

        String eplContextSingleKey = "context StartS0EndS1 " +
                "select theString from SupportBean#keepall " +
                "output snapshot when terminated " +
                "order by theString limit 1";
        tryAssertionLimitOneSingleKeySortBatch(epService, eplContextSingleKey);

        String eplContextMultiKey = "context StartS0EndS1 " +
                "select theString, intPrimitive from SupportBean#keepall " +
                "output snapshot when terminated " +
                "order by theString asc, intPrimitive desc limit 1";
        tryAssertionLimitOneMultiKeySortBatch(epService, eplContextMultiKey);
    }

    private void tryAssertionLimitOneMultiKeySortBatch(EPServiceProvider epService, String epl) {
        EPStatement stmt = epService.getEPAdministrator().createEPL(epl);
        SupportUpdateListener listener = new SupportUpdateListener();
        stmt.addListener(listener);

        sendSBSequenceAndAssert(epService, listener, "F", 10, new Object[][]{{"F", 10}, {"X", 8}, {"F", 8}, {"G", 10}, {"X", 1}});
        sendSBSequenceAndAssert(epService, listener, "G", 12, new Object[][]{{"X", 10}, {"G", 12}, {"H", 100}, {"G", 10}, {"X", 1}});
        sendSBSequenceAndAssert(epService, listener, "G", 11, new Object[][]{{"G", 10}, {"G", 8}, {"G", 8}, {"G", 10}, {"G", 11}});

        stmt.destroy();
    }

    private void tryAssertionLimitOneSingleKeySortBatch(EPServiceProvider epService, String epl) {
        EPStatement stmt = epService.getEPAdministrator().createEPL(epl);
        SupportUpdateListener listener = new SupportUpdateListener();
        stmt.addListener(listener);

        sendSBSequenceAndAssert(epService, listener, "A", new String[]{"F", "Q", "R", "T", "M", "T", "A", "I", "P", "B"});
        sendSBSequenceAndAssert(epService, listener, "B", new String[]{"P", "Q", "P", "T", "P", "T", "P", "P", "P", "B"});
        sendSBSequenceAndAssert(epService, listener, "C", new String[]{"C", "P", "Q", "P", "T", "P", "T", "P", "P", "P", "X"});

        stmt.destroy();
    }

    private void runAssertionBatchNoOffsetNoOrder(EPServiceProvider epService) {
        String statementString = "select irstream * from SupportBean#length_batch(3) limit 1";
        EPStatement stmt = epService.getEPAdministrator().createEPL(statementString);
        SupportUpdateListener listener = new SupportUpdateListener();
        tryAssertion(epService, stmt, listener);
        stmt.destroy();
    }

    private void runAssertionLengthOffsetVariable(EPServiceProvider epService) {
        epService.getEPAdministrator().createEPL("create variable int myrows = 2");
        epService.getEPAdministrator().createEPL("create variable int myoffset = 1");
        epService.getEPAdministrator().createEPL("on SupportBeanNumeric set myrows = intOne, myoffset = intTwo");

        String statementString = "select * from SupportBean#length(5) output every 5 events limit myoffset, myrows";
        EPStatement stmt = epService.getEPAdministrator().createEPL(statementString);
        SupportUpdateListener listener = new SupportUpdateListener();
        tryAssertionVariable(epService, stmt, listener);
        stmt.destroy();
        listener.reset();
        epService.getEPRuntime().sendEvent(new SupportBeanNumeric(2, 1));

        statementString = "select * from SupportBean#length(5) output every 5 events limit myrows offset myoffset";
        stmt = epService.getEPAdministrator().createEPL(statementString);
        tryAssertionVariable(epService, stmt, listener);
        stmt.destroy();
        listener.reset();
        epService.getEPRuntime().sendEvent(new SupportBeanNumeric(2, 1));

        EPStatementObjectModel model = epService.getEPAdministrator().compileEPL(statementString);
        assertEquals(statementString, model.toEPL());
        stmt = epService.getEPAdministrator().create(model);
        tryAssertionVariable(epService, stmt, listener);

        epService.getEPAdministrator().destroyAllStatements();
    }

    private void runAssertionOrderBy(EPServiceProvider epService) {
        String statementString = "select * from SupportBean#length(5) output every 5 events order by intPrimitive limit 2 offset 2";
        EPStatement stmt = epService.getEPAdministrator().createEPL(statementString);
        SupportUpdateListener listener = new SupportUpdateListener();

        String[] fields = "theString".split(",");
        stmt.addListener(listener);
        EPAssertionUtil.assertPropsPerRow(stmt.iterator(), fields, null);

        sendEvent(epService, "E1", 90);
        EPAssertionUtil.assertPropsPerRow(stmt.iterator(), fields, null);

        sendEvent(epService, "E2", 5);
        EPAssertionUtil.assertPropsPerRow(stmt.iterator(), fields, null);

        sendEvent(epService, "E3", 60);
        EPAssertionUtil.assertPropsPerRow(stmt.iterator(), fields, new Object[][]{{"E1"}});

        sendEvent(epService, "E4", 99);
        EPAssertionUtil.assertPropsPerRow(stmt.iterator(), fields, new Object[][]{{"E1"}, {"E4"}});
        assertFalse(listener.isInvoked());

        sendEvent(epService, "E5", 6);
        EPAssertionUtil.assertPropsPerRow(stmt.iterator(), fields, new Object[][]{{"E3"}, {"E1"}});
        EPAssertionUtil.assertPropsPerRow(listener.getAndResetLastNewData(), fields, new Object[][]{{"E3"}, {"E1"}});

        stmt.destroy();
    }

    private void tryAssertionVariable(EPServiceProvider epService, EPStatement stmt, SupportUpdateListener listener) {
        String[] fields = "theString".split(",");
        stmt.addListener(listener);
        EPAssertionUtil.assertPropsPerRow(stmt.iterator(), fields, null);

        sendEvent(epService, "E1", 1);
        sendEvent(epService, "E2", 2);
        EPAssertionUtil.assertPropsPerRow(stmt.iterator(), fields, new Object[][]{{"E2"}});

        sendEvent(epService, "E3", 3);
        EPAssertionUtil.assertPropsPerRow(stmt.iterator(), fields, new Object[][]{{"E2"}, {"E3"}});

        sendEvent(epService, "E4", 4);
        EPAssertionUtil.assertPropsPerRow(stmt.iterator(), fields, new Object[][]{{"E2"}, {"E3"}});
        assertFalse(listener.isInvoked());

        sendEvent(epService, "E5", 5);
        EPAssertionUtil.assertPropsPerRow(stmt.iterator(), fields, new Object[][]{{"E2"}, {"E3"}});
        EPAssertionUtil.assertPropsPerRow(listener.getAndResetLastNewData(), fields, new Object[][]{{"E2"}, {"E3"}});

        sendEvent(epService, "E6", 6);
        EPAssertionUtil.assertPropsPerRow(stmt.iterator(), fields, new Object[][]{{"E3"}, {"E4"}});
        assertFalse(listener.isInvoked());

        // change variable values
        epService.getEPRuntime().sendEvent(new SupportBeanNumeric(2, 3));
        sendEvent(epService, "E7", 7);
        EPAssertionUtil.assertPropsPerRow(stmt.iterator(), fields, new Object[][]{{"E6"}, {"E7"}});
        assertFalse(listener.isInvoked());

        epService.getEPRuntime().sendEvent(new SupportBeanNumeric(-1, 0));
        sendEvent(epService, "E8", 8);
        EPAssertionUtil.assertPropsPerRow(stmt.iterator(), fields, new Object[][]{{"E4"}, {"E5"}, {"E6"}, {"E7"}, {"E8"}});
        assertFalse(listener.isInvoked());

        epService.getEPRuntime().sendEvent(new SupportBeanNumeric(10, 0));
        sendEvent(epService, "E9", 9);
        EPAssertionUtil.assertPropsPerRow(stmt.iterator(), fields, new Object[][]{{"E5"}, {"E6"}, {"E7"}, {"E8"}, {"E9"}});
        assertFalse(listener.isInvoked());

        epService.getEPRuntime().sendEvent(new SupportBeanNumeric(6, 3));
        sendEvent(epService, "E10", 10);
        EPAssertionUtil.assertPropsPerRow(stmt.iterator(), fields, new Object[][]{{"E9"}, {"E10"}});
        EPAssertionUtil.assertPropsPerRow(listener.getAndResetLastNewData(), fields, new Object[][]{{"E9"}, {"E10"}});

        epService.getEPRuntime().sendEvent(new SupportBeanNumeric(1, 1));
        EPAssertionUtil.assertPropsPerRow(stmt.iterator(), fields, new Object[][]{{"E7"}});

        epService.getEPRuntime().sendEvent(new SupportBeanNumeric(2, 1));
        EPAssertionUtil.assertPropsPerRow(stmt.iterator(), fields, new Object[][]{{"E7"}, {"E8"}});

        epService.getEPRuntime().sendEvent(new SupportBeanNumeric(1, 2));
        EPAssertionUtil.assertPropsPerRow(stmt.iterator(), fields, new Object[][]{{"E8"}});

        epService.getEPRuntime().sendEvent(new SupportBeanNumeric(6, 6));
        EPAssertionUtil.assertPropsPerRow(stmt.iterator(), fields, null);

        epService.getEPRuntime().sendEvent(new SupportBeanNumeric(1, 4));
        EPAssertionUtil.assertPropsPerRow(stmt.iterator(), fields, new Object[][]{{"E10"}});

        epService.getEPRuntime().sendEvent(new SupportBeanNumeric((Integer) null, null));
        EPAssertionUtil.assertPropsPerRow(stmt.iterator(), fields, new Object[][]{{"E6"}, {"E7"}, {"E8"}, {"E9"}, {"E10"}});

        epService.getEPRuntime().sendEvent(new SupportBeanNumeric(null, 2));
        EPAssertionUtil.assertPropsPerRow(stmt.iterator(), fields, new Object[][]{{"E8"}, {"E9"}, {"E10"}});

        epService.getEPRuntime().sendEvent(new SupportBeanNumeric(2, null));
        EPAssertionUtil.assertPropsPerRow(stmt.iterator(), fields, new Object[][]{{"E6"}, {"E7"}});

        epService.getEPRuntime().sendEvent(new SupportBeanNumeric(-1, 4));
        EPAssertionUtil.assertPropsPerRow(stmt.iterator(), fields, new Object[][]{{"E10"}});

        epService.getEPRuntime().sendEvent(new SupportBeanNumeric(-1, 0));
        EPAssertionUtil.assertPropsPerRow(stmt.iterator(), fields, new Object[][]{{"E6"}, {"E7"}, {"E8"}, {"E9"}, {"E10"}});

        epService.getEPRuntime().sendEvent(new SupportBeanNumeric(0, 0));
        EPAssertionUtil.assertPropsPerRow(stmt.iterator(), fields, null);
    }

    private void runAssertionBatchOffsetNoOrderOM(EPServiceProvider epService) {
        EPStatementObjectModel model = new EPStatementObjectModel();
        model.setSelectClause(SelectClause.createWildcard());
        model.getSelectClause().setStreamSelector(StreamSelector.RSTREAM_ISTREAM_BOTH);
        model.setFromClause(FromClause.create(FilterStream.create("SupportBean").addView("length_batch", Expressions.constant(3))));
        model.setRowLimitClause(RowLimitClause.create(1));

        String statementString = "select irstream * from SupportBean#length_batch(3) limit 1";
        assertEquals(statementString, model.toEPL());
        EPStatement stmt = epService.getEPAdministrator().create(model);
        SupportUpdateListener listener = new SupportUpdateListener();
        tryAssertion(epService, stmt, listener);
        stmt.destroy();
        listener.reset();

        model = epService.getEPAdministrator().compileEPL(statementString);
        assertEquals(statementString, model.toEPL());
        stmt = epService.getEPAdministrator().create(model);
        tryAssertion(epService, stmt, listener);
        stmt.destroy();
    }

    private void runAssertionFullyGroupedOrdered(EPServiceProvider epService) {
        String statementString = "select theString, sum(intPrimitive) as mysum from SupportBean#length(5) group by theString order by sum(intPrimitive) limit 2";
        EPStatement stmt = epService.getEPAdministrator().createEPL(statementString);
        SupportUpdateListener listener = new SupportUpdateListener();

        String[] fields = "theString,mysum".split(",");
        stmt.addListener(listener);
        EPAssertionUtil.assertPropsPerRow(stmt.iterator(), fields, null);

        sendEvent(epService, "E1", 90);
        EPAssertionUtil.assertPropsPerRow(stmt.iterator(), fields, new Object[][]{{"E1", 90}});

        sendEvent(epService, "E2", 5);
        EPAssertionUtil.assertPropsPerRow(stmt.iterator(), fields, new Object[][]{{"E2", 5}, {"E1", 90}});

        sendEvent(epService, "E3", 60);
        EPAssertionUtil.assertPropsPerRow(stmt.iterator(), fields, new Object[][]{{"E2", 5}, {"E3", 60}});

        sendEvent(epService, "E3", 40);
        EPAssertionUtil.assertPropsPerRow(stmt.iterator(), fields, new Object[][]{{"E2", 5}, {"E1", 90}});

        sendEvent(epService, "E2", 1000);
        EPAssertionUtil.assertPropsPerRow(stmt.iterator(), fields, new Object[][]{{"E1", 90}, {"E3", 100}});
    }

    private void runAssertionEventPerRowUnGrouped(EPServiceProvider epService) {
        sendTimer(epService, 1000);
        String statementString = "select theString, sum(intPrimitive) as mysum from SupportBean#length(5) output every 10 seconds order by theString desc limit 2";
        EPStatement stmt = epService.getEPAdministrator().createEPL(statementString);
        SupportUpdateListener listener = new SupportUpdateListener();

        String[] fields = "theString,mysum".split(",");
        stmt.addListener(listener);
        EPAssertionUtil.assertPropsPerRow(stmt.iterator(), fields, null);

        sendEvent(epService, "E1", 10);
        sendEvent(epService, "E2", 5);
        sendEvent(epService, "E3", 20);
        sendEvent(epService, "E4", 30);

        sendTimer(epService, 11000);
        EPAssertionUtil.assertPropsPerRow(listener.getLastNewData(), fields, new Object[][]{{"E4", 65}, {"E3", 35}});

        stmt.destroy();
    }

    private void runAssertionGroupedSnapshot(EPServiceProvider epService) {
        sendTimer(epService, 1000);
        String statementString = "select theString, sum(intPrimitive) as mysum from SupportBean#length(5) group by theString output snapshot every 10 seconds order by sum(intPrimitive) desc limit 2";
        EPStatement stmt = epService.getEPAdministrator().createEPL(statementString);
        SupportUpdateListener listener = new SupportUpdateListener();

        String[] fields = "theString,mysum".split(",");
        stmt.addListener(listener);
        EPAssertionUtil.assertPropsPerRow(stmt.iterator(), fields, null);

        sendEvent(epService, "E1", 10);
        sendEvent(epService, "E2", 5);
        sendEvent(epService, "E3", 20);
        sendEvent(epService, "E1", 30);

        sendTimer(epService, 11000);
        EPAssertionUtil.assertPropsPerRow(listener.getLastNewData(), fields, new Object[][]{{"E1", 40}, {"E3", 20}});

        stmt.destroy();
    }

    private void runAssertionGroupedSnapshotNegativeRowcount(EPServiceProvider epService) {
        sendTimer(epService, 1000);
        String statementString = "select theString, sum(intPrimitive) as mysum from SupportBean#length(5) group by theString output snapshot every 10 seconds order by sum(intPrimitive) desc limit -1 offset 1";
        EPStatement stmt = epService.getEPAdministrator().createEPL(statementString);
        SupportUpdateListener listener = new SupportUpdateListener();

        String[] fields = "theString,mysum".split(",");
        stmt.addListener(listener);
        EPAssertionUtil.assertPropsPerRow(stmt.iterator(), fields, null);

        sendEvent(epService, "E1", 10);
        sendEvent(epService, "E2", 5);
        sendEvent(epService, "E3", 20);
        sendEvent(epService, "E1", 30);

        sendTimer(epService, 11000);
        EPAssertionUtil.assertPropsPerRow(listener.getLastNewData(), fields, new Object[][]{{"E3", 20}, {"E2", 5}});

        stmt.destroy();
    }

    private void runAssertionInvalid(EPServiceProvider epService) {
        epService.getEPAdministrator().createEPL("create variable string myrows = 'abc'");
        tryInvalid(epService, "select * from SupportBean limit myrows",
                "Error starting statement: Limit clause requires a variable of numeric type [select * from SupportBean limit myrows]");
        tryInvalid(epService, "select * from SupportBean limit 1, myrows",
                "Error starting statement: Limit clause requires a variable of numeric type [select * from SupportBean limit 1, myrows]");
        tryInvalid(epService, "select * from SupportBean limit dummy",
                "Error starting statement: Limit clause variable by name 'dummy' has not been declared [select * from SupportBean limit dummy]");
        tryInvalid(epService, "select * from SupportBean limit 1,dummy",
                "Error starting statement: Limit clause variable by name 'dummy' has not been declared [select * from SupportBean limit 1,dummy]");
    }

    private void sendTimer(EPServiceProvider epService, long timeInMSec) {
        CurrentTimeEvent theEvent = new CurrentTimeEvent(timeInMSec);
        EPRuntime runtime = epService.getEPRuntime();
        runtime.sendEvent(theEvent);
    }

    private void tryAssertion(EPServiceProvider epService, EPStatement stmt, SupportUpdateListener listener) {
        String[] fields = "theString".split(",");
        stmt.addListener(listener);
        sendEvent(epService, "E1", 1);
        EPAssertionUtil.assertPropsPerRow(stmt.iterator(), fields, new Object[][]{{"E1"}});

        sendEvent(epService, "E2", 2);
        assertFalse(listener.isInvoked());
        EPAssertionUtil.assertPropsPerRow(stmt.iterator(), fields, new Object[][]{{"E1"}});

        sendEvent(epService, "E3", 3);
        EPAssertionUtil.assertPropsPerRow(listener.getAndResetLastNewData(), fields, new Object[][]{{"E1"}});
        EPAssertionUtil.assertPropsPerRow(stmt.iterator(), fields, null);

        sendEvent(epService, "E4", 4);
        EPAssertionUtil.assertPropsPerRow(stmt.iterator(), fields, new Object[][]{{"E4"}});

        sendEvent(epService, "E5", 5);
        EPAssertionUtil.assertPropsPerRow(stmt.iterator(), fields, new Object[][]{{"E4"}});

        sendEvent(epService, "E6", 6);
        EPAssertionUtil.assertPropsPerRow(listener.getLastNewData(), fields, new Object[][]{{"E4"}});
        EPAssertionUtil.assertPropsPerRow(listener.getLastOldData(), fields, new Object[][]{{"E1"}});
        EPAssertionUtil.assertPropsPerRow(stmt.iterator(), fields, null);
    }

    private void sendEvent(EPServiceProvider epService, String theString, int intPrimitive) {
        epService.getEPRuntime().sendEvent(new SupportBean(theString, intPrimitive));
    }

    private void sendSBSequenceAndAssert(EPServiceProvider epService, SupportUpdateListener listener, String expected, String[] theStrings) {
        epService.getEPRuntime().sendEvent(new SupportBean_S0(0));
        for (String theString : theStrings) {
            sendEvent(epService, theString, 0);
        }
        epService.getEPRuntime().sendEvent(new SupportBean_S1(0));
        EPAssertionUtil.assertProps(listener.assertOneGetNewAndReset(), "theString".split(","), new Object[]{expected});
    }

    private void sendSBSequenceAndAssert(EPServiceProvider epService, SupportUpdateListener listener, String expectedString, int expectedInt, Object[][] rows) {
        epService.getEPRuntime().sendEvent(new SupportBean_S0(0));
        for (Object[] row : rows) {
            sendEvent(epService, row[0].toString(), (Integer) row[1]);
        }
        epService.getEPRuntime().sendEvent(new SupportBean_S1(0));
        EPAssertionUtil.assertProps(listener.assertOneGetNewAndReset(), "theString,intPrimitive".split(","), new Object[]{expectedString, expectedInt});
    }
}
