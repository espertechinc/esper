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
package com.espertech.esper.regressionlib.suite.client.extension;

import com.espertech.esper.common.client.EventType;
import com.espertech.esper.common.client.hook.aggmultifunc.AggregationMultiFunctionDeclarationContext;
import com.espertech.esper.common.client.hook.aggmultifunc.AggregationMultiFunctionValidationContext;
import com.espertech.esper.common.client.scopetest.EPAssertionUtil;
import com.espertech.esper.common.client.soda.EPStatementObjectModel;
import com.espertech.esper.common.internal.support.SupportBean;
import com.espertech.esper.common.internal.support.SupportBean_S0;
import com.espertech.esper.common.internal.support.SupportEventTypeAssertionEnum;
import com.espertech.esper.common.internal.support.SupportEventTypeAssertionUtil;
import com.espertech.esper.regressionlib.framework.RegressionEnvironment;
import com.espertech.esper.regressionlib.framework.RegressionExecution;
import com.espertech.esper.regressionlib.framework.RegressionPath;
import com.espertech.esper.regressionlib.support.extend.aggmultifunc.SupportAggMFMultiRTForge;
import com.espertech.esper.regressionlib.support.extend.aggmultifunc.SupportAggMFMultiRTHandler;
import com.espertech.esper.regressionlib.support.extend.aggmultifunc.SupportAggMFMultiRTSingleEventState;
import com.espertech.esper.regressionlib.support.extend.aggmultifunc.SupportAggMFMultiRTSingleEventStateFactory;
import com.espertech.esper.runtime.client.scopetest.SupportListener;

import java.util.ArrayList;
import java.util.Collection;

import static org.junit.Assert.*;

public class ClientExtendAggregationMultiFunction {

    public static Collection<RegressionExecution> executions() {
        ArrayList<RegressionExecution> execs = new ArrayList<>();
        execs.add(new ClientExtendAggregationMFManagedSimpleState());
        execs.add(new ClientExtendAggregationMFManagedScalarOnly());
        execs.add(new ClientExtendAggregationMFManagedScalarArray());
        execs.add(new ClientExtendAggregationMFManagedScalarColl());
        execs.add(new ClientExtendAggregationMFManagedSingleEvent());
        execs.add(new ClientExtendAggregationMFManagedCollEvent());
        execs.add(new ClientExtendAggregationMFManagedSameProviderGroupedReturnSingleEvent());
        execs.add(new ClientExtendAggregationMFManagedWithTable());
        return execs;
    }

    public void run(RegressionEnvironment env) {
    }

    private static class ClientExtendAggregationMFManagedWithTable implements RegressionExecution {
        public boolean excludeWhenInstrumented() {
            return true;
        }

