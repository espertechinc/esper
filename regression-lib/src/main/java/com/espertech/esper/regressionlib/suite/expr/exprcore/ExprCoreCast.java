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
package com.espertech.esper.regressionlib.suite.expr.exprcore;

import com.espertech.esper.common.client.EPException;
import com.espertech.esper.common.client.EventBean;
import com.espertech.esper.common.client.EventType;
import com.espertech.esper.common.client.scopetest.EPAssertionUtil;
import com.espertech.esper.common.client.soda.*;
import com.espertech.esper.common.internal.support.SupportBean;
import com.espertech.esper.common.internal.util.SerializableObjectCopier;
import com.espertech.esper.regressionlib.framework.RegressionEnvironment;
import com.espertech.esper.regressionlib.framework.RegressionExecution;
import com.espertech.esper.regressionlib.framework.RegressionPath;
import com.espertech.esper.regressionlib.framework.SupportMessageAssertUtil;
import com.espertech.esper.regressionlib.support.bean.*;
import com.espertech.esper.regressionlib.support.schedule.SupportDateTimeUtil;
import com.espertech.esper.runtime.client.EPStatement;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.Assert.*;

public class ExprCoreCast {

    public static Collection<RegressionExecution> executions() {
        ArrayList<RegressionExecution> executions = new ArrayList<>();
        executions.add(new ExprCoreCastDates());
        executions.add(new ExprCoreCastSimple());
        executions.add(new ExprCoreCastSimpleMoreTypes());
        executions.add(new ExprCoreCastAsParse());
        executions.add(new ExprCoreCastDates());
        executions.add(new ExprCoreDoubleAndNullOM());
        executions.add(new ExprCoreCastInterface());
        executions.add(new ExprCastStringAndNullCompile());
        executions.add(new ExprCoreCastBoolean());
        executions.add(new ExprCastWStaticType());
        executions.add(new ExprCastWArray(false));
        executions.add(new ExprCastWArray(true));
        return executions;
    }

    private static class ExprCastWArray implements RegressionExecution {
        private boolean soda;

        public ExprCastWArray(boolean soda) {
            this.soda = soda;
        }

        public void run(RegressionEnvironment env) {
            RegressionPath path = new RegressionPath();
            String epl =
                "create schema MyEvent(arr_string java.lang.Object, arr_primitive java.lang.Object, " +
                    "arr_boxed_one java.lang.Object, arr_boxed_two java.lang.Object, arr_object java.lang.Object," +
                    "arr_2dim_primitive java.lang.Object, arr_2dim_object java.lang.Object," +
                    "arr_3dim_primitive java.lang.Object, arr_3dim_object java.lang.Object" +
                    ");\n" +
                    "create schema MyArrayEvent as " + MyArrayEvent.class.getName() + ";\n";
            env.compileDeployWBusPublicType(epl, path);

            String insert = "@name('s0') insert into MyArrayEvent select " +
                "cast(arr_string, string[]) as c0, " +
                "cast(arr_primitive, int[primitive]) as c1, " +
                "cast(arr_boxed_one, int[]) as c2, " +
                "cast(arr_boxed_two, java.lang.Integer[]) as c3, " +
                "cast(arr_object, java.lang.Object[]) as c4," +
                "cast(arr_2dim_primitive, int[primitive][]) as c5," +
                "cast(arr_2dim_object, java.lang.Object[][]) as c6," +
                "cast(arr_3dim_primitive, int[primitive][][]) as c7," +
                "cast(arr_3dim_object, java.lang.Object[][][]) as c8 " +
                "from MyEvent";
            env.compileDeploy(soda, insert, path);

            EPStatement stmt = env.addListener("s0").statement("s0");
            EventType eventType = stmt.getEventType();
            assertEquals(String[].class, eventType.getPropertyType("c0"));
            assertEquals(int[].class, eventType.getPropertyType("c1"));
            assertEquals(Integer[].class, eventType.getPropertyType("c2"));
            assertEquals(Integer[].class, eventType.getPropertyType("c3"));
            assertEquals(Object[].class, eventType.getPropertyType("c4"));
            assertEquals(int[][].class, eventType.getPropertyType("c5"));
            assertEquals(Object[][].class, eventType.getPropertyType("c6"));
            assertEquals(int[][][].class, eventType.getPropertyType("c7"));
            assertEquals(Object[][][].class, eventType.getPropertyType("c8"));

            Map<String, Object> map = new HashMap<String, Object>();
            map.put("arr_string", new String[]{"a"});
            map.put("arr_primitive", new int[]{1});
            map.put("arr_boxed_one", new Integer[]{2});
            map.put("arr_boxed_two", new Integer[]{3});
            map.put("arr_object", new SupportBean[]{new SupportBean("E1", 0)});
            map.put("arr_2dim_primitive", new int[][]{{10}});
            map.put("arr_2dim_object", new Integer[][]{{11}});
            map.put("arr_3dim_primitive", new int[][][]{{{12}}});
            map.put("arr_3dim_object", new Integer[][][]{{{13}}});

            env.sendEventMap(map, "MyEvent");

            MyArrayEvent mae = (MyArrayEvent) env.listener("s0").assertOneGetNewAndReset().getUnderlying();
            assertEquals("a", mae.c0[0]);
            assertEquals(1, mae.c1[0]);
            assertEquals(2, mae.c2[0].intValue());
            assertEquals(3, mae.c3[0].intValue());
            assertEquals(new SupportBean("E1", 0), mae.c4[0]);
            assertEquals(10, mae.c5[0][0]);
            assertEquals(11, mae.c6[0][0]);
            assertEquals(12, mae.c7[0][0][0]);
            assertEquals(13, mae.c8[0][0][0]);

            env.sendEventMap(Collections.emptyMap(), "MyEvent");

            env.undeployAll();
        }
    }

