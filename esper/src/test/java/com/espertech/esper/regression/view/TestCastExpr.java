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

import com.espertech.esper.client.*;
import com.espertech.esper.client.scopetest.EPAssertionUtil;
import com.espertech.esper.client.scopetest.SupportUpdateListener;
import com.espertech.esper.client.soda.*;
import com.espertech.esper.metrics.instrumentation.InstrumentationHelper;
import com.espertech.esper.support.bean.*;
import com.espertech.esper.support.client.SupportConfigFactory;
import com.espertech.esper.support.timer.SupportDateTimeUtil;
import com.espertech.esper.support.util.SupportMessageAssertUtil;
import com.espertech.esper.support.util.SupportModelHelper;
import com.espertech.esper.util.SerializableObjectCopier;
import junit.framework.TestCase;

import java.text.SimpleDateFormat;
import java.util.*;

public class TestCastExpr extends TestCase
{
    private EPServiceProvider epService;
    private SupportUpdateListener listener;

    public void setUp()
    {
        listener = new SupportUpdateListener();
        epService = EPServiceProviderManager.getDefaultProvider(SupportConfigFactory.getConfiguration());
        epService.initialize();
        if (InstrumentationHelper.ENABLED) { InstrumentationHelper.startTest(epService, this.getClass(), getName());}
    }

    protected void tearDown() throws Exception {
        if (InstrumentationHelper.ENABLED) { InstrumentationHelper.endTest();}
        listener = null;
    }

    public void testCaseDates() throws Exception {
        epService.getEPAdministrator().createEPL("create map schema MyType(yyyymmdd string)");
        epService.getEPAdministrator().getConfiguration().addEventType(SupportBean_StringAlphabetic.class);
        epService.getEPAdministrator().getConfiguration().addEventType(SupportBean.class);

        runAssertionDatetimeTypes(true);
        runAssertionDatetimeTypes(false);

        runAssertionDatetimeRenderOutCol();

        runAssertionDatetimeInvalid();

        runAssertionDynamicDateFormat();

        runAssertionConstantDate();

        runAssertionISO8601Date();
    }

    private void runAssertionISO8601Date() {
        String epl = "select " +
                "cast('1997-07-16T19:20:30Z',calendar,dateformat:'iso') as c0," +
                "cast('1997-07-16T19:20:30+01:00',calendar,dateformat:'iso') as c1," +
                "cast('1997-07-16T19:20:30',calendar,dateformat:'iso') as c2," +
                "cast('1997-07-16T19:20:30.45Z',calendar,dateformat:'iso') as c3," +
                "cast('1997-07-16T19:20:30.45+01:00',calendar,dateformat:'iso') as c4," +
                "cast('1997-07-16T19:20:30.45',calendar,dateformat:'iso') as c5," +
                "cast('1997-07-16T19:20:30.45',long,dateformat:'iso') as c6," +
                "cast('1997-07-16T19:20:30.45',date,dateformat:'iso') as c7," +
                "cast(theString,calendar,dateformat:'iso') as c8," +
                "cast(theString,long,dateformat:'iso') as c9," +
                "cast(theString,date,dateformat:'iso') as c10" +
                " from SupportBean";
        EPStatement stmt = epService.getEPAdministrator().createEPL(epl);
        stmt.addListener(listener);

        epService.getEPRuntime().sendEvent(new SupportBean());
        EventBean event = listener.assertOneGetNewAndReset();
        SupportDateTimeUtil.compareDate((Calendar) event.get("c0"), 1997, 6, 16, 19, 20, 30, 0, "GMT+00:00");
        SupportDateTimeUtil.compareDate((Calendar) event.get("c1"), 1997, 6, 16, 19, 20, 30, 0, "GMT+01:00");
        SupportDateTimeUtil.compareDate((Calendar) event.get("c2"), 1997, 6, 16, 19, 20, 30, 0, TimeZone.getDefault().getID());
        SupportDateTimeUtil.compareDate((Calendar) event.get("c3"), 1997, 6, 16, 19, 20, 30, 450, "GMT+00:00");
        SupportDateTimeUtil.compareDate((Calendar) event.get("c4"), 1997, 6, 16, 19, 20, 30, 450, "GMT+01:00");
        SupportDateTimeUtil.compareDate((Calendar) event.get("c5"), 1997, 6, 16, 19, 20, 30, 450, TimeZone.getDefault().getID());
        assertEquals(Long.class, event.get("c6").getClass());
        assertEquals(Date.class, event.get("c7").getClass());
        for (String prop : "c8,c9,c10".split(",")) {
            assertNull(event.get(prop));
        }
    }

