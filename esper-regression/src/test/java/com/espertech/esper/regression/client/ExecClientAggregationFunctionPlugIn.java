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
package com.espertech.esper.regression.client;

import com.espertech.esper.client.*;
import com.espertech.esper.client.hook.*;
import com.espertech.esper.client.scopetest.EPAssertionUtil;
import com.espertech.esper.client.scopetest.SupportUpdateListener;
import com.espertech.esper.client.soda.*;
import com.espertech.esper.epl.agg.aggregator.AggregationMethod;
import com.espertech.esper.epl.agg.service.common.AggregationValidationContext;
import com.espertech.esper.supportregression.bean.SupportBean;
import com.espertech.esper.supportregression.bean.SupportBean_A;
import com.espertech.esper.supportregression.client.MyConcatAggregationFunction;
import com.espertech.esper.supportregression.client.MyConcatAggregationFunctionFactory;
import com.espertech.esper.supportregression.client.MyConcatNoCodegenAggFunctionFactory;
import com.espertech.esper.supportregression.client.MyConcatTwoAggFunctionFactory;
import com.espertech.esper.supportregression.epl.SupportPluginAggregationMethodOneFactory;
import com.espertech.esper.supportregression.epl.SupportPluginAggregationMethodThree;
import com.espertech.esper.supportregression.epl.SupportPluginAggregationMethodThreeFactory;
import com.espertech.esper.supportregression.epl.SupportPluginAggregationMethodTwoFactory;
import com.espertech.esper.supportregression.execution.RegressionExecution;
import com.espertech.esper.supportregression.util.SupportMessageAssertUtil;
import com.espertech.esper.supportregression.util.SupportModelHelper;
import com.espertech.esper.util.SerializableObjectCopier;

import static com.espertech.esper.codegen.model.expression.CodegenExpressionBuilder.*;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.fail;

public class ExecClientAggregationFunctionPlugIn implements RegressionExecution {
    public void configure(Configuration configuration) throws Exception {
        configuration.addPlugInAggregationFunctionFactory("concatstring", MyConcatAggregationFunctionFactory.class.getName());
        configuration.addPlugInAggregationFunctionFactory("concatstringTwo", MyConcatTwoAggFunctionFactory.class.getName());
        configuration.addPlugInAggregationFunctionFactory("concatstringNoCodegen", MyConcatNoCodegenAggFunctionFactory.class.getName());
        configuration.addPlugInAggregationFunctionFactory("concat", SupportPluginAggregationMethodTwoFactory.class.getName());
        configuration.addPlugInAggregationFunctionFactory("xxx", String.class.getName());
        configuration.addPlugInAggregationFunctionFactory("yyy", "com.NoSuchClass");
        configuration.getEngineDefaults().getThreading().setEngineFairlock(true);
    }

    public void run(EPServiceProvider epService) throws Exception {
        runAssertionGrouped(epService);
        runAssertionWindow(epService);
        runAssertionDistinctAndStarParam(epService);
        runAssertionArrayParamsAndDotMethod(epService);
        runAssertionMultipleParams(epService);
        runAssertionNoSubnodesRuntimeAdd(epService);
        runAssertionMappedPropertyLookAlike(epService);
        runAssertionFailedValidation(epService);
        runAssertionInvalidUse(epService);
        runAssertionInvalidConfigure(epService);
        runAssertionInvalid(epService);
        runAssertionNoCodegeneration(epService);
    }

    private void runAssertionNoCodegeneration(EPServiceProvider epService) {
        String epl = "select concatstringNoCodegen(theString) as c0 from " + SupportBean.class.getName() + "#length(2)";
        EPStatement stmt = epService.getEPAdministrator().createEPL(epl);
        SupportUpdateListener listener = new SupportUpdateListener();
        stmt.addListener(listener);
        String[] fields = "c0".split(",");

        epService.getEPRuntime().sendEvent(new SupportBean("E1", 0));
        EPAssertionUtil.assertProps(listener.assertOneGetNewAndReset(), fields, new Object[] {"E1"});

        epService.getEPRuntime().sendEvent(new SupportBean("E2", 0));
        EPAssertionUtil.assertProps(listener.assertOneGetNewAndReset(), fields, new Object[] {"E1 E2"});

        epService.getEPRuntime().sendEvent(new SupportBean("E3", 0));
        EPAssertionUtil.assertProps(listener.assertOneGetNewAndReset(), fields, new Object[] {"E2 E3"});
    }

