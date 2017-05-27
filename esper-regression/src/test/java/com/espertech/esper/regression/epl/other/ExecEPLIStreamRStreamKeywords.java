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
package com.espertech.esper.regression.epl.other;

import com.espertech.esper.client.EPServiceProvider;
import com.espertech.esper.client.EPStatement;
import com.espertech.esper.client.scopetest.SupportUpdateListener;
import com.espertech.esper.client.soda.*;
import com.espertech.esper.supportregression.bean.SupportBean;
import com.espertech.esper.supportregression.execution.RegressionExecution;
import com.espertech.esper.util.SerializableObjectCopier;

import static junit.framework.TestCase.*;
import static org.junit.Assert.assertEquals;

public class ExecEPLIStreamRStreamKeywords implements RegressionExecution {
    public void run(EPServiceProvider epService) throws Exception {
        runAssertionRStreamOnly_OM(epService);
        runAssertionRStreamOnly_Compile(epService);
        runAssertionRStreamOnly(epService);
        runAssertionRStreamInsertInto(epService);
        runAssertionRStreamInsertIntoRStream(epService);
        runAssertionRStreamJoin(epService);
        runAssertionIStreamOnly(epService);
        runAssertionIStreamInsertIntoRStream(epService);
        runAssertionIStreamJoin(epService);
    }

    private void runAssertionRStreamOnly_OM(EPServiceProvider epService) throws Exception {
        String stmtText = "select rstream * from " + SupportBean.class.getName() + "#length(3)";
        EPStatementObjectModel model = new EPStatementObjectModel();
        model.setSelectClause(SelectClause.createWildcard(StreamSelector.RSTREAM_ONLY));
        FromClause fromClause = FromClause.create(FilterStream.create(SupportBean.class.getName()).addView(View.create("length", Expressions.constant(3))));
        model.setFromClause(fromClause);
        model = (EPStatementObjectModel) SerializableObjectCopier.copy(model);

        assertEquals(stmtText, model.toEPL());
        EPStatement statement = epService.getEPAdministrator().create(model);
        SupportUpdateListener testListener = new SupportUpdateListener();
        statement.addListener(testListener);

        Object theEvent = sendEvent(epService, "a", 2);
        assertFalse(testListener.isInvoked());

        sendEvents(epService, new String[]{"a", "b"});
        assertFalse(testListener.isInvoked());

        sendEvent(epService, "d", 2);
        assertSame(theEvent, testListener.getLastNewData()[0].getUnderlying());    // receive 'a' as new data
        assertNull(testListener.getLastOldData());  // receive no more old data

        statement.destroy();
    }

    private void runAssertionRStreamOnly_Compile(EPServiceProvider epService) throws Exception {
        String stmtText = "select rstream * from " + SupportBean.class.getName() + "#length(3)";
        EPStatementObjectModel model = epService.getEPAdministrator().compileEPL(stmtText);
        model = (EPStatementObjectModel) SerializableObjectCopier.copy(model);

        assertEquals(stmtText, model.toEPL());
        EPStatement statement = epService.getEPAdministrator().create(model);
        SupportUpdateListener testListener = new SupportUpdateListener();
        statement.addListener(testListener);

        Object theEvent = sendEvent(epService, "a", 2);
        assertFalse(testListener.isInvoked());

        sendEvents(epService, new String[]{"a", "b"});
        assertFalse(testListener.isInvoked());

        sendEvent(epService, "d", 2);
        assertSame(theEvent, testListener.getLastNewData()[0].getUnderlying());    // receive 'a' as new data
        assertNull(testListener.getLastOldData());  // receive no more old data

        statement.destroy();
    }

