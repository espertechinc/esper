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

import com.espertech.esper.client.*;
import com.espertech.esper.client.hook.EPLMethodInvocationContext;
import com.espertech.esper.client.scopetest.EPAssertionUtil;
import com.espertech.esper.client.scopetest.SupportUpdateListener;
import com.espertech.esper.client.soda.*;
import com.espertech.esper.metrics.instrumentation.InstrumentationHelper;
import com.espertech.esper.supportregression.bean.*;
import com.espertech.esper.supportregression.epl.SupportStaticMethodLib;
import com.espertech.esper.supportregression.execution.RegressionExecution;
import com.espertech.esper.util.SerializableObjectCopier;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

import static junit.framework.TestCase.*;
import static org.junit.Assert.assertEquals;

public class ExecEPLStaticFunctions implements RegressionExecution {
    private final static String STREAM_MDB_LEN5 = " from " + SupportMarketDataBean.class.getName() + "#length(5) ";

    public void configure(Configuration configuration) throws Exception {
        configuration.addImport(SupportStaticMethodLib.class.getName());
        configuration.addImport(LevelZero.class);
        configuration.addImport(SupportChainTop.class);
        configuration.addImport(NullPrimitive.class);

        configuration.addEventType(SupportBean.class);
        configuration.addEventType(SupportChainTop.class);
        configuration.addEventType("Temperature", SupportTemperatureBean.class);

        configuration.addPlugInSingleRowFunction("sleepme", SupportStaticMethodLib.class.getName(), "sleep", ConfigurationPlugInSingleRowFunction.ValueCache.ENABLED);
    }

    public void run(EPServiceProvider epService) throws Exception {
        runAssertionNullPrimitive(epService);
        runAssertionChainedInstance(epService);
        runAssertionChainedStatic(epService);
        runAssertionEscape(epService);
        runAssertionReturnsMapIndexProperty(epService);
        runAssertionPattern(epService);
        runAssertionRuntimeException(epService);
        runAssertionArrayParameter(epService);
        if (!InstrumentationHelper.ENABLED) {
            runAssertionNoParameters(epService);
            runAssertionPerfConstantParameters(epService);
            runAssertionPerfConstantParametersNested(epService);
        }
        runAssertionSingleParameterOM(epService);
        runAssertionSingleParameterCompile(epService);
        runAssertionSingleParameter(epService);
        runAssertionTwoParameters(epService);
        runAssertionUserDefined(epService);
        runAssertionComplexParameters(epService);
        runAssertionMultipleMethodInvocations(epService);
        runAssertionOtherClauses(epService);
        runAssertionNestedFunction(epService);
        runAssertionPassthru(epService);
        runAssertionPrimitiveConversion(epService);
    }

    private void runAssertionPrimitiveConversion(EPServiceProvider epService) {
        epService.getEPAdministrator().getConfiguration().addImport(PrimitiveConversionLib.class);
        EPStatement stmt = epService.getEPAdministrator().createEPL("select " +
                "PrimitiveConversionLib.passIntAsObject(intPrimitive) as c0," +
                "PrimitiveConversionLib.passIntAsNumber(intPrimitive) as c1," +
                "PrimitiveConversionLib.passIntAsComparable(intPrimitive) as c2," +
                "PrimitiveConversionLib.passIntAsSerializable(intPrimitive) as c3" +
                " from SupportBean");
        SupportUpdateListener listener = new SupportUpdateListener();
        stmt.addListener(listener);

        epService.getEPRuntime().sendEvent(new SupportBean("E1", 10));
        EPAssertionUtil.assertProps(listener.assertOneGetNewAndReset(), "c0,c1,c2,c3".split(","), new Object[] {10, 10, 10, 10});

        stmt.destroy();
    }

    private void runAssertionNullPrimitive(EPServiceProvider epService) {
        // test passing null
        SupportUpdateListener listener = new SupportUpdateListener();
        epService.getEPAdministrator().createEPL("select NullPrimitive.getValue(intBoxed) from SupportBean").addListener(listener);

        epService.getEPRuntime().sendEvent(new SupportBean());

        epService.getEPAdministrator().destroyAllStatements();
    }

