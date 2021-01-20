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
package com.espertech.esper.regressionlib.suite.epl.other;

import com.espertech.esper.common.client.EventBean;
import com.espertech.esper.common.client.EventPropertyDescriptor;
import com.espertech.esper.common.client.hook.expr.EPLMethodInvocationContext;
import com.espertech.esper.common.client.soda.*;
import com.espertech.esper.common.internal.support.SupportBean;
import com.espertech.esper.common.internal.support.SupportBean_S0;
import com.espertech.esper.common.internal.util.SerializableObjectCopier;
import com.espertech.esper.regressionlib.framework.RegressionEnvironment;
import com.espertech.esper.regressionlib.framework.RegressionExecution;
import com.espertech.esper.regressionlib.framework.RegressionPath;
import com.espertech.esper.regressionlib.support.bean.SupportChainTop;
import com.espertech.esper.regressionlib.support.bean.SupportMarketDataBean;
import com.espertech.esper.regressionlib.support.bean.SupportTemperatureBean;
import com.espertech.esper.regressionlib.support.epl.SupportStaticMethodLib;
import org.junit.Assert;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import static junit.framework.TestCase.assertTrue;

public class EPLOtherStaticFunctions {
    private final static String STREAM_MDB_LEN5 = " from SupportMarketDataBean#length(5) ";

    public static List<RegressionExecution> executions() {
        List<RegressionExecution> execs = new ArrayList<>();
        execs.add(new EPLOtherNullPrimitive());
        execs.add(new EPLOtherChainedInstance());
        execs.add(new EPLOtherChainedStatic());
        execs.add(new EPLOtherEscape());
        execs.add(new EPLOtherReturnsMapIndexProperty());
        execs.add(new EPLOtherPattern());
        execs.add(new EPLOtherRuntimeException());
        execs.add(new EPLOtherArrayParameter());
        execs.add(new EPLOtherNoParameters());
        execs.add(new EPLOtherPerfConstantParameters());
        execs.add(new EPLOtherPerfConstantParametersNested());
        execs.add(new EPLOtherSingleParameterOM());
        execs.add(new EPLOtherSingleParameterCompile());
        execs.add(new EPLOtherSingleParameter());
        execs.add(new EPLOtherTwoParameters());
        execs.add(new EPLOtherUserDefined());
        execs.add(new EPLOtherComplexParameters());
        execs.add(new EPLOtherMultipleMethodInvocations());
        execs.add(new EPLOtherOtherClauses());
        execs.add(new EPLOtherNestedFunction());
        execs.add(new EPLOtherPassthru());
        execs.add(new EPLOtherPrimitiveConversion());
        execs.add(new EPLOtherStaticFuncWCurrentTimeStamp());
        execs.add(new EPLOtherStaticFuncEnumConstant());
        return execs;
    }

    private static class EPLOtherStaticFuncEnumConstant implements RegressionExecution {
        public void run(RegressionEnvironment env) {
            String epl =
                "create map schema MyEvent(someDate Date, dateFrom string, dateTo string, minutesOfDayFrom int, minutesOfDayTo int, daysOfWeek string);\n" +
                    "select " +
                    "java.time.ZonedDateTime.ofInstant(someDate.toInstant(),java.time.ZoneId.of('CET')).isAfter(cast(dateFrom||'T00:00:00Z', zoneddatetime, dateformat:java.time.format.DateTimeFormatter.ISO_OFFSET_DATE_TIME.withZone(java.time.ZoneId.of('CET')))) as c0,\n" +
                    "java.time.ZonedDateTime.ofInstant(someDate.toInstant(),java.time.ZoneId.of('CET')).isBefore(cast(dateTo||'T00:00:00Z', zoneddatetime, dateformat:java.time.format.DateTimeFormatter.ISO_OFFSET_DATE_TIME.withZone(java.time.ZoneId.of('CET')))) as c1,\n" +
                    "java.time.ZonedDateTime.ofInstant(someDate.toInstant(),java.time.ZoneId.of('CET')).get(java.time.temporal.ChronoField.MINUTE_OF_DAY)>= minutesOfDayFrom as c2,\n" +
                    "java.time.ZonedDateTime.ofInstant(someDate.toInstant(),java.time.ZoneId.of('CET')).get(java.time.temporal.ChronoField.MINUTE_OF_DAY)<= minutesOfDayTo as c3,\n" +
                    "daysOfWeek.contains(java.lang.String.valueOf(java.time.ZonedDateTime.ofInstant(someDate.toInstant(),java.time.ZoneId.of('CET')).getDayOfWeek().getValue())) as c4\n" +
                    "from MyEvent";
            env.compile(epl);
        }
    }