    private void runAssertionConstantDate() throws Exception {
        String epl = "select cast('20030201',date,dateformat:\"yyyyMMdd\") as c0 from SupportBean";
        EPStatement stmt = epService.getEPAdministrator().createEPL(epl);
        stmt.addListener(listener);

        SimpleDateFormat dateFormat = new SimpleDateFormat("yyyyMMdd");
        Date expectedDate = dateFormat.parse("20030201");
        epService.getEPRuntime().sendEvent(new SupportBean("E1", 1));
        EPAssertionUtil.assertProps(listener.assertOneGetNewAndReset(), "c0".split(","), new Object[] {expectedDate});

        stmt.destroy();
    }

    private void runAssertionDynamicDateFormat() throws Exception {

        String epl = "select " +
                "cast(a,date,dateformat:b) as c0," +
                "cast(a,long,dateformat:b) as c1," +
                "cast(a,calendar,dateformat:b) as c2" +
                " from SupportBean_StringAlphabetic";
        EPStatement stmt = epService.getEPAdministrator().createEPL(epl);
        stmt.addListener(listener);

        runAssertionDynamicDateFormat("20100502", "yyyyMMdd");
        runAssertionDynamicDateFormat("20100502101112", "yyyyMMddhhmmss");
        runAssertionDynamicDateFormat(null, "yyyyMMdd");

        // invalid date
        try {
            epService.getEPRuntime().sendEvent(new SupportBean_StringAlphabetic("x", "yyyyMMddhhmmss"));
        }
        catch (EPException ex) {
            SupportMessageAssertUtil.assertMessageContains(ex, "Exception parsing date 'x' format 'yyyyMMddhhmmss': Unparseable date: \"x\"");
        }

        // invalid format
        try {
            epService.getEPRuntime().sendEvent(new SupportBean_StringAlphabetic("20100502", "UUHHYY"));
        }
        catch (EPException ex) {
            SupportMessageAssertUtil.assertMessageContains(ex, "Illegal pattern character 'U'");
        }

        stmt.destroy();
    }

    private void runAssertionDynamicDateFormat(String date, String format) throws Exception {
        epService.getEPRuntime().sendEvent(new SupportBean_StringAlphabetic(date, format));

        SimpleDateFormat dateFormat = new SimpleDateFormat(format);
        Date expectedDate = date == null ? null : dateFormat.parse(date);
        Calendar cal = null;
        Long theLong = null;
        if (expectedDate != null) {
            cal = Calendar.getInstance();
            cal.setTime(expectedDate);
            theLong = expectedDate.getTime();
        }
        EPAssertionUtil.assertProps(listener.assertOneGetNewAndReset(), "c0,c1,c2".split(","),
                new Object[] {expectedDate, theLong, cal});
    }

    private void runAssertionDatetimeInvalid() {
        // not a valid named parameter
        SupportMessageAssertUtil.tryInvalid(epService, "select cast(theString, date, x:1) from SupportBean",
                "Error starting statement: Failed to validate select-clause expression 'cast(theString,date,x:1)': Unexpected named parameter 'x', expecting any of the following: [dateformat]");

        // invalid date format
        SupportMessageAssertUtil.tryInvalid(epService, "select cast(theString, date, dateformat:'BBBBMMDD') from SupportBean",
                "Error starting statement: Failed to validate select-clause expression 'cast(theString,date,dateformat:\"BBB...(42 chars)': Invalid date format 'BBBBMMDD': Illegal pattern character 'B'");
        SupportMessageAssertUtil.tryInvalid(epService, "select cast(theString, date, dateformat:1) from SupportBean",
                "Error starting statement: Failed to validate select-clause expression 'cast(theString,date,dateformat:1)': Failed to validate named parameter 'dateformat', expected a single expression returning a string-typed value");

        // invalid input
        SupportMessageAssertUtil.tryInvalid(epService, "select cast(intPrimitive, date, dateformat:'yyyyMMdd') from SupportBean",
                "Error starting statement: Failed to validate select-clause expression 'cast(intPrimitive,date,dateformat:\"...(45 chars)': Use of the 'dateformat' named parameter requires a string-type input");

        // invalid target
        SupportMessageAssertUtil.tryInvalid(epService, "select cast(theString, int, dateformat:'yyyyMMdd') from SupportBean",
                "Error starting statement: Failed to validate select-clause expression 'cast(theString,int,dateformat:\"yyyy...(41 chars)': Use of the 'dateformat' named parameter requires a target type of calendar, date or long [select cast(theString, int, dateformat:'yyyyMMdd') from SupportBean]");
    }

