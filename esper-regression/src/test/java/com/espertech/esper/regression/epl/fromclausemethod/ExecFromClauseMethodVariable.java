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
package com.espertech.esper.regression.epl.fromclausemethod;

import com.espertech.esper.client.Configuration;
import com.espertech.esper.client.ConfigurationMethodRef;
import com.espertech.esper.client.EPServiceProvider;
import com.espertech.esper.client.EPStatement;
import com.espertech.esper.client.scopetest.EPAssertionUtil;
import com.espertech.esper.client.scopetest.SupportUpdateListener;
import com.espertech.esper.supportregression.bean.*;
import com.espertech.esper.supportregression.execution.RegressionExecution;
import com.espertech.esper.supportregression.util.SupportMessageAssertUtil;
import com.espertech.esper.supportregression.util.SupportModelHelper;

import java.io.Serializable;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;

public class ExecFromClauseMethodVariable implements RegressionExecution {
    public void configure(Configuration configuration) throws Exception {
        configuration.addMethodRef(MyStaticService.class, new ConfigurationMethodRef());
        configuration.addImport(MyStaticService.class);

        configuration.getEngineDefaults().getLogging().setEnableQueryPlan(true);
        configuration.addEventType(SupportBean.class);
        configuration.addEventType(SupportBean_S0.class);
        configuration.addEventType(SupportBean_S1.class);
        configuration.addEventType(SupportBean_S2.class);
    }

    public void run(EPServiceProvider epService) throws Exception {

        epService.getEPAdministrator().getConfiguration().addVariable("MyConstantServiceVariable", MyConstantServiceVariable.class, new MyConstantServiceVariable());
        runAssertionConstantVariable(epService);

        epService.getEPAdministrator().getConfiguration().addVariable("MyNonConstantServiceVariable", MyNonConstantServiceVariable.class, new MyNonConstantServiceVariable("postfix"));
        tryAssertionNonConstantVariable(epService, false);
        tryAssertionNonConstantVariable(epService, true);

        runAssertionContextVariable(epService);

        runAssertionVariableMapAndOA(epService);

        // invalid footprint
        SupportMessageAssertUtil.tryInvalid(epService, "select * from method:MyConstantServiceVariable.fetchABean() as h0",
                "Error starting statement: Method footprint does not match the number or type of expression parameters, expecting no parameters in method: Could not find enumeration method, date-time method or instance method named 'fetchABean' in class 'com.espertech.esper.regression.epl.fromclausemethod.ExecFromClauseMethodVariable$MyConstantServiceVariable' taking no parameters (nearest match found was 'fetchABean' taking type(s) 'int') [");

        // null variable value and metadata is instance method
        epService.getEPAdministrator().getConfiguration().addVariable("MyNullMap", MyMethodHandlerMap.class, null);
        SupportMessageAssertUtil.tryInvalid(epService, "select field1, field2 from method:MyNullMap.getMapData()",
                "Error starting statement: Failed to access variable method invocation metadata: The variable value is null and the metadata method is an instance method");

        // variable with context and metadata is instance method
        epService.getEPAdministrator().createEPL("create context BetweenStartAndEnd start SupportBean end SupportBean");
        epService.getEPAdministrator().createEPL("context BetweenStartAndEnd create variable " + MyMethodHandlerMap.class.getName() + " themap");
        SupportMessageAssertUtil.tryInvalid(epService, "context BetweenStartAndEnd select field1, field2 from method:themap.getMapData()",
                "Error starting statement: Failed to access variable method invocation metadata: The metadata method is an instance method however the variable is contextual, please declare the metadata method as static or remove the context declaration for the variable");
    }

    private void runAssertionVariableMapAndOA(EPServiceProvider epService) {
        epService.getEPAdministrator().getConfiguration().addVariable("MyMethodHandlerMap", MyMethodHandlerMap.class, new MyMethodHandlerMap("a", "b"));
        epService.getEPAdministrator().getConfiguration().addVariable("MyMethodHandlerOA", MyMethodHandlerOA.class, new MyMethodHandlerOA("a", "b"));

        for (String epl : new String[]{
            "select field1, field2 from method:MyMethodHandlerMap.getMapData()",
            "select field1, field2 from method:MyMethodHandlerOA.getOAData()"
        }) {
            EPStatement stmt = epService.getEPAdministrator().createEPL(epl);
            EPAssertionUtil.assertProps(stmt.iterator().next(), "field1,field2".split(","), new Object[]{"a", "b"});
        }
    }

