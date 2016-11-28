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

package com.espertech.esper.regression.client;

import com.espertech.esper.client.*;
import com.espertech.esper.client.hook.EPLMethodInvocationContext;
import com.espertech.esper.client.scopetest.EPAssertionUtil;
import com.espertech.esper.client.scopetest.SupportUpdateListener;
import com.espertech.esper.client.soda.EPStatementObjectModel;
import com.espertech.esper.collection.UniformPair;
import com.espertech.esper.metrics.instrumentation.InstrumentationHelper;
import com.espertech.esper.support.bean.ISupportBImpl;
import com.espertech.esper.support.bean.SupportBean;
import com.espertech.esper.support.client.SupportConfigFactory;
import junit.framework.TestCase;

import java.io.StringWriter;
import java.util.Collection;

public class TestSingleRowFunctionPlugIn extends TestCase
{
    private EPServiceProvider epService;
    private SupportUpdateListener listener;

    public void setUp()
    {
        listener = new SupportUpdateListener();

        Configuration configuration = SupportConfigFactory.getConfiguration();
        configuration.addPlugInSingleRowFunction("power3", MySingleRowFunction.class.getName(), "computePower3");
        configuration.addPlugInSingleRowFunction("chainTop", MySingleRowFunction.class.getName(), "getChainTop");
        configuration.addPlugInSingleRowFunction("surroundx", MySingleRowFunction.class.getName(), "surroundx");
        configuration.addPlugInSingleRowFunction("throwExceptionLogMe", MySingleRowFunction.class.getName(), "throwexception", ConfigurationPlugInSingleRowFunction.ValueCache.DISABLED, ConfigurationPlugInSingleRowFunction.FilterOptimizable.ENABLED, false);
        configuration.addPlugInSingleRowFunction("throwExceptionRethrow", MySingleRowFunction.class.getName(), "throwexception", ConfigurationPlugInSingleRowFunction.ValueCache.DISABLED, ConfigurationPlugInSingleRowFunction.FilterOptimizable.ENABLED, true);
        configuration.addPlugInSingleRowFunction("power3Rethrow", MySingleRowFunction.class.getName(), "computePower3", ConfigurationPlugInSingleRowFunction.ValueCache.DISABLED, ConfigurationPlugInSingleRowFunction.FilterOptimizable.ENABLED, true);
        configuration.addPlugInSingleRowFunction("power3Context", MySingleRowFunction.class.getName(), "computePower3WithContext", ConfigurationPlugInSingleRowFunction.ValueCache.DISABLED, ConfigurationPlugInSingleRowFunction.FilterOptimizable.ENABLED, true);
        configuration.addPlugInSingleRowFunction("isNullValue", MySingleRowFunction.class.getName(), "isNullValue");
        configuration.addPlugInSingleRowFunction("getValueAsString", MySingleRowFunction.class.getName(), "getValueAsString");
        configuration.addPlugInSingleRowFunction("eventsCheckStrings", MySingleRowFunction.class.getName(), "eventsCheckStrings");
        configuration.addPlugInSingleRowFunction("varargsOnlyInt", MySingleRowFunction.class.getName(), "varargsOnlyInt");
        configuration.addPlugInSingleRowFunction("varargsOnlyString", MySingleRowFunction.class.getName(), "varargsOnlyString");
        configuration.addPlugInSingleRowFunction("varargsOnlyObject", MySingleRowFunction.class.getName(), "varargsOnlyObject");
        configuration.addPlugInSingleRowFunction("varargsOnlyNumber", MySingleRowFunction.class.getName(), "varargsOnlyNumber");
        configuration.addPlugInSingleRowFunction("varargsOnlyISupportBaseAB", MySingleRowFunction.class.getName(), "varargsOnlyISupportBaseAB");
        configuration.addPlugInSingleRowFunction("varargsW1Param", MySingleRowFunction.class.getName(), "varargsW1Param");
        configuration.addPlugInSingleRowFunction("varargsW2Param", MySingleRowFunction.class.getName(), "varargsW2Param");
        configuration.addPlugInSingleRowFunction("varargsOnlyWCtx", MySingleRowFunction.class.getName(), "varargsOnlyWCtx");
        configuration.addPlugInSingleRowFunction("varargsW1ParamWCtx", MySingleRowFunction.class.getName(), "varargsW1ParamWCtx");
        configuration.addPlugInSingleRowFunction("varargsW2ParamWCtx", MySingleRowFunction.class.getName(), "varargsW2ParamWCtx");
        configuration.addPlugInSingleRowFunction("varargsObjectsWCtx", MySingleRowFunction.class.getName(), "varargsObjectsWCtx");
        configuration.addPlugInSingleRowFunction("varargsW1ParamObjectsWCtx", MySingleRowFunction.class.getName(), "varargsW1ParamObjectsWCtx");
        epService = EPServiceProviderManager.getDefaultProvider(configuration);
        epService.initialize();
        if (InstrumentationHelper.ENABLED) { InstrumentationHelper.startTest(epService, this.getClass(), getName());}

        epService.getEPAdministrator().getConfiguration().addEventType("SupportBean", SupportBean.class);
    }

