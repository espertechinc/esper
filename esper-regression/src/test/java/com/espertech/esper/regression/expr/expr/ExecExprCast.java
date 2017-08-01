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
package com.espertech.esper.regression.expr.expr;

import com.espertech.esper.client.EPException;
import com.espertech.esper.client.EPServiceProvider;
import com.espertech.esper.client.EPStatement;
import com.espertech.esper.client.EventBean;
import com.espertech.esper.client.scopetest.EPAssertionUtil;
import com.espertech.esper.client.scopetest.SupportUpdateListener;
import com.espertech.esper.client.soda.*;
import com.espertech.esper.supportregression.bean.*;
import com.espertech.esper.supportregression.execution.RegressionExecution;
import com.espertech.esper.supportregression.timer.SupportDateTimeUtil;
import com.espertech.esper.supportregression.util.SupportMessageAssertUtil;
import com.espertech.esper.supportregression.util.SupportModelHelper;
import com.espertech.esper.util.SerializableObjectCopier;
import org.junit.Assert;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.text.SimpleDateFormat;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;

import static junit.framework.TestCase.assertEquals;
import static org.junit.Assert.assertNull;

public class ExecExprCast implements RegressionExecution {
    public void run(EPServiceProvider epService) throws Exception {
        epService.getEPAdministrator().getConfiguration().addEventType(SupportBean.class);

        runAssertionCaseDates(epService);
        runAssertionCastSimple(epService);
        runAssertionCastAsParse(epService);
        runAssertionCastDoubleAndNull_OM(epService);
        runAssertionCastStringAndNull_Compile(epService);
        runAssertionCastInterface(epService);
        runAssertionCastBoolean(epService);
        runAssertionCastSimpleMoreTypes(epService);
    }

    private void runAssertionCastSimpleMoreTypes(EPServiceProvider epService) {
        String[] fields = "c0,c1,c2,c3,c4,c5,c6,c7,c8".split(",");
        EPStatement stmt = epService.getEPAdministrator().createEPL("select " +
                "cast(intPrimitive, float) as c0," +
                "cast(intPrimitive, short) as c1," +
                "cast(intPrimitive, byte) as c2," +
                "cast(theString, char) as c3," +
                "cast(theString, boolean) as c4," +
                "cast(intPrimitive, BigInteger) as c5," +
                "cast(intPrimitive, BigDecimal) as c6," +
                "cast(doublePrimitive, BigDecimal) as c7," +
                "cast(theString, char) as c8" +
                " from SupportBean");
        SupportUpdateListener listener = new SupportUpdateListener();
        stmt.addListener(listener);

        assertTypes(stmt, fields, Float.class, Short.class, Byte.class, Character.class, Boolean.class, BigInteger.class, BigDecimal.class, BigDecimal.class, Character.class);

        SupportBean bean = new SupportBean("true", 1);
        bean.setDoublePrimitive(1);
        epService.getEPRuntime().sendEvent(bean);
        EPAssertionUtil.assertProps(listener.assertOneGetNewAndReset(), fields, new Object[] {1.0f, (short) 1, (byte) 1, 't', true, BigInteger.valueOf(1), BigDecimal.valueOf(1), new BigDecimal(1d), 't'});
    }

    private void runAssertionCaseDates(EPServiceProvider epService) throws Exception {
        epService.getEPAdministrator().createEPL("create map schema MyType(yyyymmdd string, yyyymmddhhmmss string, hhmmss string, yyyymmddhhmmssvv string)");
        epService.getEPAdministrator().getConfiguration().addEventType(SupportBean_StringAlphabetic.class);

        runAssertionDatetimeBaseTypes(epService, true);
        runAssertionDatetimeBaseTypes(epService, false);
        runAssertionDatetimeJava8Types(epService);

        runAssertionDatetimeRenderOutCol(epService);

        runAssertionDynamicDateFormat(epService);

        runAssertionConstantDate(epService);

        runAssertionISO8601Date(epService);

        runAssertionDateformatNonString(epService);

        runAssertionDatetimeInvalid(epService);
    }

