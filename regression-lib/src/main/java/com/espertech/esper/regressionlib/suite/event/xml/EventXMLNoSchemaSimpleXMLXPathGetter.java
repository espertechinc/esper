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

import com.espertech.esper.regressionlib.framework.RegressionEnvironment;
import com.espertech.esper.regressionlib.framework.RegressionExecution;

import static com.espertech.esper.regressionlib.suite.event.xml.EventXMLNoSchemaSimpleXMLDOMGetter.assertDataGetter;
import static com.espertech.esper.regressionlib.suite.event.xml.EventXMLNoSchemaSimpleXMLXPathProperties.sendEvent;

public class EventXMLNoSchemaSimpleXMLXPathGetter implements RegressionExecution {

    public void run(RegressionEnvironment env) {
        String stmt = "@name('s0') select element1, invalidelement, " +
            "element4.element41 as nestedElement," +
            "element2.element21('e21_2') as mappedElement," +
            "element2.element21[1] as indexedElement," +
            "element3.myattribute as invalidattribute " +
            "from TestXMLNoSchemaTypeWXPathPropTrue#length(100)";
        env.compileDeploy(stmt).addListener("s0");

        // Generate document with the specified in element1 to confirm we have independent events
        sendEvent(env, "EventA", "TestXMLNoSchemaTypeWXPathPropTrue");
        assertDataGetter(env, "EventA", true);

        sendEvent(env, "EventB", "TestXMLNoSchemaTypeWXPathPropTrue");
        assertDataGetter(env, "EventB", true);

        env.undeployAll();
    }
}