    private void runAssertionGrouped(EPServiceProvider epService) throws Exception {
        String textOne = "select irstream CONCATSTRING(theString) as val from " + SupportBean.class.getName() + "#length(10) group by intPrimitive";
        tryGrouped(epService, textOne, null);

        String textTwo = "select irstream concatstring(theString) as val from " + SupportBean.class.getName() + "#win:length(10) group by intPrimitive";
        tryGrouped(epService, textTwo, null);

        String textThree = "select irstream concatstring(theString) as val from " + SupportBean.class.getName() + "#length(10) group by intPrimitive";
        EPStatementObjectModel model = epService.getEPAdministrator().compileEPL(textThree);
        SerializableObjectCopier.copy(model);
        assertEquals(textThree, model.toEPL());
        tryGrouped(epService, null, model);

        String textFour = "select irstream concatstring(theString) as val from " + SupportBean.class.getName() + "#length(10) group by intPrimitive";
        EPStatementObjectModel modelTwo = new EPStatementObjectModel();
        modelTwo.setSelectClause(SelectClause.create().streamSelector(StreamSelector.RSTREAM_ISTREAM_BOTH)
                .add(Expressions.plugInAggregation("concatstring", Expressions.property("theString")), "val"));
        modelTwo.setFromClause(FromClause.create(FilterStream.create(SupportBean.class.getName()).addView(null, "length", Expressions.constant(10))));
        modelTwo.setGroupByClause(GroupByClause.create("intPrimitive"));
        assertEquals(textFour, modelTwo.toEPL());
        SerializableObjectCopier.copy(modelTwo);
        tryGrouped(epService, null, modelTwo);

        String textFive = "select irstream concatstringTwo(theString) as val from " + SupportBean.class.getName() + "#length(10) group by intPrimitive";
        tryGrouped(epService, textFive, null);

        epService.getEPAdministrator().destroyAllStatements();
    }

    private void tryGrouped(EPServiceProvider epService, String text, EPStatementObjectModel model) {
        EPStatement statement;
        if (model != null) {
            statement = epService.getEPAdministrator().create(model);
        } else {
            statement = epService.getEPAdministrator().createEPL(text);
        }
        SupportUpdateListener listener = new SupportUpdateListener();
        statement.addListener(listener);

        epService.getEPRuntime().sendEvent(new SupportBean("a", 1));
        EPAssertionUtil.assertPropsPerRow(listener.assertInvokedAndReset(), "val", new Object[]{"a"}, new Object[]{""});

        epService.getEPRuntime().sendEvent(new SupportBean("b", 2));
        EPAssertionUtil.assertPropsPerRow(listener.assertInvokedAndReset(), "val", new Object[]{"b"}, new Object[]{""});

        epService.getEPRuntime().sendEvent(new SupportBean("c", 1));
        EPAssertionUtil.assertPropsPerRow(listener.assertInvokedAndReset(), "val", new Object[]{"a c"}, new Object[]{"a"});

        epService.getEPRuntime().sendEvent(new SupportBean("d", 2));
        EPAssertionUtil.assertPropsPerRow(listener.assertInvokedAndReset(), "val", new Object[]{"b d"}, new Object[]{"b"});

        epService.getEPRuntime().sendEvent(new SupportBean("e", 1));
        EPAssertionUtil.assertPropsPerRow(listener.assertInvokedAndReset(), "val", new Object[]{"a c e"}, new Object[]{"a c"});

        epService.getEPRuntime().sendEvent(new SupportBean("f", 2));
        EPAssertionUtil.assertPropsPerRow(listener.assertInvokedAndReset(), "val", new Object[]{"b d f"}, new Object[]{"b d"});

        listener.reset();
    }