    private void runAssertionDatetimeRenderOutCol() {
        String epl = "select cast(yyyymmdd,date,dateformat:\"yyyyMMdd\") from MyType";
        EPStatement stmt = epService.getEPAdministrator().createEPL(epl);
        assertEquals("cast(yyyymmdd,date,dateformat:\"yyyyMMdd\")", stmt.getEventType().getPropertyNames()[0]);
        stmt.destroy();
    }

    private void runAssertionDatetimeTypes(boolean soda) throws Exception
    {
        String epl = "select " +
                "cast(yyyymmdd,date,dateformat:\"yyyyMMdd\") as c0, " +
                "cast(yyyymmdd,java.util.Date,dateformat:\"yyyyMMdd\") as c1, " +
                "cast(yyyymmdd,long,dateformat:\"yyyyMMdd\") as c2, " +
                "cast(yyyymmdd,java.lang.Long,dateformat:\"yyyyMMdd\") as c3, " +
                "cast(yyyymmdd,calendar,dateformat:\"yyyyMMdd\") as c4, " +
                "cast(yyyymmdd,java.util.Calendar,dateformat:\"yyyyMMdd\") as c5, " +
                "cast(yyyymmdd,date,dateformat:\"yyyyMMdd\").get(\"month\") as c6, " +
                "cast(yyyymmdd,calendar,dateformat:\"yyyyMMdd\").get(\"month\") as c7, " +
                "cast(yyyymmdd,long,dateformat:\"yyyyMMdd\").get(\"month\") as c8 " +
                "from MyType";
        EPStatement stmt = SupportModelHelper.createByCompileOrParse(epService, soda, epl);
        stmt.addListener(listener);

        Map<String, Object> values = new HashMap<String, Object>();
        values.put("yyyymmdd", "20100510");
        epService.getEPRuntime().sendEvent(values, "MyType");

        SimpleDateFormat formatYYYYMMdd = new SimpleDateFormat("yyyyMMdd");
        Date dateYYMMddDate = formatYYYYMMdd.parse("20100510");
        Calendar calYYMMddDate = Calendar.getInstance();
        calYYMMddDate.setTime(dateYYMMddDate);
        EPAssertionUtil.assertProps(listener.assertOneGetNewAndReset(), "c0,c1,c2,c3,c4,c5,c6,c7,c8".split(","), new Object[] {
                dateYYMMddDate, dateYYMMddDate, dateYYMMddDate.getTime(), dateYYMMddDate.getTime(),
                calYYMMddDate, calYYMMddDate, 4, 4, 4});

        stmt.destroy();
    }

