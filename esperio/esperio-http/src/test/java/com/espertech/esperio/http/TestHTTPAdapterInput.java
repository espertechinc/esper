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

import com.espertech.esper.common.client.configuration.Configuration;
import com.espertech.esper.common.client.scopetest.EPAssertionUtil;
import com.espertech.esper.runtime.client.EPRuntime;
import com.espertech.esper.runtime.client.EPRuntimeProvider;
import com.espertech.esper.runtime.client.EPStatement;
import com.espertech.esper.runtime.client.scopetest.SupportUpdateListener;
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

    private void validateGet(boolean isNio, int port, String runtimeURI) throws Exception {

        String esperIOHTTPConfig = "<esperio-http-configuration>\n" +
            "<service name=\"service1\" port=\"" + port + "\" nio=\"" + isNio + "\"/>" +
            "<get service=\"service1\" pattern=\"*\"/>" +
            "</esperio-http-configuration>";

        Configuration configuration = new Configuration();
        configuration.getRuntime().addPluginLoader("EsperIOHTTPAdapter", EsperIOHTTPAdapterPlugin.class.getName(), new Properties(), esperIOHTTPConfig);

        configuration.getCommon().addEventType("SupportBean", SupportBean.class);

        EPRuntime runtime = EPRuntimeProvider.getRuntime(runtimeURI, configuration);

        EPStatement stmt = SupportCompileUtil.compileDeploy(runtime, "select * from SupportBean").getStatements()[0];
        stmt.addListener(listener);

        SupportHTTPClient client = new SupportHTTPClient(port);
        String[] fields = "stringProp,intProp".split(",");
        client.request(port, "sendevent", "stream", "SupportBean", "stringProp", "abc", "intProp", "5");
        EPAssertionUtil.assertProps(listener.assertOneGetNew(), fields, new Object[]{"abc", 5});

        listener.reset();
        runtime.destroy();

        try {
            client = new SupportHTTPClient(port);
            client.request(port, "sendevent");
            fail();
        } catch (Exception expected) {
        }
    }
}