    private static class ExprCastWStaticType implements RegressionExecution {
        public void run(RegressionEnvironment env) {
            String stmt = "@name('s0') select " +
                "cast(anInt, int) as intVal, " +
                "cast(anDouble, double) as doubleVal, " +
                "cast(anLong, long) as longVal, " +
                "cast(anFloat, float) as floatVal, " +
                "cast(anByte, byte) as byteVal, " +
                "cast(anShort, short) as shortVal, " +
                "cast(intPrimitive, int) as intOne, " +
                "cast(intBoxed, int) as intTwo, " +
                "cast(intPrimitive, java.lang.Long) as longOne, " +
                "cast(intBoxed, long) as longTwo " +
                "from StaticTypeMapEvent";

            env.compileDeploy(stmt).addListener("s0");

            Map<String, Object> map = new HashMap<String, Object>();
            map.put("anInt", "100");
            map.put("anDouble", "1.4E-1");
            map.put("anLong", "-10");
            map.put("anFloat", "1.001");
            map.put("anByte", "0x0A");
            map.put("anShort", "223");
            map.put("intPrimitive", 10);
            map.put("intBoxed", 11);

            env.sendEventMap(map, "StaticTypeMapEvent");
            EventBean row = env.listener("s0").assertOneGetNewAndReset();
            assertEquals(100, row.get("intVal"));
            assertEquals(0.14d, row.get("doubleVal"));
            assertEquals(-10L, row.get("longVal"));
            assertEquals(1.001f, row.get("floatVal"));
            assertEquals((byte) 10, row.get("byteVal"));
            assertEquals((short) 223, row.get("shortVal"));
            assertEquals(10, row.get("intOne"));
            assertEquals(11, row.get("intTwo"));
            assertEquals(10L, row.get("longOne"));
            assertEquals(11L, row.get("longTwo"));

            env.undeployAll();
        }
    }

    private static class ExprCoreCastSimpleMoreTypes implements RegressionExecution {
        public void run(RegressionEnvironment env) {
            String[] fields = "c0,c1,c2,c3,c4,c5,c6,c7,c8".split(",");
            String epl = "@Name('s0') select " +
                "cast(intPrimitive, float) as c0," +
                "cast(intPrimitive, short) as c1," +
                "cast(intPrimitive, byte) as c2," +
                "cast(theString, char) as c3," +
                "cast(theString, boolean) as c4," +
                "cast(intPrimitive, BigInteger) as c5," +
                "cast(intPrimitive, BigDecimal) as c6," +
                "cast(doublePrimitive, BigDecimal) as c7," +
                "cast(theString, char) as c8" +
                " from SupportBean";
            env.compileDeploy(epl).addListener("s0");

            assertTypes(env.statement("s0"), fields, Float.class, Short.class, Byte.class, Character.class, Boolean.class, BigInteger.class, BigDecimal.class, BigDecimal.class, Character.class);

            SupportBean bean = new SupportBean("true", 1);
            bean.setDoublePrimitive(1);
            env.sendEventBean(bean);
            EPAssertionUtil.assertProps(env.listener("s0").assertOneGetNewAndReset(), fields, new Object[]{1.0f, (short) 1, (byte) 1, 't', true, BigInteger.valueOf(1), BigDecimal.valueOf(1), new BigDecimal(1d), 't'});

            env.undeployAll();
        }
    }