    private void runAssertionChainedInstance(EPServiceProvider epService) {

        SupportUpdateListener listener = new SupportUpdateListener();
        epService.getEPAdministrator().createEPL("select " +
                "LevelZero.getLevelOne().getLevelTwoValue() as val0 " +
                "from SupportBean").addListener(listener);

        LevelOne.setField("v1");
        epService.getEPRuntime().sendEvent(new SupportBean());
        EPAssertionUtil.assertProps(listener.assertOneGetNewAndReset(), "val0".split(","), new Object[]{"v1"});

        LevelOne.setField("v2");
        epService.getEPRuntime().sendEvent(new SupportBean());
        EPAssertionUtil.assertProps(listener.assertOneGetNewAndReset(), "val0".split(","), new Object[]{"v2"});

        epService.getEPAdministrator().destroyAllStatements();
    }

    private void runAssertionChainedStatic(EPServiceProvider epService) {

        String subexp = "SupportChainTop.make().getChildOne(\"abc\",1).getChildTwo(\"def\").getText()";
        String statementText = "select " + subexp + " from SupportBean";
        EPStatement stmtOne = epService.getEPAdministrator().createEPL(statementText);
        SupportUpdateListener listener = new SupportUpdateListener();
        stmtOne.addListener(listener);

        Object[][] rows = new Object[][]{
                {subexp, String.class}
        };
        for (int i = 0; i < rows.length; i++) {
            EventPropertyDescriptor prop = stmtOne.getEventType().getPropertyDescriptors()[i];
            assertEquals(rows[i][0], prop.getPropertyName());
            assertEquals(rows[i][1], prop.getPropertyType());
        }

        epService.getEPRuntime().sendEvent(new SupportBean());
        EPAssertionUtil.assertProps(listener.assertOneGetNew(), new String[]{subexp},
                new Object[]{SupportChainTop.make().getChildOne("abc", 1).getChildTwo("def").getText()});

        stmtOne.destroy();
    }

    private void runAssertionEscape(EPServiceProvider epService) {
        String statementText = "select SupportStaticMethodLib.`join`(abcstream) as value from SupportBean abcstream";
        EPStatement stmtOne = epService.getEPAdministrator().createEPL(statementText);
        SupportUpdateListener listener = new SupportUpdateListener();
        stmtOne.addListener(listener);

        epService.getEPRuntime().sendEvent(new SupportBean("E1", 99));

        EPAssertionUtil.assertProps(listener.assertOneGetNew(), "value".split(","), new Object[]{"E1 99"});

        stmtOne.destroy();
    }

    private void runAssertionReturnsMapIndexProperty(EPServiceProvider epService) {
        String statementText = "insert into ABCStream select SupportStaticMethodLib.myMapFunc() as mymap, SupportStaticMethodLib.myArrayFunc() as myindex from SupportBean";
        EPStatement stmtOne = epService.getEPAdministrator().createEPL(statementText);

        statementText = "select mymap('A') as v0, myindex[1] as v1 from ABCStream";
        EPStatement stmtTwo = epService.getEPAdministrator().createEPL(statementText);
        SupportUpdateListener listener = new SupportUpdateListener();
        stmtTwo.addListener(listener);

        epService.getEPRuntime().sendEvent(new SupportBean());

        EPAssertionUtil.assertProps(listener.assertOneGetNew(), "v0,v1".split(","), new Object[]{"A1", 200});

        stmtOne.destroy();
        stmtTwo.destroy();
    }

    private void runAssertionPattern(EPServiceProvider epService) {
        String className = SupportStaticMethodLib.class.getName();
        String statementText = "select * from pattern [myevent=" + SupportBean.class.getName() + "(" +
                className + ".delimitPipe(theString) = '|a|')]";
        EPStatement stmt = epService.getEPAdministrator().createEPL(statementText);
        SupportUpdateListener listener = new SupportUpdateListener();
        stmt.addListener(listener);

        epService.getEPRuntime().sendEvent(new SupportBean("b", 0));
        assertFalse(listener.isInvoked());
        epService.getEPRuntime().sendEvent(new SupportBean("a", 0));
        assertTrue(listener.isInvoked());

        stmt.destroy();
        statementText = "select * from pattern [myevent=" + SupportBean.class.getName() + "(" +
                className + ".delimitPipe(null) = '|<null>|')]";
        stmt = epService.getEPAdministrator().createEPL(statementText);
        listener = new SupportUpdateListener();
        stmt.addListener(listener);

        epService.getEPRuntime().sendEvent(new SupportBean("a", 0));
        assertTrue(listener.isInvoked());

        stmt.destroy();
    }

