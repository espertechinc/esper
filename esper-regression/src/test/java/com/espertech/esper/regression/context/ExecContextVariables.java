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
package com.espertech.esper.regression.context;

import com.espertech.esper.client.*;
import com.espertech.esper.client.context.ContextPartitionVariableState;
import com.espertech.esper.client.deploy.DeploymentException;
import com.espertech.esper.client.deploy.DeploymentResult;
import com.espertech.esper.client.deploy.ParseException;
import com.espertech.esper.client.scopetest.EPAssertionUtil;
import com.espertech.esper.client.scopetest.SupportUpdateListener;
import com.espertech.esper.supportregression.bean.SupportBean;
import com.espertech.esper.supportregression.bean.SupportBean_S0;
import com.espertech.esper.supportregression.bean.SupportBean_S1;
import com.espertech.esper.supportregression.bean.SupportBean_S2;
import com.espertech.esper.supportregression.client.SupportConfigFactory;
import com.espertech.esper.supportregression.context.SupportSelectorById;
import com.espertech.esper.supportregression.execution.RegressionExecution;

import java.io.IOException;
import java.util.Collections;
import java.util.List;
import java.util.Map;

import static com.espertech.esper.supportregression.util.SupportMessageAssertUtil.tryInvalid;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.fail;

public class ExecContextVariables implements RegressionExecution {

    public void configure(Configuration configuration) throws Exception {
        configuration.addEventType("SupportBean", SupportBean.class);
        configuration.addEventType("SupportBean_S0", SupportBean_S0.class);
        configuration.addEventType("SupportBean_S1", SupportBean_S1.class);
        configuration.addEventType("SupportBean_S2", SupportBean_S2.class);
    }

    public void run(EPServiceProvider epService) throws Exception {
        runAssertionSegmentedByKey(epService);
        runAssertionOverlapping(epService);
        runAssertionIterateAndListen(epService);
        runAssertionGetSetAPI(epService);
        runAssertionInvalid(epService);
    }

    private void runAssertionSegmentedByKey(EPServiceProvider epService) {
        String[] fields = "mycontextvar".split(",");
        epService.getEPAdministrator().createEPL("create context MyCtx as " +
                "partition by theString from SupportBean, p00 from SupportBean_S0");
        epService.getEPAdministrator().createEPL("context MyCtx create variable int mycontextvar = 0");
        epService.getEPAdministrator().createEPL("context MyCtx on SupportBean(intPrimitive > 0) set mycontextvar = intPrimitive");
        SupportUpdateListener listener = new SupportUpdateListener();
        epService.getEPAdministrator().createEPL("context MyCtx select mycontextvar from SupportBean_S0").addListener(listener);

        epService.getEPRuntime().sendEvent(new SupportBean("P1", 0));   // allocate partition P1
        epService.getEPRuntime().sendEvent(new SupportBean("P1", 10));   // set variable
        epService.getEPRuntime().sendEvent(new SupportBean_S0(1, "P1"));
        EPAssertionUtil.assertProps(listener.assertOneGetNewAndReset(), fields, new Object[]{10});

        epService.getEPRuntime().sendEvent(new SupportBean("P2", 11));   // allocate and set variable partition E2
        epService.getEPRuntime().sendEvent(new SupportBean_S0(2, "P2"));
        EPAssertionUtil.assertProps(listener.assertOneGetNewAndReset(), fields, new Object[]{11});

        epService.getEPRuntime().sendEvent(new SupportBean_S0(3, "P1"));
        EPAssertionUtil.assertProps(listener.assertOneGetNewAndReset(), fields, new Object[]{10});
        epService.getEPRuntime().sendEvent(new SupportBean_S0(4, "P2"));
        EPAssertionUtil.assertProps(listener.assertOneGetNewAndReset(), fields, new Object[]{11});
        epService.getEPRuntime().sendEvent(new SupportBean_S0(5, "P3"));
        EPAssertionUtil.assertProps(listener.assertOneGetNewAndReset(), fields, new Object[]{0});

        epService.getEPRuntime().sendEvent(new SupportBean("P3", 12));
        epService.getEPRuntime().sendEvent(new SupportBean_S0(6, "P3"));
        EPAssertionUtil.assertProps(listener.assertOneGetNewAndReset(), fields, new Object[]{12});

        for (String statement : epService.getEPAdministrator().getStatementNames()) {
            epService.getEPAdministrator().getStatement(statement).stop();
        }

        epService.getEPAdministrator().destroyAllStatements();
    }

