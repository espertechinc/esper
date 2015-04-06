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

package com.espertech.esper.regression.view;

import com.espertech.esper.client.EPServiceProvider;
import com.espertech.esper.client.EPServiceProviderManager;
import com.espertech.esper.client.EPStatement;
import com.espertech.esper.client.EventBean;
import com.espertech.esper.client.scopetest.EPAssertionUtil;
import com.espertech.esper.client.scopetest.SupportUpdateListener;
import com.espertech.esper.metrics.instrumentation.InstrumentationHelper;
import com.espertech.esper.support.bean.SupportBeanNumeric;
import com.espertech.esper.support.bean.SupportBean;
import com.espertech.esper.support.client.SupportConfigFactory;
import com.espertech.esper.support.epl.SupportStaticMethodLib;
import junit.framework.TestCase;

import java.math.BigDecimal;
import java.math.BigInteger;

public class TestBigNumberSupport extends TestCase
{
    private EPServiceProvider epService;
    private SupportUpdateListener listener;

    public void setUp()
    {
        epService = EPServiceProviderManager.getDefaultProvider(SupportConfigFactory.getConfiguration());
        epService.initialize();
        if (InstrumentationHelper.ENABLED) { InstrumentationHelper.startTest(epService, this.getClass(), getName());}
        listener = new SupportUpdateListener();
        epService.getEPAdministrator().getConfiguration().addEventType("SupportBeanNumeric", SupportBeanNumeric.class);
        epService.getEPAdministrator().getConfiguration().addEventType("SupportBean", SupportBean.class);
    }

    protected void tearDown() throws Exception {
        if (InstrumentationHelper.ENABLED) { InstrumentationHelper.endTest();}
        listener = null;
    }

    public void testEquals()
    {
        // test equals BigDecimal
        EPStatement stmt = epService.getEPAdministrator().createEPL("select * from SupportBeanNumeric where bigdec = 1 or bigdec = intOne or bigdec = doubleOne");
        stmt.addListener(listener);

        sendBigNumEvent(-1, 1);
        assertTrue(listener.getAndClearIsInvoked());
        sendBigNumEvent(-1, 2);
        assertFalse(listener.getAndClearIsInvoked());

        epService.getEPRuntime().sendEvent(new SupportBeanNumeric(2, 0, null, new BigDecimal(2), 0, 0));
        assertTrue(listener.getAndClearIsInvoked());
        epService.getEPRuntime().sendEvent(new SupportBeanNumeric(3, 0, null, new BigDecimal(2), 0, 0));
        assertFalse(listener.getAndClearIsInvoked());

        epService.getEPRuntime().sendEvent(new SupportBeanNumeric(0, 0, null, new BigDecimal(3d), 3d, 0));
        assertTrue(listener.getAndClearIsInvoked());
        epService.getEPRuntime().sendEvent(new SupportBeanNumeric(0, 0, null, new BigDecimal(3.9999d), 4d, 0));
        assertFalse(listener.getAndClearIsInvoked());

        // test equals BigInteger
        stmt = epService.getEPAdministrator().createEPL("select * from SupportBeanNumeric where bigdec = bigint or bigint = intOne or bigint = 1");
        stmt.addListener(listener);

        epService.getEPRuntime().sendEvent(new SupportBeanNumeric(0, 0, BigInteger.valueOf(2), new BigDecimal(2), 0, 0));
        assertTrue(listener.getAndClearIsInvoked());
        epService.getEPRuntime().sendEvent(new SupportBeanNumeric(0, 0, BigInteger.valueOf(3), new BigDecimal(2), 0, 0));
        assertFalse(listener.getAndClearIsInvoked());

        epService.getEPRuntime().sendEvent(new SupportBeanNumeric(2, 0, BigInteger.valueOf(2), null, 0, 0));
        assertTrue(listener.getAndClearIsInvoked());
        epService.getEPRuntime().sendEvent(new SupportBeanNumeric(3, 0, BigInteger.valueOf(2), null, 0, 0));
        assertFalse(listener.getAndClearIsInvoked());

        epService.getEPRuntime().sendEvent(new SupportBeanNumeric(0, 0, BigInteger.valueOf(1), null, 0, 0));
        assertTrue(listener.getAndClearIsInvoked());
        epService.getEPRuntime().sendEvent(new SupportBeanNumeric(0, 0, BigInteger.valueOf(4), null, 0, 0));
        assertFalse(listener.getAndClearIsInvoked());
    }