    public void tearDown() {
        if (InstrumentationHelper.ENABLED) { InstrumentationHelper.endTest();}
        listener = null;
    }

    public void testVarargs() {
        runVarargAssertion(
                makePair("varargsOnlyInt(1, 2, 3, 4)", "1,2,3,4"),
                makePair("varargsOnlyInt(1, 2, 3)", "1,2,3"),
                makePair("varargsOnlyInt(1, 2)", "1,2"),
                makePair("varargsOnlyInt(1)", "1"),
                makePair("varargsOnlyInt()", ""));

        runVarargAssertion(
                makePair("varargsW1Param('abc', 1.0, 2.0)", "abc,1.0,2.0"),
                makePair("varargsW1Param('abc', 1, 2)", "abc,1.0,2.0"),
                makePair("varargsW1Param('abc', 1)", "abc,1.0"),
                makePair("varargsW1Param('abc')", "abc"));

        runVarargAssertion(
                makePair("varargsW2Param(1, 2.0, 3L, 4L)", "1,2.0,3,4"),
                makePair("varargsW2Param(1, 2.0, 3L)", "1,2.0,3"),
                makePair("varargsW2Param(1, 2.0)", "1,2.0"),
                makePair("varargsW2Param(1, 2.0, 3, 4L)", "1,2.0,3,4"),
                makePair("varargsW2Param(1, 2.0, 3L, 4L)", "1,2.0,3,4"),
                makePair("varargsW2Param(1, 2.0, 3, 4)", "1,2.0,3,4"),
                makePair("varargsW2Param(1, 2.0, 3L, 4)", "1,2.0,3,4"));

        runVarargAssertion(
                makePair("varargsOnlyWCtx(1, 2, 3)", "CTX+1,2,3"),
                makePair("varargsOnlyWCtx(1, 2)", "CTX+1,2"),
                makePair("varargsOnlyWCtx(1)", "CTX+1"),
                makePair("varargsOnlyWCtx()", "CTX+"));

        runVarargAssertion(
                makePair("varargsW1ParamWCtx('a', 1, 2, 3)", "CTX+a,1,2,3"),
                makePair("varargsW1ParamWCtx('a', 1, 2)", "CTX+a,1,2"),
                makePair("varargsW1ParamWCtx('a', 1)", "CTX+a,1"),
                makePair("varargsW1ParamWCtx('a')", "CTX+a,"));

        runVarargAssertion(
                makePair("varargsW2ParamWCtx('a', 'b', 1, 2, 3)", "CTX+a,b,1,2,3"),
                makePair("varargsW2ParamWCtx('a', 'b', 1, 2)", "CTX+a,b,1,2"),
                makePair("varargsW2ParamWCtx('a', 'b', 1)", "CTX+a,b,1"),
                makePair("varargsW2ParamWCtx('a', 'b')", "CTX+a,b,"),
                makePair(MySingleRowFunction.class.getName() + ".varargsW2ParamWCtx('a', 'b')", "CTX+a,b,"));

        runVarargAssertion(
                makePair("varargsOnlyObject('a', 1, new BigInteger('2'))", "a,1,2"));

        runVarargAssertion(
                makePair("varargsOnlyNumber(1f, 2L, 3, new BigInteger('4'))", "1.0,2,3,4"));

        runVarargAssertion(
                makePair("varargsOnlyNumber(1f, 2L, 3, new BigInteger('4'))", "1.0,2,3,4"));

        runVarargAssertion(
                makePair("varargsOnlyISupportBaseAB(new " + ISupportBImpl.class.getName() + "('a', 'b'))", "ISupportBImpl{valueB='a', valueBaseAB='b'}"));

        // tests for array-passthru
        runVarargAssertion(
                makePair("varargsOnlyString({'a'})", "a"),
                makePair("varargsOnlyString({'a', 'b'})", "a,b"),
                makePair("varargsOnlyObject({'a', 'b'})", "a,b"),
                makePair("varargsOnlyObject({})", ""),
                makePair("varargsObjectsWCtx({1, 'a'})", "CTX+1,a"),
                makePair("varargsW1ParamObjectsWCtx(1, {'a', 1})", "CTX+,1,a,1")
                );

        // try Arrays.asList
        runAssertionArraysAsList();
    }