    private void runAssertionOverlapping(EPServiceProvider epService) throws ParseException, DeploymentException, IOException, InterruptedException {
        String[] fields = "mycontextvar".split(",");
        epService.getEPAdministrator().createEPL("create context MyCtx as " +
                "initiated by SupportBean_S0 s0 terminated by SupportBean_S1(p10 = s0.p00)");
        epService.getEPAdministrator().createEPL("context MyCtx create variable int mycontextvar = 5");
        epService.getEPAdministrator().createEPL("context MyCtx on SupportBean(theString = context.s0.p00) set mycontextvar = intPrimitive");
        epService.getEPAdministrator().createEPL("context MyCtx on SupportBean(intPrimitive < 0) set mycontextvar = intPrimitive");
        SupportUpdateListener listener = new SupportUpdateListener();
        epService.getEPAdministrator().createEPL("context MyCtx select mycontextvar from SupportBean_S2(p20 = context.s0.p00)").addListener(listener);

        epService.getEPRuntime().sendEvent(new SupportBean_S0(0, "P1"));    // allocate partition P1
        epService.getEPRuntime().sendEvent(new SupportBean_S2(1, "P1"));
        EPAssertionUtil.assertProps(listener.assertOneGetNewAndReset(), fields, new Object[]{5});

        epService.getEPRuntime().sendEvent(new SupportBean_S0(0, "P2"));    // allocate partition P2
        epService.getEPRuntime().sendEvent(new SupportBean("P2", 10));
        epService.getEPRuntime().sendEvent(new SupportBean_S2(2, "P2"));
        EPAssertionUtil.assertProps(listener.assertOneGetNewAndReset(), fields, new Object[]{10});

        // set all to -1
        epService.getEPRuntime().sendEvent(new SupportBean("P2", -1));
        epService.getEPRuntime().sendEvent(new SupportBean_S2(2, "P2"));
        EPAssertionUtil.assertProps(listener.assertOneGetNewAndReset(), fields, new Object[]{-1});
        epService.getEPRuntime().sendEvent(new SupportBean_S2(2, "P1"));
        EPAssertionUtil.assertProps(listener.assertOneGetNewAndReset(), fields, new Object[]{-1});

        epService.getEPRuntime().sendEvent(new SupportBean("P2", 20));
        epService.getEPRuntime().sendEvent(new SupportBean("P1", 21));
        epService.getEPRuntime().sendEvent(new SupportBean_S2(2, "P2"));
        EPAssertionUtil.assertProps(listener.assertOneGetNewAndReset(), fields, new Object[]{20});
        epService.getEPRuntime().sendEvent(new SupportBean_S2(2, "P1"));
        EPAssertionUtil.assertProps(listener.assertOneGetNewAndReset(), fields, new Object[]{21});

        // terminate context partitions
        epService.getEPRuntime().sendEvent(new SupportBean_S1(0, "P1"));
        epService.getEPRuntime().sendEvent(new SupportBean_S1(0, "P2"));

        epService.getEPRuntime().sendEvent(new SupportBean_S0(0, "P1"));    // allocate partition P1
        epService.getEPRuntime().sendEvent(new SupportBean_S2(1, "P1"));
        EPAssertionUtil.assertProps(listener.assertOneGetNewAndReset(), fields, new Object[]{5});

        epService.getEPAdministrator().destroyAllStatements();

        // test module deployment and undeployment
        String epl = "@Name(\"context\")\n" +
                "create context MyContext\n" +
                "initiated by distinct(theString) SupportBean as input\n" +
                "terminated by SupportBean(theString = input.theString);\n" +
                "\n" +
                "@Name(\"ctx variable counter\")\n" +
                "context MyContext create variable integer counter = 0;\n";
        DeploymentResult result = epService.getEPAdministrator().getDeploymentAdmin().parseDeploy(epl);
        epService.getEPAdministrator().getDeploymentAdmin().undeploy(result.getDeploymentId());
        result = epService.getEPAdministrator().getDeploymentAdmin().parseDeploy(epl);
        epService.getEPAdministrator().getDeploymentAdmin().undeploy(result.getDeploymentId());
    }