    public void testRelOp()
    {
        // relational op tests handled by relational op unit test
        EPStatement stmt = epService.getEPAdministrator().createEPL("select * from SupportBeanNumeric where bigdec < 10 and bigint > 10");
        stmt.addListener(listener);

        sendBigNumEvent(10, 10);
        assertFalse(listener.getAndClearIsInvoked());

        sendBigNumEvent(11, 9);
        assertTrue(listener.getAndClearIsInvoked());
        stmt.destroy();

        stmt = epService.getEPAdministrator().createEPL("select * from SupportBeanNumeric where bigdec < 10.0");
        stmt.addListener(listener);

        sendBigNumEvent(0, 11);
        assertFalse(listener.getAndClearIsInvoked());

        epService.getEPRuntime().sendEvent(new SupportBeanNumeric(null, new BigDecimal(9.999)));
        assertTrue(listener.getAndClearIsInvoked());
        stmt.destroy();

        // test float
        stmt = epService.getEPAdministrator().createEPL("select * from SupportBeanNumeric where floatOne < 10f and floatTwo > 10f");
        stmt.addListener(listener);
        
        epService.getEPRuntime().sendEvent(new SupportBeanNumeric(true, 1f, 20f));
        assertTrue(listener.getAndClearIsInvoked());
        epService.getEPRuntime().sendEvent(new SupportBeanNumeric(true, 20f, 1f));
        assertFalse(listener.getAndClearIsInvoked());
    }

    public void testBetween()
    {
        EPStatement stmt = epService.getEPAdministrator().createEPL("select * from SupportBeanNumeric where bigdec between 10 and 20 or bigint between 100 and 200");
        stmt.addListener(listener);

        sendBigNumEvent(0, 9);
        assertFalse(listener.getAndClearIsInvoked());

        sendBigNumEvent(0, 10);
        assertTrue(listener.getAndClearIsInvoked());

        sendBigNumEvent(99, 0);
        assertFalse(listener.getAndClearIsInvoked());

        sendBigNumEvent(100, 0);
        assertTrue(listener.getAndClearIsInvoked());
        stmt.destroy();
    }

    public void testIn()
    {
        EPStatement stmt = epService.getEPAdministrator().createEPL("select * from SupportBeanNumeric where bigdec in (10, 20d) or bigint in (0x02, 3)");
        stmt.addListener(listener);

        sendBigNumEvent(0, 9);
        assertFalse(listener.getAndClearIsInvoked());

        sendBigNumEvent(0, 10);
        assertTrue(listener.getAndClearIsInvoked());

        epService.getEPRuntime().sendEvent(new SupportBeanNumeric(null, new BigDecimal(20d)));
        assertTrue(listener.getAndClearIsInvoked());

        sendBigNumEvent(99, 0);
        assertFalse(listener.getAndClearIsInvoked());

        sendBigNumEvent(2, 0);
        assertTrue(listener.getAndClearIsInvoked());

        sendBigNumEvent(3, 0);
        assertTrue(listener.getAndClearIsInvoked());
        stmt.destroy();
    }

