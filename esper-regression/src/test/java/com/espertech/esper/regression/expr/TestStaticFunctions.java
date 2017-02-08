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
package com.espertech.esper.regression.expr;

import com.espertech.esper.client.*;
import com.espertech.esper.client.hook.EPLMethodInvocationContext;
import com.espertech.esper.client.scopetest.EPAssertionUtil;
import com.espertech.esper.client.scopetest.SupportUpdateListener;
import com.espertech.esper.client.soda.*;
import com.espertech.esper.metrics.instrumentation.InstrumentationHelper;
import com.espertech.esper.supportregression.bean.*;
import com.espertech.esper.supportregression.client.SupportConfigFactory;
import com.espertech.esper.supportregression.epl.SupportStaticMethodLib;
import com.espertech.esper.util.SerializableObjectCopier;
import junit.framework.TestCase;

import java.util.ArrayList;
import java.util.List;

public class TestStaticFunctions extends TestCase
{
	private EPServiceProvider epService;
	private String stream;
	private String statementText;
	private SupportUpdateListener listener;

	protected void setUp()
	{
	    epService = EPServiceProviderManager.getDefaultProvider(SupportConfigFactory.getConfiguration());
	    epService.initialize();
        if (InstrumentationHelper.ENABLED) { InstrumentationHelper.startTest(epService, this.getClass(), getName());}
        stream = " from " + SupportMarketDataBean.class.getName() +"#length(5) ";
        listener = new SupportUpdateListener();
	}

    protected void tearDown() throws Exception {
        if (InstrumentationHelper.ENABLED) { InstrumentationHelper.endTest();}
        listener = null;
    }

    public void testNullPrimitive() {
        epService.getEPAdministrator().getConfiguration().addEventType("SupportBean", SupportBean.class);
        epService.getEPAdministrator().getConfiguration().addImport(NullPrimitive.class);

        // test passing null
        epService.getEPAdministrator().createEPL("select NullPrimitive.getValue(intBoxed) from SupportBean").addListener(listener);

        epService.getEPRuntime().sendEvent(new SupportBean());
    }

    public void testChainedInstance() {
        epService.getEPAdministrator().getConfiguration().addEventType("SupportBean", SupportBean.class);
        epService.getEPAdministrator().getConfiguration().addImport(LevelZero.class);

        epService.getEPAdministrator().createEPL("select " +
                "LevelZero.getLevelOne().getLevelTwoValue() as val0 " +
                "from SupportBean").addListener(listener);

        LevelOne.setField("v1");
        epService.getEPRuntime().sendEvent(new SupportBean());
        EPAssertionUtil.assertProps(listener.assertOneGetNewAndReset(), "val0".split(","), new Object[]{"v1"});

        LevelOne.setField("v2");
        epService.getEPRuntime().sendEvent(new SupportBean());
        EPAssertionUtil.assertProps(listener.assertOneGetNewAndReset(), "val0".split(","), new Object[]{"v2"});
    }

