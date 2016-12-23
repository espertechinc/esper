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
import com.espertech.esper.client.hook.AggregationFunctionFactory;
import com.espertech.esper.client.scopetest.EPAssertionUtil;
import com.espertech.esper.client.scopetest.SupportUpdateListener;
import com.espertech.esper.client.soda.*;
import com.espertech.esper.epl.agg.aggregator.AggregationMethod;
import com.espertech.esper.epl.agg.service.AggregationValidationContext;
import com.espertech.esper.metrics.instrumentation.InstrumentationHelper;
import com.espertech.esper.supportregression.bean.SupportBean;
import com.espertech.esper.supportregression.bean.SupportBean_A;
import com.espertech.esper.supportregression.client.SupportConfigFactory;
import com.espertech.esper.supportregression.epl.*;
import com.espertech.esper.supportregression.util.SupportMessageAssertUtil;
import com.espertech.esper.supportregression.util.SupportModelHelper;
import com.espertech.esper.util.SerializableObjectCopier;
import junit.framework.TestCase;

public class TestAggregationFunctionPlugIn extends TestCase
{
    private EPServiceProvider epService;

    public void setUp()
    {
        Configuration configuration = SupportConfigFactory.getConfiguration();
        configuration.addPlugInAggregationFunctionFactory("concatstring", MyConcatAggregationFunctionFactory.class.getName());
        configuration.addPlugInAggregationFunctionFactory("concatstringTwo", MyConcatTwoAggFunctionFactory.class.getName());
        configuration.getEngineDefaults().getThreading().setEngineFairlock(true);
        epService = EPServiceProviderManager.getProvider("TestAggregationFunctionPlugIn", configuration);
        epService.initialize();
        if (InstrumentationHelper.ENABLED) { InstrumentationHelper.startTest(epService, this.getClass(), getName());}
    }

    public void tearDown()
    {
        if (InstrumentationHelper.ENABLED) { InstrumentationHelper.endTest();}
        epService.initialize();
    }

    public void testGrouped() throws Exception
    {
        String textOne = "select irstream CONCATSTRING(theString) as val from " + SupportBean.class.getName() + "#length(10) group by intPrimitive";
        tryGrouped(textOne, null);

        String textTwo = "select irstream concatstring(theString) as val from " + SupportBean.class.getName() + "#win:length(10) group by intPrimitive";
        tryGrouped(textTwo, null);

        String textThree = "select irstream concatstring(theString) as val from " + SupportBean.class.getName() + "#length(10) group by intPrimitive";
        EPStatementObjectModel model = epService.getEPAdministrator().compileEPL(textThree);
        SerializableObjectCopier.copy(model);
        assertEquals(textThree, model.toEPL());
        tryGrouped(null, model);

        String textFour = "select irstream concatstring(theString) as val from " + SupportBean.class.getName() + "#length(10) group by intPrimitive";
        EPStatementObjectModel modelTwo = new EPStatementObjectModel();
        modelTwo.setSelectClause(SelectClause.create().streamSelector(StreamSelector.RSTREAM_ISTREAM_BOTH)
                .add(Expressions.plugInAggregation("concatstring", Expressions.property("theString")), "val"));
        modelTwo.setFromClause(FromClause.create(FilterStream.create(SupportBean.class.getName()).addView(null, "length", Expressions.constant(10))));
        modelTwo.setGroupByClause(GroupByClause.create("intPrimitive"));
        assertEquals(textFour, modelTwo.toEPL());
        SerializableObjectCopier.copy(modelTwo);
        tryGrouped(null, modelTwo);

        String textFive = "select irstream concatstringTwo(theString) as val from " + SupportBean.class.getName() + "#length(10) group by intPrimitive";
        tryGrouped(textFive, null);
    }

    private void tryGrouped(String text, EPStatementObjectModel model)
    {
        EPStatement statement;
        if (model != null)
        {
            statement = epService.getEPAdministrator().create(model);
        }
        else
        {
            statement = epService.getEPAdministrator().createEPL(text);
        }
        SupportUpdateListener listener = new SupportUpdateListener();
        statement.addListener(listener);

        epService.getEPRuntime().sendEvent(new SupportBean("a", 1));
        EPAssertionUtil.assertPropsPerRow(listener.assertInvokedAndReset(), "val", new Object[] {"a"}, new Object[] {""});

        epService.getEPRuntime().sendEvent(new SupportBean("b", 2));
        EPAssertionUtil.assertPropsPerRow(listener.assertInvokedAndReset(), "val", new Object[] {"b"}, new Object[] {""});

        epService.getEPRuntime().sendEvent(new SupportBean("c", 1));
        EPAssertionUtil.assertPropsPerRow(listener.assertInvokedAndReset(), "val", new Object[] {"a c"}, new Object[] {"a"});

        epService.getEPRuntime().sendEvent(new SupportBean("d", 2));
        EPAssertionUtil.assertPropsPerRow(listener.assertInvokedAndReset(), "val", new Object[] {"b d"}, new Object[] {"b"});

        epService.getEPRuntime().sendEvent(new SupportBean("e", 1));
        EPAssertionUtil.assertPropsPerRow(listener.assertInvokedAndReset(), "val", new Object[] {"a c e"}, new Object[] {"a c"});

        epService.getEPRuntime().sendEvent(new SupportBean("f", 2));
        EPAssertionUtil.assertPropsPerRow(listener.assertInvokedAndReset(), "val", new Object[] {"b d f"}, new Object[] {"b d"});

        listener.reset();
    }