    private static class ExprCoreCastSimple implements RegressionExecution {
        public void run(RegressionEnvironment env) {
            String epl = "@name('s0') select cast(theString as string) as t0, " +
                " cast(intBoxed, int) as t1, " +
                " cast(floatBoxed, java.lang.Float) as t2, " +
                " cast(theString, java.lang.String) as t3, " +
                " cast(intPrimitive, java.lang.Integer) as t4, " +
                " cast(intPrimitive, long) as t5, " +
                " cast(intPrimitive, java.lang.Number) as t6, " +
                " cast(floatBoxed, long) as t7 " +
                " from SupportBean";
            env.compileDeploy(epl).addListener("s0");

            EventType type = env.statement("s0").getEventType();
            assertEquals(String.class, type.getPropertyType("t0"));
            assertEquals(Integer.class, type.getPropertyType("t1"));
            assertEquals(Float.class, type.getPropertyType("t2"));
            assertEquals(String.class, type.getPropertyType("t3"));
            assertEquals(Integer.class, type.getPropertyType("t4"));
            assertEquals(Long.class, type.getPropertyType("t5"));
            assertEquals(Number.class, type.getPropertyType("t6"));
            assertEquals(Long.class, type.getPropertyType("t7"));

            SupportBean bean = new SupportBean("abc", 100);
            bean.setFloatBoxed(9.5f);
            bean.setIntBoxed(3);
            env.sendEventBean(bean);
            EventBean theEvent = env.listener("s0").assertOneGetNewAndReset();
            assertResults(theEvent, new Object[]{"abc", 3, 9.5f, "abc", 100, 100L, 100, 9L});

            bean = new SupportBean(null, 100);
            bean.setFloatBoxed(null);
            bean.setIntBoxed(null);
            env.sendEventBean(bean);
            theEvent = env.listener("s0").assertOneGetNewAndReset();
            assertResults(theEvent, new Object[]{null, null, null, null, 100, 100L, 100, null});
            bean = new SupportBean(null, 100);
            bean.setFloatBoxed(null);
            bean.setIntBoxed(null);
            env.sendEventBean(bean);
            theEvent = env.listener("s0").assertOneGetNewAndReset();
            assertResults(theEvent, new Object[]{null, null, null, null, 100, 100L, 100, null});

            env.undeployAll();

            // test cast with chained and null
            epl = "@name('s0') select cast(one as " + SupportBean.class.getName() + ").getTheString() as t0," +
                "cast(null, " + SupportBean.class.getName() + ") as t1" +
                " from SupportBeanObject";
            env.compileDeploy(epl).addListener("s0");

            env.sendEventBean(new SupportBeanObject(new SupportBean("E1", 1)));
            EPAssertionUtil.assertProps(env.listener("s0").assertOneGetNewAndReset(), "t0,t1".split(","), new Object[]{"E1", null});
            assertEquals(SupportBean.class, env.statement("s0").getEventType().getPropertyType("t1"));

            env.undeployAll();
        }
    }

    private static class ExprCoreDoubleAndNullOM implements RegressionExecution {
        public void run(RegressionEnvironment env) {
            String epl = "select cast(item?,double) as t0 from SupportBeanDynRoot";

            EPStatementObjectModel model = new EPStatementObjectModel();
            model.setSelectClause(SelectClause.create().add(Expressions.cast("item?", "double"), "t0"));
            model.setFromClause(FromClause.create(FilterStream.create(SupportBeanDynRoot.class.getSimpleName())));
            model = SerializableObjectCopier.copyMayFail(model);
            assertEquals(epl, model.toEPL());

            model.setAnnotations(Collections.singletonList(AnnotationPart.nameAnnotation("s0")));
            env.compileDeploy(model).addListener("s0");

            assertEquals(Double.class, env.statement("s0").getEventType().getPropertyType("t0"));

            env.sendEventBean(new SupportBeanDynRoot(100));
            assertEquals(100d, env.listener("s0").assertOneGetNewAndReset().get("t0"));

            env.sendEventBean(new SupportBeanDynRoot((byte) 2));
            assertEquals(2d, env.listener("s0").assertOneGetNewAndReset().get("t0"));

            env.sendEventBean(new SupportBeanDynRoot(77.7777));
            assertEquals(77.7777d, env.listener("s0").assertOneGetNewAndReset().get("t0"));

            env.sendEventBean(new SupportBeanDynRoot(6L));
            assertEquals(6d, env.listener("s0").assertOneGetNewAndReset().get("t0"));

            env.sendEventBean(new SupportBeanDynRoot(null));
            assertEquals(null, env.listener("s0").assertOneGetNewAndReset().get("t0"));

            env.sendEventBean(new SupportBeanDynRoot("abc"));
            assertEquals(null, env.listener("s0").assertOneGetNewAndReset().get("t0"));

            env.undeployAll();
        }
    }

    private static class ExprCoreCastDates implements RegressionExecution {
        public void run(RegressionEnvironment env) {
            AtomicInteger milestone = new AtomicInteger();

            runAssertionDatetimeBaseTypes(env, true, milestone);

            runAssertionDatetimeJava8Types(env, milestone);

            runAssertionDatetimeRenderOutCol(env, milestone);

            runAssertionDynamicDateFormat(env, milestone);

            runAssertionConstantDate(env, milestone);

            runAssertionISO8601Date(env, milestone);

            runAssertionDateformatNonString(env, milestone);

            runAssertionDatetimeInvalid(env);
        }
    }

