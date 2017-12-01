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
import com.espertech.esper.supportregression.execution.RegressionExecution;
import com.espertech.esper.support.SupportEventTypeAssertionUtil;

import static org.junit.Assert.assertEquals;

public class ExecEventXMLSchemaEventTypes implements RegressionExecution {
    private final static String CLASSLOADER_SCHEMA_URI = "regression/typeTestSchema.xsd";

    public void configure(Configuration configuration) throws Exception {
        ConfigurationEventTypeXMLDOM eventTypeMeta = new ConfigurationEventTypeXMLDOM();
        eventTypeMeta.setRootElementName("typesEvent");
        String schemaUri = ExecEventXMLSchemaInvalid.class.getClassLoader().getResource(CLASSLOADER_SCHEMA_URI).toString();
        eventTypeMeta.setSchemaResource(schemaUri);
        configuration.addEventType("TestTypesEvent", eventTypeMeta);
    }

    public void run(EPServiceProvider epService) throws Exception {
        String stmtSelectWild = "select * from TestTypesEvent";
        EPStatement wildStmt = epService.getEPAdministrator().createEPL(stmtSelectWild);
        EventType type = wildStmt.getEventType();
        SupportEventTypeAssertionUtil.assertConsistency(type);

        Object[][] types = new Object[][]{
                {"attrNonPositiveInteger", Integer.class},
                {"attrNonNegativeInteger", Integer.class},
                {"attrNegativeInteger", Integer.class},
                {"attrPositiveInteger", Integer.class},
                {"attrLong", Long.class},
                {"attrUnsignedLong", Long.class},
                {"attrInt", Integer.class},
                {"attrUnsignedInt", Integer.class},
                {"attrDecimal", Double.class},
                {"attrInteger", Integer.class},
                {"attrFloat", Float.class},
                {"attrDouble", Double.class},
                {"attrString", String.class},
                {"attrShort", Short.class},
                {"attrUnsignedShort", Short.class},
                {"attrByte", Byte.class},
                {"attrUnsignedByte", Byte.class},
                {"attrBoolean", Boolean.class},
                {"attrDateTime", String.class},
                {"attrDate", String.class},
                {"attrTime", String.class}};

        for (int i = 0; i < types.length; i++) {
            String name = types[i][0].toString();
            EventPropertyDescriptor desc = type.getPropertyDescriptor(name);
            Class expected = (Class) types[i][1];
            assertEquals("Failed for " + name, expected, desc.getPropertyType());
        }
    }
}
