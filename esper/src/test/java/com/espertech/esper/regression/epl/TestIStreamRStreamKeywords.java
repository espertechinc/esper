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

package com.espertech.esper.regression.epl;

import com.espertech.esper.client.EPServiceProvider;
import com.espertech.esper.client.EPServiceProviderManager;
import com.espertech.esper.client.EPStatement;
import com.espertech.esper.client.Configuration;
import com.espertech.esper.client.scopetest.SupportUpdateListener;
import com.espertech.esper.client.soda.*;
import com.espertech.esper.metrics.instrumentation.InstrumentationHelper;
import com.espertech.esper.support.bean.SupportBean;
import com.espertech.esper.support.client.SupportConfigFactory;
import com.espertech.esper.util.SerializableObjectCopier;
import junit.framework.TestCase;

public class TestIStreamRStreamKeywords extends TestCase
{
    private EPServiceProvider epService;
    private SupportUpdateListener testListener;
    private SupportUpdateListener testListenerInsertInto;

    public void setUp()
    {
        testListener = new SupportUpdateListener();
        testListenerInsertInto = new SupportUpdateListener();
    }

    protected void tearDown() throws Exception {
        if (InstrumentationHelper.ENABLED) { InstrumentationHelper.endTest();}
        testListener = null;
        testListenerInsertInto = null;
    }

    public void testChangeEngineDefaultRStream()
    {
        Configuration config = SupportConfigFactory.getConfiguration();
        config.getEngineDefaults().getStreamSelection().setDefaultStreamSelector(StreamSelector.RSTREAM_ONLY);
        epService = EPServiceProviderManager.getDefaultProvider(config);
        epService.initialize();
        if (InstrumentationHelper.ENABLED) { InstrumentationHelper.startTest(epService, this.getClass(), getName());}

        String stmtText = "select * from " + SupportBean.class.getName() + "#length(3)";
        EPStatement statement = epService.getEPAdministrator().createEPL(stmtText);
        statement.addListener(testListener);

        Object theEvent = sendEvent("a");
        sendEvent("b");
        sendEvent("c");
        assertFalse(testListener.isInvoked());

        sendEvent("d");
        assertTrue(testListener.isInvoked());
        assertSame(theEvent, testListener.getLastNewData()[0].getUnderlying());    // receive 'a' as new data
        assertNull(testListener.getLastOldData());  // receive no more old data
    }

    public void testChangeEngineDefaultIRStream()
    {
        Configuration config = SupportConfigFactory.getConfiguration();
        config.getEngineDefaults().getStreamSelection().setDefaultStreamSelector(StreamSelector.RSTREAM_ISTREAM_BOTH);
        epService = EPServiceProviderManager.getDefaultProvider(config);
        epService.initialize();
        if (InstrumentationHelper.ENABLED) { InstrumentationHelper.startTest(epService, this.getClass(), getName());}

        String stmtText = "select * from " + SupportBean.class.getName() + "#length(3)";
        EPStatement statement = epService.getEPAdministrator().createEPL(stmtText);
        statement.addListener(testListener);

        Object eventOld = sendEvent("a");
        sendEvent("b");
        sendEvent("c");
        testListener.reset();

        Object eventNew = sendEvent("d");
        assertTrue(testListener.isInvoked());
        assertSame(eventNew, testListener.getLastNewData()[0].getUnderlying());    // receive 'a' as new data
        assertSame(eventOld, testListener.getLastOldData()[0].getUnderlying());    // receive 'a' as new data
    }

    public void testRStreamOnly_OM() throws Exception
    {
        epService = EPServiceProviderManager.getDefaultProvider(SupportConfigFactory.getConfiguration());
        epService.initialize();
        if (InstrumentationHelper.ENABLED) { InstrumentationHelper.startTest(epService, this.getClass(), getName());}

        String stmtText = "select rstream * from " + SupportBean.class.getName() + "#length(3)";
        EPStatementObjectModel model = new EPStatementObjectModel();
        model.setSelectClause(SelectClause.createWildcard(StreamSelector.RSTREAM_ONLY));
        FromClause fromClause = FromClause.create(FilterStream.create(SupportBean.class.getName()).addView(View.create("length", Expressions.constant(3))));
        model.setFromClause(fromClause);
        model = (EPStatementObjectModel) SerializableObjectCopier.copy(model);

        assertEquals(stmtText, model.toEPL());
        EPStatement statement = epService.getEPAdministrator().create(model);
        statement.addListener(testListener);

        Object theEvent = sendEvent("a");
        assertFalse(testListener.isInvoked());

        sendEvents(new String[] {"a", "b"});
        assertFalse(testListener.isInvoked());

        sendEvent("d");
        assertSame(theEvent, testListener.getLastNewData()[0].getUnderlying());    // receive 'a' as new data
        assertNull(testListener.getLastOldData());  // receive no more old data
        testListener.reset();
    }