    private static class ExprCoreCastAsParse implements RegressionExecution {
        public void run(RegressionEnvironment env) {
            String epl = "@name('s0') select cast(theString, int) as t0 from SupportBean";
            env.compileDeploy(epl).addListener("s0");

            assertEquals(Integer.class, env.statement("s0").getEventType().getPropertyType("t0"));

            env.sendEventBean(new SupportBean("12", 1));
            EPAssertionUtil.assertProps(env.listener("s0").assertOneGetNewAndReset(), "t0".split(","), new Object[]{12});

            env.undeployAll();
        }
    }

    private static class ExprCoreCastInterface implements RegressionExecution {
        public void run(RegressionEnvironment env) {
            String epl = "@name('s0') select cast(item?, " + SupportMarkerInterface.class.getName() + ") as t0, " +
                " cast(item?, " + ISupportA.class.getName() + ") as t1, " +
                " cast(item?, " + ISupportBaseAB.class.getName() + ") as t2, " +
                " cast(item?, " + ISupportBaseABImpl.class.getName() + ") as t3, " +
                " cast(item?, " + ISupportC.class.getName() + ") as t4, " +
                " cast(item?, " + ISupportD.class.getName() + ") as t5, " +
                " cast(item?, " + ISupportAImplSuperG.class.getName() + ") as t6, " +
                " cast(item?, " + ISupportAImplSuperGImplPlus.class.getName() + ") as t7 " +
                " from SupportBeanDynRoot";

            env.compileDeploy(epl).addListener("s0");

            EventType type = env.statement("s0").getEventType();
            assertEquals(SupportMarkerInterface.class, type.getPropertyType("t0"));
            assertEquals(ISupportA.class, type.getPropertyType("t1"));
            assertEquals(ISupportBaseAB.class, type.getPropertyType("t2"));
            assertEquals(ISupportBaseABImpl.class, type.getPropertyType("t3"));
            assertEquals(ISupportC.class, type.getPropertyType("t4"));
            assertEquals(ISupportD.class, type.getPropertyType("t5"));
            assertEquals(ISupportAImplSuperG.class, type.getPropertyType("t6"));
            assertEquals(ISupportAImplSuperGImplPlus.class, type.getPropertyType("t7"));

            Object bean = new SupportBeanDynRoot("abc");
            env.sendEventBean(new SupportBeanDynRoot(bean));
            EventBean theEvent = env.listener("s0").assertOneGetNewAndReset();
            assertResults(theEvent, new Object[]{bean, null, null, null, null, null, null, null});

            bean = new ISupportDImpl("", "", "");
            env.sendEventBean(new SupportBeanDynRoot(bean));
            theEvent = env.listener("s0").assertOneGetNewAndReset();
            assertResults(theEvent, new Object[]{null, null, null, null, null, bean, null, null});

            bean = new ISupportBCImpl("", "", "");
            env.sendEventBean(new SupportBeanDynRoot(bean));
            theEvent = env.listener("s0").assertOneGetNewAndReset();
            assertResults(theEvent, new Object[]{null, null, bean, null, bean, null, null, null});

            bean = new ISupportAImplSuperGImplPlus();
            env.sendEventBean(new SupportBeanDynRoot(bean));
            theEvent = env.listener("s0").assertOneGetNewAndReset();
            assertResults(theEvent, new Object[]{null, bean, bean, null, bean, null, bean, bean});

            bean = new ISupportBaseABImpl("");
            env.sendEventBean(new SupportBeanDynRoot(bean));
            theEvent = env.listener("s0").assertOneGetNewAndReset();
            assertResults(theEvent, new Object[]{null, null, bean, bean, null, null, null, null});

            env.undeployAll();
        }
    }

    private static class ExprCastStringAndNullCompile implements RegressionExecution {
        public void run(RegressionEnvironment env) {
            String epl = "@name('s0') select cast(item?,java.lang.String) as t0 from SupportBeanDynRoot";

            env.eplToModelCompileDeploy(epl).addListener("s0");

            assertEquals(String.class, env.statement("s0").getEventType().getPropertyType("t0"));

            env.sendEventBean(new SupportBeanDynRoot(100));
            assertEquals("100", env.listener("s0").assertOneGetNewAndReset().get("t0"));

            env.sendEventBean(new SupportBeanDynRoot((byte) 2));
            assertEquals("2", env.listener("s0").assertOneGetNewAndReset().get("t0"));

            env.sendEventBean(new SupportBeanDynRoot(77.7777));
            assertEquals("77.7777", env.listener("s0").assertOneGetNewAndReset().get("t0"));

            env.sendEventBean(new SupportBeanDynRoot(6L));
            assertEquals("6", env.listener("s0").assertOneGetNewAndReset().get("t0"));

            env.sendEventBean(new SupportBeanDynRoot(null));
            assertEquals(null, env.listener("s0").assertOneGetNewAndReset().get("t0"));

            env.sendEventBean(new SupportBeanDynRoot("abc"));
            assertEquals("abc", env.listener("s0").assertOneGetNewAndReset().get("t0"));

            env.undeployAll();
        }
    }

