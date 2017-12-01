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
import com.espertech.esper.client.scopetest.EPAssertionUtil;
import com.espertech.esper.client.scopetest.SupportUpdateListener;
import com.espertech.esper.client.soda.EPStatementObjectModel;
import com.espertech.esper.plugin.PlugInAggregationMultiFunctionDeclarationContext;
import com.espertech.esper.plugin.PlugInAggregationMultiFunctionStateContext;
import com.espertech.esper.plugin.PlugInAggregationMultiFunctionValidationContext;
import com.espertech.esper.support.SupportEventTypeAssertionEnum;
import com.espertech.esper.support.SupportEventTypeAssertionUtil;
import com.espertech.esper.supportregression.bean.SupportBean;
import com.espertech.esper.supportregression.bean.SupportBean_S0;
import com.espertech.esper.supportregression.client.*;
import com.espertech.esper.supportregression.execution.RegressionExecution;
import com.espertech.esper.util.EPServiceProviderName;

import java.util.Collection;

import static org.junit.Assert.*;

public class ExecClientAggregationMultiFunctionPlugIn implements RegressionExecution {
    public void configure(Configuration configuration) throws Exception {
        ConfigurationPlugInAggregationMultiFunction configGeneral = new ConfigurationPlugInAggregationMultiFunction(SupportAggMFFunc.getFunctionNames(), SupportAggMFFactory.class.getName());
        configuration.addPlugInAggregationMultiFunction(configGeneral);

        ConfigurationPlugInAggregationMultiFunction codegenTestAccum = new ConfigurationPlugInAggregationMultiFunction("codegenTestAccum".split(","), SupportAggMFEventsAsListFactory.class.getName());
        configuration.addPlugInAggregationMultiFunction(codegenTestAccum);

        configuration.addEventType(SupportBean.class);
    }

    public void run(EPServiceProvider epService) throws Exception {
        runAssertionDifferentReturnTypes(epService);
        runAssertionSameProviderGroupedReturnSingleEvent(epService);
        runAssertionNoCodegen(epService);
        runAssertionNoCodegenWithTable(epService);
        runAssertionInvalid(epService);
    }

    private void runAssertionNoCodegenWithTable(EPServiceProvider epService) {
        epService.getEPAdministrator().getConfiguration().addEventType(SupportBean_S0.class);
        epService.getEPAdministrator().createEPL("create table MyTable(col codegenTestAccum())");
        epService.getEPAdministrator().createEPL("into table MyTable select codegenTestAccum(*) as col from SupportBean#length(2)");

        EPStatement statement = epService.getEPAdministrator().createEPL("on SupportBean_S0 select col as c0 from MyTable");
        SupportUpdateListener listener = new SupportUpdateListener();
        statement.addListener(listener);

        SupportBean e1 = new SupportBean("E1", 1);
        epService.getEPRuntime().sendEvent(e1);
        sendAssertList(epService, listener, e1);

        SupportBean e2 = new SupportBean("E2", 2);
        epService.getEPRuntime().sendEvent(e2);
        sendAssertList(epService, listener, e1, e2);

        SupportBean e3 = new SupportBean("E3", 3);
        epService.getEPRuntime().sendEvent(e3);
        sendAssertList(epService, listener, e2, e3);

        statement.destroy();
    }

    private void runAssertionNoCodegen(EPServiceProvider epService) {
        EPStatement stmt = epService.getEPAdministrator().createEPL("select codegenTestAccum(*) as c0 from SupportBean#length(2)");
        SupportUpdateListener listener = new SupportUpdateListener();
        stmt.addListener(listener);

        SupportBean e1 = new SupportBean("E1", 1);
        epService.getEPRuntime().sendEvent(e1);
        assertList(listener, e1);

        SupportBean e2 = new SupportBean("E2", 2);
        epService.getEPRuntime().sendEvent(e2);
        assertList(listener, e1, e2);

        SupportBean e3 = new SupportBean("E3", 3);
        epService.getEPRuntime().sendEvent(e3);
        assertList(listener, e2, e3);

        stmt.destroy();
    }