    private void runAssertionDateformatNonString(EPServiceProvider epService) {
        SupportDateTime sdt = SupportDateTime.make("2002-05-30T09:00:00.000");
        String sdfDate = SimpleDateFormat.getInstance().format(sdt.getUtildate());
        String ldtDate = sdt.getLocaldate().format(DateTimeFormatter.ISO_DATE_TIME);

        String epl = "select " +
                "cast('" + sdfDate + "',date,dateformat:SimpleDateFormat.getInstance()) as c0," +
                "cast('" + ldtDate + "',localdatetime,dateformat:java.time.format.DateTimeFormatter.ISO_DATE_TIME) as c1" +
                " from SupportBean";
        EPStatement stmt = epService.getEPAdministrator().createEPL(epl);
        SupportUpdateListener listener = new SupportUpdateListener();
        stmt.addListener(listener);

        epService.getEPRuntime().sendEvent(new SupportBean());
        EventBean event = listener.assertOneGetNewAndReset();
        EPAssertionUtil.assertProps(event, "c0,c1".split(","), new Object[]{sdt.getUtildate(), sdt.getLocaldate()});

        stmt.destroy();
    }

    private void runAssertionISO8601Date(EPServiceProvider epService) {
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
                "cast(theString,date,dateformat:'iso') as c10," +
                "cast('1997-07-16T19:20:30.45',localdatetime,dateformat:'iso') as c11," +
                "cast('1997-07-16T19:20:30+01:00',zoneddatetime,dateformat:'iso') as c12," +
                "cast('1997-07-16',localdate,dateformat:'iso') as c13," +
                "cast('19:20:30',localtime,dateformat:'iso') as c14" +
                " from SupportBean";
        EPStatement stmt = epService.getEPAdministrator().createEPL(epl);
        SupportUpdateListener listener = new SupportUpdateListener();
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
        assertEquals(LocalDateTime.parse("1997-07-16T19:20:30.45", DateTimeFormatter.ISO_DATE_TIME), event.get("c11"));
        assertEquals(ZonedDateTime.parse("1997-07-16T19:20:30+01:00", DateTimeFormatter.ISO_ZONED_DATE_TIME), event.get("c12"));
        assertEquals(LocalDate.parse("1997-07-16", DateTimeFormatter.ISO_DATE), event.get("c13"));
        assertEquals(LocalTime.parse("19:20:30", DateTimeFormatter.ISO_TIME), event.get("c14"));

        stmt.destroy();
    }

    private void runAssertionConstantDate(EPServiceProvider epService) throws Exception {
        String epl = "select cast('20030201',date,dateformat:\"yyyyMMdd\") as c0 from SupportBean";
        EPStatement stmt = epService.getEPAdministrator().createEPL(epl);
        SupportUpdateListener listener = new SupportUpdateListener();
        stmt.addListener(listener);

        SimpleDateFormat dateFormat = new SimpleDateFormat("yyyyMMdd");
        Date expectedDate = dateFormat.parse("20030201");
        epService.getEPRuntime().sendEvent(new SupportBean("E1", 1));
        EPAssertionUtil.assertProps(listener.assertOneGetNewAndReset(), "c0".split(","), new Object[]{expectedDate});

        stmt.destroy();
    }

