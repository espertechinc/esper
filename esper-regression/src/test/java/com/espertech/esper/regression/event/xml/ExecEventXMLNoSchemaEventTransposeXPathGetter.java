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
import com.espertech.esper.core.service.EPServiceProviderSPI;
import com.espertech.esper.supportregression.event.SupportXML;
import com.espertech.esper.supportregression.execution.RegressionExecution;
import com.espertech.esper.support.SupportEventTypeAssertionUtil;
import org.w3c.dom.Node;

import static org.junit.Assert.assertNull;

public class ExecEventXMLNoSchemaEventTransposeXPathGetter implements RegressionExecution {
    private final static String CLASSLOADER_SCHEMA_URI = "regression/simpleSchema.xsd";

    public void configure(Configuration configuration) throws Exception {
        configuration.getEngineDefaults().getViewResources().setIterableUnbound(true);
    }

    public void run(EPServiceProvider epService) throws Exception {
        ConfigurationEventTypeXMLDOM eventTypeMeta = new ConfigurationEventTypeXMLDOM();
        eventTypeMeta.setRootElementName("simpleEvent");
        String schemaUri = ExecEventXMLNoSchemaEventTransposeXPathGetter.class.getClassLoader().getResource(CLASSLOADER_SCHEMA_URI).toString();
        eventTypeMeta.setSchemaResource(schemaUri);
        eventTypeMeta.setXPathPropertyExpr(true);       // <== note this
        eventTypeMeta.addNamespacePrefix("ss", "samples:schemas:simpleSchema");
        epService.getEPAdministrator().getConfiguration().addEventType("TestXMLSchemaType", eventTypeMeta);

        // note class not a fragment
        EPStatement stmtInsert = epService.getEPAdministrator().createEPL("insert into MyNestedStream select nested1 from TestXMLSchemaType");
        EPAssertionUtil.assertEqualsAnyOrder(new Object[]{
            new EventPropertyDescriptor("nested1", Node.class, null, false, false, false, false, false),
        }, stmtInsert.getEventType().getPropertyDescriptors());
        SupportEventTypeAssertionUtil.assertConsistency(stmtInsert.getEventType());

        EventType type = ((EPServiceProviderSPI) epService).getEventAdapterService().getExistsTypeByName("TestXMLSchemaType");
        SupportEventTypeAssertionUtil.assertConsistency(type);
        assertNull(type.getFragmentType("nested1"));
        assertNull(type.getFragmentType("nested1.nested2"));

        SupportXML.sendDefaultEvent(epService.getEPRuntime(), "ABC");
        SupportEventTypeAssertionUtil.assertConsistency(stmtInsert.iterator().next());
    }
}