    private static class ExprCoreCastBoolean implements RegressionExecution {
        public void run(RegressionEnvironment env) {
            String epl = "@name('s0') select cast(boolPrimitive as java.lang.Boolean) as t0, " +
                " cast(boolBoxed | boolPrimitive, boolean) as t1, " +
                " cast(boolBoxed, string) as t2 " +
                " from SupportBean";
            env.compileDeploy(epl).addListener("s0");

            EventType type = env.statement("s0").getEventType();
            assertEquals(Boolean.class, type.getPropertyType("t0"));
            assertEquals(Boolean.class, type.getPropertyType("t1"));
            assertEquals(String.class, type.getPropertyType("t2"));

            SupportBean bean = new SupportBean("abc", 100);
            bean.setBoolPrimitive(true);
            bean.setBoolBoxed(true);
            env.sendEventBean(bean);
            EventBean theEvent = env.listener("s0").assertOneGetNewAndReset();
            assertResults(theEvent, new Object[]{true, true, "true"});

            bean = new SupportBean(null, 100);
            bean.setBoolPrimitive(false);
            bean.setBoolBoxed(false);
            env.sendEventBean(bean);
            theEvent = env.listener("s0").assertOneGetNewAndReset();
            assertResults(theEvent, new Object[]{false, false, "false"});

            bean = new SupportBean(null, 100);
            bean.setBoolPrimitive(true);
            bean.setBoolBoxed(null);
            env.sendEventBean(bean);
            theEvent = env.listener("s0").assertOneGetNewAndReset();
            assertResults(theEvent, new Object[]{true, null, null});

            env.undeployAll();
        }
    }

    private static void runAssertionDatetimeBaseTypes(RegressionEnvironment env, boolean soda, AtomicInteger milestone) {
        String epl = "@name('s0') select " +
            "cast(yyyymmdd,date,dateformat:\"yyyyMMdd\") as c0, " +
            "cast(yyyymmdd,java.util.Date,dateformat:\"yyyyMMdd\") as c1, " +
            "cast(yyyymmdd,long,dateformat:\"yyyyMMdd\") as c2, " +
            "cast(yyyymmdd,java.lang.Long,dateformat:\"yyyyMMdd\") as c3, " +
            "cast(yyyymmdd,calendar,dateformat:\"yyyyMMdd\") as c4, " +
            "cast(yyyymmdd,java.util.Calendar,dateformat:\"yyyyMMdd\") as c5, " +
            "cast(yyyymmdd,date,dateformat:\"yyyyMMdd\").get(\"month\") as c6, " +
            "cast(yyyymmdd,calendar,dateformat:\"yyyyMMdd\").get(\"month\") as c7, " +
            "cast(yyyymmdd,long,dateformat:\"yyyyMMdd\").get(\"month\") as c8 " +
            "from MyDateType";
        env.compileDeploy(soda, epl).addListener("s0").milestoneInc(milestone);

        Map<String, Object> values = new HashMap<>();
        values.put("yyyymmdd", "20100510");
        env.sendEventMap(values, "MyDateType");

        SimpleDateFormat formatYYYYMMdd = new SimpleDateFormat("yyyyMMdd");
        Date dateYYMMddDate = null;
        try {
            dateYYMMddDate = formatYYYYMMdd.parse("20100510");
        } catch (ParseException e) {
            fail();
        }
        Calendar calYYMMddDate = Calendar.getInstance();
        calYYMMddDate.setTime(dateYYMMddDate);
        EPAssertionUtil.assertProps(env.listener("s0").assertOneGetNewAndReset(), "c0,c1,c2,c3,c4,c5,c6,c7,c8".split(","), new Object[]{
            dateYYMMddDate, dateYYMMddDate, dateYYMMddDate.getTime(), dateYYMMddDate.getTime(),
            calYYMMddDate, calYYMMddDate, 4, 4, 4});

        env.undeployAll();
    }