    private void runAssertionDynamicDateFormat(EPServiceProvider epService) throws Exception {

        // try legacy date types
        String epl = "select " +
                "cast(a,date,dateformat:b) as c0," +
                "cast(a,long,dateformat:b) as c1," +
                "cast(a,calendar,dateformat:b) as c2" +
                " from SupportBean_StringAlphabetic";
        EPStatement stmt = epService.getEPAdministrator().createEPL(epl);
        SupportUpdateListener listener = new SupportUpdateListener();
        stmt.addListener(listener);

        runAssertionDynamicDateFormat(epService, listener, "20100502", "yyyyMMdd");
        runAssertionDynamicDateFormat(epService, listener, "20100502101112", "yyyyMMddhhmmss");
        runAssertionDynamicDateFormat(epService, listener, null, "yyyyMMdd");

        // invalid date
        try {
            epService.getEPRuntime().sendEvent(new SupportBean_StringAlphabetic("x", "yyyyMMddhhmmss"));
        } catch (EPException ex) {
            SupportMessageAssertUtil.assertMessageContains(ex, "Exception parsing date 'x' format 'yyyyMMddhhmmss': Unparseable date: \"x\"");
        }

        // invalid format
        try {
            epService.getEPRuntime().sendEvent(new SupportBean_StringAlphabetic("20100502", "UUHHYY"));
        } catch (EPException ex) {
            SupportMessageAssertUtil.assertMessageContains(ex, "Illegal pattern character 'U'");
        }

        stmt.destroy();

        // try java 8 types
        epService.getEPAdministrator().createEPL("create schema ValuesAndFormats(" +
                "ldt string, ldtf string," +
                "ld string, ldf string," +
                "lt string, ltf string," +
                "zdt string, zdtf string)");
        String eplExtended = "select " +
                "cast(ldt,localdatetime,dateformat:ldtf) as c0," +
                "cast(ld,localdate,dateformat:ldf) as c1," +
                "cast(lt,localtime,dateformat:ltf) as c2," +
                "cast(zdt,zoneddatetime,dateformat:zdtf) as c3 " +
                " from ValuesAndFormats";
        Map<String, Object> event = new HashMap<>();
        event.put("ldtf", "yyyyMMddHHmmss");
        event.put("ldt", "19990102030405");
        event.put("ldf", "yyyyMMdd");
        event.put("ld", "19990102");
        event.put("ltf", "HHmmss");
        event.put("lt", "030405");
        event.put("zdtf", "yyyyMMddHHmmssVV");
        event.put("zdt", "20100510141516America/Los_Angeles");
        EPStatement stmtExtended = epService.getEPAdministrator().createEPL(eplExtended);
        stmtExtended.addListener(listener);
        epService.getEPRuntime().sendEvent(event, "ValuesAndFormats");
        EPAssertionUtil.assertProps(listener.assertOneGetNewAndReset(), "c0,c1,c2,c3".split(","), new Object[]{
                LocalDateTime.parse("19990102030405", DateTimeFormatter.ofPattern("yyyyMMddHHmmss")),
                LocalDate.parse("19990102", DateTimeFormatter.ofPattern("yyyyMMdd")),
                LocalTime.parse("030405", DateTimeFormatter.ofPattern("HHmmss")),
                ZonedDateTime.parse("20100510141516America/Los_Angeles", DateTimeFormatter.ofPattern("yyyyMMddHHmmssVV")),
        });
        stmtExtended.destroy();
    }

