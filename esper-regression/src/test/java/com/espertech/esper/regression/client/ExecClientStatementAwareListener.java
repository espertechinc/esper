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
package com.espertech.esper.regression.client;

import com.espertech.esper.client.*;
import com.espertech.esper.client.scopetest.EPAssertionUtil;
import com.espertech.esper.client.scopetest.SupportUpdateListener;
import com.espertech.esper.supportregression.bean.SupportBean;
import com.espertech.esper.supportregression.execution.RegressionExecution;
import com.espertech.esper.supportregression.util.SupportStmtAwareUpdateListener;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import static junit.framework.TestCase.*;
import static org.junit.Assert.assertEquals;

public class ExecClientStatementAwareListener implements RegressionExecution {
    public void configure(Configuration configuration) throws Exception {
        configuration.addEventType("Bean", SupportBean.class.getName());
    }

    public void run(EPServiceProvider epService) throws Exception {
        runAssertionStmtAware(epService);
        runAssertionInvalid(epService);
        runAssertionBothListeners(epService);
        runAssertionUseOnMultipleStmts(epService);
        runAssertionOrderOfInvocation(epService);
    }

    private void runAssertionStmtAware(EPServiceProvider epService) {
        String stmtText = "select * from Bean";
        EPStatement statement = epService.getEPAdministrator().createEPL(stmtText);
        SupportStmtAwareUpdateListener listener = new SupportStmtAwareUpdateListener();
        statement.addListener(listener);

        epService.getEPRuntime().sendEvent(new SupportBean());
        assertTrue(listener.isInvoked());
        assertEquals(1, listener.getStatementList().size());
        assertEquals(statement, listener.getStatementList().get(0));
        assertEquals(1, listener.getSvcProviderList().size());
        assertEquals(epService, listener.getSvcProviderList().get(0));

        statement.destroy();
    }

    private void runAssertionInvalid(EPServiceProvider epService) {
        String stmtText = "select * from Bean";
        EPStatement statement = epService.getEPAdministrator().createEPL(stmtText);
        try {
            statement.addListener((StatementAwareUpdateListener) null);
            fail();
        } catch (IllegalArgumentException ex) {
            // expected
        }

        statement.destroy();
    }

    private void runAssertionBothListeners(EPServiceProvider epService) {
        String stmtText = "select * from Bean";
        EPStatement statement = epService.getEPAdministrator().createEPL(stmtText);

        SupportStmtAwareUpdateListener[] awareListeners = new SupportStmtAwareUpdateListener[3];
        SupportUpdateListener[] updateListeners = new SupportUpdateListener[awareListeners.length];
        for (int i = 0; i < awareListeners.length; i++) {
            awareListeners[i] = new SupportStmtAwareUpdateListener();
            statement.addListener(awareListeners[i]);
            updateListeners[i] = new SupportUpdateListener();
            statement.addListener(updateListeners[i]);
        }

        Object theEvent = new SupportBean();
        epService.getEPRuntime().sendEvent(theEvent);
        for (int i = 0; i < awareListeners.length; i++) {
            assertSame(theEvent, updateListeners[i].assertOneGetNewAndReset().getUnderlying());
            assertSame(theEvent, awareListeners[i].assertOneGetNewAndReset().getUnderlying());
        }

        statement.removeListener(awareListeners[1]);
        theEvent = new SupportBean();
        epService.getEPRuntime().sendEvent(theEvent);
        for (int i = 0; i < awareListeners.length; i++) {
            if (i == 1) {
                assertSame(theEvent, updateListeners[i].assertOneGetNewAndReset().getUnderlying());
                assertFalse(awareListeners[i].isInvoked());
            } else {
                assertSame(theEvent, updateListeners[i].assertOneGetNewAndReset().getUnderlying());
                assertSame(theEvent, awareListeners[i].assertOneGetNewAndReset().getUnderlying());
            }
        }

        statement.removeListener(updateListeners[1]);
        theEvent = new SupportBean();
        epService.getEPRuntime().sendEvent(theEvent);
        for (int i = 0; i < awareListeners.length; i++) {
            if (i == 1) {
                assertFalse(updateListeners[i].isInvoked());
                assertFalse(awareListeners[i].isInvoked());
            } else {
                assertSame(theEvent, updateListeners[i].assertOneGetNewAndReset().getUnderlying());
                assertSame(theEvent, awareListeners[i].assertOneGetNewAndReset().getUnderlying());
            }
        }

        statement.addListener(updateListeners[1]);
        statement.addListener(awareListeners[1]);
        theEvent = new SupportBean();
        epService.getEPRuntime().sendEvent(theEvent);
        for (int i = 0; i < awareListeners.length; i++) {
            assertSame(theEvent, updateListeners[i].assertOneGetNewAndReset().getUnderlying());
            assertSame(theEvent, awareListeners[i].assertOneGetNewAndReset().getUnderlying());
        }

        statement.removeAllListeners();
        for (int i = 0; i < awareListeners.length; i++) {
            assertFalse(updateListeners[i].isInvoked());
            assertFalse(awareListeners[i].isInvoked());
        }

        epService.getEPAdministrator().destroyAllStatements();
    }