    private static class EPLOtherStaticFuncWCurrentTimeStamp implements RegressionExecution {
        public void run(RegressionEnvironment env) {
            env.advanceTime(1000);
            String epl = "@name('s0') select java.time.format.DateTimeFormatter.ISO_INSTANT.format(java.time.Instant.ofEpochMilli(current_timestamp())) as c0 from SupportBean";
            env.compileDeploy(epl).addListener("s0");

            env.sendEventBean(new SupportBean());
            env.assertEqualsNew("s0", "c0", "1970-01-01T00:00:01Z");

            env.undeployAll();
        }
    }

    private static class EPLOtherPrimitiveConversion implements RegressionExecution {
        public void run(RegressionEnvironment env) {
            env.compileDeploy("@name('s0') select " +
                "PrimitiveConversionLib.passIntAsObject(intPrimitive) as c0," +
                "PrimitiveConversionLib.passIntAsNumber(intPrimitive) as c1," +
                "PrimitiveConversionLib.passIntAsComparable(intPrimitive) as c2," +
                "PrimitiveConversionLib.passIntAsSerializable(intPrimitive) as c3" +
                " from SupportBean").addListener("s0");

            env.sendEventBean(new SupportBean("E1", 10));
            env.assertPropsNew("s0", "c0,c1,c2,c3".split(","), new Object[]{10, 10, 10, 10});

            env.undeployAll();
        }
    }

    private static class EPLOtherNullPrimitive implements RegressionExecution {
        public void run(RegressionEnvironment env) {
            // test passing null
            env.compileDeploy("@name('s0') select NullPrimitive.getValue(intBoxed) from SupportBean").addListener("s0");

            env.sendEventBean(new SupportBean());

            env.undeployAll();
        }
    }

    private static class EPLOtherChainedInstance implements RegressionExecution {
        public void run(RegressionEnvironment env) {
            env.compileDeploy("@name('s0') select " +
                "LevelZero.getLevelOne().getLevelTwoValue() as val0 " +
                "from SupportBean").addListener("s0");

            LevelOne.setField("v1");
            env.sendEventBean(new SupportBean());
            env.assertPropsNew("s0", "val0".split(","), new Object[]{"v1"});

            LevelOne.setField("v2");
            env.sendEventBean(new SupportBean());
            env.assertPropsNew("s0", "val0".split(","), new Object[]{"v2"});

            env.undeployAll();
        }
    }

    private static class EPLOtherChainedStatic implements RegressionExecution {
        public void run(RegressionEnvironment env) {

            String subexp = "SupportChainTop.make().getChildOne(\"abc\",1).getChildTwo(\"def\").getText()";
            String statementText = "@name('s0') select " + subexp + " from SupportBean";
            env.compileDeploy(statementText).addListener("s0");

            Object[][] rows = new Object[][]{
                {subexp, String.class}
            };
            env.assertStatement("s0", statement -> {
                EventPropertyDescriptor[] prop = statement.getEventType().getPropertyDescriptors();
                for (int i = 0; i < rows.length; i++) {
                    Assert.assertEquals(rows[i][0], prop[i].getPropertyName());
                    Assert.assertEquals(rows[i][1], prop[i].getPropertyType());
                }
            });

            env.sendEventBean(new SupportBean());
            env.assertPropsNew("s0", new String[]{subexp},
                new Object[]{SupportChainTop.make().getChildOne("abc", 1).getChildTwo("def").getText()});

            env.undeployAll();
        }
    }

    private static class EPLOtherEscape implements RegressionExecution {
        public void run(RegressionEnvironment env) {
            String statementText = "@name('s0') select SupportStaticMethodLib.`join`(abcstream) as value from SupportBean abcstream";
            env.compileDeploy(statementText).addListener("s0");

            env.sendEventBean(new SupportBean("E1", 99));

            env.assertPropsNew("s0", "value".split(","), new Object[]{"E1 99"});

            env.undeployAll();
        }
    }

