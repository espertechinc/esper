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
package com.espertech.esperio.http;

import com.espertech.esper.client.Configuration;
import com.espertech.esper.client.EPServiceProvider;
import com.espertech.esper.client.EPServiceProviderManager;
import com.espertech.esper.client.EPStatement;
import com.espertech.esper.client.scopetest.EPAssertionUtil;
import com.espertech.esper.client.scopetest.SupportUpdateListener;
import junit.framework.TestCase;

import java.util.Properties;

public class TestHTTPAdapterInput extends TestCase {
    private SupportUpdateListener listener;

    public void setUp() throws Exception {
        listener = new SupportUpdateListener();
    }

    public void testGet() throws Exception {
        // test classic
        validateGet(false, 8082, "TestHTTPAdapterOutputClassic");

        // test nio
        validateGet(true, 8082, "TestHTTPAdapterOutputNIO");
    }

    private void validateGet(boolean isNio, int port, String engineURI) throws Exception {

        String esperIOHTTPConfig = "<esperio-http-configuration>\n" +
                "<service name=\"service1\" port=\"" + port + "\" nio=\"" + isNio + "\"/>" +
                "<get service=\"service1\" pattern=\"*\"/>" +
                "</esperio-http-configuration>";

        Configuration engineConfig = new Configuration();
        engineConfig.addPluginLoader("EsperIOHTTPAdapter", EsperIOHTTPAdapterPlugin.class.getName(), new Properties(), esperIOHTTPConfig);

        engineConfig.addEventType("SupportBean", SupportBean.class);

        EPServiceProvider provider = EPServiceProviderManager.getProvider(engineURI, engineConfig);

        EPStatement stmt = provider.getEPAdministrator().createEPL("select * from SupportBean");
        stmt.addListener(listener);

        SupportHTTPClient client = new SupportHTTPClient(port);
        String[] fields = "stringProp,intProp".split(",");
        client.request(port, "sendevent", "stream", "SupportBean", "stringProp", "abc", "intProp", "5");
        EPAssertionUtil.assertProps(listener.assertOneGetNew(), fields, new Object[]{"abc", 5});

        listener.reset();
        provider.destroy();

        try {
            client = new SupportHTTPClient(port);
            client.request(port, "sendevent");
            fail();
        } catch (Exception expected) {
        }
    }
}