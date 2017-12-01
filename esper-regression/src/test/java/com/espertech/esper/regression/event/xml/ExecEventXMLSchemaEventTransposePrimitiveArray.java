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

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;

public class ExecEventXMLSchemaEventTransposePrimitiveArray implements RegressionExecution {
    private final static String CLASSLOADER_SCHEMA_URI = "regression/simpleSchema.xsd";

    public void run(EPServiceProvider epService) throws Exception {
        String schemaURI = ExecEventXMLSchemaEventTransposePrimitiveArray.class.getClassLoader().getResource(CLASSLOADER_SCHEMA_URI).toString();
        ConfigurationEventTypeXMLDOM eventTypeMeta = new ConfigurationEventTypeXMLDOM();
        eventTypeMeta.setRootElementName("simpleEvent");
        eventTypeMeta.setSchemaResource(schemaURI);
        epService.getEPAdministrator().getConfiguration().addEventType("ABCType", eventTypeMeta);

        eventTypeMeta = new ConfigurationEventTypeXMLDOM();
        eventTypeMeta.setRootElementName("//nested2");
        eventTypeMeta.setSchemaResource(schemaURI);
        eventTypeMeta.setEventSenderValidatesRoot(false);
        epService.getEPAdministrator().getConfiguration().addEventType("TestNested2", eventTypeMeta);

        // try array property in select
        EPStatement stmtInsert = epService.getEPAdministrator().createEPL("select * from TestNested2#lastevent");
        SupportUpdateListener listener = new SupportUpdateListener();
        stmtInsert.addListener(listener);
        EPAssertionUtil.assertEqualsAnyOrder(new Object[]{
            new EventPropertyDescriptor("prop3", Integer[].class, null, false, false, true, false, false),
        }, stmtInsert.getEventType().getPropertyDescriptors());
        SupportEventTypeAssertionUtil.assertConsistency(stmtInsert.getEventType());

        SupportXML.sendDefaultEvent(epService.getEPRuntime(), "test");
        assertFalse(listener.isInvoked());

        EventSender sender = epService.getEPRuntime().getEventSender("TestNested2");
        sender.sendEvent(SupportXML.getDocument("<nested2><prop3>2</prop3><prop3></prop3><prop3>4</prop3></nested2>"));
        EventBean theEvent = stmtInsert.iterator().next();
        EPAssertionUtil.assertEqualsExactOrder((Integer[]) theEvent.get("prop3"), new Object[]{2, null, 4});
        SupportEventTypeAssertionUtil.assertConsistency(theEvent);

        // try array property nested
        EPStatement stmtSelect = epService.getEPAdministrator().createEPL("select nested3.* from ABCType#lastevent");
        SupportXML.sendDefaultEvent(epService.getEPRuntime(), "test");
        EventBean stmtSelectResult = stmtSelect.iterator().next();
        SupportEventTypeAssertionUtil.assertConsistency(stmtSelectResult);
        assertEquals(String[].class, stmtSelectResult.getEventType().getPropertyType("nested4[2].prop5"));
        assertEquals("SAMPLE_V8", stmtSelectResult.get("nested4[0].prop5[1]"));
        EPAssertionUtil.assertEqualsExactOrder((String[]) stmtSelectResult.get("nested4[2].prop5"), new Object[]{"SAMPLE_V10", "SAMPLE_V11"});

        EventBean fragmentNested4 = (EventBean) stmtSelectResult.getFragment("nested4[2]");
        EPAssertionUtil.assertEqualsExactOrder((String[]) fragmentNested4.get("prop5"), new Object[]{"SAMPLE_V10", "SAMPLE_V11"});
        assertEquals("SAMPLE_V11", fragmentNested4.get("prop5[1]"));
        SupportEventTypeAssertionUtil.assertConsistency(fragmentNested4);

        epService.getEPAdministrator().destroyAllStatements();
    }
}