    public void testRStreamOnly_Compile() throws Exception
    {
        epService = EPServiceProviderManager.getDefaultProvider(SupportConfigFactory.getConfiguration());
        epService.initialize();
        if (InstrumentationHelper.ENABLED) { InstrumentationHelper.startTest(epService, this.getClass(), getName());}

        String stmtText = "select rstream * from " + SupportBean.class.getName() + "#length(3)";
        EPStatementObjectModel model = epService.getEPAdministrator().compileEPL(stmtText);
        model = (EPStatementObjectModel) SerializableObjectCopier.copy(model);

        assertEquals(stmtText, model.toEPL());
        EPStatement statement = epService.getEPAdministrator().create(model);
        statement.addListener(testListener);

        Object theEvent = sendEvent("a");
        assertFalse(testListener.isInvoked());

        sendEvents(new String[] {"a", "b"});
        assertFalse(testListener.isInvoked());

        sendEvent("d");
        assertSame(theEvent, testListener.getLastNewData()[0].getUnderlying());    // receive 'a' as new data
        assertNull(testListener.getLastOldData());  // receive no more old data
        testListener.reset();
    }

    public void testRStreamOnly()
    {
        epService = EPServiceProviderManager.getDefaultProvider(SupportConfigFactory.getConfiguration());
        epService.initialize();
        if (InstrumentationHelper.ENABLED) { InstrumentationHelper.startTest(epService, this.getClass(), getName());}

        EPStatement statement = epService.getEPAdministrator().createEPL(
                "select rstream * from " + SupportBean.class.getName() + "#length(3)");
        statement.addListener(testListener);

        Object theEvent = sendEvent("a");
        assertFalse(testListener.isInvoked());

        sendEvents(new String[] {"a", "b"});
        assertFalse(testListener.isInvoked());

        sendEvent("d");
        assertSame(theEvent, testListener.getLastNewData()[0].getUnderlying());    // receive 'a' as new data
        assertNull(testListener.getLastOldData());  // receive no more old data
        testListener.reset();
    }

    public void testRStreamInsertInto()
    {
        epService = EPServiceProviderManager.getDefaultProvider(SupportConfigFactory.getConfiguration());
        epService.initialize();
        if (InstrumentationHelper.ENABLED) { InstrumentationHelper.startTest(epService, this.getClass(), getName());}

        EPStatement statement = epService.getEPAdministrator().createEPL(
                "insert into NextStream " +
                "select rstream s0.theString as theString from " + SupportBean.class.getName() + "#length(3) as s0");
        statement.addListener(testListener);

        statement = epService.getEPAdministrator().createEPL("select * from NextStream");
        statement.addListener(testListenerInsertInto);

        sendEvent("a");
        assertFalse(testListener.isInvoked());
        assertEquals("a", testListenerInsertInto.assertOneGetNewAndReset().get("theString"));    // insert into unchanged

        sendEvents(new String[] {"b", "c"});
        assertFalse(testListener.isInvoked());
        assertEquals(2, testListenerInsertInto.getNewDataList().size());    // insert into unchanged
        testListenerInsertInto.reset();

        sendEvent("d");
        assertSame("a", testListener.getLastNewData()[0].get("theString"));    // receive 'a' as new data
        assertNull(testListener.getLastOldData());  // receive no more old data
        assertEquals("d", testListenerInsertInto.getLastNewData()[0].get("theString"));    // insert into unchanged
        assertNull(testListenerInsertInto.getLastOldData());  // receive no old data in insert into
        testListener.reset();
    }

    public void testRStreamInsertIntoRStream()
    {
        epService = EPServiceProviderManager.getDefaultProvider(SupportConfigFactory.getConfiguration());
        epService.initialize();
        if (InstrumentationHelper.ENABLED) { InstrumentationHelper.startTest(epService, this.getClass(), getName());}

        EPStatement statement = epService.getEPAdministrator().createEPL(
                "insert rstream into NextStream " +
                "select rstream s0.theString as theString from " + SupportBean.class.getName() + "#length(3) as s0");
        statement.addListener(testListener);

        statement = epService.getEPAdministrator().createEPL("select * from NextStream");
        statement.addListener(testListenerInsertInto);

        sendEvent("a");
        assertFalse(testListener.isInvoked());
        assertFalse(testListenerInsertInto.isInvoked());

        sendEvents(new String[] {"b", "c"});
        assertFalse(testListener.isInvoked());
        assertFalse(testListenerInsertInto.isInvoked());

        sendEvent("d");
        assertSame("a", testListener.getLastNewData()[0].get("theString"));    // receive 'a' as new data
        assertNull(testListener.getLastOldData());  // receive no more old data
        assertEquals("a", testListenerInsertInto.getLastNewData()[0].get("theString"));    // insert into unchanged
        assertNull(testListener.getLastOldData());  // receive no old data in insert into
        testListener.reset();
    }