        public void run(RegressionEnvironment env) {
            RegressionPath path = new RegressionPath();
            env.compileDeploy("create table MyTable(col collectEvents())", path);
            env.compileDeploy("into table MyTable select collectEvents(*) as col from SupportBean#length(2)", path);
            env.compileDeploy("@name('s0') on SupportBean_S0 select col as c0 from MyTable", path).addListener("s0");

            SupportBean e1 = new SupportBean("E1", 1);
            env.sendEventBean(e1);
            sendAssertList(env, e1);

            SupportBean e2 = new SupportBean("E2", 2);
            env.sendEventBean(e2);
            sendAssertList(env, e1, e2);

            SupportBean e3 = new SupportBean("E3", 3);
            env.sendEventBean(e3);
            sendAssertList(env, e2, e3);

            env.undeployAll();
        }
    }
    private static class ClientExtendAggregationMFManagedCollEvent implements RegressionExecution {
        public void run(RegressionEnvironment env) {
            String[] fieldsEnumEvent = "c0,c1,c2".split(",");
            String eplEnumEvent = "@name('s0') select " +
                "ee() as c0, " +
                "ee().allOf(v => v.theString = 'E1') as c1, " +
                "ee().allOf(v => v.intPrimitive = 1) as c2 " +
                "from SupportBean";
            env.compileDeploy(eplEnumEvent).addListener("s0");

            Object[][] expectedEnumEvent = new Object[][]{
                {"c0", SupportBean[].class, SupportBean.class.getName(), true},
                {"c1", Boolean.class, null, null}, {"c2", Boolean.class, null, null}
            };
            SupportEventTypeAssertionUtil.assertEventTypeProperties(expectedEnumEvent, env.statement("s0").getEventType(), SupportEventTypeAssertionEnum.getSetWithFragment());

            SupportBean eventEnumOne = new SupportBean("E1", 1);
            env.sendEventBean(eventEnumOne);
            EPAssertionUtil.assertProps(env.listener("s0").assertOneGetNewAndReset(), fieldsEnumEvent, new Object[]{new SupportBean[]{eventEnumOne}, true, true});

            SupportBean eventEnumTwo = new SupportBean("E2", 2);
            env.sendEventBean(eventEnumTwo);
            EPAssertionUtil.assertProps(env.listener("s0").assertOneGetNewAndReset(), fieldsEnumEvent, new Object[]{new SupportBean[]{eventEnumOne, eventEnumTwo}, false, false});

            env.undeployAll();
        }
    }
    private static class ClientExtendAggregationMFManagedSingleEvent implements RegressionExecution {
        public void run(RegressionEnvironment env) {
            // test single-event return
            String[] fieldsSingleEvent = "c0,c1,c2,c3,c4".split(",");
            String eplSingleEvent = "@name('s0') select " +
                "se1() as c0, " +
                "se1().allOf(v => v.theString = 'E1') as c1, " +
                "se1().allOf(v => v.intPrimitive = 1) as c2, " +
                "se1().theString as c3, " +
                "se1().intPrimitive as c4 " +
                "from SupportBean";
            env.compileDeploy(eplSingleEvent).addListener("s0");

            Object[][] expectedSingleEvent = new Object[][]{
                {"c0", SupportBean.class, SupportBean.class.getName(), false},
                {"c1", Boolean.class, null, null}, {"c2", Boolean.class, null, null},
                {"c3", String.class, null, null}, {"c4", Integer.class, null, null},
            };
            SupportEventTypeAssertionUtil.assertEventTypeProperties(expectedSingleEvent, env.statement("s0").getEventType(), SupportEventTypeAssertionEnum.getSetWithFragment());

            SupportBean eventOne = new SupportBean("E1", 1);
            env.sendEventBean(eventOne);
            EPAssertionUtil.assertProps(env.listener("s0").assertOneGetNewAndReset(), fieldsSingleEvent, new Object[]{eventOne, true, true, "E1", 1});

            SupportBean eventTwo = new SupportBean("E2", 2);
            env.sendEventBean(eventTwo);
            EPAssertionUtil.assertProps(env.listener("s0").assertOneGetNewAndReset(), fieldsSingleEvent, new Object[]{eventTwo, false, false, "E2", 2});

            env.undeployAll();
        }
    }
    private static class ClientExtendAggregationMFManagedScalarColl implements RegressionExecution {
        public void run(RegressionEnvironment env) {
            // test scalar-collection only
            String[] fieldsScalarColl = "c2,c3".split(",");
            String eplScalarColl = "@name('s0') select " +
                "sc(theString) as c0, " +
                "sc(intPrimitive) as c1, " +
                "sc(theString).allOf(v => v = 'E1') as c2, " +
                "sc(intPrimitive).allOf(v => v = 1) as c3 " +
                "from SupportBean";
            env.compileDeploy(eplScalarColl).addListener("s0");

            Object[][] expectedScalarColl = new Object[][]{
                {"c0", Collection.class, null, null}, {"c1", Collection.class, null, null},
                {"c2", Boolean.class, null, null}, {"c3", Boolean.class, null, null},
            };
            SupportEventTypeAssertionUtil.assertEventTypeProperties(expectedScalarColl, env.statement("s0").getEventType(), SupportEventTypeAssertionEnum.getSetWithFragment());

            env.sendEventBean(new SupportBean("E1", 1));
            EPAssertionUtil.assertEqualsExactOrder(new Object[]{"E1"}, (Collection) env.listener("s0").assertOneGetNew().get("c0"));
            EPAssertionUtil.assertEqualsExactOrder(new Object[]{1}, (Collection) env.listener("s0").assertOneGetNew().get("c1"));
            EPAssertionUtil.assertProps(env.listener("s0").assertOneGetNewAndReset(), fieldsScalarColl, new Object[]{true, true});

            env.sendEventBean(new SupportBean("E2", 2));
            EPAssertionUtil.assertEqualsExactOrder(new Object[]{"E1", "E2"}, (Collection) env.listener("s0").assertOneGetNew().get("c0"));
            EPAssertionUtil.assertEqualsExactOrder(new Object[]{1, 2}, (Collection) env.listener("s0").assertOneGetNew().get("c1"));
            EPAssertionUtil.assertProps(env.listener("s0").assertOneGetNewAndReset(), fieldsScalarColl, new Object[]{false, false});

            env.undeployAll();
        }
    }
    private static class ClientExtendAggregationMFManagedScalarArray implements RegressionExecution {
        public void run(RegressionEnvironment env) {
            String[] fieldsScalarArray = "c0,c1,c2,c3".split(",");
            String eplScalarArray = "@name('s0') select " +
                "sa(theString) as c0, " +
                "sa(intPrimitive) as c1, " +
                "sa(theString).allOf(v => v = 'E1') as c2, " +
                "sa(intPrimitive).allOf(v => v = 1) as c3 " +
                "from SupportBean";
            env.compileDeploy(eplScalarArray).addListener("s0");

            Object[][] expectedScalarArray = new Object[][]{
                {"c0", String[].class, null, null}, {"c1", Integer[].class, null, null},
                {"c2", Boolean.class, null, null}, {"c3", Boolean.class, null, null},
            };
            SupportEventTypeAssertionUtil.assertEventTypeProperties(expectedScalarArray, env.statement("s0").getEventType(), SupportEventTypeAssertionEnum.getSetWithFragment());

            env.sendEventBean(new SupportBean("E1", 1));
            EPAssertionUtil.assertProps(env.listener("s0").assertOneGetNewAndReset(), fieldsScalarArray, new Object[]{
                new String[]{"E1"}, new int[]{1}, true, true});

            env.sendEventBean(new SupportBean("E2", 2));
            EPAssertionUtil.assertProps(env.listener("s0").assertOneGetNewAndReset(), fieldsScalarArray, new Object[]{
                new String[]{"E1", "E2"}, new int[]{1, 2}, false, false});

            env.undeployAll();
        }
    }
    private static class ClientExtendAggregationMFManagedScalarOnly implements RegressionExecution {
        public void run(RegressionEnvironment env) {
            String[] fieldsScalar = "c0,c1".split(",");
            String eplScalar = "@name('s0') select ss(theString) as c0, ss(intPrimitive) as c1 from SupportBean";
            env.compileDeploy(eplScalar).addListener("s0");

            Object[][] expectedScalar = new Object[][]{{"c0", String.class, null, null}, {"c1", Integer.class, null, null}};
            SupportEventTypeAssertionUtil.assertEventTypeProperties(expectedScalar, env.statement("s0").getEventType(), SupportEventTypeAssertionEnum.getSetWithFragment());

            env.sendEventBean(new SupportBean("E1", 1));
            EPAssertionUtil.assertProps(env.listener("s0").assertOneGetNewAndReset(), fieldsScalar, new Object[]{"E1", 1});

            env.sendEventBean(new SupportBean("E2", 2));
            EPAssertionUtil.assertProps(env.listener("s0").assertOneGetNewAndReset(), fieldsScalar, new Object[]{"E2", 2});

            env.undeployAll();
        }
    }
    private static class ClientExtendAggregationMFManagedSimpleState implements RegressionExecution {
        public boolean excludeWhenInstrumented() {
            return true;
        }

