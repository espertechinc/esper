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
import com.espertech.esper.client.scopetest.SupportUpdateListener;
import com.espertech.esper.supportregression.event.SupportXML;
import com.espertech.esper.supportregression.execution.RegressionExecution;
import com.espertech.esper.support.SupportEventTypeAssertionUtil;
import org.w3c.dom.Document;
import org.w3c.dom.Node;

import javax.xml.xpath.XPathConstants;

import static org.junit.Assert.*;

public class ExecEventXMLSchemaXPathBacked implements RegressionExecution {
    public static final String CLASSLOADER_SCHEMA_URI = "regression/simpleSchema.xsd";

    public void configure(Configuration configuration) throws Exception {
        configuration.addEventType("TestXMLSchemaType", getConfigTestType(null, true));
    }

    protected static ConfigurationEventTypeXMLDOM getConfigTestType(String additionalXPathProperty, boolean isUseXPathPropertyExpression) {
        ConfigurationEventTypeXMLDOM eventTypeMeta = new ConfigurationEventTypeXMLDOM();
        eventTypeMeta.setRootElementName("simpleEvent");
        String schemaUri = ExecEventXMLSchemaXPathBacked.class.getClassLoader().getResource(CLASSLOADER_SCHEMA_URI).toString();
        eventTypeMeta.setSchemaResource(schemaUri);
        eventTypeMeta.addNamespacePrefix("ss", "samples:schemas:simpleSchema");
        eventTypeMeta.addXPathProperty("customProp", "count(/ss:simpleEvent/ss:nested3/ss:nested4)", XPathConstants.NUMBER);
        eventTypeMeta.setXPathPropertyExpr(isUseXPathPropertyExpression);
        if (additionalXPathProperty != null) {
            eventTypeMeta.addXPathProperty(additionalXPathProperty, "count(/ss:simpleEvent/ss:nested3/ss:nested4)", XPathConstants.NUMBER);
        }
        return eventTypeMeta;
    }

    public void run(EPServiceProvider epService) throws Exception {
        runAssertion(epService, true);
    }

    protected static void runAssertion(EPServiceProvider epService, boolean xpath) throws Exception {

        SupportUpdateListener updateListener = new SupportUpdateListener();

        String stmtSelectWild = "select * from TestXMLSchemaType";
        EPStatement wildStmt = epService.getEPAdministrator().createEPL(stmtSelectWild);
        EventType type = wildStmt.getEventType();
        SupportEventTypeAssertionUtil.assertConsistency(type);

        EPAssertionUtil.assertEqualsAnyOrder(new Object[]{
            new EventPropertyDescriptor("nested1", Node.class, null, false, false, false, false, !xpath),
            new EventPropertyDescriptor("prop4", String.class, null, false, false, false, false, false),
            new EventPropertyDescriptor("nested3", Node.class, null, false, false, false, false, !xpath),
            new EventPropertyDescriptor("customProp", Double.class, null, false, false, false, false, false),
        }, type.getPropertyDescriptors());

        String stmt =
                "select nested1 as nodeProp," +
                        "prop4 as nested1Prop," +
                        "nested1.prop2 as nested2Prop," +
                        "nested3.nested4('a').prop5[1] as complexProp," +
                        "nested1.nested2.prop3[2] as indexedProp," +
                        "customProp," +
                        "prop4.attr2 as attrOneProp," +
                        "nested3.nested4[2].id as attrTwoProp" +
                        " from TestXMLSchemaType#length(100)";

        EPStatement selectStmt = epService.getEPAdministrator().createEPL(stmt);
        selectStmt.addListener(updateListener);
        type = selectStmt.getEventType();
        SupportEventTypeAssertionUtil.assertConsistency(type);
        EPAssertionUtil.assertEqualsAnyOrder(new Object[]{
            new EventPropertyDescriptor("nodeProp", Node.class, null, false, false, false, false, !xpath),
            new EventPropertyDescriptor("nested1Prop", String.class, null, false, false, false, false, false),
            new EventPropertyDescriptor("nested2Prop", Boolean.class, null, false, false, false, false, false),
            new EventPropertyDescriptor("complexProp", String.class, null, false, false, false, false, false),
            new EventPropertyDescriptor("indexedProp", Integer.class, null, false, false, false, false, false),
            new EventPropertyDescriptor("customProp", Double.class, null, false, false, false, false, false),
            new EventPropertyDescriptor("attrOneProp", Boolean.class, null, false, false, false, false, false),
            new EventPropertyDescriptor("attrTwoProp", String.class, null, false, false, false, false, false),
        }, type.getPropertyDescriptors());

        Document eventDoc = SupportXML.sendDefaultEvent(epService.getEPRuntime(), "test");

        assertNotNull(updateListener.getLastNewData());
        EventBean theEvent = updateListener.getLastNewData()[0];

        assertSame(eventDoc.getDocumentElement().getChildNodes().item(1), theEvent.get("nodeProp"));
        assertEquals("SAMPLE_V6", theEvent.get("nested1Prop"));
        assertEquals(true, theEvent.get("nested2Prop"));
        assertEquals("SAMPLE_V8", theEvent.get("complexProp"));
        assertEquals(5, theEvent.get("indexedProp"));
        assertEquals(3.0, theEvent.get("customProp"));
        assertEquals(true, theEvent.get("attrOneProp"));
        assertEquals("c", theEvent.get("attrTwoProp"));

        /**
         * Comment-in for performance testing
         long start = System.nanoTime();
         for (int i = 0; i < 1000; i++)
         {
         sendEvent("test");
         }
         long end = System.nanoTime();
         double delta = (end - start) / 1000d / 1000d / 1000d;
         System.out.println(delta);
         */
    }
}