    private static void runAssertionDatetimeJava8Types(RegressionEnvironment env, AtomicInteger milestone) {
        String epl = "@name('s0') select " +
            "cast(yyyymmdd,localdate,dateformat:\"yyyyMMdd\") as c0, " +
            "cast(yyyymmdd,java.time.LocalDate,dateformat:\"yyyyMMdd\") as c1, " +
            "cast(yyyymmddhhmmss,localdatetime,dateformat:\"yyyyMMddHHmmss\") as c2, " +
            "cast(yyyymmddhhmmss,java.time.LocalDateTime,dateformat:\"yyyyMMddHHmmss\") as c3, " +
            "cast(hhmmss,localtime,dateformat:\"HHmmss\") as c4, " +
            "cast(hhmmss,java.time.LocalTime,dateformat:\"HHmmss\") as c5, " +
            "cast(yyyymmddhhmmssvv,zoneddatetime,dateformat:\"yyyyMMddHHmmssVV\") as c6, " +
            "cast(yyyymmddhhmmssvv,java.time.ZonedDateTime,dateformat:\"yyyyMMddHHmmssVV\") as c7 " +
            "from MyDateType";
        env.compileDeployAddListenerMile(epl, "s0", milestone.getAndIncrement());

        String yyyymmdd = "20100510";
        String yyyymmddhhmmss = "20100510141516";
        String hhmmss = "141516";
        String yyyymmddhhmmssvv = "20100510141516America/Los_Angeles";
        Map<String, Object> values = new HashMap<>();
        values.put("yyyymmdd", yyyymmdd);
        values.put("yyyymmddhhmmss", yyyymmddhhmmss);
        values.put("hhmmss", hhmmss);
        values.put("yyyymmddhhmmssvv", yyyymmddhhmmssvv);
        env.sendEventMap(values, "MyDateType");

        LocalDate resultLocalDate = LocalDate.parse(yyyymmdd, DateTimeFormatter.ofPattern("yyyyMMdd"));
        LocalDateTime resultLocalDateTime = LocalDateTime.parse(yyyymmddhhmmss, DateTimeFormatter.ofPattern("yyyyMMddHHmmss"));
        LocalTime resultLocalTime = LocalTime.parse(hhmmss, DateTimeFormatter.ofPattern("HHmmss"));
        ZonedDateTime resultZonedDateTime = ZonedDateTime.parse(yyyymmddhhmmssvv, DateTimeFormatter.ofPattern("yyyyMMddHHmmssVV"));

        EPAssertionUtil.assertProps(env.listener("s0").assertOneGetNewAndReset(), "c0,c1,c2,c3,c4,c5,c6,c7".split(","), new Object[]{
            resultLocalDate, resultLocalDate,
            resultLocalDateTime, resultLocalDateTime,
            resultLocalTime, resultLocalTime,
            resultZonedDateTime, resultZonedDateTime});

        env.undeployAll();
    }

    private static void runAssertionDynamicDateFormat(RegressionEnvironment env, AtomicInteger milestone) {

        // try legacy date types
        String epl = "@name('s0') select " +
            "cast(a,date,dateformat:b) as c0," +
            "cast(a,long,dateformat:b) as c1," +
            "cast(a,calendar,dateformat:b) as c2" +
            " from SupportBean_StringAlphabetic";

        env.compileDeploy(epl).addListener("s0").milestone(milestone.getAndIncrement());

        assertDynamicDateFormat(env, "20100502", "yyyyMMdd");
        assertDynamicDateFormat(env, "20100502101112", "yyyyMMddhhmmss");
        assertDynamicDateFormat(env, null, "yyyyMMdd");

        // invalid date
        try {
            env.sendEventBean(new SupportBean_StringAlphabetic("x", "yyyyMMddhhmmss"));
        } catch (EPException ex) {
            SupportMessageAssertUtil.assertMessageContains(ex, "Exception parsing date 'x' format 'yyyyMMddhhmmss': Unparseable date: \"x\"");
        }

        // invalid format
        try {
            env.sendEventBean(new SupportBean_StringAlphabetic("20100502", "UUHHYY"));
        } catch (EPException ex) {
            SupportMessageAssertUtil.assertMessageContains(ex, "Illegal pattern character 'U'");
        }

        env.undeployAll();

        // try java 8 types
        epl = "create schema ValuesAndFormats(" +
            "ldt string, ldtf string," +
            "ld string, ldf string," +
            "lt string, ltf string," +
            "zdt string, zdtf string)";
        RegressionPath path = new RegressionPath();
        env.compileDeployWBusPublicType(epl, path);

        String eplExtended = "@name('s0') select " +
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
        env.compileDeploy(eplExtended, path).addListener("s0");
        env.sendEventMap(event, "ValuesAndFormats");
        EPAssertionUtil.assertProps(env.listener("s0").assertOneGetNewAndReset(), "c0,c1,c2,c3".split(","), new Object[]{
            LocalDateTime.parse("19990102030405", DateTimeFormatter.ofPattern("yyyyMMddHHmmss")),
            LocalDate.parse("19990102", DateTimeFormatter.ofPattern("yyyyMMdd")),
            LocalTime.parse("030405", DateTimeFormatter.ofPattern("HHmmss")),
            ZonedDateTime.parse("20100510141516America/Los_Angeles", DateTimeFormatter.ofPattern("yyyyMMddHHmmssVV")),
        });
        env.undeployAll();
    }

