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
package com.espertech.esper.regressionlib.suite.event.xml;

import com.espertech.esper.common.client.EventPropertyDescriptor;
import com.espertech.esper.common.client.EventType;
import com.espertech.esper.common.client.scopetest.EPAssertionUtil;
import com.espertech.esper.common.internal.support.SupportEventTypeAssertionUtil;
import com.espertech.esper.regressionlib.framework.RegressionEnvironment;
import com.espertech.esper.regressionlib.framework.RegressionExecution;
import com.espertech.esper.regressionlib.support.util.SupportXML;
import org.w3c.dom.Node;

import static org.junit.Assert.assertNull;

public class EventXMLNoSchemaEventTransposeXPathGetter implements RegressionExecution {

    public void run(RegressionEnvironment env) {

        // note class not a fragment
        env.compileDeploy("@name('s0') insert into MyNestedStream select nested1 from TestXMLSchemaTypeTXG");
        EPAssertionUtil.assertEqualsAnyOrder(new Object[]{
            new EventPropertyDescriptor("nested1", Node.class, null, false, false, false, false, false),
        }, env.statement("s0").getEventType().getPropertyDescriptors());
        SupportEventTypeAssertionUtil.assertConsistency(env.statement("s0").getEventType());

        EventType type = env.runtime().getEventTypeService().getEventTypePreconfigured("TestXMLSchemaTypeTXG");
        SupportEventTypeAssertionUtil.assertConsistency(type);
        assertNull(type.getFragmentType("nested1"));
        assertNull(type.getFragmentType("nested1.nested2"));

        SupportXML.sendDefaultEvent(env.eventService(), "ABC", "TestXMLSchemaTypeTXG");
        SupportEventTypeAssertionUtil.assertConsistency(env.iterator("s0").next());

        env.undeployAll();
    }
}
