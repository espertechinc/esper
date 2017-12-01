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
import com.espertech.esper.supportregression.client.SupportConfigFactory;
import com.espertech.esper.supportregression.execution.RegressionExecution;
import com.espertech.esper.support.SupportEventTypeAssertionUtil;
import org.w3c.dom.Node;

import javax.xml.xpath.XPathConstants;

public class ExecEventXMLSchemaEventReplace implements RegressionExecution {
    public static final String CLASSLOADER_SCHEMA_URI = "regression/simpleSchema.xsd";
    public static final String CLASSLOADER_SCHEMA_VERSION2_URI = "regression/simpleSchema_version2.xsd";

    private ConfigurationEventTypeXMLDOM eventTypeMeta;

    public void configure(Configuration configuration) throws Exception {
        eventTypeMeta = new ConfigurationEventTypeXMLDOM();
        eventTypeMeta.setRootElementName("simpleEvent");
        String schemaUri = ExecEventXMLSchemaEventReplace.class.getClassLoader().getResource(CLASSLOADER_SCHEMA_URI).toString();
        eventTypeMeta.setSchemaResource(schemaUri);
        eventTypeMeta.addNamespacePrefix("ss", "samples:schemas:simpleSchema");
        eventTypeMeta.addXPathProperty("customProp", "count(/ss:simpleEvent/ss:nested3/ss:nested4)", XPathConstants.NUMBER);
        configuration.addEventType("TestXMLSchemaType", eventTypeMeta);
    }

    public void run(EPServiceProvider epService) throws Exception {
        if (SupportConfigFactory.skipTest(ExecEventXMLSchemaEventReplace.class)) {
            return;
        }

        String stmtSelectWild = "select * from TestXMLSchemaType";
        EPStatement wildStmt = epService.getEPAdministrator().createEPL(stmtSelectWild);
        EventType type = wildStmt.getEventType();
        SupportEventTypeAssertionUtil.assertConsistency(type);

        EPAssertionUtil.assertEqualsAnyOrder(new Object[]{
            new EventPropertyDescriptor("nested1", Node.class, null, false, false, false, false, true),
            new EventPropertyDescriptor("prop4", String.class, null, false, false, false, false, false),
            new EventPropertyDescriptor("nested3", Node.class, null, false, false, false, false, true),
            new EventPropertyDescriptor("customProp", Double.class, null, false, false, false, false, false),
        }, type.getPropertyDescriptors());

        // update type and replace
        String schemaUri = ExecEventXMLSchemaEventReplace.class.getClassLoader().getResource(CLASSLOADER_SCHEMA_VERSION2_URI).toString();
        eventTypeMeta.setSchemaResource(schemaUri);
        eventTypeMeta.addXPathProperty("countProp", "count(/ss:simpleEvent/ss:nested3/ss:nested4)", XPathConstants.NUMBER);
        epService.getEPAdministrator().getConfiguration().replaceXMLEventType("TestXMLSchemaType", eventTypeMeta);

        wildStmt = epService.getEPAdministrator().createEPL(stmtSelectWild);
        type = wildStmt.getEventType();
        SupportEventTypeAssertionUtil.assertConsistency(type);

        EPAssertionUtil.assertEqualsAnyOrder(new Object[]{
            new EventPropertyDescriptor("nested1", Node.class, null, false, false, false, false, true),
            new EventPropertyDescriptor("prop4", String.class, null, false, false, false, false, false),
            new EventPropertyDescriptor("prop5", String.class, null, false, false, false, false, false),
            new EventPropertyDescriptor("nested3", Node.class, null, false, false, false, false, true),
            new EventPropertyDescriptor("customProp", Double.class, null, false, false, false, false, false),
            new EventPropertyDescriptor("countProp", Double.class, null, false, false, false, false, false),
        }, type.getPropertyDescriptors());
    }
}