    public void testEventBeanFootprint() {
        epService.getEPAdministrator().getConfiguration().addImport(this.getClass());

        // test select-clause
        String fields[] = new String[] {"c0", "c1"};
        String text = "select isNullValue(*, 'theString') as c0," +
                "TestSingleRowFunctionPlugIn.localIsNullValue(*, 'theString') as c1 from SupportBean";
        EPStatement stmt = epService.getEPAdministrator().createEPL(text);
        stmt.addListener(listener);

        epService.getEPRuntime().sendEvent(new SupportBean("a", 1));
        EPAssertionUtil.assertProps(listener.assertOneGetNewAndReset(), fields, new Object[]{false, false});

        epService.getEPRuntime().sendEvent(new SupportBean(null, 2));
        EPAssertionUtil.assertProps(listener.assertOneGetNewAndReset(), fields, new Object[]{true, true});
        stmt.destroy();

        // test pattern
        String textPattern = "select * from pattern [a=SupportBean -> b=SupportBean(theString=getValueAsString(a, 'theString'))]";
        EPStatement stmtPattern = epService.getEPAdministrator().createEPL(textPattern);
        stmtPattern.addListener(listener);
        epService.getEPRuntime().sendEvent(new SupportBean("E1", 1));
        epService.getEPRuntime().sendEvent(new SupportBean("E1", 2));
        EPAssertionUtil.assertProps(listener.assertOneGetNewAndReset(), "a.intPrimitive,b.intPrimitive".split(","), new Object[] {1, 2});
        stmtPattern.destroy();

        // test filter
        String textFilter = "select * from SupportBean('E1'=getValueAsString(*, 'theString'))";
        EPStatement stmtFilter = epService.getEPAdministrator().createEPL(textFilter);
        stmtFilter.addListener(listener);
        epService.getEPRuntime().sendEvent(new SupportBean("E2", 1));
        epService.getEPRuntime().sendEvent(new SupportBean("E1", 2));
        assertEquals(1, listener.getAndResetLastNewData().length);
        stmtFilter.destroy();

        // test "first"
        String textAccessAgg = "select * from SupportBean#keepall having 'E2' = getValueAsString(last(*), 'theString')";
        EPStatement stmtAccessAgg = epService.getEPAdministrator().createEPL(textAccessAgg);
        stmtAccessAgg.addListener(listener);
        epService.getEPRuntime().sendEvent(new SupportBean("E2", 1));
        epService.getEPRuntime().sendEvent(new SupportBean("E1", 2));
        assertEquals(1, listener.getAndResetLastNewData().length);
        stmtAccessAgg.destroy();

        // test "window"
        String textWindowAgg = "select * from SupportBean#keepall having eventsCheckStrings(window(*), 'theString', 'E1')";
        EPStatement stmtWindowAgg = epService.getEPAdministrator().createEPL(textWindowAgg);
        stmtWindowAgg.addListener(listener);
        epService.getEPRuntime().sendEvent(new SupportBean("E2", 1));
        epService.getEPRuntime().sendEvent(new SupportBean("E1", 2));
        assertEquals(1, listener.getAndResetLastNewData().length);
        stmtWindowAgg.destroy();
    }

    public void testPropertyOrSingleRowMethod() throws Exception
    {
        String text = "select surroundx('test') as val from SupportBean";
        EPStatement stmt = epService.getEPAdministrator().createEPL(text);
        stmt.addListener(listener);

        String fields[] = new String[] {"val"};
        epService.getEPRuntime().sendEvent(new SupportBean("a", 3));
        EPAssertionUtil.assertProps(listener.assertOneGetNewAndReset(), fields, new Object[]{"XtestX"});
    }