    private void runAssertionUseOnMultipleStmts(EPServiceProvider epService) {
        EPStatement statementOne = epService.getEPAdministrator().createEPL("select * from Bean(theString='A' or theString='C')");
        EPStatement statementTwo = epService.getEPAdministrator().createEPL("select * from Bean(theString='B' or theString='C')");

        SupportStmtAwareUpdateListener awareListener = new SupportStmtAwareUpdateListener();
        statementOne.addListener(awareListener);
        statementTwo.addListener(awareListener);

        epService.getEPRuntime().sendEvent(new SupportBean("B", 1));
        assertEquals("B", awareListener.assertOneGetNewAndReset().get("theString"));

        epService.getEPRuntime().sendEvent(new SupportBean("A", 1));
        assertEquals("A", awareListener.assertOneGetNewAndReset().get("theString"));

        epService.getEPRuntime().sendEvent(new SupportBean("C", 1));
        assertEquals(2, awareListener.getNewDataList().size());
        assertEquals("C", awareListener.getNewDataList().get(0)[0].get("theString"));
        assertEquals("C", awareListener.getNewDataList().get(1)[0].get("theString"));
        EPStatement[] stmts = awareListener.getStatementList().toArray(new EPStatement[0]);
        EPAssertionUtil.assertEqualsAnyOrder(stmts, new Object[]{statementOne, statementTwo});

        epService.getEPAdministrator().destroyAllStatements();
    }

    private void runAssertionOrderOfInvocation(EPServiceProvider epService) {
        String stmtText = "select * from Bean";
        EPStatement statement = epService.getEPAdministrator().createEPL(stmtText);

        MyStmtAwareUpdateListener[] awareListeners = new MyStmtAwareUpdateListener[2];
        MyUpdateListener[] updateListeners = new MyUpdateListener[awareListeners.length];
        List<Object> invoked = new ArrayList<>();
        for (int i = 0; i < awareListeners.length; i++) {
            awareListeners[i] = new MyStmtAwareUpdateListener(invoked);
            updateListeners[i] = new MyUpdateListener(invoked);
        }

        statement.addListener(awareListeners[0]);
        statement.addListener(updateListeners[1]);
        statement.addListener(updateListeners[0]);
        statement.addListener(awareListeners[1]);

        epService.getEPRuntime().sendEvent(new SupportBean());

        assertEquals(updateListeners[1], invoked.get(0));
        assertEquals(updateListeners[0], invoked.get(1));
        assertEquals(awareListeners[0], invoked.get(2));
        assertEquals(awareListeners[1], invoked.get(3));

        try {
            Iterator<UpdateListener> itOne = statement.getUpdateListeners();
            itOne.next();
            itOne.remove();
            fail();
        } catch (UnsupportedOperationException ex) {
            // expected
        }

        try {
            Iterator<StatementAwareUpdateListener> itOne = statement.getStatementAwareListeners();
            itOne.next();
            itOne.remove();
            fail();
        } catch (UnsupportedOperationException ex) {
            // expected
        }

        Iterator<UpdateListener> itOne = statement.getUpdateListeners();
        assertEquals(updateListeners[1], itOne.next());
        assertEquals(updateListeners[0], itOne.next());
        assertFalse(itOne.hasNext());

        Iterator<StatementAwareUpdateListener> itTwo = statement.getStatementAwareListeners();
        assertEquals(awareListeners[0], itTwo.next());
        assertEquals(awareListeners[1], itTwo.next());
        assertFalse(itTwo.hasNext());

        statement.removeAllListeners();
        assertFalse(statement.getStatementAwareListeners().hasNext());
        assertFalse(statement.getUpdateListeners().hasNext());

        epService.getEPAdministrator().destroyAllStatements();
    }

    public class MyUpdateListener implements UpdateListener {
        private List<Object> invoked;

        MyUpdateListener(List<Object> invoked) {
            this.invoked = invoked;
        }

        public void update(EventBean[] newEvents, EventBean[] oldEvents) {
            invoked.add(this);
        }
    }

    public class MyStmtAwareUpdateListener implements StatementAwareUpdateListener {
        private List<Object> invoked;

        MyStmtAwareUpdateListener(List<Object> invoked) {
            this.invoked = invoked;
        }

        public void update(EventBean[] newEvents, EventBean[] oldEvents, EPStatement statement, EPServiceProvider epServiceProvider) {
            invoked.add(this);
        }
    }
}