    private void runAssertionDifferentReturnTypes(EPServiceProvider epService) {

        // test scalar only
        String[] fieldsScalar = "c0,c1".split(",");
        String eplScalar = "select ss(theString) as c0, ss(intPrimitive) as c1 from SupportBean";
        EPStatement stmtScalar = epService.getEPAdministrator().createEPL(eplScalar);
        SupportUpdateListener listener = new SupportUpdateListener();
        stmtScalar.addListener(listener);

        Object[][] expectedScalar = new Object[][]{{"c0", String.class, null, null}, {"c1", Integer.class, null, null}};
        SupportEventTypeAssertionUtil.assertEventTypeProperties(expectedScalar, stmtScalar.getEventType(), SupportEventTypeAssertionEnum.getSetWithFragment());

        epService.getEPRuntime().sendEvent(new SupportBean("E1", 1));
        EPAssertionUtil.assertProps(listener.assertOneGetNewAndReset(), fieldsScalar, new Object[]{"E1", 1});

        epService.getEPRuntime().sendEvent(new SupportBean("E2", 2));
        EPAssertionUtil.assertProps(listener.assertOneGetNewAndReset(), fieldsScalar, new Object[]{"E2", 2});
        stmtScalar.destroy();

        // test scalar-array only
        String[] fieldsScalarArray = "c0,c1,c2,c3".split(",");
        String eplScalarArray = "select " +
                "sa(theString) as c0, " +
                "sa(intPrimitive) as c1, " +
                "sa(theString).allOf(v => v = 'E1') as c2, " +
                "sa(intPrimitive).allOf(v => v = 1) as c3 " +
                "from SupportBean";
        EPStatement stmtScalarArray = epService.getEPAdministrator().createEPL(eplScalarArray);
        stmtScalarArray.addListener(listener);

        Object[][] expectedScalarArray = new Object[][]{
                {"c0", String[].class, null, null}, {"c1", Integer[].class, null, null},
                {"c2", Boolean.class, null, null}, {"c3", Boolean.class, null, null},
        };
        SupportEventTypeAssertionUtil.assertEventTypeProperties(expectedScalarArray, stmtScalarArray.getEventType(), SupportEventTypeAssertionEnum.getSetWithFragment());

        epService.getEPRuntime().sendEvent(new SupportBean("E1", 1));
        EPAssertionUtil.assertProps(listener.assertOneGetNewAndReset(), fieldsScalarArray, new Object[]{
            new String[]{"E1"}, new int[]{1}, true, true});

        epService.getEPRuntime().sendEvent(new SupportBean("E2", 2));
        EPAssertionUtil.assertProps(listener.assertOneGetNewAndReset(), fieldsScalarArray, new Object[]{
            new String[]{"E1", "E2"}, new int[]{1, 2}, false, false});
        stmtScalarArray.destroy();

        // test scalar-collection only
        String[] fieldsScalarColl = "c2,c3".split(",");
        String eplScalarColl = "select " +
                "sc(theString) as c0, " +
                "sc(intPrimitive) as c1, " +
                "sc(theString).allOf(v => v = 'E1') as c2, " +
                "sc(intPrimitive).allOf(v => v = 1) as c3 " +
                "from SupportBean";
        EPStatement stmtScalarColl = epService.getEPAdministrator().createEPL(eplScalarColl);
        stmtScalarColl.addListener(listener);

        Object[][] expectedScalarColl = new Object[][]{
                {"c0", Collection.class, null, null}, {"c1", Collection.class, null, null},
                {"c2", Boolean.class, null, null}, {"c3", Boolean.class, null, null},
        };
        SupportEventTypeAssertionUtil.assertEventTypeProperties(expectedScalarColl, stmtScalarColl.getEventType(), SupportEventTypeAssertionEnum.getSetWithFragment());

        epService.getEPRuntime().sendEvent(new SupportBean("E1", 1));
        EPAssertionUtil.assertEqualsExactOrder(new Object[]{"E1"}, (Collection) listener.assertOneGetNew().get("c0"));
        EPAssertionUtil.assertEqualsExactOrder(new Object[]{1}, (Collection) listener.assertOneGetNew().get("c1"));
        EPAssertionUtil.assertProps(listener.assertOneGetNewAndReset(), fieldsScalarColl, new Object[]{true, true});

        epService.getEPRuntime().sendEvent(new SupportBean("E2", 2));
        EPAssertionUtil.assertEqualsExactOrder(new Object[]{"E1", "E2"}, (Collection) listener.assertOneGetNew().get("c0"));
        EPAssertionUtil.assertEqualsExactOrder(new Object[]{1, 2}, (Collection) listener.assertOneGetNew().get("c1"));
        EPAssertionUtil.assertProps(listener.assertOneGetNewAndReset(), fieldsScalarColl, new Object[]{false, false});
        stmtScalarColl.destroy();

        // test single-event return
        String[] fieldsSingleEvent = "c0,c1,c2,c3,c4".split(",");
        String eplSingleEvent = "select " +
                "se1() as c0, " +
                "se1().allOf(v => v.theString = 'E1') as c1, " +
                "se1().allOf(v => v.intPrimitive = 1) as c2, " +
                "se1().theString as c3, " +
                "se1().intPrimitive as c4 " +
                "from SupportBean";
        EPStatement stmtSingleEvent = epService.getEPAdministrator().createEPL(eplSingleEvent);
        stmtSingleEvent.addListener(listener);

        Object[][] expectedSingleEvent = new Object[][]{
                {"c0", SupportBean.class, "SupportBean", false},
                {"c1", Boolean.class, null, null}, {"c2", Boolean.class, null, null},
                {"c3", String.class, null, null}, {"c4", Integer.class, null, null},
        };
        SupportEventTypeAssertionUtil.assertEventTypeProperties(expectedSingleEvent, stmtSingleEvent.getEventType(), SupportEventTypeAssertionEnum.getSetWithFragment());

        SupportBean eventOne = new SupportBean("E1", 1);
        epService.getEPRuntime().sendEvent(eventOne);
        EPAssertionUtil.assertProps(listener.assertOneGetNewAndReset(), fieldsSingleEvent, new Object[]{eventOne, true, true, "E1", 1});

        SupportBean eventTwo = new SupportBean("E2", 2);
        epService.getEPRuntime().sendEvent(eventTwo);
        EPAssertionUtil.assertProps(listener.assertOneGetNewAndReset(), fieldsSingleEvent, new Object[]{eventTwo, false, false, "E2", 2});
        stmtSingleEvent.destroy();

        // test single-event return
        String[] fieldsEnumEvent = "c0,c1,c2".split(",");
        String eplEnumEvent = "select " +
                "ee() as c0, " +
                "ee().allOf(v => v.theString = 'E1') as c1, " +
                "ee().allOf(v => v.intPrimitive = 1) as c2 " +
                "from SupportBean";
        EPStatement stmtEnumEvent = epService.getEPAdministrator().createEPL(eplEnumEvent);
        stmtEnumEvent.addListener(listener);

        Object[][] expectedEnumEvent = new Object[][]{
                {"c0", SupportBean[].class, "SupportBean", true},
                {"c1", Boolean.class, null, null}, {"c2", Boolean.class, null, null}
        };
        SupportEventTypeAssertionUtil.assertEventTypeProperties(expectedEnumEvent, stmtEnumEvent.getEventType(), SupportEventTypeAssertionEnum.getSetWithFragment());

        SupportBean eventEnumOne = new SupportBean("E1", 1);
        epService.getEPRuntime().sendEvent(eventEnumOne);
        EPAssertionUtil.assertProps(listener.assertOneGetNewAndReset(), fieldsEnumEvent, new Object[]{new SupportBean[]{eventEnumOne}, true, true});

        SupportBean eventEnumTwo = new SupportBean("E2", 2);
        epService.getEPRuntime().sendEvent(eventEnumTwo);
        EPAssertionUtil.assertProps(listener.assertOneGetNewAndReset(), fieldsEnumEvent, new Object[]{new SupportBean[]{eventEnumOne, eventEnumTwo}, false, false});

        stmtEnumEvent.destroy();
    }