        public void run(RegressionEnvironment env) {
            env.compileDeploy("@name('s0') select collectEvents(*) as c0 from SupportBean#length(2)").addListener("s0");

            SupportBean e1 = new SupportBean("E1", 1);
            env.sendEventBean(e1);
            assertList(env.listener("s0"), e1);

            SupportBean e2 = new SupportBean("E2", 2);
            env.sendEventBean(e2);
            assertList(env.listener("s0"), e1, e2);

            SupportBean e3 = new SupportBean("E3", 3);
            env.sendEventBean(e3);
            assertList(env.listener("s0"), e2, e3);

            env.undeployAll();
        }
    }
    private static class ClientExtendAggregationMFManagedSameProviderGroupedReturnSingleEvent implements RegressionExecution {
        public void run(RegressionEnvironment env) {
            String epl = "@name('s0') select se1() as c0, se2() as c1 from SupportBean#keepall group by theString";

            // test regular
            SupportAggMFMultiRTForge.reset();
            SupportAggMFMultiRTHandler.reset();
            SupportAggMFMultiRTSingleEventStateFactory.reset();

            env.compileDeploy(epl).addListener("s0");
            tryAssertion(env);

            // test SODA
            EPStatementObjectModel model = env.eplToModel(epl);
            SupportAggMFMultiRTForge.reset();
            SupportAggMFMultiRTHandler.reset();
            SupportAggMFMultiRTSingleEventStateFactory.reset();
            assertEquals(epl, model.toEPL());
            env.compileDeploy(model).addListener("s0");
            tryAssertion(env);
        }
    }