    private void runAssertionRuntimeException(EPServiceProvider epService) {
        String className = SupportStaticMethodLib.class.getName();
        String statementText = "select price, " + className + ".throwException() as value " + STREAM_MDB_LEN5;
        EPStatement statement = epService.getEPAdministrator().createEPL(statementText);
        SupportUpdateListener listener = new SupportUpdateListener();
        statement.addListener(listener);
        sendEvent(epService, "IBM", 10d, 4L);
        assertNull(listener.assertOneGetNewAndReset().get("value"));
        statement.destroy();
    }

    private void runAssertionArrayParameter(EPServiceProvider epService) {
        String text = "select " +
                "SupportStaticMethodLib.arraySumIntBoxed({1,2,null,3,4}) as v1, " +
                "SupportStaticMethodLib.arraySumDouble({1,2,3,4.0}) as v2, " +
                "SupportStaticMethodLib.arraySumString({'1','2','3','4'}) as v3, " +
                "SupportStaticMethodLib.arraySumObject({'1',2,3.0,'4.0'}) as v4 " +
                " from " + SupportBean.class.getName();
        SupportUpdateListener listener = new SupportUpdateListener();
        EPStatement stmt = epService.getEPAdministrator().createEPL(text);
        stmt.addListener(listener);

        epService.getEPRuntime().sendEvent(new SupportBean("E1", 1));
        EPAssertionUtil.assertProps(listener.assertOneGetNewAndReset(), "v1,v2,v3,v4".split(","), new Object[]{10, 10d, 10d, 10d});

        stmt.destroy();
    }

    private void runAssertionNoParameters(EPServiceProvider epService) {
        Long startTime = System.currentTimeMillis();
        SupportUpdateListener listener = new SupportUpdateListener();

        String statementText = "select System.currentTimeMillis() " + STREAM_MDB_LEN5;
        EPStatement stmt = epService.getEPAdministrator().createEPL(statementText);
        stmt.addListener(listener);
        Long result = (Long) createStatementAndGet(epService, listener, statementText, "System.currentTimeMillis()");
        Long finishTime = System.currentTimeMillis();
        assertTrue(startTime <= result);
        assertTrue(result <= finishTime);
        stmt.destroy();

        statementText = "select java.lang.ClassLoader.getSystemClassLoader() " + STREAM_MDB_LEN5;
        stmt = epService.getEPAdministrator().createEPL(statementText);
        stmt.addListener(listener);
        Object expected = ClassLoader.getSystemClassLoader();
        Object[] resultTwo = assertStatementAndGetProperty(epService, listener, true, "java.lang.ClassLoader.getSystemClassLoader()");
        if (resultTwo == null) {
            fail();
        }
        assertEquals(expected, resultTwo[0]);
        stmt.destroy();

        statementText = "select UnknownClass.invalidMethod() " + STREAM_MDB_LEN5;
        try {
            stmt = epService.getEPAdministrator().createEPL(statementText);
            fail();
        } catch (EPStatementException e) {
            // Expected
        }
    }

    private void runAssertionSingleParameterOM(EPServiceProvider epService) throws Exception {
        EPStatementObjectModel model = new EPStatementObjectModel();
        model.setSelectClause(SelectClause.create().add(Expressions.staticMethod("Integer", "toBinaryString", 7), "value"));
        model.setFromClause(FromClause.create(FilterStream.create(SupportMarketDataBean.class.getName()).addView("length", Expressions.constant(5))));
        model = (EPStatementObjectModel) SerializableObjectCopier.copy(model);
        String statementText = "select Integer.toBinaryString(7) as value" + STREAM_MDB_LEN5;

        assertEquals(statementText.trim(), model.toEPL());
        EPStatement statement = epService.getEPAdministrator().create(model);
        SupportUpdateListener listener = new SupportUpdateListener();
        statement.addListener(listener);

        sendEvent(epService, "IBM", 10d, 4L);
        assertEquals(Integer.toBinaryString(7), listener.assertOneGetNewAndReset().get("value"));

        statement.destroy();
    }

    private void runAssertionSingleParameterCompile(EPServiceProvider epService) throws Exception {
        String statementText = "select Integer.toBinaryString(7) as value" + STREAM_MDB_LEN5;
        EPStatementObjectModel model = epService.getEPAdministrator().compileEPL(statementText);
        model = (EPStatementObjectModel) SerializableObjectCopier.copy(model);

        assertEquals(statementText.trim(), model.toEPL());
        EPStatement statement = epService.getEPAdministrator().create(model);
        SupportUpdateListener listener = new SupportUpdateListener();
        statement.addListener(listener);

        sendEvent(epService, "IBM", 10d, 4L);
        assertEquals(Integer.toBinaryString(7), listener.assertOneGetNewAndReset().get("value"));

        statement.destroy();
    }