    private void runAssertionRStreamOnly(EPServiceProvider epService) {
        EPStatement statement = epService.getEPAdministrator().createEPL(
                "select rstream * from " + SupportBean.class.getName() + "#length(3)");
        SupportUpdateListener testListener = new SupportUpdateListener();
        statement.addListener(testListener);

        Object theEvent = sendEvent(epService, "a", 2);
        assertFalse(testListener.isInvoked());

        sendEvents(epService, new String[]{"a", "b"});
        assertFalse(testListener.isInvoked());

        sendEvent(epService, "d", 2);
        assertSame(theEvent, testListener.getLastNewData()[0].getUnderlying());    // receive 'a' as new data
        assertNull(testListener.getLastOldData());  // receive no more old data

        statement.destroy();
    }

    private void runAssertionRStreamInsertInto(EPServiceProvider epService) {
        EPStatement statement = epService.getEPAdministrator().createEPL(
                "insert into NextStream " +
                        "select rstream s0.theString as theString from " + SupportBean.class.getName() + "#length(3) as s0");
        SupportUpdateListener testListener = new SupportUpdateListener();
        statement.addListener(testListener);

        statement = epService.getEPAdministrator().createEPL("select * from NextStream");
        SupportUpdateListener testListenerInsertInto = new SupportUpdateListener();
        statement.addListener(testListenerInsertInto);

        sendEvent(epService, "a", 2);
        assertFalse(testListener.isInvoked());
        assertEquals("a", testListenerInsertInto.assertOneGetNewAndReset().get("theString"));    // insert into unchanged

        sendEvents(epService, new String[]{"b", "c"});
        assertFalse(testListener.isInvoked());
        assertEquals(2, testListenerInsertInto.getNewDataList().size());    // insert into unchanged
        testListenerInsertInto.reset();

        sendEvent(epService, "d", 2);
        assertSame("a", testListener.getLastNewData()[0].get("theString"));    // receive 'a' as new data
        assertNull(testListener.getLastOldData());  // receive no more old data
        assertEquals("d", testListenerInsertInto.getLastNewData()[0].get("theString"));    // insert into unchanged
        assertNull(testListenerInsertInto.getLastOldData());  // receive no old data in insert into

        epService.getEPAdministrator().destroyAllStatements();
    }

    private void runAssertionRStreamInsertIntoRStream(EPServiceProvider epService) {
        EPStatement statement = epService.getEPAdministrator().createEPL(
                "insert rstream into NextStream " +
                        "select rstream s0.theString as theString from " + SupportBean.class.getName() + "#length(3) as s0");
        SupportUpdateListener testListener = new SupportUpdateListener();
        statement.addListener(testListener);

        statement = epService.getEPAdministrator().createEPL("select * from NextStream");
        SupportUpdateListener testListenerInsertInto = new SupportUpdateListener();
        statement.addListener(testListenerInsertInto);

        sendEvent(epService, "a", 2);
        assertFalse(testListener.isInvoked());
        assertFalse(testListenerInsertInto.isInvoked());

        sendEvents(epService, new String[]{"b", "c"});
        assertFalse(testListener.isInvoked());
        assertFalse(testListenerInsertInto.isInvoked());

        sendEvent(epService, "d", 2);
        assertSame("a", testListener.getLastNewData()[0].get("theString"));    // receive 'a' as new data
        assertNull(testListener.getLastOldData());  // receive no more old data
        assertEquals("a", testListenerInsertInto.getLastNewData()[0].get("theString"));    // insert into unchanged
        assertNull(testListener.getLastOldData());  // receive no old data in insert into

        epService.getEPAdministrator().destroyAllStatements();
    }

    private void runAssertionRStreamJoin(EPServiceProvider epService) {
        EPStatement statement = epService.getEPAdministrator().createEPL(
                "select rstream s1.intPrimitive as aID, s2.intPrimitive as bID " +
                        "from " + SupportBean.class.getName() + "(theString='a')#length(2) as s1, "
                        + SupportBean.class.getName() + "(theString='b')#keepall as s2" +
                        " where s1.intPrimitive = s2.intPrimitive");
        SupportUpdateListener testListener = new SupportUpdateListener();
        statement.addListener(testListener);

        sendEvent(epService, "a", 1);
        sendEvent(epService, "b", 1);
        assertFalse(testListener.isInvoked());

        sendEvent(epService, "a", 2);
        assertFalse(testListener.isInvoked());

        sendEvent(epService, "a", 3);
        assertEquals(1, testListener.getLastNewData()[0].get("aID"));    // receive 'a' as new data
        assertEquals(1, testListener.getLastNewData()[0].get("bID"));
        assertNull(testListener.getLastOldData());  // receive no more old data

        statement.destroy();
    }