    private void runAssertionWindow(EPServiceProvider epService) {
        String text = "select irstream concatstring(theString) as val from " + SupportBean.class.getName() + "#length(2)";
        EPStatement statement = epService.getEPAdministrator().createEPL(text);
        SupportUpdateListener listener = new SupportUpdateListener();
        statement.addListener(listener);

        epService.getEPRuntime().sendEvent(new SupportBean("a", -1));
        EPAssertionUtil.assertPropsPerRow(listener.assertInvokedAndReset(), "val", new Object[]{"a"}, new Object[]{""});

        epService.getEPRuntime().sendEvent(new SupportBean("b", -1));
        EPAssertionUtil.assertPropsPerRow(listener.assertInvokedAndReset(), "val", new Object[]{"a b"}, new Object[]{"a"});

        epService.getEPRuntime().sendEvent(new SupportBean("c", -1));
        EPAssertionUtil.assertPropsPerRow(listener.assertInvokedAndReset(), "val", new Object[]{"b c"}, new Object[]{"a b"});

        epService.getEPRuntime().sendEvent(new SupportBean("d", -1));
        EPAssertionUtil.assertPropsPerRow(listener.assertInvokedAndReset(), "val", new Object[]{"c d"}, new Object[]{"b c"});

        epService.getEPAdministrator().destroyAllStatements();
    }

    private void runAssertionDistinctAndStarParam(EPServiceProvider epService) {
        epService.getEPAdministrator().getConfiguration().addEventType(SupportBean.class);

        // test *-parameter
        String textTwo = "select concatstring(*) as val from SupportBean";
        EPStatement statementTwo = epService.getEPAdministrator().createEPL(textTwo);
        SupportUpdateListener listenerTwo = new SupportUpdateListener();
        statementTwo.addListener(listenerTwo);

        epService.getEPRuntime().sendEvent(new SupportBean("d", -1));
        EPAssertionUtil.assertProps(listenerTwo.assertOneGetNewAndReset(), "val".split(","), new Object[]{"SupportBean(d, -1)"});

        epService.getEPRuntime().sendEvent(new SupportBean("e", 2));
        EPAssertionUtil.assertProps(listenerTwo.assertOneGetNewAndReset(), "val".split(","), new Object[]{"SupportBean(d, -1) SupportBean(e, 2)"});

        try {
            epService.getEPAdministrator().createEPL("select concatstring(*) as val from SupportBean#lastevent, SupportBean unidirectional");
        } catch (EPStatementException ex) {
            SupportMessageAssertUtil.assertMessage(ex, "Error starting statement: Failed to validate select-clause expression 'concatstring(*)': The 'concatstring' aggregation function requires that in joins or subqueries the stream-wildcard (stream-alias.*) syntax is used instead");
        }

        // test distinct
        String text = "select irstream concatstring(distinct theString) as val from SupportBean";
        EPStatement statement = epService.getEPAdministrator().createEPL(text);
        SupportUpdateListener listener = new SupportUpdateListener();
        statement.addListener(listener);

        epService.getEPRuntime().sendEvent(new SupportBean("a", -1));
        EPAssertionUtil.assertPropsPerRow(listener.assertInvokedAndReset(), "val", new Object[]{"a"}, new Object[]{""});

        epService.getEPRuntime().sendEvent(new SupportBean("b", -1));
        EPAssertionUtil.assertPropsPerRow(listener.assertInvokedAndReset(), "val", new Object[]{"a b"}, new Object[]{"a"});

        epService.getEPRuntime().sendEvent(new SupportBean("b", -1));
        EPAssertionUtil.assertPropsPerRow(listener.assertInvokedAndReset(), "val", new Object[]{"a b"}, new Object[]{"a b"});

        epService.getEPRuntime().sendEvent(new SupportBean("c", -1));
        EPAssertionUtil.assertPropsPerRow(listener.assertInvokedAndReset(), "val", new Object[]{"a b c"}, new Object[]{"a b"});

        epService.getEPRuntime().sendEvent(new SupportBean("a", -1));
        EPAssertionUtil.assertPropsPerRow(listener.assertInvokedAndReset(), "val", new Object[]{"a b c"}, new Object[]{"a b c"});

        epService.getEPAdministrator().destroyAllStatements();
    }

