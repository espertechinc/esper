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

public class ExecEventXMLSchemaEventTransposeDOMGetter implements RegressionExecution {
    private final static String CLASSLOADER_SCHEMA_URI = "regression/simpleSchema.xsd";

    public void run(EPServiceProvider epService) throws Exception {
        ConfigurationEventTypeXMLDOM eventTypeMeta = new ConfigurationEventTypeXMLDOM();
        eventTypeMeta.setRootElementName("simpleEvent");
        String schemaUri = ExecEventXMLSchemaEventTransposeDOMGetter.class.getClassLoader().getResource(CLASSLOADER_SCHEMA_URI).toString();
        eventTypeMeta.setSchemaResource(schemaUri);
        // eventTypeMeta.setXPathPropertyExpr(false); <== the default
        epService.getEPAdministrator().getConfiguration().addEventType("TestXMLSchemaType", eventTypeMeta);

        EPStatement stmtInsert = epService.getEPAdministrator().createEPL("insert into MyNestedStream select nested1 from TestXMLSchemaType#lastevent");
        EPAssertionUtil.assertEqualsAnyOrder(new Object[]{
            new EventPropertyDescriptor("nested1", Node.class, null, false, false, false, false, true),
        }, stmtInsert.getEventType().getPropertyDescriptors());
        SupportEventTypeAssertionUtil.assertConsistency(stmtInsert.getEventType());

        EPStatement stmtSelect = epService.getEPAdministrator().createEPL("select nested1.attr1 as attr1, nested1.prop1 as prop1, nested1.prop2 as prop2, nested1.nested2.prop3 as prop3, nested1.nested2.prop3[0] as prop3_0, nested1.nested2 as nested2 from MyNestedStream#lastevent");
        EPAssertionUtil.assertEqualsAnyOrder(new Object[]{
            new EventPropertyDescriptor("prop1", String.class, null, false, false, false, false, false),
            new EventPropertyDescriptor("prop2", Boolean.class, null, false, false, false, false, false),
            new EventPropertyDescriptor("attr1", String.class, null, false, false, false, false, false),
            new EventPropertyDescriptor("prop3", Integer[].class, Integer.class, false, false, true, false, false),
            new EventPropertyDescriptor("prop3_0", Integer.class, null, false, false, false, false, false),
            new EventPropertyDescriptor("nested2", Node.class, null, false, false, false, false, true),
        }, stmtSelect.getEventType().getPropertyDescriptors());
        SupportEventTypeAssertionUtil.assertConsistency(stmtSelect.getEventType());

        EPStatement stmtSelectWildcard = epService.getEPAdministrator().createEPL("select * from MyNestedStream");
        EPAssertionUtil.assertEqualsAnyOrder(new Object[]{
            new EventPropertyDescriptor("nested1", Node.class, null, false, false, false, false, true),
        }, stmtSelectWildcard.getEventType().getPropertyDescriptors());
        SupportEventTypeAssertionUtil.assertConsistency(stmtSelectWildcard.getEventType());

        EPStatement stmtInsertWildcard = epService.getEPAdministrator().createEPL("insert into MyNestedStreamTwo select nested1.* from TestXMLSchemaType#lastevent");
        EPAssertionUtil.assertEqualsAnyOrder(new Object[]{
            new EventPropertyDescriptor("prop1", String.class, null, false, false, false, false, false),
            new EventPropertyDescriptor("prop2", Boolean.class, null, false, false, false, false, false),
            new EventPropertyDescriptor("attr1", String.class, null, false, false, false, false, false),
            new EventPropertyDescriptor("nested2", Node.class, null, false, false, false, false, true),
        }, stmtInsertWildcard.getEventType().getPropertyDescriptors());
        SupportEventTypeAssertionUtil.assertConsistency(stmtInsertWildcard.getEventType());

        SupportXML.sendDefaultEvent(epService.getEPRuntime(), "test");
        EventBean stmtInsertWildcardBean = stmtInsertWildcard.iterator().next();
        EPAssertionUtil.assertProps(stmtInsertWildcardBean, "prop1,prop2,attr1".split(","),
            new Object[]{"SAMPLE_V1", true, "SAMPLE_ATTR1"});

        SupportEventTypeAssertionUtil.assertConsistency(stmtSelect.iterator().next());
        EventBean stmtInsertBean = stmtInsert.iterator().next();
        SupportEventTypeAssertionUtil.assertConsistency(stmtInsertWildcard.iterator().next());
        SupportEventTypeAssertionUtil.assertConsistency(stmtInsert.iterator().next());

        EventBean fragmentNested1 = (EventBean) stmtInsertBean.getFragment("nested1");
        assertEquals(5, fragmentNested1.get("nested2.prop3[2]"));
        assertEquals("TestXMLSchemaType.nested1", fragmentNested1.getEventType().getName());

        EventBean fragmentNested2 = (EventBean) stmtInsertWildcardBean.getFragment("nested2");
        assertEquals(4, fragmentNested2.get("prop3[1]"));
        assertEquals("TestXMLSchemaType.nested1.nested2", fragmentNested2.getEventType().getName());
    }
}