    private void runAssertionSameProviderGroupedReturnSingleEvent(EPServiceProvider epService) throws Exception {
        String epl = "select se1() as c0, se2() as c1 from SupportBean#keepall group by theString";

        // test regular
        SupportAggMFFactory.reset();
        SupportAggMFHandler.reset();
        SupportAggMFFactorySingleEvent.reset();

        EPStatement stmt = epService.getEPAdministrator().createEPL(epl);
        tryAssertion(epService, stmt);

        // test SODA
        EPStatementObjectModel model = epService.getEPAdministrator().compileEPL(epl);
        SupportAggMFFactory.reset();
        SupportAggMFHandler.reset();
        SupportAggMFFactorySingleEvent.reset();
        assertEquals(epl, model.toEPL());
        EPStatement stmtModel = epService.getEPAdministrator().create(model);
        assertEquals(epl, stmtModel.getText());
        tryAssertion(epService, stmtModel);
    }

    private void tryAssertion(EPServiceProvider epService, EPStatement stmt) {

        SupportUpdateListener listener = new SupportUpdateListener();
        stmt.addListener(listener);
        String[] fields = "c0,c1".split(",");
        for (String prop : fields) {
            assertEquals(SupportBean.class, stmt.getEventType().getPropertyDescriptor(prop).getPropertyType());
            assertEquals(true, stmt.getEventType().getPropertyDescriptor(prop).isFragment());
            assertEquals("SupportBean", stmt.getEventType().getFragmentType(prop).getFragmentType().getName());
        }

        // there should be just 1 factory instance for all of the registered functions for this statement
        assertEquals(1, SupportAggMFFactory.getFactories().size());
        assertEquals(2, SupportAggMFFactory.getFunctionDeclContexts().size());
        for (int i = 0; i < 2; i++) {
            PlugInAggregationMultiFunctionDeclarationContext contextDecl = SupportAggMFFactory.getFunctionDeclContexts().get(i);
            assertEquals(i == 0 ? "se1" : "se2", contextDecl.getFunctionName());
            assertEquals(EPServiceProviderName.DEFAULT_ENGINE_URI, contextDecl.getEngineURI());
            assertFalse(contextDecl.isDistinct());
            assertNotNull(contextDecl.getConfiguration());

            PlugInAggregationMultiFunctionValidationContext contextValid = SupportAggMFFactory.getFunctionHandlerValidationContexts().get(i);
            assertEquals(i == 0 ? "se1" : "se2", contextValid.getFunctionName());
            assertEquals(EPServiceProviderName.DEFAULT_ENGINE_URI, contextValid.getEngineURI());
            assertNotNull(contextValid.getParameterExpressions());
            assertNotNull(contextValid.getAllParameterExpressions());
            assertNotNull(contextValid.getConfig());
            assertNotNull(contextValid.getEventTypes());
            assertNotNull(contextValid.getValidationContext());
            assertNotNull(contextValid.getStatementName());
        }
        assertEquals(2, SupportAggMFHandler.getProviderKeys().size());
        if (!SupportAggMFHandler.getAccessors().isEmpty()) {
            assertEquals(2, SupportAggMFHandler.getAccessors().size());
            assertEquals(1, SupportAggMFHandler.getProviderFactories().size());
        }
        assertEquals(0, SupportAggMFFactorySingleEvent.getStateContexts().size());

        // group 1
        SupportBean eventOne = new SupportBean("E1", 1);
        epService.getEPRuntime().sendEvent(eventOne);
        EPAssertionUtil.assertProps(listener.assertOneGetNewAndReset(), fields, new Object[]{eventOne, eventOne});
        if (!SupportAggMFFactorySingleEvent.getStateContexts().isEmpty()) {
            assertEquals(1, SupportAggMFFactorySingleEvent.getStateContexts().size());
            PlugInAggregationMultiFunctionStateContext context = SupportAggMFFactorySingleEvent.getStateContexts().get(0);
            assertEquals("E1", context.getGroupKey());
        }

        // group 2
        SupportBean eventTwo = new SupportBean("E2", 2);
        epService.getEPRuntime().sendEvent(eventTwo);
        EPAssertionUtil.assertProps(listener.assertOneGetNewAndReset(), fields, new Object[]{eventTwo, eventTwo});
        if (!SupportAggMFFactorySingleEvent.getStateContexts().isEmpty()) {
            assertEquals(2, SupportAggMFFactorySingleEvent.getStateContexts().size());
        }

        stmt.destroy();
    }

