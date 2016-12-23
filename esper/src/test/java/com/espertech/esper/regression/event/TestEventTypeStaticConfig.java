/*
 * *************************************************************************************
 *  Copyright (C) 2006-2015 EsperTech, Inc. All rights reserved.                       *
 *  http://www.espertech.com/esper                                                     *
 *  http://www.espertech.com                                                           *
 *  ---------------------------------------------------------------------------------- *
 *  The software in this package is published under the terms of the GPL license       *
 *  a copy of which has been included with this distribution in the license.txt file.  *
 * *************************************************************************************
 */

package com.espertech.esper.regression.event;

import com.espertech.esper.client.Configuration;
import com.espertech.esper.client.EPServiceProvider;
import com.espertech.esper.client.EPServiceProviderManager;
import com.espertech.esper.client.EventPropertyDescriptor;
import com.espertech.esper.client.scopetest.EPAssertionUtil;
import junit.framework.TestCase;

import java.util.HashMap;
import java.util.Map;

public class TestEventTypeStaticConfig extends TestCase
{
    public void testStaticConfig() throws Exception
    {
        String xml = "<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n" +
                "<esper-configuration>\t\n" +
                "\t<event-type name=\"MyMapEvent\">\n" +
                "\t\t<java-util-map>\n" +
                "\t  \t\t<map-property name=\"myStringArray\" class=\"string[]\"/>\n" +
                "\t  \t</java-util-map>\n" +
                "\t</event-type>\n" +
                "\t\n" +
                "\t<event-type name=\"MyObjectArrayEvent\">\n" +
                "\t\t<objectarray>\n" +
                "\t  \t\t<objectarray-property name=\"myStringArray\" class=\"string[]\"/>\n" +
                "\t  \t</objectarray>\n" +
                "\t</event-type>\n" +
                "</esper-configuration>\n";

        Configuration config = new Configuration();
        config.configure(SupportXML.getDocument(xml));

        // add a map-type and then clear the map to test copy of type definition for preventing accidental overwrite
        Map<String, Object> typeMyEventIsCopyDef = new HashMap<String, Object>();
        typeMyEventIsCopyDef.put("prop1", String.class);
        config.addEventType("MyEventIsCopyDef", typeMyEventIsCopyDef);
        typeMyEventIsCopyDef.clear();

        // obtain engine
        EPServiceProvider epService = EPServiceProviderManager.getDefaultProvider(config);
        epService.initialize();

        // ensure cleared type is available (type information was copied to prevent accidental overwrite)
        epService.getEPAdministrator().createEPL("select prop1 from MyEventIsCopyDef");

        // assert array types
        for (String name : new String[] {"MyObjectArrayEvent", "MyMapEvent"}) {
            EPAssertionUtil.assertEqualsAnyOrder(new Object[]{
                    new EventPropertyDescriptor("myStringArray", String[].class, String.class, false, false, true, false, false),
            }, epService.getEPAdministrator().getConfiguration().getEventType(name).getPropertyDescriptors());
        }
    }
}