    private static class EPLOtherReturnsMapIndexProperty implements RegressionExecution {
        public void run(RegressionEnvironment env) {
            RegressionPath path = new RegressionPath();
            String statementText = "@public insert into ABCStream select SupportStaticMethodLib.myMapFunc() as mymap, SupportStaticMethodLib.myArrayFunc() as myindex from SupportBean";
            env.compileDeploy(statementText, path);

            statementText = "@name('s0') select mymap('A') as v0, myindex[1] as v1 from ABCStream";
            env.compileDeploy(statementText, path).addListener("s0");

            env.sendEventBean(new SupportBean());

            env.assertPropsNew("s0", "v0,v1".split(","), new Object[]{"A1", 200});

            env.undeployAll();
        }
    }

    private static class EPLOtherPattern implements RegressionExecution {
        public void run(RegressionEnvironment env) {
            String className = SupportStaticMethodLib.class.getName();
            String statementText = "@name('s0') select * from pattern [myevent=SupportBean(" +
                className + ".delimitPipe(theString) = '|a|')]";
            env.compileDeploy(statementText).addListener("s0");

            env.sendEventBean(new SupportBean("b", 0));
            env.assertListenerNotInvoked("s0");
            env.sendEventBean(new SupportBean("a", 0));
            env.assertListenerInvoked("s0");

            env.undeployAll();
            statementText = "@name('s0') select * from pattern [myevent=SupportBean(" +
                className + ".delimitPipe(null) = '|<null>|')]";
            env.compileDeploy(statementText).addListener("s0");

            env.sendEventBean(new SupportBean("a", 0));
            env.assertListenerInvoked("s0");

            env.undeployAll();
        }
    }

    private static class EPLOtherRuntimeException implements RegressionExecution {
        public void run(RegressionEnvironment env) {
            String className = SupportStaticMethodLib.class.getName();
            String statementText = "@name('s0') select price, " + className + ".throwException() as value " + STREAM_MDB_LEN5;
            env.compileDeploy(statementText).addListener("s0");

            sendEvent(env, "IBM", 10d, 4L);
            env.assertEqualsNew("s0", "value", null);

            env.undeployAll();
        }
    }

    private static class EPLOtherArrayParameter implements RegressionExecution {
        public void run(RegressionEnvironment env) {
            String text = "@name('s0') select " +
                "SupportStaticMethodLib.arraySumIntBoxed({1,2,null,3,4}) as v1, " +
                "SupportStaticMethodLib.arraySumDouble({1,2,3,4.0}) as v2, " +
                "SupportStaticMethodLib.arraySumString({'1','2','3','4'}) as v3, " +
                "SupportStaticMethodLib.arraySumObject({'1',2,3.0,'4.0'}) as v4 " +
                " from SupportBean";
            env.compileDeploy(text).addListener("s0");

            env.sendEventBean(new SupportBean("E1", 1));
            env.assertPropsNew("s0", "v1,v2,v3,v4".split(","), new Object[]{10, 10d, 10d, 10d});

            env.undeployAll();
        }
    }

    private static class EPLOtherNoParameters implements RegressionExecution {
        public void run(RegressionEnvironment env) {
            Long startTime = System.currentTimeMillis();
            String statementText = "@name('s0') select System.currentTimeMillis() " + STREAM_MDB_LEN5;
            env.compileDeploy(statementText).addListener("s0");

            env.sendEventBean(new SupportMarketDataBean("IBM", 10d, 4L, ""));
            env.assertEventNew("s0", event -> {
                Long result = (Long) event.get("System.currentTimeMillis()");
                Long finishTime = System.currentTimeMillis();
                assertTrue(startTime <= result);
                assertTrue(result <= finishTime);
            });

            env.undeployAll();

            statementText = "@name('s0') select java.lang.ClassLoader.getSystemClassLoader() " + STREAM_MDB_LEN5;
            env.compileDeploy(statementText).addListener("s0");

            Object expected = ClassLoader.getSystemClassLoader();
            sendEvent(env, "IBM", 10d, 4L);
            env.assertEqualsNew("s0", "java.lang.ClassLoader.getSystemClassLoader()", expected);

            env.undeployAll();

            env.tryInvalidCompile("select UnknownClass.invalidMethod() " + STREAM_MDB_LEN5,
                "Failed to validate select-clause expression 'UnknownClass.invalidMethod()': Failed to resolve 'UnknownClass.invalidMethod' to a property, single-row function, aggregation function, script, stream or class name ");
        }
    }

