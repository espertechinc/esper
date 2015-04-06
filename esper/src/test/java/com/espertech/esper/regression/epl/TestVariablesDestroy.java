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
import com.espertech.esper.metrics.instrumentation.InstrumentationHelper;
import com.espertech.esper.support.bean.SupportBean;
import com.espertech.esper.support.client.SupportConfigFactory;
import junit.framework.TestCase;

public class TestVariablesDestroy extends TestCase
{
    private EPServiceProvider epService;

    public void setUp()
    {
        Configuration config = SupportConfigFactory.getConfiguration();
        config.addVariable("MyPermanentVar", String.class, "thevalue");
        epService = EPServiceProviderManager.getDefaultProvider(config);
        epService.initialize();
        if (InstrumentationHelper.ENABLED) { InstrumentationHelper.startTest(epService, this.getClass(), getName());}
        epService.getEPAdministrator().getConfiguration().addEventType("SupportBean", SupportBean.class);
    }

    public void tearDown() {
        if (InstrumentationHelper.ENABLED) { InstrumentationHelper.endTest();}
    }

    public void testDestroyReCreateChangeType()
    {
        String text = "@Name('ABC') create variable long var1 = 2";
        EPStatement stmtOne = epService.getEPAdministrator().createEPL(text);

        assertEquals(2L, epService.getEPRuntime().getVariableValue("var1"));
        assertStmtsRef("var1", new String[] {"ABC"});

        stmtOne.destroy();

        assertNotFound("var1");
        assertStmtsRef("var1", null);

        text = "@Name('CDE') create variable string var1 = 'a'";
        EPStatement stmtTwo = epService.getEPAdministrator().createEPL(text);

        assertStmtsRef("var1", new String[] {"CDE"});
        assertEquals("a", epService.getEPRuntime().getVariableValue("var1"));

        stmtTwo.destroy();
        assertNotFound("var1");
    }

    public void testManageDependency()
    {
        // single variable
        EPStatement stmtOne = epService.getEPAdministrator().createEPL("@Name('S0') create variable boolean var2 = true");
        EPStatement stmtTwo = epService.getEPAdministrator().createEPL("@Name('S1') select * from SupportBean(var2)");
        assertStmtsRef("var2", new String[] {"S0", "S1"});
        assertEquals(true, epService.getEPRuntime().getVariableValue("var2"));

        stmtOne.destroy();
        assertStmtsRef("var2", new String[] {"S1"});
        assertEquals(true, epService.getEPRuntime().getVariableValue("var2"));

        stmtTwo.destroy();
        assertStmtsRef("var2", null);
        assertNotFound("var2");
        
        // multiple variable
        EPStatement stmt1 = epService.getEPAdministrator().createEPL("@Name('T0') create variable boolean v1 = true");
        EPStatement stmt2 = epService.getEPAdministrator().createEPL("@Name('T1') create variable long v2 = 1");
        EPStatement stmt3 = epService.getEPAdministrator().createEPL("@Name('T2') create variable string v3 = 'a'");
        EPStatement stmtUseOne = epService.getEPAdministrator().createEPL("@Name('TX') select * from SupportBean(v1, v2=1, v3='a')");
        EPStatement stmtUseTwo = epService.getEPAdministrator().createEPL("@Name('TY') select * from SupportBean(v2=2)");
        EPStatement stmtUseThree = epService.getEPAdministrator().createEPL("@Name('TZ') select * from SupportBean(v3='A', v1)");

        assertStmtsRef("v1", new String[] {"T0", "TX", "TZ"});
        assertStmtsRef("v2", new String[] {"T1", "TX", "TY"});
        assertStmtsRef("v3", new String[] {"T2", "TX", "TZ"});

        stmt2.destroy();
        assertStmtsRef("v2", new String[] {"TX", "TY"});
        
        stmtUseOne.destroy();
        assertStmtsRef("v2", new String[] {"TY"});

        stmtUseTwo.destroy();
        assertStmtsRef("v2", null);
        assertNotFound("v2");

        EPStatement stmt4 = epService.getEPAdministrator().createEPL("@Name('T3') create variable boolean v4 = true");
        EPStatement stmtUseFour = epService.getEPAdministrator().createEPL("@Name('TQ') select * from SupportBean(v4)");

        assertStmtsRef("v4", new String[] {"T3", "TQ"});
        assertEquals(true, epService.getEPRuntime().getVariableValue("v4"));

        stmt1.destroy();
        stmtUseThree.destroy();
        
        assertStmtsRef("v1", null);
        assertNotFound("v1");
        assertEquals("a", epService.getEPRuntime().getVariableValue("v3"));
        assertStmtsRef("v3", new String[] {"T2"});

        stmt3.destroy();
        assertNotFound("v3");

        stmt4.destroy();
        stmtUseFour.destroy();
        assertNotFound("v4");

        assertEquals(1, epService.getEPRuntime().getVariableValueAll().size());
    }

    public void testConfigAPI()
    {
        EPStatement stmtOne = epService.getEPAdministrator().createEPL("@Name('S0') create variable boolean var2 = true");
        EPStatement stmtTwo = epService.getEPAdministrator().createEPL("@Name('S1') select * from SupportBean(var2)");

        try {
            epService.getEPAdministrator().getConfiguration().removeVariable("var2", false);
            fail();
        }
        catch (ConfigurationException ex) {
            assertEquals("Variable 'var2' is in use by one or more statements", ex.getMessage());
        }

        epService.getEPAdministrator().getConfiguration().removeVariable("var2", true);
        
        stmtOne.destroy();
        stmtTwo.destroy();
        assertNotFound("var2");
                
        // try permanent variable
        assertEquals("thevalue", epService.getEPRuntime().getVariableValue("MyPermanentVar"));
        EPStatement stmtThree = epService.getEPAdministrator().createEPL("@Name('S2') select * from SupportBean(MyPermanentVar = 'Y')");
        assertStmtsRef("MyPermanentVar", new String[] {"S2"});
        stmtThree.destroy();
        assertStmtsRef("MyPermanentVar", null);
        assertEquals("thevalue", epService.getEPRuntime().getVariableValue("MyPermanentVar"));
    }

    private void assertStmtsRef(String variableName, String[] stmts) {
        EPAssertionUtil.assertEqualsAnyOrder(stmts, epService.getEPAdministrator().getConfiguration().getVariableNameUsedBy(variableName).toArray());
    }

    private void assertNotFound(String var) {
        try {
            epService.getEPRuntime().getVariableValue(var);
            fail();
        }
        catch (VariableNotFoundException ex) {
            // expected
        }
        assertStmtsRef(var, null);
    }
}