    private void runAssertionSingleParameter(EPServiceProvider epService) {
        SupportUpdateListener listener = new SupportUpdateListener();

        String statementText = "select Integer.toBinaryString(7) " + STREAM_MDB_LEN5;
        EPStatement stmt = epService.getEPAdministrator().createEPL(statementText);
        stmt.addListener(listener);
        Object[] result = assertStatementAndGetProperty(epService, listener, true, "Integer.toBinaryString(7)");
        assertEquals(Integer.toBinaryString(7), result[0]);
        stmt.destroy();

        statementText = "select Integer.valueOf(\"6\") " + STREAM_MDB_LEN5;
        stmt = epService.getEPAdministrator().createEPL(statementText);
        stmt.addListener(listener);
        result = assertStatementAndGetProperty(epService, listener, true, "Integer.valueOf(\"6\")");
        assertEquals(Integer.valueOf("6"), result[0]);
        stmt.destroy();

        statementText = "select java.lang.String.valueOf(\'a\') " + STREAM_MDB_LEN5;
        stmt = epService.getEPAdministrator().createEPL(statementText);
        stmt.addListener(listener);
        result = assertStatementAndGetProperty(epService, listener, true, "java.lang.String.valueOf(\"a\")");
        assertEquals(String.valueOf('a'), result[0]);
        stmt.destroy();
    }

    private void runAssertionTwoParameters(EPServiceProvider epService) {
        SupportUpdateListener listener = new SupportUpdateListener();
        String statementText = "select Math.max(2,3) " + STREAM_MDB_LEN5;
        EPStatement stmt = epService.getEPAdministrator().createEPL(statementText);
        stmt.addListener(listener);
        assertEquals(3, assertStatementAndGetProperty(epService, listener, true, "Math.max(2,3)")[0]);
        stmt.destroy();

        statementText = "select java.lang.Math.max(2,3d) " + STREAM_MDB_LEN5;
        stmt = epService.getEPAdministrator().createEPL(statementText);
        stmt.addListener(listener);
        assertEquals(3d, assertStatementAndGetProperty(epService, listener, true, "java.lang.Math.max(2,3.0)")[0]);
        stmt.destroy();

        statementText = "select Long.parseLong(\"123\",10)" + STREAM_MDB_LEN5;
        stmt = epService.getEPAdministrator().createEPL(statementText);
        stmt.addListener(listener);
        Object expected = Long.parseLong("123", 10);
        assertEquals(expected, assertStatementAndGetProperty(epService, listener, true, "Long.parseLong(\"123\",10)")[0]);
        stmt.destroy();
    }

    private void runAssertionUserDefined(EPServiceProvider epService) {
        SupportUpdateListener listener = new SupportUpdateListener();
        String className = SupportStaticMethodLib.class.getName();
        String statementText = "select " + className + ".staticMethod(2)" + STREAM_MDB_LEN5;
        EPStatement stmt = epService.getEPAdministrator().createEPL(statementText);
        stmt.addListener(listener);
        assertEquals(2, assertStatementAndGetProperty(epService, listener, true, className + ".staticMethod(2)")[0]);
        stmt.destroy();

        // try context passed
        SupportStaticMethodLib.getMethodInvocationContexts().clear();
        statementText = "@Name('S0') select " + className + ".staticMethodWithContext(2)" + STREAM_MDB_LEN5;
        stmt = epService.getEPAdministrator().createEPL(statementText);
        stmt.addListener(listener);
        assertEquals(2, assertStatementAndGetProperty(epService, listener, true, className + ".staticMethodWithContext(2)")[0]);
        EPLMethodInvocationContext first = SupportStaticMethodLib.getMethodInvocationContexts().get(0);
        assertEquals("S0", first.getStatementName());
        assertEquals(epService.getURI(), first.getEngineURI());
        assertEquals(-1, first.getContextPartitionId());
        assertEquals("staticMethodWithContext", first.getFunctionName());
        stmt.destroy();
    }

