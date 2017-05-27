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
package com.espertech.esper.regression.event.bean;

import com.espertech.esper.client.*;
import com.espertech.esper.client.scopetest.EPAssertionUtil;
import com.espertech.esper.supportregression.bean.SupportBean;
import com.espertech.esper.supportregression.bean.SupportBean_A;
import com.espertech.esper.supportregression.bean.SupportMarketDataBean;
import com.espertech.esper.supportregression.execution.RegressionExecution;

import static junit.framework.TestCase.*;

public class ExecEventBeanAddRemoveType implements RegressionExecution {
    public void run(EPServiceProvider epService) throws Exception {
        ConfigurationOperations configOps = epService.getEPAdministrator().getConfiguration();

        // test remove type with statement used (no force)
        configOps.addEventType("MyBeanEvent", SupportBean_A.class);
        EPStatement stmt = epService.getEPAdministrator().createEPL("select id from MyBeanEvent", "stmtOne");
        EPAssertionUtil.assertEqualsExactOrder(configOps.getEventTypeNameUsedBy("MyBeanEvent").toArray(), new String[]{"stmtOne"});

        try {
            configOps.removeEventType("MyBeanEvent", false);
        } catch (ConfigurationException ex) {
            assertTrue(ex.getMessage().contains("MyBeanEvent"));
        }

        // destroy statement and type
        stmt.destroy();
        assertTrue(configOps.getEventTypeNameUsedBy("MyBeanEvent").isEmpty());
        assertTrue(configOps.isEventTypeExists("MyBeanEvent"));
        assertTrue(configOps.removeEventType("MyBeanEvent", false));
        assertFalse(configOps.removeEventType("MyBeanEvent", false));    // try double-remove
        assertFalse(configOps.isEventTypeExists("MyBeanEvent"));
        try {
            epService.getEPAdministrator().createEPL("select id from MyBeanEvent");
            fail();
        } catch (EPException ex) {
            // expected
        }

        // add back the type
        configOps.addEventType("MyBeanEvent", SupportBean.class);
        assertTrue(configOps.isEventTypeExists("MyBeanEvent"));
        assertTrue(configOps.getEventTypeNameUsedBy("MyBeanEvent").isEmpty());

        // compile
        epService.getEPAdministrator().createEPL("select boolPrimitive from MyBeanEvent", "stmtTwo");
        EPAssertionUtil.assertEqualsExactOrder(configOps.getEventTypeNameUsedBy("MyBeanEvent").toArray(), new String[]{"stmtTwo"});
        try {
            epService.getEPAdministrator().createEPL("select id from MyBeanEvent");
            fail();
        } catch (EPException ex) {
            // expected
        }

        // remove with force
        try {
            configOps.removeEventType("MyBeanEvent", false);
        } catch (ConfigurationException ex) {
            assertTrue(ex.getMessage().contains("MyBeanEvent"));
        }
        assertTrue(configOps.removeEventType("MyBeanEvent", true));
        assertFalse(configOps.isEventTypeExists("MyBeanEvent"));
        assertTrue(configOps.getEventTypeNameUsedBy("MyBeanEvent").isEmpty());

        // add back the type
        configOps.addEventType("MyBeanEvent", SupportMarketDataBean.class);
        assertTrue(configOps.isEventTypeExists("MyBeanEvent"));

        // compile
        epService.getEPAdministrator().createEPL("select feed from MyBeanEvent");
        try {
            epService.getEPAdministrator().createEPL("select boolPrimitive from MyBeanEvent");
            fail();
        } catch (EPException ex) {
            // expected
        }
    }
}