    private static void tryAssertion(RegressionEnvironment env) {

        String[] fields = "c0,c1".split(",");
        EventType eventType = env.statement("s0").getEventType();
        for (String prop : fields) {
            assertEquals(SupportBean.class, eventType.getPropertyDescriptor(prop).getPropertyType());
            assertEquals(true, eventType.getPropertyDescriptor(prop).isFragment());
            assertEquals(SupportBean.class.getName(), eventType.getFragmentType(prop).getFragmentType().getName());
        }

        // there should be just 1 forge instance for all of the registered functions for this statement
        assertEquals(1, SupportAggMFMultiRTForge.getForges().size());
        assertEquals(2, SupportAggMFMultiRTForge.getFunctionDeclContexts().size());
        for (int i = 0; i < 2; i++) {
            AggregationMultiFunctionDeclarationContext contextDecl = SupportAggMFMultiRTForge.getFunctionDeclContexts().get(i);
            assertEquals(i == 0 ? "se1" : "se2", contextDecl.getFunctionName());
            assertFalse(contextDecl.isDistinct());
            assertNotNull(contextDecl.getConfiguration());

            AggregationMultiFunctionValidationContext contextValid = SupportAggMFMultiRTForge.getFunctionHandlerValidationContexts().get(i);
            assertEquals(i == 0 ? "se1" : "se2", contextValid.getFunctionName());
            assertNotNull(contextValid.getParameterExpressions());
            assertNotNull(contextValid.getAllParameterExpressions());
            assertNotNull(contextValid.getConfig());
            assertNotNull(contextValid.getEventTypes());
            assertNotNull(contextValid.getValidationContext());
            assertNotNull(contextValid.getStatementName());
        }
        assertEquals(2, SupportAggMFMultiRTHandler.getProviderKeys().size());
        if (!SupportAggMFMultiRTHandler.getAccessorModes().isEmpty()) {
            assertEquals(2, SupportAggMFMultiRTHandler.getAccessorModes().size());
            assertEquals(1, SupportAggMFMultiRTHandler.getStateFactoryModes().size());
        }
        assertEquals(0, SupportAggMFMultiRTSingleEventStateFactory.getStateContexts().size());

        // group 1
        SupportBean eventOne = new SupportBean("E1", 1);
        env.sendEventBean(eventOne);
        EPAssertionUtil.assertProps(env.listener("s0").assertOneGetNewAndReset(), fields, new Object[]{eventOne, eventOne});
        if (!SupportAggMFMultiRTSingleEventStateFactory.getStateContexts().isEmpty()) {
            assertEquals(1, SupportAggMFMultiRTSingleEventStateFactory.getStateContexts().size());
            SupportAggMFMultiRTSingleEventState context = SupportAggMFMultiRTSingleEventStateFactory.getStateContexts().get(0);
            // Not available: assertEquals("E1", context.getGroupKey());
        }

        // group 2
        SupportBean eventTwo = new SupportBean("E2", 2);
        env.sendEventBean(eventTwo);
        EPAssertionUtil.assertProps(env.listener("s0").assertOneGetNewAndReset(), fields, new Object[]{eventTwo, eventTwo});
        if (!SupportAggMFMultiRTSingleEventStateFactory.getStateContexts().isEmpty()) {
            assertEquals(2, SupportAggMFMultiRTSingleEventStateFactory.getStateContexts().size());
        }

        env.undeployAll();
    }

    private static void sendAssertList(RegressionEnvironment env, SupportBean... events) {
        env.sendEventBean(new SupportBean_S0(1));
        Object[] out = ((Collection) env.listener("s0").assertOneGetNewAndReset().get("c0")).toArray();
        EPAssertionUtil.assertEqualsExactOrder(out, events);
    }

    private static void assertList(SupportListener listener, SupportBean... events) {
        Object[] out = ((Collection) listener.assertOneGetNewAndReset().get("c0")).toArray();
        EPAssertionUtil.assertEqualsExactOrder(out, events);
    }
}