    private static void runAssertionDatetimeRenderOutCol(RegressionEnvironment env, AtomicInteger milestone) {
        String epl = "@name('s0') select cast(yyyymmdd,date,dateformat:\"yyyyMMdd\") from MyDateType";
        env.compileDeploy(epl).addListener("s0").milestone(milestone.getAndIncrement());
        assertEquals("cast(yyyymmdd,date,dateformat:\"yyyyMMdd\")", env.statement("s0").getEventType().getPropertyNames()[0]);
        env.undeployAll();
    }

    private static void assertDynamicDateFormat(RegressionEnvironment env, String date, String format) {
        env.sendEventBean(new SupportBean_StringAlphabetic(date, format));

        SimpleDateFormat dateFormat = new SimpleDateFormat(format);
        Date expectedDate = null;
        try {
            expectedDate = date == null ? null : dateFormat.parse(date);
        } catch (ParseException e) {
            fail(e.getMessage());
        }
        Calendar cal = null;
        Long theLong = null;
        if (expectedDate != null) {
            cal = Calendar.getInstance();
            cal.setTime(expectedDate);
            theLong = expectedDate.getTime();
        }
        EPAssertionUtil.assertProps(env.listener("s0").assertOneGetNewAndReset(), "c0,c1,c2".split(","),
            new Object[]{expectedDate, theLong, cal});
    }

    private static void runAssertionConstantDate(RegressionEnvironment env, AtomicInteger milestone) {
        String epl = "@name('s0') select cast('20030201',date,dateformat:\"yyyyMMdd\") as c0 from SupportBean";
        env.compileDeployAddListenerMile(epl, "s0", milestone.getAndIncrement());

        SimpleDateFormat dateFormat = new SimpleDateFormat("yyyyMMdd");
        Date expectedDate = null;
        try {
            expectedDate = dateFormat.parse("20030201");
        } catch (ParseException e) {
            fail(e.getMessage());
        }
        env.sendEventBean(new SupportBean("E1", 1));
        EPAssertionUtil.assertProps(env.listener("s0").assertOneGetNewAndReset(), "c0".split(","), new Object[]{expectedDate});

        env.undeployAll();
    }

    private static void runAssertionISO8601Date(RegressionEnvironment env, AtomicInteger milestone) {
        String epl = "@name('s0') select " +
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
        env.compileDeployAddListenerMile(epl, "s0", milestone.getAndIncrement());

        env.sendEventBean(new SupportBean());
        EventBean event = env.listener("s0").assertOneGetNewAndReset();
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

        env.undeployAll();
    }

    private static void runAssertionDateformatNonString(RegressionEnvironment env, AtomicInteger milestone) {
        SupportDateTime sdt = SupportDateTime.make("2002-05-30T09:00:00.000");
        String sdfDate = SimpleDateFormat.getInstance().format(sdt.getUtildate());
        String ldtDate = sdt.getLocaldate().format(DateTimeFormatter.ISO_DATE_TIME);
        String zdtDate = sdt.getZoneddate().format(DateTimeFormatter.ISO_ZONED_DATE_TIME);
        String ldDate = sdt.getLocaldate().toLocalDate().format(DateTimeFormatter.ISO_DATE);
        String ltDate = sdt.getLocaldate().toLocalTime().format(DateTimeFormatter.ISO_TIME);

        String epl = "@name('s0') select " +
            "cast('" + sdfDate + "',date,dateformat:SimpleDateFormat.getInstance()) as c0," +
            "cast('" + sdfDate + "',calendar,dateformat:SimpleDateFormat.getInstance()) as c1," +
            "cast('" + sdfDate + "',long,dateformat:SimpleDateFormat.getInstance()) as c2," +
            "cast('" + ldtDate + "',localdatetime,dateformat:java.time.format.DateTimeFormatter.ISO_DATE_TIME) as c3," +
            "cast('" + zdtDate + "',zoneddatetime,dateformat:java.time.format.DateTimeFormatter.ISO_ZONED_DATE_TIME) as c4," +
            "cast('" + ldDate + "',localdate,dateformat:java.time.format.DateTimeFormatter.ISO_DATE) as c5," +
            "cast('" + ltDate + "',localtime,dateformat:java.time.format.DateTimeFormatter.ISO_TIME) as c6" +
            " from SupportBean";
        env.compileDeployAddListenerMile(epl, "s0", milestone.getAndIncrement());

        env.sendEventBean(new SupportBean());
        EventBean event = env.listener("s0").assertOneGetNewAndReset();
        EPAssertionUtil.assertProps(event, "c0,c1,c2,c3,c4,c5,c6".split(","), new Object[]{sdt.getUtildate(), sdt.getCaldate(),
            sdt.getLongdate(), sdt.getLocaldate(), sdt.getZoneddate(), sdt.getLocaldate().toLocalDate(), sdt.getLocaldate().toLocalTime()});

        env.undeployAll();
    }

