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

import com.espertech.esper.common.client.EventBean;
import com.espertech.esper.common.client.EventPropertyDescriptor;
import com.espertech.esper.common.client.EventType;
import com.espertech.esper.common.client.scopetest.EPAssertionUtil;
import com.espertech.esper.regressionlib.framework.RegressionEnvironment;
import com.espertech.esper.regressionlib.framework.RegressionExecution;
import com.espertech.esper.regressionlib.support.util.SupportXML;
import org.w3c.dom.Node;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;

public class EventXMLSchemaWithAll implements RegressionExecution {

    public void run(RegressionEnvironment env) {
        // url='page4'
        String text = "@name('s0') select a.url as sesja from pattern [ every a=PageVisitEvent(url='page1') ]";
        env.compileDeploy(text).addListener("s0");

        SupportXML.sendXMLEvent(env,
            "<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n" +
                "<event-page-visit xmlns=\"samples:schemas:simpleSchemaWithAll\" xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\" xsi:schemaLocation=\"samples:schemas:simpleSchemaWithAll simpleSchemaWithAll.xsd\">\n" +
                "<url>page1</url>" +
                "</event-page-visit>", "PageVisitEvent");
        EventBean theEvent = env.listener("s0").getLastNewData()[0];
        assertEquals("page1", theEvent.get("sesja"));
        env.listener("s0").reset();

        SupportXML.sendXMLEvent(env,
            "<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n" +
                "<event-page-visit xmlns=\"samples:schemas:simpleSchemaWithAll\" xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\" xsi:schemaLocation=\"samples:schemas:simpleSchemaWithAll simpleSchemaWithAll.xsd\">\n" +
                "<url>page2</url>" +
                "</event-page-visit>", "PageVisitEvent");
        assertFalse(env.listener("s0").isInvoked());

        EventType type = env.compileDeploy("@name('s1') select * from PageVisitEvent").statement("s1").getEventType();
        EPAssertionUtil.assertEqualsAnyOrder(new Object[]{
            new EventPropertyDescriptor("sessionId", Node.class, null, false, false, false, false, true),
            new EventPropertyDescriptor("customerId", Node.class, null, false, false, false, false, true),
            new EventPropertyDescriptor("url", String.class, null, false, false, false, false, false),
            new EventPropertyDescriptor("method", Node.class, null, false, false, false, false, true),
        }, type.getPropertyDescriptors());

        env.undeployAll();
    }
}
