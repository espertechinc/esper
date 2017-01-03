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

package com.espertech.esper.regression.event;

import com.espertech.esper.client.*;
import com.espertech.esper.metrics.instrumentation.InstrumentationHelper;
import com.espertech.esper.supportregression.client.SupportConfigFactory;
import com.espertech.esper.supportregression.event.EventTypeAssertionUtil;
import junit.framework.TestCase;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class TestXMLSchemaEventTypes extends TestCase
{
    private static String CLASSLOADER_SCHEMA_URI = "regression/typeTestSchema.xsd";

    private EPServiceProvider epService;

    public void testSchemaXMLTypes() throws Exception
    {
        Configuration configuration = SupportConfigFactory.getConfiguration();
        ConfigurationEventTypeXMLDOM eventTypeMeta = new ConfigurationEventTypeXMLDOM();
        eventTypeMeta.setRootElementName("typesEvent");
        String schemaUri = TestXMLSchemaEvent.class.getClassLoader().getResource(CLASSLOADER_SCHEMA_URI).toString();
        eventTypeMeta.setSchemaResource(schemaUri);
        configuration.addEventType("TestTypesEvent", eventTypeMeta);

        epService = EPServiceProviderManager.getDefaultProvider(configuration);
        epService.initialize();
        if (InstrumentationHelper.ENABLED) { InstrumentationHelper.startTest(epService, this.getClass(), getName());}

        String stmtSelectWild = "select * from TestTypesEvent";
        EPStatement wildStmt = epService.getEPAdministrator().createEPL(stmtSelectWild);
        EventType type = wildStmt.getEventType();
        EventTypeAssertionUtil.assertConsistency(type);

        Object[][] types = new Object[][] {
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
        
        for (int i = 0; i < types.length; i++)
        {
            String name = types[i][0].toString();
            EventPropertyDescriptor desc = type.getPropertyDescriptor(name);
            Class expected = (Class) types[i][1];
            assertEquals("Failed for " + name, expected, desc.getPropertyType());
        }

        if (InstrumentationHelper.ENABLED) { InstrumentationHelper.endTest();}
    }

    private static final Logger log = LoggerFactory.getLogger(TestXMLSchemaEvent.class);
}