    public void testWindow()
    {
        String text = "select irstream concatstring(theString) as val from " + SupportBean.class.getName() + "#length(2)";
        EPStatement statement = epService.getEPAdministrator().createEPL(text);
        SupportUpdateListener listener = new SupportUpdateListener();
        statement.addListener(listener);

        epService.getEPRuntime().sendEvent(new SupportBean("a", -1));
        EPAssertionUtil.assertPropsPerRow(listener.assertInvokedAndReset(), "val", new Object[] {"a"}, new Object[] {""});

        epService.getEPRuntime().sendEvent(new SupportBean("b", -1));
        EPAssertionUtil.assertPropsPerRow(listener.assertInvokedAndReset(), "val", new Object[] {"a b"}, new Object[] {"a"});

        epService.getEPRuntime().sendEvent(new SupportBean("c", -1));
        EPAssertionUtil.assertPropsPerRow(listener.assertInvokedAndReset(), "val", new Object[] {"b c"}, new Object[] {"a b"});

        epService.getEPRuntime().sendEvent(new SupportBean("d", -1));
        EPAssertionUtil.assertPropsPerRow(listener.assertInvokedAndReset(), "val", new Object[] {"c d"}, new Object[] {"b c"});
        epService.getEPAdministrator().destroyAllStatements();
    }

    public void testDistinctAndStarParam()
    {
        epService.getEPAdministrator().getConfiguration().addEventType(SupportBean.class);

        // test *-parameter
        String textTwo = "select concatstring(*) as val from SupportBean";
        EPStatement statementTwo = epService.getEPAdministrator().createEPL(textTwo);
        SupportUpdateListener listenerTwo = new SupportUpdateListener();
        statementTwo.addListener(listenerTwo);

        epService.getEPRuntime().sendEvent(new SupportBean("d", -1));
        EPAssertionUtil.assertProps(listenerTwo.assertOneGetNewAndReset(), "val".split(","), new Object[] {"SupportBean(d, -1)"});

        epService.getEPRuntime().sendEvent(new SupportBean("e", 2));
        EPAssertionUtil.assertProps(listenerTwo.assertOneGetNewAndReset(), "val".split(","), new Object[] {"SupportBean(d, -1) SupportBean(e, 2)"});

        try {
            epService.getEPAdministrator().createEPL("select concatstring(*) as val from SupportBean#lastevent, SupportBean unidirectional");
        }
        catch (EPStatementException ex) {
            SupportMessageAssertUtil.assertMessage(ex, "Error starting statement: Failed to validate select-clause expression 'concatstring(*)': The 'concatstring' aggregation function requires that in joins or subqueries the stream-wildcard (stream-alias.*) syntax is used instead");
        }

        // test distinct
        String text = "select irstream concatstring(distinct theString) as val from SupportBean";
        EPStatement statement = epService.getEPAdministrator().createEPL(text);
        SupportUpdateListener listener = new SupportUpdateListener();
        statement.addListener(listener);

        epService.getEPRuntime().sendEvent(new SupportBean("a", -1));
        EPAssertionUtil.assertPropsPerRow(listener.assertInvokedAndReset(), "val", new Object[] {"a"}, new Object[] {""});

        epService.getEPRuntime().sendEvent(new SupportBean("b", -1));
        EPAssertionUtil.assertPropsPerRow(listener.assertInvokedAndReset(), "val", new Object[] {"a b"}, new Object[] {"a"});

        epService.getEPRuntime().sendEvent(new SupportBean("b", -1));
        EPAssertionUtil.assertPropsPerRow(listener.assertInvokedAndReset(), "val", new Object[] {"a b"}, new Object[] {"a b"});

        epService.getEPRuntime().sendEvent(new SupportBean("c", -1));
        EPAssertionUtil.assertPropsPerRow(listener.assertInvokedAndReset(), "val", new Object[] {"a b c"}, new Object[] {"a b"});

        epService.getEPRuntime().sendEvent(new SupportBean("a", -1));
        EPAssertionUtil.assertPropsPerRow(listener.assertInvokedAndReset(), "val", new Object[] {"a b c"}, new Object[] {"a b c"});
    }