    public void testCastSimple()
    {
        String stmtText = "select cast(theString as string) as t0, " +
                          " cast(intBoxed, int) as t1, " +
                          " cast(floatBoxed, java.lang.Float) as t2, " +
                          " cast(theString, java.lang.String) as t3, " +
                          " cast(intPrimitive, java.lang.Integer) as t4, " +
                          " cast(intPrimitive, long) as t5, " +
                          " cast(intPrimitive, java.lang.Number) as t6, " +
                          " cast(floatBoxed, long) as t7 " +
                          " from " + SupportBean.class.getName();

        EPStatement selectTestCase = epService.getEPAdministrator().createEPL(stmtText);
        selectTestCase.addListener(listener);

        assertEquals(String.class, selectTestCase.getEventType().getPropertyType("t0"));
        assertEquals(Integer.class, selectTestCase.getEventType().getPropertyType("t1"));
        assertEquals(Float.class, selectTestCase.getEventType().getPropertyType("t2"));
        assertEquals(String.class, selectTestCase.getEventType().getPropertyType("t3"));
        assertEquals(Integer.class, selectTestCase.getEventType().getPropertyType("t4"));
        assertEquals(Long.class, selectTestCase.getEventType().getPropertyType("t5"));
        assertEquals(Number.class, selectTestCase.getEventType().getPropertyType("t6"));
        assertEquals(Long.class, selectTestCase.getEventType().getPropertyType("t7"));

        SupportBean bean = new SupportBean("abc", 100);
        bean.setFloatBoxed(9.5f);
        bean.setIntBoxed(3);
        epService.getEPRuntime().sendEvent(bean);
        EventBean theEvent = listener.assertOneGetNewAndReset();
        assertResults(theEvent, new Object[] {"abc", 3, 9.5f, "abc", 100, 100L, 100, 9l});

        bean = new SupportBean(null, 100);
        bean.setFloatBoxed(null);
        bean.setIntBoxed(null);
        epService.getEPRuntime().sendEvent(bean);
        theEvent = listener.assertOneGetNewAndReset();
        assertResults(theEvent, new Object[]{null, null, null, null, 100, 100L, 100, null});
        bean = new SupportBean(null, 100);
        bean.setFloatBoxed(null);
        bean.setIntBoxed(null);
        epService.getEPRuntime().sendEvent(bean);
        theEvent = listener.assertOneGetNewAndReset();
        assertResults(theEvent, new Object[] {null, null, null, null, 100, 100L, 100, null});

        // test cast with chained
        selectTestCase.destroy();
        stmtText = "select cast(one as " + SupportBean.class.getName() + ").getTheString() as t0" +
                          " from " + SupportBeanObject.class.getName();
        selectTestCase = epService.getEPAdministrator().createEPL(stmtText);
        selectTestCase.addListener(listener);

        epService.getEPRuntime().sendEvent(new SupportBeanObject(new SupportBean("E1", 1)));
        assertEquals("E1", listener.assertOneGetNewAndReset().get("t0"));
    }

    public void testCastAsParse()
    {
        String stmtText = "select cast(theString, int) as t0 from " + SupportBean.class.getName();
        EPStatement selectTestCase = epService.getEPAdministrator().createEPL(stmtText);
        selectTestCase.addListener(listener);

        assertEquals(Integer.class, selectTestCase.getEventType().getPropertyType("t0"));

        epService.getEPRuntime().sendEvent(new SupportBean("12", 1));
        EPAssertionUtil.assertProps(listener.assertOneGetNewAndReset(), "t0".split(","), new Object[]{12});
    }

    public void testCastDoubleAndNull_OM() throws Exception
    {
        String stmtText = "select cast(item?,double) as t0 " +
                          "from " + SupportMarkerInterface.class.getName();

        EPStatementObjectModel model = new EPStatementObjectModel();
        model.setSelectClause(SelectClause.create().add(Expressions.cast("item?", "double"), "t0"));
        model.setFromClause(FromClause.create(FilterStream.create(SupportMarkerInterface.class.getName())));
        model = (EPStatementObjectModel) SerializableObjectCopier.copy(model);
        assertEquals(stmtText, model.toEPL());

        EPStatement selectTestCase = epService.getEPAdministrator().create(model);
        selectTestCase.addListener(listener);

        assertEquals(Double.class, selectTestCase.getEventType().getPropertyType("t0"));

        epService.getEPRuntime().sendEvent(new SupportBeanDynRoot(100));
        assertEquals(100d, listener.assertOneGetNewAndReset().get("t0"));

        epService.getEPRuntime().sendEvent(new SupportBeanDynRoot((byte)2));
        assertEquals(2d, listener.assertOneGetNewAndReset().get("t0"));

        epService.getEPRuntime().sendEvent(new SupportBeanDynRoot(77.7777));
        assertEquals(77.7777d, listener.assertOneGetNewAndReset().get("t0"));

        epService.getEPRuntime().sendEvent(new SupportBeanDynRoot(6L));
        assertEquals(6d, listener.assertOneGetNewAndReset().get("t0"));

        epService.getEPRuntime().sendEvent(new SupportBeanDynRoot(null));
        assertEquals(null, listener.assertOneGetNewAndReset().get("t0"));

        epService.getEPRuntime().sendEvent(new SupportBeanDynRoot("abc"));
        assertEquals(null, listener.assertOneGetNewAndReset().get("t0"));
    }

