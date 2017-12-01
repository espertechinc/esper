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
import com.espertech.esper.supportregression.event.SupportXML;
import com.espertech.esper.supportregression.execution.RegressionExecution;
import com.espertech.esper.support.SupportEventTypeAssertionUtil;
import org.w3c.dom.Node;

import static org.junit.Assert.assertEquals;

public class ExecEventXMLSchemaEventTransposeNodeArray implements RegressionExecution {
    private final static String CLASSLOADER_SCHEMA_URI = "regression/simpleSchema.xsd";

    public void run(EPServiceProvider epService) throws Exception {
        ConfigurationEventTypeXMLDOM eventTypeMeta = new ConfigurationEventTypeXMLDOM();
        eventTypeMeta.setRootElementName("simpleEvent");
        String schemaUri = ExecEventXMLSchemaEventTransposeNodeArray.class.getClassLoader().getResource(CLASSLOADER_SCHEMA_URI).toString();
        eventTypeMeta.setSchemaResource(schemaUri);
        epService.getEPAdministrator().getConfiguration().addEventType("TestXMLSchemaType", eventTypeMeta);

        // try array property insert
        EPStatement stmtInsert = epService.getEPAdministrator().createEPL("select nested3.nested4 as narr from TestXMLSchemaType#lastevent");
        EPAssertionUtil.assertEqualsAnyOrder(new Object[]{
            new EventPropertyDescriptor("narr", Node[].class, Node.class, false, false, true, false, true),
        }, stmtInsert.getEventType().getPropertyDescriptors());
        SupportEventTypeAssertionUtil.assertConsistency(stmtInsert.getEventType());

        SupportXML.sendDefaultEvent(epService.getEPRuntime(), "test");

        EventBean result = stmtInsert.iterator().next();
        SupportEventTypeAssertionUtil.assertConsistency(result);
        EventBean[] fragments = (EventBean[]) result.getFragment("narr");
        assertEquals(3, fragments.length);
        assertEquals("SAMPLE_V8", fragments[0].get("prop5[1]"));
        assertEquals("SAMPLE_V11", fragments[2].get("prop5[1]"));

        EventBean fragmentItem = (EventBean) result.getFragment("narr[2]");
        assertEquals("TestXMLSchemaType.nested3.nested4", fragmentItem.getEventType().getName());
        assertEquals("SAMPLE_V10", fragmentItem.get("prop5[0]"));

        // try array index property insert
        EPStatement stmtInsertItem = epService.getEPAdministrator().createEPL("select nested3.nested4[1] as narr from TestXMLSchemaType#lastevent");
        EPAssertionUtil.assertEqualsAnyOrder(new Object[]{
            new EventPropertyDescriptor("narr", Node.class, null, false, false, false, false, true),
        }, stmtInsertItem.getEventType().getPropertyDescriptors());
        SupportEventTypeAssertionUtil.assertConsistency(stmtInsertItem.getEventType());

        SupportXML.sendDefaultEvent(epService.getEPRuntime(), "test");

        EventBean resultItem = stmtInsertItem.iterator().next();
        assertEquals("b", resultItem.get("narr.id"));
        SupportEventTypeAssertionUtil.assertConsistency(resultItem);
        EventBean fragmentsInsertItem = (EventBean) resultItem.getFragment("narr");
        SupportEventTypeAssertionUtil.assertConsistency(fragmentsInsertItem);
        assertEquals("b", fragmentsInsertItem.get("id"));
        assertEquals("SAMPLE_V9", fragmentsInsertItem.get("prop5[0]"));
    }
}