    private static void runAssertionDatetimeInvalid(RegressionEnvironment env) {
        // not a valid named parameter
        SupportMessageAssertUtil.tryInvalidCompile(env, "select cast(theString, date, x:1) from SupportBean",
            "Failed to validate select-clause expression 'cast(theString,date,x:1)': Unexpected named parameter 'x', expecting any of the following: [dateformat]");

        // invalid date format
        SupportMessageAssertUtil.tryInvalidCompile(env, "select cast(theString, date, dateformat:'BBBBMMDD') from SupportBean",
            "Failed to validate select-clause expression 'cast(theString,date,dateformat:\"BBB...(42 chars)': Invalid date format 'BBBBMMDD' (as obtained from new SimpleDateFormat): Illegal pattern character 'B'");
        SupportMessageAssertUtil.tryInvalidCompile(env, "select cast(theString, date, dateformat:1) from SupportBean",
            "Failed to validate select-clause expression 'cast(theString,date,dateformat:1)': Failed to validate named parameter 'dateformat', expected a single expression returning any of the following types: string,DateFormat,DateTimeFormatter");

        // invalid input
        SupportMessageAssertUtil.tryInvalidCompile(env, "select cast(intPrimitive, date, dateformat:'yyyyMMdd') from SupportBean",
            "Failed to validate select-clause expression 'cast(intPrimitive,date,dateformat:\"...(45 chars)': Use of the 'dateformat' named parameter requires a string-type input");

        // invalid target
        SupportMessageAssertUtil.tryInvalidCompile(env, "select cast(theString, int, dateformat:'yyyyMMdd') from SupportBean",
            "Failed to validate select-clause expression 'cast(theString,int,dateformat:\"yyyy...(41 chars)': Use of the 'dateformat' named parameter requires a target type of calendar, date, long, localdatetime, localdate, localtime or zoneddatetime");

        // invalid parser
        SupportMessageAssertUtil.tryInvalidCompile(env, "select cast('xx', date, dateformat:java.time.format.DateTimeFormatter.ofPattern(\"yyyyMMddHHmmssVV\")) from SupportBean",
            "Failed to validate select-clause expression 'cast(\"xx\",date,dateformat:java.time...(91 chars)': Invalid format, expected string-format or DateFormat but received java.time.format.DateTimeFormatter");
        SupportMessageAssertUtil.tryInvalidCompile(env, "select cast('xx', localdatetime, dateformat:SimpleDateFormat.getInstance()) from SupportBean",
            "Failed to validate select-clause expression 'cast(\"xx\",localdatetime,dateformat:...(66 chars)': Invalid format, expected string-format or DateTimeFormatter but received java.text.DateFormat");
    }

    private static void assertResults(EventBean theEvent, Object[] result) {
        for (int i = 0; i < result.length; i++) {
            assertEquals("failed for index " + i, result[i], theEvent.get("t" + i));
        }
    }

    private static void assertTypes(EPStatement stmt, String[] fields, Class... types) {
        for (int i = 0; i < fields.length; i++) {
            assertEquals("failed for " + i, types[i], stmt.getEventType().getPropertyType(fields[i]));
        }
    }

    public final static class MyArrayEvent {
        private final String[] c0;
        private final int[] c1;
        private final Integer[] c2;
        private final Integer[] c3;
        private final Object[] c4;
        private final int[][] c5;
        private final Object[][] c6;
        private final int[][][] c7;
        private final Object[][][] c8;

        public MyArrayEvent(String[] c0, int[] c1, Integer[] c2, Integer[] c3, Object[] c4, int[][] c5, Object[][] c6, int[][][] c7, Object[][][] c8) {
            this.c0 = c0;
            this.c1 = c1;
            this.c2 = c2;
            this.c3 = c3;
            this.c4 = c4;
            this.c5 = c5;
            this.c6 = c6;
            this.c7 = c7;
            this.c8 = c8;
        }

        public String[] getC0() {
            return c0;
        }

        public int[] getC1() {
            return c1;
        }

        public Integer[] getC2() {
            return c2;
        }

        public Integer[] getC3() {
            return c3;
        }

        public Object[] getC4() {
            return c4;
        }

        public int[][] getC5() {
            return c5;
        }

        public Object[][] getC6() {
            return c6;
        }

        public int[][][] getC7() {
            return c7;
        }

        public Object[][][] getC8() {
            return c8;
        }
    }
}