    private void runAssertionComplexParameters(EPServiceProvider epService) {
        SupportUpdateListener listener = new SupportUpdateListener();

        String statementText = "select String.valueOf(price) " + STREAM_MDB_LEN5;
        EPStatement stmt = epService.getEPAdministrator().createEPL(statementText);
        stmt.addListener(listener);
        Object[] result = assertStatementAndGetProperty(epService, listener, true, "String.valueOf(price)");
        assertEquals(String.valueOf(10d), result[0]);
        stmt.destroy();

        statementText = "select String.valueOf(2 + 3*5) " + STREAM_MDB_LEN5;
        stmt = epService.getEPAdministrator().createEPL(statementText);
        stmt.addListener(listener);
        result = assertStatementAndGetProperty(epService, listener, true, "String.valueOf(2+3*5)");
        assertEquals(String.valueOf(17), result[0]);
        stmt.destroy();

        statementText = "select String.valueOf(price*volume +volume) " + STREAM_MDB_LEN5;
        stmt = epService.getEPAdministrator().createEPL(statementText);
        stmt.addListener(listener);
        result = assertStatementAndGetProperty(epService, listener, true, "String.valueOf(price*volume+volume)");
        assertEquals(String.valueOf(44d), result[0]);
        stmt.destroy();

        statementText = "select String.valueOf(Math.pow(price,Integer.valueOf(\"2\"))) " + STREAM_MDB_LEN5;
        stmt = epService.getEPAdministrator().createEPL(statementText);
        stmt.addListener(listener);
        result = assertStatementAndGetProperty(epService, listener, true, "String.valueOf(Math.pow(price,Integer.valueOf(\"2\")))");
        assertEquals(String.valueOf(100d), result[0]);
        stmt.destroy();
    }

    private void runAssertionMultipleMethodInvocations(EPServiceProvider epService) {
        SupportUpdateListener listener = new SupportUpdateListener();
        String statementText = "select Math.max(2d,price), Math.max(volume,4d)" + STREAM_MDB_LEN5;
        EPStatement stmt = epService.getEPAdministrator().createEPL(statementText);
        stmt.addListener(listener);
        Object[] props = assertStatementAndGetProperty(epService, listener, true, "Math.max(2.0,price)", "Math.max(volume,4.0)");
        assertEquals(10d, props[0]);
        assertEquals(4d, props[1]);
        stmt.destroy();
    }

    private void runAssertionOtherClauses(EPServiceProvider epService) {
        SupportUpdateListener listener = new SupportUpdateListener();

        // where
        String statementText = "select *" + STREAM_MDB_LEN5 + "where Math.pow(price, .5) > 2";
        EPStatement statement = epService.getEPAdministrator().createEPL(statementText);
        statement.addListener(listener);
        assertEquals("IBM", assertStatementAndGetProperty(epService, listener, true, "symbol")[0]);
        sendEvent(epService, "CAT", 4d, 100);
        assertNull(getProperty(listener, "symbol"));
        statement.destroy();

        // group-by
        statementText = "select symbol, sum(price)" + STREAM_MDB_LEN5 + "group by String.valueOf(symbol)";
        statement = epService.getEPAdministrator().createEPL(statementText);
        statement.addListener(listener);
        assertEquals(10d, assertStatementAndGetProperty(epService, listener, true, "sum(price)")[0]);
        sendEvent(epService, "IBM", 4d, 100);
        assertEquals(14d, getProperty(listener, "sum(price)"));
        statement.destroy();

        // having
        statementText = "select symbol, sum(price)" + STREAM_MDB_LEN5 + "having Math.pow(sum(price), .5) > 3";
        statement = epService.getEPAdministrator().createEPL(statementText);
        statement.addListener(listener);
        assertEquals(10d, assertStatementAndGetProperty(epService, listener, true, "sum(price)")[0]);
        sendEvent(epService, "IBM", 100d, 100);
        assertEquals(110d, getProperty(listener, "sum(price)"));
        statement.destroy();

        // order-by
        statementText = "select symbol, price" + STREAM_MDB_LEN5 + "output every 3 events order by Math.pow(price, 2)";
        statement = epService.getEPAdministrator().createEPL(statementText);
        statement.addListener(listener);
        assertStatementAndGetProperty(epService, listener, false, "symbol");
        sendEvent(epService, "CAT", 10d, 0L);
        sendEvent(epService, "MAT", 3d, 0L);

        EventBean[] newEvents = listener.getAndResetLastNewData();
        assertTrue(newEvents.length == 3);
        assertEquals("MAT", newEvents[0].get("symbol"));
        assertEquals("IBM", newEvents[1].get("symbol"));
        assertEquals("CAT", newEvents[2].get("symbol"));
        statement.destroy();
    }

