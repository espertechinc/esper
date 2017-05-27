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
package com.espertech.esper.regression.event.xml;

import com.espertech.esper.client.*;
import com.espertech.esper.client.scopetest.EPAssertionUtil;
import com.espertech.esper.supportregression.execution.RegressionExecution;

import static com.espertech.esper.regression.event.xml.ExecEventXMLSchemaXPathBacked.getConfigTestType;
import static junit.framework.TestCase.*;

public class ExecEventXMLSchemaAddRemoveType implements RegressionExecution {
    public void configure(Configuration configuration) throws Exception {
        configuration.addEventType("TestXMLSchemaType", getConfigTestType(null, false));
    }

    public void run(EPServiceProvider epService) throws Exception {
        ConfigurationOperations configOps = epService.getEPAdministrator().getConfiguration();

        // test remove type with statement used (no force)
        configOps.addEventType("MyXMLEvent", getConfigTestType("p01", false));
        EPStatement stmt = epService.getEPAdministrator().createEPL("select p01 from MyXMLEvent", "stmtOne");
        EPAssertionUtil.assertEqualsExactOrder(configOps.getEventTypeNameUsedBy("MyXMLEvent").toArray(), new String[]{"stmtOne"});

        try {
            configOps.removeEventType("MyXMLEvent", false);
        } catch (ConfigurationException ex) {
            assertTrue(ex.getMessage().contains("MyXMLEvent"));
        }

        // destroy statement and type
        stmt.destroy();
        assertTrue(configOps.getEventTypeNameUsedBy("MyXMLEvent").isEmpty());
        assertTrue(configOps.isEventTypeExists("MyXMLEvent"));
        assertTrue(configOps.removeEventType("MyXMLEvent", false));
        assertFalse(configOps.removeEventType("MyXMLEvent", false));    // try double-remove
        assertFalse(configOps.isEventTypeExists("MyXMLEvent"));
        try {
            epService.getEPAdministrator().createEPL("select p01 from MyXMLEvent");
            fail();
        } catch (EPException ex) {
            // expected
        }

        // add back the type
        configOps.addEventType("MyXMLEvent", getConfigTestType("p20", false));
        assertTrue(configOps.isEventTypeExists("MyXMLEvent"));
        assertTrue(configOps.getEventTypeNameUsedBy("MyXMLEvent").isEmpty());

        // compile
        epService.getEPAdministrator().createEPL("select p20 from MyXMLEvent", "stmtTwo");
        EPAssertionUtil.assertEqualsExactOrder(configOps.getEventTypeNameUsedBy("MyXMLEvent").toArray(), new String[]{"stmtTwo"});
        try {
            epService.getEPAdministrator().createEPL("select p01 from MyXMLEvent");
            fail();
        } catch (EPException ex) {
            // expected
        }

        // remove with force
        try {
            configOps.removeEventType("MyXMLEvent", false);
        } catch (ConfigurationException ex) {
            assertTrue(ex.getMessage().contains("MyXMLEvent"));
        }
        assertTrue(configOps.removeEventType("MyXMLEvent", true));
        assertFalse(configOps.isEventTypeExists("MyXMLEvent"));
        assertTrue(configOps.getEventTypeNameUsedBy("MyXMLEvent").isEmpty());

        // add back the type
        configOps.addEventType("MyXMLEvent", getConfigTestType("p03", false));
        assertTrue(configOps.isEventTypeExists("MyXMLEvent"));

        // compile
        epService.getEPAdministrator().createEPL("select p03 from MyXMLEvent");
        try {
            epService.getEPAdministrator().createEPL("select p20 from MyXMLEvent");
            fail();
        } catch (EPException ex) {
            // expected
        }
    }
}
