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
import com.espertech.esper.common.client.scopetest.EPAssertionUtil;
import com.espertech.esper.common.client.soda.*;
import com.espertech.esper.common.internal.support.SupportBean;
import com.espertech.esper.common.internal.support.SupportBean_S0;
import com.espertech.esper.common.internal.util.SerializableObjectCopier;
import com.espertech.esper.regressionlib.framework.RegressionEnvironment;
import com.espertech.esper.regressionlib.framework.RegressionExecution;
import com.espertech.esper.regressionlib.framework.RegressionPath;
import com.espertech.esper.regressionlib.support.bean.*;
import com.espertech.esper.regressionlib.support.epl.SupportStaticMethodLib;
import junit.framework.TestCase;
import org.junit.Assert;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import static com.espertech.esper.regressionlib.framework.SupportMessageAssertUtil.tryInvalidCompile;
import static junit.framework.TestCase.*;
import static org.junit.Assert.assertEquals;

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
        return execs;
    }

    private static class EPLOtherStaticFuncWCurrentTimeStamp implements RegressionExecution {
        public void run(RegressionEnvironment env) {
            env.advanceTime(1000);
            String epl = "@name('s0') select java.time.format.DateTimeFormatter.ISO_INSTANT.format(java.time.Instant.ofEpochMilli(current_timestamp())) as c0 from SupportBean";
            env.compileDeploy(epl).addListener("s0");

            env.sendEventBean(new SupportBean());
            assertEquals("1970-01-01T00:00:01Z", env.listener("s0").assertOneGetNewAndReset().get("c0"));

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
            EPAssertionUtil.assertProps(env.listener("s0").assertOneGetNewAndReset(), "c0,c1,c2,c3".split(","), new Object[]{10, 10, 10, 10});

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
            EPAssertionUtil.assertProps(env.listener("s0").assertOneGetNewAndReset(), "val0".split(","), new Object[]{"v1"});

            LevelOne.setField("v2");
            env.sendEventBean(new SupportBean());
            EPAssertionUtil.assertProps(env.listener("s0").assertOneGetNewAndReset(), "val0".split(","), new Object[]{"v2"});

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
            EventPropertyDescriptor[] prop = env.statement("s0").getEventType().getPropertyDescriptors();
            for (int i = 0; i < rows.length; i++) {
                Assert.assertEquals(rows[i][0], prop[i].getPropertyName());
                Assert.assertEquals(rows[i][1], prop[i].getPropertyType());
            }

            env.sendEventBean(new SupportBean());
            EPAssertionUtil.assertProps(env.listener("s0").assertOneGetNew(), new String[]{subexp},
                new Object[]{SupportChainTop.make().getChildOne("abc", 1).getChildTwo("def").getText()});

            env.undeployAll();
        }
    }

    private static class EPLOtherEscape implements RegressionExecution {
        public void run(RegressionEnvironment env) {
            String statementText = "@name('s0') select SupportStaticMethodLib.`join`(abcstream) as value from SupportBean abcstream";
            env.compileDeploy(statementText).addListener("s0");

            env.sendEventBean(new SupportBean("E1", 99));

            EPAssertionUtil.assertProps(env.listener("s0").assertOneGetNew(), "value".split(","), new Object[]{"E1 99"});

            env.undeployAll();
        }
    }

    private static class EPLOtherReturnsMapIndexProperty implements RegressionExecution {
        public void run(RegressionEnvironment env) {
            RegressionPath path = new RegressionPath();
            String statementText = "insert into ABCStream select SupportStaticMethodLib.myMapFunc() as mymap, SupportStaticMethodLib.myArrayFunc() as myindex from SupportBean";
            env.compileDeploy(statementText, path);

            statementText = "@name('s0') select mymap('A') as v0, myindex[1] as v1 from ABCStream";
            env.compileDeploy(statementText, path).addListener("s0");

            env.sendEventBean(new SupportBean());

            EPAssertionUtil.assertProps(env.listener("s0").assertOneGetNew(), "v0,v1".split(","), new Object[]{"A1", 200});

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
            TestCase.assertFalse(env.listener("s0").isInvoked());
            env.sendEventBean(new SupportBean("a", 0));
            TestCase.assertTrue(env.listener("s0").isInvoked());

            env.undeployAll();
            statementText = "@name('s0') select * from pattern [myevent=SupportBean(" +
                className + ".delimitPipe(null) = '|<null>|')]";
            env.compileDeploy(statementText).addListener("s0");

            env.sendEventBean(new SupportBean("a", 0));
            TestCase.assertTrue(env.listener("s0").isInvoked());

            env.undeployAll();
        }
    }

    private static class EPLOtherRuntimeException implements RegressionExecution {
        public void run(RegressionEnvironment env) {
            String className = SupportStaticMethodLib.class.getName();
            String statementText = "@name('s0') select price, " + className + ".throwException() as value " + STREAM_MDB_LEN5;
            env.compileDeploy(statementText).addListener("s0");

            sendEvent(env, "IBM", 10d, 4L);
            TestCase.assertNull(env.listener("s0").assertOneGetNewAndReset().get("value"));

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
            EPAssertionUtil.assertProps(env.listener("s0").assertOneGetNewAndReset(), "v1,v2,v3,v4".split(","), new Object[]{10, 10d, 10d, 10d});

            env.undeployAll();
        }
    }

    private static class EPLOtherNoParameters implements RegressionExecution {
        public void run(RegressionEnvironment env) {
            Long startTime = System.currentTimeMillis();
            String statementText = "@name('s0') select System.currentTimeMillis() " + STREAM_MDB_LEN5;
            env.compileDeploy(statementText).addListener("s0");

            env.sendEventBean(new SupportMarketDataBean("IBM", 10d, 4L, ""));
            Long result = (Long) getProperty(env, "System.currentTimeMillis()");
            Long finishTime = System.currentTimeMillis();
            assertTrue(startTime <= result);
            assertTrue(result <= finishTime);
            env.undeployAll();

            statementText = "@name('s0') select java.lang.ClassLoader.getSystemClassLoader() " + STREAM_MDB_LEN5;
            env.compileDeploy(statementText).addListener("s0");

            Object expected = ClassLoader.getSystemClassLoader();
            Object[] resultTwo = assertStatementAndGetProperty(env, true, "java.lang.ClassLoader.getSystemClassLoader()");
            if (resultTwo == null) {
                fail();
            }
            assertEquals(expected, resultTwo[0]);
            env.undeployAll();

            tryInvalidCompile(env, "select UnknownClass.invalidMethod() " + STREAM_MDB_LEN5,
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
            Assert.assertEquals(Integer.toBinaryString(7), env.listener("s0").assertOneGetNewAndReset().get("value"));

            env.undeployAll();
        }
    }

    private static class EPLOtherSingleParameterCompile implements RegressionExecution {
        public void run(RegressionEnvironment env) {
            String statementText = "@name('s0') select Integer.toBinaryString(7) as value" + STREAM_MDB_LEN5;
            env.eplToModelCompileDeploy(statementText).addListener("s0");

            sendEvent(env, "IBM", 10d, 4L);
            Assert.assertEquals(Integer.toBinaryString(7), env.listener("s0").assertOneGetNewAndReset().get("value"));

            env.undeployAll();
        }
    }

    private static class EPLOtherSingleParameter implements RegressionExecution {
        public void run(RegressionEnvironment env) {
            String statementText = "@name('s0') select Integer.toBinaryString(7) " + STREAM_MDB_LEN5;
            env.compileDeploy(statementText).addListener("s0");

            Object[] result = assertStatementAndGetProperty(env, true, "Integer.toBinaryString(7)");
            assertEquals(Integer.toBinaryString(7), result[0]);
            env.undeployAll();

            statementText = "@name('s0') select Integer.valueOf(\"6\") " + STREAM_MDB_LEN5;
            env.compileDeploy(statementText).addListener("s0");

            result = assertStatementAndGetProperty(env, true, "Integer.valueOf(\"6\")");
            assertEquals(Integer.valueOf("6"), result[0]);
            env.undeployAll();

            statementText = "@name('s0') select java.lang.String.valueOf(\'a\') " + STREAM_MDB_LEN5;
            env.compileDeploy(statementText).addListener("s0");

            result = assertStatementAndGetProperty(env, true, "java.lang.String.valueOf(\"a\")");
            assertEquals(String.valueOf('a'), result[0]);
            env.undeployAll();
        }
    }

    private static class EPLOtherTwoParameters implements RegressionExecution {
        public void run(RegressionEnvironment env) {

            String statementText = "@name('s0') select Math.max(2,3) " + STREAM_MDB_LEN5;
            env.compileDeploy(statementText).addListener("s0");

            assertEquals(3, assertStatementAndGetProperty(env, true, "Math.max(2,3)")[0]);
            env.undeployAll();

            statementText = "@name('s0') select java.lang.Math.max(2,3d) " + STREAM_MDB_LEN5;
            env.compileDeploy(statementText).addListener("s0");

            assertEquals(3d, assertStatementAndGetProperty(env, true, "java.lang.Math.max(2,3.0)")[0]);
            env.undeployAll();

            statementText = "@name('s0') select Long.parseLong(\"123\",10)" + STREAM_MDB_LEN5;
            env.compileDeploy(statementText).addListener("s0");

            Object expected = Long.parseLong("123", 10);
            assertEquals(expected, assertStatementAndGetProperty(env, true, "Long.parseLong(\"123\",10)")[0]);
            env.undeployAll();
        }
    }

    private static class EPLOtherUserDefined implements RegressionExecution {
        public void run(RegressionEnvironment env) {

            String className = SupportStaticMethodLib.class.getName();
            String statementText = "@name('s0') select " + className + ".staticMethod(2)" + STREAM_MDB_LEN5;
            env.compileDeploy(statementText).addListener("s0");

            assertEquals(2, assertStatementAndGetProperty(env, true, className + ".staticMethod(2)")[0]);
            env.undeployAll();

            // try context passed
            SupportStaticMethodLib.getMethodInvocationContexts().clear();
            statementText = "@Name('s0') select " + className + ".staticMethodWithContext(2)" + STREAM_MDB_LEN5;
            env.compileDeploy(statementText).addListener("s0");

            assertEquals(2, assertStatementAndGetProperty(env, true, className + ".staticMethodWithContext(2)")[0]);
            EPLMethodInvocationContext first = SupportStaticMethodLib.getMethodInvocationContexts().get(0);
            Assert.assertEquals("s0", first.getStatementName());
            Assert.assertEquals(env.runtimeURI(), first.getRuntimeURI());
            Assert.assertEquals(-1, first.getContextPartitionId());
            Assert.assertEquals("staticMethodWithContext", first.getFunctionName());
            env.undeployAll();
        }
    }

    private static class EPLOtherComplexParameters implements RegressionExecution {
        public void run(RegressionEnvironment env) {


            String statementText = "@name('s0') select String.valueOf(price) " + STREAM_MDB_LEN5;
            env.compileDeploy(statementText).addListener("s0");

            Object[] result = assertStatementAndGetProperty(env, true, "String.valueOf(price)");
            assertEquals(String.valueOf(10d), result[0]);
            env.undeployAll();

            statementText = "@name('s0') select String.valueOf(2 + 3*5) " + STREAM_MDB_LEN5;
            env.compileDeploy(statementText).addListener("s0");

            result = assertStatementAndGetProperty(env, true, "String.valueOf(2+3*5)");
            assertEquals(String.valueOf(17), result[0]);
            env.undeployAll();

            statementText = "@name('s0') select String.valueOf(price*volume +volume) " + STREAM_MDB_LEN5;
            env.compileDeploy(statementText).addListener("s0");

            result = assertStatementAndGetProperty(env, true, "String.valueOf(price*volume+volume)");
            assertEquals(String.valueOf(44d), result[0]);
            env.undeployAll();

            statementText = "@name('s0') select String.valueOf(Math.pow(price,Integer.valueOf(\"2\"))) " + STREAM_MDB_LEN5;
            env.compileDeploy(statementText).addListener("s0");

            result = assertStatementAndGetProperty(env, true, "String.valueOf(Math.pow(price,Integer.valueOf(\"2\")))");
            assertEquals(String.valueOf(100d), result[0]);

            env.undeployAll();
        }
    }

    private static class EPLOtherMultipleMethodInvocations implements RegressionExecution {
        public void run(RegressionEnvironment env) {

            String statementText = "@name('s0') select Math.max(2d,price), Math.max(volume,4d)" + STREAM_MDB_LEN5;
            env.compileDeploy(statementText).addListener("s0");

            Object[] props = assertStatementAndGetProperty(env, true, "Math.max(2.0,price)", "Math.max(volume,4.0)");
            assertEquals(10d, props[0]);
            assertEquals(4d, props[1]);
            env.undeployAll();
        }
    }

    private static class EPLOtherOtherClauses implements RegressionExecution {
        public void run(RegressionEnvironment env) {
            // where
            String statementText = "@name('s0') select *" + STREAM_MDB_LEN5 + "where Math.pow(price, .5) > 2";
            env.compileDeploy(statementText).addListener("s0");
            assertEquals("IBM", assertStatementAndGetProperty(env, true, "symbol")[0]);
            sendEvent(env, "CAT", 4d, 100);
            assertNull(getProperty(env, "symbol"));
            env.undeployAll();

            // group-by
            statementText = "@name('s0') select symbol, sum(price)" + STREAM_MDB_LEN5 + "group by String.valueOf(symbol)";
            env.compileDeploy(statementText).addListener("s0");
            assertEquals(10d, assertStatementAndGetProperty(env, true, "sum(price)")[0]);
            sendEvent(env, "IBM", 4d, 100);
            assertEquals(14d, getProperty(env, "sum(price)"));
            env.undeployAll();

            // having
            statementText = "@name('s0') select symbol, sum(price)" + STREAM_MDB_LEN5 + "having Math.pow(sum(price), .5) > 3";
            env.compileDeploy(statementText).addListener("s0");
            assertEquals(10d, assertStatementAndGetProperty(env, true, "sum(price)")[0]);
            sendEvent(env, "IBM", 100d, 100);
            assertEquals(110d, getProperty(env, "sum(price)"));
            env.undeployAll();

            // order-by
            statementText = "@name('s0') select symbol, price" + STREAM_MDB_LEN5 + "output every 3 events order by Math.pow(price, 2)";
            env.compileDeploy(statementText).addListener("s0");
            assertStatementAndGetProperty(env, false, "symbol");
            sendEvent(env, "CAT", 10d, 0L);
            sendEvent(env, "MAT", 3d, 0L);

            EventBean[] newEvents = env.listener("s0").getAndResetLastNewData();
            assertTrue(newEvents.length == 3);
            Assert.assertEquals("MAT", newEvents[0].get("symbol"));
            Assert.assertEquals("IBM", newEvents[1].get("symbol"));
            Assert.assertEquals("CAT", newEvents[2].get("symbol"));
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
            Assert.assertEquals("|POLYGON ((100.0 100, \", 100 100, 400 400))||a", env.listener("s0").assertOneGetNewAndReset().get("val"));

            env.undeployAll();
        }
    }

    private static class EPLOtherPassthru implements RegressionExecution {
        public void run(RegressionEnvironment env) {
            String text = "@name('s0') select " +
                "SupportStaticMethodLib.passthru(id) as val from SupportBean_S0";
            env.compileDeploy(text).addListener("s0");

            env.sendEventBean(new SupportBean_S0(1));
            Assert.assertEquals(1L, env.listener("s0").assertOneGetNewAndReset().get("val"));

            env.sendEventBean(new SupportBean_S0(2));
            Assert.assertEquals(2L, env.listener("s0").assertOneGetNewAndReset().get("val"));

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

    private static Object getProperty(RegressionEnvironment env, String propertyName) {
        EventBean[] newData = env.listener("s0").getAndResetLastNewData();
        if (newData == null || newData.length == 0) {
            return null;
        } else {
            return newData[0].get(propertyName);
        }
    }

    private static Object[] assertStatementAndGetProperty(RegressionEnvironment env, boolean expectResult, String... propertyNames) {
        if (propertyNames == null) {
            fail("no prop names");
        }
        sendEvent(env, "IBM", 10d, 4L);

        if (expectResult) {
            List<Object> properties = new ArrayList<>();
            EventBean theEvent = env.listener("s0").getAndResetLastNewData()[0];
            for (String propertyName : propertyNames) {
                properties.add(theEvent.get(propertyName));
            }
            return properties.toArray(new Object[0]);
        }
        return null;
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