    public void testMath()
    {
        EPStatement stmt = epService.getEPAdministrator().createEPL("select * from SupportBeanNumeric " +
                        "where bigdec+bigint=100 or bigdec+1=2 or bigdec+2d=5.0 or bigint+5L=8 or bigint+5d=9.0");
        stmt.addListener(listener);

        sendBigNumEvent(50, 49);
        assertFalse(listener.getAndClearIsInvoked());

        sendBigNumEvent(50, 50);
        assertTrue(listener.getAndClearIsInvoked());

        sendBigNumEvent(0, 1);
        assertTrue(listener.getAndClearIsInvoked());

        sendBigNumEvent(0, 2);
        assertFalse(listener.getAndClearIsInvoked());

        sendBigNumEvent(0, 3);
        assertTrue(listener.getAndClearIsInvoked());

        sendBigNumEvent(0, 0);
        assertFalse(listener.getAndClearIsInvoked());

        sendBigNumEvent(3, 0);
        assertTrue(listener.getAndClearIsInvoked());

        sendBigNumEvent(4, 0);
        assertTrue(listener.getAndClearIsInvoked());
        stmt.destroy();

        stmt = epService.getEPAdministrator().createEPL(
                "select bigdec+bigint as v1, bigdec+2 as v2, bigdec+3d as v3, bigint+5L as v4, bigint+5d as v5 " +
                " from SupportBeanNumeric");
        stmt.addListener(listener);
        listener.reset();

        assertEquals(BigDecimal.class, stmt.getEventType().getPropertyType("v1"));
        assertEquals(BigDecimal.class, stmt.getEventType().getPropertyType("v2"));
        assertEquals(BigDecimal.class, stmt.getEventType().getPropertyType("v3"));
        assertEquals(BigInteger.class, stmt.getEventType().getPropertyType("v4"));
        assertEquals(BigDecimal.class, stmt.getEventType().getPropertyType("v5"));

        sendBigNumEvent(1, 2);
        EventBean theEvent = listener.assertOneGetNewAndReset();
        EPAssertionUtil.assertProps(theEvent, "v1,v2,v3,v4,v5".split(","),
                new Object[]{new BigDecimal(3), new BigDecimal(4), new BigDecimal(5d), BigInteger.valueOf(6), new BigDecimal(6d)});

        // test aggregation-sum, multiplication and division all together; test for ESPER-340
        stmt.destroy();
        stmt = epService.getEPAdministrator().createEPL(
                "select (sum(bigdecTwo * bigdec)/sum(bigdec)) as avgRate from SupportBeanNumeric");
        stmt.addListener(listener);
        listener.reset();
        assertEquals(BigDecimal.class, stmt.getEventType().getPropertyType("avgRate"));
        sendBigNumEvent(0, 5);
        Object avgRate = listener.assertOneGetNewAndReset().get("avgRate");
        assertTrue(avgRate instanceof BigDecimal);
        assertEquals(new BigDecimal(5d), avgRate);
    }

    public void testAggregation()
    {
        String fields = "sum(bigint),sum(bigdec)," +
                "avg(bigint),avg(bigdec)," +
                "median(bigint),median(bigdec)," +
                "stddev(bigint),stddev(bigdec)," +
                "avedev(bigint),avedev(bigdec)," +
                "min(bigint),min(bigdec)";
        EPStatement stmt = epService.getEPAdministrator().createEPL(
                "select " + fields + " from SupportBeanNumeric");
        stmt.addListener(listener);
        listener.reset();

        String[] fieldList = fields.split(",");
        sendBigNumEvent(1, 2);
        EventBean theEvent = listener.assertOneGetNewAndReset();
        EPAssertionUtil.assertProps(theEvent, fieldList,
                new Object[]{BigInteger.valueOf(1), new BigDecimal(2d),        // sum
                        new BigDecimal(1), new BigDecimal(2),               // avg
                        1d, 2d,               // median
                        null, null,
                        0.0, 0.0,
                        BigInteger.valueOf(1), new BigDecimal(2),
                });
    }

    public void testMinMax()
    {
        EPStatement stmt = epService.getEPAdministrator().createEPL(
                "select min(bigint, 10) as v1, min(10, bigint) as v2, " +
                "max(bigdec, 10) as v3, max(10, 100d, bigint, bigdec) as v4 from SupportBeanNumeric");
        stmt.addListener(listener);
        listener.reset();

        String[] fieldList = "v1,v2,v3,v4".split(",");

        sendBigNumEvent(1, 2);
        EPAssertionUtil.assertProps(listener.assertOneGetNewAndReset(), fieldList,
                new Object[]{BigInteger.valueOf(1), BigInteger.valueOf(1), new BigDecimal(10), new BigDecimal(100d)});

        sendBigNumEvent(40, 300);
        EPAssertionUtil.assertProps(listener.assertOneGetNewAndReset(), fieldList,
                new Object[]{BigInteger.valueOf(10), BigInteger.valueOf(10), new BigDecimal(300), new BigDecimal(300)});

        sendBigNumEvent(250, 200);
        EPAssertionUtil.assertProps(listener.assertOneGetNewAndReset(), fieldList,
                new Object[]{BigInteger.valueOf(10), BigInteger.valueOf(10), new BigDecimal(200), new BigDecimal(250)});
    }

