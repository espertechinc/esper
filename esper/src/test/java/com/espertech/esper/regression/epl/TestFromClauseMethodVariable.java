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

package com.espertech.esper.regression.epl;

import com.espertech.esper.client.*;
import com.espertech.esper.client.scopetest.EPAssertionUtil;
import com.espertech.esper.client.scopetest.SupportUpdateListener;
import com.espertech.esper.metrics.instrumentation.InstrumentationHelper;
import com.espertech.esper.support.bean.*;
import com.espertech.esper.support.client.SupportConfigFactory;
import com.espertech.esper.support.util.SupportMessageAssertUtil;
import com.espertech.esper.support.util.SupportModelHelper;
import junit.framework.TestCase;

import java.io.Serializable;

public class TestFromClauseMethodVariable extends TestCase
{
    private EPServiceProvider epService;
    private SupportUpdateListener listener;

    public void setUp()
    {
        Configuration config = SupportConfigFactory.getConfiguration();
        config.addMethodRef(MyStaticService.class, new ConfigurationMethodRef());
        config.addImport(MyStaticService.class);

        config.getEngineDefaults().getLogging().setEnableQueryPlan(true);
        config.addEventType(SupportBean.class);
        config.addEventType(SupportBean_S0.class);
        config.addEventType(SupportBean_S1.class);
        config.addEventType(SupportBean_S2.class);
        epService = EPServiceProviderManager.getDefaultProvider(config);
        epService.initialize();
        if (InstrumentationHelper.ENABLED) { InstrumentationHelper.startTest(epService, this.getClass(), getName());}
        listener = new SupportUpdateListener();
    }

    protected void tearDown() throws Exception {
        if (InstrumentationHelper.ENABLED) { InstrumentationHelper.endTest();}
        listener = null;
    }

    public void testVariables() {
        epService.getEPAdministrator().getConfiguration().addVariable("MyConstantServiceVariable", MyConstantServiceVariable.class, new MyConstantServiceVariable());
        runAssertionConstantVariable();

        epService.getEPAdministrator().getConfiguration().addVariable("MyNonConstantServiceVariable", MyNonConstantServiceVariable.class, new MyNonConstantServiceVariable("postfix"));
        runAssertionNonConstantVariable(false);
        runAssertionNonConstantVariable(true);

        runAssertionContextVariable();

        // invalid footprint
        SupportMessageAssertUtil.tryInvalid(epService, "select * from method:MyConstantServiceVariable.fetchABean() as h0",
                "Error starting statement: Method footprint does not match the number or type of expression parameters, expecting no parameters in method: Could not find enumeration method, date-time method or instance method named 'fetchABean' in class 'com.espertech.esper.regression.epl.TestFromClauseMethodVariable$MyConstantServiceVariable' taking no parameters (nearest match found was 'fetchABean' taking type(s) 'int') [");
    }

    private void runAssertionContextVariable() {
        epService.getEPAdministrator().getConfiguration().addImport(MyNonConstantServiceVariableFactory.class);
        epService.getEPAdministrator().getConfiguration().addImport(MyNonConstantServiceVariable.class);

        epService.getEPAdministrator().createEPL("create context MyContext " +
                "initiated by SupportBean_S0 as c_s0 " +
                "terminated by SupportBean_S1(id=c_s0.id)");
        epService.getEPAdministrator().createEPL("context MyContext " +
                "create variable MyNonConstantServiceVariable var = MyNonConstantServiceVariableFactory.make()");
        epService.getEPAdministrator().createEPL("context MyContext " +
                "select id as c0 from SupportBean(intPrimitive=context.c_s0.id) as sb, " +
                "method:var.fetchABean(intPrimitive) as h0").addListener(listener);
        epService.getEPAdministrator().createEPL("context MyContext on SupportBean_S2(id = context.c_s0.id) set var.postfix=p20");

        epService.getEPRuntime().sendEvent(new SupportBean_S0(1));
        epService.getEPRuntime().sendEvent(new SupportBean_S0(2));

        sendEventAssert("E1", 1, "_1_context_postfix");
        sendEventAssert("E2", 2, "_2_context_postfix");

        epService.getEPRuntime().sendEvent(new SupportBean_S2(1, "a"));
        epService.getEPRuntime().sendEvent(new SupportBean_S2(2, "b"));

        sendEventAssert("E1", 1, "_1_a");
        sendEventAssert("E2", 2, "_2_b");

        // invalid context
        SupportMessageAssertUtil.tryInvalid(epService, "select * from method:var.fetchABean(intPrimitive) as h0",
                "Error starting statement: Variable by name 'var' has been declared for context 'MyContext' and can only be used within the same context");
        epService.getEPAdministrator().createEPL("create context ABC start @now end after 1 minute");
        SupportMessageAssertUtil.tryInvalid(epService, "context ABC select * from method:var.fetchABean(intPrimitive) as h0",
                "Error starting statement: Variable by name 'var' has been declared for context 'MyContext' and can only be used within the same context");
    }