    private void runAssertionArrayParamsAndDotMethod(EPServiceProvider epService) {
        epService.getEPAdministrator().getConfiguration().addPlugInAggregationFunctionFactory("countback", SupportPluginAggregationMethodOneFactory.class.getName());

        String text = "select irstream countback({1,2,intPrimitive}) as val from " + SupportBean.class.getName();
        EPStatement statement = epService.getEPAdministrator().createEPL(text);
        SupportUpdateListener listener = new SupportUpdateListener();
        statement.addListener(listener);

        epService.getEPRuntime().sendEvent(new SupportBean());
        EPAssertionUtil.assertPropsPerRow(listener.assertInvokedAndReset(), "val", new Object[]{-1}, new Object[]{0});

        // test dot-method
        MyAggFuncFactory.setInstanceCount(0);
        epService.getEPAdministrator().getConfiguration().addEventType(SupportBean_A.class);
        epService.getEPAdministrator().getConfiguration().addPlugInAggregationFunctionFactory("myagg", MyAggFuncFactory.class.getName());
        String[] fields = "val0,val1".split(",");
        epService.getEPAdministrator().createEPL("select (myagg(id)).getTheString() as val0, (myagg(id)).getIntPrimitive() as val1 from SupportBean_A").addListener(listener);

        epService.getEPRuntime().sendEvent(new SupportBean_A("A1"));
        EPAssertionUtil.assertProps(listener.assertOneGetNewAndReset(), fields, new Object[]{"XX", 1});
        assertEquals(1, MyAggFuncFactory.getInstanceCount());

        epService.getEPRuntime().sendEvent(new SupportBean_A("A2"));
        EPAssertionUtil.assertProps(listener.assertOneGetNewAndReset(), fields, new Object[]{"XX", 2});

        epService.getEPAdministrator().destroyAllStatements();
    }

    private void runAssertionMultipleParams(EPServiceProvider epService) {
        epService.getEPAdministrator().getConfiguration().addPlugInAggregationFunctionFactory("countboundary", SupportPluginAggregationMethodThreeFactory.class.getName());

        tryAssertionMultipleParams(epService, false);
        tryAssertionMultipleParams(epService, true);
    }

    private void tryAssertionMultipleParams(EPServiceProvider epService, boolean soda) {

        String text = "select irstream countboundary(1,10,intPrimitive,*) as val from " + SupportBean.class.getName();
        EPStatement statement = SupportModelHelper.createByCompileOrParse(epService, soda, text);
        SupportUpdateListener listener = new SupportUpdateListener();
        statement.addListener(listener);

        AggregationValidationContext validContext = SupportPluginAggregationMethodThreeFactory.getContexts().get(0);
        EPAssertionUtil.assertEqualsExactOrder(new Class[]{int.class, int.class, Integer.class, SupportBean.class}, validContext.getParameterTypes());
        EPAssertionUtil.assertEqualsExactOrder(new Object[]{1, 10, null, null}, validContext.getConstantValues());
        EPAssertionUtil.assertEqualsExactOrder(new boolean[]{true, true, false, false}, validContext.getIsConstantValue());

        SupportBean e1 = new SupportBean("E1", 5);
        epService.getEPRuntime().sendEvent(e1);
        EPAssertionUtil.assertPropsPerRow(listener.assertInvokedAndReset(), "val", new Object[]{1}, new Object[]{0});
        EPAssertionUtil.assertEqualsExactOrder(new Object[]{1, 10, 5, e1}, SupportPluginAggregationMethodThree.getLastEnterParameters());

        epService.getEPRuntime().sendEvent(new SupportBean("E1", 0));
        EPAssertionUtil.assertPropsPerRow(listener.assertInvokedAndReset(), "val", new Object[]{1}, new Object[]{1});

        epService.getEPRuntime().sendEvent(new SupportBean("E1", 11));
        EPAssertionUtil.assertPropsPerRow(listener.assertInvokedAndReset(), "val", new Object[]{1}, new Object[]{1});

        epService.getEPRuntime().sendEvent(new SupportBean("E1", 1));
        EPAssertionUtil.assertPropsPerRow(listener.assertInvokedAndReset(), "val", new Object[]{2}, new Object[]{1});

        epService.getEPAdministrator().destroyAllStatements();
    }

