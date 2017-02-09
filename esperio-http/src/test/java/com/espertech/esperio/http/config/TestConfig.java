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
package com.espertech.esperio.http.config;

import junit.framework.TestCase;

import java.net.URL;

public class TestConfig extends TestCase {
    private ConfigurationHTTPAdapter config;

    public void setUp() {
        config = new ConfigurationHTTPAdapter();
    }

    public void testConfigureFromStream() throws Exception {
        URL url = this.getClass().getClassLoader().getResource("esperio-http-sample-config.xml");
        ConfigurationHTTPAdapterParser.doConfigure(config, url.openStream(), url.toString());
        assertFileConfig(config);
    }

    public void testEngineDefaults() {
        config = new ConfigurationHTTPAdapter();
    }

    protected static void assertFileConfig(ConfigurationHTTPAdapter config) throws Exception {
        assertEquals(1, config.getServices().size());
        Service service = config.getServices().get("myservice");
        assertEquals(8079, service.getPort());
        assertEquals(true, service.isNio());

        assertEquals(1, config.getGetHandlers().size());
        GetHandler handler = config.getGetHandlers().get(0);
        assertEquals("myservice", handler.getService());
        assertEquals("*", handler.getPattern());

        assertEquals(2, config.getRequests().size());
        Request request = config.getRequests().get(0);
        assertEquals("MyOutputEventStream", request.getStream());
        assertEquals("http://myremotehost:80/root/event", request.getUri());

        request = config.getRequests().get(1);
        assertEquals("MyOutputEventStream", request.getStream());
        assertEquals("http://myremotehost:80/root/event/type=${stream}&parameterOne=${eventProperty1}&parameterTwo=${eventProperty2}", request.getUri());
    }
}