    private void runAssertionNestedFunction(EPServiceProvider epService) {
        String text = "select " +
                "SupportStaticMethodLib.appendPipe(SupportStaticMethodLib.delimitPipe('POLYGON ((100.0 100, \", 100 100, 400 400))'),temp.geom) as val" +
                " from Temperature as temp";
        EPStatement stmt = epService.getEPAdministrator().createEPL(text);
        SupportUpdateListener listener = new SupportUpdateListener();
        stmt.addListener(listener);

        epService.getEPRuntime().sendEvent(new SupportTemperatureBean("a"));
        assertEquals("|POLYGON ((100.0 100, \", 100 100, 400 400))||a", listener.assertOneGetNewAndReset().get("val"));

        stmt.destroy();
    }

    private void runAssertionPassthru(EPServiceProvider epService) {
        String text = "select " +
                "SupportStaticMethodLib.passthru(id) as val" +
                " from " + SupportBean_S0.class.getName();
        EPStatement stmt = epService.getEPAdministrator().createEPL(text);
        SupportUpdateListener listener = new SupportUpdateListener();
        stmt.addListener(listener);

        epService.getEPRuntime().sendEvent(new SupportBean_S0(1));
        assertEquals(1L, listener.assertOneGetNewAndReset().get("val"));

        epService.getEPRuntime().sendEvent(new SupportBean_S0(2));
        assertEquals(2L, listener.assertOneGetNewAndReset().get("val"));

        stmt.destroy();
    }

    private void runAssertionPerfConstantParameters(EPServiceProvider epService) {
        String text = "select " +
                "SupportStaticMethodLib.sleep(100) as val" +
                " from Temperature as temp";
        EPStatement stmt = epService.getEPAdministrator().createEPL(text);
        SupportUpdateListener listener = new SupportUpdateListener();
        stmt.addListener(listener);

        long startTime = System.currentTimeMillis();
        for (int i = 0; i < 1000; i++) {
            epService.getEPRuntime().sendEvent(new SupportTemperatureBean("a"));
        }
        long endTime = System.currentTimeMillis();
        long delta = endTime - startTime;

        assertTrue("Failed perf test, delta=" + delta, delta < 2000);
        stmt.destroy();
    }

    private void runAssertionPerfConstantParametersNested(EPServiceProvider epService) {
        String text = "select " +
                "SupportStaticMethodLib.sleep(SupportStaticMethodLib.passthru(100)) as val" +
                " from Temperature as temp";
        EPStatement stmt = epService.getEPAdministrator().createEPL(text);
        SupportUpdateListener listener = new SupportUpdateListener();
        stmt.addListener(listener);

        long startTime = System.currentTimeMillis();
        for (int i = 0; i < 500; i++) {
            epService.getEPRuntime().sendEvent(new SupportTemperatureBean("a"));
        }
        long endTime = System.currentTimeMillis();
        long delta = endTime - startTime;

        assertTrue("Failed perf test, delta=" + delta, delta < 1000);

        stmt.destroy();
    }

    private Object createStatementAndGet(EPServiceProvider epService, SupportUpdateListener listener, String statementText, String propertyName) {
        EPStatement statement = epService.getEPAdministrator().createEPL(statementText);
        statement.addListener(listener);
        epService.getEPRuntime().sendEvent(new SupportMarketDataBean("IBM", 10d, 4L, ""));
        return getProperty(listener, propertyName);
    }

    private Object getProperty(SupportUpdateListener listener, String propertyName) {
        EventBean[] newData = listener.getAndResetLastNewData();
        if (newData == null || newData.length == 0) {
            return null;
        } else {
            return newData[0].get(propertyName);
        }
    }

    private Object[] assertStatementAndGetProperty(EPServiceProvider epService, SupportUpdateListener listener, boolean expectResult, String... propertyNames) {
        if (propertyNames == null) {
            fail("no prop names");
        }
        sendEvent(epService, "IBM", 10d, 4L);

        if (expectResult) {
            List<Object> properties = new ArrayList<>();
            EventBean theEvent = listener.getAndResetLastNewData()[0];
            for (String propertyName : propertyNames) {
                properties.add(theEvent.get(propertyName));
            }
            return properties.toArray(new Object[0]);
        }
        return null;
    }

    private void sendEvent(EPServiceProvider epService, String symbol, double price, long volume) {
        epService.getEPRuntime().sendEvent(new SupportMarketDataBean(symbol, price, volume, ""));
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