    private void runAssertionIStreamOnly(EPServiceProvider epService) {
        EPStatement statement = epService.getEPAdministrator().createEPL(
                "select istream * from " + SupportBean.class.getName() + "#length(1)");
        SupportUpdateListener testListener = new SupportUpdateListener();
        statement.addListener(testListener);

        Object theEvent = sendEvent(epService, "a", 2);
        assertSame(theEvent, testListener.assertOneGetNewAndReset().getUnderlying());

        theEvent = sendEvent(epService, "b", 2);
        assertSame(theEvent, testListener.getLastNewData()[0].getUnderlying());
        assertNull(testListener.getLastOldData()); // receive no old data, just istream events

        statement.destroy();
    }

    private void runAssertionIStreamInsertIntoRStream(EPServiceProvider epService) {
        EPStatement statement = epService.getEPAdministrator().createEPL(
                "insert rstream into NextStream " +
                        "select istream a.theString as theString from " + SupportBean.class.getName() + "#length(1) as a");
        SupportUpdateListener testListener = new SupportUpdateListener();
        statement.addListener(testListener);

        statement = epService.getEPAdministrator().createEPL("select * from NextStream");
        SupportUpdateListener testListenerInsertInto = new SupportUpdateListener();
        statement.addListener(testListenerInsertInto);

        sendEvent(epService, "a", 2);
        assertEquals("a", testListener.assertOneGetNewAndReset().get("theString"));
        assertFalse(testListenerInsertInto.isInvoked());

        sendEvent(epService, "b", 2);
        assertEquals("b", testListener.getLastNewData()[0].get("theString"));
        assertNull(testListener.getLastOldData());
        assertEquals("a", testListenerInsertInto.getLastNewData()[0].get("theString"));
        assertNull(testListenerInsertInto.getLastOldData());

        epService.getEPAdministrator().destroyAllStatements();
    }

    private void runAssertionIStreamJoin(EPServiceProvider epService) {
        EPStatement statement = epService.getEPAdministrator().createEPL(
                "select istream s1.intPrimitive as aID, s2.intPrimitive as bID " +
                        "from " + SupportBean.class.getName() + "(theString='a')#length(2) as s1, "
                        + SupportBean.class.getName() + "(theString='b')#keepall as s2" +
                        " where s1.intPrimitive = s2.intPrimitive");
        SupportUpdateListener testListener = new SupportUpdateListener();
        statement.addListener(testListener);

        sendEvent(epService, "a", 1);
        sendEvent(epService, "b", 1);
        assertEquals(1, testListener.getLastNewData()[0].get("aID"));    // receive 'a' as new data
        assertEquals(1, testListener.getLastNewData()[0].get("bID"));
        assertNull(testListener.getLastOldData());  // receive no more old data
        testListener.reset();

        sendEvent(epService, "a", 2);
        assertFalse(testListener.isInvoked());

        sendEvent(epService, "a", 3);
        assertFalse(testListener.isInvoked());

        statement.destroy();
    }

    private void sendEvents(EPServiceProvider epService, String[] stringValue) {
        for (int i = 0; i < stringValue.length; i++) {
            sendEvent(epService, stringValue[i], 2);
        }
    }

    private Object sendEvent(EPServiceProvider epService, String stringValue, int intPrimitive) {
        SupportBean theEvent = new SupportBean();
        theEvent.setTheString(stringValue);
        theEvent.setIntPrimitive(intPrimitive);
        epService.getEPRuntime().sendEvent(theEvent);
        return theEvent;
    }
}
