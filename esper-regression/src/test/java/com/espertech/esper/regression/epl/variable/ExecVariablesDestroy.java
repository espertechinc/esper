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
package com.espertech.esper.regression.epl.variable;

import com.espertech.esper.client.*;
import com.espertech.esper.client.scopetest.EPAssertionUtil;
import com.espertech.esper.supportregression.bean.SupportBean;
import com.espertech.esper.supportregression.execution.RegressionExecution;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.fail;

public class ExecVariablesDestroy implements RegressionExecution {
    public void configure(Configuration configuration) throws Exception {
        configuration.addVariable("MyPermanentVar", String.class, "thevalue");
        configuration.addEventType(SupportBean.class);
    }

    public void run(EPServiceProvider epService) throws Exception {
        runAssertionDestroyReCreateChangeType(epService);
        runAssertionManageDependency(epService);
        runAssertionConfigAPI(epService);
    }

    private void runAssertionDestroyReCreateChangeType(EPServiceProvider epService) {
        String text = "@Name('ABC') create variable long var1 = 2";
        EPStatement stmtOne = epService.getEPAdministrator().createEPL(text);

        assertEquals(2L, epService.getEPRuntime().getVariableValue("var1"));
        assertStmtsRef(epService, "var1", new String[]{"ABC"});

        stmtOne.destroy();

        assertNotFound(epService, "var1");
        assertStmtsRef(epService, "var1", null);

        text = "@Name('CDE') create variable string var1 = 'a'";
        EPStatement stmtTwo = epService.getEPAdministrator().createEPL(text);

        assertStmtsRef(epService, "var1", new String[]{"CDE"});
        assertEquals("a", epService.getEPRuntime().getVariableValue("var1"));

        stmtTwo.destroy();
        assertNotFound(epService, "var1");
    }

    private void runAssertionManageDependency(EPServiceProvider epService) {
        // single variable
        EPStatement stmtOne = epService.getEPAdministrator().createEPL("@Name('S0') create variable boolean var2 = true");
        EPStatement stmtTwo = epService.getEPAdministrator().createEPL("@Name('S1') select * from SupportBean(var2)");
        assertStmtsRef(epService, "var2", new String[]{"S0", "S1"});
        assertEquals(true, epService.getEPRuntime().getVariableValue("var2"));

        stmtOne.destroy();
        assertStmtsRef(epService, "var2", new String[]{"S1"});
        assertEquals(true, epService.getEPRuntime().getVariableValue("var2"));

        stmtTwo.destroy();
        assertStmtsRef(epService, "var2", null);
        assertNotFound(epService, "var2");

        // multiple variable
        EPStatement stmt1 = epService.getEPAdministrator().createEPL("@Name('T0') create variable boolean v1 = true");
        EPStatement stmt2 = epService.getEPAdministrator().createEPL("@Name('T1') create variable long v2 = 1");
        EPStatement stmt3 = epService.getEPAdministrator().createEPL("@Name('T2') create variable string v3 = 'a'");
        EPStatement stmtUseOne = epService.getEPAdministrator().createEPL("@Name('TX') select * from SupportBean(v1, v2=1, v3='a')");
        EPStatement stmtUseTwo = epService.getEPAdministrator().createEPL("@Name('TY') select * from SupportBean(v2=2)");
        EPStatement stmtUseThree = epService.getEPAdministrator().createEPL("@Name('TZ') select * from SupportBean(v3='A', v1)");

        assertStmtsRef(epService, "v1", new String[]{"T0", "TX", "TZ"});
        assertStmtsRef(epService, "v2", new String[]{"T1", "TX", "TY"});
        assertStmtsRef(epService, "v3", new String[]{"T2", "TX", "TZ"});

        stmt2.destroy();
        assertStmtsRef(epService, "v2", new String[]{"TX", "TY"});

        stmtUseOne.destroy();
        assertStmtsRef(epService, "v2", new String[]{"TY"});

        stmtUseTwo.destroy();
        assertStmtsRef(epService, "v2", null);
        assertNotFound(epService, "v2");

        EPStatement stmt4 = epService.getEPAdministrator().createEPL("@Name('T3') create variable boolean v4 = true");
        EPStatement stmtUseFour = epService.getEPAdministrator().createEPL("@Name('TQ') select * from SupportBean(v4)");

        assertStmtsRef(epService, "v4", new String[]{"T3", "TQ"});
        assertEquals(true, epService.getEPRuntime().getVariableValue("v4"));

        stmt1.destroy();
        stmtUseThree.destroy();

        assertStmtsRef(epService, "v1", null);
        assertNotFound(epService, "v1");
        assertEquals("a", epService.getEPRuntime().getVariableValue("v3"));
        assertStmtsRef(epService, "v3", new String[]{"T2"});

        stmt3.destroy();
        assertNotFound(epService, "v3");

        stmt4.destroy();
        stmtUseFour.destroy();
        assertNotFound(epService, "v4");

        assertEquals(1, epService.getEPRuntime().getVariableValueAll().size());

        epService.getEPAdministrator().destroyAllStatements();
    }

    private void runAssertionConfigAPI(EPServiceProvider epService) {
        EPStatement stmtOne = epService.getEPAdministrator().createEPL("@Name('S0') create variable boolean var2 = true");
        EPStatement stmtTwo = epService.getEPAdministrator().createEPL("@Name('S1') select * from SupportBean(var2)");

        try {
            epService.getEPAdministrator().getConfiguration().removeVariable("var2", false);
            fail();
        } catch (ConfigurationException ex) {
            assertEquals("Variable 'var2' is in use by one or more statements", ex.getMessage());
        }

        epService.getEPAdministrator().getConfiguration().removeVariable("var2", true);

        stmtOne.destroy();
        stmtTwo.destroy();
        assertNotFound(epService, "var2");

        // try permanent variable
        assertEquals("thevalue", epService.getEPRuntime().getVariableValue("MyPermanentVar"));
        EPStatement stmtThree = epService.getEPAdministrator().createEPL("@Name('S2') select * from SupportBean(MyPermanentVar = 'Y')");
        assertStmtsRef(epService, "MyPermanentVar", new String[]{"S2"});
        stmtThree.destroy();
        assertStmtsRef(epService, "MyPermanentVar", null);
        assertEquals("thevalue", epService.getEPRuntime().getVariableValue("MyPermanentVar"));
    }

    private void assertStmtsRef(EPServiceProvider epService, String variableName, String[] stmts) {
        EPAssertionUtil.assertEqualsAnyOrder(stmts, epService.getEPAdministrator().getConfiguration().getVariableNameUsedBy(variableName).toArray());
    }

    private void assertNotFound(EPServiceProvider epService, String var) {
        try {
            epService.getEPRuntime().getVariableValue(var);
            fail();
        } catch (VariableNotFoundException ex) {
            // expected
        }
        assertStmtsRef(epService, var, null);
    }
}