    private void runAssertionIterateAndListen(EPServiceProvider epService) {
        epService.getEPAdministrator().createEPL("@name('ctx') create context MyCtx as initiated by SupportBean_S0 s0 terminated after 24 hours");

        String[] fields = "mycontextvar".split(",");
        SupportUpdateListener listenerCreateVariable = new SupportUpdateListener();
        EPStatement stmtVar = epService.getEPAdministrator().createEPL("@name('var') context MyCtx create variable int mycontextvar = 5");
        stmtVar.addListener(listenerCreateVariable);

        SupportUpdateListener listenerUpdate = new SupportUpdateListener();
        EPStatement stmtUpd = epService.getEPAdministrator().createEPL("@name('upd') context MyCtx on SupportBean(theString = context.s0.p00) set mycontextvar = intPrimitive");
        stmtUpd.addListener(listenerUpdate);

        epService.getEPRuntime().sendEvent(new SupportBean_S0(0, "P1"));    // allocate partition P1
        epService.getEPRuntime().sendEvent(new SupportBean("P1", 100));    // update
        EPAssertionUtil.assertProps(listenerUpdate.assertOneGetNewAndReset(), "mycontextvar".split(","), new Object[]{100});
        EPAssertionUtil.assertPropsPerRow(EPAssertionUtil.iteratorToArray(stmtUpd.iterator()), fields, new Object[][]{{100}});

        epService.getEPRuntime().sendEvent(new SupportBean_S0(0, "P2"));    // allocate partition P1
        epService.getEPRuntime().sendEvent(new SupportBean("P2", 101));    // update
        EPAssertionUtil.assertProps(listenerUpdate.assertOneGetNewAndReset(), "mycontextvar".split(","), new Object[]{101});
        EPAssertionUtil.assertPropsPerRow(EPAssertionUtil.iteratorToArray(stmtUpd.iterator()), fields, new Object[][]{{100}, {101}});

        EventBean[] events = EPAssertionUtil.iteratorToArray(stmtVar.iterator());
        EPAssertionUtil.assertPropsPerRowAnyOrder(events, "mycontextvar".split(","), new Object[][]{{100}, {101}});
        EPAssertionUtil.assertPropsPerRowAnyOrder(listenerCreateVariable.getNewDataListFlattened(), "mycontextvar".split(","), new Object[][]{{100}, {101}});

        epService.getEPAdministrator().destroyAllStatements();
    }

    private void runAssertionGetSetAPI(EPServiceProvider epService) {
        if (SupportConfigFactory.skipTest(ExecContextVariables.class)) {
            return;
        }

        epService.getEPAdministrator().createEPL("create context MyCtx as initiated by SupportBean_S0 s0 terminated after 24 hours");
        epService.getEPAdministrator().createEPL("context MyCtx create variable int mycontextvar = 5");
        epService.getEPAdministrator().createEPL("context MyCtx on SupportBean(theString = context.s0.p00) set mycontextvar = intPrimitive");

        epService.getEPRuntime().sendEvent(new SupportBean_S0(0, "P1"));    // allocate partition P1
        assertVariableValues(epService, 0, 5);

        epService.getEPRuntime().setVariableValue(Collections.<String, Object>singletonMap("mycontextvar", 10), 0);
        assertVariableValues(epService, 0, 10);

        epService.getEPRuntime().sendEvent(new SupportBean_S0(0, "P2"));    // allocate partition P2
        assertVariableValues(epService, 1, 5);

        epService.getEPRuntime().setVariableValue(Collections.<String, Object>singletonMap("mycontextvar", 11), 1);
        assertVariableValues(epService, 1, 11);

        // global variable - trying to set via context partition selection
        epService.getEPAdministrator().createEPL("create variable int myglobarvar = 0");
        try {
            epService.getEPRuntime().setVariableValue(Collections.<String, Object>singletonMap("myglobarvar", 11), 0);
            fail();
        } catch (VariableNotFoundException ex) {
            assertEquals("Variable by name 'myglobarvar' is a global variable and not context-partitioned", ex.getMessage());
        }

        // global variable - trying to get via context partition selection
        try {
            epService.getEPRuntime().getVariableValue(Collections.singleton("myglobarvar"), new SupportSelectorById(1));
            fail();
        } catch (VariableNotFoundException ex) {
            assertEquals("Variable by name 'myglobarvar' is a global variable and not context-partitioned", ex.getMessage());
        }

        epService.getEPAdministrator().destroyAllStatements();
    }