    public void testCastStringAndNull_Compile() throws Exception
    {
        String stmtText = "select cast(item?,java.lang.String) as t0 " +
                          "from " + SupportMarkerInterface.class.getName();

        EPStatementObjectModel model = epService.getEPAdministrator().compileEPL(stmtText);
        model = (EPStatementObjectModel) SerializableObjectCopier.copy(model);
        EPStatement selectTestCase = epService.getEPAdministrator().create(model);
        assertEquals(stmtText, model.toEPL());
        selectTestCase.addListener(listener);

        assertEquals(String.class, selectTestCase.getEventType().getPropertyType("t0"));

        epService.getEPRuntime().sendEvent(new SupportBeanDynRoot(100));
        assertEquals("100", listener.assertOneGetNewAndReset().get("t0"));

        epService.getEPRuntime().sendEvent(new SupportBeanDynRoot((byte)2));
        assertEquals("2", listener.assertOneGetNewAndReset().get("t0"));

        epService.getEPRuntime().sendEvent(new SupportBeanDynRoot(77.7777));
        assertEquals("77.7777", listener.assertOneGetNewAndReset().get("t0"));

        epService.getEPRuntime().sendEvent(new SupportBeanDynRoot(6L));
        assertEquals("6", listener.assertOneGetNewAndReset().get("t0"));

        epService.getEPRuntime().sendEvent(new SupportBeanDynRoot(null));
        assertEquals(null, listener.assertOneGetNewAndReset().get("t0"));

        epService.getEPRuntime().sendEvent(new SupportBeanDynRoot("abc"));
        assertEquals("abc", listener.assertOneGetNewAndReset().get("t0"));
    }

    public void testCastInterface()
    {
        String caseExpr = "select cast(item?, " + SupportMarkerInterface.class.getName() + ") as t0, " +
                          " cast(item?, " + ISupportA.class.getName() + ") as t1, " +
                          " cast(item?, " + ISupportBaseAB.class.getName() + ") as t2, " +
                          " cast(item?, " + ISupportBaseABImpl.class.getName() + ") as t3, " +
                          " cast(item?, " + ISupportC.class.getName() + ") as t4, " +
                          " cast(item?, " + ISupportD.class.getName() + ") as t5, " +
                          " cast(item?, " + ISupportAImplSuperG.class.getName() + ") as t6, " +
                          " cast(item?, " + ISupportAImplSuperGImplPlus.class.getName() + ") as t7 " +
                          " from " + SupportMarkerInterface.class.getName();

        EPStatement selectTestCase = epService.getEPAdministrator().createEPL(caseExpr);
        selectTestCase.addListener(listener);

        assertEquals(SupportMarkerInterface.class, selectTestCase.getEventType().getPropertyType("t0"));
        assertEquals(ISupportA.class, selectTestCase.getEventType().getPropertyType("t1"));
        assertEquals(ISupportBaseAB.class, selectTestCase.getEventType().getPropertyType("t2"));
        assertEquals(ISupportBaseABImpl.class, selectTestCase.getEventType().getPropertyType("t3"));
        assertEquals(ISupportC.class, selectTestCase.getEventType().getPropertyType("t4"));
        assertEquals(ISupportD.class, selectTestCase.getEventType().getPropertyType("t5"));
        assertEquals(ISupportAImplSuperG.class, selectTestCase.getEventType().getPropertyType("t6"));
        assertEquals(ISupportAImplSuperGImplPlus.class, selectTestCase.getEventType().getPropertyType("t7"));

        Object bean = new SupportBeanDynRoot("abc");
        epService.getEPRuntime().sendEvent(new SupportBeanDynRoot(bean));
        EventBean theEvent = listener.assertOneGetNewAndReset();
        assertResults(theEvent, new Object[] {bean, null, null, null, null, null, null, null});

        bean = new ISupportDImpl("", "", "");
        epService.getEPRuntime().sendEvent(new SupportBeanDynRoot(bean));
        theEvent = listener.assertOneGetNewAndReset();
        assertResults(theEvent, new Object[]{null, null, null, null, null, bean, null, null});

        bean = new ISupportBCImpl("", "", "");
        epService.getEPRuntime().sendEvent(new SupportBeanDynRoot(bean));
        theEvent = listener.assertOneGetNewAndReset();
        assertResults(theEvent, new Object[] {null, null, bean, null, bean, null, null, null});

        bean = new ISupportAImplSuperGImplPlus();
        epService.getEPRuntime().sendEvent(new SupportBeanDynRoot(bean));
        theEvent = listener.assertOneGetNewAndReset();
        assertResults(theEvent, new Object[]{null, bean, bean, null, bean, null, bean, bean});

        bean = new ISupportBaseABImpl("");
        epService.getEPRuntime().sendEvent(new SupportBeanDynRoot(bean));
        theEvent = listener.assertOneGetNewAndReset();
        assertResults(theEvent, new Object[] {null, null, bean, bean, null, null, null, null});
    }