    private void runAssertionDynamicDateFormat(EPServiceProvider epService, SupportUpdateListener listener, String date, String format) throws Exception {
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
                new Object[]{expectedDate, theLong, cal});
    }

    private void runAssertionDatetimeInvalid(EPServiceProvider epService) {
        // not a valid named parameter
        SupportMessageAssertUtil.tryInvalid(epService, "select cast(theString, date, x:1) from SupportBean",
                "Error starting statement: Failed to validate select-clause expression 'cast(theString,date,x:1)': Unexpected named parameter 'x', expecting any of the following: [dateformat]");

        // invalid date format
        SupportMessageAssertUtil.tryInvalid(epService, "select cast(theString, date, dateformat:'BBBBMMDD') from SupportBean",
                "Error starting statement: Failed to validate select-clause expression 'cast(theString,date,dateformat:\"BBB...(42 chars)': Invalid date format 'BBBBMMDD' (as obtained from new SimpleDateFormat): Illegal pattern character 'B'");
        SupportMessageAssertUtil.tryInvalid(epService, "select cast(theString, date, dateformat:1) from SupportBean",
                "Error starting statement: Failed to validate select-clause expression 'cast(theString,date,dateformat:1)': Failed to validate named parameter 'dateformat', expected a single expression returning any of the following types: string,DateFormat,DateTimeFormatter");

        // invalid input
        SupportMessageAssertUtil.tryInvalid(epService, "select cast(intPrimitive, date, dateformat:'yyyyMMdd') from SupportBean",
                "Error starting statement: Failed to validate select-clause expression 'cast(intPrimitive,date,dateformat:\"...(45 chars)': Use of the 'dateformat' named parameter requires a string-type input");

        // invalid target
        SupportMessageAssertUtil.tryInvalid(epService, "select cast(theString, int, dateformat:'yyyyMMdd') from SupportBean",
                "Error starting statement: Failed to validate select-clause expression 'cast(theString,int,dateformat:\"yyyy...(41 chars)': Use of the 'dateformat' named parameter requires a target type of calendar, date, long, localdatetime, localdate, localtime or zoneddatetime");

        // invalid parser
        SupportMessageAssertUtil.tryInvalid(epService, "select cast('xx', date, dateformat:java.time.format.DateTimeFormatter.ofPattern(\"yyyyMMddHHmmssVV\")) from SupportBean",
                "Error starting statement: Failed to validate select-clause expression 'cast(\"xx\",date,dateformat:java.time...(91 chars)': Invalid format, expected string-format or DateFormat but received java.time.format.DateTimeFormatter");
        SupportMessageAssertUtil.tryInvalid(epService, "select cast('xx', localdatetime, dateformat:SimpleDateFormat.getInstance()) from SupportBean",
                "Error starting statement: Failed to validate select-clause expression 'cast(\"xx\",localdatetime,dateformat:...(66 chars)': Invalid format, expected string-format or DateTimeFormatter but received java.text.SimpleDateFormat");
    }

    private void runAssertionDatetimeRenderOutCol(EPServiceProvider epService) {
        String epl = "select cast(yyyymmdd,date,dateformat:\"yyyyMMdd\") from MyType";
        EPStatement stmt = epService.getEPAdministrator().createEPL(epl);
        assertEquals("cast(yyyymmdd,date,dateformat:\"yyyyMMdd\")", stmt.getEventType().getPropertyNames()[0]);
        stmt.destroy();
    }

    private void runAssertionDatetimeJava8Types(EPServiceProvider epService) {
        String epl = "select " +
                "cast(yyyymmdd,localdate,dateformat:\"yyyyMMdd\") as c0, " +
                "cast(yyyymmdd,java.time.LocalDate,dateformat:\"yyyyMMdd\") as c1, " +
                "cast(yyyymmddhhmmss,localdatetime,dateformat:\"yyyyMMddHHmmss\") as c2, " +
                "cast(yyyymmddhhmmss,java.time.LocalDateTime,dateformat:\"yyyyMMddHHmmss\") as c3, " +
                "cast(hhmmss,localtime,dateformat:\"HHmmss\") as c4, " +
                "cast(hhmmss,java.time.LocalTime,dateformat:\"HHmmss\") as c5, " +
                "cast(yyyymmddhhmmssvv,zoneddatetime,dateformat:\"yyyyMMddHHmmssVV\") as c6, " +
                "cast(yyyymmddhhmmssvv,java.time.ZonedDateTime,dateformat:\"yyyyMMddHHmmssVV\") as c7 " +
                "from MyType";
        EPStatement stmt = SupportModelHelper.createByCompileOrParse(epService, false, epl);
        SupportUpdateListener listener = new SupportUpdateListener();
        stmt.addListener(listener);

        String yyyymmdd = "20100510";
        String yyyymmddhhmmss = "20100510141516";
        String hhmmss = "141516";
        String yyyymmddhhmmssvv = "20100510141516America/Los_Angeles";
        Map<String, Object> values = new HashMap<>();
        values.put("yyyymmdd", yyyymmdd);
        values.put("yyyymmddhhmmss", yyyymmddhhmmss);
        values.put("hhmmss", hhmmss);
        values.put("yyyymmddhhmmssvv", yyyymmddhhmmssvv);
        epService.getEPRuntime().sendEvent(values, "MyType");

        LocalDate resultLocalDate = LocalDate.parse(yyyymmdd, DateTimeFormatter.ofPattern("yyyyMMdd"));
        LocalDateTime resultLocalDateTime = LocalDateTime.parse(yyyymmddhhmmss, DateTimeFormatter.ofPattern("yyyyMMddHHmmss"));
        LocalTime resultLocalTime = LocalTime.parse(hhmmss, DateTimeFormatter.ofPattern("HHmmss"));
        ZonedDateTime resultZonedDateTime = ZonedDateTime.parse(yyyymmddhhmmssvv, DateTimeFormatter.ofPattern("yyyyMMddHHmmssVV"));

        EPAssertionUtil.assertProps(listener.assertOneGetNewAndReset(), "c0,c1,c2,c3,c4,c5,c6,c7".split(","), new Object[]{
            resultLocalDate, resultLocalDate,
            resultLocalDateTime, resultLocalDateTime,
            resultLocalTime, resultLocalTime,
            resultZonedDateTime, resultZonedDateTime});

        stmt.destroy();
    }

    private void runAssertionDatetimeBaseTypes(EPServiceProvider epService, boolean soda) throws Exception {
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
        SupportUpdateListener listener = new SupportUpdateListener();
        stmt.addListener(listener);

        Map<String, Object> values = new HashMap<>();
        values.put("yyyymmdd", "20100510");
        epService.getEPRuntime().sendEvent(values, "MyType");

        SimpleDateFormat formatYYYYMMdd = new SimpleDateFormat("yyyyMMdd");
        Date dateYYMMddDate = formatYYYYMMdd.parse("20100510");
        Calendar calYYMMddDate = Calendar.getInstance();
        calYYMMddDate.setTime(dateYYMMddDate);
        EPAssertionUtil.assertProps(listener.assertOneGetNewAndReset(), "c0,c1,c2,c3,c4,c5,c6,c7,c8".split(","), new Object[]{
            dateYYMMddDate, dateYYMMddDate, dateYYMMddDate.getTime(), dateYYMMddDate.getTime(),
            calYYMMddDate, calYYMMddDate, 4, 4, 4});

        stmt.destroy();
    }

    private void runAssertionCastSimple(EPServiceProvider epService) {
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
        SupportUpdateListener listener = new SupportUpdateListener();
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
        assertResults(theEvent, new Object[]{"abc", 3, 9.5f, "abc", 100, 100L, 100, 9L});

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
        assertResults(theEvent, new Object[]{null, null, null, null, 100, 100L, 100, null});

        // test cast with chained and null
        selectTestCase.destroy();
        stmtText = "select cast(one as " + SupportBean.class.getName() + ").getTheString() as t0," +
                "cast(null, " + SupportBean.class.getName() + ") as t1" +
                " from " + SupportBeanObject.class.getName();
        selectTestCase = epService.getEPAdministrator().createEPL(stmtText);
        selectTestCase.addListener(listener);

        epService.getEPRuntime().sendEvent(new SupportBeanObject(new SupportBean("E1", 1)));
        EPAssertionUtil.assertProps(listener.assertOneGetNewAndReset(), "t0,t1".split(","), new Object[]{"E1", null});
        assertEquals(SupportBean.class, selectTestCase.getEventType().getPropertyType("t1"));

        selectTestCase.destroy();
    }

    private void runAssertionCastAsParse(EPServiceProvider epService) {
        String stmtText = "select cast(theString, int) as t0 from " + SupportBean.class.getName();
        EPStatement selectTestCase = epService.getEPAdministrator().createEPL(stmtText);
        SupportUpdateListener listener = new SupportUpdateListener();
        selectTestCase.addListener(listener);

        assertEquals(Integer.class, selectTestCase.getEventType().getPropertyType("t0"));

        epService.getEPRuntime().sendEvent(new SupportBean("12", 1));
        EPAssertionUtil.assertProps(listener.assertOneGetNewAndReset(), "t0".split(","), new Object[]{12});

        selectTestCase.destroy();
    }

    private void runAssertionCastDoubleAndNull_OM(EPServiceProvider epService) throws Exception {
        String stmtText = "select cast(item?,double) as t0 " +
                "from " + SupportMarkerInterface.class.getName();

        EPStatementObjectModel model = new EPStatementObjectModel();
        model.setSelectClause(SelectClause.create().add(Expressions.cast("item?", "double"), "t0"));
        model.setFromClause(FromClause.create(FilterStream.create(SupportMarkerInterface.class.getName())));
        model = (EPStatementObjectModel) SerializableObjectCopier.copy(model);
        assertEquals(stmtText, model.toEPL());

        EPStatement selectTestCase = epService.getEPAdministrator().create(model);
        SupportUpdateListener listener = new SupportUpdateListener();
        selectTestCase.addListener(listener);

        assertEquals(Double.class, selectTestCase.getEventType().getPropertyType("t0"));

        epService.getEPRuntime().sendEvent(new SupportBeanDynRoot(100));
        assertEquals(100d, listener.assertOneGetNewAndReset().get("t0"));

        epService.getEPRuntime().sendEvent(new SupportBeanDynRoot((byte) 2));
        assertEquals(2d, listener.assertOneGetNewAndReset().get("t0"));

        epService.getEPRuntime().sendEvent(new SupportBeanDynRoot(77.7777));
        assertEquals(77.7777d, listener.assertOneGetNewAndReset().get("t0"));

        epService.getEPRuntime().sendEvent(new SupportBeanDynRoot(6L));
        assertEquals(6d, listener.assertOneGetNewAndReset().get("t0"));

        epService.getEPRuntime().sendEvent(new SupportBeanDynRoot(null));
        assertEquals(null, listener.assertOneGetNewAndReset().get("t0"));

        epService.getEPRuntime().sendEvent(new SupportBeanDynRoot("abc"));
        assertEquals(null, listener.assertOneGetNewAndReset().get("t0"));

        selectTestCase.destroy();
    }

    private void runAssertionCastStringAndNull_Compile(EPServiceProvider epService) throws Exception {
        String stmtText = "select cast(item?,java.lang.String) as t0 " +
                "from " + SupportMarkerInterface.class.getName();

        EPStatementObjectModel model = epService.getEPAdministrator().compileEPL(stmtText);
        model = (EPStatementObjectModel) SerializableObjectCopier.copy(model);
        EPStatement selectTestCase = epService.getEPAdministrator().create(model);
        SupportUpdateListener listener = new SupportUpdateListener();
        assertEquals(stmtText, model.toEPL());
        selectTestCase.addListener(listener);

        assertEquals(String.class, selectTestCase.getEventType().getPropertyType("t0"));

        epService.getEPRuntime().sendEvent(new SupportBeanDynRoot(100));
        assertEquals("100", listener.assertOneGetNewAndReset().get("t0"));

        epService.getEPRuntime().sendEvent(new SupportBeanDynRoot((byte) 2));
        assertEquals("2", listener.assertOneGetNewAndReset().get("t0"));

        epService.getEPRuntime().sendEvent(new SupportBeanDynRoot(77.7777));
        assertEquals("77.7777", listener.assertOneGetNewAndReset().get("t0"));

        epService.getEPRuntime().sendEvent(new SupportBeanDynRoot(6L));
        assertEquals("6", listener.assertOneGetNewAndReset().get("t0"));

        epService.getEPRuntime().sendEvent(new SupportBeanDynRoot(null));
        assertEquals(null, listener.assertOneGetNewAndReset().get("t0"));

        epService.getEPRuntime().sendEvent(new SupportBeanDynRoot("abc"));
        assertEquals("abc", listener.assertOneGetNewAndReset().get("t0"));

        selectTestCase.destroy();
    }

    private void runAssertionCastInterface(EPServiceProvider epService) {
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
        SupportUpdateListener listener = new SupportUpdateListener();
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
        assertResults(theEvent, new Object[]{bean, null, null, null, null, null, null, null});

        bean = new ISupportDImpl("", "", "");
        epService.getEPRuntime().sendEvent(new SupportBeanDynRoot(bean));
        theEvent = listener.assertOneGetNewAndReset();
        assertResults(theEvent, new Object[]{null, null, null, null, null, bean, null, null});

        bean = new ISupportBCImpl("", "", "");
        epService.getEPRuntime().sendEvent(new SupportBeanDynRoot(bean));
        theEvent = listener.assertOneGetNewAndReset();
        assertResults(theEvent, new Object[]{null, null, bean, null, bean, null, null, null});

        bean = new ISupportAImplSuperGImplPlus();
        epService.getEPRuntime().sendEvent(new SupportBeanDynRoot(bean));
        theEvent = listener.assertOneGetNewAndReset();
        assertResults(theEvent, new Object[]{null, bean, bean, null, bean, null, bean, bean});

        bean = new ISupportBaseABImpl("");
        epService.getEPRuntime().sendEvent(new SupportBeanDynRoot(bean));
        theEvent = listener.assertOneGetNewAndReset();
        assertResults(theEvent, new Object[]{null, null, bean, bean, null, null, null, null});

        selectTestCase.destroy();
    }

    private void runAssertionCastBoolean(EPServiceProvider epService) {
        String stmtText = "select cast(boolPrimitive as java.lang.Boolean) as t0, " +
                " cast(boolBoxed | boolPrimitive, boolean) as t1, " +
                " cast(boolBoxed, string) as t2 " +
                " from " + SupportBean.class.getName();

        EPStatement selectTestCase = epService.getEPAdministrator().createEPL(stmtText);
        SupportUpdateListener listener = new SupportUpdateListener();
        selectTestCase.addListener(listener);

        assertEquals(Boolean.class, selectTestCase.getEventType().getPropertyType("t0"));
        assertEquals(Boolean.class, selectTestCase.getEventType().getPropertyType("t1"));
        assertEquals(String.class, selectTestCase.getEventType().getPropertyType("t2"));

        SupportBean bean = new SupportBean("abc", 100);
        bean.setBoolPrimitive(true);
        bean.setBoolBoxed(true);
        epService.getEPRuntime().sendEvent(bean);
        EventBean theEvent = listener.assertOneGetNewAndReset();
        assertResults(theEvent, new Object[]{true, true, "true"});

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
        assertResults(theEvent, new Object[]{true, null, null});

        selectTestCase.destroy();
    }

    private void assertResults(EventBean theEvent, Object[] result) {
        for (int i = 0; i < result.length; i++) {
            assertEquals("failed for index " + i, result[i], theEvent.get("t" + i));
        }
    }

    private void assertTypes(EPStatement stmt, String[] fields, Class... types) {
        for (int i = 0; i < fields.length; i++) {
            Assert.assertEquals("failed for " + i, types[i], stmt.getEventType().getPropertyType(fields[i]));
        }
    }
}