    private void runAssertionInvalid(EPServiceProvider epService) {
        epService.getEPAdministrator().createEPL("create context MyCtxOne as partition by theString from SupportBean");
        epService.getEPAdministrator().createEPL("create context MyCtxTwo as partition by p00 from SupportBean_S0");
        epService.getEPAdministrator().createEPL("context MyCtxOne create variable int myctxone_int = 0");

        // undefined context
        tryInvalid(epService, "context MyCtx create variable int mycontext_invalid1 = 0",
                "Error starting statement: Context by name 'MyCtx' has not been declared [context MyCtx create variable int mycontext_invalid1 = 0]");

        // wrong context uses variable
        tryInvalid(epService, "context MyCtxTwo select myctxone_int from SupportBean_S0",
                "Variable 'myctxone_int' defined for use with context 'MyCtxOne' is not available for use with context 'MyCtxTwo' [context MyCtxTwo select myctxone_int from SupportBean_S0]");

        // variable use outside of context
        tryInvalid(epService, "select myctxone_int from SupportBean_S0",
                "Variable 'myctxone_int' defined for use with context 'MyCtxOne' can only be accessed within that context [select myctxone_int from SupportBean_S0]");
        tryInvalid(epService, "select * from SupportBean_S0#expr(myctxone_int > 5)",
                "Variable 'myctxone_int' defined for use with context 'MyCtxOne' can only be accessed within that context [select * from SupportBean_S0#expr(myctxone_int > 5)]");
        tryInvalid(epService, "select * from SupportBean_S0#keepall limit myctxone_int",
                "Error starting statement: Variable 'myctxone_int' defined for use with context 'MyCtxOne' can only be accessed within that context [select * from SupportBean_S0#keepall limit myctxone_int]");
        tryInvalid(epService, "select * from SupportBean_S0#keepall limit 10 offset myctxone_int",
                "Error starting statement: Variable 'myctxone_int' defined for use with context 'MyCtxOne' can only be accessed within that context [select * from SupportBean_S0#keepall limit 10 offset myctxone_int]");
        tryInvalid(epService, "select * from SupportBean_S0#keepall output every myctxone_int events",
                "Error starting statement: Error in the output rate limiting clause: Variable 'myctxone_int' defined for use with context 'MyCtxOne' can only be accessed within that context [select * from SupportBean_S0#keepall output every myctxone_int events]");
        tryInvalid(epService, "@Hint('reclaim_group_aged=myctxone_int') select longPrimitive, count(*) from SupportBean group by longPrimitive",
                "Error starting statement: Variable 'myctxone_int' defined for use with context 'MyCtxOne' can only be accessed within that context [@Hint('reclaim_group_aged=myctxone_int') select longPrimitive, count(*) from SupportBean group by longPrimitive]");
    }

    private void assertVariableValues(EPServiceProvider epService, int agentInstanceId, int expected) {
        Map<String, List<ContextPartitionVariableState>> states = epService.getEPRuntime().getVariableValue(Collections.singleton("mycontextvar"), new SupportSelectorById(agentInstanceId));
        assertEquals(1, states.size());
        List<ContextPartitionVariableState> list = states.get("mycontextvar");
        assertEquals(1, list.size());
        assertEquals(expected, list.get(0).getState());
    }
}