    private void runAssertionNoSubnodesRuntimeAdd(EPServiceProvider epService) {
        String text = "select irstream countback() as val from " + SupportBean.class.getName();
        EPStatement statement = epService.getEPAdministrator().createEPL(text);
        SupportUpdateListener listener = new SupportUpdateListener();
        statement.addListener(listener);

        epService.getEPRuntime().sendEvent(new SupportBean());
        EPAssertionUtil.assertPropsPerRow(listener.assertInvokedAndReset(), "val", new Object[]{-1}, new Object[]{0});

        epService.getEPRuntime().sendEvent(new SupportBean());
        EPAssertionUtil.assertPropsPerRow(listener.assertInvokedAndReset(), "val", new Object[]{-2}, new Object[]{-1});

        statement.destroy();
    }

    private void runAssertionMappedPropertyLookAlike(EPServiceProvider epService) {
        String text = "select irstream concatstring('a') as val from " + SupportBean.class.getName();
        EPStatement statement = epService.getEPAdministrator().createEPL(text);
        SupportUpdateListener listener = new SupportUpdateListener();
        statement.addListener(listener);
        assertEquals(String.class, statement.getEventType().getPropertyType("val"));

        epService.getEPRuntime().sendEvent(new SupportBean());
        EPAssertionUtil.assertPropsPerRow(listener.assertInvokedAndReset(), "val", new Object[]{"a"}, new Object[]{""});

        epService.getEPRuntime().sendEvent(new SupportBean());
        EPAssertionUtil.assertPropsPerRow(listener.assertInvokedAndReset(), "val", new Object[]{"a a"}, new Object[]{"a"});

        epService.getEPRuntime().sendEvent(new SupportBean());
        EPAssertionUtil.assertPropsPerRow(listener.assertInvokedAndReset(), "val", new Object[]{"a a a"}, new Object[]{"a a"});

        statement.destroy();
    }

    private void runAssertionFailedValidation(EPServiceProvider epService) {
        try {
            String text = "select concat(1) from " + SupportBean.class.getName();
            epService.getEPAdministrator().createEPL(text);
        } catch (EPStatementException ex) {
            SupportMessageAssertUtil.assertMessage(ex, "Error starting statement: Failed to validate select-clause expression 'concat(1)': Plug-in aggregation function 'concat' failed validation: Invalid parameter type 'int', expecting string [");
        }
    }

    private void runAssertionInvalidUse(EPServiceProvider epService) {
        SupportMessageAssertUtil.tryInvalid(epService, "select * from " + SupportBean.class.getName() + " group by xxx(1)",
                "Error in expression: Error resolving aggregation: Aggregation class by name 'java.lang.String' does not implement AggregationFunctionFactory");

        SupportMessageAssertUtil.tryInvalid(epService, "select * from " + SupportBean.class.getName() + " group by yyy(1)",
                "Error in expression: Error resolving aggregation: Could not load aggregation factory class by name 'com.NoSuchClass'");
    }