    private void runAssertionConstantVariable()
    {
        String epl = "select id as c0 from SupportBean as sb, " +
                   "method:MyConstantServiceVariable.fetchABean(intPrimitive) as h0";
        EPStatement stmt = epService.getEPAdministrator().createEPL(epl);
        listener = new SupportUpdateListener();
        stmt.addListener(listener);

        sendEventAssert("E1", 10, "_10_");
        sendEventAssert("E2", 20, "_20_");

        epService.getEPAdministrator().destroyAllStatements();
    }

    private void runAssertionNonConstantVariable(boolean soda)
    {
        String modifyEPL = "on SupportBean_S0 set MyNonConstantServiceVariable.postfix=p00";
        SupportModelHelper.createByCompileOrParse(epService, soda, modifyEPL);

        String epl = "select id as c0 from SupportBean as sb, " +
                "method:MyNonConstantServiceVariable.fetchABean(intPrimitive) as h0";
        EPStatement stmt = SupportModelHelper.createByCompileOrParse(epService, soda, epl);
        listener = new SupportUpdateListener();
        stmt.addListener(listener);

        sendEventAssert("E1", 10, "_10_postfix");

        epService.getEPRuntime().sendEvent(new SupportBean_S0(1, "newpostfix"));
        sendEventAssert("E1", 20, "_20_newpostfix");

        // return to original value
        epService.getEPRuntime().sendEvent(new SupportBean_S0(2, "postfix"));
        sendEventAssert("E1", 30, "_30_postfix");

        epService.getEPAdministrator().destroyAllStatements();
    }

    private void sendEventAssert(String theString, int intPrimitive, String expected) {
        String[] fields = "c0".split(",");
        epService.getEPRuntime().sendEvent(new SupportBean(theString, intPrimitive));
        EPAssertionUtil.assertProps(listener.assertOneGetNewAndReset(), fields, new Object[]{expected});
    }

    private static class MyConstantServiceVariable {
        public SupportBean_A fetchABean(int intPrimitive) {
            return new SupportBean_A("_" + intPrimitive + "_");
        }
    }

    private static class MyNonConstantServiceVariable implements Serializable {
        private String postfix;

        public MyNonConstantServiceVariable(String postfix) {
            this.postfix = postfix;
        }

        public void setPostfix(String postfix) {
            this.postfix = postfix;
        }

        public String getPostfix() {
            return postfix;
        }

        public SupportBean_A fetchABean(int intPrimitive) {
            return new SupportBean_A("_" + intPrimitive + "_" + postfix);
        }
    }

    private static class MyStaticService {
        public static SupportBean_A fetchABean(int intPrimitive) {
            return new SupportBean_A("_" + intPrimitive + "_");
        }
    }

    private static class MyNonConstantServiceVariableFactory {
        public static MyNonConstantServiceVariable make() {
            return new MyNonConstantServiceVariable("context_postfix");
        }
    }
}