    private static class EPLOtherSingleParameterOM implements RegressionExecution {
        public void run(RegressionEnvironment env) {
            EPStatementObjectModel model = new EPStatementObjectModel();
            model.setSelectClause(SelectClause.create().add(Expressions.staticMethod("Integer", "toBinaryString", 7), "value"));
            model.setFromClause(FromClause.create(FilterStream.create("SupportMarketDataBean").addView("length", Expressions.constant(5))));
            model = SerializableObjectCopier.copyMayFail(model);
            model.setAnnotations(Collections.singletonList(AnnotationPart.nameAnnotation("s0")));
            String statementText = "@name('s0') select Integer.toBinaryString(7) as value" + STREAM_MDB_LEN5;

            Assert.assertEquals(statementText.trim(), model.toEPL());
            env.compileDeploy(model).addListener("s0");

            sendEvent(env, "IBM", 10d, 4L);
            env.assertEqualsNew("s0", "value", Integer.toBinaryString(7));

            env.undeployAll();
        }
    }

    private static class EPLOtherSingleParameterCompile implements RegressionExecution {
        public void run(RegressionEnvironment env) {
            String statementText = "@name('s0') select Integer.toBinaryString(7) as value" + STREAM_MDB_LEN5;
            env.eplToModelCompileDeploy(statementText).addListener("s0");

            sendEvent(env, "IBM", 10d, 4L);
            env.assertEqualsNew("s0", "value", Integer.toBinaryString(7));

            env.undeployAll();
        }
    }

    private static class EPLOtherSingleParameter implements RegressionExecution {
        public void run(RegressionEnvironment env) {
            String statementText = "@name('s0') select Integer.toBinaryString(7) " + STREAM_MDB_LEN5;
            env.compileDeploy(statementText).addListener("s0");

            sendEvent(env, "IBM", 10d, 4L);
            env.assertEqualsNew("s0", "Integer.toBinaryString(7)", Integer.toBinaryString(7));
            env.undeployAll();

            statementText = "@name('s0') select Integer.valueOf(\"6\") " + STREAM_MDB_LEN5;
            env.compileDeploy(statementText).addListener("s0");

            sendEvent(env, "IBM", 10d, 4L);
            env.assertEqualsNew("s0", "Integer.valueOf(\"6\")", Integer.valueOf("6"));

            env.undeployAll();

            statementText = "@name('s0') select java.lang.String.valueOf(\'a\') " + STREAM_MDB_LEN5;
            env.compileDeploy(statementText).addListener("s0");

            sendEvent(env, "IBM", 10d, 4L);
            env.assertEqualsNew("s0", "java.lang.String.valueOf(\"a\")", String.valueOf('a'));

            env.undeployAll();
        }
    }

    private static class EPLOtherTwoParameters implements RegressionExecution {
        public void run(RegressionEnvironment env) {

            String statementText = "@name('s0') select Math.max(2,3) " + STREAM_MDB_LEN5;
            env.compileDeploy(statementText).addListener("s0");

            sendEvent(env, "IBM", 10d, 4L);
            env.assertEqualsNew("s0", "Math.max(2,3)", 3);

            env.undeployAll();

            statementText = "@name('s0') select java.lang.Math.max(2,3d) " + STREAM_MDB_LEN5;
            env.compileDeploy(statementText).addListener("s0");

            sendEvent(env, "IBM", 10d, 4L);
            env.assertEqualsNew("s0", "java.lang.Math.max(2,3.0)", 3d);

            env.undeployAll();

            statementText = "@name('s0') select Long.parseLong(\"123\",10)" + STREAM_MDB_LEN5;
            env.compileDeploy(statementText).addListener("s0");

            sendEvent(env, "IBM", 10d, 4L);
            env.assertEqualsNew("s0", "Long.parseLong(\"123\",10)", Long.parseLong("123", 10));
            env.undeployAll();
        }
    }