    private void runAssertionInvalidConfigure(EPServiceProvider epService) {
        tryInvalidConfigure(epService, "a b", "MyClass");
        tryInvalidConfigure(epService, "abc", "My Class");

        // configure twice
        try {
            epService.getEPAdministrator().getConfiguration().addPlugInAggregationFunctionFactory("concatstring", MyConcatAggregationFunction.class.getName());
            fail();
        } catch (ConfigurationException ex) {
            // expected
        }
    }

    private void tryInvalidConfigure(EPServiceProvider epService, String funcName, String className) {
        try {
            epService.getEPAdministrator().getConfiguration().addPlugInAggregationFunctionFactory(funcName, className);
            fail();
        } catch (ConfigurationException ex) {
            // expected
        }
    }

    private void runAssertionInvalid(EPServiceProvider epService) {
        SupportMessageAssertUtil.tryInvalid(epService, "select zzz(theString) from " + SupportBean.class.getName(),
                "Error starting statement: Failed to validate select-clause expression 'zzz(theString)': Unknown single-row function, aggregation function or mapped or indexed property named 'zzz' could not be resolved");
    }

    public static class MyAggFuncFactory implements AggregationFunctionFactory {
        private static int instanceCount;

        public static void setInstanceCount(int instanceCount) {
            MyAggFuncFactory.instanceCount = instanceCount;
        }

        static int getInstanceCount() {
            return instanceCount;
        }

        public static void incInstanceCount() {
            instanceCount++;
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

        public AggregationFunctionFactoryCodegenType getCodegenType() {
            return AggregationFunctionFactoryCodegenType.CODEGEN_UNMANAGED;
        }

        public void rowMemberCodegen(AggregationFunctionFactoryCodegenRowMemberContext context) {
            context.getCtor().getBlock().staticMethod(MyAggFuncFactory.class, "incInstanceCount");
            MyAggFuncMethod.rowMemberCodegen(context);
        }

        public void applyEnterCodegenManaged(AggregationFunctionFactoryCodegenRowApplyContextManaged context) {
        }

        public void applyLeaveCodegenManaged(AggregationFunctionFactoryCodegenRowApplyContextManaged context) {
        }

        public void applyEnterCodegenUnmanaged(AggregationFunctionFactoryCodegenRowApplyContextUnmanaged context) {
            MyAggFuncMethod.applyEnterCodegenUnmanaged(context);
        }

        public void applyLeaveCodegenUnmanaged(AggregationFunctionFactoryCodegenRowApplyContextUnmanaged context) {
            MyAggFuncMethod.applyLeaveCodegenUnmanaged(context);
        }

        public void clearCodegen(AggregationFunctionFactoryCodegenRowClearContext context) {
            MyAggFuncMethod.clearCodegen(context);
        }

        public void getValueCodegen(AggregationFunctionFactoryCodegenRowGetValueContext context) {
            MyAggFuncMethod.getValueCodegen(context);
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

        public void clear() {
            count = 0;
        }

        public Object getValue() {
            return new SupportBean("XX", count);
        }

        public static void rowMemberCodegen(AggregationFunctionFactoryCodegenRowMemberContext context) {
            context.getMembersColumnized().addMember(context.getColumn(), int.class, "count");
        }

        public static void applyEnterCodegenUnmanaged(AggregationFunctionFactoryCodegenRowApplyContextUnmanaged context) {
            context.getMethod().getBlock().increment(refCol("count", context.getColumn()));
        }

        public static void applyLeaveCodegenUnmanaged(AggregationFunctionFactoryCodegenRowApplyContextUnmanaged context) {
            context.getMethod().getBlock().decrement(refCol("count", context.getColumn()));
        }

        public static void clearCodegen(AggregationFunctionFactoryCodegenRowClearContext context) {
            context.getMethod().getBlock().assignRef(refCol("count", context.getColumn()), constant(0));
        }

        public static void getValueCodegen(AggregationFunctionFactoryCodegenRowGetValueContext context) {
            context.getMethod().getBlock().methodReturn(newInstance(SupportBean.class, constant("XX"), refCol("count", context.getColumn())));
        }
    }
}