    public void testFilterEquals()
    {
        String[] fieldList = "bigdec".split(",");

        EPStatement stmt = epService.getEPAdministrator().createEPL("select bigdec from SupportBeanNumeric(bigdec = 4)");
        stmt.addListener(listener);

        sendBigNumEvent(0, 2);
        assertFalse(listener.isInvoked());

        sendBigNumEvent(0, 4);
        EPAssertionUtil.assertProps(listener.assertOneGetNewAndReset(), fieldList, new Object[]{new BigDecimal(4)});

        stmt.destroy();
        stmt = epService.getEPAdministrator().createEPL("select bigdec from SupportBeanNumeric(bigdec = 4d)");
        stmt.addListener(listener);

        sendBigNumEvent(0, 4);
        assertTrue(listener.isInvoked());
        listener.reset();

        epService.getEPRuntime().sendEvent(new SupportBeanNumeric(BigInteger.valueOf(0), new BigDecimal(4d)));
        EPAssertionUtil.assertProps(listener.assertOneGetNewAndReset(), fieldList, new Object[]{new BigDecimal(4d)});

        stmt.destroy();
        stmt = epService.getEPAdministrator().createEPL("select bigdec from SupportBeanNumeric(bigint = 4)");
        stmt.addListener(listener);

        sendBigNumEvent(3, 4);
        assertFalse(listener.isInvoked());

        sendBigNumEvent(4, 3);
        EPAssertionUtil.assertProps(listener.assertOneGetNewAndReset(), fieldList, new Object[]{new BigDecimal(3)});
    }

    public void testJoin()
    {
        String[] fieldList = "bigint,bigdec".split(",");
        EPStatement stmt = epService.getEPAdministrator().createEPL("select bigint,bigdec from SupportBeanNumeric.win:keepall(), SupportBean.win:keepall() " +
                "where intPrimitive = bigint and doublePrimitive = bigdec");
        stmt.addListener(listener);

        sendSupportBean(2, 3);
        sendBigNumEvent(0, 2);
        sendBigNumEvent(2, 0);
        assertFalse(listener.isInvoked());

        epService.getEPRuntime().sendEvent(new SupportBeanNumeric(BigInteger.valueOf(2), new BigDecimal(3d)));
        EPAssertionUtil.assertProps(listener.assertOneGetNewAndReset(), fieldList, new Object[]{BigInteger.valueOf(2), new BigDecimal(3d)});
    }

    public void testCastAndUDF()
    {
        epService.getEPAdministrator().getConfiguration().addImport(SupportStaticMethodLib.class.getName());
        EPStatement stmt = epService.getEPAdministrator().createEPL(
                "select SupportStaticMethodLib.myBigIntFunc(cast(2, BigInteger)) as v1, SupportStaticMethodLib.myBigDecFunc(cast(3d, BigDecimal)) as v2 from SupportBeanNumeric");
        stmt.addListener(listener);

        String[] fieldList = "v1,v2".split(",");
        sendBigNumEvent(0, 2);
        EPAssertionUtil.assertProps(listener.assertOneGetNewAndReset(), fieldList, new Object[]{BigInteger.valueOf(2), new BigDecimal(3.0)});
    }

    private void sendBigNumEvent(int bigInt, double bigDec)
    {
        SupportBeanNumeric bean = new SupportBeanNumeric(BigInteger.valueOf(bigInt), new BigDecimal(bigDec));
        bean.setBigdecTwo(new BigDecimal(bigDec));
        epService.getEPRuntime().sendEvent(bean);
    }

    private void sendSupportBean(int intPrimitive, double doublePrimitive)
    {
        SupportBean bean = new SupportBean();
        bean.setIntPrimitive(intPrimitive);
        bean.setDoublePrimitive(doublePrimitive);
        epService.getEPRuntime().sendEvent(bean);
    }
}