    public void testCastBoolean()
    {
        String stmtText = "select cast(boolPrimitive as java.lang.Boolean) as t0, " +
                          " cast(boolBoxed | boolPrimitive, boolean) as t1, " +
                          " cast(boolBoxed, string) as t2 " +
                          " from " + SupportBean.class.getName();

        EPStatement selectTestCase = epService.getEPAdministrator().createEPL(stmtText);
        selectTestCase.addListener(listener);

        assertEquals(Boolean.class, selectTestCase.getEventType().getPropertyType("t0"));
        assertEquals(Boolean.class, selectTestCase.getEventType().getPropertyType("t1"));
        assertEquals(String.class, selectTestCase.getEventType().getPropertyType("t2"));

        SupportBean bean = new SupportBean("abc", 100);
        bean.setBoolPrimitive(true);
        bean.setBoolBoxed(true);
        epService.getEPRuntime().sendEvent(bean);
        EventBean theEvent = listener.assertOneGetNewAndReset();
        assertResults(theEvent, new Object[] {true, true, "true"});

        bean = new SupportBean(null, 100);
        bean.setBoolPrimitive(false);
        bean.setBoolBoxed(false);
        epService.getEPRuntime().sendEvent(bean);
        theEvent = listener.assertOneGetNewAndReset();
        assertResults(theEvent, new Object[]{false, false, "false"});

        bean = new SupportBean(null, 100);
        bean.setBoolPrimitive(true);
        bean.setBoolBoxed(null);
        epService.getEPRuntime().sendEvent(bean);
        theEvent = listener.assertOneGetNewAndReset();
        assertResults(theEvent, new Object[] {true, null, null});
    }

    public void testCastMapStringInt()
    {
        HashMap<String, Object> map = new HashMap<String, Object>();
        map.put("anInt",String.class);
        map.put("anDouble",String.class);
        map.put("anLong",String.class);
        map.put("anFloat",String.class);
        map.put("anByte",String.class);
        map.put("anShort",String.class);
        map.put("intPrimitive",int.class);
        map.put("intBoxed",Integer.class);

        Configuration config = new Configuration();
        config.addEventType("TestEvent", map);

        epService = EPServiceProviderManager.getDefaultProvider(config);
        epService.initialize();

        String stmt = "select cast(anInt, int) as intVal, " +
                            "cast(anDouble, double) as doubleVal, " +
                            "cast(anLong, long) as longVal, " +
                            "cast(anFloat, float) as floatVal, " +
                            "cast(anByte, byte) as byteVal, " +
                            "cast(anShort, short) as shortVal, " +
                            "cast(intPrimitive, int) as intOne, " +
                            "cast(intBoxed, int) as intTwo, " +
                            "cast(intPrimitive, java.lang.Long) as longOne, " +
                            "cast(intBoxed, long) as longTwo " +
                    "from TestEvent";
        
        EPStatement statement = epService.getEPAdministrator().createEPL(stmt);
        statement.addListener(listener);
        
        map = new HashMap<String, Object>();
        map.put("anInt","100");
        map.put("anDouble","1.4E-1");
        map.put("anLong","-10");
        map.put("anFloat","1.001");
        map.put("anByte","0x0A");
        map.put("anShort","223");
        map.put("intPrimitive",10);
        map.put("intBoxed",11);

        epService.getEPRuntime().sendEvent(map, "TestEvent");
        EventBean row = listener.assertOneGetNewAndReset();
        assertEquals(100, row.get("intVal"));
        assertEquals(0.14d, row.get("doubleVal"));
        assertEquals(-10L, row.get("longVal"));
        assertEquals(1.001f, row.get("floatVal"));
        assertEquals((byte)10, row.get("byteVal"));
        assertEquals((short)223, row.get("shortVal"));
        assertEquals(10, row.get("intOne"));
        assertEquals(11, row.get("intTwo"));
        assertEquals(10L, row.get("longOne"));
        assertEquals(11L, row.get("longTwo"));
    }

    private void assertResults(EventBean theEvent, Object[] result)
    {
        for (int i = 0; i < result.length; i++)
        {
            assertEquals("failed for index " + i, result[i], theEvent.get("t" + i));
        }
    }
}