    public void testChainedStatic() {
        epService.getEPAdministrator().getConfiguration().addEventType("SupportBean", SupportBean.class);
        epService.getEPAdministrator().getConfiguration().addEventType("SupportChainTop", SupportChainTop.class);
        epService.getEPAdministrator().getConfiguration().addImport(SupportChainTop.class);

        String subexp = "SupportChainTop.make().getChildOne(\"abc\",1).getChildTwo(\"def\").getText()";
        statementText = "select " + subexp + " from SupportBean";
        EPStatement stmtOne = epService.getEPAdministrator().createEPL(statementText);
        listener = new SupportUpdateListener();
        stmtOne.addListener(listener);

        Object[][] rows = new Object[][] {
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
    }

    public void testEscape() {
        epService.getEPAdministrator().getConfiguration().addEventType("SupportBean", SupportBean.class);
        epService.getEPAdministrator().getConfiguration().addImport(SupportStaticMethodLib.class.getName());

        statementText = "select SupportStaticMethodLib.`join`(abcstream) as value from SupportBean abcstream";
        EPStatement stmtOne = epService.getEPAdministrator().createEPL(statementText);
        listener = new SupportUpdateListener();
        stmtOne.addListener(listener);

        epService.getEPRuntime().sendEvent(new SupportBean("E1", 99));

        EPAssertionUtil.assertProps(listener.assertOneGetNew(), "value".split(","), new Object[]{"E1 99"});
    }

    public void testReturnsMapIndexProperty()
    {
        epService.getEPAdministrator().getConfiguration().addEventType("SupportBean", SupportBean.class);
        epService.getEPAdministrator().getConfiguration().addImport(SupportStaticMethodLib.class.getName());

        statementText = "insert into ABCStream select SupportStaticMethodLib.myMapFunc() as mymap, SupportStaticMethodLib.myArrayFunc() as myindex from SupportBean";
        EPStatement stmtOne = epService.getEPAdministrator().createEPL(statementText);

        statementText = "select mymap('A') as v0, myindex[1] as v1 from ABCStream";
        EPStatement stmtTwo = epService.getEPAdministrator().createEPL(statementText);
        listener = new SupportUpdateListener();
        stmtTwo.addListener(listener);

        epService.getEPRuntime().sendEvent(new SupportBean());
        
        EPAssertionUtil.assertProps(listener.assertOneGetNew(), "v0,v1".split(","), new Object[]{"A1", 200});
    }

    public void testPattern()
    {
        String className = SupportStaticMethodLib.class.getName();
        statementText = "select * from pattern [myevent=" + SupportBean.class.getName() + "(" +
                className + ".delimitPipe(theString) = '|a|')]";
        EPStatement stmt = epService.getEPAdministrator().createEPL(statementText);
        listener = new SupportUpdateListener();
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
    }

	public void testRuntimeException()
	{
		String className = SupportStaticMethodLib.class.getName();
		statementText = "select price, " + className + ".throwException() as value " + stream;
        EPStatement statement = epService.getEPAdministrator().createEPL(statementText);
        listener = new SupportUpdateListener();
        statement.addListener(listener);
        sendEvent("IBM", 10d, 4l);
        assertNull(listener.assertOneGetNewAndReset().get("value"));
    }

    public void testArrayParameter()
    {
        epService = EPServiceProviderManager.getDefaultProvider(SupportConfigFactory.getConfiguration());
        epService.getEPAdministrator().getConfiguration().addImport(SupportStaticMethodLib.class);
        
        String text = "select " +
                "SupportStaticMethodLib.arraySumIntBoxed({1,2,null,3,4}) as v1, " +
                "SupportStaticMethodLib.arraySumDouble({1,2,3,4.0}) as v2, " +
                "SupportStaticMethodLib.arraySumString({'1','2','3','4'}) as v3, " +
                "SupportStaticMethodLib.arraySumObject({'1',2,3.0,'4.0'}) as v4 " +
                " from " + SupportBean.class.getName();
        listener = new SupportUpdateListener();
        EPStatement stmt = epService.getEPAdministrator().createEPL(text);
        stmt.addListener(listener);

        epService.getEPRuntime().sendEvent(new SupportBean("E1", 1));
        EPAssertionUtil.assertProps(listener.assertOneGetNewAndReset(), "v1,v2,v3,v4".split(","), new Object[]{10, 10d, 10d, 10d});
    }

    public void testNoParameters()
	{
        if (InstrumentationHelper.ENABLED) { InstrumentationHelper.endTest();} // not instrumented

        Long startTime = System.currentTimeMillis();
		statementText = "select System.currentTimeMillis() " + stream;
		Long result = (Long)createStatementAndGet("System.currentTimeMillis()");
		Long finishTime = System.currentTimeMillis();
		assertTrue(startTime <= result);
		assertTrue(result <= finishTime);

		statementText = "select java.lang.ClassLoader.getSystemClassLoader() " + stream;
		Object expected = ClassLoader.getSystemClassLoader();
		Object[] resultTwo = createStatementAndGetProperty(true, "java.lang.ClassLoader.getSystemClassLoader()");
		assertEquals(expected, resultTwo[0]);

		statementText = "select UnknownClass.invalidMethod() " + stream;
		try
		{
			createStatementAndGetProperty(true, "invalidMethod()");
			fail();
		}
		catch(EPStatementException e)
		{
			// Expected
		}
	}

    public void testSingleParameterOM() throws Exception
    {
        EPStatementObjectModel model = new EPStatementObjectModel();
        model.setSelectClause(SelectClause.create().add(Expressions.staticMethod("Integer", "toBinaryString", 7), "value"));
        model.setFromClause(FromClause.create(FilterStream.create(SupportMarketDataBean.class.getName()).addView("length", Expressions.constant(5))));
        model = (EPStatementObjectModel) SerializableObjectCopier.copy(model);
        statementText = "select Integer.toBinaryString(7) as value" + stream;

        assertEquals(statementText.trim(), model.toEPL());
        EPStatement statement = epService.getEPAdministrator().create(model);
        listener = new SupportUpdateListener();
        statement.addListener(listener);

        sendEvent("IBM", 10d, 4l);
        assertEquals(Integer.toBinaryString(7), listener.assertOneGetNewAndReset().get("value"));
    }

    public void testSingleParameterCompile() throws Exception
    {
        statementText = "select Integer.toBinaryString(7) as value" + stream;
        EPStatementObjectModel model = epService.getEPAdministrator().compileEPL(statementText);
        model = (EPStatementObjectModel) SerializableObjectCopier.copy(model);

        assertEquals(statementText.trim(), model.toEPL());
        EPStatement statement = epService.getEPAdministrator().create(model);
        listener = new SupportUpdateListener();
        statement.addListener(listener);

        sendEvent("IBM", 10d, 4l);
        assertEquals(Integer.toBinaryString(7), listener.assertOneGetNewAndReset().get("value"));
    }

    public void testSingleParameter()
	{
		statementText = "select Integer.toBinaryString(7) " + stream;
		Object[] result = createStatementAndGetProperty(true, "Integer.toBinaryString(7)");
		assertEquals(Integer.toBinaryString(7), result[0]);

		statementText = "select Integer.valueOf(\"6\") " + stream;
		result = createStatementAndGetProperty(true, "Integer.valueOf(\"6\")");
		assertEquals(Integer.valueOf("6"), result[0]);

		statementText = "select java.lang.String.valueOf(\'a\') " + stream;
		result = createStatementAndGetProperty(true, "java.lang.String.valueOf(\"a\")");
		assertEquals(String.valueOf('a'), result[0]);
	}

	public void testTwoParameters()
	{
		statementText = "select Math.max(2,3) " + stream;
		assertEquals(3, createStatementAndGetProperty(true, "Math.max(2,3)")[0]);

		statementText = "select java.lang.Math.max(2,3d) " + stream;
		assertEquals(3d, createStatementAndGetProperty(true, "java.lang.Math.max(2,3.0)")[0]);

		statementText = "select Long.parseLong(\"123\",10)" + stream;
		Object expected = Long.parseLong("123", 10);
		assertEquals(expected, createStatementAndGetProperty(true, "Long.parseLong(\"123\",10)")[0]);
	}

	public void testUserDefined()
	{
		String className = SupportStaticMethodLib.class.getName();
		statementText = "select " + className + ".staticMethod(2)" + stream;
		assertEquals(2, createStatementAndGetProperty(true, className + ".staticMethod(2)")[0]);

        // try context passed
        SupportStaticMethodLib.getMethodInvocationContexts().clear();
        statementText = "@Name('S0') select " + className + ".staticMethodWithContext(2)" + stream;
        assertEquals(2, createStatementAndGetProperty(true, className + ".staticMethodWithContext(2)")[0]);
        EPLMethodInvocationContext first = SupportStaticMethodLib.getMethodInvocationContexts().get(0);
        assertEquals("S0", first.getStatementName());
        assertEquals(epService.getURI(), first.getEngineURI());
        assertEquals(-1, first.getContextPartitionId());
        assertEquals("staticMethodWithContext", first.getFunctionName());
    }

	public void testComplexParameters()
	{
		statementText = "select String.valueOf(price) " + stream;
		Object[] result = createStatementAndGetProperty(true, "String.valueOf(price)");
		assertEquals(String.valueOf(10d), result[0]);

		statementText = "select String.valueOf(2 + 3*5) " + stream;
		result = createStatementAndGetProperty(true, "String.valueOf(2+3*5)");
		assertEquals(String.valueOf(17), result[0]);

		statementText = "select String.valueOf(price*volume +volume) " + stream;
		result = createStatementAndGetProperty(true, "String.valueOf(price*volume+volume)");
		assertEquals(String.valueOf(44d), result[0]);

		statementText = "select String.valueOf(Math.pow(price,Integer.valueOf(\"2\"))) " + stream;
		result = createStatementAndGetProperty(true, "String.valueOf(Math.pow(price,Integer.valueOf(\"2\")))");
		assertEquals(String.valueOf(100d), result[0]);
	}

	public void testMultipleMethodInvocations()
	{
		statementText = "select Math.max(2d,price), Math.max(volume,4d)" + stream;
		Object[] props = createStatementAndGetProperty(true, "Math.max(2.0,price)", "Math.max(volume,4.0)");
		assertEquals(10d, props[0]);
		assertEquals(4d, props[1]);
	}

	public void testOtherClauses()
	{
		// where
		statementText = "select *" + stream + "where Math.pow(price, .5) > 2";
		assertEquals("IBM", createStatementAndGetProperty(true, "symbol")[0]);
		sendEvent("CAT", 4d, 100);
		assertNull(getProperty("symbol"));

		// group-by
		statementText = "select symbol, sum(price)" + stream + "group by String.valueOf(symbol)";
		assertEquals(10d, createStatementAndGetProperty(true, "sum(price)")[0]);
		sendEvent("IBM", 4d, 100);
		assertEquals(14d, getProperty("sum(price)"));

		epService.initialize();

		// having
		statementText = "select symbol, sum(price)" + stream + "having Math.pow(sum(price), .5) > 3";
		assertEquals(10d, createStatementAndGetProperty(true, "sum(price)")[0]);
		sendEvent("IBM", 100d, 100);
		assertEquals(110d, getProperty("sum(price)"));

        // order-by
		statementText = "select symbol, price" + stream + "output every 3 events order by Math.pow(price, 2)";
		createStatementAndGetProperty(false, "symbol");
		sendEvent("CAT", 10d, 0L);
		sendEvent("MAT", 3d, 0L);

		EventBean[] newEvents = listener.getAndResetLastNewData();
		assertTrue(newEvents.length == 3);
		assertEquals("MAT", newEvents[0].get("symbol"));
		assertEquals("IBM", newEvents[1].get("symbol"));
		assertEquals("CAT", newEvents[2].get("symbol"));
	}

    public void testNestedFunction()
    {
        Configuration configuration = SupportConfigFactory.getConfiguration();
        configuration.addImport(SupportStaticMethodLib.class.getName());
        configuration.addEventType("Temperature", SupportTemperatureBean.class);
        epService = EPServiceProviderManager.getDefaultProvider(configuration);
        epService.initialize();

        String text = "select " +
                "SupportStaticMethodLib.appendPipe(SupportStaticMethodLib.delimitPipe('POLYGON ((100.0 100, \", 100 100, 400 400))'),temp.geom) as val" +
                " from Temperature as temp";
        EPStatement stmt = epService.getEPAdministrator().createEPL(text);
        SupportUpdateListener listener = new SupportUpdateListener();
        stmt.addListener(listener);

        epService.getEPRuntime().sendEvent(new SupportTemperatureBean("a"));
        assertEquals("|POLYGON ((100.0 100, \", 100 100, 400 400))||a", listener.assertOneGetNewAndReset().get("val"));
    }

    public void testPassthru()
    {
        Configuration configuration = SupportConfigFactory.getConfiguration();
        configuration.addImport(SupportStaticMethodLib.class.getName());
        configuration.addEventType("Temperature", SupportTemperatureBean.class);
        epService = EPServiceProviderManager.getDefaultProvider(configuration);
        epService.initialize();

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
    }

    public void testPerfConstantParameters()
    {
        if (InstrumentationHelper.ENABLED) { InstrumentationHelper.endTest();} // not instrumented

        Configuration configuration = SupportConfigFactory.getConfiguration();
        configuration.addImport(SupportStaticMethodLib.class.getName());
        configuration.addEventType("Temperature", SupportTemperatureBean.class);
        configuration.addPlugInSingleRowFunction("sleepme", SupportStaticMethodLib.class.getName(), "sleep", ConfigurationPlugInSingleRowFunction.ValueCache.ENABLED);
        epService = EPServiceProviderManager.getDefaultProvider(configuration);
        epService.initialize();

        String text = "select " +
                "SupportStaticMethodLib.sleep(100) as val" +
                " from Temperature as temp";
        EPStatement stmt = epService.getEPAdministrator().createEPL(text);
        SupportUpdateListener listener = new SupportUpdateListener();
        stmt.addListener(listener);

        long startTime = System.currentTimeMillis();
        for (int i = 0; i < 1000; i++)
        {
            epService.getEPRuntime().sendEvent(new SupportTemperatureBean("a"));
        }
        long endTime = System.currentTimeMillis();
        long delta = endTime - startTime;

        assertTrue("Failed perf test, delta=" + delta, delta < 2000);
        stmt.destroy();

        // test case with non-cache
        configuration.getEngineDefaults().getExpression().setUdfCache(false);
        epService = EPServiceProviderManager.getDefaultProvider(configuration);
        epService.initialize();

        stmt = epService.getEPAdministrator().createEPL(text);
        listener = new SupportUpdateListener();
        stmt.addListener(listener);

        startTime = System.currentTimeMillis();
        epService.getEPRuntime().sendEvent(new SupportTemperatureBean("a"));
        epService.getEPRuntime().sendEvent(new SupportTemperatureBean("a"));
        epService.getEPRuntime().sendEvent(new SupportTemperatureBean("a"));
        endTime = System.currentTimeMillis();
        delta = endTime - startTime;

        assertTrue("Failed perf test, delta=" + delta, delta > 120);
        stmt.destroy();
        
        // test plug-in single-row function
        String textSingleRow = "select " +
                "sleepme(100) as val" +
                " from Temperature as temp";
        EPStatement stmtSingleRow = epService.getEPAdministrator().createEPL(textSingleRow);
        SupportUpdateListener listenerSingleRow = new SupportUpdateListener();
        stmtSingleRow.addListener(listenerSingleRow);

        startTime = System.currentTimeMillis();
        for (int i = 0; i < 1000; i++)
        {
            epService.getEPRuntime().sendEvent(new SupportTemperatureBean("a"));
        }
        delta = System.currentTimeMillis() - startTime;

        assertTrue("Failed perf test, delta=" + delta, delta < 1000);
        stmtSingleRow.destroy();
    }

    public void testPerfConstantParametersNested()
    {
        Configuration configuration = SupportConfigFactory.getConfiguration();
        configuration.addImport(SupportStaticMethodLib.class.getName());
        configuration.addEventType("Temperature", SupportTemperatureBean.class);
        epService = EPServiceProviderManager.getDefaultProvider(configuration);
        epService.initialize();

        String text = "select " +
                "SupportStaticMethodLib.sleep(SupportStaticMethodLib.passthru(100)) as val" +
                " from Temperature as temp";
        EPStatement stmt = epService.getEPAdministrator().createEPL(text);
        SupportUpdateListener listener = new SupportUpdateListener();
        stmt.addListener(listener);

        long startTime = System.currentTimeMillis();
        for (int i = 0; i < 500; i++)
        {
            epService.getEPRuntime().sendEvent(new SupportTemperatureBean("a"));
        }
        long endTime = System.currentTimeMillis();
        long delta = endTime - startTime;

        assertTrue("Failed perf test, delta=" + delta, delta < 1000);
    }

    private Object createStatementAndGet(String propertyName)
	{
		EPStatement statement = epService.getEPAdministrator().createEPL(statementText);
		listener = new SupportUpdateListener();
		statement.addListener(listener);
		epService.getEPRuntime().sendEvent(new SupportMarketDataBean("IBM", 10d, 4l, ""));
		return getProperty(propertyName);
	}

	private Object getProperty(String propertyName)
	{
		EventBean[] newData = listener.getAndResetLastNewData();
		if(newData == null || newData.length == 0)
		{
			return null;
		}
		else
		{
			return newData[0].get(propertyName);
		}
	}

	private Object[] createStatementAndGetProperty(boolean expectResult, String... propertyNames)
	{
		EPStatement statement = epService.getEPAdministrator().createEPL(statementText);
		listener = new SupportUpdateListener();
		statement.addListener(listener);
		sendEvent("IBM", 10d, 4l);

        if (expectResult)
        {
            List<Object> properties = new ArrayList<Object>();
            EventBean theEvent = listener.getAndResetLastNewData()[0];
            for(String propertyName : propertyNames)
            {
                properties.add(theEvent.get(propertyName));
            }
            return properties.toArray(new Object[0]);
        }
        return null;
    }

	private void sendEvent(String symbol, double price, long volume)
	{
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

        public static Integer getValue(int input)
        {
            return input + 10;
        }
    }
}