    private static class EPLOtherUserDefined implements RegressionExecution {
        public void run(RegressionEnvironment env) {

            String className = SupportStaticMethodLib.class.getName();
            String statementText = "@name('s0') select " + className + ".staticMethod(2)" + STREAM_MDB_LEN5;
            env.compileDeploy(statementText).addListener("s0");

            sendEvent(env, "IBM", 10d, 4L);
            env.assertEqualsNew("s0", className + ".staticMethod(2)", 2);

            env.undeployAll();

            // try context passed
            SupportStaticMethodLib.getMethodInvocationContexts().clear();
            statementText = "@Name('s0') select " + className + ".staticMethodWithContext(2)" + STREAM_MDB_LEN5;
            env.compileDeploy(statementText).addListener("s0");

            sendEvent(env, "IBM", 10d, 4L);
            env.assertEqualsNew("s0", className + ".staticMethodWithContext(2)", 2);
            env.assertThat(() -> {
                EPLMethodInvocationContext first = SupportStaticMethodLib.getMethodInvocationContexts().get(0);
                Assert.assertEquals("s0", first.getStatementName());
                Assert.assertEquals(env.runtimeURI(), first.getRuntimeURI());
                Assert.assertEquals(-1, first.getContextPartitionId());
                Assert.assertEquals("staticMethodWithContext", first.getFunctionName());
            });
            env.undeployAll();
        }
    }

    private static class EPLOtherComplexParameters implements RegressionExecution {
        public void run(RegressionEnvironment env) {

            String statementText = "@name('s0') select String.valueOf(price) " + STREAM_MDB_LEN5;
            env.compileDeploy(statementText).addListener("s0");

            sendEvent(env, "IBM", 10d, 4L);
            env.assertEqualsNew("s0", "String.valueOf(price)", String.valueOf(10d));
            env.undeployAll();

            statementText = "@name('s0') select String.valueOf(2 + 3*5) " + STREAM_MDB_LEN5;
            env.compileDeploy(statementText).addListener("s0");

            sendEvent(env, "IBM", 10d, 4L);
            env.assertEqualsNew("s0", "String.valueOf(2+3*5)", String.valueOf(2 + 3 * 5));
            env.undeployAll();

            statementText = "@name('s0') select String.valueOf(price*volume +volume) " + STREAM_MDB_LEN5;
            env.compileDeploy(statementText).addListener("s0");

            sendEvent(env, "IBM", 10d, 4L);
            env.assertEqualsNew("s0", "String.valueOf(price*volume+volume)", String.valueOf(44d));
            env.undeployAll();

            statementText = "@name('s0') select String.valueOf(Math.pow(price,Integer.valueOf(\"2\"))) " + STREAM_MDB_LEN5;
            env.compileDeploy(statementText).addListener("s0");

            sendEvent(env, "IBM", 10d, 4L);
            env.assertEqualsNew("s0", "String.valueOf(Math.pow(price,Integer.valueOf(\"2\")))", String.valueOf(100d));

            env.undeployAll();
        }
    }

    private static class EPLOtherMultipleMethodInvocations implements RegressionExecution {
        public void run(RegressionEnvironment env) {

            String statementText = "@name('s0') select Math.max(2d,price), Math.max(volume,4d)" + STREAM_MDB_LEN5;
            env.compileDeploy(statementText).addListener("s0");

            sendEvent(env, "IBM", 10d, 4L);
            env.assertPropsNew("s0", new String[]{"Math.max(2.0,price)", "Math.max(volume,4.0)"}, new Object[]{10d, 4d});
            env.undeployAll();
        }
    }

    private static class EPLOtherOtherClauses implements RegressionExecution {
        public void run(RegressionEnvironment env) {
            // where
            String statementText = "@name('s0') select *" + STREAM_MDB_LEN5 + "where Math.pow(price, .5) > 2";
            env.compileDeploy(statementText).addListener("s0");

            sendEvent(env, "IBM", 10d, 4L);
            env.assertEqualsNew("s0", "symbol", "IBM");

            sendEvent(env, "CAT", 4d, 100);
            env.assertListenerNotInvoked("s0");

            env.undeployAll();

            // group-by
            statementText = "@name('s0') select symbol, sum(price)" + STREAM_MDB_LEN5 + "group by String.valueOf(symbol)";
            env.compileDeploy(statementText).addListener("s0");

            sendEvent(env, "IBM", 10d, 4L);
            env.assertEqualsNew("s0", "sum(price)", 10d);

            sendEvent(env, "IBM", 4d, 100);
            env.assertEqualsNew("s0", "sum(price)", 14d);
            env.undeployAll();

            // having
            statementText = "@name('s0') select symbol, sum(price)" + STREAM_MDB_LEN5 + "having Math.pow(sum(price), .5) > 3";
            env.compileDeploy(statementText).addListener("s0");

            sendEvent(env, "IBM", 10d, 4L);
            env.assertEqualsNew("s0", "sum(price)", 10d);

            sendEvent(env, "IBM", 100d, 100);
            env.assertEqualsNew("s0", "sum(price)", 110d);

            env.undeployAll();

            // order-by
            statementText = "@name('s0') select symbol, price" + STREAM_MDB_LEN5 + "output every 3 events order by Math.pow(price, 2)";
            env.compileDeploy(statementText).addListener("s0");

            sendEvent(env, "IBM", 10d, 4L);
            sendEvent(env, "CAT", 10d, 0L);
            sendEvent(env, "MAT", 3d, 0L);

            env.assertListener("s0", listener -> {
                EventBean[] newEvents = listener.getAndResetLastNewData();
                assertTrue(newEvents.length == 3);
                Assert.assertEquals("MAT", newEvents[0].get("symbol"));
                Assert.assertEquals("IBM", newEvents[1].get("symbol"));
                Assert.assertEquals("CAT", newEvents[2].get("symbol"));
            });
            env.undeployAll();
        }
    }