    public void testChainMethod() throws Exception
    {
        String text = "select chainTop().chainValue(12,intPrimitive) as val from SupportBean";
        EPStatement stmt = epService.getEPAdministrator().createEPL(text);
        stmt.addListener(listener);

        runAssertionChainMethod();

        stmt.destroy();
        EPStatementObjectModel model = epService.getEPAdministrator().compileEPL(text);
        assertEquals(text, model.toEPL());
        stmt = epService.getEPAdministrator().create(model);
        assertEquals(text, stmt.getText());
        stmt.addListener(listener);

        runAssertionChainMethod();
    }

    public void testSingleMethod() throws Exception
    {
        String text = "select power3(intPrimitive) as val from SupportBean";
        EPStatement stmt = epService.getEPAdministrator().createEPL(text);
        stmt.addListener(listener);

        runAssertionSingleMethod();

        stmt.destroy();
        EPStatementObjectModel model = epService.getEPAdministrator().compileEPL(text);
        assertEquals(text, model.toEPL());
        stmt = epService.getEPAdministrator().create(model);
        assertEquals(text, stmt.getText());
        stmt.addListener(listener);

        runAssertionSingleMethod();

        stmt.destroy();
        text = "select power3(2) as val from SupportBean";
        stmt = epService.getEPAdministrator().createEPL(text);
        stmt.addListener(listener);

        runAssertionSingleMethod();
        stmt.destroy();

        // test passing a context as well
        text = "@Name('A') select power3Context(intPrimitive) as val from SupportBean";
        stmt = epService.getEPAdministrator().createEPL(text, (Object) "my_user_object");
        stmt.addListener(listener);

        MySingleRowFunction.getMethodInvokeContexts().clear();
        runAssertionSingleMethod();
        EPLMethodInvocationContext context = MySingleRowFunction.getMethodInvokeContexts().get(0);
        assertEquals("A", context.getStatementName());
        assertEquals(epService.getURI(), context.getEngineURI());
        assertEquals(-1, context.getContextPartitionId());
        assertEquals("power3Context", context.getFunctionName());
        assertEquals("my_user_object", context.getStatementUserObject());

        stmt.destroy();

        // test exception behavior
        // logged-only
        epService.getEPAdministrator().createEPL("select throwExceptionLogMe() from SupportBean").addListener(listener);
        epService.getEPRuntime().sendEvent(new SupportBean("E1", 1));
        epService.getEPAdministrator().destroyAllStatements();

        // rethrow
        epService.getEPAdministrator().createEPL("@Name('S0') select throwExceptionRethrow() from SupportBean").addListener(listener);
        try {
            epService.getEPRuntime().sendEvent(new SupportBean("E1", 1));
            fail();
        }
        catch (EPException ex) {
            assertEquals("java.lang.RuntimeException: Unexpected exception in statement 'S0': Invocation exception when invoking method 'throwexception' of class 'com.espertech.esper.regression.client.MySingleRowFunction' passing parameters [] for statement 'S0': RuntimeException : This is a 'throwexception' generated exception", ex.getMessage());
            epService.getEPAdministrator().destroyAllStatements();
        }

        // NPE when boxed is null
        epService.getEPAdministrator().createEPL("@Name('S1') select power3Rethrow(intBoxed) from SupportBean").addListener(listener);
        try {
            epService.getEPRuntime().sendEvent(new SupportBean("E1", 1));
            fail();
        }
        catch (EPException ex) {
            assertEquals("java.lang.RuntimeException: Unexpected exception in statement 'S1': NullPointerException invoking method 'computePower3' of class 'com.espertech.esper.regression.client.MySingleRowFunction' in parameter 0 passing parameters [null] for statement 'S1': The method expects a primitive int value but received a null value", ex.getMessage());
        }
    }

    private void runAssertionChainMethod()
    {
        String fields[] = new String[] {"val"};
        epService.getEPRuntime().sendEvent(new SupportBean("a", 3));
        EPAssertionUtil.assertProps(listener.assertOneGetNewAndReset(), fields, new Object[]{36});

        listener.reset();
    }

    private void runAssertionSingleMethod()
    {
        String fields[] = new String[] {"val"};
        epService.getEPRuntime().sendEvent(new SupportBean("a", 2));
        EPAssertionUtil.assertProps(listener.assertOneGetNewAndReset(), fields, new Object[]{8});

        listener.reset();
    }