    public void testArrayParamsAndDotMethod()
    {
        epService.getEPAdministrator().getConfiguration().addPlugInAggregationFunctionFactory("countback", SupportPluginAggregationMethodOneFactory.class.getName());

        String text = "select irstream countback({1,2,intPrimitive}) as val from " + SupportBean.class.getName();
        EPStatement statement = epService.getEPAdministrator().createEPL(text);
        SupportUpdateListener listener = new SupportUpdateListener();
        statement.addListener(listener);

        epService.getEPRuntime().sendEvent(new SupportBean());
        EPAssertionUtil.assertPropsPerRow(listener.assertInvokedAndReset(), "val", new Object[] {-1}, new Object[] {0});

        // test dot-method
        epService.getEPAdministrator().getConfiguration().addEventType(SupportBean_A.class);
        epService.getEPAdministrator().getConfiguration().addPlugInAggregationFunctionFactory("myagg", MyAggFuncFactory.class.getName());
        String[] fields = "val0,val1".split(",");
        epService.getEPAdministrator().createEPL("select (myagg(id)).getTheString() as val0, (myagg(id)).getIntPrimitive() as val1 from SupportBean_A").addListener(listener);
        
        epService.getEPRuntime().sendEvent(new SupportBean_A("A1"));
        EPAssertionUtil.assertProps(listener.assertOneGetNewAndReset(), fields, new Object[] {"XX", 1});
        assertEquals(1, MyAggFuncFactory.getInstanceCount());

        epService.getEPRuntime().sendEvent(new SupportBean_A("A2"));
        EPAssertionUtil.assertProps(listener.assertOneGetNewAndReset(), fields, new Object[] {"XX", 2});
    }

    public void testMultipleParams()
    {
        epService.getEPAdministrator().getConfiguration().addPlugInAggregationFunctionFactory("countboundary", SupportPluginAggregationMethodThreeFactory.class.getName());

        runAssertionMultipleParams(false);
        runAssertionMultipleParams(true);
    }

    private void runAssertionMultipleParams(boolean soda) {

        String text = "select irstream countboundary(1,10,intPrimitive,*) as val from " + SupportBean.class.getName();
        EPStatement statement = SupportModelHelper.createByCompileOrParse(epService, soda, text);
        SupportUpdateListener listener = new SupportUpdateListener();
        statement.addListener(listener);

        AggregationValidationContext validContext = SupportPluginAggregationMethodThreeFactory.getContexts().get(0);
        EPAssertionUtil.assertEqualsExactOrder(new Class[]{Integer.class, Integer.class, int.class, SupportBean.class}, validContext.getParameterTypes());
        EPAssertionUtil.assertEqualsExactOrder(new Object[]{1, 10, null, null}, validContext.getConstantValues());
        EPAssertionUtil.assertEqualsExactOrder(new boolean[]{true, true, false, false}, validContext.getIsConstantValue());

        SupportBean e1 = new SupportBean("E1", 5);
        epService.getEPRuntime().sendEvent(e1);
        EPAssertionUtil.assertPropsPerRow(listener.assertInvokedAndReset(), "val", new Object[] {1}, new Object[] {0});
        EPAssertionUtil.assertEqualsExactOrder(new Object[] {1, 10, 5, e1}, SupportPluginAggregationMethodThree.getLastEnterParameters());

        epService.getEPRuntime().sendEvent(new SupportBean("E1", 0));
        EPAssertionUtil.assertPropsPerRow(listener.assertInvokedAndReset(), "val", new Object[] {1}, new Object[] {1});

        epService.getEPRuntime().sendEvent(new SupportBean("E1", 11));
        EPAssertionUtil.assertPropsPerRow(listener.assertInvokedAndReset(), "val", new Object[] {1}, new Object[] {1});
        
        epService.getEPRuntime().sendEvent(new SupportBean("E1", 1));
        EPAssertionUtil.assertPropsPerRow(listener.assertInvokedAndReset(), "val", new Object[] {2}, new Object[] {1});

        epService.getEPAdministrator().destroyAllStatements();
    }

    public void testNoSubnodesRuntimeAdd()
    {
        epService.getEPAdministrator().getConfiguration().addPlugInAggregationFunctionFactory("countback", SupportPluginAggregationMethodOneFactory.class.getName());

        String text = "select irstream countback() as val from " + SupportBean.class.getName();
        EPStatement statement = epService.getEPAdministrator().createEPL(text);
        SupportUpdateListener listener = new SupportUpdateListener();
        statement.addListener(listener);

        epService.getEPRuntime().sendEvent(new SupportBean());
        EPAssertionUtil.assertPropsPerRow(listener.assertInvokedAndReset(), "val", new Object[]{-1}, new Object[]{0});

        epService.getEPRuntime().sendEvent(new SupportBean());
        EPAssertionUtil.assertPropsPerRow(listener.assertInvokedAndReset(), "val", new Object[] {-2}, new Object[] {-1});
    }