    public void testRStreamJoin()
    {
        epService = EPServiceProviderManager.getDefaultProvider(SupportConfigFactory.getConfiguration());
        epService.initialize();
        if (InstrumentationHelper.ENABLED) { InstrumentationHelper.startTest(epService, this.getClass(), getName());}

        EPStatement statement = epService.getEPAdministrator().createEPL(
                "select rstream s1.intPrimitive as aID, s2.intPrimitive as bID " +
                "from " + SupportBean.class.getName() + "(theString='a')#length(2) as s1, "
                        + SupportBean.class.getName() + "(theString='b')#keepall as s2" +
                " where s1.intPrimitive = s2.intPrimitive");
        statement.addListener(testListener);

        sendEvent("a", 1);
        sendEvent("b", 1);
        assertFalse(testListener.isInvoked());

        sendEvent("a", 2);
        assertFalse(testListener.isInvoked());

        sendEvent("a", 3);
        assertEquals(1, testListener.getLastNewData()[0].get("aID"));    // receive 'a' as new data
        assertEquals(1, testListener.getLastNewData()[0].get("bID"));
        assertNull(testListener.getLastOldData());  // receive no more old data
        testListener.reset();
    }

    public void testIStreamOnly()
    {
        epService = EPServiceProviderManager.getDefaultProvider(SupportConfigFactory.getConfiguration());
        epService.initialize();
        if (InstrumentationHelper.ENABLED) { InstrumentationHelper.startTest(epService, this.getClass(), getName());}

        EPStatement statement = epService.getEPAdministrator().createEPL(
                "select istream * from " + SupportBean.class.getName() + "#length(1)");
        statement.addListener(testListener);

        Object theEvent = sendEvent("a");
        assertSame(theEvent, testListener.assertOneGetNewAndReset().getUnderlying());

        theEvent = sendEvent("b");
        assertSame(theEvent, testListener.getLastNewData()[0].getUnderlying());
        assertNull(testListener.getLastOldData()); // receive no old data, just istream events
        testListener.reset();
    }

    public void testIStreamInsertIntoRStream()
    {
        epService = EPServiceProviderManager.getDefaultProvider(SupportConfigFactory.getConfiguration());
        epService.initialize();
        if (InstrumentationHelper.ENABLED) { InstrumentationHelper.startTest(epService, this.getClass(), getName());}

        EPStatement statement = epService.getEPAdministrator().createEPL(
                "insert rstream into NextStream " +
                "select istream a.theString as theString from " + SupportBean.class.getName() + "#length(1) as a");
        statement.addListener(testListener);

        statement = epService.getEPAdministrator().createEPL("select * from NextStream");
        statement.addListener(testListenerInsertInto);

        sendEvent("a");
        assertEquals("a", testListener.assertOneGetNewAndReset().get("theString"));
        assertFalse(testListenerInsertInto.isInvoked());

        sendEvent("b");
        assertEquals("b", testListener.getLastNewData()[0].get("theString"));
        assertNull(testListener.getLastOldData());
        assertEquals("a", testListenerInsertInto.getLastNewData()[0].get("theString"));
        assertNull(testListenerInsertInto.getLastOldData());
    }

    public void testIStreamJoin()
    {
        epService = EPServiceProviderManager.getDefaultProvider(SupportConfigFactory.getConfiguration());
        epService.initialize();
        if (InstrumentationHelper.ENABLED) { InstrumentationHelper.startTest(epService, this.getClass(), getName());}

        EPStatement statement = epService.getEPAdministrator().createEPL(
                "select istream s1.intPrimitive as aID, s2.intPrimitive as bID " +
                "from " + SupportBean.class.getName() + "(theString='a')#length(2) as s1, "
                        + SupportBean.class.getName() + "(theString='b')#keepall as s2" +
                " where s1.intPrimitive = s2.intPrimitive");
        statement.addListener(testListener);

        sendEvent("a", 1);
        sendEvent("b", 1);
        assertEquals(1, testListener.getLastNewData()[0].get("aID"));    // receive 'a' as new data
        assertEquals(1, testListener.getLastNewData()[0].get("bID"));
        assertNull(testListener.getLastOldData());  // receive no more old data
        testListener.reset();

        sendEvent("a", 2);
        assertFalse(testListener.isInvoked());

        sendEvent("a", 3);
        assertFalse(testListener.isInvoked());
    }

    private void sendEvents(String[] stringValue)
    {
        for (int i = 0; i < stringValue.length; i++)
        {
            sendEvent(stringValue[i]);
        }
    }

    private Object sendEvent(String stringValue)
    {
        return sendEvent(stringValue, -1);
    }

    private Object sendEvent(String stringValue, int intPrimitive)
    {
        SupportBean theEvent = new SupportBean();
        theEvent.setTheString(stringValue);
        theEvent.setIntPrimitive(intPrimitive);
        epService.getEPRuntime().sendEvent(theEvent);
        return theEvent;
    }
}