    private static class EPLOtherNestedFunction implements RegressionExecution {
        public void run(RegressionEnvironment env) {
            String text = "@name('s0') select " +
                "SupportStaticMethodLib.appendPipe(SupportStaticMethodLib.delimitPipe('POLYGON ((100.0 100, \", 100 100, 400 400))'),temp.geom) as val" +
                " from SupportTemperatureBean as temp";
            env.compileDeploy(text).addListener("s0");

            env.sendEventBean(new SupportTemperatureBean("a"));
            env.assertEqualsNew("s0", "val", "|POLYGON ((100.0 100, \", 100 100, 400 400))||a");

            env.undeployAll();
        }
    }

    private static class EPLOtherPassthru implements RegressionExecution {
        public void run(RegressionEnvironment env) {
            String text = "@name('s0') select " +
                "SupportStaticMethodLib.passthru(id) as val from SupportBean_S0";
            env.compileDeploy(text).addListener("s0");

            env.sendEventBean(new SupportBean_S0(1));
            env.assertEqualsNew("s0", "val", 1L);

            env.sendEventBean(new SupportBean_S0(2));
            env.assertEqualsNew("s0", "val", 2L);

            env.undeployAll();
        }
    }

    private static class EPLOtherPerfConstantParameters implements RegressionExecution {
        public void run(RegressionEnvironment env) {
            String text = "select " +
                "SupportStaticMethodLib.sleep(100) as val" +
                " from SupportTemperatureBean as temp";
            env.compileDeploy(text);

            long startTime = System.currentTimeMillis();
            for (int i = 0; i < 1000; i++) {
                env.sendEventBean(new SupportTemperatureBean("a"));
            }
            long endTime = System.currentTimeMillis();
            long delta = endTime - startTime;

            assertTrue("Failed perf test, delta=" + delta, delta < 2000);
            env.undeployAll();
        }
    }

    private static class EPLOtherPerfConstantParametersNested implements RegressionExecution {
        public void run(RegressionEnvironment env) {
            String text = "select " +
                "SupportStaticMethodLib.sleep(SupportStaticMethodLib.passthru(100)) as val" +
                " from SupportTemperatureBean as temp";
            env.compileDeploy(text);


            long startTime = System.currentTimeMillis();
            for (int i = 0; i < 500; i++) {
                env.sendEventBean(new SupportTemperatureBean("a"));
            }
            long endTime = System.currentTimeMillis();
            long delta = endTime - startTime;

            assertTrue("Failed perf test, delta=" + delta, delta < 1000);

            env.undeployAll();
        }
    }

    private static void sendEvent(RegressionEnvironment env, String symbol, double price, long volume) {
        env.sendEventBean(new SupportMarketDataBean(symbol, price, volume, ""));
    }

    public static class LevelZero {
        public static LevelOne getLevelOne() {
            return new LevelOne();
        }
    }

    public static class LevelOne {
        private static String field;

        public static void setField(String field) {
            LevelOne.field = field;
        }

        public String getLevelTwoValue() {
            return field;
        }
    }

    public static class NullPrimitive {

        public static Integer getValue(int input) {
            return input + 10;
        }
    }

    public static class PrimitiveConversionLib {
        public static int passIntAsObject(Object o) {
            return (Integer) o;
        }

        public static int passIntAsNumber(Number n) {
            return n.intValue();
        }

        public static int passIntAsComparable(Comparable c) {
            return (Integer) c;
        }

        public static int passIntAsSerializable(Serializable s) {
            return (Integer) s;
        }
    }
}