    public void testMappedPropertyLookAlike()
    {
        String text = "select irstream concatstring('a') as val from " + SupportBean.class.getName();
        EPStatement statement = epService.getEPAdministrator().createEPL(text);
        SupportUpdateListener listener = new SupportUpdateListener();
        statement.addListener(listener);
        assertEquals(String.class, statement.getEventType().getPropertyType("val"));

        epService.getEPRuntime().sendEvent(new SupportBean());
        EPAssertionUtil.assertPropsPerRow(listener.assertInvokedAndReset(), "val", new Object[]{"a"}, new Object[]{""});

        epService.getEPRuntime().sendEvent(new SupportBean());
        EPAssertionUtil.assertPropsPerRow(listener.assertInvokedAndReset(), "val", new Object[] {"a a"}, new Object[] {"a"});

        epService.getEPRuntime().sendEvent(new SupportBean());
        EPAssertionUtil.assertPropsPerRow(listener.assertInvokedAndReset(), "val", new Object[] {"a a a"}, new Object[] {"a a"});
    }

    public void testFailedValidation()
    {
        Configuration configuration = SupportConfigFactory.getConfiguration();
        configuration.addPlugInAggregationFunctionFactory("concat", SupportPluginAggregationMethodTwoFactory.class.getName());
        epService = EPServiceProviderManager.getDefaultProvider(configuration);
        epService.initialize();

        try
        {
            String text = "select concat(1) from " + SupportBean.class.getName();
            epService.getEPAdministrator().createEPL(text);
        }
        catch (EPStatementException ex)
        {
            SupportMessageAssertUtil.assertMessage(ex, "Error starting statement: Failed to validate select-clause expression 'concat(1)': Plug-in aggregation function 'concat' failed validation: Invalid parameter type 'java.lang.Integer', expecting string [");
        }
    }

    public void testInvalidUse()
    {
        Configuration configuration = SupportConfigFactory.getConfiguration();
        configuration.addPlugInAggregationFunctionFactory("xxx", String.class.getName());
        configuration.addPlugInAggregationFunctionFactory("yyy", "com.NoSuchClass");
        epService = EPServiceProviderManager.getDefaultProvider(configuration);
        epService.initialize();

        SupportMessageAssertUtil.tryInvalid(epService, "select * from " + SupportBean.class.getName() + " group by xxx(1)",
            "Error in expression: Error resolving aggregation: Aggregation class by name 'java.lang.String' does not implement AggregationFunctionFactory");

        SupportMessageAssertUtil.tryInvalid(epService, "select * from " + SupportBean.class.getName() + " group by yyy(1)",
            "Error in expression: Error resolving aggregation: Could not load aggregation factory class by name 'com.NoSuchClass'");
    }

    public void testInvalidConfigure()
    {
        tryInvalidConfigure("a b", "MyClass");
        tryInvalidConfigure("abc", "My Class");

        // configure twice
        try
        {
            epService.getEPAdministrator().getConfiguration().addPlugInAggregationFunctionFactory("concatstring", MyConcatAggregationFunction.class.getName());
            fail();
        }
        catch (ConfigurationException ex)
        {
            // expected
        }
    }

    private void tryInvalidConfigure(String funcName, String className)
    {
        try
        {
            epService.getEPAdministrator().getConfiguration().addPlugInAggregationFunctionFactory(funcName, className);
            fail();
        }
        catch (ConfigurationException ex)
        {
            // expected
        }
    }

    public void testInvalid()
    {
        SupportMessageAssertUtil.tryInvalid(epService, "select xxx(theString) from " + SupportBean.class.getName(),
                "Error starting statement: Failed to validate select-clause expression 'xxx(theString)': Unknown single-row function, aggregation function or mapped or indexed property named 'xxx' could not be resolved");
    }

    public static class MyAggFuncFactory implements AggregationFunctionFactory {
        private static int instanceCount;

        public static int getInstanceCount() {
            return instanceCount;
        }

        public void setFunctionName(String functionName) {
        }

        public void validate(AggregationValidationContext validationContext) {
        }

        public AggregationMethod newAggregator() {
            instanceCount++;
            return new MyAggFuncMethod();
        }

        public Class getValueType() {
            return SupportBean.class;
        }
    }

    public static class MyAggFuncMethod implements AggregationMethod {

        private int count;

        public void enter(Object value) {
            count++;
        }

        public void leave(Object value) {
            count--;
        }

        public Object getValue() {
            return new SupportBean("XX", count);
        }

        public void clear() {
            count = 0;
        }
    }
}
