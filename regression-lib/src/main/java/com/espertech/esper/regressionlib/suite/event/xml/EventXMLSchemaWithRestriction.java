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
import com.espertech.esper.regressionlib.framework.RegressionEnvironment;
import com.espertech.esper.regressionlib.framework.RegressionExecution;
import com.espertech.esper.regressionlib.support.util.SupportXML;

import static org.junit.Assert.assertEquals;

public class EventXMLSchemaWithRestriction implements RegressionExecution {

    public void run(RegressionEnvironment env) {
        String text = "@name('s0') select order_amount from OrderEvent";
        env.compileDeploy(text).addListener("s0");

        SupportXML.sendXMLEvent(env,
            "<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n" +
                "<order>\n" +
                "<order_amount>202.1</order_amount>" +
                "</order>", "OrderEvent");
        EventBean theEvent = env.listener("s0").getLastNewData()[0];
        assertEquals(Double.class, theEvent.get("order_amount").getClass());
        assertEquals(202.1d, theEvent.get("order_amount"));
        env.listener("s0").reset();

        env.undeployAll();
    }
}