    private void runAssertionContextVariable(EPServiceProvider epService) {
        epService.getEPAdministrator().getConfiguration().addImport(MyNonConstantServiceVariableFactory.class);
        epService.getEPAdministrator().getConfiguration().addImport(MyNonConstantServiceVariable.class);

        epService.getEPAdministrator().createEPL("create context MyContext " +
                "initiated by SupportBean_S0 as c_s0 " +
                "terminated by SupportBean_S1(id=c_s0.id)");
        epService.getEPAdministrator().createEPL("context MyContext " +
                "create variable MyNonConstantServiceVariable var = MyNonConstantServiceVariableFactory.make()");
        SupportUpdateListener listener = new SupportUpdateListener();
        epService.getEPAdministrator().createEPL("context MyContext " +
                "select id as c0 from SupportBean(intPrimitive=context.c_s0.id) as sb, " +
                "method:var.fetchABean(intPrimitive) as h0").addListener(listener);
        epService.getEPAdministrator().createEPL("context MyContext on SupportBean_S2(id = context.c_s0.id) set var.postfix=p20");

        epService.getEPRuntime().sendEvent(new SupportBean_S0(1));
        epService.getEPRuntime().sendEvent(new SupportBean_S0(2));

        sendEventAssert(epService, listener, "E1", 1, "_1_context_postfix");
        sendEventAssert(epService, listener, "E2", 2, "_2_context_postfix");

        epService.getEPRuntime().sendEvent(new SupportBean_S2(1, "a"));
        epService.getEPRuntime().sendEvent(new SupportBean_S2(2, "b"));

        sendEventAssert(epService, listener, "E1", 1, "_1_a");
        sendEventAssert(epService, listener, "E2", 2, "_2_b");

        // invalid context
        SupportMessageAssertUtil.tryInvalid(epService, "select * from method:var.fetchABean(intPrimitive) as h0",
                "Error starting statement: Variable by name 'var' has been declared for context 'MyContext' and can only be used within the same context");
        epService.getEPAdministrator().createEPL("create context ABC start @now end after 1 minute");
        SupportMessageAssertUtil.tryInvalid(epService, "context ABC select * from method:var.fetchABean(intPrimitive) as h0",
                "Error starting statement: Variable by name 'var' has been declared for context 'MyContext' and can only be used within the same context");
    }

    private void runAssertionConstantVariable(EPServiceProvider epService) {
        String epl = "select id as c0 from SupportBean as sb, " +
                "method:MyConstantServiceVariable.fetchABean(intPrimitive) as h0";
        EPStatement stmt = epService.getEPAdministrator().createEPL(epl);
        SupportUpdateListener listener = new SupportUpdateListener();
        stmt.addListener(listener);

        sendEventAssert(epService, listener, "E1", 10, "_10_");
        sendEventAssert(epService, listener, "E2", 20, "_20_");

        epService.getEPAdministrator().destroyAllStatements();
    }

    private void tryAssertionNonConstantVariable(EPServiceProvider epService, boolean soda) {
        String modifyEPL = "on SupportBean_S0 set MyNonConstantServiceVariable.postfix=p00";
        SupportModelHelper.createByCompileOrParse(epService, soda, modifyEPL);

        String epl = "select id as c0 from SupportBean as sb, " +
                "method:MyNonConstantServiceVariable.fetchABean(intPrimitive) as h0";
        EPStatement stmt = SupportModelHelper.createByCompileOrParse(epService, soda, epl);
        SupportUpdateListener listener = new SupportUpdateListener();
        stmt.addListener(listener);

        sendEventAssert(epService, listener, "E1", 10, "_10_postfix");

        epService.getEPRuntime().sendEvent(new SupportBean_S0(1, "newpostfix"));
        sendEventAssert(epService, listener, "E1", 20, "_20_newpostfix");

        // return to original value
        epService.getEPRuntime().sendEvent(new SupportBean_S0(2, "postfix"));
        sendEventAssert(epService, listener, "E1", 30, "_30_postfix");

        epService.getEPAdministrator().destroyAllStatements();
    }

    private void sendEventAssert(EPServiceProvider epService, SupportUpdateListener listener, String theString, int intPrimitive, String expected) {
        String[] fields = "c0".split(",");
        epService.getEPRuntime().sendEvent(new SupportBean(theString, intPrimitive));
        EPAssertionUtil.assertProps(listener.assertOneGetNewAndReset(), fields, new Object[]{expected});
    }

    public static class MyConstantServiceVariable {
        public SupportBean_A fetchABean(int intPrimitive) {
            return new SupportBean_A("_" + intPrimitive + "_");
        }
    }

    public static class MyNonConstantServiceVariable implements Serializable {
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

    public static class MyMethodHandlerMap {
        private final String field1;
        private final String field2;

        public MyMethodHandlerMap(String field1, String field2) {
            this.field1 = field1;
            this.field2 = field2;
        }

        public Map<String, Object> getMapDataMetadata() {
            Map<String, Object> fields = new HashMap<String, Object>();
            fields.put("field1", String.class);
            fields.put("field2", String.class);
            return fields;
        }

        public Map<String, Object>[] getMapData() {
            Map[] maps = new Map[1];
            HashMap<String, Object> row = new HashMap<String, Object>();
            maps[0] = row;
            row.put("field1", field1);
            row.put("field2", field2);
            return maps;
        }
    }

    public static class MyMethodHandlerOA {
        private final String field1;
        private final String field2;

        public MyMethodHandlerOA(String field1, String field2) {
            this.field1 = field1;
            this.field2 = field2;
        }

        public static LinkedHashMap<String, Object> getOADataMetadata() {
            LinkedHashMap<String, Object> fields = new LinkedHashMap<String, Object>();
            fields.put("field1", String.class);
            fields.put("field2", String.class);
            return fields;
        }

        public Object[][] getOAData() {
            return new Object[][]{{field1, field2}};
        }
    }
}
