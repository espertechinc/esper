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

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

public class ExecEventXMLNoSchemaEventTransposeDOM implements RegressionExecution {
    public void configure(Configuration configuration) throws Exception {
        configuration.getEngineDefaults().getViewResources().setIterableUnbound(true);
    }

    public void run(EPServiceProvider epService) throws Exception {
        ConfigurationEventTypeXMLDOM eventTypeMeta = new ConfigurationEventTypeXMLDOM();
        eventTypeMeta.setRootElementName("simpleEvent");
        // eventTypeMeta.setXPathPropertyExpr(false); <== the default
        epService.getEPAdministrator().getConfiguration().addEventType("TestXMLSchemaType", eventTypeMeta);

        EPStatement stmtInsert = epService.getEPAdministrator().createEPL("insert into MyNestedStream select nested1 from TestXMLSchemaType");
        EPAssertionUtil.assertEqualsAnyOrder(new Object[]{
            new EventPropertyDescriptor("nested1", String.class, null, false, false, false, false, false),
        }, stmtInsert.getEventType().getPropertyDescriptors());
        SupportEventTypeAssertionUtil.assertConsistency(stmtInsert.getEventType());

        EPStatement stmtSelectWildcard = epService.getEPAdministrator().createEPL("select * from TestXMLSchemaType");
        EPAssertionUtil.assertEqualsAnyOrder(new Object[0], stmtSelectWildcard.getEventType().getPropertyDescriptors());
        SupportEventTypeAssertionUtil.assertConsistency(stmtSelectWildcard.getEventType());

        SupportXML.sendDefaultEvent(epService.getEPRuntime(), "test");
        EventBean stmtInsertWildcardBean = stmtInsert.iterator().next();
        EventBean stmtSelectWildcardBean = stmtSelectWildcard.iterator().next();
        assertNotNull(stmtInsertWildcardBean.get("nested1"));
        SupportEventTypeAssertionUtil.assertConsistency(stmtSelectWildcardBean);
        SupportEventTypeAssertionUtil.assertConsistency(stmtInsert.iterator().next());

        assertEquals(0, stmtSelectWildcardBean.getEventType().getPropertyNames().length);
    }
}