    private void runAssertionInvalid(EPServiceProvider epService) {
        // add overlapping config with regular agg function
        try {
            epService.getEPAdministrator().getConfiguration().addPlugInAggregationFunctionFactory(SupportAggMFFunc.SCALAR.getName(), "somefactory");
            fail();
        } catch (ConfigurationException ex) {
            assertEquals("Aggregation multi-function by name 'ss' is already defined", ex.getMessage());
        }

        // add overlapping config with regular agg function
        try {
            epService.getEPAdministrator().getConfiguration().addPlugInSingleRowFunction(SupportAggMFFunc.SCALAR.getName(), "somefactory", "somename");
            fail();
        } catch (ConfigurationException ex) {
            assertEquals("Aggregation multi-function by name 'ss' is already defined", ex.getMessage());
        }

        // test over lapping with another multi-function
        ConfigurationPlugInAggregationMultiFunction config = new ConfigurationPlugInAggregationMultiFunction("thefunction".split(","), SupportAggMFFactory.class.getName());
        epService.getEPAdministrator().getConfiguration().addPlugInAggregationMultiFunction(config);
        try {
            ConfigurationPlugInAggregationMultiFunction configTwo = new ConfigurationPlugInAggregationMultiFunction("xyz,gmbh,thefunction".split(","), ExecClientAggregationFunctionPlugIn.class.getName());
            epService.getEPAdministrator().getConfiguration().addPlugInAggregationMultiFunction(configTwo);
            fail();
        } catch (ConfigurationException ex) {
            assertEquals("Aggregation multi-function by name 'thefunction' is already defined", ex.getMessage());
        }

        // test invalid class name
        try {
            ConfigurationPlugInAggregationMultiFunction configTwo = new ConfigurationPlugInAggregationMultiFunction("thefunction2".split(","), "x y z");
            epService.getEPAdministrator().getConfiguration().addPlugInAggregationMultiFunction(configTwo);
            fail();
        } catch (ConfigurationException ex) {
            assertEquals("Invalid class name for aggregation multi-function factory 'x y z'", ex.getMessage());
        }
    }

    private void assertList(SupportUpdateListener listener, SupportBean ... events) {
        Object[] out = ((Collection) listener.assertOneGetNewAndReset().get("c0")).toArray();
        EPAssertionUtil.assertEqualsExactOrder(out, events);
    }

    private void sendAssertList(EPServiceProvider epService, SupportUpdateListener listener, SupportBean... events) {
        epService.getEPRuntime().sendEvent(new SupportBean_S0(1));
        Object[] out = ((Collection) listener.assertOneGetNewAndReset().get("c0")).toArray();
        EPAssertionUtil.assertEqualsExactOrder(out, events);
    }
}