    public void testFailedValidation()
    {
        Configuration configuration = SupportConfigFactory.getConfiguration();
        configuration.addPlugInSingleRowFunction("singlerow", MySingleRowFunctionTwo.class.getName(), "testSingleRow");
        epService = EPServiceProviderManager.getDefaultProvider(configuration);
        epService.initialize();

        try
        {
            String text = "select singlerow('a', 'b') from " + SupportBean.class.getName();
            epService.getEPAdministrator().createEPL(text);
        }
        catch (EPStatementException ex)
        {
            assertEquals("Error starting statement: Failed to validate select-clause expression 'singlerow(\"a\",\"b\")': Could not find static method named 'testSingleRow' in class 'com.espertech.esper.regression.client.MySingleRowFunctionTwo' with matching parameter number and expected parameter type(s) 'String, String' (nearest match found was 'testSingleRow' taking type(s) 'String, int') [select singlerow('a', 'b') from com.espertech.esper.support.bean.SupportBean]", ex.getMessage());
        }
    }

    public void testInvalidConfigure()
    {
        tryInvalidConfigure("a b", "MyClass", "some");
        tryInvalidConfigure("abc", "My Class", "other s");

        // configured twice
        try
        {
            epService.getEPAdministrator().getConfiguration().addPlugInSingleRowFunction("concatstring", MySingleRowFunction.class.getName(), "xyz");
            epService.getEPAdministrator().getConfiguration().addPlugInAggregationFunctionFactory("concatstring", MyConcatAggregationFunctionFactory.class.getName());
            fail();
        }
        catch (ConfigurationException ex)
        {
            // expected
        }

        // configured twice
        try
        {
            epService.getEPAdministrator().getConfiguration().addPlugInAggregationFunctionFactory("teststring", MyConcatAggregationFunctionFactory.class.getName());
            epService.getEPAdministrator().getConfiguration().addPlugInSingleRowFunction("teststring", MySingleRowFunction.class.getName(), "xyz");
            fail();
        }
        catch (ConfigurationException ex)
        {
            // expected
        }
    }

    private void tryInvalidConfigure(String funcName, String className, String methodName)
    {
        try
        {
            epService.getEPAdministrator().getConfiguration().addPlugInSingleRowFunction(funcName, className, methodName);
            fail();
        }
        catch (ConfigurationException ex)
        {
            // expected
        }
    }

    public static boolean localIsNullValue(EventBean event, String propertyName) {
        return event.get(propertyName) == null;
    }

    private void runVarargAssertion(UniformPair<String> ... pairs) {
        StringWriter buf = new StringWriter();
        buf.append("@name('test') select ");
        int count = 0;
        for (UniformPair<String> pair : pairs) {
            buf.append(pair.getFirst());
            buf.append(" as c");
            buf.append(Integer.toString(count));
            count++;
            buf.append(",");
        }
        buf.append("intPrimitive from SupportBean");

        epService.getEPAdministrator().createEPL(buf.toString()).addListener(listener);

        epService.getEPRuntime().sendEvent(new SupportBean());
        EventBean out = listener.assertOneGetNewAndReset();

        count = 0;
        for (UniformPair<String> pair : pairs) {
            assertEquals("failed for '" + pair.getFirst() + "'", pair.getSecond(), out.get("c" + count));
            count++;
        }
        epService.getEPAdministrator().getStatement("test").destroy();
    }

    private UniformPair<String> makePair(String expression, String expected) {
        return new UniformPair<String>(expression, expected);
    }

    private void runAssertionArraysAsList() {
        EPStatement stmt = epService.getEPAdministrator().createEPL("select " +
                "java.util.Arrays.asList('a') as c0, " +
                "java.util.Arrays.asList({'a'}) as c1, " +
                "java.util.Arrays.asList('a', 'b') as c2, " +
                "java.util.Arrays.asList({'a', 'b'}) as c3 " +
                "from SupportBean");
        stmt.addListener(listener);

        epService.getEPRuntime().sendEvent(new SupportBean());
        EventBean event = listener.assertOneGetNewAndReset();
        assertEqualsColl(event, "c0", "a");
        assertEqualsColl(event, "c1", "a");
        assertEqualsColl(event, "c2", "a", "b");
        assertEqualsColl(event, "c3", "a", "b");

        stmt.destroy();
    }

    private void assertEqualsColl(EventBean event, String property, String ... values) {
        Collection data = (Collection) event.get(property);
        EPAssertionUtil.assertEqualsExactOrder(values, data.toArray());
    }
}
